package com.nvhuong.devworkbench.plugin.kafka;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MCP tools of the Kafka plugin. Clusters are addressed by the connection profiles saved in the Kafka tool, so SASL
 * credentials never pass through the AI client. Reading uses a throw-away consumer group without committing offsets,
 * so it never moves a real consumer group; sending is a separate write tool.
 */
final class KafkaMcpTools {
    private static final int DEFAULT_MESSAGES = 50;
    private static final int MAX_MESSAGES = 500;
    private static final int MAX_VALUE_CHARS = 4000;

    private KafkaMcpTools() {
    }

    static void register(PluginContext context) {
        for (McpToolContribution tool : tools(context.storage())) {
            context.registerMcpTool(tool);
        }
    }

    static List<McpToolContribution> tools(PluginStorage storage) {
        KafkaConnectionProfileStore store = new KafkaConnectionProfileStore(storage);
        return List.of(
                new McpToolContribution("kafka_list_profiles", "List Kafka connection profiles",
                        "Lists the Kafka connection profiles saved in the DevWorkbench Kafka tool. SASL passwords are never returned.",
                        schema(Map.of(), List.of()), true, arguments -> listProfiles(store)),
                new McpToolContribution("kafka_list_topics", "List Kafka topics",
                        "Lists the topics of the cluster of a saved profile.",
                        schema(Map.of("profile", string("Kafka profile name (see kafka_list_profiles).")), List.of("profile")),
                        true, arguments -> listTopics(store, arguments)),
                new McpToolContribution("kafka_poll", "Read Kafka messages",
                        "Reads up to maxMessages messages from a topic with a temporary consumer group (offsets are never committed, real consumer groups are not affected). from=earliest reads from the beginning, latest only waits for new messages.",
                        schema(Map.of(
                                "profile", string("Kafka profile name."),
                                "topic", string("Topic name."),
                                "from", Map.of("type", "string", "enum", List.of("earliest", "latest"),
                                        "description", "Where to start (default earliest)."),
                                "maxMessages", Map.of("type", "integer", "minimum", 1, "maximum", MAX_MESSAGES,
                                        "description", "Maximum messages to return (default 50)."),
                                "pollTimeoutMs", Map.of("type", "integer", "minimum", 100, "maximum", 30_000,
                                        "description", "How long to wait for messages (default 3000 ms).")),
                                List.of("profile", "topic")),
                        true, arguments -> poll(store, arguments)),
                new McpToolContribution("kafka_send", "Send a Kafka message",
                        "Produces one message (optional key and partition) to a topic and returns partition and offset. Only available with --allow-writes.",
                        schema(Map.of(
                                "profile", string("Kafka profile name."),
                                "topic", string("Topic name."),
                                "value", Map.of("type", "string", "description", "Message value."),
                                "key", Map.of("type", "string", "description", "Optional message key."),
                                "partition", Map.of("type", "integer", "minimum", 0, "description", "Optional partition.")),
                                List.of("profile", "topic", "value")),
                        false, arguments -> send(store, arguments)));
    }

    static Map<String, Object> listProfiles(KafkaConnectionProfileStore store) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (KafkaConnectionProfile profile : store.loadProfiles()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", profile.getName());
            item.put("bootstrapServers", profile.getBootstrapServers());
            item.put("securityProtocol", profile.getSecurityProtocol());
            items.add(item);
        }
        return Map.of("profiles", items, "count", items.size());
    }

    static Map<String, Object> listTopics(KafkaConnectionProfileStore store, Map<String, Object> arguments) {
        KafkaConnectionProfile profile = profile(store, arguments);
        List<String> topics = new ScriptKafkaApi().listTopics(connection(profile));
        return Map.of("profile", profile.getName(), "topics", topics, "count", topics.size());
    }

    static Map<String, Object> poll(KafkaConnectionProfileStore store, Map<String, Object> arguments) {
        KafkaConnectionProfile profile = profile(store, arguments);
        Map<String, Object> options = connection(profile);
        options.put("topic", required(arguments, "topic"));
        String from = optional(arguments, "from");
        options.put("autoOffsetReset", from == null ? "earliest" : from.toLowerCase(Locale.ROOT));
        int maxMessages = Math.max(1, Math.min(MAX_MESSAGES, integer(arguments, "maxMessages", DEFAULT_MESSAGES)));
        options.put("maxMessages", maxMessages);
        options.put("maxPollRecords", maxMessages);
        options.put("pollTimeoutMs", Math.max(100, Math.min(30_000, integer(arguments, "pollTimeoutMs", 3000))));
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ScriptKafkaApi.KafkaMessage message : new ScriptKafkaApi().poll(options)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("partition", message.partition);
            item.put("offset", message.offset);
            item.put("timestamp", message.timestamp);
            item.put("key", message.key);
            item.put("value", truncate(message.value));
            if (message.headers != null && !message.headers.isEmpty()) item.put("headers", message.headers);
            messages.add(item);
        }
        return Map.of("profile", profile.getName(), "topic", options.get("topic"), "messages", messages,
                "count", messages.size());
    }

    static Map<String, Object> send(KafkaConnectionProfileStore store, Map<String, Object> arguments) {
        KafkaConnectionProfile profile = profile(store, arguments);
        Map<String, Object> options = connection(profile);
        options.put("topic", required(arguments, "topic"));
        Object value = arguments.get("value");
        if (value == null) throw new IllegalArgumentException("value is required");
        options.put("value", String.valueOf(value));
        String key = optional(arguments, "key");
        if (key != null) options.put("key", key);
        Object partition = arguments.get("partition");
        if (partition != null) options.put("partition", partition);
        ScriptKafkaApi.KafkaSendResult result = new ScriptKafkaApi().send(options);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profile", profile.getName());
        response.put("topic", result.topic);
        response.put("partition", result.partition);
        response.put("offset", result.offset);
        response.put("timestamp", result.timestamp);
        return response;
    }

    private static Map<String, Object> connection(KafkaConnectionProfile profile) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("bootstrapServers", profile.getBootstrapServers());
        if (profile.getClientId() != null && !profile.getClientId().isBlank()) options.put("clientId", profile.getClientId());
        if (profile.getSecurityProtocol() != null) options.put("securityProtocol", profile.getSecurityProtocol());
        if (profile.getSaslMechanism() != null) options.put("saslMechanism", profile.getSaslMechanism());
        if (profile.getUsername() != null) options.put("username", profile.getUsername());
        if (profile.getPassword() != null) options.put("password", profile.getPassword());
        return options;
    }

    private static String truncate(String value) {
        return value == null || value.length() <= MAX_VALUE_CHARS ? value : value.substring(0, MAX_VALUE_CHARS) + "…";
    }

    private static KafkaConnectionProfile profile(KafkaConnectionProfileStore store, Map<String, Object> arguments) {
        String name = required(arguments, "profile");
        return store.loadProfiles().stream()
                .filter(profile -> name.equalsIgnoreCase(profile.getName()) || name.equalsIgnoreCase(profile.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Kafka profile: " + name + ". Use kafka_list_profiles."));
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description, "minLength", 1);
    }

    private static String required(Map<String, Object> arguments, String name) {
        String value = optional(arguments, name);
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String optional(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    private static int integer(Map<String, Object> arguments, String name, int defaultValue) {
        Object value = arguments == null ? null : arguments.get(name);
        if (value == null) return defaultValue;
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value).trim());
    }
}
