package com.nvhuong.devworkbench.panel.collections;

import com.nvhuong.devworkbench.common.UiSingletonPanel;
import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.common.component.AppToolWindowChrome;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel;
import com.nvhuong.devworkbench.panel.collections.editor.RequestEditorPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Không gian làm việc thu thập yêu cầu hiển thị cây bộ sưu tập ở bên trái và trình chỉnh sửa yêu cầu ở bên phải.
 */
public class RequestCollectionsPanel extends UiSingletonPanel {
    private static final int DEFAULT_DIVIDER_LOCATION = AppToolWindowChrome.DEFAULT_SIDE_WIDTH;

    @Override
    protected void initUI() {
        ToolWindowSurfaceStyle.applyBackground(this);
        setLayout(new BorderLayout());
        add(createCollectionsSplitPane(), BorderLayout.CENTER);
    }

    private JSplitPane createCollectionsSplitPane() {
        CollectionTreePanel collectionTreePanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        RequestEditorPanel requestEditorPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);

        JSplitPane splitPane = createCollectionsSplitPane(collectionTreePanel, requestEditorPanel);
        return splitPane;
    }

    static JSplitPane createCollectionsSplitPane(Component collectionTreePanel, Component requestEditorPanel) {
        return AppToolWindowChrome.createHorizontalCardSplitPane(
                collectionTreePanel,
                requestEditorPanel,
                DEFAULT_DIVIDER_LOCATION
        );
    }

    static JComponent createCollectionToolWindow(Component collectionTreePanel) {
        return AppToolWindowChrome.wrapLeftInsetToolWindow(collectionTreePanel);
    }

    static JComponent createRequestEditorToolWindow(Component requestEditorPanel) {
        return AppToolWindowChrome.wrapRightToolWindow(requestEditorPanel);
    }

    @Override
    protected void registerListeners() {
        // no op
    }
}
