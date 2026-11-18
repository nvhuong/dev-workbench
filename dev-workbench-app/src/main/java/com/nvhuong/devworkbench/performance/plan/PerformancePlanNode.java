package com.nvhuong.devworkbench.performance.plan;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.performance.core.assertion.AssertionData;
import com.nvhuong.devworkbench.performance.core.config.CsvDataSetData;
import com.nvhuong.devworkbench.performance.core.controller.ConditionData;
import com.nvhuong.devworkbench.performance.core.controller.LoopData;
import com.nvhuong.devworkbench.performance.core.controller.WhileData;
import com.nvhuong.devworkbench.performance.core.extractor.ExtractorData;
import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.performance.core.model.SsePerformanceData;
import com.nvhuong.devworkbench.performance.core.model.WebSocketPerformanceData;
import com.nvhuong.devworkbench.performance.core.request.PerformanceRequestSnapshot;
import com.nvhuong.devworkbench.performance.core.threadgroup.ThreadGroupData;
import com.nvhuong.devworkbench.performance.core.timer.TimerData;


import com.nvhuong.devworkbench.service.variable.RequestExecutionScope;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
public class PerformancePlanNode {
    String name;
    NodeType type;
    boolean enabled;
    ThreadGroupData threadGroupData;
    CsvDataSetData csvDataSetData;
    LoopData loopData;
    ConditionData conditionData;
    WhileData whileData;
    PerformanceRequestSnapshot requestSnapshot;
    AssertionData assertionData;
    ExtractorData extractorData;
    TimerData timerData;
    SsePerformanceData ssePerformanceData;
    WebSocketPerformanceData webSocketPerformanceData;
    RequestExecutionScope requestExecutionScope;
    List<PerformancePlanNode> children;

    @Builder
    public PerformancePlanNode(String name,
                               NodeType type,
                               Boolean enabled,
                               ThreadGroupData threadGroupData,
                               CsvDataSetData csvDataSetData,
                               LoopData loopData,
                               ConditionData conditionData,
                               WhileData whileData,
                               HttpRequestItem httpRequestItem,
                               PerformanceRequestSnapshot requestSnapshot,
                               AssertionData assertionData,
                               ExtractorData extractorData,
                               TimerData timerData,
                               SsePerformanceData ssePerformanceData,
                               WebSocketPerformanceData webSocketPerformanceData,
                               RequestExecutionScope requestExecutionScope,
                               List<PerformancePlanNode> children) {
        this.name = name;
        this.type = type;
        this.enabled = enabled == null || enabled;
        this.threadGroupData = PerformancePlanDataCopies.copyThreadGroupData(threadGroupData);
        this.csvDataSetData = PerformancePlanDataCopies.copyCsvDataSetData(csvDataSetData);
        this.loopData = PerformancePlanDataCopies.copyLoopData(loopData);
        this.conditionData = PerformancePlanDataCopies.copyConditionData(conditionData);
        this.whileData = PerformancePlanDataCopies.copyWhileData(whileData);
        PerformanceRequestSnapshot canonicalSnapshot = canonicalRequestSnapshot(
                name,
                type,
                requestSnapshot,
                httpRequestItem,
                requestExecutionScope
        );
        this.requestSnapshot = PerformanceRequestSnapshotMapper.copyRequestSnapshot(canonicalSnapshot);
        this.assertionData = PerformancePlanDataCopies.copyAssertionData(assertionData);
        this.extractorData = PerformancePlanDataCopies.copyExtractorData(extractorData);
        this.timerData = PerformancePlanDataCopies.copyTimerData(timerData);
        this.ssePerformanceData = PerformancePlanDataCopies.copySsePerformanceData(ssePerformanceData);
        this.webSocketPerformanceData = PerformancePlanDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
        this.requestExecutionScope = PerformancePlanDataCopies.copyRequestExecutionScope(canonicalRequestExecutionScope(type, requestExecutionScope, canonicalSnapshot));
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null ? List.of() : children));
    }

    public HttpRequestItem getHttpRequestItem() {
        return PerformancePlanDataCopies.copyHttpRequestItem(
                PerformanceRequestSnapshotMapper.toHttpRequestItem(requestSnapshot)
        );
    }

    public PerformanceRequestSnapshot getRequestSnapshot() {
        return PerformanceRequestSnapshotMapper.copyRequestSnapshot(requestSnapshot);
    }

    public RequestExecutionScope getRequestExecutionScope() {
        return PerformancePlanDataCopies.copyRequestExecutionScope(requestExecutionScope);
    }

    private static PerformanceRequestSnapshot canonicalRequestSnapshot(String nodeName,
                                                                       NodeType type,
                                                                       PerformanceRequestSnapshot requestSnapshot,
                                                                       HttpRequestItem httpRequestItem,
                                                                       RequestExecutionScope requestExecutionScope) {
        if (type != NodeType.REQUEST) {
            return null;
        }
        PerformanceRequestSnapshot snapshot = requestSnapshot;
        if (snapshot == null) {
            snapshot = PerformanceRequestSnapshotMapper.fromHttpRequestItem(httpRequestItem, requestExecutionScope);
        }
        return withNodeDisplayName(snapshot, nodeName);
    }

    private static RequestExecutionScope canonicalRequestExecutionScope(NodeType type,
                                                                        RequestExecutionScope requestExecutionScope,
                                                                        PerformanceRequestSnapshot requestSnapshot) {
        if (type != NodeType.REQUEST) {
            return requestExecutionScope;
        }
        if (requestExecutionScope != null) {
            return requestExecutionScope;
        }
        return PerformanceRequestSnapshotMapper.toRequestExecutionScope(requestSnapshot);
    }

    private static PerformanceRequestSnapshot withNodeDisplayName(PerformanceRequestSnapshot snapshot, String nodeName) {
        if (snapshot == null || nodeName == null || nodeName.trim().isEmpty()) {
            return snapshot;
        }
        return snapshot.toBuilder()
                .name(nodeName.trim())
                .build();
    }
}
