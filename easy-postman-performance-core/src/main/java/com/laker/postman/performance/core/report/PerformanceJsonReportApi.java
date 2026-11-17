package com.laker.postman.performance.core.report;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceJsonReportApi {
    String apiId;
    String name;
    String protocol;
    long total;
    long success;
    long failed;
    double successRate;
    // QPS：Số lượng yêu cầu /（Thời gian kết thúc mẫu cuối cùng - thời gian bắt đầu mẫu đầu tiên)
    double samplesPerSecond;
    // Thời gian bắt đầu mẫu đầu tiên,master Hợp nhất nhiều hơn worker Sử dụng nó để xác định ranh giới bắt đầu toàn cầu
    long firstSampleStartTimeMs;
    // Thời gian kết thúc mẫu cuối cùng,master Hợp nhất nhiều hơn worker Sử dụng nó để xác định ranh giới cuối cùng toàn cầu
    long lastSampleEndTimeMs;
    PerformanceJsonReportDuration durationMs;
    // HTTP Số liệu byte: Đã gửi/Tổng số byte nhận được, tốc độ byte, số byte trung bình nhận được
    PerformanceJsonReportBytes bytes;
    PerformanceJsonReportStream stream;
    PerformanceJsonReportDuration firstMessageLatencyMs;

    @Builder
    public PerformanceJsonReportApi(String apiId,
                                    String name,
                                    String protocol,
                                    Long total,
                                    Long success,
                                    Long failed,
                                    Double successRate,
                                    Double samplesPerSecond,
                                    Long firstSampleStartTimeMs,
                                    Long lastSampleEndTimeMs,
                                    PerformanceJsonReportDuration durationMs,
                                    PerformanceJsonReportBytes bytes,
                                    PerformanceJsonReportStream stream,
                                    PerformanceJsonReportDuration firstMessageLatencyMs) {
        this.apiId = apiId == null ? "" : apiId;
        this.name = name == null ? "" : name;
        this.protocol = protocol == null ? "" : protocol;
        this.total = Math.max(0L, total == null ? 0L : total);
        this.success = Math.max(0L, success == null ? 0L : success);
        this.failed = Math.max(0L, failed == null ? this.total - this.success : failed);
        this.successRate = successRate == null || !Double.isFinite(successRate)
                ? this.total == 0 ? 0D : this.success * 100D / this.total
                : successRate;
        this.samplesPerSecond = samplesPerSecond == null || !Double.isFinite(samplesPerSecond) ? 0D : samplesPerSecond;
        this.firstSampleStartTimeMs = Math.max(0L, firstSampleStartTimeMs == null ? 0L : firstSampleStartTimeMs);
        this.lastSampleEndTimeMs = Math.max(0L, lastSampleEndTimeMs == null ? 0L : lastSampleEndTimeMs);
        this.durationMs = durationMs == null ? PerformanceJsonReportDuration.builder().build() : durationMs;
        this.bytes = bytes == null ? PerformanceJsonReportBytes.builder().build() : bytes;
        this.stream = stream == null ? PerformanceJsonReportStream.builder().build() : stream;
        this.firstMessageLatencyMs = firstMessageLatencyMs == null
                ? PerformanceJsonReportDuration.builder().build()
                : firstMessageLatencyMs;
    }
}
