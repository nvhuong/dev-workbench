package com.nvhuong.devworkbench.performance.master;

import com.nvhuong.devworkbench.performance.core.report.PerformanceJsonReport;

public record PerformanceMasterRunProgress(PerformanceJsonReport report,
                                           int activeUsers,
                                           int totalUsers,
                                           int completedWorkers,
                                           int totalWorkers,
                                           double qps) {
}
