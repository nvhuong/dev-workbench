package com.nvhuong.devworkbench.common.component.button;

import com.nvhuong.devworkbench.util.CommonI18n;
import com.nvhuong.devworkbench.util.CommonMessageKeys;
import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút xóa
 * Được sử dụng để xóa bảng điều khiển hoặc nội dung khác
 */
public class ClearButton extends JButton {
    /**
     * Hàm tạo mặc định: đã sử dụng SMALL biểu tượng
     */
    public ClearButton() {
        this(IconUtil.SIZE_MEDIUM);
    }

    /**
     * Trình xây dựng tùy chỉnh
     *
     * @param iconSize Kích thước biểu tượng
     */
    public ClearButton(int iconSize) {
        super();
        setIcon(IconUtil.createThemed("icons/clear.svg", iconSize, iconSize));
        setToolTipText(CommonI18n.get(CommonMessageKeys.BUTTON_CLEAR));
        // Thiết kế phẳng
        setFocusable(false);// Xóa đường viền tiêu điểm của nút
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }
}
