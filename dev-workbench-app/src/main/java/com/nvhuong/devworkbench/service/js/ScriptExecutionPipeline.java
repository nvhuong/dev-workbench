package com.nvhuong.devworkbench.service.js;

import cn.hutool.core.text.CharSequenceUtil;
import com.nvhuong.devworkbench.http.request.PreparedRequestFactory;
import com.nvhuong.devworkbench.http.request.PreparedRequestFinalizer;
import com.nvhuong.devworkbench.http.runtime.mapper.PreparedRequestMapper;
import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.model.Environment;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.service.EnvironmentService;
import com.nvhuong.devworkbench.service.js.api.DevWorkbenchApiContext;
import com.nvhuong.devworkbench.service.variable.ExecutionContextScope;
import com.nvhuong.devworkbench.service.variable.ExecutionVariableContext;
import com.nvhuong.devworkbench.service.variable.RequestExecutionContext;
import com.nvhuong.devworkbench.service.variable.RequestExecutionScope;
import com.nvhuong.devworkbench.service.variable.RunScopedVariableContext;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Đường dẫn thực thi tập lệnh
 * Cung cấp quản lý vòng đời thực thi tập lệnh hoàn chỉnh, bao gồm tiền tập lệnh, hậu tập lệnh, thu thập kết quả kiểm tra, v.v
 * <p>
 * Ví dụ sử dụng:
 * <pre>{@code
 * ScriptExecutionResult result = ScriptExecutionPipeline.builder()
 *     .request(req)
 *     .preScript(item.getPrescript())
 *     .postScript(item.getPostscript())
 *     .build()
 *     .executePreScript()
 *     .executePostScript(response);
 * }</pre>
 */
@Slf4j
@Builder
@Getter
public class ScriptExecutionPipeline {
    public static ScriptExecutionPipeline forRequestExecution(HttpRequestItem item,
                                                              PreparedRequest request,
                                                              ExecutionVariableContext sharedExecutionContext) {
        return forRequestExecution(item, request, sharedExecutionContext, null);
    }

    public static ScriptExecutionPipeline forRequestExecution(HttpRequestItem item,
                                                              PreparedRequest request,
                                                              ExecutionVariableContext sharedExecutionContext,
                                                              JsScriptExecutor.OutputCallback outputCallback) {
        return forRequestExecution(item, request, sharedExecutionContext, outputCallback, null);
    }

    public static ScriptExecutionPipeline forRequestExecution(HttpRequestItem item,
                                                              PreparedRequest request,
                                                              ExecutionVariableContext sharedExecutionContext,
                                                              JsScriptExecutor.OutputCallback outputCallback,
                                                              Supplier<Environment> environmentSupplier) {
        return forRequestExecution(
                item,
                request,
                sharedExecutionContext,
                outputCallback,
                environmentSupplier,
                RequestExecutionContext.captureCurrentScope()
        );
    }

    public static ScriptExecutionPipeline forRequestExecution(HttpRequestItem item,
                                                              PreparedRequest request,
                                                              ExecutionVariableContext sharedExecutionContext,
                                                              JsScriptExecutor.OutputCallback outputCallback,
                                                              Supplier<Environment> environmentSupplier,
                                                              RequestExecutionScope requestExecutionScope) {
        return forRequestExecution(
                request,
                sharedExecutionContext,
                outputCallback,
                environmentSupplier,
                requestExecutionScope,
                PreparedRequestFactory.resolveDeferredAuthorization(item),
                false
        );
    }

    /**
     * Creates a pipeline for a request whose collection inheritance has already been applied.
     * Headless collection execution uses this path so it does not depend on the active Swing collection tree.
     */
    public static ScriptExecutionPipeline forEffectiveRequestExecution(HttpRequestItem effectiveItem,
                                                                       PreparedRequest request,
                                                                       ExecutionVariableContext sharedExecutionContext,
                                                                       JsScriptExecutor.OutputCallback outputCallback,
                                                                       Supplier<Environment> environmentSupplier,
                                                                       RequestExecutionScope requestExecutionScope) {
        return forRequestExecution(
                request,
                sharedExecutionContext,
                outputCallback,
                environmentSupplier,
                requestExecutionScope,
                PreparedRequestFactory.resolveDeferredAuthorizationWithoutInheritance(effectiveItem),
                true
        );
    }

