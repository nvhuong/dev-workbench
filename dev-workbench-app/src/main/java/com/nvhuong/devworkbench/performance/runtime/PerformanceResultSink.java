package com.nvhuong.devworkbench.performance.runtime;

import com.nvhuong.devworkbench.performance.core.model.PerformanceSampleRecord;
import com.nvhuong.devworkbench.performance.core.runtime.PerformanceCoreResultSink;


import com.nvhuong.devworkbench.performance.model.PerformanceSampleEvent;

public interface PerformanceResultSink extends PerformanceCoreResultSink {
    PerformanceResultSink NOOP = new PerformanceResultSink() {
    };

    @Override
    default void onSample(PerformanceSampleRecord record) {
    }

    default void onSample(PerformanceSampleEvent event) {
        onSample(event == null ? null : event.sampleRecord());
    }
}
