package com.nvhuong.devworkbench.performance.master;

@FunctionalInterface
public interface PerformanceMasterRunListener {
    PerformanceMasterRunListener NOOP = progress -> {
    };

    void onProgress(PerformanceMasterRunProgress progress);
}
