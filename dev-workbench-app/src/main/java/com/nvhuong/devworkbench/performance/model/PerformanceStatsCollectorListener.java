package com.nvhuong.devworkbench.performance.model;

import com.nvhuong.devworkbench.performance.core.model.PerformanceStatsCollector;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class PerformanceStatsCollectorListener implements PerformanceResultListener {
    private final PerformanceStatsCollector statsCollector;

    @Override
    public void onSample(PerformanceSampleEvent event) {
        if (statsCollector == null || event == null || event.sampleRecord() == null) {
            return;
        }
        if (event.sampleRecord().isInterrupted()) {
            return;
        }
        statsCollector.record(event.sampleRecord().toRequestResult());
    }
}