    private static ScriptExecutionPipeline forRequestExecution(PreparedRequest request,
                                                               ExecutionVariableContext sharedExecutionContext,
                                                               JsScriptExecutor.OutputCallback outputCallback,
                                                               Supplier<Environment> environmentSupplier,
                                                               RequestExecutionScope requestExecutionScope,
                                                               PreparedRequestMapper.DeferredAuthorization deferredAuthorization,
                                                               boolean includeIterationDataInPmVariables) {
        return ScriptExecutionPipeline.builder()
                .request(request)
                .preScript(request.prescript)
                .postScript(request.postscript)
                .sharedExecutionContext(sharedExecutionContext)
                .requestExecutionScope(requestExecutionScope != null
                        ? requestExecutionScope
                        : RequestExecutionContext.captureCurrentScope())
                .deferredAuthorization(deferredAuthorization)
                .includeIterationDataInPmVariables(includeIterationDataInPmVariables)
                .outputCallback(outputCallback)
                .environmentSupplier(environmentSupplier)
                .build();
    }

    /**
     * Đối tượng yêu cầu đã chuẩn bị
     */
    private final PreparedRequest request;

    /**
     * Nội dung có sẵn kịch bản
     */
    private final String preScript;

    /**
     * Nội dung phần tái bút
     */
    private final String postScript;

    /**
     * Liên kết biến (tùy chọn, được tạo tự động nếu không được cung cấp)
     */
    private Map<String, Object> bindings;

    /**
     * Có hiển thị hộp thoại lỗi khi tập lệnh trước bị lỗi hay không
     */
    @Builder.Default
    private final boolean showPreScriptErrorDialog = true;

    /**
     * Gọi lại đầu ra (tùy chọn)
     */
    private final JsScriptExecutor.OutputCallback outputCallback;

    /**
     * Nhà cung cấp biến môi trường tùy chọn.Headless / cụm worker Có thể ép được run Tiêm môi trường;
     * GUI Vẫn được sử dụng khi không được thông qua theo mặc định EnvironmentService của active environment。
     */
    private final Supplier<Environment> environmentSupplier;

    /**
     * Người thực thi kịch bản. Kiểm tra căng thẳng hiệu suất sẽ được tiêm run-scoped executor，bình thường GUI Yêu cầu sử dụng toàn cầu theo mặc định executor。
     */
    private final JsScriptExecutor.ScriptExecutor scriptExecutor;

    /**
     * Phạm vi thay đổi của yêu cầu mà tập lệnh hiện tại thuộc về sẽ được sử dụng headless / GUI Độ phân giải biến nhóm được chia sẻ.
     */
    private final RequestExecutionScope requestExecutionScope;

    /**
     * Opt-in DevWorkbench runner lookup semantics for headless collection execution.
     * Existing Functional and Performance pipeline builders keep the default false value.
     */
    private final boolean includeIterationDataInPmVariables;

    /**
     * Bối cảnh được chia sẻ tùy chọn.Functional / Performance Loại điều này“Nhiều yêu cầu trong cùng một vòng”
     * Cảnh sẽ chuyển nó một cách rõ ràng để tránh dựa vào dư lượng luồng để chia sẻ pm.variables。
     */
    private final ExecutionVariableContext sharedExecutionContext;

    /**
     * Trì hoãn việc tạo cấu hình xác thực tự động cho đến khi việc gắn bối cảnh được thực thi.
     * Bằng cách này, các biến được chia sẻ có thể được sử dụng trong cùng một vòng Basic/Bearer Chứng nhận, đồng thời pre-script Vẫn có thể xóa được/Ghi đè tiêu đề yêu cầu này.
     */
    private PreparedRequestMapper.DeferredAuthorization deferredAuthorization;

    private transient ExecutionVariableContext runtimeExecutionContext;

    /**
     * Thực hiện kịch bản trước
     *
     * @return Kết quả thực hiện
     */
    public ScriptExecutionResult executePreScript() {
        return executePreScript(false);
    }

