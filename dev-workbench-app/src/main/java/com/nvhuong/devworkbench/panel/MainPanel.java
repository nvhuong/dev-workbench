package com.nvhuong.devworkbench.panel;

import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.UiSingletonPanel;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.panel.sidebar.SidebarTabPanel;

import java.awt.*;

/**
 * Vùng chứa không gian làm việc chính hiện lưu trữ các tab thanh bên và vùng nội dung tương ứng của chúng.
 */
public class MainPanel extends UiSingletonPanel {

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyBackground(this);
        add(UiSingletonFactory.getInstance(SidebarTabPanel.class), BorderLayout.CENTER);
    }

    @Override
    protected void registerListeners() {
        // no-op
    }
}
