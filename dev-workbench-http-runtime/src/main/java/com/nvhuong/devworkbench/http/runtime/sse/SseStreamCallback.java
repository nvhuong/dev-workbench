package com.nvhuong.devworkbench.http.runtime.sse;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;

public interface SseStreamCallback {
    void onOpen(HttpResponse resp, String headersText);

    void onEvent(String id, String type, String data);

    default void onRetryChange(long retryMs) {
    }

    void onClosed(HttpResponse resp);

    void onFailure(String errorMsg, HttpResponse resp);
}
