package com.nvhuong.devworkbench.panel.performance.component;

import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.performance.core.model.NodeType;
import org.testng.annotations.Test;

import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import static org.testng.Assert.assertEquals;

public class TreeNodeTransferHandlerTest {

    @Test(description = "Quy tắc kéo cho các nút giai đoạn giao thức phải được sao chép bằng nhấp chuột phải/Các quy tắc xóa là như nhau")
    public void shouldAllowProtocolStageNodesAsDragSource() {
        assertEquals(sourceActionFor(NodeType.SSE_CONNECT), TransferHandler.MOVE);
        assertEquals(sourceActionFor(NodeType.SSE_READ), TransferHandler.MOVE);
        assertEquals(sourceActionFor(NodeType.WS_CONNECT), TransferHandler.MOVE);
    }

    private static int sourceActionFor(NodeType type) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new PerformanceTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(new PerformanceTreeNode(type.name(), type));
        root.add(child);
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        JTree tree = new JTree(treeModel);
        tree.setSelectionPath(new TreePath(child.getPath()));
        return new TreeNodeTransferHandler(tree, treeModel).getSourceActions(tree);
    }
}
