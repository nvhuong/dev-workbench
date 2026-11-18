package com.nvhuong.devworkbench.workspace.cli;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;

@FunctionalInterface
public interface WorkspaceRunPlanner {
    WorkspaceRunPlan plan(WorkspaceRunWorkspace workspace, CollectionDocument document) throws Exception;
}
