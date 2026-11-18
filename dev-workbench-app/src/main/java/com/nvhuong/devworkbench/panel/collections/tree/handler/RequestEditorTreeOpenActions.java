package com.nvhuong.devworkbench.panel.collections.tree.handler;

import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.panel.collections.editor.RequestEditorPanel;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.SavedResponse;

import javax.swing.tree.DefaultMutableTreeNode;

class RequestEditorTreeOpenActions implements RequestTreeOpenActions {
    @Override
    public void openTransientRequest(HttpRequestItem item) {
        editorPanel().showOrCreateTransientTab(item);
    }

    @Override
    public void openFixedRequest(HttpRequestItem item) {
        editorPanel().showOrCreateTab(item);
    }

    @Override
    public void openTransientGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
        editorPanel().showOrCreateTransientTabForGroup(groupNode, group);
    }

    @Override
    public void openFixedGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
        editorPanel().showGroupEditPanel(groupNode, group);
    }

    @Override
    public void openTransientSavedResponse(SavedResponse savedResponse) {
        editorPanel().showOrCreateTransientTabForSavedResponse(savedResponse);
    }

    @Override
    public void openFixedSavedResponse(SavedResponse savedResponse) {
        editorPanel().showOrCreateTabForSavedResponse(savedResponse);
    }

    private RequestEditorPanel editorPanel() {
        return UiSingletonFactory.getInstance(RequestEditorPanel.class);
    }
}