    /**
     * Executes a pre-request script and preserves pm.test results for collection runners.
     * The existing executePreScript method keeps its historical result behavior.
     */
    public ScriptExecutionResult executePreScriptWithTests() {
        return executePreScript(true);
    }

    private ScriptExecutionResult executePreScript(boolean captureTestResults) {
        return withExecutionContext(() -> {
            if (CharSequenceUtil.isBlank(preScript)) {
                return ScriptExecutionResult.success();
            }
            if (bindings == null) {
                bindings = preparePreRequestBindings(request);
            }

            DevWorkbenchApiContext pm = (DevWorkbenchApiContext) bindings.get("pm");
            if (pm != null && pm.info != null) {
                pm.info.eventName = "prerequest";
            }
            clearTestResults();

            try {
                ScriptExecutionContext context = ScriptExecutionContext.builder()
                        .script(preScript)
                        .scriptType(ScriptExecutionContext.ScriptType.PRE_REQUEST)
                        .bindings(bindings)
                        .outputCallback(getEffectiveOutputCallback("[PreScript Console]\n"))
                        .build();

                executeScript(context);
                syncPreRequestChanges(pm);
                return captureTestResults && pm != null && pm.testResults != null
                        ? ScriptExecutionResult.success(pm.testResults)
                        : ScriptExecutionResult.success();

            } catch (ScriptExecutionException ex) {
                log.error("Pre-script execution failed: {}", ex.getMessage(), ex);
                appendScriptOutput("[PreScript Error]\n" + ex.getMessage(), JsScriptExecutor.ConsoleType.ERROR);
                return captureTestResults && pm != null && pm.testResults != null
                        ? ScriptExecutionResult.failure(ex.getMessage(), ex, pm.testResults)
                        : ScriptExecutionResult.failure(ex.getMessage(), ex);
            }
        });
    }

    /**
     * Thực hiện phần tái bút
     *
     * @param response HTTP đối tượng phản hồi
     * @return Kết quả thực hiện (bao gồm cả kết quả kiểm tra)
     */
    public ScriptExecutionResult executePostScript(HttpResponse response) {
        return withExecutionContext(() -> {
            if (bindings == null) {
                bindings = preparePreRequestBindings(request);
            }

            addResponseBindings(bindings, response);
            DevWorkbenchApiContext pm = (DevWorkbenchApiContext) bindings.get("pm");
            if (pm != null && pm.info != null) {
                pm.info.eventName = "test";
            }
            clearTestResults();

            try {
                ScriptExecutionContext context = ScriptExecutionContext.builder()
                        .script(postScript)
                        .scriptType(ScriptExecutionContext.ScriptType.POST_REQUEST)
                        .bindings(bindings)
                        .outputCallback(getEffectiveOutputCallback("[PostScript Console]\n"))
                        .build();

                executeScript(context);

                if (pm != null && pm.testResults != null) {
                    return ScriptExecutionResult.success(pm.testResults);
                }
                return ScriptExecutionResult.success();

            } catch (ScriptExecutionException ex) {
                log.error("Post-script execution failed: {}", ex.getMessage(), ex);
                appendScriptOutput("[PostScript Error]\n" + ex.getMessage(), JsScriptExecutor.ConsoleType.ERROR);

                if (pm != null && pm.testResults != null) {
                    return ScriptExecutionResult.failure(ex.getMessage(), ex, pm.testResults);
                }
                return ScriptExecutionResult.failure(ex.getMessage(), ex);
            }
        });
    }

