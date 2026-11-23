package com.nvhuong.devworkbench.mcp.server;

import com.nvhuong.devworkbench.util.JsonUtil;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class DevWorkbenchMcpServerTest {

    @Test(timeOut = 60_000)
    public void shouldCompleteStdioHandshakeAndToolCalls() throws Exception {
        PipedInputStream serverInput = new PipedInputStream();
        PipedOutputStream clientOutput = new PipedOutputStream(serverInput);
        PipedInputStream clientInput = new PipedInputStream();
        PipedOutputStream serverOutput = new PipedOutputStream(clientInput);
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        CompletableFuture<Integer> server = CompletableFuture.supplyAsync(() -> new DevWorkbenchMcpServer().serve(
                new FakeBackend(),
                "v1.2.3",
                serverInput,
                serverOutput,
                new PrintStream(stderr)
        ));

        try (PrintWriter writer = new PrintWriter(clientOutput, true, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientInput, StandardCharsets.UTF_8))) {
            writer.println("""
                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
                    """.trim());
            // The first response includes server start-up (SDK, Jackson, tool schemas): 3.5-6 s on slow CI machines.
            JsonNode initialize = readJsonLine(reader, 30);
            assertEquals(initialize.get("result").get("serverInfo").get("version").asText(), "1.2.3");

            writer.println("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
            writer.println("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}");
            JsonNode tools = readJsonLine(reader);
            assertEquals(tools.get("result").get("tools").size(), McpToolSpecifications.TOOL_NAMES.size() + 2,
                    "built-in tools plus the extra tool; a duplicate name must be dropped");
            JsonNode extraTool = null;
            for (JsonNode tool : tools.get("result").get("tools")) {
                if ("echo_extra".equals(tool.get("name").asText())) extraTool = tool;
            }
            assertNotNull(extraTool, "extra tool not listed");
            assertEquals(extraTool.get("annotations").get("readOnlyHint").asBoolean(), true);

            writer.println("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"echo_extra\",\"arguments\":{\"text\":\"hi\"}}}");
            JsonNode echoed = readJsonLine(reader);
            assertFalse(echoed.get("result").get("isError").asBoolean());
            assertEquals(echoed.get("result").get("structuredContent").get("echo").asText(), "hi");

            writer.println("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"list_workspaces\",\"arguments\":{}}}");
            JsonNode workspaces = readJsonLine(reader);
            assertFalse(workspaces.get("result").get("isError").asBoolean());
            assertEquals(workspaces.get("result").get("structuredContent").get("count").asInt(), 1);

            writer.println("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"list_collections\",\"arguments\":{\"workspaceId\":\"workspace-1\"}}}");
            JsonNode collections = readJsonLine(reader);
            assertFalse(collections.get("result").get("isError").asBoolean());
            assertEquals(collections.get("result").get("structuredContent").get("count").asInt(), 1);
        }

        assertEquals(server.get(5, TimeUnit.SECONDS), 0, stderr.toString(StandardCharsets.UTF_8));
    }

    @Test(timeOut = 10_000)
    public void shouldStopWhenMalformedInputTerminatesTheSdkReader() throws Exception {
        PipedInputStream serverInput = new PipedInputStream();
        PipedOutputStream clientOutput = new PipedOutputStream(serverInput);
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        CompletableFuture<Integer> server = CompletableFuture.supplyAsync(() -> new DevWorkbenchMcpServer().serve(
                new FakeBackend(),
                "1.2.3",
                serverInput,
                new ByteArrayOutputStream(),
                new PrintStream(stderr)
        ));

        try {
            clientOutput.write("not-json\n".getBytes(StandardCharsets.UTF_8));
            clientOutput.flush();
            assertEquals(server.get(5, TimeUnit.SECONDS), 0);
        } finally {
            clientOutput.close();
        }
    }

    private static JsonNode readJsonLine(BufferedReader reader) throws Exception {
        return readJsonLine(reader, 5);
    }

    private static JsonNode readJsonLine(BufferedReader reader, int timeoutSeconds) throws Exception {
        String line = CompletableFuture.supplyAsync(() -> {
            try {
                return reader.readLine();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).get(timeoutSeconds, TimeUnit.SECONDS);
        assertNotNull(line);
        return JsonUtil.readTree(line);
    }

    private static final class FakeBackend implements DevWorkbenchMcpBackend {
        @Override
        public List<McpExtraTool> extraTools() {
            Map<String, Object> schema = Map.of("type", "object", "properties",
                    Map.of("text", Map.of("type", "string")));
            return List.of(
                    new McpExtraTool("echo_extra", "Echo", "Echoes text", schema, true,
                            arguments -> Map.of("echo", arguments.get("text"))),
                    new McpExtraTool("list_workspaces", "Shadow", "must be ignored", schema, true,
                            arguments -> Map.of()),
                    new McpExtraTool("write_extra", "Write", "A write tool", schema, false,
                            arguments -> Map.of("ok", true)));
        }

        @Override
        public Map<String, Object> listWorkspaces() {
            return Map.of(
                    "workspaces", List.of(Map.of("id", "workspace-1", "name", "Test")),
                    "count", 1
            );
        }

        @Override
        public Map<String, Object> listCollections(Map<String, Object> arguments) {
            return Map.of(
                    "workspaceId", arguments.get("workspaceId"),
                    "collections", List.of(Map.of("id", "collection-1", "name", "Test API")),
                    "count", 1
            );
        }

        @Override
        public Map<String, Object> listEnvironments(Map<String, Object> arguments) {
            return Map.of();
        }

        @Override
        public Map<String, Object> listRequests(Map<String, Object> arguments) {
            return Map.of();
        }

        @Override
        public Map<String, Object> runRequest(Map<String, Object> arguments) {
            return Map.of();
        }

        @Override
        public Map<String, Object> runCollection(Map<String, Object> arguments) {
            return Map.of();
        }
    }
}
