package com.nvhuong.devworkbench.plugin.redis;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * MCP tools of the Redis plugin. Servers are addressed by the connection profiles saved in the Redis tool, so
 * passwords never pass through the AI client. Reads and writes are separate tools.
 */
final class RedisMcpTools {
    static final Set<String> READ_COMMANDS = Set.of("GET", "EXISTS", "TYPE", "TTL", "HGET", "HGETALL", "LRANGE",
            "SMEMBERS", "ZRANGE");
    static final Set<String> WRITE_COMMANDS = Set.of("SET", "DEL");
    private static final int DEFAULT_SCAN_LIMIT = 200;
    private static final int MAX_SCAN_LIMIT = 5000;

    private RedisMcpTools() {
    }

    static void register(PluginContext context) {
        for (McpToolContribution tool : tools(context.storage())) {
            context.registerMcpTool(tool);
        }
    }

    static List<McpToolContribution> tools(PluginStorage storage) {
        RedisConnectionProfileStore store = new RedisConnectionProfileStore(storage);
        return List.of(
                new McpToolContribution("redis_list_profiles", "List Redis connection profiles",
                        "Lists the Redis connection profiles saved in the DevWorkbench Redis tool (host, port, database). Passwords are never returned.",
                        schema(Map.of(), List.of()), true, arguments -> listProfiles(store)),
                new McpToolContribution("redis_scan_keys", "Find Redis keys",
                        "Scans keys matching a glob pattern (SCAN, non-blocking) and returns up to limit keys with their type.",
                        schema(Map.of(
                                "profile", string("Redis profile name (see redis_list_profiles)."),
                                "pattern", string("Glob pattern, default *."),
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", MAX_SCAN_LIMIT,
                                        "description", "Maximum keys to return (default 200).")),
                                List.of("profile")),
                        true, arguments -> scanKeys(store, arguments)),
                new McpToolContribution("redis_read", "Read a Redis key",
                        "Runs a read command on one key: " + String.join(", ", sorted(READ_COMMANDS))
                                + ". args: HGET field; LRANGE/ZRANGE start stop (default 0 99).",
                        schema(Map.of(
                                "profile", string("Redis profile name."),
                                "command", Map.of("type", "string", "enum", sorted(READ_COMMANDS)),
                                "key", string("Key name."),
                                "args", Map.of("type", "string", "description", "Optional command arguments separated by spaces.")),
                                List.of("profile", "command", "key")),
                        true, arguments -> run(store, arguments, READ_COMMANDS)),
                new McpToolContribution("redis_write", "Write a Redis key",
                        "Runs SET (value, optional ttlSeconds) or DEL on one key. Only available with --allow-writes.",
                        schema(Map.of(
                                "profile", string("Redis profile name."),
                                "command", Map.of("type", "string", "enum", sorted(WRITE_COMMANDS)),
                                "key", string("Key name."),
                                "value", Map.of("type", "string", "description", "Value for SET."),
                                "ttlSeconds", Map.of("type", "integer", "minimum", 1,
                                        "description", "Optional expiry for SET, in seconds.")),
                                List.of("profile", "command", "key")),
                        false, arguments -> run(store, arguments, WRITE_COMMANDS)));
    }

    static Map<String, Object> listProfiles(RedisConnectionProfileStore store) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (RedisConnectionProfile profile : store.loadProfiles()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", profile.getName());
            item.put("host", profile.getHost());
            item.put("port", profile.getPort());
            item.put("database", profile.getDatabase());
            item.put("authenticated", profile.getPassword() != null && !profile.getPassword().isBlank());
            items.add(item);
        }
        return Map.of("profiles", items, "count", items.size());
    }

    static Map<String, Object> scanKeys(RedisConnectionProfileStore store, Map<String, Object> arguments) {
        RedisConnectionProfile profile = profile(store, arguments);
        String pattern = optional(arguments, "pattern");
        int limit = Math.max(1, Math.min(MAX_SCAN_LIMIT, integer(arguments, "limit", DEFAULT_SCAN_LIMIT)));
        try (JedisPooled jedis = client(profile)) {
            Set<String> keys = new LinkedHashSet<>();
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().count(500).match(pattern == null ? "*" : pattern);
            do {
                var page = jedis.scan(cursor, params);
                cursor = page.getCursor();
                for (String key : page.getResult()) {
                    keys.add(key);
                    if (keys.size() >= limit) break;
                }
            } while (keys.size() < limit && !ScanParams.SCAN_POINTER_START.equals(cursor));
            List<Map<String, Object>> items = new ArrayList<>();
            for (String key : keys) {
                items.add(Map.of("key", key, "type", jedis.type(key)));
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("profile", profile.getName());
            response.put("keys", items);
            response.put("count", items.size());
            response.put("truncated", !ScanParams.SCAN_POINTER_START.equals(cursor));
            return response;
        }
    }

    static Map<String, Object> run(RedisConnectionProfileStore store, Map<String, Object> arguments, Set<String> allowed) {
        String command = required(arguments, "command").toUpperCase(Locale.ROOT);
        if (!allowed.contains(command)) {
            throw new IllegalArgumentException("Command " + command + " is not allowed here; allowed: " + sorted(allowed));
        }
        RedisConnectionProfile profile = profile(store, arguments);
        if ("SET".equals(command)) {
            return set(profile, arguments);
        }
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("host", profile.getHost());
        options.put("port", profile.getPort());
        options.put("db", profile.getDatabase());
        if (profile.getUsername() != null) options.put("username", profile.getUsername());
        if (profile.getPassword() != null) options.put("password", profile.getPassword());
        options.put("command", command);
        options.put("key", required(arguments, "key"));
        String args = optional(arguments, "args");
        if (args != null) options.put("args", args);
        Object value = arguments.get("value");
        if (value != null) options.put("value", String.valueOf(value));
        Object result = new ScriptRedisApi().execute(options);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profile", profile.getName());
        response.put("command", command);
        response.put("key", options.get("key"));
        response.put("result", result instanceof Set<?> set ? new ArrayList<>(set) : result);
        return response;
    }

    private static Map<String, Object> set(RedisConnectionProfile profile, Map<String, Object> arguments) {
        String key = required(arguments, "key");
        Object value = arguments.get("value");
        if (value == null) throw new IllegalArgumentException("SET requires value");
        Object ttl = arguments.get("ttlSeconds");
        try (JedisPooled jedis = client(profile)) {
            String result = ttl == null
                    ? jedis.set(key, String.valueOf(value))
                    : jedis.set(key, String.valueOf(value), SetParams.setParams().ex(integer(arguments, "ttlSeconds", 0)));
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("profile", profile.getName());
            response.put("command", "SET");
            response.put("key", key);
            response.put("result", result);
            if (ttl != null) response.put("ttlSeconds", integer(arguments, "ttlSeconds", 0));
            return response;
        }
    }

    private static JedisPooled client(RedisConnectionProfile profile) {
        DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder()
                .database(profile.getDatabase())
                .connectionTimeoutMillis(10_000)
                .socketTimeoutMillis(10_000);
        if (profile.getUsername() != null && !profile.getUsername().isBlank()) config.user(profile.getUsername());
        if (profile.getPassword() != null && !profile.getPassword().isBlank()) config.password(profile.getPassword());
        return new JedisPooled(new HostAndPort(profile.getHost(), profile.getPort()), config.build());
    }

    private static RedisConnectionProfile profile(RedisConnectionProfileStore store, Map<String, Object> arguments) {
        String name = required(arguments, "profile");
        return store.loadProfiles().stream()
                .filter(profile -> name.equalsIgnoreCase(profile.getName()) || name.equalsIgnoreCase(profile.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Redis profile: " + name + ". Use redis_list_profiles."));
    }

    private static List<String> sorted(Set<String> values) {
        return values.stream().sorted().toList();
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
