package com.nvhuong.devworkbench.common.themes;

import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.SystemInfo;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.util.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Trình quản lý chủ đề đơn giản
 * Hỗ trợ chuyển đổi chủ đề sáng và tối
 */
@Slf4j
@UtilityClass
public class SimpleThemeManager {

    private static final String THEME_SETTING_KEY = "ui.theme";

    private static String currentThemeId = ThemeRegistry.DEFAULT_THEME_ID;
    private static final Set<String> registeredDefaultsSources = new HashSet<>();

    /**
     * Khởi tạo chủ đề (được gọi khi ứng dụng khởi động)
     */
    public static void initTheme() {
        // Từ UserSettings để đọc chủ đề được chọn cuối cùng
        String savedTheme = UserPreferencesStore.getString(THEME_SETTING_KEY);
        ThemeDescriptor theme = ThemeRegistry.getOrDefault(savedTheme);
        currentThemeId = theme.id();
        applyTheme(theme, false);
    }

    public static void switchTheme(String themeId) {
        ThemeRegistry.find(themeId).ifPresent(theme -> {
            if (!theme.id().equals(currentThemeId)) {
                switchWithAnimation(() -> applyTheme(theme, true));
            }
        });
    }

    public static List<ThemeDescriptor> availableThemes() {
        return ThemeRegistry.all();
    }

    public static String currentThemeId() {
        return currentThemeId;
    }

    private static ThemeDescriptor currentTheme() {
        return ThemeRegistry.getOrDefault(currentThemeId);
    }

    private static void applyEditorThemeResources(ThemeDescriptor theme) {
        EditorThemeUtil.configureEditorFontApplier(EditorFontManager::applyConfiguredEditorFont);
        if (theme != null) {
            EditorThemeUtil.configureThemeResources(
                    theme.editorThemeResourcePath(),
                    theme.fallbackEditorThemeResourcePath()
            );
        }
    }

    /**
     * Thực hiện chuyển đổi chủ đề bằng cách sử dụng chuyển tiếp hoạt hình.
     * Đầu tiên hãy chụp nhanh giao diện hiện tại rồi chuyển nó invokeLater Việc chuyển đổi được thực hiện ở khung tiếp theo.
     * Đảm bảo hoạt ảnh bắt đầu sau khi ảnh chụp nhanh được hiển thị hoàn toàn để tránh điều này EDT Việc chặn khiến hoạt ảnh không thành công.
     */
    private static void switchWithAnimation(Runnable themeSwitch) {
        FlatAnimatedLafChange.showSnapshot();
        SwingUtilities.invokeLater(() -> {
            themeSwitch.run();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        });
    }

    /**
     * Lấy chủ đề hiện tại
     */
    public static boolean isDarkTheme() {
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf instanceof FlatLaf) {
            return FlatLaf.isLafDark();
        }
        return currentTheme().dark();
    }

    /**
     * Áp dụng chủ đề
     *
     * @param theme            Tên chủ đề
     * @param showNotification Có hiển thị thông báo hay không
     */
    private static void applyTheme(ThemeDescriptor theme, boolean showNotification) {
        try {
            ensureDefaultsSourceRegistered(theme);

            boolean success = theme.applyLookAndFeel();
            if (success) {
                currentThemeId = theme.id();
                applyEditorThemeResources(theme);

                // Look and Feel Công tắc sẽ được xây dựng lại UIDefaults，Khôi phục phông chữ của người dùng trước rồi làm mới cửa sổ.
                FontManager.captureLookAndFeelDefaultFont();
                FontManager.installSavedFontDefaults();
                ToolWindowSurfaceStyle.installGlobalDialogWindowChrome();

                if (showNotification) {
                    UserPreferencesStore.put(THEME_SETTING_KEY, theme.id());
                }

                // Bắt đầu không có cửa sổ; chỉ làm mới các cửa sổ hiện có sau khi chuyển đổi chủ đề trong thời gian chạy.
                if (showNotification) {
                    updateAllWindows();
                }

                log.info("Applied theme: {}", theme.id());

                if (showNotification) {
                    NotificationCenter.showSuccess(switchNotificationMessage(theme));
                }
            }
        } catch (Exception e) {
            log.error("Failed to apply theme: {}", theme.id(), e);
            if (showNotification) {
                NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.GENERAL_ERROR_MESSAGE, e.getMessage()));
            }
        }
    }

    private static void ensureDefaultsSourceRegistered(ThemeDescriptor theme) {
        String defaultsSource = theme.flatLafDefaultsSource();
        if (defaultsSource == null || defaultsSource.isBlank() || registeredDefaultsSources.contains(defaultsSource)) {
            return;
        }
        FlatLaf.registerCustomDefaultsSource(defaultsSource);
        registeredDefaultsSources.add(defaultsSource);
    }

    private static String switchNotificationMessage(ThemeDescriptor theme) {
        if (theme.dark()) {
            return I18nUtil.getMessage(MessageKeys.THEME_SWITCHED_TO_DARK);
        }
        return I18nUtil.getMessage(MessageKeys.THEME_SWITCHED_TO_LIGHT);
    }

    /**
     * Cập nhật tất cả các cửa sổ đang mở
     */
    private static void updateAllWindows() {
        // macOS Xử lý đặc biệt: Cập nhật chế độ xuất hiện cửa sổ
        if (SystemInfo.isMacOS) {
            for (Window window : Window.getWindows()) {
                if (window instanceof JFrame frame) {
                    JRootPane rootPane = frame.getRootPane();

                    // cài đặt macOS Chế độ xuất hiện cửa sổ:dark hoặc light
                    // NSAppearanceNameVibrantDark: Giao diện tối với văn bản trên thanh tiêu đề màu trắng
                    // NSAppearanceNameVibrantLight: Ngoại hình sáng sủa, văn bản trên thanh tiêu đề có màu đen
                    if (isDarkTheme()) {
                        rootPane.putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameVibrantDark");
                    } else {
                        rootPane.putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameVibrantLight");
                    }
                }
            }
        }

        // Cập nhật tất cả các cửa sổ bằng trình quản lý làm mới thống nhất
        UIRefreshManager.refreshAllWindows();
    }
}
