package com.nvhuong.devworkbench.plugin.redis;

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

public class RedisMcpToolsTest {

    private static PluginStorage storageWithProfile() {
        Map<String, String> files = new HashMap<>();
        PluginStorage storage = new PluginStorage() {
            @Override public Optional<String> readString(String path) { return Optional.ofNullable(files.get(path)); }
            @Override public void writeString(String path, String content) { files.put(path, content); }
            @Override public void delete(String path) { files.remove(path); }
            @Override public Path dataDirectory() { return Path.of(System.getProperty("java.io.tmpdir")); }
        };
        new RedisConnectionProfileStore(storage).save(RedisConnectionProfile.builder()
                .id("cache").name("Cache").host("cache.internal").port(6380).database(2)
                .username("app").password("s3cret").build());
        return storage;
    }

    private static McpToolContribution tool(PluginStorage storage, String name) {
        return RedisMcpTools.tools(storage).stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    public void readAndWriteToolsAreSeparate() {
        List<McpToolContribution> tools = RedisMcpTools.tools(storageWithProfile());
        assertEquals(tools.stream().map(McpToolContribution::name).toList(),
                List.of("redis_list_profiles", "redis_scan_keys", "redis_read", "redis_write"));
        assertEquals(tools.stream().filter(t -> !t.readOnly()).map(McpToolContribution::name).toList(), List.of("redis_write"));
    }

    @Test
    public void listsProfilesWithoutPasswords() throws Exception {
        Map<String, Object> result = tool(storageWithProfile(), "redis_list_profiles").handler().call(Map.of());
        String text = result.toString();
        assertTrue(text.contains("cache.internal") && text.contains("6380"), text);
        assertFalse(text.contains("s3cret"), text);
    }

    @Test
    public void readToolRejectsWriteCommandsAndUnknownProfiles() {
        PluginStorage storage = storageWithProfile();
        McpToolContribution read = tool(storage, "redis_read");
        expectThrows(IllegalArgumentException.class, () -> read.handler().call(Map.of("profile", "Cache", "command", "DEL", "key", "a")));
        expectThrows(IllegalArgumentException.class, () -> read.handler().call(Map.of("profile", "Nope", "command", "GET", "key", "a")));
        McpToolContribution write = tool(storage, "redis_write");
        expectThrows(IllegalArgumentException.class, () -> write.handler().call(Map.of("profile", "Cache", "command", "FLUSHALL", "key", "a")));
    }
}
