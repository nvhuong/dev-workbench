package com.nvhuong.devworkbench.panel.performance.control;

import com.nvhuong.devworkbench.performance.core.runtime.PerformanceThreadFactory;


import com.nvhuong.devworkbench.service.setting.SettingManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Trình quản lý hẹn giờ kiểm tra hiệu suất
 *
 * @author nvhuong
 */
@Slf4j
public class PerformanceTimerManager {

    /**
     * Lập lịch tác vụ theo lịch trình (nhóm luồng nền)
     */
    private ScheduledExecutorService scheduler;

    /**
     * Nhiệm vụ theo lịch trình lấy mẫu biểu đồ xu hướng
     */
    private ScheduledFuture<?> trendSamplingTask;

    /**
     * Báo cáo làm mới nhiệm vụ theo lịch trình
     */
    private ScheduledFuture<?> reportRefreshTask;

    @Getter
    private long samplingIntervalMs = 1000;

    /**
     * Khoảng thời gian làm mới báo cáo (mili giây)
     */
    private static final int REPORT_REFRESH_INTERVAL_MS = 1000;

    /**
     * Chạy trình kiểm tra trạng thái - Được sử dụng để xác định xem các tác vụ đã lên lịch có nên tiếp tục được thực hiện hay không
     */
    private final BooleanSupplier runningChecker;

    @Setter
    private Runnable trendSamplingCallback;

    @Setter
    private Runnable reportRefreshCallback;

    /**
     * Có bật lấy mẫu biểu đồ xu hướng hay không. Khi đóng, sẽ không có tác vụ lấy mẫu theo lịch trình nào được tạo để giảm chi phí lấy mẫu và làm mới biểu đồ.
     */
    private volatile boolean trendSamplingEnabled = true;

    /**
     * Có bật tính năng làm mới báo cáo đang chạy hay không. Theo mặc định, chúng được tạo đồng nhất sau khi hoàn thành để giảm chi phí cho các ảnh chụp nhanh và làm mới bảng trong quá trình kiểm tra sức chịu đựng.
     */
    private volatile boolean reportRefreshEnabled = false;

    /**
     * Cho dù người quản lý đã được bắt đầu
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Người xây dựng
     *
     * @param runningChecker Chạy trình kiểm tra trạng thái, quay lại true Cho biết đang chạy
     */
    public PerformanceTimerManager(BooleanSupplier runningChecker) {
        this.runningChecker = runningChecker;
    }

    /**
     * Bắt đầu tất cả các bộ tính giờ
     * Bao gồm bộ đếm thời gian lấy mẫu biểu đồ xu hướng và bộ đếm thời gian làm mới báo cáo
     */
    public void startAll() {
        if (started.getAndSet(true)) {
            log.warn("Trình quản lý hẹn giờ đã được khởi động, bỏ qua các lần khởi động lặp lại");
            return;
        }

        // Đọc khoảng thời gian lấy mẫu từ cài đặt
        int samplingIntervalSeconds = SettingManager.getTrendSamplingIntervalSeconds();
        samplingIntervalMs = samplingIntervalSeconds * 1000L;

        log.info("Bắt đầu hẹn giờ kiểm tra hiệu suất - Khoảng thời gian lấy mẫu: {}giây, Báo cáo được làm mới trong thời gian thực: {}",
                samplingIntervalSeconds, reportRefreshEnabled);

        scheduler = Executors.newSingleThreadScheduledExecutor(
                PerformanceThreadFactory.daemonFactory("PerformanceTimer")
        );

        if (trendSamplingEnabled) {
            startTrendSamplingTimer();
        } else {
            log.info("Việc lấy mẫu biểu đồ xu hướng bị tắt và bộ đếm thời gian lấy mẫu bị bỏ qua");
        }
        if (reportRefreshEnabled) {
            startReportRefreshTimer();
        } else {
            log.info("Tính năng làm mới báo cáo theo thời gian thực bị tắt và báo cáo sẽ được tạo thống nhất sau khi hoàn thành");
        }
    }

    public synchronized void setTrendSamplingEnabled(boolean enabled) {
        if (trendSamplingEnabled == enabled) {
            return;
        }
        trendSamplingEnabled = enabled;
        if (!enabled) {
            stopTrendSamplingTimer();
            log.info("Lấy mẫu xu hướng bị tắt");
            return;
        }
        if (started.get() && scheduler != null && !scheduler.isShutdown()) {
            startTrendSamplingTimer();
        }
        log.info("Lấy mẫu biểu đồ xu hướng được bật");
    }

    public synchronized void setReportRefreshEnabled(boolean enabled) {
        if (reportRefreshEnabled == enabled) {
            return;
        }
        reportRefreshEnabled = enabled;
        if (!enabled) {
            stopReportRefreshTimer();
            log.info("Báo cáo làm mới thời gian thực bị tắt");
            return;
        }
        if (started.get() && scheduler != null && !scheduler.isShutdown()) {
            startReportRefreshTimer();
        }
        log.info("Làm mới báo cáo theo thời gian thực được bật");
    }

