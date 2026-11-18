package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.http.execution.RequestPreparationResult;
import com.nvhuong.devworkbench.http.execution.RequestPreparationService;
import com.nvhuong.devworkbench.panel.sidebar.ConsoleScriptOutputAdapter;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * Chuẩn bị bộ điều khiển trước khi gửi yêu cầu.
 * <p>
 * Chỉ cần làm điều đó trước khi gửi“Hình thức sưu tầm + Đặt xác minh + Xây dựng yêu cầu + Thực hiện kịch bản trước”，Thỏa thuận thực tế được thực hiện bởi runtime controller Phân phối.
 */
@RequiredArgsConstructor
final class RequestEditorSendPreparationController {
    private final RequestPreparationService requestPreparationService;
    private final Runnable editorInitializer;
    private final Runnable transientTabPinAction;
    private final Supplier<String> settingsValidator;
    private final Supplier<HttpRequestItem> currentRequestSupplier;

    static RequestEditorSendPreparationController createDefault(Runnable editorInitializer,
                                                                Runnable transientTabPinAction,
                                                                Supplier<String> settingsValidator,
                                                                Supplier<HttpRequestItem> currentRequestSupplier) {
        return new RequestEditorSendPreparationController(
                new RequestPreparationService(ConsoleScriptOutputAdapter.outputCallback()),
                editorInitializer,
                transientTabPinAction,
                settingsValidator,
                currentRequestSupplier
        );
    }

    RequestPreparationResult prepareForSending() {
        editorInitializer.run();
        transientTabPinAction.run();
        String settingsValidationError = validateCurrentSettings();
        if (settingsValidationError != null) {
            return RequestPreparationResult.error(settingsValidationError);
        }
        HttpRequestItem item = currentRequestSupplier.get();
        return requestPreparationService.prepare(item);
    }

    String validateRequestSettings() {
        editorInitializer.run();
        return validateCurrentSettings();
    }

    private String validateCurrentSettings() {
        return settingsValidator.get();
    }
}
