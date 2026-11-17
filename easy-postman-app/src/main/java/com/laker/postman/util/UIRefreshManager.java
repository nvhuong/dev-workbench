package com.laker.postman.util;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.IRefreshable;
import com.laker.postman.common.component.SyntaxEditorScrollPane;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.frame.MainFrame;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * UI Làm mới người quản lý
 * <p>
 * Quản lý mọi thứ ở một nơi UI Logic làm mới của thành phần hỗ trợ các tình huống sau:
 * <ul>
 *   <li>Chuyển đổi chủ đề - Cập nhật phong cách xuất hiện của tất cả các thành phần</li>
 *   <li>Chuyển đổi phông chữ - Phông chữ được cập nhật cho tất cả các thành phần</li>
 *   <li>Chuyển đổi ngôn ngữ - Cập nhật tất cả nội dung văn bản</li>
 * </ul>
 *
 * @author laker
 */
@Slf4j
@UtilityClass
public class UIRefreshManager {

    /**
     * Làm mới tất cả các cửa sổ (được gọi khi chủ đề hoặc phông chữ được chuyển đổi)
     * <p>
     * Phương pháp này sẽ:
     * <ul>
     *   <li>Cập nhật tất cả các thành phần theo cách đệ quy UI（Áp dụng mới Look and Feel）</li>
     *   <li>Xác nhận lại bố cục</li>
     *   <li>Vẽ lại tất cả các thành phần</li>
     * </ul>
     * <p>
     * Các tình huống áp dụng: chuyển đổi chủ đề, chuyển đổi phông chữ
     */
    public static void refreshAllWindows() {
        try {
            Window[] windows = Window.getWindows();
            FlatLaf.updateUI();
            for (Window window : windows) {
                if (window.isDisplayable()) {
                    refreshEditorThemes(window);

                    if (window instanceof MainFrame mainFrame) {
                        mainFrame.refreshWindowChrome();
                    } else if (window instanceof JDialog dialog) {
                        ToolWindowSurfaceStyle.applyDialogWindowChrome(dialog);
                    }

                    // Xác nhận lại bố cục
                    window.validate();

                    // Vẽ lại cửa sổ
                    window.repaint();
                    log.debug("Refreshed window: {}", window.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to refresh windows", e);
        }
    }

    /**
     * Chỉ làm mới cài đặt phông chữ của trình soạn thảo.
     * <p>
     * Thay đổi phông chữ của trình soạn thảo không yêu cầu cài đặt lại Look and Feel。Tránh gọi điện {@link FlatLaf#updateUI()}，
     * Ngăn không cho cài đặt lại các thành phần không phải trình chỉnh sửa như tab yêu cầu, tab phản hồi và thanh công cụ trong thời gian chạy UI delegate Màu trạng thái tùy chỉnh bị mất.
     */
    public static void refreshEditorFonts() {
        try {
            Window[] windows = Window.getWindows();
            int refreshedCount = 0;

            for (Window window : windows) {
                if (window.isDisplayable()) {
                    refreshEditorFonts(window);
                    window.validate();
                    window.repaint();
                    refreshedCount++;
                    log.debug("Refreshed editor fonts in window: {}", window.getClass().getSimpleName());
                }
            }

            log.debug("Successfully refreshed editor fonts in {} window(s)", refreshedCount);
        } catch (Exception e) {
            log.error("Failed to refresh editor fonts", e);
        }
    }

    static void refreshEditorThemes(Component component) {
        visitComponentTree(component, UIRefreshManager::refreshEditorThemeComponent);
    }

    private static void refreshEditorThemeComponent(Component component) {
        if (component == null) {
            return;
        }

        if (component instanceof RSyntaxTextArea textArea) {
            try {
                EditorThemeUtil.loadTheme(textArea);
                log.debug("Refreshed editor theme: {}", component.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("Failed to refresh editor theme: {}", component.getClass().getSimpleName(), e);
            }
        }

        if (component instanceof SyntaxEditorScrollPane syntaxEditorScrollPane) {
            syntaxEditorScrollPane.refreshEditorChrome();
        } else if (component instanceof RTextScrollPane scrollPane) {
            EditorThemeUtil.applyScrollPaneChrome(scrollPane);
        }
    }

    static void refreshEditorFonts(Component component) {
        visitComponentTree(component, UIRefreshManager::refreshEditorFontComponent);
    }

    private static void refreshEditorFontComponent(Component component) {
        if (component == null) {
            return;
        }

        if (component instanceof RSyntaxTextArea textArea) {
            try {
                EditorFontManager.applyConfiguredEditorFont(textArea);
                textArea.revalidate();
                textArea.repaint();
                log.debug("Refreshed editor font: {}", component.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("Failed to refresh editor font: {}", component.getClass().getSimpleName(), e);
            }
        }
    }

    private static void visitComponentTree(Component component, Consumer<Component> visitor) {
        if (component == null) {
            return;
        }
        visitor.accept(component);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                visitComponentTree(child, visitor);
            }
        }

        if (component instanceof JFrame frame) {
            JMenuBar menuBar = frame.getJMenuBar();
            if (menuBar != null) {
                visitComponentTree(menuBar, visitor);
            }
        }
    }

    /**
     * Làm mới ngôn ngữ (được gọi khi chuyển đổi ngôn ngữ)
     * <p>
     * Phương pháp này sẽ:
     * <ul>
     *   <li>Làm mới đệ quy được thực hiện IRefreshable Các thành phần của giao diện (bao gồm cập nhật tiêu đề cửa sổ)</li>
     *   <li>Xác nhận lại bố cục và vẽ lại</li>
     * </ul>
     * <p>
     * Các tình huống áp dụng: chuyển đổi ngôn ngữ
     */
    public static void refreshLanguage() {
        log.info("Refreshing UI for language change...");

        try {
            Window[] windows = Window.getWindows();
            int refreshedCount = 0;

            for (Window window : windows) {
                if (window.isDisplayable()) {
                    // 1. Làm mới đệ quy tất cả các triển khai IRefreshable thành phần
                    refreshComponentRecursively(window);

                    // 2. Xác nhận lại bố cục
                    window.validate();

                    // 3. Vẽ lại cửa sổ
                    window.repaint();

                    refreshedCount++;
                    log.debug("Refreshed window for language: {}", window.getClass().getSimpleName());
                }
            }

            log.info("Successfully refreshed {} window(s) for language change", refreshedCount);
        } catch (Exception e) {
            log.error("Failed to refresh UI for language change", e);
        }
    }


    /**
     * Làm mới các thành phần theo cách đệ quy
     * <p>
     * Duyệt qua cây thành phần và triển khai nó IRefreshable Cuộc gọi thành phần giao diện refresh() phương pháp
     *
     * @param component Thành phần cần làm mới
     */
    private static void refreshComponentRecursively(Component component) {
        if (component == null) {
            return;
        }

        // Nếu thành phần này được triển khai IRefreshable giao diện, gọi nó refresh phương pháp
        if (component instanceof IRefreshable refreshable) {
            try {
                refreshable.refresh();
                log.debug("Refreshed component: {}", component.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("Failed to refresh component: {}", component.getClass().getSimpleName(), e);
            }
        }

        // Xử lý các thành phần con một cách đệ quy
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                refreshComponentRecursively(child);
            }
        }

        // Xử lý thanh menu (JFrame Thanh menu không có ở đó contentPane giữa)
        if (component instanceof JFrame frame) {
            JMenuBar menuBar = frame.getJMenuBar();
            if (menuBar != null) {
                refreshComponentRecursively(menuBar);
            }
        }
    }

}
