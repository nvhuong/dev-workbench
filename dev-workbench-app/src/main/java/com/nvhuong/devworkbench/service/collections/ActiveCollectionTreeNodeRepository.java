package com.nvhuong.devworkbench.service.collections;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Optional;

/**
 * Kho nút cây được triển khai theo mặc định.
 * <p>
 * vượt qua CollectionTreeRootRegistry Đọc hiện tại UI Đăng ký collection nút gốc cây,
 * service Các lớp không được tạo hoặc tìm thấy trực tiếp Swing panel。
 */
@Slf4j
public class ActiveCollectionTreeNodeRepository implements TreeNodeRepository {

    @Override
    public Optional<DefaultMutableTreeNode> findNodeByRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }

        Optional<DefaultMutableTreeNode> rootOpt = getRootNode();
        if (rootOpt.isEmpty()) {
            log.trace("Nút gốc trống và không thể tìm thấy nút đó: {}", requestId);
            return Optional.empty();
        }

        return findByTraversal(rootOpt.get(), requestId);
    }

    @Override
    public Optional<DefaultMutableTreeNode> getRootNode() {
        return CollectionTreeRootRegistry.getRootNode();
    }

    private Optional<DefaultMutableTreeNode> findByTraversal(
            DefaultMutableTreeNode root,
            String requestId) {

        if (root == null || requestId == null) {
            return Optional.empty();
        }

        Optional<HttpRequestItem> request = CollectionTreeNodes.request(root);
        if (request.isPresent() && requestId.equals(request.get().getId())) {

            log.trace("Tìm nút: {}", requestId);
            return Optional.of(root);
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode child = root.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode childNode) {
                Optional<DefaultMutableTreeNode> result = findByTraversal(childNode, requestId);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return Optional.empty();
    }
}
