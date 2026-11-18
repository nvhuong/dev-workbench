package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;

interface WebSocketConnectionUi {
    void updateUIForResponse(HttpResponse resp);

    void resetSendButton();

    void switchSendButtonToClose();

    void setWebSocketConnected(boolean connected);

    void activateWebSocketBodyTab();
}
