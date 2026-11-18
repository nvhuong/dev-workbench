package com.nvhuong.devworkbench.common.component.notification;

import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.model.NotificationPosition;
import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * IntelliJ IDEA Bức tranh lớn về phong cách Toast Lối vào thông báo.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationCenter {
    private static final int MAX_ACTIVE_TOASTS = 5;
    private static final NotificationCenter DEFAULT = new NotificationCenter(new ToastStack(MAX_ACTIVE_TOASTS));

    private NotificationPosition defaultPosition = NotificationPosition.BOTTOM_RIGHT;
    private final ToastStack toastStack;

    private Window getMainFrame() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        Window activeAnchor = resolveNotificationAnchor(focusManager.getActiveWindow());
        if (activeAnchor != null) {
            return activeAnchor;
        }

        Window focusedAnchor = resolveNotificationAnchor(focusManager.getFocusedWindow());
        if (focusedAnchor != null) {
            return focusedAnchor;
        }

        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.isActive()) {
                Window activeWindowAnchor = resolveNotificationAnchor(window);
                if (activeWindowAnchor != null) {
                    return activeWindowAnchor;
                }
            }
        }

        Window visibleWindow = findVisibleFallbackWindow();
        if (visibleWindow != null) {
            return visibleWindow;
        }

        return JOptionPane.getRootFrame();
    }

    /**
     * Thông báo có sẵn ở cấp độ ngoài cùng theo mặc định owner để tránh dính vào mép cửa sổ con khi được kích hoạt trong hộp thoại phương thức.
     */
    private static Window resolveNotificationAnchor(Window window) {
        if (window == null || !window.isShowing()) {
            return null;
        }
        Window anchor = isUsableAnchorWindow(window) ? window : null;
        Window owner = window.getOwner();
        while (owner != null) {
            if (isUsableAnchorWindow(owner)) {
                anchor = owner;
            }
            owner = owner.getOwner();
        }
        return anchor;
    }

    /**
     * Khi ứng dụng bị chặn bởi các cửa sổ khác.AWT Có lẽ là không active/focused window。Nó vẫn nên được treo vào thời điểm này
     * DevWorkbench Cửa sổ chính đã được hiển thị thay vì bị bỏ JOptionPane của 0x0 shared owner。
     */
    private static Window findVisibleFallbackWindow() {
        Window dialogAnchor = null;
        Window anyAnchor = null;
        for (Window window : Window.getWindows()) {
            Window anchor = resolveNotificationAnchor(window);
            if (anchor == null) {
                continue;
            }
            if (anchor instanceof Frame) {
                return anchor;
            }
            if (dialogAnchor == null && anchor instanceof Dialog) {
                dialogAnchor = anchor;
            }
            if (anyAnchor == null) {
                anyAnchor = anchor;
            }
        }
        return dialogAnchor != null ? dialogAnchor : anyAnchor;
    }

    private static boolean isUsableAnchorWindow(Window window) {
        if (window == null || window instanceof ToastWindow || !window.isShowing()) {
            return false;
        }
        if (window.getWidth() <= 1 || window.getHeight() <= 1) {
            return false;
        }
        return !(window instanceof Frame frame && (frame.getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED);
    }

    // ==================== Loại thông báo ====================

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum NotificationType {
        SUCCESS(ModernColors::getSuccess, "✓", CommonMessageKeys.GENERAL_SUCCESS),
        INFO(ModernColors::getInfo, "i", CommonMessageKeys.GENERAL_INFO),
        WARNING(ModernColors::getWarning, "!", CommonMessageKeys.GENERAL_WARNING),
        ERROR(ModernColors::getError, "✕", CommonMessageKeys.GENERAL_ERROR);

        private final Supplier<Color> colorSupplier;
        private final String icon;
        private final String titleKey;

        public Color getColor() {
            return colorSupplier.get();
        }

        public String getDefaultTitle() {
            return CommonI18n.get(titleKey);
        }
    }

    // ==================== công cộng API ====================

    public static void setDefaultPosition(NotificationPosition position) {
        DEFAULT.defaultPosition = position != null ? position : NotificationPosition.BOTTOM_RIGHT;
    }

    public static void showSuccess(String message) {
        DEFAULT.show(message, NotificationType.SUCCESS, 2, DEFAULT.defaultPosition, null);
    }

    public static void showInfo(String message) {
        DEFAULT.show(message, NotificationType.INFO, 2, DEFAULT.defaultPosition, null);
    }

    public static void showWarning(String message) {
        DEFAULT.show(message, NotificationType.WARNING, 3, DEFAULT.defaultPosition, null);
    }

    public static void showError(String message) {
        DEFAULT.show(message, NotificationType.ERROR, 3, DEFAULT.defaultPosition, null);
    }

    public static void showCloseable(String message, NotificationType type, int seconds) {
        DEFAULT.show(message, type, seconds, DEFAULT.defaultPosition, null);
    }

    public static void showLongMessage(String message, NotificationType type) {
        DEFAULT.show(message, type, 5, DEFAULT.defaultPosition, null);
    }

    public static void showToast(String message, NotificationType type, int seconds) {
        DEFAULT.show(message, type, seconds, DEFAULT.defaultPosition, null);
    }

    public static void showToast(String message, NotificationType type, int seconds, NotificationPosition position) {
        DEFAULT.show(message, type, seconds, position, null);
    }

    public static void showToast(String message, NotificationType type, int seconds, String title) {
        DEFAULT.show(message, type, seconds, DEFAULT.defaultPosition, title);
    }

    // ==================== Được triển khai nội bộ ====================

    private void show(String message, NotificationType type, int seconds,
                      NotificationPosition position, String title) {
        SwingUtilities.invokeLater(() -> {
            Window mainFrame = getMainFrame();
            ToastWindow toast = new ToastWindow(
                    mainFrame,
                    message,
                    title,
                    type != null ? type : NotificationType.INFO,
                    seconds,
                    position != null ? position : defaultPosition,
                    toastStack::remove,
                    toastStack::refreshPositions
            );
            toastStack.add(toast);
            toast.startShow();
        });
    }

    static Color toastTitleColor(NotificationType type) {
        return ToastStyle.titleColor(type);
    }

    static Color toastHeaderBackgroundColor(NotificationType type) {
        return ToastStyle.headerBackgroundColor(type);
    }
}
