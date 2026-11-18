package com.nvhuong.devworkbench.performance.core.model;

import com.nvhuong.devworkbench.util.MessageKeys;

public enum PerformanceProtocol {
    HTTP(MessageKeys.PERFORMANCE_PROTOCOL_HTTP),
    WEBSOCKET(MessageKeys.PERFORMANCE_PROTOCOL_WEBSOCKET),
    SSE(MessageKeys.PERFORMANCE_PROTOCOL_SSE);

    private final String messageKey;

    PerformanceProtocol(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
