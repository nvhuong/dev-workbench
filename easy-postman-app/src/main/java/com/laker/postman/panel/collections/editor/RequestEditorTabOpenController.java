package com.laker.postman.panel.collections.editor;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * Request an editor Tab Bật bộ điều khiển.
 * <p>
 * Mở tạm thời, mở cố định, giống nhau ID Kiểm tra đạo văn và tạm thời Tab Buộc chặt và chèn vào +Tab Tất cả trước đây Tab Quy tắc hành vi không được đặt trong lớp bố trí bảng điều khiển.
 */
@Slf4j
@RequiredArgsConstructor
final class RequestEditorTabOpenController {
    private final JTabbedPane tabbedPane;
    private final RequestEditorTransientTabManager transientTabManager;
    private final RequestEditorExecutionScopeSynchronizer executionScopeSynchronizer;
    private final Runnable beforeOpenAction;
    private final Runnable plusTabRestorer;
    private final IntPredicate plusTabPredicate;
    private final BiFunction<String, RequestItemProtocolEnum, RequestEditSubPanel> newRequestTabCreator;
    private final BiConsumer<HttpRequestItem, RequestEditSubPanel> requestDataInitializer;
    private final String defaultRequestTitle;
    private final CollectionTreeEditorGateway collectionGateway;

    void showOrCreateTransientRequest(HttpRequestItem item) {
        beforeOpenAction.run();
        String requestId = item.getId();
        if (requestId == null || requestId.isEmpty()) {
            RequestEditSubPanel tab = newRequestTabCreator.apply(null, item.getProtocol());
            requestDataInitializer.accept(item, tab);
            return;
        }

        executionScopeSynchronizer.syncScopeForRequest(requestId);
        if (switchToExistingRequestTab(requestId)) {
            return;
        }

        transientTabManager.validate();
        RequestEditSubPanel newPanel = new RequestEditSubPanel(requestId, item.getProtocol());
        newPanel.initPanelData(item);
        transientTabManager.showOrReplace(newPanel, requestTitle(item), newPanel.getCurrentRequest());
    }

    void showOrCreateRequestTab(HttpRequestItem item) {
        beforeOpenAction.run();
        String requestId = item.getId();
        if (requestId == null || requestId.isEmpty()) {
            RequestEditSubPanel tab = newRequestTabCreator.apply(null, item.getProtocol());
            requestDataInitializer.accept(item, tab);
            return;
        }

        executionScopeSynchronizer.syncScopeForRequest(requestId);
        if (transientTabManager.isTransientRequest(requestId)) {
            transientTabManager.pin();
            return;
        }
        if (switchToExistingRequestTab(requestId)) {
            return;
        }

        RequestEditSubPanel subPanel = new RequestEditSubPanel(requestId, item.getProtocol());
        subPanel.initPanelData(item);
        String title = requestTitle(item);
        insertFixedTab(title, subPanel, ClosableTabComponent.forRequest(title, subPanel.getCurrentRequest()));
    }

    void showOrCreateTransientGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
        beforeOpenAction.run();
        String groupId = group.getId();
        if (switchToExistingGroupTab(groupId)) {
            return;
        }

