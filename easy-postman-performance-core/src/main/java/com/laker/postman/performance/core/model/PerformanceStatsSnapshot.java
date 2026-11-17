package com.laker.postman.performance.core.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PerformanceStatsSnapshot {

    private final List<ApiSummary> summaries;
    private final Map<PerformanceProtocol, ApiSummary> totalsByProtocol;
    private final long totalRequests;
    private final long successRequests;
    private final long retainedRequestResultCount;

    PerformanceStatsSnapshot(List<ApiSummary> summaries,
                             Map<PerformanceProtocol, ApiSummary> totalsByProtocol,
                             long totalRequests,
                             long successRequests,
                             long retainedRequestResultCount) {
        this.summaries = List.copyOf(summaries);
        this.totalsByProtocol = new EnumMap<>(PerformanceProtocol.class);
        this.totalsByProtocol.putAll(totalsByProtocol);
        this.totalRequests = totalRequests;
        this.successRequests = successRequests;
        this.retainedRequestResultCount = retainedRequestResultCount;
    }

    public List<ApiSummary> summaries() {
        return summaries;
    }

    public long totalRequests() {
        return totalRequests;
    }

    public long successRequests() {
        return successRequests;
    }

    public long retainedRequestResultCount() {
        return retainedRequestResultCount;
    }

    public ApiSummary totalFor(PerformanceProtocol protocol, String name) {
        ApiSummary summary = totalsByProtocol.get(protocol);
        return summary == null ? null : summary.withName(name);
    }

    public record ApiSummary(
            String apiId,
            String name,
            PerformanceProtocol protocol,
            long total,
            long success,
            long fail,
            double successRate,
            // QPS：Số lượng yêu cầu /（Thời gian kết thúc mẫu cuối cùng - thời gian bắt đầu mẫu đầu tiên)
            double samplesPerSecond,
            // Thời gian bắt đầu mẫu đầu tiên, cho master hợp nhất worker Tính toán lại theo cửa sổ toàn cầu QPS
            long firstSampleStartTimeMs,
            // Thời gian kết thúc mẫu cuối cùng, cho master hợp nhất worker Tính toán lại theo cửa sổ toàn cầu QPS
            long lastSampleEndTimeMs,
            // Yêu cầu số liệu thống kê tốn thời gian: chỉ thống kê sampler/request Bản thân nó không bao gồm tiền kịch bản, hậu kịch bản, xác nhận và UI kết xuất
            DurationStats durationStats,
            long sentMessages,
            long receivedMessages,
            long matchedMessages,
            double sendRate,
            double receiveRate,
            double matchedRate,
            // Số byte được gửi: tiêu đề yêu cầu + Request body
            long sentBytes,
            // Số byte nhận được: tiêu đề phản hồi + cơ quan phản ứng
            long receivedBytes,
            // Tốc độ byte gửi: .sentBytes / sample cửa sổ thời gian,UI Có thể ép được KB/s hiển thị
            double sentBytesPerSecond,
            // Nhận tốc độ byte:receivedBytes / sample cửa sổ thời gian,UI Có thể ép được KB/s hiển thị
            double receivedBytesPerSecond,
            // Số byte trung bình nhận được:receivedBytes / total，được sử dụng để căn chỉnh JMeter Avg. Bytes tầm cỡ
            long avgReceivedBytes,
            long avgFirstMessageLatencyMs,
            DurationStats firstMessageLatencyStats
    ) {
        ApiSummary withName(String newName) {
            return new ApiSummary(
                    apiId,
                    newName,
                    protocol,
                    total,
                    success,
                    fail,
                    successRate,
                    samplesPerSecond,
                    firstSampleStartTimeMs,
                    lastSampleEndTimeMs,
                    durationStats,
                    sentMessages,
                    receivedMessages,
                    matchedMessages,
                    sendRate,
                    receiveRate,
                    matchedRate,
                    sentBytes,
                    receivedBytes,
                    sentBytesPerSecond,
                    receivedBytesPerSecond,
                    avgReceivedBytes,
                    avgFirstMessageLatencyMs,
                    firstMessageLatencyStats
            );
        }
    }

    public record DurationStats(
            long avg,
            long min,
            long max,
            long p90,
            long p95,
            long p99
    ) {
        public static DurationStats empty() {
            return new DurationStats(0, 0, 0, 0, 0, 0);
        }
    }
}
