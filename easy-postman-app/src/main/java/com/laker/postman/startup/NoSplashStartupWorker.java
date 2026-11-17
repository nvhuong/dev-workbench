package com.laker.postman.startup;

import com.laker.postman.frame.MainFrame;
import lombok.RequiredArgsConstructor;

import javax.swing.SwingWorker;
import java.util.concurrent.CountDownLatch;

/**
 * Không Splash Cửa sổ chính của phương thức bắt đầu tác vụ ở chế độ nền.
 */
@RequiredArgsConstructor
class NoSplashStartupWorker extends SwingWorker<MainFrame, Void> {
    private final StartupCoordinator startupCoordinator;
    private final CountDownLatch edtStartupCompleted = new CountDownLatch(1);

    void start() {
        // SwingWorker Chuỗi mặc định cho  daemon；Khi không có cửa sổ AWT Nó cũng sẽ tự động thoát.
        // Bởi vòng đời ngắn ngủi không daemon Chủ đề được giữ cho đến khi done Vào EDT Cửa sổ được hiển thị hoặc xử lý không thành công.
        Thread startupThread = new Thread(() -> {
            run();
            boolean interrupted = false;
            while (true) {
                try {
                    edtStartupCompleted.await();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }, "GUI-No-Splash-Startup");
        startupThread.setDaemon(false);
        startupThread.start();
    }

    @Override
    protected MainFrame doInBackground() {
        try {
            return startupCoordinator.prepareMainFrameShell(null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize GUI runtime", e);
        }
    }

    @Override
    protected void done() {
        try {
            MainFrame mainFrame = get();
            startupCoordinator.showMainFrameAndLoadContent(mainFrame);
            startupCoordinator.runAfterMainContentReady(
                    mainFrame,
                    startupCoordinator::scheduleBackgroundTasks,
                    StartupFailureHandler::showStartupErrorAndExit
            );
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            StartupFailureHandler.showStartupErrorAndExit(e);
        } finally {
            edtStartupCompleted.countDown();
        }
    }
}
