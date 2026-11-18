package com.nvhuong.devworkbench.panel.collections.tree.action;

import com.nvhuong.devworkbench.collection.CollectionIdNormalizer;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.request.model.SavedResponse;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.service.collections.CollectionTreeNodes;
import com.nvhuong.devworkbench.util.JsonUtil;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.UUID;

/**
 * Lớp công cụ nhân bản nút cây
 */
@UtilityClass
public class TreeNodeCloner {

    /**
     * Sao chép sâu nút được nhóm và tất cả các nút con của nó
     */
    public static DefaultMutableTreeNode deepCopyGroupNode(DefaultMutableTreeNode node) {
        RequestGroup originalGroup = CollectionTreeNodes.group(node)
                .orElseThrow(() -> new IllegalArgumentException("Expected group node: " + node));
        RequestGroup copiedGroup = JsonUtil.deepCopy(originalGroup, RequestGroup.class);
        copiedGroup.setId(UUID.randomUUID().toString());
        DefaultMutableTreeNode copy = CollectionTreeNodes.groupNode(copiedGroup);

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            copy.add(copyChildNode(child));
        }

        return copy;
    }

    /**
     * Sao chép các nút con
     */
    private static DefaultMutableTreeNode copyChildNode(DefaultMutableTreeNode child) {
        Object childUserObj = child.getUserObject();
        if (!(childUserObj instanceof Object[])) {
            return new DefaultMutableTreeNode(childUserObj);
        }

        if (CollectionTreeNodes.isGroup(child)) {
            return deepCopyGroupNode(child);
        } else if (CollectionTreeNodes.isRequest(child)) {
            return copyRequestNode(child);
        } else if (CollectionTreeNodes.isSavedResponse(child)) {
            SavedResponse savedResponse = CollectionTreeNodes.savedResponse(child).orElseThrow();
            return CollectionTreeNodes.savedResponseNode(savedResponse);
        }

        return new DefaultMutableTreeNode(childUserObj);
    }

    /**
     * Nút yêu cầu sao chép sâu
     */
    private static DefaultMutableTreeNode copyRequestNode(DefaultMutableTreeNode child) {
        HttpRequestItem item = CollectionTreeNodes.request(child).orElseThrow();
        return CollectionTreeNodes.requestNodeWithExamples(CollectionIdNormalizer.copyRequestWithNewIds(item));
    }

    /**
     * Sao chép đệ quy các nút cây (được sử dụng để tạo cây lựa chọn)
     * sẽ được lọc ra SavedResponse loại yêu cầu.
     */
    public static DefaultMutableTreeNode cloneTreeNode(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(
                userObj instanceof Object[] ? ((Object[]) userObj).clone() : userObj
        );

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);

            // Kiểm tra xem nút này có cần được lọc không
            if (shouldFilterNode(child)) {
                continue; // Bỏ qua SavedResponse loại yêu cầu
            }

            copy.add(cloneTreeNode(child));
        }

        return copy;
    }

    /**
     * Xác định xem nút có nên được lọc ra (SavedResponse）
     */
    private static boolean shouldFilterNode(DefaultMutableTreeNode node) {
        return CollectionTreeNodes.isSavedResponse(node);
    }
}
