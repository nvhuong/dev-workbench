package com.nvhuong.devworkbench.performance.model;

import com.nvhuong.devworkbench.performance.core.model.PerformanceTrendWindowCollector;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class PerformanceTrendWindowCollectorListener implements PerformanceResultListener {
    private final PerformanceTrendWindowCollector trendWindowCollector;

    @Override
    public void onSample(PerformanceSampleEvent event) {
        if (trendWindowCollector == null || event == null || event.sampleRecord() == null
                || event.sampleRecord().isInterrupted()) {
            return;
        }
        trendWindowCollector.record(event.sampleRecord().toRequestResult());
    }
}
