package com.nvhuong.devworkbench.plugin.host;

import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.model.RequestImportDraft;
import com.nvhuong.devworkbench.model.RequestImportResult;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.panel.collections.editor.RequestEditorPanel;
import com.nvhuong.devworkbench.panel.collections.tree.CollectionGroupSelectionDialog;
import com.nvhuong.devworkbench.panel.collections.tree.CollectionTreePanel;
import com.nvhuong.devworkbench.plugin.api.service.RequestCollectionImportService;
import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import javax.swing.tree.TreeModel;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GUI Yêu cầu của máy chủ nhập việc triển khai dịch vụ.
 * vượt qua Swing Chọn bộ sưu tập đích và đồng bộ hóa kết quả nhập vào cây bộ sưu tập và yêu cầu trình chỉnh sửa.
 */
@Slf4j
public class AppSwingRequestCollectionImportService implements RequestCollectionImportService {

    @Override
    public RequestImportResult importRequests(List<RequestImportDraft> requests) {
        if (requests == null || requests.isEmpty()) {
            return RequestImportResult.imported(0);
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return importRequestsOnEdt(requests);
        }
        AtomicReference<RequestImportResult> result = new AtomicReference<>(RequestImportResult.unavailable());
        try {
            SwingUtilities.invokeAndWait(() -> result.set(importRequestsOnEdt(requests)));
        } catch (Exception e) {
            log.warn("Failed to import requests into collection", e);
            return RequestImportResult.unavailable();
        }
        return result.get();
    }

    private RequestImportResult importRequestsOnEdt(List<RequestImportDraft> requests) {
        CollectionTreePanel collectionPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        RequestEditorPanel requestEditorPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        TreeModel groupTreeModel = collectionPanel.getGroupTreeModel();
        if (groupTreeModel == null || groupTreeModel.getRoot() == null) {
            return RequestImportResult.unavailable();
        }

        RequestGroup group = CollectionGroupSelectionDialog.chooseGroup(groupTreeModel).orElse(null);
        if (group == null) {
            return RequestImportResult.cancelled();
        }

        HttpRequestItem lastImported = null;
        for (RequestImportDraft draft : requests) {
            HttpRequestItem item = RequestImportDraftMapper.toHttpRequestItem(draft);
            collectionPanel.saveRequestToGroup(group, item);
            lastImported = item;
        }

        if (lastImported != null) {
            collectionPanel.locateAndSelectRequest(lastImported.getId());
            requestEditorPanel.showOrCreateTab(lastImported);
        }
        return RequestImportResult.imported(requests.size());
    }
}