    public ScriptExecutionResult executeWebSocketSendScript(String script,
                                                            int sendIndex,
                                                            int sendCount,
                                                            String stepName) {
        return withExecutionContext(() -> {
            if (bindings == null) {
                bindings = preparePreRequestBindings(request);
            }

            DevWorkbenchApiContext pm = (DevWorkbenchApiContext) bindings.get("pm");
            if (pm != null && pm.info != null) {
                pm.info.eventName = "websocket_send";
                pm.info.setWebSocketSendInfo(sendIndex, sendCount, stepName);
            }
            clearTestResults();

            try {
                ScriptExecutionContext context = ScriptExecutionContext.builder()
                        .script(script)
                        .scriptType(ScriptExecutionContext.ScriptType.CUSTOM)
                        .bindings(bindings)
                        .outputCallback(getEffectiveOutputCallback("[WebSocket Send Script Console]\n"))
                        .build();

                executeScript(context);
                boolean requestBodyMutated = syncPreRequestChanges(pm);
                return ScriptExecutionResult.builder()
                        .success(true)
                        .requestBodyMutated(requestBodyMutated)
                        .build();
            } catch (ScriptExecutionException ex) {
                log.error("WebSocket send script execution failed: {}", ex.getMessage(), ex);
                appendScriptOutput("[WebSocket Send Script Error]\n" + ex.getMessage(), JsScriptExecutor.ConsoleType.ERROR);
                return ScriptExecutionResult.failure(ex.getMessage(), ex);
            }
        });
    }

    /**
     * Quá trình thực hiện hoàn chỉnh: pre-script -> post-script
     * Nếu tập lệnh trước không thành công, tập lệnh sau sẽ không được thực thi
     *
     * @param response HTTP đối tượng phản hồi
     * @return Kết quả thực hiện của phần tái bút
     */
    public ScriptExecutionResult executeFullPipeline(HttpResponse response) {
        // Thực hiện kịch bản trước
        ScriptExecutionResult preResult = executePreScript();
        if (!preResult.isSuccess()) {
            return preResult;
        }

        // Thực hiện phần tái bút
        return executePostScript(response);
    }

    /**
     * Kết quả kiểm tra rõ ràng
     */
    private void clearTestResults() {
        if (bindings != null) {
            DevWorkbenchApiContext pm = (DevWorkbenchApiContext) bindings.get("pm");
            if (pm != null && pm.testResults != null) {
                pm.testResults.clear();
            }
        }
    }

    private boolean syncPreRequestChanges(DevWorkbenchApiContext pm) {
        if (pm != null && pm.request != null) {
            return pm.request.syncToRaw();
        }
        return false;
    }

    /**
     * Nhận một cuộc gọi lại đầu ra hợp lệ
     */
    private JsScriptExecutor.OutputCallback getEffectiveOutputCallback(String prefix) {
        if (outputCallback != null) {
            return new JsScriptExecutor.OutputCallback() {
                @Override
                public void onOutput(String output) {
                    outputCallback.onOutput(prefix + output);
                }

                @Override
                public void onOutput(String output, JsScriptExecutor.ConsoleType consoleType) {
                    outputCallback.onOutput(prefix + output, consoleType);
                }
            };
        }
        return output -> {
        };
    }

    private void appendScriptOutput(String output, JsScriptExecutor.ConsoleType consoleType) {
        if (outputCallback != null) {
            outputCallback.onOutput(output, consoleType);
        }
    }

    /**
     * Sau khi pre-script được thực thi thành công sẽ bước vào giai đoạn hoàn thiện cuối cùng.
     * Ở đây không còn việc xử lý rải rác việc thay thế biến nữa / Authorization Quy tắc ngón tay cái.
     * Nhưng hãy ủy thác tất cả PreparedRequestFinalizer，Đảm bảo rằng hành vi của mỗi cổng gửi là nhất quán.
     */
    public void finalizeRequest() {
        withExecutionContext(() -> PreparedRequestFinalizer.finalizeForSend(request, deferredAuthorization));
        deferredAuthorization = null;
    }

    public void withExecutionContext(Runnable action) {
        withExecutionContext(() -> {
            action.run();
            return null;
        });
    }

    public <T> T withExecutionContext(Supplier<T> action) {
        try (ExecutionContextScope ignored = ExecutionContextScope.open(getOrCreateExecutionContext(), requestExecutionScope)) {
            return action.get();
        }
    }

