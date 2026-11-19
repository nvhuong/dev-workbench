package com.nvhuong.devworkbench.performance.report;

import com.nvhuong.devworkbench.performance.core.model.PerformanceTrendSnapshot;

/**
 * One sampled point of the performance trend, shared by the trend panel and the headless HTML/PDF report exporters
 * (kept outside the UI package so report generation stays Swing-free).
 */
public record TrendReportPoint(long timestampMs, PerformanceTrendSnapshot snapshot) {
}
