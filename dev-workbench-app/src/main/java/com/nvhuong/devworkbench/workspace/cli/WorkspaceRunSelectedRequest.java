package com.nvhuong.devworkbench.workspace.cli;

import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;

import java.util.List;

public record WorkspaceRunSelectedRequest(
        HttpRequestItem request,
        List<RequestGroup> groupChain,
        String path
) {
    public WorkspaceRunSelectedRequest {
        groupChain = groupChain == null ? List.of() : List.copyOf(groupChain);
    }
}
