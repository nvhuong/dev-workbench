package com.nvhuong.devworkbench.performance.core.model;

import java.util.concurrent.atomic.LongAdder;

/**
 * Tóm tắt phân phối số, thư từ Micrometer DistributionSummary Ngữ nghĩa, thích hợp để ghi lại số lượng không tốn thời gian như số byte và số lượng tin nhắn.
 */
final class PerformanceDistributionSummary {
    private final LongAdder count = new LongAdder();
    private final LongAdder totalAmount = new LongAdder();

    void record(long amount) {
        long normalized = Math.max(0L, amount);
        totalAmount.add(normalized);
        count.increment();
    }

    long count() {
        return count.sum();
    }

    long totalAmount() {
        return totalAmount.sum();
    }

    double mean() {
        long currentCount = count();
        return currentCount == 0 ? Double.NaN : PerformanceMetricMath.round((double) totalAmount() / currentCount);
    }

    long avg() {
        long currentCount = count();
        return currentCount == 0 ? 0 : totalAmount() / currentCount;
    }

    void clear() {
        count.reset();
        totalAmount.reset();
    }
}
