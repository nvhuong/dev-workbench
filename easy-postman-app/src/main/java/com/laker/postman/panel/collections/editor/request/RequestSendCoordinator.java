package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.execution.RequestPreparationResult;
import com.laker.postman.panel.collections.editor.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
final class RequestSendCoordinator {
    private final RequestExecutionState executionState;
    private final Runnable cancelCurrentRequestAction;
    private final JTextField urlField;
    private final RequestPreparationFeedbackPresenter requestPreparationFeedbackPresenter;
    private final RequestLinePanel requestLinePanel;
    private final ActionListener sendAction;
    private final ResponsePanel responsePanel;
    private final Supplier<RequestPreparationResult> preparationSupplier;
    private final Runnable updateUiForRequestingAction;
    private final Consumer<RequestPreparationResult> dispatchAction;

    void sendRequest() {
        if (executionState.currentWorker() != null) {
            cancelCurrentRequestAction.run();
            return;
        }

        requestPreparationFeedbackPresenter.clearUrlValidationFeedback(urlField);
        // Cắt ngay sau khi nhấn gửi“Yêu cầu”trạng thái để duy trì phản hồi tức thì phù hợp với yêu cầu mạng thực tế.
        updateUiForRequestingAction.run();

        SwingWorker<Void, Void> preparationWorker = new SwingWorker<>() {
            RequestPreparationResult preparationResult;

            @Override
            protected Void doInBackground() {
                // Giai đoạn tiền xử lý chỉ chịu trách nhiệm chuẩn bị dữ liệu yêu cầu và không liên quan trực tiếp đến việc thực thi giao thức, điều này tạo điều kiện thuận lợi cho sự phát triển logic xác minh độc lập tiếp theo.
                preparationResult = preparationSupplier.get();
                return null;
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    executionState.clearCurrentWorkerIf(this);
                    return;
                }

                // tiền xử lý worker Nó chỉ chịu trách nhiệm chuẩn bị dữ liệu; trước khi vào phân phối kết quả, trước tiên hãy đặt dòng điện worker Trở về máy trạng thái chính.
                executionState.clearCurrentWorkerIf(this);

                if (requestPreparationFeedbackPresenter.handlePreparationFailure(
                        preparationResult, urlField, requestLinePanel, sendAction, responsePanel)) {
                    return;
                }

                requestPreparationFeedbackPresenter.showPreparationWarningIfNeeded(preparationResult);
                if (!requestPreparationFeedbackPresenter.confirmContinuationIfNeeded(
                        preparationResult, requestLinePanel, sendAction, responsePanel)) {
                    return;
                }
                dispatchAction.accept(preparationResult);
            }
        };

        executionState.startWorker(preparationWorker);
        preparationWorker.execute();
    }
}
