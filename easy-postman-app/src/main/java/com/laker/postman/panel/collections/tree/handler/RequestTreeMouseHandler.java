package com.laker.postman.panel.collections.tree.handler;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.model.HttpRequestItem;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.tree.RequestTreeCellRenderer;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.tree.coordinator.RequestTreeCoordinator;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Yêu cầu xử lý sự kiện chuột cây
 * Chịu trách nhiệm xử lý menu nhấp chuột một lần, nhấp đúp và nhấp chuột phải của các nút cây
 */
public class RequestTreeMouseHandler extends MouseAdapter {
    private final JTree requestTree;
    private final RequestTreePopupMenu popupMenu;
    private final RequestTreeCoordinator coordinator;
    private final RequestTreeOpenActions openActions;
    private final CollectionTreeClickResolver clickResolver;

    public RequestTreeMouseHandler(JTree requestTree, CollectionTreePanel leftPanel, RequestTreeCoordinator coordinator) {
        this(requestTree, leftPanel, coordinator, new RequestEditorTreeOpenActions());
    }

    RequestTreeMouseHandler(
            JTree requestTree,
            CollectionTreePanel leftPanel,
            RequestTreeCoordinator coordinator,
            RequestTreeOpenActions openActions
    ) {
        this.requestTree = requestTree;
        this.popupMenu = new RequestTreePopupMenu(requestTree, leftPanel, coordinator);
        this.coordinator = coordinator;
        this.openActions = openActions;
        this.clickResolver = new CollectionTreeClickResolver(requestTree);
    }

    // ==================== hover Theo dõi ====================

    @Override
    public void mouseMoved(MouseEvent e) {
        handleHover(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        handleHover(e);
    }

    private void handleHover(MouseEvent e) {
        int row = clickResolver.rowAtY(e.getY());
        updateHoveredRow(row);
        updateTooltip(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        updateHoveredRow(-1);
        requestTree.setToolTipText(null);
    }

    /**
     * Tự động thiết lập dựa trên vị trí chuột tooltip
     */
    private void updateTooltip(MouseEvent e) {
        CollectionTreeClickTarget target = clickResolver.resolve(e.getX(), e.getY());
        if (target != null && target.isGroupMoreActions()) {
            requestTree.setToolTipText("More actions");
            return;
        }
        if (target != null && target.isGroupAddRequestAction()) {
            requestTree.setToolTipText("Add Request");
            return;
        }
        requestTree.setToolTipText(null);
    }

    private void updateHoveredRow(int row) {
        TreeCellRenderer renderer = requestTree.getCellRenderer();
        if (renderer instanceof RequestTreeCellRenderer r) {
            r.setHoveredRow(row);
            requestTree.repaint();
        }
    }

    // ==================== Nhấp vào Quá trình ====================

    @Override
    public void mousePressed(MouseEvent e) {
        // chủ đề/Có thể làm mới phông chữ Swing UI Trình nghe được xếp hàng sau trình nghe này và không thể dựa vào đó để xóa tính năng bảo vệ của lần nhấp cuối cùng.
        coordinator.finishRequestCreationSelection();
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
            handleLeftSingleClick(e);
        } else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
            handleDoubleClick(e);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            handleRightClick(e);
        }
    }

    /**
     * mouseReleased Đánh chặn Add/More Nhấp vào khu vực nút để tránh kích hoạt mở rộng nút/Hành vi mặc định như sụp đổ
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
            CollectionTreeClickTarget target = clickResolver.resolve(e.getX(), e.getY());
            if (isGroupRowAction(target)) {
                e.consume();
            }
        }
        coordinator.finishRequestCreationSelection();
    }

    private void handleLeftSingleClick(MouseEvent e) {
        CollectionTreeClickTarget target = clickResolver.resolve(e.getX(), e.getY());
        if (target == null) {
            return;
        }

        if (target.isGroupMoreActions()) {
            e.consume();
            handleGroupMoreActionsClick(target, e.getX(), e.getY());
            return;
        }

        if (target.isGroupAddRequestAction()) {
            e.consume();
            handleGroupAddRequestClick(target.node());
            return;
        }

        openTargetTransiently(target);
    }

    private boolean isGroupRowAction(CollectionTreeClickTarget target) {
        return target != null && (target.isGroupMoreActions() || target.isGroupAddRequestAction());
    }

    private void handleGroupAddRequestClick(DefaultMutableTreeNode groupNode) {
        // addHttpRequestDirectly Yêu cầu mới sẽ được chọn.mouseReleased Sau đó kết thúc bảo vệ đã chọn.
        coordinator.addHttpRequestDirectly(groupNode);
    }

    private void handleGroupMoreActionsClick(CollectionTreeClickTarget target, int x, int y) {
        requestTree.setSelectionPath(target.path());
        showGroupActionMenu(target.node(), x, y);
    }

    /**
     * bật lên group Menu độc quyền (yêu cầu mới, nhóm mới, v.v.)
     */
    private void showGroupActionMenu(DefaultMutableTreeNode groupNode, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

        JMenuItem addRequest = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_REQUEST),
                IconUtil.createThemed("icons/request.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        addRequest.addActionListener(ev -> coordinator.showAddRequestDialog(groupNode));
        menu.add(addRequest);

        JMenuItem addGroup = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_GROUP),
                IconUtil.create("icons/group.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        addGroup.addActionListener(ev -> coordinator.showAddGroupDialog(groupNode));
        menu.add(addGroup);

        menu.addSeparator();

        JMenuItem rename = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_RENAME),
                IconUtil.createThemed("icons/edit.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        rename.addActionListener(ev -> coordinator.renameSelectedItem());
        rename.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        menu.add(rename);

        JMenuItem duplicate = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_DUPLICATE),
                IconUtil.createThemed("icons/duplicate.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        duplicate.addActionListener(ev -> coordinator.duplicateSelectedGroup());
        menu.add(duplicate);

        menu.addSeparator();

        JMenuItem delete = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_DELETE),
                IconUtil.createThemed("icons/delete.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        delete.addActionListener(ev -> coordinator.deleteSelectedItem());
        delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        menu.add(delete);

        menu.show(requestTree, x, y);
    }

