package com.nvhuong.devworkbench.panel.collections.editor;

import com.nvhuong.devworkbench.common.UiSingletonPanel;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.tab.TabbedPaneDragHandler;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.panel.collections.editor.request.RequestEditSubPanel;
import com.nvhuong.devworkbench.panel.collections.editor.request.RequestEditSubPanelType;
import com.nvhuong.devworkbench.panel.collections.tree.CollectionGroupSelectionDialog;
import com.nvhuong.devworkbench.panel.collections.tree.RequestNameSelection;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.request.model.SavedResponse;
import com.nvhuong.devworkbench.service.collections.RequestSaveEventPublisher;
import com.nvhuong.devworkbench.service.setting.SettingManager;
import com.nvhuong.devworkbench.service.variable.RequestExecutionContext;
import com.nvhuong.devworkbench.service.variable.RequestExecutionScope;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.Optional;

import static com.formdev.flatlaf.FlatClientProperties.*;

/**
 * Bảng chỉnh sửa yêu cầu hỗ trợ nhiều trang tab và mỗi trang tab là một bảng phụ chỉnh sửa yêu cầu độc lập
 */
public class RequestEditorPanel extends UiSingletonPanel {
    private static final String REQUEST_STRING = I18nUtil.getMessage(MessageKeys.NEW_REQUEST);
    static final Insets EDITOR_WORKSPACE_INSETS = new Insets(6, 6, 6, 6);
    static final int REQUEST_TAB_HEIGHT = 34;
    static final Insets REQUEST_TAB_INSETS = new Insets(2, 5, 2, 5);
    static final Insets REQUEST_TAB_AREA_INSETS = new Insets(0, 0, 0, 5);
    static final String PLUS_TAB = "+";
    @Getter
    private JTabbedPane tabbedPane; // sử dụng JTabbedPane Quản lý nhiều bảng con chỉnh sửa yêu cầu

    @Getter
    private TabbedPaneDragHandler dragHandler; // Tab Hỗ trợ sắp xếp kéo và thả

    // Đã thêm dưới sự tương tác bình thường/Đã chọn tab sẽ vào vòng tiếp theo EDT Khởi tạo trình soạn thảo hiện tại.
    @Setter
    private boolean autoInitializeSelectedTabOnTabAdd = true;
    // Khi bắt đầu khôi phục nhiều yêu cầu, chỉ yêu cầu cuối cùng tab Tự động được chọn tab。
    @Getter
    @Setter
    private boolean startupRestoreSelectingLastTab;

    private RequestEditorTransientTabManager transientTabManager;
    private RequestEditorSaveController saveController;
    private RequestEditorShortcutInstaller shortcutInstaller;
    private RequestEditorTabOpenController tabOpenController;
    private RequestEditorTabCloseController tabCloseController;
    private RequestEditorTabStateController tabStateController;
    private RequestEditorTabLifecycleController tabLifecycleController;
    private RequestEditorTabRemovalController tabRemovalController;
    private RequestEditorTabInitializationScheduler tabInitializationScheduler;
    private RequestEditorExecutionScopeSynchronizer executionScopeSynchronizer;
    private final CollectionTreeEditorGateway collectionTreeGateway = new CollectionTreeEditorGateway();


    // MớiTab，Một tiêu đề có thể được chỉ định
    public RequestEditSubPanel addNewTab(String title, RequestItemProtocolEnum protocol) {
        cancelStartupRestoreAutoSelectionIfNeeded();
        return tabLifecycleController.addNewRequestTab(title, protocol);
    }

    // MớiTab，Một tiêu đề có thể được chỉ định
    public void addNewTab(String title) {
        addNewTab(title, RequestItemProtocolEnum.HTTP);
    }

    // Thêm vào"+"Tab
    public void addPlusTab() {
        tabLifecycleController.addPlusTab();
    }

    // Xác định xem có phải như vậy không“+”Tab
    private boolean isPlusTab(int idx) {
        return tabLifecycleController.isPlusTab(idx);
    }

