package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.function.BooleanSupplier;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class RequestExecutionUiUpdater implements WebSocketConnectionUi {
    private final ResponsePanel responsePanel;
    private final RequestLinePanel requestLinePanel;
    private final RequestBodyPanel requestBodyPanel;
    private final JTabbedPane reqTabs;
    private final ActionListener sendAction;
    private final BooleanSupplier baseHttpProtocol;
    private final BooleanSupplier effectiveHttpProtocol;
    private final BooleanSupplier effectiveWebSocketProtocol;

    void updateUIForRequesting() {
        requestLinePanel.setSendButtonToCancel(sendAction);
        responsePanel.clearInFlightRequestDetails();
        boolean keepResponseTabsEnabled = baseHttpProtocol.getAsBoolean()
                && !effectiveWebSocketProtocol.getAsBoolean()
                && responsePanel.hasResponseData();
        if (!keepResponseTabsEnabled) {
            responsePanel.setResponseTabButtonsEnable(false);
        }
        if (baseHttpProtocol.getAsBoolean()) {
            responsePanel.setResponseBodyEnabled(false);
        }
        // bình thường HTTP Giữ nguyên nội dung phản hồi trước đó khi yêu cầu được gửi để tránh hiện tượng nhấp nháy do tab phản hồi hiện tại bị xóa trước rồi mới vẽ lại.
        // Giao thức truyền phát vẫn sử dụng chiến lược thanh toán bù trừ để ngăn các tin nhắn mới và cũ bị trộn lẫn với nhau.
        if (!baseHttpProtocol.getAsBoolean() || effectiveWebSocketProtocol.getAsBoolean()) {
            responsePanel.clearAll();
        }
        responsePanel.showLoadingOverlay();
    }

    public void updateUIForResponse(HttpResponse resp) {
        responsePanel.hideLoadingOverlay();

        if (resp == null) {
            responsePanel.clearAll();
            responsePanel.setStatus(0);
            if (baseHttpProtocol.getAsBoolean()) {
                responsePanel.setResponseBodyEnabled(effectiveHttpProtocol.getAsBoolean());
            }
            return;
        }

        responsePanel.setResponseHeaders(resp);
        if (!effectiveWebSocketProtocol.getAsBoolean()) {
            responsePanel.setTiming(resp);
        }
        if (effectiveHttpProtocol.getAsBoolean() && !resp.isSse) {
            responsePanel.setResponseBody(resp);
            responsePanel.setResponseBodyEnabled(true);
        } else if (baseHttpProtocol.getAsBoolean()) {
            responsePanel.setResponseBodyEnabled(false);
        }
        responsePanel.setStatus(resp.code);
        responsePanel.setResponseTime(resp.costMs);
        responsePanel.setResponseSize(resp.bodySize, resp);
        responsePanel.markResponseDataLoaded();
    }

    public void resetSendButton() {
        requestLinePanel.setSendButtonToSend(sendAction);
    }

    public void switchSendButtonToClose() {
        requestLinePanel.setSendButtonToClose(sendAction);
    }

    public void setWebSocketConnected(boolean connected) {
        requestBodyPanel.setWebSocketConnected(connected);
    }

    public void activateWebSocketBodyTab() {
        RequestTabSelector.selectFirstVisible(reqTabs, requestBodyPanel);
        requestBodyPanel.getWsSendButton().requestFocusInWindow();
    }
}
