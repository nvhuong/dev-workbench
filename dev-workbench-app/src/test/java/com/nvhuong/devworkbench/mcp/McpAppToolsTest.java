package com.nvhuong.devworkbench.mcp;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import com.nvhuong.devworkbench.collection.model.CollectionNode;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanConfiguration;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanStorage;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanWorkspace;
import com.nvhuong.devworkbench.performance.plan.PerformanceSavedPlan;
import com.nvhuong.devworkbench.performance.runtime.PerformanceRunExecutionResult;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.RequestBodyTypes;
import com.nvhuong.devworkbench.service.collections.CollectionDocumentJsonCodec;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class McpAppToolsTest {
    private MockWebServer server;

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (server != null) server.shutdown();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void runFunctionalRunsTheSelectedRequestsOfTheWorkspace() throws Exception {
        server = new MockWebServer();
        server.start();
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
        Path workspace = workspace(request("request-1", "Health", server.url("/health").toString()),
                request("request-2", "Not selected", server.url("/other").toString()));
        Files.writeString(workspace.resolve("functional_config.json"), """
                {"rows":[{"requestItemId":"request-1","selected":true},{"requestItemId":"request-2","selected":false}]}
                """, StandardCharsets.UTF_8);
        McpAppTools tools = new McpAppTools(service(workspace, false));

        Map<String, Object> result = tools.runFunctional(Map.of());

        Map<String, Object> run = (Map<String, Object>) result.get("run");
        assertEquals(run.get("status"), "SUCCESS");
        assertEquals(server.getRequestCount(), 1, "only the selected request runs");
        assertEquals(server.takeRequest().getPath(), "/health");
    }

    @Test
    public void runFunctionalExplainsMissingConfig() throws Exception {
        Path workspace = workspace(request("request-1", "Health", "https://example.test/health"));
        McpAppTools tools = new McpAppTools(service(workspace, false));

        IllegalArgumentException error = expectThrows(IllegalArgumentException.class, () -> tools.runFunctional(Map.of()));
        assertTrue(error.getMessage().contains("functional_config.json"), error.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void requestHistoryIsNewestFirstFilteredAndRedacted() throws Exception {
        Path history = Files.createTempFile("history", ".json");
        Files.writeString(history, """
                [
                  {"method":"GET","url":"https://api.test/a?access_token=old-secret","responseCode":200,"requestTime":1000,
                   "response":{"costMs":12,"body":"{\\"access_token\\":\\"body-secret\\"}"}},
                  {"method":"POST","url":"https://api.test/orders","responseCode":201,"requestTime":3000,
                   "response":{"costMs":40,"body":"created"}},
                  {"method":"GET","url":"https://other.test/b","responseCode":404,"requestTime":2000,"response":{"costMs":5}}
                ]
                """, StandardCharsets.UTF_8);
        McpAppTools tools = new McpAppTools(service(workspace(request("r", "R", "https://example.test")), false));
        tools.historyFile = () -> history;

        Map<String, Object> all = tools.listRequestHistory(Map.of("includeBodies", true));
        List<Map<String, Object>> items = (List<Map<String, Object>>) all.get("items");
        assertEquals(items.stream().map(item -> item.get("method") + " " + item.get("status")).toList(),
                List.of("POST 201", "GET 404", "GET 200"));
        assertFalse(all.toString().contains("old-secret"), all.toString());
        assertFalse(all.toString().contains("body-secret"), all.toString());

        Map<String, Object> filtered = tools.listRequestHistory(Map.of("query", "api.test", "limit", 1));
        assertEquals(filtered.get("count"), 1);
        assertEquals(filtered.get("matched"), 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mockServersAreListedWithoutAccessKeys() throws Exception {
        Path workspace = workspace(request("r", "R", "https://example.test"));
        Files.writeString(workspace.resolve("mock_servers.json"), """
                [{"id":"m1","name":"Orders mock","host":"0.0.0.0","port":3005,"accessKey":"top-secret-key",
                  "standaloneRoutes":[{"id":"r1","name":"List","method":"GET","pathPattern":"/orders"}]}]
                """, StandardCharsets.UTF_8);
        McpAppTools tools = new McpAppTools(service(workspace, false));

        Map<String, Object> result = tools.listMockServers(Map.of());

        Map<String, Object> mock = ((List<Map<String, Object>>) result.get("servers")).get(0);
        assertEquals(mock.get("address"), "http://localhost:3005");
        assertEquals(mock.get("protectedByAccessKey"), true);
        assertEquals(mock.get("standaloneRoutes"), List.of(Map.of("method", "GET", "path", "/orders")));
        assertFalse(result.toString().contains("top-secret-key"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void performancePlansAreListedAndRunOnlyWithWrites() throws Exception {
        Path workspace = workspace(request("r", "R", "https://example.test"));
        new PerformancePlanStorage().saveWorkspace(workspace.resolve("performance_config.json"), new PerformancePlanWorkspace("p2",
                List.of(PerformanceSavedPlan.fromConfiguration("p1", "Smoke", PerformancePlanConfiguration.builder().build()),
                        PerformanceSavedPlan.fromConfiguration("p2", "Peak", PerformancePlanConfiguration.builder().build()))));

        McpAppTools readOnly = new McpAppTools(service(workspace, false));
        Map<String, Object> plans = readOnly.listPerformancePlans(Map.of());
        assertEquals(((List<Map<String, Object>>) plans.get("plans")).stream()
                .map(plan -> plan.get("name") + ":" + plan.get("active")).toList(), List.of("Smoke:false", "Peak:true"));
        assertFalse(readOnly.tools().stream().anyMatch(tool -> tool.name().equals("run_performance")));
        expectThrows(IllegalStateException.class, () -> readOnly.runPerformance(Map.of()));

        McpAppTools writable = new McpAppTools(service(workspace, true));
        AtomicReference<String> label = new AtomicReference<>();
        writable.performanceRunner = (plan, runLabel, control) -> {
            label.set(runLabel);
            return PerformanceRunExecutionResult.builder().status(PerformanceRunExecutionResult.STATUS_SUCCESS)
                    .totalRequests(10L).successRequests(9L).elapsedTimeMs(1500L).build();
        };
        Map<String, Object> result = writable.runPerformance(Map.of("plan", "smoke", "maxDurationSeconds", 30));
        assertEquals(label.get(), "mcp:Smoke");
        assertEquals(result.get("totalRequests"), 10L);
        assertEquals(result.get("failedRequests"), 1L);
        assertEquals(result.get("maxDurationSeconds"), 30);
        expectThrows(IllegalArgumentException.class, () -> writable.runPerformance(Map.of("plan", "missing")));
    }

    private static McpWorkspaceToolService service(Path workspace, boolean allowWrites) {
        return new McpWorkspaceToolService(new McpServeOptions(workspace.toString(), 64 * 1024, allowWrites));
    }

    private static Path workspace(HttpRequestItem... requests) throws Exception {
        Path workspace = Files.createTempDirectory("dev-workbench-mcp-app-");
        RequestGroup group = new RequestGroup("API");
        group.setId("collection-1");
        CollectionNode root = CollectionNode.group(group);
        for (HttpRequestItem request : requests) root.addChild(CollectionNode.request(request));
        CollectionDocumentJsonCodec.write(workspace.resolve("collections.json").toFile(), new CollectionDocument(List.of(root)));
        return workspace;
    }

    private static HttpRequestItem request(String id, String name, String url) {
        HttpRequestItem request = new HttpRequestItem();
        request.setId(id);
        request.setName(name);
        request.setMethod("GET");
        request.setUrl(url);
        request.setBodyType(RequestBodyTypes.BODY_TYPE_NONE);
        return request;
    }
}
