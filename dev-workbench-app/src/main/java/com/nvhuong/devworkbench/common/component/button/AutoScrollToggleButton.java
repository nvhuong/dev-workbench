package com.nvhuong.devworkbench.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút chuyển đổi tự động cuộn
 * Tính năng tự động cuộn để chuyển đổi bảng điều khiển
 */
public class AutoScrollToggleButton extends JToggleButton {

    private static final String ICON_PATH = "icons/auto-scroll.svg";

    public AutoScrollToggleButton() {
        super();
        setToolTipText("Auto-scroll to bottom");
        setPreferredSize(new Dimension(28, 28));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        setSelected(true); // Tính năng tự động cuộn được bật theo mặc định
        updateIcon(true);

        // Thêm trình xử lý thay đổi trạng thái để tự động cập nhật màu biểu tượng
        addItemListener(e -> updateIcon(isSelected()));
    }

    /**
     * Cập nhật màu biểu tượng dựa trên trạng thái đã chọn của nút
     *
     * @param selected Đã kiểm tra hay chưa
     */
    private void updateIcon(boolean selected) {
        if (selected) {
            setIcon(IconUtil.createOnPrimary(ICON_PATH, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        } else {
            setIcon(IconUtil.createThemed(ICON_PATH, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        }
    }
}
