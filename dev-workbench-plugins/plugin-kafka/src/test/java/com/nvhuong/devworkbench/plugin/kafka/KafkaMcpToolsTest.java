package com.nvhuong.devworkbench.plugin.kafka;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;
import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class KafkaMcpToolsTest {

    private static PluginStorage storageWithProfile() {
        Map<String, String> files = new HashMap<>();
        PluginStorage storage = new PluginStorage() {
            @Override public Optional<String> readString(String path) { return Optional.ofNullable(files.get(path)); }
            @Override public void writeString(String path, String content) { files.put(path, content); }
            @Override public void delete(String path) { files.remove(path); }
            @Override public Path dataDirectory() { return Path.of(System.getProperty("java.io.tmpdir")); }
        };
        new KafkaConnectionProfileStore(storage).save(KafkaConnectionProfile.builder()
                .id("prod").name("Prod").bootstrapServers("broker1:9093").securityProtocol("SASL_SSL")
                .saslMechanism("PLAIN").username("svc").password("kafka-secret").build());
        return storage;
    }

    @Test
    public void toolsReadSafelyAndOnlySendWrites() throws Exception {
        PluginStorage storage = storageWithProfile();
        List<McpToolContribution> tools = KafkaMcpTools.tools(storage);
        assertEquals(tools.stream().map(McpToolContribution::name).toList(),
                List.of("kafka_list_profiles", "kafka_list_topics", "kafka_poll", "kafka_send"));
        assertEquals(tools.stream().filter(t -> !t.readOnly()).map(McpToolContribution::name).toList(), List.of("kafka_send"));
        assertFalse(tools.get(2).inputSchema().toString().contains("groupId"), "AI must not join a real consumer group");

        String profiles = tools.get(0).handler().call(Map.of()).toString();
        assertTrue(profiles.contains("broker1:9093"), profiles);
        assertFalse(profiles.contains("kafka-secret"), profiles);
        expectThrows(IllegalArgumentException.class, () -> tools.get(1).handler().call(Map.of("profile", "missing")));
    }
}
