package com.nvhuong.devworkbench.http.execution;

import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.http.request.HttpRequestValidationResult;
import com.nvhuong.devworkbench.http.request.HttpRequestValidator;
import com.nvhuong.devworkbench.http.request.PreparedRequestFactory;
import com.nvhuong.devworkbench.service.js.JsScriptExecutor;
import com.nvhuong.devworkbench.service.js.ScriptExecutionPipeline;
import com.nvhuong.devworkbench.service.js.ScriptExecutionResult;
import com.nvhuong.devworkbench.service.variable.ExecutionVariableContext;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class RequestPreparationService {

    private final JsScriptExecutor.OutputCallback scriptOutputCallback;

    public RequestPreparationResult prepare(HttpRequestItem item) {
        try {
            HttpRequestValidationResult protocolValidation = validateProtocol(item);
            if (!protocolValidation.isValid()) {
                return RequestPreparationResult.validationFailure(protocolValidation);
            }

            // Đặt nó ở đây“Trạng thái bảng điều khiển”Chuyển đổi thành một cái gì đó thực sự có thể gửi được PreparedRequest，Theo dõi helper Họ chỉ tiêu thụ đối tượng này.
            PreparedRequest request = PreparedRequestFactory.build(item);
            ScriptExecutionPipeline pipeline = createScriptPipeline(item, request);

            String preScriptError = executePreScript(pipeline);
            if (preScriptError != null) {
                return RequestPreparationResult.error(preScriptError);
            }

            // Pre-script có thể ghi các biến, do đó phải thay thế biến pre-script Làm lại sau.
            pipeline.finalizeRequest();

            HttpRequestValidationResult validationResult = HttpRequestValidator.validate(request, item);
            if (!validationResult.isValid()) {
                return RequestPreparationResult.validationFailure(validationResult);
            }

            return RequestPreparationResult.success(item, request, pipeline, validationResult);
        } catch (Exception ex) {
            log.error("Error preparing request: {}", ex.getMessage(), ex);
            return RequestPreparationResult.error(ex.getMessage());
        }
    }

    private HttpRequestValidationResult validateProtocol(HttpRequestItem item) {
        String url = item.getUrl();
        if (item.getProtocol().isWebSocketProtocol()
                && !url.toLowerCase().startsWith("ws://")
                && !url.toLowerCase().startsWith("wss://")) {
            return HttpRequestValidationResult.error(
                    I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_WEBSOCKET_PROTOCOL),
                    true
            );
        }
        return HttpRequestValidationResult.ok();
    }

    private ScriptExecutionPipeline createScriptPipeline(HttpRequestItem item, PreparedRequest request) {
        return ScriptExecutionPipeline.forRequestExecution(
                item,
                request,
                new ExecutionVariableContext(),
                scriptOutputCallback
        );
    }

    private String executePreScript(ScriptExecutionPipeline pipeline) {
        ScriptExecutionResult preResult = pipeline.executePreScript();
        if (preResult.isSuccess()) {
            return null;
        }
        return I18nUtil.getMessage(MessageKeys.SCRIPT_PRESCRIPT_EXECUTION_FAILED,
                preResult.getErrorMessage());
    }
}
