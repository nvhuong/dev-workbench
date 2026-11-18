package com.nvhuong.devworkbench.panel.performance.result;


import com.nvhuong.devworkbench.performance.model.PerformanceResultListener;
import com.nvhuong.devworkbench.performance.model.PerformanceResultRetentionPolicy;
import com.nvhuong.devworkbench.performance.core.model.PerformanceSampleRecord;
import com.nvhuong.devworkbench.performance.model.PerformanceSampleEvent;
import com.nvhuong.devworkbench.performance.model.PerformanceSampleResult;
import com.nvhuong.devworkbench.performance.result.PerformanceResultDisplayMapper;
import lombok.RequiredArgsConstructor;

import java.util.function.IntSupplier;

@RequiredArgsConstructor
public final class PerformanceResultTableVisualizer implements PerformanceResultListener {
    private final PerformanceResultTablePanel resultTablePanel;
    private final IntSupplier slowRequestThresholdSupplier;

    @Override
    public void onSample(PerformanceSampleEvent event) {
        if (resultTablePanel == null || event == null || event.sampleRecord() == null) {
            return;
        }
        PerformanceSampleRecord sampleRecord = event.sampleRecord();
        int slowRequestThresholdMs = slowRequestThresholdSupplier == null
                ? 0
                : slowRequestThresholdSupplier.getAsInt();
        if (!PerformanceResultRetentionPolicy.shouldRecord(
                event.isEfficientMode(),
                sampleRecord.isSuccessful(),
                sampleRecord.getElapsedTimeMs(),
                slowRequestThresholdMs)) {
            return;
        }
        PerformanceSampleResult sampleResult = event.getSampleResult();
        if (sampleResult == null) {
            return;
        }
        resultTablePanel.addResult(
                PerformanceResultDisplayMapper.toDisplayNodeInfo(sampleResult, event.isEfficientMode()),
                event.isEfficientMode()
        );
    }
}
