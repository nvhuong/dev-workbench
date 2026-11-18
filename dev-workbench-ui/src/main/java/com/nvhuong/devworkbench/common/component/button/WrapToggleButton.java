package com.nvhuong.devworkbench.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.UiI18n;
import com.nvhuong.devworkbench.util.UiMessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * Nút chuyển đổi ngắt dòng tự động
 * Được sử dụng để chuyển đổi chức năng gói từ tự động của trình soạn thảo văn bản
 */
public class WrapToggleButton extends JToggleButton {

    private static final String ICON_PATH = "icons/wrap.svg";

    public WrapToggleButton() {
        setToolTipText(UiI18n.get(UiMessageKeys.BUTTON_TOGGLE_LINE_WRAP));
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        setSelected(false);
        updateIcon(false);

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
