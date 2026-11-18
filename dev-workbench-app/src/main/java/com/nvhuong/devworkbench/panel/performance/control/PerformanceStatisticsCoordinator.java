package com.nvhuong.devworkbench.panel.performance.control;

import com.nvhuong.devworkbench.performance.core.model.PerformanceRealtimeMetrics;
import com.nvhuong.devworkbench.performance.core.model.PerformanceStatsCollector;
import com.nvhuong.devworkbench.performance.core.model.PerformanceTrendSnapshot;
import com.nvhuong.devworkbench.performance.core.model.PerformanceTrendWindowCollector;
import com.nvhuong.devworkbench.performance.core.runtime.PerformanceThreadFactory;
import com.nvhuong.devworkbench.performance.result.PerformanceMetricsSnapshotService;


import com.nvhuong.devworkbench.panel.performance.result.PerformanceReportPanel;
import com.nvhuong.devworkbench.panel.performance.result.PerformanceTrendView;
import lombok.extern.slf4j.Slf4j;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;

import javax.swing.*;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;

@Slf4j
public final class PerformanceStatisticsCoordinator {

    private final PerformanceReportPanel performanceReportPanel;
    private final PerformanceTrendView performanceTrendPanel;
    private final JTabbedPane resultTabbedPane;
    private final BooleanSupplier trendEnabledSupplier;
    private final PerformanceMetricsSnapshotService snapshotService;
    private final ExecutorService metricsExecutor =
            Executors.newSingleThreadExecutor(PerformanceThreadFactory.daemonFactory("PerformanceMetrics"));
    private final AtomicLong runGeneration = new AtomicLong();
    private final Object runGenerationLock = new Object();
    private volatile boolean disposed;

    public PerformanceStatisticsCoordinator(PerformanceStatsCollector statsCollector,
                                            PerformanceTrendWindowCollector trendWindowCollector,
                                            PerformanceReportPanel performanceReportPanel,
                                            PerformanceTrendView performanceTrendPanel,
                                            JTabbedPane resultTabbedPane,
                                            IntSupplier activeThreadsSupplier,
                                            IntSupplier activeWebSocketsSupplier,
                                            IntSupplier activeSseStreamsSupplier,
                                            LongSupplier samplingIntervalSupplier,
                                            BooleanSupplier trendEnabledSupplier,
                                            LongFunction<PerformanceRealtimeMetrics.Sample> realtimeMetricsSampler,
                                            LongFunction<PerformanceRealtimeMetrics.LiveSnapshot> liveMetricsSnapshotSupplier) {
        this.performanceReportPanel = performanceReportPanel;
        this.performanceTrendPanel = performanceTrendPanel;
        this.resultTabbedPane = resultTabbedPane;
        this.trendEnabledSupplier = trendEnabledSupplier;
        this.snapshotService = new PerformanceMetricsSnapshotService(
                statsCollector,
                trendWindowCollector,
                activeThreadsSupplier,
                activeWebSocketsSupplier,
                activeSseStreamsSupplier,
                samplingIntervalSupplier,
                realtimeMetricsSampler,
                liveMetricsSnapshotSupplier
        );
    }

