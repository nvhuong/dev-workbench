package com.nvhuong.devworkbench.http.runtime.observation;

/**
 * Cổng đầu ra nhật ký mạng.
 * <p>
 * HTTP Lớp thực thi chỉ dựa vào cổng này để xuất bản các sự kiện và không truy cập trực tiếp vào nó Swing bảng điều khiển;GUI、Functional、Performance
 * Các cách triển khai khác nhau có thể được chuyển theo các kịch bản để tránh sự phụ thuộc ngược vào mã truyền cơ bản UI。
 */
@FunctionalInterface
public interface NetworkLogSink {
    NetworkLogSink NOOP = event -> {
    };

    void append(NetworkLogEvent event);

    static NetworkLogSink noop() {
        return NOOP;
    }

    default void append(NetworkLogEventStage stage, String message, Long elapsedMs) {
        append(new NetworkLogEvent(stage, message, elapsedMs));
    }

    default void append(NetworkLogEventStage stage, String message, Long elapsedMs, Long durationMs) {
        append(new NetworkLogEvent(stage, message, elapsedMs, durationMs));
    }
}
