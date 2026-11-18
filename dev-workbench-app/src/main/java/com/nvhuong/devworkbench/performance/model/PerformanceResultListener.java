package com.nvhuong.devworkbench.performance.model;


@FunctionalInterface
public interface PerformanceResultListener {
    void onSample(PerformanceSampleEvent event);
}
