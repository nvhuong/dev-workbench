package com.nvhuong.devworkbench.performance.worker;

import com.nvhuong.devworkbench.performance.core.report.PerformanceJsonReport;
import com.nvhuong.devworkbench.performance.core.worker.PerformanceWorkerRunRequest;
import com.nvhuong.devworkbench.performance.runtime.PerformanceRunExecutionControl;

@FunctionalInterface
public interface PerformanceWorkerRunExecutor {
    PerformanceJsonReport execute(PerformanceWorkerRunRequest request,
                                  PerformanceRunExecutionControl control) throws Exception;
}
