package com.nvhuong.devworkbench.panel.collections.tree.handler;

import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.SavedResponse;

import javax.swing.tree.DefaultMutableTreeNode;

interface RequestTreeOpenActions {
    void openTransientRequest(HttpRequestItem item);

    void openFixedRequest(HttpRequestItem item);

    void openTransientGroup(DefaultMutableTreeNode groupNode, RequestGroup group);

    void openFixedGroup(DefaultMutableTreeNode groupNode, RequestGroup group);

    void openTransientSavedResponse(SavedResponse savedResponse);

    void openFixedSavedResponse(SavedResponse savedResponse);
}
