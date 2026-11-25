package com.nvhuong.devworkbench.mcp;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.common.constants.AppConstants;
import com.nvhuong.devworkbench.common.constants.ConfigPathConstants;
import com.nvhuong.devworkbench.functional.cli.FunctionalRunPlanner;
import com.nvhuong.devworkbench.mcp.server.McpExtraTool;
import com.nvhuong.devworkbench.mock.model.MockRoute;
import com.nvhuong.devworkbench.mock.model.MockServerDefinition;
import com.nvhuong.devworkbench.model.Environment;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanStorage;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanWorkspace;
import com.nvhuong.devworkbench.performance.plan.PerformanceRunPlanFactory;
import com.nvhuong.devworkbench.performance.plan.PerformanceSavedPlan;
import com.nvhuong.devworkbench.performance.runtime.PerformanceRunExecutionControl;
import com.nvhuong.devworkbench.performance.runtime.PerformanceRunExecutionResult;
import com.nvhuong.devworkbench.performance.runtime.PerformanceRunPlanExecutor;
import com.nvhuong.devworkbench.util.JsonUtil;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * MCP tools for the app features beyond saved requests: functional tests, load tests, request history and mock
 * servers. Load tests generate real traffic, so run_performance is only listed with --allow-writes and is always
 * stopped after a hard time limit.
 */
final class McpAppTools {
    static final int DEFAULT_PERFORMANCE_SECONDS = 120;
    static final int MAX_PERFORMANCE_SECONDS = 900;
    private static final int MAX_HISTORY_ITEMS = 100;
    private static final int MAX_BODY_PREVIEW = 2000;
    private static final long MAX_HISTORY_FILE_BYTES = 64L * 1024 * 1024;

    private final McpWorkspaceToolService service;
    /** Request history file; replaced in tests. */
    Supplier<Path> historyFile = () -> Path.of(ConfigPathConstants.REQUEST_HISTORY);
    /** Load-test runner; replaced in tests. */
    PerformanceRunner performanceRunner = McpAppTools::runPlan;

    @FunctionalInterface
    interface PerformanceRunner {
        PerformanceRunExecutionResult run(com.nvhuong.devworkbench.performance.core.run.PerformanceRunPlan plan, String label,
                                          PerformanceRunExecutionControl control) throws Exception;
    }

    McpAppTools(McpWorkspaceToolService service) {
        this.service = service;
    }

    List<McpExtraTool> tools() {
        List<McpExtraTool> tools = new ArrayList<>();
        tools.add(new McpExtraTool("run_functional", "Run functional test",
                "Runs the functional test saved in the workspace (the selected requests and CSV rows of "
                        + "functional_config.json) and returns pass/fail per request with tests, like run_collection. "
                        + "HTTP requests only.",
                schema(Map.of(
                        "workspaceId", string("Workspace id from list_workspaces (optional with one workspace)."),
                        "environmentId", string("Environment id or name; defaults to the active environment."),
                        "environmentOverrides", Map.of("type", "object", "additionalProperties", Map.of("type", "string"),
                                "description", "Temporary variable values for this run only."),
                        "bail", Map.of("type", "boolean", "description", "Stop after the first failed request or test.")),
                        List.of()),
                false, this::runFunctional));
        tools.add(new McpExtraTool("list_performance_plans", "List load test plans",
                "Lists the load test plans saved in the workspace (id, name, which one is active).",
                schema(Map.of("workspaceId", string("Workspace id from list_workspaces.")), List.of()),
                true, this::listPerformancePlans));
        if (service.allowWrites()) {
            tools.add(new McpExtraTool("run_performance", "Run load test",
                    "Runs a saved load test plan against the real targets and returns totals and the summary report. "
                            + "Generates real traffic; stopped after maxDurationSeconds (default 120, max 900). "
                            + "Only available with --allow-writes.",
                    schema(Map.of(
                            "workspaceId", string("Workspace id from list_workspaces."),
                            "plan", string("Plan id or name (see list_performance_plans); defaults to the active plan."),
                            "environmentId", string("Environment id or name; defaults to the active environment."),
                            "maxDurationSeconds", Map.of("type", "integer", "minimum", 5, "maximum", MAX_PERFORMANCE_SECONDS,
                                    "description", "Hard stop after this many seconds.")),
                            List.of()),
                    false, this::runPerformance));
        }
        tools.add(new McpExtraTool("list_request_history", "List request history",
                "Lists recently sent requests from the DevWorkbench history (newest first): method, URL, status, "
                        + "time. Sensitive query values are redacted; includeBodies adds a short response preview.",
                schema(Map.of(
                        "query", string("Optional case-insensitive filter on method or URL."),
                        "limit", Map.of("type", "integer", "minimum", 1, "maximum", MAX_HISTORY_ITEMS,
                                "description", "Maximum entries (default 20)."),
                        "includeBodies", Map.of("type", "boolean",
                                "description", "Include a response body preview (2000 characters).")),
                        List.of()),
                true, this::listRequestHistory));
        tools.add(new McpExtraTool("list_mock_servers", "List mock servers",
                "Lists the mock servers defined in the workspace with their address and routes. Access keys are never "
                        + "returned.",
                schema(Map.of("workspaceId", string("Workspace id from list_workspaces.")), List.of()),
                true, this::listMockServers));
        return tools;
    }

