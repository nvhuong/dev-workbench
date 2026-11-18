package com.nvhuong.devworkbench.panel.lifecycle;

import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.exception.CancelException;
import com.nvhuong.devworkbench.frame.MainFrame;
import com.nvhuong.devworkbench.ioc.Component;
import com.nvhuong.devworkbench.panel.collections.OpenedRequestTabSessionSaver;
import com.nvhuong.devworkbench.panel.functional.FunctionalPanel;
import com.nvhuong.devworkbench.panel.performance.PerformancePanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AppExitCoordinator {
    private static final long REMOTE_STOP_TIMEOUT_SECONDS = 10;
    private boolean exitInProgress;

    /**
     * Hiển thị hộp thoại xác nhận thoát để xử lý nội dung chưa được lưu.
     */
    public void exitApplication() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::exitApplication);
            return;
        }
        if (exitInProgress) {
            return;
        }
        // Vẫn có thể được nhận trong hộp thoại lưu phương thức Quit/Sự kiện đóng cửa sổ để tránh quá trình thoát lặp lại.
        exitInProgress = true;

        // Lưu tất cả các yêu cầu đang mở (cả chưa lưu và đã lưu)
        try {
            OpenedRequestTabSessionSaver.saveOpenTabsOnExit();
        } catch (CancelException e) {
            // Người dùng đã hủy thao tác lưu và chấm dứt việc thoát
            exitInProgress = false;
            return;
        } catch (RuntimeException e) {
            exitInProgress = false;
            throw e;
        }

        // Lưu cấu hình kiểm tra chức năng
        try {
            UiSingletonFactory.getExistingInstance(FunctionalPanel.class)
                    .ifPresent(FunctionalPanel::save);
        } catch (Exception e) {
            log.error("Failed to save functional test config on exit", e);
        }

        // Lưu cấu hình kiểm tra hiệu suất
        try {
            UiSingletonFactory.getExistingInstance(PerformancePanel.class)
                    .ifPresent(PerformancePanel::save);
        } catch (Exception e) {
            log.error("Failed to save performance test config on exit", e);
        }

        // Tất cả các mục sẽ được dọn sạch sau khi xác nhận lưu; việc hủy thoát sẽ không giải phóng các bảng vẫn đang được sử dụng.
        log.info("User chose to exit application");
        MainFrame mainFrame = UiSingletonFactory.getExistingInstance(MainFrame.class).orElse(null);
        if (mainFrame != null) {
            mainFrame.prepareForExit();
        }
        PerformancePanel performancePanel = UiSingletonFactory.getExistingInstance(PerformancePanel.class).orElse(null);
        if (performancePanel != null) {
            try {
                performancePanel.cleanup();
            } catch (Exception e) {
                log.warn("Failed to clean up performance panel on exit", e);
            }
        }

        // Yêu cầu dừng mạng cần phải được hoàn thành trước khi thoát JVM，Đợi ở chế độ nền và giữ nó EDT Sự kiện có thể được xử lý.
        Thread exitThread = new Thread(() -> {
            try {
                if (performancePanel != null
                        && !performancePanel.awaitRemoteStopOnExit(REMOTE_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    log.warn("Remote stop requests did not all complete successfully within {} seconds",
                            REMOTE_STOP_TIMEOUT_SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for remote performance workers to stop", e);
            } catch (Exception e) {
                log.warn("Failed to wait for remote performance workers to stop", e);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    if (mainFrame != null) {
                        mainFrame.dispose();
                    }
                    System.exit(0);
                });
            }
        }, "ApplicationExit");
        exitThread.setDaemon(false);
        exitThread.start();
    }
}
