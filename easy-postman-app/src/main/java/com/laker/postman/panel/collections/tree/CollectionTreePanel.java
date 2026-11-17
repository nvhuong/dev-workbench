package com.laker.postman.panel.collections.tree;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.model.Workspace;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;


import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.async.EasyTaskExecutor;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.tree.RequestTreeCellRenderer;
import com.laker.postman.common.component.tree.TreeTransferHandler;
import com.laker.postman.panel.collections.OpenedRequestTabSessionRestorer;
import com.laker.postman.panel.collections.tree.action.TreeNodeCloner;
import com.laker.postman.panel.collections.tree.handler.RequestTreeKeyboardHandler;
import com.laker.postman.panel.collections.tree.handler.RequestTreeMouseHandler;
import com.laker.postman.panel.collections.tree.coordinator.RequestTreeCoordinator;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.service.collections.CollectionDocumentRegistry;
import com.laker.postman.panel.collections.tree.adapter.SwingCollectionRequestSaveCoordinator;
import com.laker.postman.service.collections.CollectionTreeNodeTypes;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.service.collections.CollectionTreeRootRegistry;
import com.laker.postman.service.collections.OpenedRequestTabsStore;
import com.laker.postman.service.collections.CollectionTreeQueryService;
import com.laker.postman.service.collections.RequestSaveEventPublisher;
import com.laker.postman.panel.collections.tree.adapter.SwingCollectionTreePersistence;
import com.laker.postman.panel.collections.tree.adapter.SwingCollectionTreeQueries;
import com.laker.postman.panel.collections.tree.adapter.SwingCollectionTreeDocumentMapper;
import com.laker.postman.util.SystemUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bảng thu thập yêu cầu hiển thị tất cả các nhóm yêu cầu và mục yêu cầu
 * Hỗ trợ các chức năng như yêu cầu thêm, xóa, sửa đổi, quản lý nhóm, sắp xếp kéo thả, v.v
 */
@Slf4j
public class CollectionTreePanel extends UiSingletonPanel {
    public static final String REQUEST = CollectionTreeNodeTypes.REQUEST;
    public static final String GROUP = CollectionTreeNodeTypes.GROUP;
    public static final String ROOT = CollectionTreeNodeTypes.ROOT;
    public static final String SAVED_RESPONSE = CollectionTreeNodeTypes.SAVED_RESPONSE;
    public static final String EXPORT_FILE_NAME = "Dev Workbench-Collections.json";
    // Nút gốc của bộ sưu tập yêu cầu
    @Getter
    private DefaultMutableTreeNode rootTreeNode;
    // Request tree component
    @Getter
    private JTree requestTree;
    // Mô hình cây, được sử dụng để quản lý các nút cây
    @Getter
    private DefaultTreeModel treeModel;
    @Getter
    private transient SwingCollectionTreePersistence collectionTreePersistence;
    private transient SwingCollectionRequestSaveCoordinator requestSaveCoordinator;