        transientTabManager.validate();
        GroupEditPanel groupEditPanel = createGroupEditPanel(groupNode, group);
        transientTabManager.showOrReplace(groupEditPanel, group.getName(), null, isRootGroup(groupNode));
    }

    void showOrCreateGroupTab(DefaultMutableTreeNode groupNode, RequestGroup group) {
        beforeOpenAction.run();
        String groupId = group.getId();
        if (transientTabManager.isTransientGroup(groupId)) {
            transientTabManager.pin();
            return;
        }
        if (switchToExistingGroupTab(groupId)) {
            return;
        }

        GroupEditPanel groupEditPanel = createGroupEditPanel(groupNode, group);
        insertFixedTab(group.getName(), groupEditPanel,
                new ClosableTabComponent(group.getName(), null, isRootGroup(groupNode)));
    }

    void showOrCreateTransientSavedResponse(SavedResponse savedResponse) {
        beforeOpenAction.run();
        if (savedResponse == null || isBlank(savedResponse.getId())) {
            return;
        }

        if (switchToExistingSavedResponseTab(savedResponse)) {
            return;
        }

        transientTabManager.validate();
        transientTabManager.showOrReplace(createSavedResponsePanel(savedResponse), savedResponse.getName(),
                RequestItemProtocolEnum.SAVED_RESPONSE);
    }

    void showOrCreateSavedResponseTab(SavedResponse savedResponse) {
        beforeOpenAction.run();
        if (savedResponse == null || isBlank(savedResponse.getId())) {
            return;
        }

        if (transientTabManager.isTransientRequest(savedResponse.getId())) {
            transientTabManager.pin();
            return;
        }
        if (switchToExistingSavedResponseTab(savedResponse)) {
            return;
        }

        insertFixedTab(savedResponse.getName(), createSavedResponsePanel(savedResponse),
                new ClosableTabComponent(savedResponse.getName(), RequestItemProtocolEnum.SAVED_RESPONSE, false));
    }

    private RequestEditSubPanel createSavedResponsePanel(SavedResponse savedResponse) {
        RequestEditSubPanel panel = new RequestEditSubPanel(
                savedResponse, collectionGateway.findRequestContainingResponse(savedResponse.getId()));
        panel.loadSavedResponse(savedResponse);
        return panel;
    }

    private GroupEditPanel createGroupEditPanel(DefaultMutableTreeNode groupNode, RequestGroup group) {
        return new GroupEditPanel(groupNode, group, () -> collectionGateway.saveGroupNode(groupNode));
    }

    private void insertFixedTab(String title, Component component, ClosableTabComponent tabComponent) {
        int plusTabIndex = tabbedPane.getTabCount() > 0 ? tabbedPane.getTabCount() - 1 : 0;
        if (!plusTabPredicate.test(plusTabIndex)) {
            plusTabIndex = tabbedPane.getTabCount();
        }
        tabbedPane.insertTab(title, null, component, null, plusTabIndex);
        tabbedPane.setTabComponentAt(plusTabIndex, tabComponent);
        tabbedPane.setSelectedIndex(plusTabIndex);
        plusTabRestorer.run();
    }

    private boolean switchToExistingRequestTab(String id) {
        return selectTab(findFixedTab(component -> component instanceof RequestEditSubPanel subPanel
                && id.equals(subPanel.getId())));
    }

    private boolean switchToExistingSavedResponseTab(SavedResponse savedResponse) {
        String id = savedResponse.getId();
        int index = findFixedTab(component -> component instanceof RequestEditSubPanel subPanel
                && id.equals(subPanel.getId()));
        if (index >= 0) {
            logIfDifferentExampleShared(index, savedResponse);
        }
        return selectTab(index);
    }

    private boolean switchToExistingGroupTab(String groupId) {
        if (isBlank(groupId)) {
            return false;
        }
        return selectTab(findFixedTab(component -> component instanceof GroupEditPanel groupPanel
                && groupId.equals(groupPanel.getGroup().getId())));
    }

    /**
     * Tìm những cái được ghim (không tạm thời) Tab，Bỏ qua tạm thời Tab và cuối cùng "+" Tab。
     */
    private int findFixedTab(Predicate<Component> matcher) {
        for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
            if (i != transientTabManager.transientTabIndex() && matcher.test(tabbedPane.getComponentAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean selectTab(int index) {
        if (index < 0) {
            return false;
        }
        tabbedPane.setSelectedIndex(index);
        return true;
    }

    /**
     * Đã mở Tab với nhấp chuột Example chỉ một ID Giống nhau, không giống nhau Example Ghi lại nhật ký thường xuyên để tạo điều kiện khắc phục sự cố ID Các vấn đề lặp đi lặp lại dẫn đến không thể chuyển đổi.
     */
    private void logIfDifferentExampleShared(int index, SavedResponse clicked) {
        if (!(tabbedPane.getComponentAt(index) instanceof RequestEditSubPanel subPanel)) {
            return;
        }
        SavedResponse opened = subPanel.getSavedResponse();
        // Sau khi bộ sưu tập được tải lại Tab Đối tượng cũ vẫn được giữ và nội dung giống nhau được coi là giống nhau Example
        if (Objects.equals(opened, clicked)) {
            return;
        }
        HttpRequestItem clickedOwner = collectionGateway.findRequestContainingResponse(clicked.getId());
        log.info("Example tab reused by duplicate id: id={}, clickedName={}, openedTabIndex={}, openedTabTitle={}, "
                        + "openedName={}, openedIsExample={}, ownerRequestId={}, ownerRequestName={}",
                clicked.getId(), clicked.getName(), index, tabbedPane.getTitleAt(index),
                opened != null ? opened.getName() : null, subPanel.isSavedResponseTab(),
                clickedOwner != null ? clickedOwner.getId() : null,
                clickedOwner != null ? clickedOwner.getName() : null);
    }

    private String requestTitle(HttpRequestItem item) {
        return CharSequenceUtil.isNotBlank(item.getName()) ? item.getName() : defaultRequestTitle;
    }

    private boolean isRootGroup(DefaultMutableTreeNode groupNode) {
        return groupNode.getLevel() == 1;
    }

    private boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}
