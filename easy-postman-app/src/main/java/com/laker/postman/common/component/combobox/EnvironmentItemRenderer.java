package com.laker.postman.common.component.combobox;

import com.laker.postman.model.Environment;
import com.laker.postman.environment.EnvironmentItem;

import javax.swing.*;
import java.awt.*;

/**
 * Trình kết xuất thả xuống cho môi trường
 */
public class EnvironmentItemRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // Lấy thành phần cơ sở bằng trình kết xuất mặc định
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof EnvironmentItem item) {
            Environment env = item.getEnvironment();
            label.setText(env.getName());
        }
        return label;
    }
}