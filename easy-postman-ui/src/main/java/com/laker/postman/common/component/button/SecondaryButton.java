package com.laker.postman.common.component.button;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Nút phụ - Thiết kế hiện đại
 * Hỗ trợ đầy đủ việc thích ứng chủ đề sáng và tối
 * Được sử dụng cho các hoạt động phụ (chẳng hạn như hủy, lưu, v.v.)
 *
 * @author laker
 */
public class SecondaryButton extends JButton {
    private static final int ICON_SIZE = 14;

    public SecondaryButton(String text) {
        this(text, null);
    }

    public SecondaryButton(String text, String iconPath) {
        super(text);

        if (iconPath != null && !iconPath.isEmpty()) {
            setIcon(IconUtil.createThemed(iconPath, ICON_SIZE, ICON_SIZE));
            setIconTextGap(4);
        }

        // Đặt phông chữ và phong cách
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        putClientProperty(FlatClientProperties.STYLE_CLASS, ModernButtonFactory.SECONDARY_STYLE_CLASS);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRolloverEnabled(true);
    }
}
