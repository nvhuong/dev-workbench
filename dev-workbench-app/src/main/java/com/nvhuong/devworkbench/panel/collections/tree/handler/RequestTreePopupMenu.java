package com.nvhuong.devworkbench.panel.collections.tree.handler;

import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.panel.mock.MockServerPanel;
import com.nvhuong.devworkbench.panel.sidebar.SidebarTab;
import com.nvhuong.devworkbench.panel.sidebar.SidebarTabPanel;
import com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel;
import com.nvhuong.devworkbench.panel.collections.tree.CollectionTreeToolbar;
import com.nvhuong.devworkbench.panel.collections.tree.coordinator.RequestTreeCoordinator;
import com.nvhuong.devworkbench.service.collections.CollectionTreeNodes;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.MessageKeys;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;

import static com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel.*;

/**
 * Request tree right-click popup menu
 */
public class RequestTreePopupMenu {
    private final JTree requestTree;
    private final CollectionTreePanel leftPanel;
    private final RequestTreeCoordinator coordinator;

    public RequestTreePopupMenu(JTree requestTree, CollectionTreePanel leftPanel) {
        this(requestTree, leftPanel, new RequestTreeCoordinator(requestTree, leftPanel));
    }

    RequestTreePopupMenu(JTree requestTree, CollectionTreePanel leftPanel, RequestTreeCoordinator coordinator) {
        this.requestTree = requestTree;
        this.leftPanel = leftPanel;
        this.coordinator = coordinator;
    }

    /**
     * Hiển thị menu bật lên
     */
    public void show(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        boolean isMultipleSelection = selectedPaths != null && selectedPaths.length > 1;


        if (selectedNode == null || selectedNode == leftPanel.getRootTreeNode()) {
            menu.show(requestTree, x, y);
            return;
        }

        // Menu nút nhóm
        if (CollectionTreeNodes.isGroup(selectedNode)) {
            addGroupMenuItems(menu, selectedNode, isMultipleSelection);
        }

        // Menu nút yêu cầu
        if (CollectionTreeNodes.isRequest(selectedNode)) {
            addRequestMenuItems(menu, selectedNode, isMultipleSelection);
        }

        // Menu nút phản hồi đã lưu - Chỉ đổi tên và xóa được hiển thị và các tùy chọn khác như dán không được hiển thị
        if (CollectionTreeNodes.isSavedResponse(selectedNode)) {
            addRenameAndDeleteMenuItems(menu, isMultipleSelection);
            menu.show(requestTree, x, y);
            return;
        }

        // Tùy chọn dán
        if (!coordinator.isCopiedRequestsEmpty()) {
            addPasteMenuItem(menu);
        }

        // Đổi tên và xóa (các nút không phải gốc)
        if (selectedNode != leftPanel.getRootTreeNode()) {
            addRenameAndDeleteMenuItems(menu, isMultipleSelection);
        }

        menu.show(requestTree, x, y);
    }


    /**
     * Thêm nhóm các mục menu liên quan
     */
    private void addGroupMenuItems(JPopupMenu menu, DefaultMutableTreeNode selectedNode, boolean isMultipleSelection) {

        // Đã thêm vào các bài kiểm tra chức năng
        JMenuItem addToFunctional = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_ADD_TO_FUNCTIONAL,
                "icons/functional.svg",
                e -> coordinator.addSelectedRequestsToFunctionalTest()
        );
        menu.add(addToFunctional);