    /**
     * Xử lý các sự kiện nhấp chuột: tạm thời mở một yêu cầu hoặc nhóm
     */
    private void openTargetTransiently(CollectionTreeClickTarget target) {
        DefaultMutableTreeNode node = target.node();

        if (CollectionTreeNodes.isGroup(node)) {
            toggleGroupAndOpenTransiently(target);
        } else if (CollectionTreeNodes.isRequest(node)) {
            CollectionTreeNodes.request(node).ifPresent(this::handleRequestClick);
        } else if (CollectionTreeNodes.isSavedResponse(node)) {
            CollectionTreeNodes.savedResponse(node).ifPresent(this::handleSavedResponseClick);
        }
    }

    /**
     * Xử lý sự kiện nhấp đúp: Mở bảng chỉnh sửa yêu cầu hoặc nhóm
     */
    private void handleDoubleClick(MouseEvent e) {
        CollectionTreeClickTarget target = clickResolver.resolve(e.getX(), e.getY());
        if (target == null) return;

        DefaultMutableTreeNode node = target.node();

        if (CollectionTreeNodes.isRequest(node)) {
            CollectionTreeNodes.request(node)
                    .ifPresent(openActions::openFixedRequest);
        } else if (CollectionTreeNodes.isGroup(node)) {
            RequestGroup group = CollectionTreeNodes.group(node).orElse(null);
            if (group == null) {
                return;
            }
            openActions.openFixedGroup(node, group);
            e.consume(); // Ngăn chặn sự mở rộng/Bỏ đi
        } else if (CollectionTreeNodes.isSavedResponse(node)) {
            // Bấm đúp vào câu trả lời đã lưu: Mở ghim Tab
            CollectionTreeNodes.savedResponse(node).ifPresent(this::handleSavedResponseDoubleClick);
            e.consume();
        }
    }

    /**
     * Xử lý nhấp chuột phải: Hiển thị menu bật lên
     */
    private void handleRightClick(MouseEvent e) {
        int row = clickResolver.rowAtY(e.getY());
        if (row != -1) {
            TreePath clickedPath = requestTree.getPathForRow(row);
            // Chỉ khi đường dẫn được nhấp không được chọn, nó sẽ được đặt làm mục hiện được chọn
            // Nếu đường dẫn được nhấp đã được chọn thì trạng thái đa lựa chọn hiện tại sẽ được duy trì
            if (clickedPath != null && !requestTree.isPathSelected(clickedPath)) {
                requestTree.setSelectionPath(clickedPath);
            }
        } else {
            requestTree.clearSelection();
        }
        popupMenu.show(e.getX(), e.getY());
    }

    /**
     * Xử lý các sự kiện nhấp vào nhóm
     */
    private void toggleGroupAndOpenTransiently(CollectionTreeClickTarget target) {
        if (target.isExpansionHandle()) {
            return;
        }

        toggleGroupExpansion(target.path());
        RequestGroup group = CollectionTreeNodes.group(target.node()).orElse(null);
        if (group != null) {
            openActions.openTransientGroup(target.node(), group);
        }
    }

    private void toggleGroupExpansion(TreePath path) {
        if (requestTree.isExpanded(path)) {
            requestTree.collapsePath(path);
        } else {
            requestTree.expandPath(path);
        }
    }

    /**
     * Xử lý các sự kiện nhấp chuột yêu cầu
     */
    private void handleRequestClick(HttpRequestItem item) {
        openActions.openTransientRequest(item);
    }

    /**
     * Xử lý các phản hồi đã lưu cho các sự kiện nhấp chuột: Mở tạm thời
     */
    private void handleSavedResponseClick(SavedResponse savedResponse) {
        openActions.openTransientSavedResponse(savedResponse);
    }

    /**
     * Xử lý các phản hồi đã lưu cho các sự kiện nhấp đúp: ghim mở Tab
     */
    private void handleSavedResponseDoubleClick(SavedResponse savedResponse) {
        openActions.openFixedSavedResponse(savedResponse);
    }

}
