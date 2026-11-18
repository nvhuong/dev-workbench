package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút cộng
 * Được sử dụng cho các hoạt động như tạo và bổ sung mới
 */
public class PlusButton extends JButton {
    /**
     * Hàm tạo mặc định: đã sử dụng MEDIUM Biểu tượng kích thước
     */
    public PlusButton() {
        this(IconUtil.SIZE_MEDIUM);
    }

    /**
     * Trình xây dựng tùy chỉnh
     *
     * @param iconSize Kích thước biểu tượng
     */
    public PlusButton(int iconSize) {
        setIcon(IconUtil.createThemed("icons/plus.svg", iconSize, iconSize));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_ADD));
        // Thiết kế phẳng
        setFocusable(false);// Xóa đường viền tiêu điểm của nút
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
