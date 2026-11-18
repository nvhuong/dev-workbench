package com.nvhuong.devworkbench.panel.performance.result;

import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.placeholder.PerformanceTrendPlaceholderPanel;
import com.nvhuong.devworkbench.performance.core.model.PerformanceProtocol;
import com.nvhuong.devworkbench.performance.core.model.PerformanceTrendSnapshot;
import org.jfree.data.time.RegularTimePeriod;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.Set;
import java.util.List;
import java.util.function.Supplier;

public class LazyPerformanceTrendPanel extends JPanel implements PerformanceTrendView {
    private final Supplier<PerformanceTrendPanel> trendPanelSupplier;
    private PerformanceTrendPanel trendPanel;
    private Set<PerformanceProtocol> availableProtocols = EnumSet.of(PerformanceProtocol.HTTP);

    public LazyPerformanceTrendPanel() {
        this(() -> UiSingletonFactory.getInstance(PerformanceTrendPanel.class));
    }

    LazyPerformanceTrendPanel(Supplier<PerformanceTrendPanel> trendPanelSupplier) {
        super(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        this.trendPanelSupplier = trendPanelSupplier == null
                ? () -> UiSingletonFactory.getInstance(PerformanceTrendPanel.class)
                : trendPanelSupplier;
        add(new PerformanceTrendPlaceholderPanel(), BorderLayout.CENTER);
    }

    public boolean isTrendPanelCreated() {
        return trendPanel != null;
    }

    @Override
    public void setAvailableProtocols(Set<PerformanceProtocol> protocols) {
        availableProtocols = normalizeProtocols(protocols);
        if (trendPanel != null) {
            trendPanel.setAvailableProtocols(availableProtocols);
        }
    }

    @Override
    public void clearTrendDataset() {
        if (trendPanel != null) {
            trendPanel.clearTrendDataset();
        }
    }

    @Override
    public void addOrUpdate(RegularTimePeriod period, PerformanceTrendSnapshot snapshot) {
        if (period == null || snapshot == null) {
            return;
        }
        getOrCreateTrendPanel().addOrUpdate(period, snapshot);
    }

    @Override
    public List<TrendReportPoint> reportPoints() {
        return trendPanel == null ? List.of() : trendPanel.reportPoints();
    }

    private PerformanceTrendPanel getOrCreateTrendPanel() {
        if (trendPanel != null) {
            return trendPanel;
        }

        trendPanel = trendPanelSupplier.get();
        trendPanel.setAvailableProtocols(availableProtocols);
        removeAll();
        add(trendPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
        return trendPanel;
    }

    private static Set<PerformanceProtocol> normalizeProtocols(Set<PerformanceProtocol> protocols) {
        EnumSet<PerformanceProtocol> normalized = EnumSet.noneOf(PerformanceProtocol.class);
        if (protocols != null) {
            normalized.addAll(protocols);
        }
        if (normalized.isEmpty()) {
            normalized.add(PerformanceProtocol.HTTP);
        }
        return normalized;
    }
}
