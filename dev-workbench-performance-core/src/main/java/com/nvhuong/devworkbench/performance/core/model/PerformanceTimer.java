package com.nvhuong.devworkbench.performance.core.model;

import java.util.concurrent.atomic.LongAdder;

/**
 * Máy đo tốn thời gian, tương ứng Micrometer Timer Ngữ nghĩa: số lượng bản ghi, tổng thời gian thực hiện và ảnh chụp nhanh phân phối.
 */
final class PerformanceTimer {
    private final DurationStatsHistogram histogram = new DurationStatsHistogram();
    private final LongAdder count = new LongAdder();
    private final LongAdder totalTimeMs = new LongAdder();

    void record(long durationMs) {
        long normalized = Math.max(0L, durationMs);
        totalTimeMs.add(normalized);
        histogram.record(normalized);
        count.increment();
    }

    long count() {
        return count.sum();
    }

    long totalTimeMs() {
        return totalTimeMs.sum();
    }

    double meanMs() {
        long currentCount = count();
        return currentCount == 0 ? Double.NaN : PerformanceMetricMath.round((double) totalTimeMs() / currentCount);
    }

    long avgMs() {
        long currentCount = count();
        return currentCount == 0 ? 0 : totalTimeMs() / currentCount;
    }

    PerformanceStatsSnapshot.DurationStats snapshot() {
        return histogram.snapshot();
    }

    void clear() {
        count.reset();
        totalTimeMs.reset();
        histogram.clear();
    }
}
