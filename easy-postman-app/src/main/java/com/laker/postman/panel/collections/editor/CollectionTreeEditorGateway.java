package com.laker.postman.panel.collections.editor;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

/**
 * Yêu cầu người soạn thảo truy cập vào cây bộ sưu tập Swing cổng.
 * <p>
 * Điểm cốt lõi: Bộ điều khiển soạn thảo không tìm kiếm trực tiếp CollectionTreePanel，Nếu cây bộ sưu tập được thay thế sau đó, .
 * Bạn chỉ cần thay thế cổng này thay vì thay đổi nó ở mọi nơi trong bộ điều khiển lưu, mở và chỉnh sửa nhóm.
 */
public final class CollectionTreeEditorGateway {

    public TreeModel groupTreeModel() {
        return collectionTreePanel().getGroupTreeModel();
    }

    public void saveRequestToGroup(RequestGroup group, HttpRequestItem item) {
        collectionTreePanel().saveRequestToGroup(group, item);
    }

    public boolean updateExistingRequest(HttpRequestItem item) {
        return collectionTreePanel().updateExistingRequest(item);
    }

    public void saveGroupNode(DefaultMutableTreeNode groupNode) {
        CollectionTreePanel collectionTreePanel = collectionTreePanel();
        collectionTreePanel.getTreeModel().nodeChanged(groupNode);
        collectionTreePanel.getCollectionTreePersistence().saveCurrentTree();
    }

    public boolean saveResponseForRequest(HttpRequestItem requestItem, SavedResponse savedResponse) {
        return collectionTreePanel().saveResponseForRequest(requestItem, savedResponse);
    }

    public boolean upsertResponseForRequest(String requestId, SavedResponse savedResponse) {
        return collectionTreePanel().upsertResponseForRequest(requestId, savedResponse);
    }

    public boolean deleteResponseForRequest(String requestId, String responseId) {
        return collectionTreePanel().deleteResponseForRequest(requestId, responseId);
    }

    public HttpRequestItem findRequestContainingResponse(String responseId) {
        return collectionTreePanel().findRequestContainingResponse(responseId);
    }

    private CollectionTreePanel collectionTreePanel() {
        return UiSingletonFactory.getInstance(CollectionTreePanel.class);
    }
}