    public <T> T withExecutionContextThrowing(ThrowingSupplier<T> action) throws Exception {
        try (ExecutionContextScope ignored = ExecutionContextScope.open(getOrCreateExecutionContext(), requestExecutionScope)) {
            return action.get();
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private ExecutionVariableContext getOrCreateExecutionContext() {
        if (runtimeExecutionContext != null) {
            return runtimeExecutionContext;
        }
        runtimeExecutionContext = sharedExecutionContext != null
                ? sharedExecutionContext
                : new ExecutionVariableContext();
        return runtimeExecutionContext;
    }

    /**
     * Thực thi tập lệnh (phương thức chung)
     *
     * @param context Bối cảnh thực thi tập lệnh
     * @throws ScriptExecutionException Ngoại lệ thực thi tập lệnh
     */
    private void executeScript(ScriptExecutionContext context) throws ScriptExecutionException {
        if (context == null || CharSequenceUtil.isBlank(context.getScript())) {
            return;
        }
        JsScriptExecutor.ScriptExecutor executor = scriptExecutor == null
                ? JsScriptExecutor::executeScript
                : scriptExecutor;
        executor.execute(context);
    }


    /**
     * Chuẩn bị ràng buộc biến cho tập lệnh trước
     * Chỉ bị lộ thôi pm context và khởi tạo một đối tượng phản hồi trống.
     *
     * @param req Đối tượng yêu cầu đã chuẩn bị
     * @return Ràng buộc biến đổi Map
     */
    private Map<String, Object> preparePreRequestBindings(PreparedRequest req) {
        Environment activeEnv = resolveActiveEnvironment();
        DevWorkbenchApiContext devworkbench = createDevWorkbenchApiContext(activeEnv);
        devworkbench.setRequest(req);

        Map<String, Object> bindings = new java.util.LinkedHashMap<>();
        bindings.put("pm", devworkbench);
        bindings.put("devworkbench", devworkbench);
        // Legacy Postman API object (postman.setEnvironmentVariable...) used by imported Postman scripts.
        bindings.put("postman", devworkbench);
        bindings.put("request", req);
        bindings.put("env", activeEnv);
        bindings.put("globals", devworkbench.globals);
        bindings.put("iterationData", devworkbench.iterationData);

        // Cung cấp một đối tượng phản hồi trống cho tập lệnh trước để ngăn chặn pm.response vì undefined
        try {
            HttpResponse emptyResponse = new HttpResponse();
            emptyResponse.code = 0;
            emptyResponse.headers = new java.util.LinkedHashMap<>();
            emptyResponse.body = "{}";
            devworkbench.setResponse(emptyResponse);

            bindings.put("response", emptyResponse);
            bindings.put("responseBody", emptyResponse.body);
            bindings.put("responseHeaders", emptyResponse.headers);
            bindings.put("statusCode", emptyResponse.code);
        } catch (Exception e) {
            log.warn("Failed to initialize empty response object for pre-request script", e);
        }

        return bindings;
    }

    private DevWorkbenchApiContext createDevWorkbenchApiContext(Environment activeEnv) {
        if (environmentSupplier != null) {
            Environment scopedGlobals = RunScopedVariableContext.currentGlobals();
            return includeIterationDataInPmVariables
                    ? DevWorkbenchApiContext.scopedWithIterationData(activeEnv, scopedGlobals)
                    : DevWorkbenchApiContext.scoped(activeEnv, scopedGlobals);
        }
        return new DevWorkbenchApiContext(activeEnv);
    }

    private Environment resolveActiveEnvironment() {
        if (environmentSupplier != null) {
            return environmentSupplier.get();
        }
        return EnvironmentService.getActiveEnvironment();
    }

    /**
     * Thêm các ràng buộc biến liên quan đến phản hồi vào các tập lệnh sau
     * ở hiện tại bindings Thêm dữ liệu phản hồi dựa trên điều này
     *
     * @param bindings Ràng buộc biến hiện có
     * @param response HTTP đối tượng phản hồi
     */
    private static void addResponseBindings(Map<String, Object> bindings, HttpResponse response) {
        if (bindings == null || response == null) {
            return;
        }

        DevWorkbenchApiContext pm = (DevWorkbenchApiContext) bindings.get("pm");
        if (pm != null) {
            pm.setResponse(response);
        }

        bindings.put("response", response);
        bindings.put("responseBody", response.body);
        bindings.put("responseHeaders", response.headers);
        bindings.put("statusCode", response.code);
    }
}
