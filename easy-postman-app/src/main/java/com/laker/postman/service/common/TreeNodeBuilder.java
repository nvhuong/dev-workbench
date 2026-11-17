package com.laker.postman.service.common;

import com.laker.postman.collection.model.CollectionNode;
import com.laker.postman.collection.model.CollectionParseResult;
import com.laker.postman.service.collections.CollectionTreeNodes;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * TreeNode Xây dựng các lớp công cụ
 * thống nhất UI Logic chuyển đổi lớp, chuyển đổi kết quả phân tích cú pháp thành Swing TreeNode
 */
public class TreeNodeBuilder {

    private TreeNodeBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Được xây dựng từ các kết quả được phân tích cú pháp TreeNode
     *
     * @param parseResult Phân tích kết quả
     * @return TreeNode
     */
    public static DefaultMutableTreeNode buildFromParseResult(CollectionParseResult parseResult) {
        // Tạo các nút nhóm
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(parseResult.getGroup());

        // Xây dựng các nút con theo cách đệ quy
        for (CollectionNode child : parseResult.getChildren()) {
            DefaultMutableTreeNode childNode = buildFromCollectionNode(child);
            groupNode.add(childNode);
        }

        return groupNode;
    }

    /**
     * Xây dựng đệ quy TreeNode
     *
     * @param node Collection nút
     * @return TreeNode
     */
    private static DefaultMutableTreeNode buildFromCollectionNode(CollectionNode node) {
        if (node.isGroup()) {
            // Các nút nhóm
            DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(node.asGroup());

            // Xử lý các nút con theo cách đệ quy
            for (CollectionNode child : node.getChildren()) {
                groupNode.add(buildFromCollectionNode(child));
            }

            return groupNode;
        } else {
            return CollectionTreeNodes.requestNodeWithExamples(node.asRequest());
        }
    }
}
