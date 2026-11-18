package com.nvhuong.devworkbench.performance.execution;


import com.nvhuong.devworkbench.http.runtime.transport.HttpBaseClientProvider;
import com.nvhuong.devworkbench.http.runtime.transport.HttpCallTracker;
import com.nvhuong.devworkbench.http.runtime.transport.RealtimeConnectionHandle;
import com.nvhuong.devworkbench.http.runtime.transport.RealtimeWebSocketConnection;

import java.util.Set;

public interface PerformanceNetworkRuntime extends HttpCallTracker, HttpBaseClientProvider {
    default void beginRun() {
    }

    Set<RealtimeConnectionHandle> activeSseSources();

    Set<RealtimeWebSocketConnection> activeWebSockets();

    int activeHttpCallCount();

    int activeSseCount();

    int activeWebSocketCount();

    void cancelAll();

    default void endRun() {
    }
}
