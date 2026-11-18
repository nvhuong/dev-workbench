package com.nvhuong.devworkbench.panel.performance.tree;

import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.performance.plan.PerformanceRequestSnapshotMapper;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.service.collections.CollectionRequestExecutionScopeResolver;
import com.nvhuong.devworkbench.service.variable.RequestExecutionScope;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class PerformanceRequestScopeResolver {

    public Optional<RequestExecutionScope> resolveLatestCollectionScope(PerformanceTreeNode nodeData) {
        String requestId = requestId(nodeData);
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }
        return CollectionRequestExecutionScopeResolver.resolveCurrentScope(requestId);
    }

    public RequestExecutionScope resolveEffectiveScope(PerformanceTreeNode nodeData) {
        if (nodeData == null || nodeData.type != NodeType.REQUEST) {
            return RequestExecutionScope.empty();
        }
        return resolveLatestCollectionScope(nodeData)
                .orElseGet(() -> storedScope(nodeData));
    }

    private RequestExecutionScope storedScope(PerformanceTreeNode nodeData) {
        if (nodeData.requestExecutionScope != null) {
            return nodeData.requestExecutionScope;
        }
        return PerformanceRequestSnapshotMapper.toRequestExecutionScope(nodeData.requestSnapshot);
    }

    private String requestId(PerformanceTreeNode nodeData) {
        if (nodeData == null || nodeData.type != NodeType.REQUEST) {
            return null;
        }
        HttpRequestItem item = nodeData.httpRequestItem;
        if (item != null && item.getId() != null && !item.getId().trim().isEmpty()) {
            return item.getId();
        }
        if (nodeData.requestSnapshot != null) {
            return nodeData.requestSnapshot.getId();
        }
        return null;
    }
}
