package com.laker.postman.plugin.kafka.shared.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.IconUtil;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;

@UtilityClass
public class KafkaSwingSupport {

    private static final int TOGGLE_BUTTON_SIZE = 28;
    private static final int TOGGLE_ICON_SIZE = 16;

    public static void onTextChange(JTextComponent component, Runnable action) {
        component.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    /**
     * Nút chuyển đổi biểu tượng kiểu thanh công cụ. Biểu tượng đã chọn được vẽ theo chủ đề toolbar Chọn màu nền trước (Easy Sáng/Chủ đề tối với màu chủ đạo và biểu tượng màu trắng).
     * Khi chủ đề không được xác định, nó sẽ trở lại màu nền trước bình thường, đảm bảo rằng nó hiển thị theo bất kỳ chủ đề nào.
     */
    public static JToggleButton toolbarToggle(String iconPath, String tooltip) {
        JToggleButton button = new JToggleButton();
        button.setIcon(IconUtil.createThemed(iconPath, TOGGLE_ICON_SIZE, TOGGLE_ICON_SIZE));
        FlatSVGIcon selectedIcon = IconUtil.createThemed(iconPath, TOGGLE_ICON_SIZE, TOGGLE_ICON_SIZE);
        selectedIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> selectedToolbarForeground()));
        button.setSelectedIcon(selectedIcon);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(TOGGLE_BUTTON_SIZE, TOGGLE_BUTTON_SIZE));
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        return button;
    }

    private static Color selectedToolbarForeground() {
        Color color = UIManager.getColor("ToggleButton.toolbar.selectedForeground");
        return color != null ? color : UIManager.getColor("Label.foreground");
    }
}
