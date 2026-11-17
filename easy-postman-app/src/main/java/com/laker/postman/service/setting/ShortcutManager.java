package com.laker.postman.service.setting;

import com.laker.postman.common.constants.ConfigPathConstants;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Trình quản lý phím tắt
 * Quản lý toàn bộ cấu hình phím tắt của ứng dụng và hỗ trợ người dùng tùy chỉnh
 */
@UtilityClass
public class ShortcutManager {
    private static final String SHORTCUT_CONFIG_FILE = ConfigPathConstants.SHORTCUTS;
    private static final Properties props = new Properties();
    private static final Map<String, ShortcutConfig> shortcuts = new HashMap<>();

    // phím tắtIDkhông thay đổi
    public static final String SEND_REQUEST = "send_request";
    public static final String NEW_REQUEST = "new_request";
    public static final String SAVE_REQUEST = "save_request";
    public static final String CLOSE_CURRENT_TAB = "close_current_tab";
    public static final String CLOSE_OTHER_TABS = "close_other_tabs";
    public static final String CLOSE_ALL_TABS = "close_all_tabs";
    public static final String EXIT_APP = "exit_app";

    static {
        initDefaultShortcuts();
        load();
    }

    /**
     * Khởi tạo các phím tắt mặc định
     */
    private static void initDefaultShortcuts() {
        int cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        shortcuts.put(SEND_REQUEST, new ShortcutConfig(
                SEND_REQUEST, "send_request",
                KeyEvent.VK_ENTER, cmdMask, "Ctrl+Enter", "Cmd+Enter"
        ));

        shortcuts.put(NEW_REQUEST, new ShortcutConfig(
                NEW_REQUEST, "new_request",
                KeyEvent.VK_N, cmdMask, "Ctrl+N", "Cmd+N"
        ));

        shortcuts.put(SAVE_REQUEST, new ShortcutConfig(
                SAVE_REQUEST, "save_request",
                KeyEvent.VK_S, cmdMask, "Ctrl+S", "Cmd+S"
        ));

        shortcuts.put(CLOSE_CURRENT_TAB, new ShortcutConfig(
                CLOSE_CURRENT_TAB, "close_current_tab",
                KeyEvent.VK_W, cmdMask, "Ctrl+W", "Cmd+W"
        ));

        shortcuts.put(CLOSE_OTHER_TABS, new ShortcutConfig(
                CLOSE_OTHER_TABS, "close_other_tabs",
                KeyEvent.VK_W, cmdMask | InputEvent.ALT_DOWN_MASK, "Ctrl+Alt+W", "Cmd+Option+W"
        ));

        shortcuts.put(CLOSE_ALL_TABS, new ShortcutConfig(
                CLOSE_ALL_TABS, "close_all_tabs",
                KeyEvent.VK_W, cmdMask | InputEvent.SHIFT_DOWN_MASK, "Ctrl+Shift+W", "Cmd+Shift+W"
        ));

        shortcuts.put(EXIT_APP, new ShortcutConfig(
                EXIT_APP, "exit_app",
                KeyEvent.VK_Q, cmdMask, "Ctrl+Q", "Cmd+Q"
        ));
    }

