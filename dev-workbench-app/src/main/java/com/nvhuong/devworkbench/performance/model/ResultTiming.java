package com.nvhuong.devworkbench.performance.model;

import com.nvhuong.devworkbench.http.runtime.model.HttpEventInfo;
import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;

/**
 * Bảng kết quả JMeter Thời gian kiểu: thời gian bắt đầu, byte đầu tiên (Latency）và thời gian kết nối (Connect）。
 * <p>
 * Cả byte đầu tiên và kết nối đều được tính từ khi bắt đầu yêu cầu (bắt đầu xếp hàng), trên cùng cơ sở với thời lượng;
 * Các kết nối bao gồm xếp hàng,DNS、TCP Và TLS，Đóng khi kết nối ghép kênh 0。Được sử dụng cho các giá trị không xác định -1 bày tỏ.
 */
public record ResultTiming(long startTimeMs, long latencyMs, long connectMs) {

    public static final long UNKNOWN = -1L;
    public static final ResultTiming EMPTY = new ResultTiming(UNKNOWN, UNKNOWN, UNKNOWN);

    public static ResultTiming of(long startTimeMs, long latencyMs, long connectMs) {
        return new ResultTiming(positiveOrUnknown(startTimeMs), nonNegativeOrUnknown(latencyMs),
                nonNegativeOrUnknown(connectMs));
    }

    public static ResultTiming fromResponse(HttpResponse response, long fallbackStartTimeMs) {
        if (response == null) {
            return of(fallbackStartTimeMs, UNKNOWN, UNKNOWN);
        }
        HttpEventInfo info = response.httpEventInfo;
        long base = info == null ? 0L : info.getQueueStart() > 0 ? info.getQueueStart() : info.getCallStart();
        long startTimeMs = base > 0 ? base : fallbackStartTimeMs;
        if (startTimeMs <= 0 && response.endTime > 0 && response.costMs >= 0) {
            startTimeMs = response.endTime - response.costMs;
        }
        if (base <= 0) {
            return of(startTimeMs, UNKNOWN, UNKNOWN);
        }
        long latencyMs = elapsed(base, info.getResponseHeadersStart());
        long connectMs = elapsed(base, info.getRequestHeadersStart());
        // Sử dụng đồng hồ đơn điệu cho các sự kiện tốn thời gian và đồng hồ treo tường cho các sự kiện. Việc làm tròn có thể khác nhau 1ms；Giữ JMeter Semantics: connection ≤ Byte đầu tiên ≤ Tốn thời gian.
        if (latencyMs >= 0 && response.costMs > 0) {
            latencyMs = Math.min(latencyMs, response.costMs);
        }
        if (connectMs >= 0 && latencyMs >= 0) {
            connectMs = Math.min(connectMs, latencyMs);
        }
        return of(startTimeMs, latencyMs, connectMs);
    }

    private static long elapsed(long base, long timestamp) {
        return timestamp > 0 && timestamp >= base ? timestamp - base : UNKNOWN;
    }

    private static long positiveOrUnknown(long value) {
        return value > 0 ? value : UNKNOWN;
    }

    private static long nonNegativeOrUnknown(long value) {
        return value >= 0 ? value : UNKNOWN;
    }
}
