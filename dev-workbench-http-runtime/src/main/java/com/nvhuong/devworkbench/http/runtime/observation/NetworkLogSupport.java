package com.nvhuong.devworkbench.http.runtime.observation;

import com.nvhuong.devworkbench.http.runtime.model.HttpCaptureProfiles;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import lombok.experimental.UtilityClass;

/**
 * Lớp bảo vệ xuất bản nhật ký web.
 * <p>
 * Các yêu cầu thực sự không thể bị ảnh hưởng bởi bảng nhật ký và tất cả các giao thức đều được đánh giá, chuyển đổi và phân tích cú pháp tại đây sink Và nuốt ngoại lệ nhật ký.
 */
@UtilityClass
public class NetworkLogSupport {

    public static boolean isEnabled(PreparedRequest request) {
        return request != null && HttpCaptureProfiles.resolve(request).emitNetworkLog();
    }

    public static NetworkLogSink resolveSink(PreparedRequest request) {
        return request == null || request.networkLogSink == null
                ? NetworkLogSink.noop()
                : request.networkLogSink;
    }

    public static void append(PreparedRequest request, NetworkLogEventStage stage, String message) {
        append(request, stage, message, null);
    }

    public static void append(PreparedRequest request, NetworkLogEventStage stage, String message, Long elapsedMs) {
        append(request, stage, message, elapsedMs, null);
    }

    public static void append(PreparedRequest request,
                              NetworkLogEventStage stage,
                              String message,
                              Long elapsedMs,
                              Long durationMs) {
        if (!isEnabled(request)) {
            return;
        }
        try {
            resolveSink(request).append(stage, message, elapsedMs, durationMs);
        } catch (Throwable ignored) {
            // Nhật ký mạng là khả năng quan sát và các trường hợp ngoại lệ không thể ảnh hưởng đến các yêu cầu thực.SSE hoặc WebSocket。
        }
    }
}
