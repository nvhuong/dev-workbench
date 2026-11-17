package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.execution.RequestPreparationResult;
import com.laker.postman.http.execution.RequestPreparationNetworkLogPublisher;
import com.laker.postman.http.runtime.model.HttpCaptureProfile;
import com.laker.postman.http.runtime.model.HttpCaptureProfiles;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.ssl.SSLConfigurationUtil;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.panel.http.runtime.SwingHttpRuntimeInteractionAdapter;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class RequestProtocolDispatcher {
    private final ResponsePanel responsePanel;
    private final HttpRequestExecutor httpRequestExecutor;
    private final WebSocketRequestExecutor webSocketRequestExecutor;
    private final RequestExecutionState executionState;
    private final int maxRedirectCount;

    void dispatch(RequestPreparationResult result) {
        RequestItemProtocolEnum protocol = result.getItem().getProtocol();
        PreparedRequest request = result.getRequest();
        attachNetworkLogSink(request);
        attachHttpResponseInteraction(request);
        RequestPreparationNetworkLogPublisher.publish(request);
        ScriptExecutionPipeline pipeline = result.getPipeline();
        SwingWorker<Void, Void> worker;
        // Đây là lối thoát duy nhất để phân phối giao thức: .SSE như HTTP Mẫu phản hồi bao gồm HTTP Người thực thi xử lý.
        if (protocol.isWebSocketProtocol()) {
            worker = webSocketRequestExecutor.createWorker(request, pipeline);
        } else {
            executionState.clearAutoDetectedHttpSseOpen();
            worker = httpRequestExecutor.createWorker(request, pipeline, maxRedirectCount);
        }

        executionState.startWorker(worker);
        worker.execute();
    }

    private void attachNetworkLogSink(PreparedRequest request) {
        if (request == null) {
            return;
        }
        // HTTP Lớp thực thi chỉ xuất bản NetworkLogEvent；The request editor receives the event back to the current response panel here.
        HttpCaptureProfiles.apply(request, HttpCaptureProfile.COLLECTION_DIAGNOSTIC);
        request.networkLogSink = event -> responsePanel.getNetworkLogPanel().appendLog(event);
        request.lifecycleLogSink = SwingHttpRuntimeInteractionAdapter.lifecycleLogSink();
    }

    private void attachHttpResponseInteraction(PreparedRequest request) {
        if (request == null) {
            return;
        }
        // HTTP Lớp thực thi chỉ phụ thuộc vào UI-neutral sink；Swing The request editor hooks up specific interactions here.
        request.downloadProgressSinkFactory = SwingHttpRuntimeInteractionAdapter.downloadProgressSinkFactory();
        request.responseSizeLimitWarningSink = SwingHttpRuntimeInteractionAdapter.responseSizeLimitWarningSink();
        SSLConfigurationUtil.setLifecycleLogSink(request.lifecycleLogSink);
    }
}
