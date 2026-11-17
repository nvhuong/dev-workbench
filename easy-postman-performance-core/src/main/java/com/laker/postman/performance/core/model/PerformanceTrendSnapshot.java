package com.laker.postman.performance.core.model;

import java.util.List;

public record PerformanceTrendSnapshot(
        int activeUsers,
        int activeWebSocketConnections,
        int activeSseStreams,
        ProtocolWindowMetrics overview,
        ProtocolWindowMetrics http,
        ProtocolWindowMetrics webSocket,
        ProtocolWindowMetrics sse
) {
    private static final ProtocolWindowMetrics EMPTY_METRICS =
            new ProtocolWindowMetrics(
                    0,
                    0,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    0,
                    0,
                    0,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN,
                    Double.NaN
            );

    /**
     * Điểm hiển thị bổ sung được thêm vào sau bài kiểm tra căng thẳng: chỉ những người dùng đang hoạt động/Kết nối được đặt lại về 0 và thông lượng, mức tiêu thụ thời gian cũng như tỷ lệ lỗi không đại diện cho cửa sổ lấy mẫu thực.
     */
    public static PerformanceTrendSnapshot terminalIdle() {
        return new PerformanceTrendSnapshot(
                0,
                0,
                0,
                EMPTY_METRICS,
                EMPTY_METRICS,
                EMPTY_METRICS,
                EMPTY_METRICS
        );
    }

    public static PerformanceTrendSnapshot fromResults(List<RequestResult> results,
                                                       long windowStart,
                                                       long now,
                                                       int activeUsers,
                                                       int activeWebSocketConnections,
                                                       int activeSseStreams,
                                                       long samplingIntervalMs) {
        return fromResults(
                results,
                windowStart,
                now,
                activeUsers,
                activeWebSocketConnections,
                activeSseStreams,
                samplingIntervalMs,
                null
        );
    }

    public static PerformanceTrendSnapshot fromResults(List<RequestResult> results,
                                                       long windowStart,
                                                       long now,
                                                       int activeUsers,
                                                       int activeWebSocketConnections,
                                                       int activeSseStreams,
                                                       long samplingIntervalMs,
                                                       PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        List<RequestResult> safeResults = results == null ? List.of() : results;
        List<RequestResult> windowResults = safeResults.stream()
                .filter(result -> result.endTime >= windowStart && result.endTime <= now)
                .toList();
        PerformanceSampleMeterSnapshot overviewStats = accumulate(windowResults, null);
        return new PerformanceTrendSnapshot(
                activeUsers,
                activeWebSocketConnections,
                activeSseStreams,
                PerformanceWindowMetricsFactory.fromSnapshot(overviewStats, null, samplingIntervalMs, null),
                PerformanceWindowMetricsFactory.fromSnapshot(
                        accumulate(windowResults, PerformanceProtocol.HTTP),
                        PerformanceProtocol.HTTP,
                        samplingIntervalMs,
                        null
                ),
                PerformanceWindowMetricsFactory.fromSnapshot(
                        accumulate(windowResults, PerformanceProtocol.WEBSOCKET),
                        PerformanceProtocol.WEBSOCKET,
                        samplingIntervalMs,
                        realtimeMetrics
                ),
                PerformanceWindowMetricsFactory.fromSnapshot(
                        accumulate(windowResults, PerformanceProtocol.SSE),
                        PerformanceProtocol.SSE,
                        samplingIntervalMs,
                        realtimeMetrics
                )
        );
    }

    private static PerformanceSampleMeterSnapshot accumulate(List<RequestResult> results, PerformanceProtocol protocol) {
        PerformanceSampleMeterSet accumulator = new PerformanceSampleMeterSet("", protocol);
        for (RequestResult result : results) {
            if (protocol == null || result.protocol == protocol) {
                accumulator.record(result);
            }
        }
        return accumulator.snapshot();
    }

    public record ProtocolWindowMetrics(
            int samples,
            int failures,
            double failurePercent,
            double sampleRate,
            double avgDurationMs,
            int sentMessages,
            int receivedMessages,
            int matchedMessages,
            double sentRate,
            double receivedRate,
            double matchedRate,
            double avgFirstMessageLatencyMs,
            /* Thời gian phản hồi tối đa trong cửa sổ, được sử dụng để hiển thị các yêu cầu chậm bị che khuất bởi mức trung bình; không rõ NaN。 */
            double maxDurationMs
    ) {
        public ProtocolWindowMetrics(int samples,
                                     int failures,
                                     double failurePercent,
                                     double sampleRate,
                                     double avgDurationMs,
                                     int sentMessages,
                                     int receivedMessages,
                                     int matchedMessages,
                                     double sentRate,
                                     double receivedRate,
                                     double matchedRate,
                                     double avgFirstMessageLatencyMs) {
            this(samples, failures, failurePercent, sampleRate, avgDurationMs, sentMessages, receivedMessages,
                    matchedMessages, sentRate, receivedRate, matchedRate, avgFirstMessageLatencyMs, Double.NaN);
        }
    }
}
