package com.nvhuong.devworkbench.performance.runtime;

import com.nvhuong.devworkbench.performance.core.report.PerformanceJsonReport;

@FunctionalInterface
public interface PerformanceRunReportListener {
    PerformanceRunReportListener NOOP = report -> {
    };

    void onReport(PerformanceJsonReport report);
}
