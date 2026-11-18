package com.nvhuong.devworkbench.panel.sidebar;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * Thanh bên Tab Siêu dữ liệu và nội dung bảng điều khiển được tải một cách lười biếng theo yêu cầu.
 */
@Getter
public class TabInfo {
    @Setter
    private String title;
    private final Icon icon;
    private final Supplier<JPanel> panelSupplier;
    private JPanel panel;

    public TabInfo(String title, Icon icon, Supplier<JPanel> panelSupplier) {
        this.title = title;
        this.icon = icon;
        this.panelSupplier = panelSupplier;
    }

    public JPanel getPanel() {
        if (panel == null) {
            panel = panelSupplier.get();
        }
        return panel;
    }
}