    Map<String, Object> runFunctional(Map<String, Object> arguments) throws Exception {
        McpAuthorizedWorkspace workspace = service.resolveWorkspace(arguments);
        if (!Files.isRegularFile(workspace.runWorkspace().directory().resolve("functional_config.json"))) {
            throw new IllegalArgumentException("This workspace has no functional test yet (functional_config.json). "
                    + "Select requests in the Functional panel of DevWorkbench first.");
        }
        return service.executePlan(workspace,
                McpToolArguments.optionalString(arguments, "environmentId"),
                McpToolArguments.stringMap(arguments, "environmentOverrides"),
                McpToolArguments.bool(arguments, "bail", false),
                new FunctionalRunPlanner());
    }

    Map<String, Object> listPerformancePlans(Map<String, Object> arguments) {
        McpAuthorizedWorkspace workspace = service.resolveWorkspace(arguments);
        PerformancePlanWorkspace plans = loadPlans(workspace);
        List<Map<String, Object>> items = new ArrayList<>();
        for (PerformanceSavedPlan plan : plans.getPlans()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", plan.getId());
            item.put("name", plan.getName());
            item.put("active", plan.getId().equals(plans.getActivePlanId()));
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspaceId", workspace.id());
        result.put("plans", items);
        result.put("count", items.size());
        return result;
    }

    Map<String, Object> runPerformance(Map<String, Object> arguments) throws Exception {
        if (!service.allowWrites()) {
            throw new IllegalStateException("run_performance needs --allow-writes");
        }
        McpAuthorizedWorkspace workspace = service.resolveWorkspace(arguments);
        PerformancePlanWorkspace plans = loadPlans(workspace);
        String selector = McpToolArguments.optionalString(arguments, "plan");
        PerformanceSavedPlan plan = plans.getPlans().stream()
                .filter(candidate -> selector == null
                        ? candidate.getId().equals(plans.getActivePlanId())
                        : selector.equals(candidate.getId()) || selector.equalsIgnoreCase(candidate.getName()))
                .findFirst()
                .or(() -> selector == null ? plans.getPlans().stream().findFirst() : java.util.Optional.empty())
                .orElseThrow(() -> new IllegalArgumentException("Load test plan not found: " + selector
                        + ". Use list_performance_plans."));
        Environment environment = selectEnvironment(service.environmentsOf(workspace),
                McpToolArguments.optionalString(arguments, "environmentId"));
        int maxSeconds = McpToolArguments.integer(arguments, "maxDurationSeconds", DEFAULT_PERFORMANCE_SECONDS, 5,
                MAX_PERFORMANCE_SECONDS);
        var runPlan = PerformanceRunPlanFactory.create(plan.toConfiguration(), environment, null,
                AppConstants.APP_NAME + " MCP");

        PerformanceRunExecutionControl control = new PerformanceRunExecutionControl();
        ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "mcp-performance-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        service.executionLock().lockInterruptibly();
        try {
            watchdog.schedule(control::stop, maxSeconds, TimeUnit.SECONDS);
            PerformanceRunExecutionResult result = performanceRunner.run(runPlan, "mcp:" + plan.getName(), control);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("workspaceId", workspace.id());
            response.put("plan", plan.getName());
            response.put("environment", environment == null ? null : environment.getName());
            response.put("status", result.getStatus());
            response.put("stoppedByTimeLimit", result.isStopped());
            response.put("maxDurationSeconds", maxSeconds);
            response.put("elapsedMs", result.getElapsedTimeMs());
            response.put("totalRequests", result.getTotalRequests());
            response.put("successRequests", result.getSuccessRequests());
            response.put("failedRequests", result.getFailedRequests());
            if (!result.getError().isBlank()) {
                response.put("error", McpSensitiveDataRedactor.message(result.getError()));
            }
            if (result.getReport() != null && result.getReport().getSummary() != null) {
                response.put("summary", new LinkedHashMap<>(JSONUtil.parseObj(JsonUtil.toJsonStr(result.getReport().getSummary()))));
            }
            return response;
        } finally {
            watchdog.shutdownNow();
            service.executionLock().unlock();
        }
    }

    Map<String, Object> listRequestHistory(Map<String, Object> arguments) throws Exception {
        String query = McpToolArguments.optionalString(arguments, "query");
        int limit = McpToolArguments.integer(arguments, "limit", 20, 1, MAX_HISTORY_ITEMS);
        boolean includeBodies = McpToolArguments.bool(arguments, "includeBodies", false);
        Path file = historyFile.get();
        List<Map<String, Object>> items = new ArrayList<>();
        int matched = 0;
        if (Files.isRegularFile(file) && Files.size(file) > 0 && Files.size(file) <= MAX_HISTORY_FILE_BYTES) {
            JSONArray history = JSONUtil.parseArray(Files.readString(file, StandardCharsets.UTF_8));
            List<cn.hutool.json.JSONObject> entries = new ArrayList<>();
            for (Object raw : history) {
                if (raw instanceof cn.hutool.json.JSONObject entry) entries.add(entry);
            }
            entries.sort((a, b) -> Long.compare(b.getLong("requestTime", 0L), a.getLong("requestTime", 0L)));
            for (cn.hutool.json.JSONObject entry : entries) {
                String method = entry.getStr("method", "");
                String url = entry.getStr("url", "");
                if (query != null && !(method + " " + url).toLowerCase(Locale.ROOT)
                        .contains(query.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                matched++;
                if (items.size() >= limit) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("method", method);
                item.put("url", McpSensitiveDataRedactor.url(url));
                item.put("status", entry.getInt("responseCode", 0));
                long time = entry.getLong("requestTime", 0L);
                item.put("time", time > 0 ? Instant.ofEpochMilli(time).toString() : null);
                cn.hutool.json.JSONObject response = entry.getJSONObject("response");
                if (response != null) {
                    item.put("durationMs", response.getLong("costMs", 0L));
                    if (includeBodies) {
                        String body = response.getStr("body", "");
                        boolean truncated = body.length() > MAX_BODY_PREVIEW;
                        item.put("bodyPreview", McpSensitiveDataRedactor.payload(
                                truncated ? body.substring(0, MAX_BODY_PREVIEW) : body));
                        item.put("bodyTruncated", truncated);
                    }
                }
                items.add(item);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("count", items.size());
        result.put("matched", matched);
        return result;
    }

    Map<String, Object> listMockServers(Map<String, Object> arguments) {
        McpAuthorizedWorkspace workspace = service.resolveWorkspace(arguments);
        Path file = workspace.runWorkspace().directory().resolve("mock_servers.json");
        List<Map<String, Object>> servers = new ArrayList<>();
        if (Files.isRegularFile(file)) {
            List<MockServerDefinition> definitions = JSONUtil.toList(
                    JSONUtil.readJSONArray(file.toFile(), StandardCharsets.UTF_8), MockServerDefinition.class);
            for (MockServerDefinition definition : definitions) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", definition.getId());
                item.put("name", definition.getName());
                item.put("address", "http://" + ("0.0.0.0".equals(definition.getHost()) ? "localhost" : definition.getHost())
                        + ":" + definition.getPort());
                item.put("corsEnabled", definition.isCorsEnabled());
                item.put("fixedDelayMs", definition.getFixedDelayMs());
                item.put("protectedByAccessKey", definition.getAccessKey() != null && !definition.getAccessKey().isBlank());
                item.put("collectionSources", definition.getCollectionSources() == null ? 0 : definition.getCollectionSources().size());
                List<Map<String, Object>> routes = new ArrayList<>();
                if (definition.getStandaloneRoutes() != null) {
                    for (MockRoute route : definition.getStandaloneRoutes()) {
                        routes.add(Map.of("method", String.valueOf(route.method()), "path", String.valueOf(route.pathPattern())));
                    }
                }
                item.put("standaloneRoutes", routes);
                servers.add(item);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspaceId", workspace.id());
        result.put("servers", servers);
        result.put("count", servers.size());
        return result;
    }

    private static PerformancePlanWorkspace loadPlans(McpAuthorizedWorkspace workspace) {
        Path file = workspace.runWorkspace().directory().resolve("performance_config.json");
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("This workspace has no load test plans yet (performance_config.json).");
        }
        return new PerformancePlanStorage().loadWorkspace(file);
    }

    static Environment selectEnvironment(List<Environment> environments, String selector) {
        if (environments == null || environments.isEmpty()) return null;
        if (selector != null) {
            return environments.stream()
                    .filter(environment -> selector.equals(environment.getId()) || selector.equalsIgnoreCase(environment.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + selector));
        }
        return environments.stream().filter(Environment::isActive).findFirst().orElse(environments.get(0));
    }

    private static PerformanceRunExecutionResult runPlan(com.nvhuong.devworkbench.performance.core.run.PerformanceRunPlan plan,
                                                         String label,
                                                         PerformanceRunExecutionControl control) throws Exception {
        // Scripts in the plan may print; MCP stdout carries JSON-RPC only, so their output is discarded.
        PrintStream discard = new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8);
        return new PerformanceRunPlanExecutor().execute(plan, label, null, discard, control);
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
        return Map.of("type", "string", "description", description);
    }
}
