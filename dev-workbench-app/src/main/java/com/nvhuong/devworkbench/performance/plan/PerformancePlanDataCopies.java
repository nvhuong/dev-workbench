package com.nvhuong.devworkbench.performance.plan;

import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.performance.core.assertion.AssertionData;
import com.nvhuong.devworkbench.performance.core.config.CsvDataSetData;
import com.nvhuong.devworkbench.performance.core.controller.ConditionData;
import com.nvhuong.devworkbench.performance.core.controller.LoopData;
import com.nvhuong.devworkbench.performance.core.controller.WhileData;
import com.nvhuong.devworkbench.performance.core.extractor.ExtractorData;
import com.nvhuong.devworkbench.performance.core.model.SsePerformanceData;
import com.nvhuong.devworkbench.performance.core.model.WebSocketPerformanceData;
import com.nvhuong.devworkbench.performance.core.threadgroup.ThreadGroupData;
import com.nvhuong.devworkbench.performance.core.timer.TimerData;


import com.nvhuong.devworkbench.service.variable.RequestExecutionScope;
import com.nvhuong.devworkbench.util.JsonUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformancePlanDataCopies {

    public ThreadGroupData copyThreadGroupData(ThreadGroupData source) {
        return JsonUtil.deepCopy(source, ThreadGroupData.class);
    }

    public CsvDataSetData copyCsvDataSetData(CsvDataSetData source) {
        return JsonUtil.deepCopy(source, CsvDataSetData.class);
    }

    public LoopData copyLoopData(LoopData source) {
        return JsonUtil.deepCopy(source, LoopData.class);
    }

    public ConditionData copyConditionData(ConditionData source) {
        return JsonUtil.deepCopy(source, ConditionData.class);
    }

    public WhileData copyWhileData(WhileData source) {
        return JsonUtil.deepCopy(source, WhileData.class);
    }

    public TimerData copyTimerData(TimerData source) {
        return JsonUtil.deepCopy(source, TimerData.class);
    }

    public HttpRequestItem copyHttpRequestItem(HttpRequestItem source) {
        return JsonUtil.deepCopy(source, HttpRequestItem.class);
    }

    public AssertionData copyAssertionData(AssertionData source) {
        return JsonUtil.deepCopy(source, AssertionData.class);
    }

    public ExtractorData copyExtractorData(ExtractorData source) {
        return JsonUtil.deepCopy(source, ExtractorData.class);
    }

    public SsePerformanceData copySsePerformanceData(SsePerformanceData source) {
        return JsonUtil.deepCopy(source, SsePerformanceData.class);
    }

    public WebSocketPerformanceData copyWebSocketPerformanceData(WebSocketPerformanceData source) {
        return JsonUtil.deepCopy(source, WebSocketPerformanceData.class);
    }

    public RequestExecutionScope copyRequestExecutionScope(RequestExecutionScope source) {
        return source == null ? null : RequestExecutionScope.fromGroupVariables(source.getGroupVariables());
    }
}
