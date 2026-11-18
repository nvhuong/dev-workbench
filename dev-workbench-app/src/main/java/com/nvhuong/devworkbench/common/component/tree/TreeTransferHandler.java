package com.nvhuong.devworkbench.common.component.tree;

import com.nvhuong.devworkbench.service.collections.CollectionTreeNodes;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;

/**
 * Hỗ trợJTreeCác nút được sắp xếp bằng cách kéo và thảTransferHandler
 */
@Slf4j
public class TreeTransferHandler extends TransferHandler {
    private final JTree requestTree;
    private final DefaultMutableTreeNode rootTreeNode;
    private final DefaultTreeModel treeModel;
    private final Runnable saveRequestGroups;
    private DataFlavor nodesFlavor; // Đối với các nút vận chuyểnDataFlavor
    private DefaultMutableTreeNode[] nodesToRemove; // Mảng nút đang được kéo

    public TreeTransferHandler(JTree requestTree, DefaultTreeModel treeModel, Runnable saveRequestGroups) {
        this.requestTree = requestTree;
        this.rootTreeNode = (DefaultMutableTreeNode) treeModel.getRoot();
        this.treeModel = treeModel;
        this.saveRequestGroups = saveRequestGroups;
        try {
            nodesFlavor = new DataFlavor(DefaultMutableTreeNode[].class, "Node Array");
        } catch (Exception e) {
            log.error("Đã xảy ra ngoại lệ khi kéo", e);
        }
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) return false; // Chỉ xử lý các thao tác kéo và thả
        support.setShowDropLocation(true); // Hiển thị vị trí kéo và thả
        if (!support.isDataFlavorSupported(nodesFlavor)) return false; // Chỉ hỗ trợ mảng nútDataFlavor
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation(); // Lấy vị trí kéo và thả
        TreePath dest = dl.getPath(); // Nhận đường dẫn mục tiêu
        TreePath[] selPaths = requestTree.getSelectionPaths(); // Nhận đường dẫn hiện đang được chọn
        if (selPaths == null || dest == null) return false; // Phải có một đường dẫn được chọn và một đường dẫn đích

        DefaultMutableTreeNode draggedNode = (DefaultMutableTreeNode) selPaths[0].getLastPathComponent(); // Lấy nút được kéo
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) dest.getLastPathComponent(); // Lấy nút mục tiêu
        if (draggedNode == rootTreeNode) return false; // Vô hiệu hóa việc kéo nút gốc

        if (CollectionTreeNodes.isGroup(draggedNode)) {
            // Cấm kéo vào chính nó hoặc các nút con của nó
            if (isNodeDescendant(draggedNode, targetNode)) return false;
            if (targetNode == rootTreeNode) return true;
            return CollectionTreeNodes.isGroup(targetNode);
        }

        // Nút yêu cầu chỉ có thể được kéo dưới nút nhóm và không thể được kéo dưới nút yêu cầu
        if (CollectionTreeNodes.isRequest(draggedNode)) {
            return CollectionTreeNodes.isGroup(targetNode);
        }
        return false;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return null;
        List<DefaultMutableTreeNode> copies = new ArrayList<>();
        List<DefaultMutableTreeNode> toRemove = new ArrayList<>();
        for (TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            copies.add(node);
            toRemove.add(node);
        }
        DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[0]);
        nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[0]);
        return new NodesTransferable(nodes);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;
        DefaultMutableTreeNode[] nodes = null;
        try {
            nodes = (DefaultMutableTreeNode[]) support.getTransferable().getTransferData(nodesFlavor);
        } catch (Exception e) {
            log.error("import data error", e);
            return false;
        }
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        int childIndex = dl.getChildIndex();
        TreePath dest = dl.getPath();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
        if (childIndex == -1) childIndex = parent.getChildCount();
        for (DefaultMutableTreeNode node : nodes) {
            // Khi kéo một nút yêu cầu, nó luôn được chèn vào trong nhóm
            if (CollectionTreeNodes.isRequest(node) || CollectionTreeNodes.isGroup(node)) {
                treeModel.insertNodeInto(node, parent, childIndex++);
            }
        }
        return true;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if ((action & MOVE) == MOVE) {
            for (DefaultMutableTreeNode node : nodesToRemove) {
                treeModel.removeNodeFromParent(node);
            }
            if (saveRequestGroups != null) {
                saveRequestGroups.run();
            }
        }
    }

    // Xác định xem nút đích là nút được kéo hay nút con của nó
    private boolean isNodeDescendant(DefaultMutableTreeNode node, DefaultMutableTreeNode possibleDescendant) {
        if (node == possibleDescendant) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (isNodeDescendant(child, possibleDescendant)) return true;
        }
        return false;
    }

    class NodesTransferable implements Transferable {
        private final DefaultMutableTreeNode[] nodes;

        public NodesTransferable(DefaultMutableTreeNode[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) {
            if (!isDataFlavorSupported(flavor)) return null;
            return nodes;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{nodesFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return nodesFlavor.equals(flavor);
        }
    }
}
