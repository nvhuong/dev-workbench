package com.laker.postman.common.component.tab;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;

import static com.laker.postman.util.IconUtil.SIZE_MEDIUM;

/**
 * PlusTabComponent Thành phần nhãn được sử dụng để hiển thị biểu tượng dấu cộng
 */
public class PlusTabComponent extends JPanel {
    private static final int TAB_SIZE = 28;

    public PlusTabComponent() {
        setOpaque(false); // Đặt thành nền trong suốt
        setFocusable(false); // Không thể lấy nét
        setLayout(new BorderLayout()); // sử dụng BorderLayout cách trình bày
        Dimension size = new Dimension(TAB_SIZE, TAB_SIZE);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);

        JLabel plusLabel = new JLabel();
        plusLabel.setIcon(IconUtil.createThemed("icons/plus.svg", SIZE_MEDIUM, SIZE_MEDIUM)); // sử dụngSVGbiểu tượng
        plusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        plusLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(plusLabel, BorderLayout.CENTER);
    }
}
