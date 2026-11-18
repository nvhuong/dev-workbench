package com.nvhuong.devworkbench.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Thành phần nhãn hiển thị mức sử dụng bộ nhớ, dòng điện hiển thịJVMSử dụng bộ nhớ.
 * Hỗ trợ làm mới tự động và nhấp đúp thủ côngGCChức năng.
 */
public class MemoryLabel extends JLabel {
    private static final double BYTES_PER_MB = 1024.0 * 1024.0;
    private static final double MB_PER_GB = 1024.0;
    private static final String DEFAULT_SEPARATOR = " / ";
    private static final String COMPACT_SEPARATOR = "/";

    private transient Timer refreshTimer;
    private final int refreshInterval;
    private final boolean compactSeparator;
    private boolean autoRefresh = true;

    /**
     * Tạo thẻ nhớ, mặc định1Làm mới mỗi giây một lần
     */
    public MemoryLabel() {
        this(1000, false);
    }

    /**
     * Tạo thẻ nhớ và chỉ định khoảng thời gian làm mới
     *
     * @param refreshIntervalMs Làm mới khoảng thời gian(mili giây)
     */
    public MemoryLabel(int refreshIntervalMs) {
        this(refreshIntervalMs, false);
    }

    public MemoryLabel(int refreshIntervalMs, boolean compactSeparator) {
        super();
        this.refreshInterval = refreshIntervalMs;
        this.compactSeparator = compactSeparator;

        // Đặt biểu tượng và phông chữ
        setIcon(new FlatSVGIcon("icons/computer.svg", 20, 20));
        setFont(getFont().deriveFont(Font.BOLD));
        setToolTipText("hiện tạiJVMSử dụng bộ nhớ, nhấp đúp chuộtGC");

        // Làm mới ban đầu
        updateMemoryInfo();

        // Đã thêm nhấp đúp chuộtGCChức năng
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    System.gc();
                    updateMemoryInfo();
                }
            }
        });

        // Bắt đầu làm mới theo lịch trình
        startAutoRefresh();
    }

    /**
     * Cập nhật hiển thị thông tin bộ nhớ
     */
    public void updateMemoryInfo() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        String separator = compactSeparator ? COMPACT_SEPARATOR : DEFAULT_SEPARATOR;
        setText(formatSize(used) + separator + formatSize(max));
    }

    /**
     * Kích thước bộ nhớ được định dạng, tự động hiển thị dưới dạng MB hoặc GB
     */
    private String formatSize(long bytes) {
        double mb = bytes / BYTES_PER_MB;
        if (mb < MB_PER_GB) {
            return String.format(Locale.ROOT, "%dMB", Math.round(mb));
        } else {
            double gb = mb / MB_PER_GB;
            if (gb == (long) gb) {
                return String.format(Locale.ROOT, "%dGB", (long) gb);
            } else {
                return String.format(Locale.ROOT, "%.1fGB", gb).replace(".0GB", "GB");
            }
        }
    }

    /**
     * Bắt đầu làm mới tự động
     */
    public void startAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        autoRefresh = true;
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> updateMemoryInfo());
            }
        }, 0, refreshInterval);
    }

    /**
     * Dừng làm mới tự động
     */
    public void stopAutoRefresh() {
        autoRefresh = false;
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    /**
     * Dừng bộ đếm thời gian khi thành phần được gỡ bỏ
     */
    @Override
    public void removeNotify() {
        stopAutoRefresh();
        super.removeNotify();
    }

    /**
     * Đặt xem có tự động làm mới hay không
     *
     * @param autoRefresh Có tự động làm mới hay không
     */
    public void setAutoRefresh(boolean autoRefresh) {
        if (this.autoRefresh == autoRefresh) return;

        if (autoRefresh) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }
    }

}
