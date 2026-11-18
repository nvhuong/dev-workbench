package com.nvhuong.devworkbench.panel.collections.tree.action;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import lombok.experimental.UtilityClass;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

import static com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel.*;

/**
 * Lớp trợ giúp trạng thái cây
 * Dùng để lưu và khôi phục trạng thái mở rộng của cây
 */
@UtilityClass
public class TreeStateHelper {

    /**
     * Lưu tất cả các đường dẫn mở rộng
     */
    public static List<TreePath> saveExpandedPaths(JTree tree) {
        List<TreePath> expandedPaths = new ArrayList<>();
        int rowCount = tree.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            TreePath path = tree.getPathForRow(i);
            if (path != null && tree.isExpanded(path)) {
                expandedPaths.add(path);
            }
        }
        return expandedPaths;
    }

    /**
     * Khôi phục đường dẫn mở rộng
     */
    public static void restoreExpandedPaths(JTree tree, List<TreePath> expandedPaths,
                                            DefaultMutableTreeNode rootNode) {
        if (expandedPaths == null || expandedPaths.isEmpty()) {
            return;
        }

        for (TreePath oldPath : expandedPaths) {
            TreePath newPath = findMatchingPath(oldPath, rootNode);
            if (newPath != null) {
                tree.expandPath(newPath);
            }
        }
    }

    /**
     * Nút dựa trên đường dẫn cũuserObjectTìm đường dẫn mới phù hợp
     */
    private static TreePath findMatchingPath(TreePath oldPath, DefaultMutableTreeNode rootNode) {
        Object[] oldNodes = oldPath.getPath();
        if (oldNodes.length == 0) {
            return null;
        }

        DefaultMutableTreeNode currentNode = rootNode;
        List<DefaultMutableTreeNode> matchedNodes = new ArrayList<>();
        matchedNodes.add(currentNode);

        // Bỏ qua nút gốc và bắt đầu khớp từ nút con đầu tiên
        for (int i = 1; i < oldNodes.length; i++) {
            DefaultMutableTreeNode oldNode = (DefaultMutableTreeNode) oldNodes[i];
            Object oldUserObj = oldNode.getUserObject();

            DefaultMutableTreeNode matchedChild = findMatchingChild(currentNode, oldUserObj);
            if (matchedChild == null) {
                return null;
            }

            matchedNodes.add(matchedChild);
            currentNode = matchedChild;
        }

        return new TreePath(matchedNodes.toArray());
    }

    /**
     * Tìm các nút phù hợp trong số các nút con của nút hiện tại
     */
    private static DefaultMutableTreeNode findMatchingChild(DefaultMutableTreeNode parent, Object targetUserObj) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (isSameNode(child.getUserObject(), targetUserObj)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Đánh giá hai nútuserObjectCho dù nó đại diện cho cùng một nút
     */
    private static boolean isSameNode(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }

        // So sánh nút gốc
        if (ROOT.equals(obj1) && ROOT.equals(obj2)) {
            return true;
        }

        // So sánh nút kiểu mảng
        if (obj1 instanceof Object[] arr1 && obj2 instanceof Object[] arr2) {
            if (arr1.length < 2 || arr2.length < 2) {
                return false;
            }

            String type1 = (String) arr1[0];
            String type2 = (String) arr2[0];

            if (!type1.equals(type2)) {
                return false;
            }

            if (GROUP.equals(type1)) {
                // Các nút được nhóm được so sánh theo tên
                return arr1[1].equals(arr2[1]);
            } else if (REQUEST.equals(type1)) {
                // Yêu cầu nhấn nútIDSo sánh
                HttpRequestItem item1 = (HttpRequestItem) arr1[1];
                HttpRequestItem item2 = (HttpRequestItem) arr2[1];
                return item1.getId().equals(item2.getId());
            }
        }

        return false;
    }
}

