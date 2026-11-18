package com.nvhuong.devworkbench.performance.core.plan;

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
import com.nvhuong.devworkbench.util.JsonUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
class PerformancePlanCoreDataCopies {

    ThreadGroupData copyThreadGroupData(ThreadGroupData source) {
        return JsonUtil.deepCopy(source, ThreadGroupData.class);
    }

    CsvDataSetData copyCsvDataSetData(CsvDataSetData source) {
        return JsonUtil.deepCopy(source, CsvDataSetData.class);
    }

    LoopData copyLoopData(LoopData source) {
        return JsonUtil.deepCopy(source, LoopData.class);
    }

    ConditionData copyConditionData(ConditionData source) {
        return JsonUtil.deepCopy(source, ConditionData.class);
    }

    WhileData copyWhileData(WhileData source) {
        return JsonUtil.deepCopy(source, WhileData.class);
    }

    TimerData copyTimerData(TimerData source) {
        return JsonUtil.deepCopy(source, TimerData.class);
    }

    AssertionData copyAssertionData(AssertionData source) {
        return JsonUtil.deepCopy(source, AssertionData.class);
    }

    ExtractorData copyExtractorData(ExtractorData source) {
        return JsonUtil.deepCopy(source, ExtractorData.class);
    }

    SsePerformanceData copySsePerformanceData(SsePerformanceData source) {
        return JsonUtil.deepCopy(source, SsePerformanceData.class);
    }

    WebSocketPerformanceData copyWebSocketPerformanceData(WebSocketPerformanceData source) {
        return JsonUtil.deepCopy(source, WebSocketPerformanceData.class);
    }
}