        JMenuItem createMockServer = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_CREATE_MOCK_SERVER,
                "icons/mock-server.svg",
                e -> openMockServerForCollection(selectedNode)
        );
        createMockServer.setEnabled(!isMultipleSelection);
        menu.add(createMockServer);
        menu.addSeparator();

        // Thêm yêu cầu mới
        JMenuItem addRequest = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_ADD_REQUEST,
                "icons/request.svg",
                e -> coordinator.showAddRequestDialog(selectedNode)
        );
        addRequest.setEnabled(!isMultipleSelection);
        menu.add(addRequest);

        // Thêm nhóm mới
        JMenuItem addGroup = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_ADD_GROUP,
                "icons/group.svg",
                e -> coordinator.showAddGroupDialog(selectedNode)
        );
        addGroup.setEnabled(!isMultipleSelection);
        menu.add(addGroup);

        // Sao chép nhóm
        JMenuItem duplicate = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_DUPLICATE,
                "icons/duplicate.svg",
                e -> coordinator.duplicateSelectedGroup()
        );
        duplicate.setEnabled(!isMultipleSelection);
        menu.add(duplicate);

        JMenuItem importCurl = createMenuItem(
                MessageKeys.COLLECTIONS_IMPORT_CURL,
                "icons/curl.svg",
                e -> CollectionTreeNodes.group(selectedNode)
                        .ifPresent(group -> UiSingletonFactory.getInstance(CollectionTreeToolbar.class)
                                .importCurlToGroup(group))
        );
        importCurl.setEnabled(!isMultipleSelection);
        menu.add(importCurl);

        // Xuất dưới dạng DevWorkbench
        JMenuItem exportDevWorkbench = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_EXPORT_DEV_WORKBENCH,
                "icons/devworkbench.svg",
                e -> coordinator.exportGroupAsDevWorkbench(selectedNode)
        );
        exportDevWorkbench.setEnabled(!isMultipleSelection);
        menu.add(exportDevWorkbench);

        // Di chuyển đến không gian làm việc khác
        JMenuItem moveToWorkspace = createMenuItem(
                MessageKeys.WORKSPACE_TRANSFER_MENU_ITEM,
                "icons/workspace.svg",
                e -> coordinator.moveCollectionToWorkspace(selectedNode)
        );
        moveToWorkspace.setEnabled(!isMultipleSelection);
        menu.add(moveToWorkspace);

        menu.addSeparator();
    }

    /**
     * Thêm các mục menu liên quan đến yêu cầu
     */
    private void addRequestMenuItems(JPopupMenu menu, DefaultMutableTreeNode selectedNode, boolean isMultipleSelection) {
        // Đã thêm vào các bài kiểm tra chức năng
        JMenuItem addToFunctional = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_ADD_TO_FUNCTIONAL,
                "icons/functional.svg",
                e -> coordinator.addSelectedRequestsToFunctionalTest()
        );
        menu.add(addToFunctional);

        JMenuItem addMockResponse = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_ADD_MOCK_RESPONSE,
                "icons/mock-server.svg",
                e -> addMockResponse(selectedNode)
        );
        addMockResponse.setEnabled(!isMultipleSelection);
        menu.add(addMockResponse);
        menu.addSeparator();

        // Thêm yêu cầu mới (thuộc nhóm anh chị em)
        DefaultMutableTreeNode parentGroup = (DefaultMutableTreeNode) selectedNode.getParent();
        if (parentGroup != null) {
            JMenuItem addRequest = createMenuItem(
                    MessageKeys.COLLECTIONS_MENU_ADD_REQUEST,
                    "icons/request.svg",
                    e -> coordinator.showAddRequestDialog(parentGroup)
            );
            addRequest.setEnabled(!isMultipleSelection);
            menu.add(addRequest);
        }

        // Sao chép (tạo một bản sao)
        JMenuItem duplicate = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_DUPLICATE,
                "icons/duplicate.svg",
                e -> coordinator.duplicateSelectedRequests()
        );
        menu.add(duplicate);

        // Sao chép vào clipboard
        JMenuItem copy = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_COPY,
                "icons/copy.svg",
                e -> coordinator.copySelectedRequests()
        );
        int cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, cmdMask));
        menu.add(copy);

        // Sao chép dưới dạng cURL
        JMenuItem copyAsCurl = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_COPY_CURL,
                "icons/curl.svg",
                e -> coordinator.copySelectedRequestAsCurl()
        );
        copyAsCurl.setEnabled(!isMultipleSelection);
        menu.add(copyAsCurl);
        menu.addSeparator();
    }


    /**
     * Đã thêm mục menu Dán
     */
    private void addPasteMenuItem(JPopupMenu menu) {
        JMenuItem paste = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_PASTE,
                "icons/paste.svg",
                e -> coordinator.pasteRequests()
        );
        int cmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, cmdMask));
        menu.add(paste);
        menu.addSeparator();
    }

    /**
     * Đã thêm đổi tên và xóa các mục menu
     */
    private void addRenameAndDeleteMenuItems(JPopupMenu menu, boolean isMultipleSelection) {
        // Đổi tên
        JMenuItem rename = createMenuItem(
                MessageKeys.COLLECTIONS_MENU_RENAME,
                "icons/edit.svg",
                e -> coordinator.renameSelectedItem()
        );
        rename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        rename.setEnabled(!isMultipleSelection);
        menu.add(rename);

        // Xóa (hiển thị số lượng đã chọn)
        TreePath[] selectedPaths = requestTree.getSelectionPaths();
        int selectedCount = selectedPaths != null ? selectedPaths.length : 0;

        String deleteText = I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_DELETE);
        if (selectedCount > 1) {
            deleteText += " (" + selectedCount + ")";
        }

        JMenuItem delete = new JMenuItem(deleteText,
                IconUtil.createThemed("icons/close.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        delete.addActionListener(e -> coordinator.deleteSelectedItem());
        delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        menu.add(delete);
    }

    /**
     * Phương pháp trợ giúp để tạo các mục menu (điều chỉnh chủ đề)
     */
    private JMenuItem createMenuItem(String messageKey, String iconPath, java.awt.event.ActionListener listener) {
        // Danh sách biểu tượng màu: Bản thân các biểu tượng này có màu thương hiệu hoặc màu chuyển sắc và không được tuân theo các thay đổi về chủ đề
        // - devworkbench.svg: DevWorkbench Thương hiệu màu cam
        // - group.svg: Thư mục màu xanh (#007AFF)
        // - curl.svg: cURL Biểu tượng dòng lệnh, thiết bị đầu cuối màu xanh đậm với dấu nhắc lệnh màu xanh lá cây
        boolean isColoredIcon = iconPath.contains("devworkbench.svg")
                             || iconPath.contains("group.svg")
                             || iconPath.contains("curl.svg");

        JMenuItem item = new JMenuItem(
                I18nUtil.getMessage(messageKey),
                isColoredIcon
                    ? IconUtil.create(iconPath, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)
                    : IconUtil.createThemed(iconPath, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)
        );
        item.addActionListener(listener);
        return item;
    }

    private void openMockServerForCollection(DefaultMutableTreeNode node) {
        RequestGroup collection = rootCollection(node);
        if (collection == null) return;
        SidebarTabPanel sidebar = UiSingletonFactory.getInstance(SidebarTabPanel.class);
        if (!sidebar.showTab(SidebarTab.MOCK_SERVER)) return;
        MockServerPanel mockPanel = UiSingletonFactory.getInstance(MockServerPanel.class);
        SwingUtilities.invokeLater(() -> mockPanel.createServerForCollection(collection.getId()));
    }

    private void addMockResponse(DefaultMutableTreeNode requestNode) {
        RequestGroup collection = rootCollection(requestNode);
        String requestId = CollectionTreeNodes.request(requestNode).map(item -> item.getId()).orElse(null);
        if (collection == null || requestId == null) return;
        SidebarTabPanel sidebar = UiSingletonFactory.getInstance(SidebarTabPanel.class);
        if (!sidebar.showTab(SidebarTab.MOCK_SERVER)) return;
        MockServerPanel mockPanel = UiSingletonFactory.getInstance(MockServerPanel.class);
        SwingUtilities.invokeLater(() -> mockPanel.addMockResponseForRequest(collection.getId(), requestId));
    }

    private RequestGroup rootCollection(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode current = node;
        while (current != null && current.getParent() != leftPanel.getRootTreeNode()) {
            current = (DefaultMutableTreeNode) current.getParent();
        }
        return CollectionTreeNodes.group(current).orElse(null);
    }
}
