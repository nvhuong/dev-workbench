package com.laker.postman.performance.core.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

public final class PerformanceStatsCollector {

    // Nguồn có thẩm quyền duy nhất của các tuyên bố cuối cùng. Biểu đồ xu hướng và báo cáo thời gian thực có thể ít nhất quán nhưng không thể viết ngược lại ở đây.
    private final ConcurrentMap<PerformanceProtocol, ConcurrentMap<String, PerformanceSampleMeterSet>> apiStatsByProtocol =
            new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<PerformanceSampleMeterSet> apiStatsInFirstSeenOrder = new ConcurrentLinkedDeque<>();
    private final ConcurrentMap<PerformanceProtocol, PerformanceSampleMeterSet> protocolTotals = new ConcurrentHashMap<>();
    private final PerformanceSampleMeterSet overallStats = new PerformanceSampleMeterSet("", PerformanceProtocol.HTTP);

    public void record(RequestResult result) {
        if (result == null) {
            return;
        }
        PerformanceProtocol protocol = result.protocol == null ? PerformanceProtocol.HTTP : result.protocol;
        String apiId = result.apiId == null ? "" : result.apiId;
        ConcurrentMap<String, PerformanceSampleMeterSet> protocolApiStats =
                apiStatsByProtocol.computeIfAbsent(protocol, ignored -> new ConcurrentHashMap<>());

        protocolApiStats.computeIfAbsent(apiId, ignored -> {
            PerformanceSampleMeterSet stats = new PerformanceSampleMeterSet(apiId, protocol);
            apiStatsInFirstSeenOrder.add(stats);
            return stats;
        }).record(result);
        protocolTotals.computeIfAbsent(protocol, ignored -> new PerformanceSampleMeterSet("", protocol)).record(result);
        overallStats.record(result);
    }

    public PerformanceStatsSnapshot snapshot() {
        // Meter Để sử dụng nội bộ LongAdder/CAS；Ảnh chụp nhanh đang chạy cho phép tính nhất quán yếu và hội tụ đến giá trị chính xác cuối cùng khi luồng thu thập thoát ra sau khi hoàn tất quá trình kiểm tra căng thẳng.
        List<PerformanceStatsSnapshot.ApiSummary> summaries = new ArrayList<>();
        for (PerformanceSampleMeterSet stats : apiStatsInFirstSeenOrder) {
            summaries.add(stats.toSummary(stats.apiName()));
        }

        EnumMap<PerformanceProtocol, PerformanceStatsSnapshot.ApiSummary> totals = new EnumMap<>(PerformanceProtocol.class);
        for (Map.Entry<PerformanceProtocol, PerformanceSampleMeterSet> entry : protocolTotals.entrySet()) {
            totals.put(entry.getKey(), entry.getValue().toSummary(""));
        }
        PerformanceStatsSnapshot.ApiSummary overallSummary = overallStats.toSummary("");

        return new PerformanceStatsSnapshot(
                summaries,
                totals,
                overallSummary.total(),
                overallSummary.success(),
                0
        );
    }

    public PerformanceStatsProgressSnapshot progressSnapshot() {
        return overallStats.toProgressSnapshot();
    }

    public void clear() {
        apiStatsByProtocol.clear();
        apiStatsInFirstSeenOrder.clear();
        protocolTotals.clear();
        overallStats.clear();
    }
}
