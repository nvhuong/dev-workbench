package com.laker.postman.panel;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.panel.sidebar.SidebarTabPanel;

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