    /**
     * Bắt đầu hẹn giờ lấy mẫu biểu đồ xu hướng
     */
    private void startTrendSamplingTimer() {
        if (!trendSamplingEnabled) {
            return;
        }
        if (trendSamplingCallback == null) {
            log.warn("Cuộc gọi lại lấy mẫu biểu đồ xu hướng không được đặt và bộ đếm thời gian lấy mẫu bị bỏ qua");
            return;
        }
        if (scheduler == null || scheduler.isShutdown()) {
            log.warn("Bộ lập lịch hẹn giờ chưa sẵn sàng, hãy bỏ qua việc khởi động bộ hẹn giờ lấy mẫu");
            return;
        }
        if (trendSamplingTask != null && !trendSamplingTask.isCancelled() && !trendSamplingTask.isDone()) {
            return;
        }

        trendSamplingTask = scheduler.scheduleAtFixedRate(() -> {
            if (!runningChecker.getAsBoolean()) {
                log.debug("Kiểm tra trạng thái đang chạy không thành công và việc lấy mẫu biểu đồ xu hướng bị bỏ qua");
                return;
            }
            try {
                trendSamplingCallback.run();
            } catch (Exception ex) {
                log.error("Việc thực hiện lấy mẫu biểu đồ xu hướng không thành công", ex);
            }
        }, samplingIntervalMs, samplingIntervalMs, TimeUnit.MILLISECONDS);

        log.debug("Bộ đếm thời gian lấy mẫu xu hướng đã được bắt đầu - khoảng thời gian: {}ms", samplingIntervalMs);
    }

    private void stopTrendSamplingTimer() {
        if (trendSamplingTask != null) {
            trendSamplingTask.cancel(false);
            trendSamplingTask = null;
        }
    }

    /**
     * Bắt đầu hẹn giờ làm mới báo cáo
     */
    private void startReportRefreshTimer() {
        if (!reportRefreshEnabled) {
            return;
        }
        if (reportRefreshCallback == null) {
            log.warn("Cuộc gọi lại làm mới báo cáo không được đặt và bộ đếm thời gian làm mới bị bỏ qua");
            return;
        }
        if (scheduler == null || scheduler.isShutdown()) {
            log.warn("Bộ lập lịch hẹn giờ chưa sẵn sàng, bỏ qua việc bắt đầu hẹn giờ làm mới báo cáo");
            return;
        }
        if (reportRefreshTask != null && !reportRefreshTask.isCancelled() && !reportRefreshTask.isDone()) {
            return;
        }

        reportRefreshTask = scheduler.scheduleAtFixedRate(() -> {
            if (!runningChecker.getAsBoolean()) {
                log.debug("Kiểm tra trạng thái đang chạy không thành công và quá trình làm mới báo cáo bị bỏ qua");
                return;
            }
            try {
                reportRefreshCallback.run();
            } catch (Exception ex) {
                log.error("Việc thực hiện làm mới báo cáo không thành công", ex);
            }
        }, 0, REPORT_REFRESH_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.debug("Đã bắt đầu hẹn giờ làm mới báo cáo - khoảng thời gian: {}ms", REPORT_REFRESH_INTERVAL_MS);
    }

    private void stopReportRefreshTimer() {
        if (reportRefreshTask != null) {
            reportRefreshTask.cancel(false);
            reportRefreshTask = null;
        }
    }

    /**
     * Dừng tất cả các bộ tính giờ
     */
    public void stopAll() {
        if (!started.getAndSet(false)) {
            log.debug("Trình quản lý hẹn giờ không được khởi động và không cần phải dừng");
            return;
        }

        log.info("Dừng tất cả các bộ tính giờ kiểm tra hiệu suất");

        // Hủy bỏ các nhiệm vụ đã lên lịch
        stopTrendSamplingTimer();
        stopReportRefreshTimer();

        // Đóng lịch trình
        ScheduledExecutorService schedulerToStop = scheduler;
        scheduler = null;
        if (schedulerToStop != null) {
            schedulerToStop.shutdown();
            if (SwingUtilities.isEventDispatchThread()) {
                log.debug("Vào EDT Bỏ qua việc chờ hết giờ để tránh chặn giao diện");
                return;
            }
            try {
                if (!schedulerToStop.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("Đồng hồ hẹn giờ không thành công 2 Tắt máy bình thường trong vài giây và buộc phải dừng");
                    schedulerToStop.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Bộ đếm thời gian chờ bị gián đoạn khi nó đóng và buộc phải dừng lại");
                schedulerToStop.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.debug("Bộ đếm thời gian kiểm tra hiệu suất đã dừng");
    }

    /**
     * Dọn dẹp tài nguyên
     */
    public void dispose() {
        log.info("Phá hủy trình quản lý hẹn giờ");
        stopAll();
        trendSamplingCallback = null;
        reportRefreshCallback = null;
    }
}
