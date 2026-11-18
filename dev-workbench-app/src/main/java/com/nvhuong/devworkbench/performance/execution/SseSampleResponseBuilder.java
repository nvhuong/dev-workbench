package com.nvhuong.devworkbench.performance.execution;

import com.nvhuong.devworkbench.performance.core.model.SsePerformanceData;


import cn.hutool.core.text.CharSequenceUtil;
import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.LinkedHashMap;

@UtilityClass
class SseSampleResponseBuilder {

    void addSummaryHeaders(HttpResponse response,
                           SsePerformanceData config,
                           int eventCount,
                           int matchedMessageCount,
                           long firstEventLatencyMs,
                           String lastEventId,
                           String lastEventType,
                           String errorMessage) {
        if (response.headers == null) {
            response.headers = new LinkedHashMap<>();
        }
        SsePerformanceData headerConfig = config == null ? new SsePerformanceData() : config;
        response.addHeader("X-DevWorkbench-SSE-Mode", Collections.singletonList(headerConfig.completionMode.name()));
        response.addHeader("X-DevWorkbench-SSE-Event-Filter", Collections.singletonList(CharSequenceUtil.blankToDefault(headerConfig.eventNameFilter, "")));
        response.addHeader("X-DevWorkbench-SSE-Message-Filter", Collections.singletonList(CharSequenceUtil.blankToDefault(headerConfig.messageFilter, "")));
        response.addHeader("X-DevWorkbench-SSE-Event-Count", Collections.singletonList(String.valueOf(eventCount)));
        response.addHeader("X-DevWorkbench-SSE-Message-Count", Collections.singletonList(String.valueOf(matchedMessageCount)));
        String firstEventLatencyHeader = firstEventLatencyMs >= 0 ? String.valueOf(firstEventLatencyMs) : "";
        response.addHeader("X-DevWorkbench-SSE-First-Event-Latency-Ms", Collections.singletonList(firstEventLatencyHeader));
        response.addHeader("X-DevWorkbench-SSE-Event-Id", Collections.singletonList(CharSequenceUtil.blankToDefault(lastEventId, "")));
        response.addHeader("X-DevWorkbench-SSE-Event-Type", Collections.singletonList(CharSequenceUtil.blankToDefault(lastEventType, "")));
        if (CharSequenceUtil.isNotBlank(errorMessage)) {
            response.addHeader("X-DevWorkbench-SSE-Error", Collections.singletonList(errorMessage));
        }
    }
}