    private record StartupLoadSnapshot(List<HttpRequestItem> openedRequests, HttpRequestItem lastNonNewRequest) {
    }


    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(200, 200));

        // Trên bảng trên cùng, các nút nhập và xuất ở trên cùng và thông tin môi trường ở dưới cùng
        JPanel topPanel = getTopPanel();
        topPanel.setOpaque(false);
        add(topPanel, BorderLayout.NORTH);

        JScrollPane treeScrollPane = getTreeScrollPane();
        add(treeScrollPane, BorderLayout.CENTER);
    }

    private JScrollPane getTreeScrollPane() {
        // Khởi tạo cây yêu cầu
        rootTreeNode = new DefaultMutableTreeNode(ROOT);
        CollectionTreeRootRegistry.registerRootSupplier(() -> rootTreeNode);
        CollectionDocumentRegistry.registerDocumentSupplier(() -> SwingCollectionTreeDocumentMapper.fromRoot(rootTreeNode));
        treeModel = new DefaultTreeModel(rootTreeNode);
        Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
        String filePath = SystemUtil.getCollectionPathForWorkspace(currentWorkspace);
        // Khởi tạo công cụ kiên trì
        collectionTreePersistence = new SwingCollectionTreePersistence(filePath, rootTreeNode, treeModel);
        requestSaveCoordinator = new SwingCollectionRequestSaveCoordinator(rootTreeNode, collectionTreePersistence::saveCurrentTree);
        // Tạo một thành phần cây và ghi đè lên nó getScrollableTracksViewportWidth Đảm bảo chiều rộng của cây luôn được che chắn viewport，
        // Bằng cách này, chuột vẫn ở trong khoảng trống ở phía bên phải của hàng JTree thượng đẳng,mouseMoved Sự kiện có thể được kích hoạt bình thường
        requestTree = new JTree(treeModel) {
            @Override
            public boolean isPathEditable(TreePath path) {
                Object node = path.getLastPathComponent();
                if (node instanceof DefaultMutableTreeNode treeNode) {
                    return treeNode.getParent() != null;
                }
                return false;
            }

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }

            @Override
            public void paint(Graphics g) {
                super.paint(g);
                // Sau khi vẽ cây bình thường đúng không hover Lớp phủ hàng Toàn bộ hàng được đánh dấu bằng nền
                CollectionTreeHoverOverlay.paint(g, this);
            }
        };
        // Nút gốc không được hiển thị
        requestTree.setRootVisible(false);
        // cho phép JTree Thành phần này hiển thị nút gốc"Mở rộng/Bỏ đi"Hình tam giác nhỏ (tức là tay cầm của cấu trúc cây)
        requestTree.setShowsRootHandles(true);
        // Vô hiệu hóa mở rộng nhấp đúp/Thu gọn: Cài đặt toggleClickCount vì 0
        // Bằng cách này, một cú nhấp đúp sẽ chỉ kích hoạt thao tác mở tùy chỉnh của chúng tôi tab Hành vi, sẽ không diễn ra/Bỏ đi
        requestTree.setToggleClickCount(0);
        // Cây cài đặt hỗ trợ nhiều lựa chọn (hỗ trợ xóa hàng loạt)
        requestTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        // FlatLaf wideCellRenderer：cho phép CellRenderer Chiều rộng bao phủ toàn bộ hàng như vậy "+" Con số được dán chính xác ở bên phải
        requestTree.putClientProperty("FlatLaf.style", "wideCellRenderer: true");
        // Đăng ký ToolTipManager，Cho phép cài đặt động tooltip（hover "+" được hiển thị Add Request）
        ToolTipManager.sharedInstance().registerComponent(requestTree);
        // Đặt phông chữ và chiều cao dòng của cây
        requestTree.setCellRenderer(new RequestTreeCellRenderer());
        requestTree.setRowHeight(28);
        JScrollPane treeScrollPane = new JScrollPane(requestTree);
        ToolWindowSurfaceStyle.applyTreeScrollPaneCard(treeScrollPane, requestTree);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(16); // Đặt mức tăng của thanh cuộn
        // Bật sắp xếp kéo
        requestTree.setDragEnabled(true);
        requestTree.setDropMode(DropMode.ON_OR_INSERT);
        requestTree.setTransferHandler(new TreeTransferHandler(requestTree, treeModel, this::saveCurrentTree));
        return treeScrollPane;
    }

    private JPanel getTopPanel() {
        return UiSingletonFactory.getInstance(CollectionTreeToolbar.class);
    }


    @Override
    protected void registerListeners() {
        RequestTreeCoordinator treeCoordinator = new RequestTreeCoordinator(requestTree, this);
        requestTree.addKeyListener(new RequestTreeKeyboardHandler(requestTree, this, treeCoordinator));
        RequestTreeMouseHandler mouseHandler = new RequestTreeMouseHandler(requestTree, this, treeCoordinator);
        requestTree.addMouseListener(mouseHandler);
        requestTree.addMouseMotionListener(mouseHandler);

        EasyTaskExecutor.execute(
                this::loadStartupSnapshot,
                this::applyStartupSnapshot,
                this::handleStartupLoadError,
                "RequestCollections-Loader"
        );
    }

    private StartupLoadSnapshot loadStartupSnapshot() {
        collectionTreePersistence.loadIntoTree();
        List<HttpRequestItem> openedRequests = OpenedRequestTabsStore.loadAll();
        HttpRequestItem lastNonNewRequest = CollectionTreeQueryService.findLastPersistedRequest(openedRequests);
        return new StartupLoadSnapshot(openedRequests, lastNonNewRequest);
    }

    private void applyStartupSnapshot(StartupLoadSnapshot snapshot) {
        RequestEditorPanel requestEditPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        requestEditPanel.setAutoInitializeSelectedTabOnTabAdd(true);
        restoreOpenedRequestTabs(snapshot, requestEditPanel);
        restoreTreeSelection(snapshot);
    }

    private void restoreOpenedRequestTabs(StartupLoadSnapshot snapshot, RequestEditorPanel requestEditPanel) {
        if (snapshot.openedRequests().isEmpty()) {
            requestEditPanel.addPlusTab();
            return;
        }
        requestEditPanel.setStartupRestoreSelectingLastTab(true);
        requestEditPanel.setAutoInitializeSelectedTabOnTabAdd(false);
        requestEditPanel.addPlusTab();
        OpenedRequestTabSessionRestorer.restoreOpenedRequests(
                snapshot.openedRequests(),
                () -> {
                    requestEditPanel.setStartupRestoreSelectingLastTab(false);
                    requestEditPanel.setAutoInitializeSelectedTabOnTabAdd(true);
                    requestEditPanel.initializeSelectedStartupRestoreTab();
                    requestEditPanel.warmUpDeferredRequestTabsAfterStartup();
                }
        );
    }

    private void restoreTreeSelection(StartupLoadSnapshot snapshot) {
        if (snapshot.lastNonNewRequest() != null) {
            locateAndSelectRequest(snapshot.lastNonNewRequest().getId());
            return;
        }
        expandAndSelectFirstGroupIfPresent();
    }

    private void expandAndSelectFirstGroupIfPresent() {
        if (rootTreeNode.getChildCount() <= 0) {
            return;
        }
        DefaultMutableTreeNode firstGroup = (DefaultMutableTreeNode) rootTreeNode.getChildAt(0);
        TreePath path = new TreePath(firstGroup.getPath());
        requestTree.setSelectionPath(path);
        requestTree.expandPath(path);
    }

    private void handleStartupLoadError(Throwable error) {
        log.error("Error loading request collections", error);
        UiSingletonFactory.getInstance(RequestEditorPanel.class).addPlusTab();
    }


    private void saveCurrentTree() {
        collectionTreePersistence.saveCurrentTree();
    }


    /**
     * Lưu yêu cầu vào nhóm được chỉ định
     *
     * @param targetGroup Thông tin nhóm
     * @param item        yêu cầu các mục
     */
    public void saveRequestToGroup(RequestGroup targetGroup, HttpRequestItem item) {
        requestSaveCoordinator.addRequestToGroup(targetGroup, item)
                .ifPresent(result -> {
                    treeModel.reload(result.groupNode());
                    requestTree.expandPath(new TreePath(result.groupNode().getPath()));
                });
    }

    /**
     * Tìm các nút được nhóm theo tên
     */
    public DefaultMutableTreeNode findGroupNode(DefaultMutableTreeNode node, String groupName) {
        if (node == null) return null;

        RequestGroup group = CollectionTreeNodes.group(node).orElse(null);
        if (group != null && groupName.equals(group.getName())) {
            return node;
        }


        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode result = findGroupNode(child, groupName);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Cập nhật một yêu cầu hiện có
     *
     * @param item yêu cầu các mục
     * @return Liệu việc cập nhật có thành công hay không
     */
    public boolean updateExistingRequest(HttpRequestItem item) {
        if (item == null || item.getId() == null || item.getId().isEmpty()) {
            return false;
        }
        SwingCollectionRequestSaveCoordinator.RequestSaveResult result = requestSaveCoordinator
                .updateExistingRequest(item)
                .orElse(null);
        if (result == null) {
            return false;
        }
        HttpRequestItem updatedItem = result.requestItem();

        treeModel.nodeChanged(result.requestNode());
        // Thông báo cho mọi người sau khi lưu UI Thuê bao tự đồng bộ dữ liệu mới nhất.
        SwingUtilities.invokeLater(() -> RequestSaveEventPublisher.publishRequestSaved(updatedItem));
        return true;
    }

    public boolean saveResponseForRequest(HttpRequestItem requestItem, SavedResponse savedResponse) {
        SwingCollectionRequestSaveCoordinator.SavedResponseSaveResult result = requestSaveCoordinator
                .appendSavedResponse(requestItem, savedResponse)
                .orElse(null);
        if (result == null) {
            return false;
        }

        DefaultMutableTreeNode requestNode = result.requestNode();
        treeModel.reload(requestNode);
        requestTree.expandPath(new TreePath(requestNode.getPath()));
        return true;
    }

    public boolean upsertResponseForRequest(String requestId, SavedResponse savedResponse) {
        SwingCollectionRequestSaveCoordinator.SavedResponseSaveResult result = requestSaveCoordinator
                .upsertSavedResponse(requestId, savedResponse)
                .orElse(null);
        if (result == null) return false;
        treeModel.reload(result.requestNode());
        requestTree.expandPath(new TreePath(result.requestNode().getPath()));
        return true;
    }

    public HttpRequestItem findRequestContainingResponse(String responseId) {
        return SwingCollectionTreeQueries.findRequestContainingResponse(rootTreeNode, responseId);
    }

    public boolean deleteResponseForRequest(String requestId, String responseId) {
        SwingCollectionRequestSaveCoordinator.RequestSaveResult result = requestSaveCoordinator
                .removeSavedResponse(requestId, responseId)
                .orElse(null);
        if (result == null) return false;
        treeModel.reload(result.requestNode());
        return true;
    }


    /**
     * Lấy cây nhóm TreeModel（đối với các cây chọn lọc được nhóm)
     */
    public DefaultTreeModel getGroupTreeModel() {
        return treeModel;
    }


    // Tạo một yêu cầu nhiều lựa chọn/Cây lựa chọn được nhóm (đối với Runnercửa sổ bật lên của bảng điều khiển)
    public JTree createRequestSelectionTree() {
        DefaultTreeModel model = new DefaultTreeModel(TreeNodeCloner.cloneTreeNode(rootTreeNode));
        JTree tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new RequestTreeCellRenderer());
        tree.setRowHeight(28);
        // Hỗ trợ nhiều lựa chọn
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        return tree;
    }

    // Nhận tất cả các yêu cầu được chọn trong cây (bao gồm tất cả các yêu cầu trong nhóm)
    public List<HttpRequestItem> getSelectedRequestsFromTree(JTree tree) {
        List<HttpRequestItem> result = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return result;
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            collectRequestsRecursively(node, result);
        }
        // Chống trùng lặp (nhấnid）
        Map<String, HttpRequestItem> map = new LinkedHashMap<>();
        for (HttpRequestItem item : result) {
            map.put(item.getId(), item);
        }
        return new ArrayList<>(map.values());
    }

    // Yêu cầu thu thập đệ quy
    private void collectRequestsRecursively(DefaultMutableTreeNode node, List<HttpRequestItem> list) {
        CollectionTreeNodes.request(node).ifPresent(list::add);
        if (CollectionTreeNodes.isGroup(node)) {
            for (int i = 0; i < node.getChildCount(); i++) {
                collectRequestsRecursively((DefaultMutableTreeNode) node.getChildAt(i), list);
            }
        }
    }

    /**
     * Theo yêu cầuIDXác định vị trí và chọn nút tương ứng trong cây
     *
     * @param requestId yêu cầuID
     */
    public void locateAndSelectRequest(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return;
        }

        DefaultMutableTreeNode targetNode = SwingCollectionTreeQueries.findRequestNodeById(rootTreeNode, requestId);
        if (targetNode == null) {
            return;
        }

        TreePath treePath = new TreePath(targetNode.getPath());
        requestTree.expandPath(treePath.getParentPath());
        requestTree.setSelectionPath(treePath);
        requestTree.requestFocusInWindow();
    }

    /**
     * Chuyển sang tệp bộ sưu tập được yêu cầu của không gian làm việc đã chỉ định và thực hiện lệnh gọi lại sau khi cây bộ sưu tập được tải.
     */
    public void switchWorkspaceAndRefreshUI(String collectionFilePath, Runnable onSuccessCallback) {
        // sử dụng AsyncTaskExecutor Chuyển đổi không gian làm việc không đồng bộ
        EasyTaskExecutor.builder()
                .threadName("SwitchWorkspace-Loader")
                .backgroundTask(() -> {
                    // Chủ đề nền: thực hiện các thao tác tải tập tin
                    if (collectionTreePersistence != null) {
                        collectionTreePersistence.switchDataFilePath(collectionFilePath);
                    }
                })
                .onSuccess(() -> {
                    // EDTChủ đề: Cập nhậtUI
                    UiSingletonFactory.getInstance(RequestEditorPanel.class).getTabbedPane().removeAll();
                    UiSingletonFactory.getInstance(RequestEditorPanel.class).addPlusTab();
                    expandFirstGroup();
                    if (onSuccessCallback != null) {
                        onSuccessCallback.run();
                    }
                })
                .onError(error -> {
                    // EDTChủ đề: xử lý lỗi
                    log.error("Error switching workspace and loading collections", error);
                    UiSingletonFactory.getInstance(RequestEditorPanel.class).getTabbedPane().removeAll();
                    UiSingletonFactory.getInstance(RequestEditorPanel.class).addPlusTab();
                })
                .execute();
    }

    /**
     * Mở rộng nhóm đầu tiên
     */
    public void expandFirstGroup() {
        if (rootTreeNode != null && rootTreeNode.getChildCount() > 0) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) rootTreeNode.getChildAt(0);
            TreePath path = new TreePath(firstChild.getPath());
            requestTree.expandPath(path);
        }
    }

}
