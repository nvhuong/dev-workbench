package com.laker.postman.startup;

import com.laker.postman.frame.MainFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.Timer;
import java.util.function.Consumer;

/**
 * Lắng nghe chốt khởi chạy cửa sổ chính: vỏ khởi chạy hiển thị và nội dung chính tùy chọn được tải.
 */
@RequiredArgsConstructor
@Slf4j
class MainFrameReadinessWatcher {
    private static final int STARTUP_SHELL_PAINT_FALLBACK_MS = 300;

    private final MainFrame mainFrame;
    private final boolean waitForMainContent;
    private final Runnable onReady;
    private final Consumer<Throwable> onFailure;

    private boolean contentReady;
    private boolean shellReady;
    private boolean readyNotified;
    private boolean failureHandled;
    private Timer shellPaintFallbackTimer;

    void start() {
        if (mainFrame == null || onReady == null) {
            return;
        }

        mainFrame.whenMainContentLoaded(this::markMainContentReady);
        mainFrame.whenMainContentLoadFailed(this::handleFailure);
        mainFrame.whenStartupShellPainted(this::markStartupShellReady);
        startShellPaintFallbackTimer();
    }

    private void markMainContentReady() {
        contentReady = true;
        notifyReadyIfPossible();
    }

    private void markStartupShellReady() {
        if (shellReady) {
            return;
        }
        shellReady = true;
        stopShellPaintFallbackTimer();
        notifyReadyIfPossible();
    }

    private void startShellPaintFallbackTimer() {
        // Một số nền tảng sẽ không kích hoạt gọi lại khung đầu tiên một cách ổn định, vì vậy hãy tránh hiện tượng đoản mạch Splash mắc kẹt.
        shellPaintFallbackTimer = new Timer(STARTUP_SHELL_PAINT_FALLBACK_MS, e -> {
            if (!shellReady) {
                log.warn("Main frame startup shell did not report a paint within {} ms; continuing via fallback",
                        STARTUP_SHELL_PAINT_FALLBACK_MS);
            }
            markStartupShellReady();
        });
        shellPaintFallbackTimer.setRepeats(false);
        shellPaintFallbackTimer.start();
    }

    private void notifyReadyIfPossible() {
        if (readyNotified || failureHandled || !shellReady || (waitForMainContent && !contentReady)) {
            return;
        }
        readyNotified = true;
        AppLauncher.markStartupCheckpoint(contentReady
                ? "main content ready"
                : "startup shell painted");
        log.info("Main frame readiness satisfied; running startup continuation");
        onReady.run();
    }

    private void handleFailure(Throwable throwable) {
        if (failureHandled) {
            return;
        }
        failureHandled = true;
        log.warn("Main frame readiness failed; forwarding to startup failure handler");
        stopShellPaintFallbackTimer();
        if (onFailure != null) {
            onFailure.accept(throwable);
        }
    }

    private void stopShellPaintFallbackTimer() {
        if (shellPaintFallbackTimer != null && shellPaintFallbackTimer.isRunning()) {
            shellPaintFallbackTimer.stop();
        }
    }
}
