package com.nvhuong.devworkbench.panel.performance.result;

import com.nvhuong.devworkbench.performance.report.TrendReportPoint;
import com.nvhuong.devworkbench.performance.core.model.PerformanceTrendSnapshot;
import com.nvhuong.devworkbench.performance.core.model.PerformanceProtocol;
import org.jfree.data.time.RegularTimePeriod;

import java.util.Set;
import java.util.List;

public interface PerformanceTrendView {
    default void setAvailableProtocols(Set<PerformanceProtocol> protocols) {
    }

    void clearTrendDataset();

    void addOrUpdate(RegularTimePeriod period, PerformanceTrendSnapshot snapshot);

    default List<TrendReportPoint> reportPoints() {
        return List.of();
    }
}
