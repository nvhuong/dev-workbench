package com.nvhuong.devworkbench.panel.collections.editor;

import cn.hutool.core.util.IdUtil;
import com.nvhuong.devworkbench.common.component.tab.ClosableTabComponent;
import com.nvhuong.devworkbench.common.component.tab.PlusTabComponent;
import com.nvhuong.devworkbench.panel.collections.editor.request.RequestEditSubPanel;
import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.function.BooleanSupplier;

/**
 * Request an editor Tab Bộ điều khiển vòng đời.
 * <p>
 * Tạo một yêu cầu mới Tab、Kết thúc bảo trì +Tab Bản thân những hành vi này không phải là một phần của bố cục bảng điều khiển và dễ xác nhận hơn khi được tập trung hóa Tab Quy tắc trình tự.
 */
@RequiredArgsConstructor
final class RequestEditorTabLifecycleController {
    private final JTabbedPane tabbedPane;
    private final String plusTabTitle;
    private final String defaultRequestTitle;
    private final BooleanSupplier autoInitializeSelectedTabOnAdd;
    private final Runnable selectedTabInitializer;

    RequestEditSubPanel addNewRequestTab(String title, RequestItemProtocolEnum protocol) {
        removeTrailingPlusTabIfPresent();

        String tabTitle = title != null ? title : defaultRequestTitle;
        RequestEditSubPanel subPanel = new RequestEditSubPanel(IdUtil.simpleUUID(), protocol);
        tabbedPane.addTab(tabTitle, subPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1,
                ClosableTabComponent.forRequest(tabTitle, "GET", protocol));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

        addPlusTab();
        RequestEditorTabInserter.setTabNewRequestMarker(subPanel, true);
        return subPanel;
    }

    void addPlusTab() {
        if (tabbedPane.getTabCount() > 0 && isPlusTab(tabbedPane.getTabCount() - 1)) {
            if (autoInitializeSelectedTabOnAdd.getAsBoolean()) {
                selectedTabInitializer.run();
            }
            return;
        }
        tabbedPane.addTab(plusTabTitle, new RequestEditorEmptyStatePanel());
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new PlusTabComponent());
    }

    boolean isPlusTab(int index) {
        if (index < 0 || index >= tabbedPane.getTabCount()) {
            return false;
        }
        return plusTabTitle.equals(tabbedPane.getTitleAt(index));
    }

    private void removeTrailingPlusTabIfPresent() {
        if (tabbedPane.getTabCount() > 0 && isPlusTab(tabbedPane.getTabCount() - 1)) {
            tabbedPane.removeTabAt(tabbedPane.getTabCount() - 1);
        }
    }
}