    public void refreshReport() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshReport);
            return;
        }

        try {
            if (resultTabbedPane.getSelectedIndex() != 1) {
                log.debug("Báo cáo hiện không được xemTab，Bỏ qua việc làm mới");
                return;
            }
            updateReportWithLatestData();
        } catch (Exception ex) {
            log.warn("Không thể làm mới báo cáo trong thời gian thực: {}", ex.getMessage(), ex);
        }
    }

    public void updateReportWithLatestData() {
        long generation = currentGeneration();
        submitMetricsTask("Báo cáo cập nhật", generation, () -> {
            var snapshot = snapshotService.reportSnapshot(System.currentTimeMillis());
            invokeUiIfActive(generation, () -> performanceReportPanel.updateReport(snapshot));
        });
    }

    public void updateReportWithLatestDataSync() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateReportWithLatestDataSync);
            return;
        }

        performanceReportPanel.updateReport(snapshotService.reportSnapshot(System.currentTimeMillis()));
    }

    public void updateFinalReportWithStatsSync() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateFinalReportWithStatsSync);
            return;
        }

        // Báo cáo cuối cùng chỉ xem xét tổng số tích lũy stats，Không hợp nhất trong thời gian thực live snapshot，Tránh để dữ liệu có tính nhất quán yếu ở cuối quá trình chạy làm ảnh hưởng đến kết quả cuối cùng.
        performanceReportPanel.updateReport(snapshotService.statsSnapshot());
    }

    public void sampleTrendData() {
        if (!isTrendEnabled()) {
            log.debug("Biểu đồ xu hướng không được bật và việc lấy mẫu bị bỏ qua");
            return;
        }
        if (performanceTrendPanel == null) {
            log.debug("Bảng xu hướng không được khởi tạo và việc lấy mẫu bị bỏ qua");
            return;
        }
        long generation = currentGeneration();

        submitMetricsTask("Lấy mẫu biểu đồ xu hướng", generation, () -> {
            // Cửa sổ xu hướng là có thật ở đây drain，X Thời gian trục phải được sử dụng drain thời gian để tránh sự chậm trễ trong hàng đợi nền khiến mẫu được rút về điểm thời gian cũ.
            long sampleTimeMs = System.currentTimeMillis();
            RegularTimePeriod period = createTrendPeriod(sampleTimeMs);
            PerformanceTrendSnapshot snapshot = snapshotService.drainTrendWindowSnapshot(sampleTimeMs);
            invokeUiIfActive(generation, () -> {
                log.debug("Dữ liệu mẫu {} - Số lượng người dùng: {}, HTTP: {}, WS: {}, SSE: {}",
                        period, snapshot.activeUsers(), snapshot.http().samples(), snapshot.webSocket().samples(), snapshot.sse().samples());
                performanceTrendPanel.addOrUpdate(period, snapshot);
                performanceReportPanel.setTrendReportPoints(performanceTrendPanel.reportPoints());
            });
        });
    }

    public void sampleTrendDataSync() {
        sampleTrendDataSync(System.currentTimeMillis());
    }

    public void sampleTrendDataSync(long sampleTimeMs) {
        if (!isTrendEnabled()) {
            return;
        }
        if (performanceTrendPanel == null) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> sampleTrendDataSync(sampleTimeMs));
            return;
        }

        long normalizedSampleTimeMs = Math.max(0L, sampleTimeMs);
        RegularTimePeriod period = createTrendPeriod(normalizedSampleTimeMs);
        PerformanceTrendSnapshot snapshot = snapshotService.drainTrendWindowSnapshot(normalizedSampleTimeMs);

        log.debug("Đồng bộ hóa dữ liệu được lấy mẫu {} - Số lượng người dùng: {}, HTTP: {}, WS: {}, SSE: {}",
                period, snapshot.activeUsers(), snapshot.http().samples(), snapshot.webSocket().samples(), snapshot.sse().samples());
        performanceTrendPanel.addOrUpdate(period, snapshot);
        performanceReportPanel.setTrendReportPoints(performanceTrendPanel.reportPoints());
    }

    public void appendTerminalIdleTrendPointSync() {
        appendTerminalIdleTrendPointSync(System.currentTimeMillis() + 1L);
    }

    public void appendTerminalIdleTrendPointSync(long timestampMs) {
        if (!isTrendEnabled() || performanceTrendPanel == null) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendTerminalIdleTrendPointSync(timestampMs));
            return;
        }

        performanceTrendPanel.addOrUpdate(createTrendPeriod(timestampMs),
                PerformanceTrendSnapshot.terminalIdle());
        performanceReportPanel.setTrendReportPoints(performanceTrendPanel.reportPoints());
    }

    public void markRunStarted(long startTimeMs) {
        snapshotService.resetTrendWindow(startTimeMs);
        appendTerminalIdleTrendPointSync(startTimeMs);
    }

    private boolean isTrendEnabled() {
        return trendEnabledSupplier == null || trendEnabledSupplier.getAsBoolean();
    }

    public void dispose() {
        disposed = true;
        resetForNewRun();
        metricsExecutor.shutdownNow();
    }

    public void resetForNewRun() {
        synchronized (runGenerationLock) {
            runGeneration.incrementAndGet();
        }
    }

    private long currentGeneration() {
        return runGeneration.get();
    }

    private boolean isCurrentGeneration(long generation) {
        return generation == currentGeneration();
    }

    private void submitMetricsTask(String taskName, long generation, Runnable task) {
        if (disposed) {
            return;
        }
        try {
            metricsExecutor.execute(() -> {
                synchronized (runGenerationLock) {
                    if (!isCurrentGeneration(generation)) {
                        log.debug("{}Đã hết hạn, bỏ qua nhiệm vụ không đồng bộ của vòng kiểm tra căng thẳng cũ", taskName);
                        return;
                    }
                    try {
                        task.run();
                    } catch (Exception ex) {
                        log.error("{}thất bại", taskName, ex);
                    }
                }
            });
        } catch (RejectedExecutionException ex) {
            if (!disposed) {
                log.warn("{}Gửi không thành công", taskName, ex);
            }
        }
    }

    private void invokeUiIfActive(long generation, Runnable uiTask) {
        SwingUtilities.invokeLater(() -> {
            if (!disposed && isCurrentGeneration(generation)) {
                uiTask.run();
            }
        });
    }

    private static RegularTimePeriod createTrendPeriod(long timestampMs) {
        return new Millisecond(new Date(timestampMs));
    }

}
