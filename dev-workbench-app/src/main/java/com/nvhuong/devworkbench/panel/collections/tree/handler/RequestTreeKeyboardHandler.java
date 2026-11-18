package com.nvhuong.devworkbench.panel.collections.tree.handler;

import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.request.model.SavedResponse;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel;
import com.nvhuong.devworkbench.panel.collections.tree.coordinator.RequestTreeCoordinator;
import com.nvhuong.devworkbench.service.collections.CollectionTreeNodes;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel.*;

/**
 * Yêu cầu xử lý sự kiện bàn phím cây
 * Chịu trách nhiệm xử lý F2 Đổi tên,Delete xóa bỏ,Ctrl+C Sao chép,Ctrl+V Dán và các phím tắt khác
 */
public class RequestTreeKeyboardHandler extends KeyAdapter {
    private final JTree requestTree;
    private final CollectionTreePanel leftPanel;
    private final RequestTreeCoordinator coordinator;
    private final RequestTreeOpenActions openActions;

    public RequestTreeKeyboardHandler(JTree requestTree, CollectionTreePanel leftPanel) {
        this(requestTree, leftPanel, new RequestTreeCoordinator(requestTree, leftPanel));
    }

    public RequestTreeKeyboardHandler(JTree requestTree, CollectionTreePanel leftPanel, RequestTreeCoordinator coordinator) {
        this(requestTree, leftPanel, coordinator, new RequestEditorTreeOpenActions());
    }

    RequestTreeKeyboardHandler(
            JTree requestTree,
            CollectionTreePanel leftPanel,
            RequestTreeCoordinator coordinator,
            RequestTreeOpenActions openActions
    ) {
        this.requestTree = requestTree;
        this.leftPanel = leftPanel;
        this.coordinator = coordinator;
        this.openActions = openActions;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!hasSelection()) return;

        int cmdMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Ctrl+C / Cmd+C Sao chép
        if (isKeyWithModifier(e, KeyEvent.VK_C, cmdMask)) {
            coordinator.copySelectedRequests();
            e.consume();
            return;
        }

        // Ctrl+V / Cmd+V Dán
        if (isKeyWithModifier(e, KeyEvent.VK_V, cmdMask)) {
            coordinator.pasteRequests();
            e.consume();
            return;
        }

        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null || (leftPanel != null && selectedNode == leftPanel.getRootTreeNode())) {
            return;
        }

        boolean isMultipleSelection = hasMultipleSelection();

        // Enter Phím: Mô phỏng sự kiện nhấp chuột để tạm thời mở một yêu cầu hoặc nhóm (chỉ lựa chọn radio)
        if (e.getKeyCode() == KeyEvent.VK_ENTER && !isMultipleSelection) {
            handleEnterKey();
            e.consume();
        }
        // F2 Đổi tên (chỉ một lựa chọn)
        else if (e.getKeyCode() == KeyEvent.VK_F2 && !isMultipleSelection) {
            coordinator.renameSelectedItem();
        }
        // Delete hoặc Backspace Xóa (hỗ trợ nhiều lựa chọn)
        else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            coordinator.deleteSelectedItem();
        }
    }

    private boolean hasSelection() {
        return requestTree.getSelectionPaths() != null && requestTree.getSelectionPaths().length > 0;
    }

    private boolean hasMultipleSelection() {
        var paths = requestTree.getSelectionPaths();
        return paths != null && paths.length > 1;
    }

    private DefaultMutableTreeNode getSelectedNode() {
        return (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
    }

    private boolean isKeyWithModifier(KeyEvent e, int keyCode, int modifier) {
        return e.getKeyCode() == keyCode && (e.getModifiersEx() & modifier) != 0;
    }

    /**
     * xử lý Enter Phím: Mô phỏng sự kiện nhấp chuột để tạm thời mở một yêu cầu hoặc nhóm
     */
    private void handleEnterKey() {
        DefaultMutableTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null) return;

        if (CollectionTreeNodes.isGroup(selectedNode)) {
            handleGroupEnter(selectedNode);
        } else if (CollectionTreeNodes.isRequest(selectedNode)) {
            CollectionTreeNodes.request(selectedNode).ifPresent(this::handleRequestEnter);
        } else if (CollectionTreeNodes.isSavedResponse(selectedNode)) {
            CollectionTreeNodes.savedResponse(selectedNode).ifPresent(this::handleSavedResponseEnter);
        }
    }

    /**
     * Nhóm quy trình Enter Sự kiện chính: Tạm thời mở nhóm
     */
    private void handleGroupEnter(DefaultMutableTreeNode node) {
        RequestGroup group = CollectionTreeNodes.group(node).orElse(null);
        if (group == null) {
            return;
        }
        openActions.openTransientGroup(node, group);
    }

    /**
     * Xử lý yêu cầu Enter Sự kiện chính: Tạm thời mở một yêu cầu
     */
    private void handleRequestEnter(HttpRequestItem item) {
        openActions.openTransientRequest(item);
    }

    /**
     * Xử lý phản hồi đã lưu Enter Sự kiện chính: Tạm thời mở phản hồi
     */
    private void handleSavedResponseEnter(SavedResponse savedResponse) {
        openActions.openTransientSavedResponse(savedResponse);
    }

}
