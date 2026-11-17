package com.laker.postman.panel.collections.editor;

import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.common.component.tab.RequestTabMarkers;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Request an editor Tab Bộ điều khiển nhà nước.
 * <p>
 * Tiêu đề, nhận dạng giao thức, chấm đỏ, lưu đồng bộ hóa sự kiện Tab Việc bảo trì trạng thái được tập trung ở đây để ngăn lớp bảng điều khiển tiếp tục mở rộng.
 */
final class RequestEditorTabStateController {
    private final JTabbedPane tabbedPane;
    private final Consumer<Component> dirtyTransientTabPinAction;

    RequestEditorTabStateController(JTabbedPane tabbedPane) {
        this(tabbedPane, component -> {
        });
    }

    RequestEditorTabStateController(JTabbedPane tabbedPane, Consumer<Component> dirtyTransientTabPinAction) {
        this.tabbedPane = tabbedPane;
        this.dirtyTransientTabPinAction = dirtyTransientTabPinAction == null ? component -> {
        } : dirtyTransientTabPinAction;
    }

    RequestEditSubPanel currentRequestTab() {
        Component component = tabbedPane.getSelectedComponent();
        if (component instanceof RequestEditSubPanel requestEditSubPanel) {
            return requestEditSubPanel;
        }
        return null;
    }

    void updateCurrentRequest(HttpRequestItem item) {
        RequestEditSubPanel subPanel = currentRequestTab();
        if (subPanel != null) {
            subPanel.initPanelData(item);
        }
    }

    void refreshNewRequestTab(String requestName, HttpRequestItem item) {
        int currentTabIndex = tabbedPane.getSelectedIndex();
        if (currentTabIndex < 0) {
            return;
        }
        tabbedPane.setTitleAt(currentTabIndex, requestName);
        if (tabbedPane.getTabComponentAt(currentTabIndex) instanceof ClosableTabComponent) {
            replaceRequestTabComponent(currentTabIndex, requestName, item);
        }
        updateCurrentRequest(item);
        markRequestSaved(currentTabIndex);
    }

    void updateRequestDirty(RequestEditSubPanel panel, boolean dirty) {
        if (dirty) {
            dirtyTransientTabPinAction.accept(panel);
        }
        int index = tabbedPane.indexOfComponent(panel);
        if (index < 0) {
            return;
        }
        Component tabComponent = tabbedPane.getTabComponentAt(index);
        if (tabComponent instanceof ClosableTabComponent closable) {
            closable.updateMarkers(markers -> markers.withDirty(dirty));
        }
    }

    void syncSavedRequest(HttpRequestItem updatedItem) {
        if (updatedItem == null || updatedItem.getId() == null) {
            return;
        }
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            if (!(component instanceof RequestEditSubPanel subPanel)) {
                continue;
            }
            HttpRequestItem tabItem = subPanel.getCurrentRequest();
            if (tabItem != null && updatedItem.getId().equals(tabItem.getId())) {
                updateRequestDirty(subPanel, false);
                subPanel.setOriginalRequestItem(updatedItem);
                replaceRequestTabComponent(i, tabbedPane.getTitleAt(i), updatedItem);
                markRequestSaved(i);
            }
        }
    }

    void updateRequestDisplay(RequestEditSubPanel panel, String method, RequestItemProtocolEnum protocol) {
        int index = tabbedPane.indexOfComponent(panel);
        if (index < 0) {
            return;
        }
        if (!(tabbedPane.getTabComponentAt(index) instanceof ClosableTabComponent)) {
            return;
        }

        String title = tabbedPane.getTitleAt(index);
        replaceRequestTabComponent(index, title, method, protocol);
    }

    void updateGroupTitle(GroupEditPanel panel, String newTitle) {
        int index = tabbedPane.indexOfComponent(panel);
        if (index < 0) {
            return;
        }

        boolean rootGroup = panel.getGroupNode() != null && panel.getGroupNode().getLevel() == 1;
        tabbedPane.setTabComponentAt(index, new ClosableTabComponent(newTitle, null, rootGroup));
        tabbedPane.setToolTipTextAt(index, newTitle);
    }

    void updateAllRequestLayouts(boolean vertical) {
        forEachRequestTab(subPanel -> subPanel.updateLayoutOrientation(vertical));
    }

    void updateAllRequestEditorTabsVisibility() {
        forEachRequestTab(RequestEditSubPanel::updateRequestEditorTabsVisibility);
    }

    private void forEachRequestTab(java.util.function.Consumer<RequestEditSubPanel> action) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            if (component instanceof RequestEditSubPanel subPanel) {
                action.accept(subPanel);
            }
        }
    }

    private void replaceRequestTabComponent(int index, String title, HttpRequestItem item) {
        if (item == null) {
            return;
        }
        replaceRequestTabComponent(index, title, item.getMethod(), item.getProtocol());
    }

    private void replaceRequestTabComponent(int index, String title, String method, RequestItemProtocolEnum protocol) {
        Component tabComponent = tabbedPane.getTabComponentAt(index);
        if (!(tabComponent instanceof ClosableTabComponent previous)) {
            tabbedPane.setTabComponentAt(index, ClosableTabComponent.forRequest(title, method, protocol));
            return;
        }

        ClosableTabComponent updated = ClosableTabComponent.forRequest(title, method, protocol);
        updated.updateMarkers(ignored -> previous.getMarkers());
        tabbedPane.setTabComponentAt(index, updated);
    }

    private void markRequestSaved(int index) {
        if (index < 0 || index >= tabbedPane.getTabCount()) {
            return;
        }
        Component tabComponent = tabbedPane.getTabComponentAt(index);
        if (tabComponent instanceof ClosableTabComponent closable) {
            closable.updateMarkers(RequestTabMarkers::saved);
        }
    }
}
