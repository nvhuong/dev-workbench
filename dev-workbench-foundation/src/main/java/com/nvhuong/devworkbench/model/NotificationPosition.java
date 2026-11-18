package com.nvhuong.devworkbench.model;

import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Thông báo liệt kê vị trí
 * Chứa chỉ mục tuần tự về vị trí và khóa quốc tế hóa
 */
@Getter
@RequiredArgsConstructor
public enum NotificationPosition {
    TOP_RIGHT(0, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_TOP_RIGHT),
    TOP_CENTER(1, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_TOP_CENTER),
    TOP_LEFT(2, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_TOP_LEFT),
    BOTTOM_RIGHT(3, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_BOTTOM_RIGHT),
    BOTTOM_CENTER(4, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_BOTTOM_CENTER),
    BOTTOM_LEFT(5, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_BOTTOM_LEFT),
    CENTER(6, MessageKeys.SETTINGS_GENERAL_NOTIFICATION_POSITION_CENTER);

    private final int index;
    private final String i18nKey;

    /**
     * Nhận vị trí dựa trên chỉ số
     */
    public static NotificationPosition fromIndex(int index) {
        for (NotificationPosition position : values()) {
            if (position.index == index) {
                return position;
            }
        }
        return BOTTOM_RIGHT; // Mặc định là góc dưới bên phải
    }

    /**
     * Nhận vị trí dựa trên tên
     */
    public static NotificationPosition fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return BOTTOM_RIGHT; // Mặc định là góc dưới bên phải
        }
    }
}