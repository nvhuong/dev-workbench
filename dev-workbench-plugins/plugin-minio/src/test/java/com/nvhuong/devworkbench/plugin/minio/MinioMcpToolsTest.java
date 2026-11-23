package com.nvhuong.devworkbench.plugin.minio;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;
import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class MinioMcpToolsTest {

    private static PluginStorage storage() {
        Map<String, String> files = new HashMap<>();
        files.put(MinioMcpTools.PROFILE_FILE, Stream.of("Files", "http://minio.local:9000", "AKIA", "minio-secret")
                .map(v -> Base64.getUrlEncoder().withoutPadding().encodeToString(v.getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.joining("\t")) + "\n");
        return new PluginStorage() {
            @Override public Optional<String> readString(String path) { return Optional.ofNullable(files.get(path)); }
            @Override public void writeString(String path, String content) { files.put(path, content); }
            @Override public void delete(String path) { files.remove(path); }
            @Override public Path dataDirectory() { return Path.of(System.getProperty("java.io.tmpdir")); }
        };
    }

    @Test
    public void readToolsAndGatedPresignedLink() throws Exception {
        List<McpToolContribution> tools = MinioMcpTools.tools(storage());
        assertEquals(tools.stream().map(McpToolContribution::name).toList(), List.of("minio_list_profiles",
                "minio_list_buckets", "minio_list_objects", "minio_read_object", "minio_presigned_url"));
        assertEquals(tools.stream().filter(t -> !t.readOnly()).map(McpToolContribution::name).toList(),
                List.of("minio_presigned_url"));
        String profiles = tools.get(0).handler().call(Map.of()).toString();
        assertTrue(profiles.contains("minio.local"), profiles);
        assertFalse(profiles.contains("minio-secret") || profiles.contains("AKIA"), profiles);
        expectThrows(IllegalArgumentException.class, () -> tools.get(1).handler().call(Map.of("profile", "missing")));
    }

    @Test
    public void textDetectionToleratesACutMultiByteCharacter() {
        byte[] vietnamese = "Báo cáo tháng".getBytes(StandardCharsets.UTF_8);
        assertEquals(MinioMcpTools.utf8(vietnamese), "Báo cáo tháng");
        byte[] cut = java.util.Arrays.copyOf(vietnamese, 2); // 'B' + first byte of 'á'
        assertEquals(MinioMcpTools.utf8(cut), "B");
        assertNull(MinioMcpTools.utf8(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0, (byte) 0xFF, (byte) 0xFE}));
    }
}
