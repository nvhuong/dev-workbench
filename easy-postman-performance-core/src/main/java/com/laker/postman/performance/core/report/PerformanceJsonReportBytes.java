package com.laker.postman.performance.core.report;

import lombok.Builder;
import lombok.Value;

/**
 * trong báo cáo HTTP Chỉ báo byte.
 */
@Value
public class PerformanceJsonReportBytes {
    // Tổng số byte đã gửi: tiêu đề yêu cầu + Request body
    long sentBytes;
    // Tổng số byte nhận được: tiêu đề phản hồi + cơ quan phản ứng
    long receivedBytes;
    // Tỷ lệ gửi:sentBytes / sample Cửa sổ thời gian có thể được chuyển đổi sang khi hiển thị KB/s
    double sentBytesPerSecond;
    // Tỷ lệ nhận:receivedBytes / sample Cửa sổ thời gian có thể được chuyển đổi sang khi hiển thị KB/s
    double receivedBytesPerSecond;
    // Số byte trung bình nhận được:receivedBytes / total，Căn chỉnh JMeter Avg. Bytes
    long avgReceivedBytes;

    @Builder
    public PerformanceJsonReportBytes(Long sentBytes,
                                      Long receivedBytes,
                                      Double sentBytesPerSecond,
                                      Double receivedBytesPerSecond,
                                      Long avgReceivedBytes) {
        this.sentBytes = Math.max(0L, sentBytes == null ? 0L : sentBytes);
        this.receivedBytes = Math.max(0L, receivedBytes == null ? 0L : receivedBytes);
        this.sentBytesPerSecond = finite(sentBytesPerSecond);
        this.receivedBytesPerSecond = finite(receivedBytesPerSecond);
        this.avgReceivedBytes = Math.max(0L, avgReceivedBytes == null ? 0L : avgReceivedBytes);
    }

    private static double finite(Double value) {
        return value == null || !Double.isFinite(value) ? 0D : value;
    }
}
