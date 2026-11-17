package com.laker.postman.performance.core.model;

import java.util.concurrent.atomic.LongAdder;

/**
 * Bộ đếm tăng đơn điệu, tương ứng Micrometer Counter Ngữ nghĩa: Chỉ số gia tăng tích lũy mới được ghi lại, tốc độ được người gọi tính theo từng cửa sổ.
 */
final class PerformanceCounter {
    private final LongAdder count = new LongAdder();

    void increment() {
        increment(1);
    }

    void increment(long amount) {
        if (amount > 0) {
            count.add(amount);
        }
    }

    long count() {
        return count.sum();
    }

    void clear() {
        count.reset();
    }
}
