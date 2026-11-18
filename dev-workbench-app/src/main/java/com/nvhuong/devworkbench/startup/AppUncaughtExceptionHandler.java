package com.nvhuong.devworkbench.startup;

import com.nvhuong.devworkbench.frame.MainFrame;
import com.nvhuong.devworkbench.util.ExceptionUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.Window;

/**
 * Các trường hợp ngoại lệ chưa được nắm bắt được xử lý thống nhất để ngăn các lớp đầu vào dựa trực tiếp vào thông báo UI chi tiết.
 */
@Slf4j
class AppUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        if (ExceptionUtil.shouldIgnoreException(throwable)) {
            log.debug("Ignoring harmless exception in thread: {}", thread.getName(), throwable);
            return;
        }

        log.error("Uncaught exception in thread: {}", thread.getName(), throwable);
        if (GraphicsEnvironment.isHeadless()) {
            log.debug("Skipping uncaught exception UI notification in headless mode");
            return;
        }
        SwingUtilities.invokeLater(this::notifyUser);
    }

    private void notifyUser() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        String message = I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE);
        if (isMainFrameVisible()) {
            NotificationCenter.showError(message);
            return;
        }

        JOptionPane.showMessageDialog(
                null,
                message,
                I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private boolean isMainFrameVisible() {
        for (Window window : Window.getWindows()) {
            if (window instanceof MainFrame && window.isShowing()) {
                return true;
            }
        }
        return false;
    }
}
