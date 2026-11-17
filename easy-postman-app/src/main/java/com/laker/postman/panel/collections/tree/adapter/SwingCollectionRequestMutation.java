package com.laker.postman.panel.collections.tree.adapter;

import com.laker.postman.request.edit.HttpRequestSaveMerger;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.collections.CollectionTreeNodes;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Optional;

@UtilityClass
public class SwingCollectionRequestMutation {

    /**
     * Chỉ cập nhật Swing Ảnh chụp nhanh yêu cầu trong nút cây; người gọi có trách nhiệm kiên trì và UI Làm mới.
     */
    public Optional<Result> updateExistingRequest(DefaultMutableTreeNode rootTreeNode,
                                                  HttpRequestItem editedItem) {
        if (editedItem == null || editedItem.getId() == null || editedItem.getId().isEmpty()) {
            return Optional.empty();
        }

        DefaultMutableTreeNode requestNode = SwingCollectionTreeQueries.findRequestNodeById(rootTreeNode, editedItem.getId());
        if (requestNode == null) {
            return Optional.empty();
        }

        HttpRequestItem persistedItem = CollectionTreeNodes.request(requestNode).orElse(null);
        if (persistedItem == null) {
            return Optional.empty();
        }

        HttpRequestItem updatedItem = HttpRequestSaveMerger.mergeExisting(persistedItem, editedItem);
        CollectionTreeNodes.setRequest(requestNode, updatedItem);
        return Optional.of(new Result(requestNode, updatedItem));
    }

    public record Result(DefaultMutableTreeNode requestNode, HttpRequestItem updatedItem) {
    }
}