    /**
     * Hiển thị hoặc sử dụng lại tạm thời khi nhấp vào nút cây Tab。
     */
    public void showOrCreateTransientTab(HttpRequestItem item) {
        tabOpenController.showOrCreateTransientRequest(item);
    }

    /**
     * Hiện tại sẽ là tạm thời Tab Đã sửa thành bình thường Tab。
     */
    public void pinTransientTab() {
        transientTabManager.pin();
    }

    /**
     * Bấm vào Group Hiển thị hoặc tái sử dụng tạm thời Tab。
     */
    public void showOrCreateTransientTabForGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
        tabOpenController.showOrCreateTransientGroup(groupNode, group);
    }

    // showOrCreateTab Yêu cầu thích ứng "+" Tab（Được gọi khi nhấp đúp để tạo mã pin tab）
    public void showOrCreateTab(HttpRequestItem item) {
        tabOpenController.showOrCreateRequestTab(item);
    }

    /**
     * Tải lại các phím tắt (được gọi sau khi sửa đổi cài đặt phím tắt)
     */
    public void reloadShortcuts() {
        shortcutInstaller.install();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getComponentAt(i) instanceof RequestEditorEmptyStatePanel emptyStatePanel) {
                emptyStatePanel.refreshShortcutHints();
            }
        }
    }

    /**
     * Lưu yêu cầu hiện tại
     */
    public boolean saveCurrentRequest() {
        return saveController.saveCurrentRequest();
    }

    protected Optional<RequestNameSelection> chooseGroupAndRequestName(
            TreeModel groupTreeModel,
            String defaultName
    ) {
        return CollectionGroupSelectionDialog.chooseGroupAndRequestName(groupTreeModel, defaultName);
    }

    // để cập nhật độngtabchấm đỏ
    public void updateTabDirty(RequestEditSubPanel panel, boolean dirty) {
        tabStateController.updateRequestDirty(panel, dirty);
    }

    public void updateTabDisplay(RequestEditSubPanel panel, String method, RequestItemProtocolEnum protocol) {
        tabStateController.updateRequestDisplay(panel, method, protocol);
    }

    /**
     * đổi mới GroupEditPanel của Tab tiêu đề
     */
    void updateGroupTabTitle(GroupEditPanel panel, String newTitle) {
        tabStateController.updateGroupTitle(panel, newTitle);
    }

    @Override
    protected void initUI() {
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(
                EDITOR_WORKSPACE_INSETS.top,
                EDITOR_WORKSPACE_INSETS.left,
                EDITOR_WORKSPACE_INSETS.bottom,
                EDITOR_WORKSPACE_INSETS.right));
        setLayout(new BorderLayout());
        tabbedPane = createRequestTabbedPane();
        tabLifecycleController = new RequestEditorTabLifecycleController(
                tabbedPane,
                PLUS_TAB,
                REQUEST_STRING,
                () -> autoInitializeSelectedTabOnTabAdd,
                this::initializeSelectedTabSoon);
        installDeferredTabInitialization();
        add(tabbedPane, BorderLayout.CENTER);
        installTransientTabAndDragSupport();
        createTabControllers();
    }

    private JTabbedPane createRequestTabbedPane() {
        JTabbedPane tabs = new RequestEditorTabbedPane(SwingConstants.TOP, requestTabsLayoutPolicy());
        ToolWindowSurfaceStyle.applyTabbedPaneCard(tabs);
        tabs.putClientProperty(TABBED_PANE_TAB_AREA_INSETS, REQUEST_TAB_AREA_INSETS);
        tabs.putClientProperty(TABBED_PANE_TAB_INSETS, REQUEST_TAB_INSETS);
        tabs.putClientProperty(TABBED_PANE_TAB_HEIGHT, REQUEST_TAB_HEIGHT);
        tabs.putClientProperty(TABBED_PANE_TAB_WIDTH_MODE, TABBED_PANE_TAB_WIDTH_MODE_PREFERRED);
        tabs.putClientProperty(TABBED_PANE_TAB_AREA_ALIGNMENT, TABBED_PANE_ALIGN_LEADING);
        tabs.putClientProperty(TABBED_PANE_TAB_ALIGNMENT, TABBED_PANE_ALIGN_LEADING);
        tabs.putClientProperty(TABBED_PANE_HAS_FULL_BORDER, false);
        tabs.putClientProperty(TABBED_PANE_SHOW_CONTENT_SEPARATOR, true);
        return tabs;
    }

    private static final class RequestEditorTabbedPane extends JTabbedPane {
        private RequestEditorTabbedPane(int tabPlacement, int tabLayoutPolicy) {
            super(tabPlacement, tabLayoutPolicy);
        }

        @Override
        public void updateUI() {
            setUI(new RequestEditorTabbedPaneUi());
        }
    }

    static int requestTabsLayoutPolicy() {
        return SettingManager.isRequestEditorTabsMultiLineEnabled()
                ? JTabbedPane.WRAP_TAB_LAYOUT
                : JTabbedPane.SCROLL_TAB_LAYOUT;
    }

    private void installDeferredTabInitialization() {
        tabInitializationScheduler = new RequestEditorTabInitializationScheduler(tabbedPane, () -> startupRestoreSelectingLastTab);
        tabbedPane.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (tabbedPane.getTabCount() > 0 && autoInitializeSelectedTabOnTabAdd) {
                    refreshSelectedRequestExecutionScope();
                    tabInitializationScheduler.initializeSelectedTabSoon();
                }
            }
        });
        tabbedPane.addChangeListener(e -> {
            refreshSelectedRequestExecutionScope();
            tabInitializationScheduler.initializeSelectedTabSoon();
        });
    }

    private void installTransientTabAndDragSupport() {
        transientTabManager = new RequestEditorTransientTabManager(
                tabbedPane,
                this::isPlusTab,
                this::addPlusTab,
                RequestEditorTabResourceCleaner::cleanup);

        dragHandler = TabbedPaneDragHandler.install(tabbedPane,
                transientTabManager::transientTabIndex,
                transientTabManager::setTransientTabIndex);
    }

    private void createTabControllers() {
        executionScopeSynchronizer = new RequestEditorExecutionScopeSynchronizer();
        tabStateController = new RequestEditorTabStateController(tabbedPane, transientTabManager::pinIfTransient);
        tabRemovalController = new RequestEditorTabRemovalController(tabbedPane, transientTabManager);
        saveController = new RequestEditorSaveController(
                this,
                tabStateController::currentRequestTab,
                this::pinTransientTab,
                this::chooseGroupAndRequestName,
                tabStateController::refreshNewRequestTab,
                collectionTreeGateway);
        tabCloseController = new RequestEditorTabCloseController(
                tabbedPane,
                this::isPlusTab,
                this::saveCurrentRequest,
                tabRemovalController::removeAt,
                tabRemovalController::removeComponent);
        tabOpenController = new RequestEditorTabOpenController(
                tabbedPane,
                transientTabManager,
                executionScopeSynchronizer,
                this::cancelStartupRestoreAutoSelectionIfNeeded,
                this::addPlusTab,
                this::isPlusTab,
                this::addNewTab,
                (item, tab) -> tab.initPanelData(item),
                REQUEST_STRING,
                collectionTreeGateway);
        shortcutInstaller = createShortcutInstaller();
    }

    /**
     * ở vòng tiếp theo EDT Khởi tạo trình chỉnh sửa yêu cầu hiện được chọn, tiếp tục chuyển đổi trực tiếp và không giới thiệu thêm bộ tính giờ.
     */
    void initializeSelectedTabSoon() {
        tabInitializationScheduler.initializeSelectedTabSoon();
    }

    void refreshSelectedRequestExecutionScope() {
        if (executionScopeSynchronizer == null || tabbedPane == null) {
            return;
        }
        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent instanceof RequestEditSubPanel subPanel
                && subPanel.getPanelType() == RequestEditSubPanelType.NORMAL) {
            executionScopeSynchronizer.syncScopeForRequest(subPanel.getId());
            return;
        }
        RequestExecutionContext.setCurrentScope(RequestExecutionScope.empty());
    }

    public void initializeSelectedStartupRestoreTab() {
        tabInitializationScheduler.initializeSelectedStartupRestoreTab();
    }

    public void warmUpDeferredRequestTabsAfterStartup() {
        tabInitializationScheduler.warmUpDeferredRequestTabsAfterStartup();
    }

    private void cancelStartupRestoreAutoSelectionIfNeeded() {
        if (!startupRestoreSelectingLastTab) {
            return;
        }
        startupRestoreSelectingLastTab = false;
    }

    @Override
    protected void registerListeners() {
        RequestSaveEventPublisher.register(tabStateController::syncSavedRequest);
        reloadShortcuts();
        new RequestEditorPlusTabController(tabbedPane, this, PLUS_TAB, REQUEST_STRING, this::addNewTab).install();
    }

    private RequestEditorShortcutInstaller createShortcutInstaller() {
        return new RequestEditorShortcutInstaller(
                this,
                tabStateController::currentRequestTab,
                () -> addNewTab(null),
                this::closeCurrentTab,
                this::closeOtherTabs,
                this::closeAllTabs);
    }

    /**
     * Hiển thị bảng chỉnh sửa nhóm (reference DevWorkbench，Không sử dụng cửa sổ bật lên)
     * Được gọi khi nhấp đúp để tạo mã pin tab
     */
    public void showGroupEditPanel(DefaultMutableTreeNode groupNode, RequestGroup group) {
        tabOpenController.showOrCreateGroupTab(groupNode, group);
    }

    /**
     * Đóng tab hiện tại
     */
    public void closeCurrentTab() {
        tabCloseController.closeCurrentTab();
    }

    /**
     * Đóng các tab khác
     */
    public void closeOtherTabs() {
        tabCloseController.closeOtherTabs();
    }

    /**
     * Đóng tất cả các tab
     */
    public void closeAllTabs() {
        tabCloseController.closeAllTabs();
    }

    /**
     * Nhấp vào Phản hồi đã lưu: Tạm thời Tab hiển thị trong
     */
    public void showOrCreateTransientTabForSavedResponse(SavedResponse savedResponse) {
        tabOpenController.showOrCreateTransientSavedResponse(savedResponse);
    }

    /**
     * Bấm đúp vào câu trả lời đã lưu: trong Đã ghim Tab hiển thị trong
     */
    public void showOrCreateTabForSavedResponse(SavedResponse savedResponse) {
        tabOpenController.showOrCreateSavedResponseTab(savedResponse);
    }

    // ==================== Helper Methods ====================

    public void removeTabAtWithCleanup(int index) {
        tabRemovalController.removeAt(index);
    }

    /**
     * Cập nhật hướng bố cục của tất cả các tab đang mở
     *
     * @param isVertical true=bố cục dọc,false=Bố cục ngang
     */
    public void updateAllTabsLayout(boolean isVertical) {
        tabStateController.updateAllRequestLayouts(isVertical);
    }

    public void updateAllRequestEditorTabsVisibility() {
        tabStateController.updateAllRequestEditorTabsVisibility();
    }

    public void updateRequestEditorTabsLayoutPolicy() {
        if (tabbedPane == null) {
            return;
        }
        tabbedPane.setTabLayoutPolicy(requestTabsLayoutPolicy());
        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    /**
     * Snapshot used by the global right-side request assistant.
     */
    public HttpRequestItem getCurrentRequestSnapshotForAssistant() {
        if (tabStateController == null) {
            return null;
        }
        RequestEditSubPanel currentTab = tabStateController.currentRequestTab();
        if (currentTab == null || currentTab.isSavedResponseTab()
                || currentTab.getPanelType() == RequestEditSubPanelType.PERFORMANCE_SNAPSHOT) {
            return null;
        }
        HttpRequestItem request = currentTab.getCurrentRequest();
        if (request == null || request.getProtocol() == null) {
            return request;
        }
        return request.getProtocol().isHttpProtocol() ? request : null;
    }

}
