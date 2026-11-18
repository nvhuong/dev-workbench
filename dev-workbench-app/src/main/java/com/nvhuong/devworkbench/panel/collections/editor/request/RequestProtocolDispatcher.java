package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.http.execution.RequestPreparationResult;
import com.nvhuong.devworkbench.http.execution.RequestPreparationNetworkLogPublisher;
import com.nvhuong.devworkbench.http.runtime.model.HttpCaptureProfile;
import com.nvhuong.devworkbench.http.runtime.model.HttpCaptureProfiles;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.http.runtime.ssl.SSLConfigurationUtil;
import com.nvhuong.devworkbench.panel.collections.editor.request.sub.ResponsePanel;
import com.nvhuong.devworkbench.panel.http.runtime.SwingHttpRuntimeInteractionAdapter;
import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.service.js.ScriptExecutionPipeline;
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