    /**
     * Tải cấu hình phím tắt từ tập tin
     */
    public static void load() {
        File file = new File(SHORTCUT_CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                // Áp dụng cấu hình đã tải
                for (String key : shortcuts.keySet()) {
                    String keyCodeStr = props.getProperty(key + ".keyCode");
                    String modifiersStr = props.getProperty(key + ".modifiers");
                    if (keyCodeStr != null && modifiersStr != null) {
                        try {
                            int keyCode = Integer.parseInt(keyCodeStr);
                            int modifiers = Integer.parseInt(modifiersStr);
                            shortcuts.get(key).setKeyCode(keyCode);
                            shortcuts.get(key).setModifiers(modifiers);
                        } catch (NumberFormatException e) {
                            // Bỏ qua các lỗi phân tích cú pháp và sử dụng các giá trị mặc định
                        }
                    }
                }
            } catch (IOException e) {
                // Bỏ qua và sử dụng cấu hình mặc định
            }
        }
    }

    /**
     * Lưu cấu hình phím tắt vào một tập tin
     */
    public static void save() {
        for (Map.Entry<String, ShortcutConfig> entry : shortcuts.entrySet()) {
            String key = entry.getKey();
            ShortcutConfig config = entry.getValue();
            props.setProperty(key + ".keyCode", String.valueOf(config.getKeyCode()));
            props.setProperty(key + ".modifiers", String.valueOf(config.getModifiers()));
        }

        try (FileOutputStream fos = new FileOutputStream(SHORTCUT_CONFIG_FILE)) {
            props.store(fos, "EasyPostman Keyboard Shortcuts");
        } catch (IOException e) {
            // Bỏ qua lỗi lưu
        }
    }

    /**
     * Nhận cấu hình phím tắt
     */
    public static ShortcutConfig getShortcut(String id) {
        return shortcuts.get(id);
    }

    /**
     * Lấy KeyStroke
     */
    public static KeyStroke getKeyStroke(String id) {
        ShortcutConfig config = shortcuts.get(id);
        if (config != null && config.isAssigned()) {
            return KeyStroke.getKeyStroke(config.getKeyCode(), config.getModifiers());
        }
        return null;
    }

    /**
     * Đặt phím tắt
     */
    public static void setShortcut(String id, int keyCode, int modifiers) {
        ShortcutConfig config = shortcuts.get(id);
        if (config != null) {
            config.setKeyCode(keyCode);
            config.setModifiers(modifiers);
        }
    }

    /**
     * Xóa các phím tắt (để trống) để tránh xung đột với các phím tắt chung của phần mềm khác
     */
    public static void clearShortcut(String id) {
        setShortcut(id, KeyEvent.VK_UNDEFINED, 0);
    }

    public static boolean isAssigned(String id) {
        ShortcutConfig config = shortcuts.get(id);
        return config != null && config.isAssigned();
    }

    /**
     * Lấy văn bản hiển thị của phím tắt (theo hệ điều hành), trả về chuỗi trống nếu không được đặt
     */
    public static String getShortcutText(String id) {
        ShortcutConfig config = shortcuts.get(id);
        if (config != null) {
            return config.getDisplayText();
        }
        return "";
    }

    /**
     * Đặt lại về phím tắt mặc định
     */
    public static void resetToDefaults() {
        shortcuts.clear();
        initDefaultShortcuts();
        save();
    }

    /**
     * Kiểm tra xung đột phím tắt
     */
    public static String checkConflict(String excludeId, int keyCode, int modifiers) {
        if (keyCode == KeyEvent.VK_UNDEFINED) {
            return null;
        }
        for (Map.Entry<String, ShortcutConfig> entry : shortcuts.entrySet()) {
            if (!entry.getKey().equals(excludeId)) {
                ShortcutConfig config = entry.getValue();
                if (config.getKeyCode() == keyCode && config.getModifiers() == modifiers) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Lớp cấu hình phím tắt
     */
    @Getter
    public static class ShortcutConfig {
        private final String id;
        private final String nameKey;
        @Setter
        private int keyCode;
        @Setter
        private int modifiers;
        private final String windowsText;
        private final String macText;

        public ShortcutConfig(String id, String nameKey, int keyCode, int modifiers,
                              String windowsText, String macText) {
            this.id = id;
            this.nameKey = nameKey;
            this.keyCode = keyCode;
            this.modifiers = modifiers;
            this.windowsText = windowsText;
            this.macText = macText;
        }

        public boolean isAssigned() {
            return keyCode != KeyEvent.VK_UNDEFINED;
        }

        /**
         * Lấy văn bản hiển thị (tùy thuộc vào hệ điều hành), trả về một chuỗi trống nếu không được đặt
         */
        public String getDisplayText() {
            if (!isAssigned()) {
                return "";
            }
            boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
            StringBuilder displayText = new StringBuilder();

            int cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

            if ((modifiers & cmdMask) != 0) {
                displayText.append(isMac ? "Cmd+" : "Ctrl+");
            }
            if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
                displayText.append("Shift+");
            }
            if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
                displayText.append(isMac ? "Option+" : "Alt+");
            }
            if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0 && (modifiers & cmdMask) == 0) {
                displayText.append("Ctrl+");
            }

            displayText.append(KeyEvent.getKeyText(keyCode));

            return displayText.toString();
        }
    }
}
