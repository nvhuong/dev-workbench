package com.nvhuong.devworkbench.performance.execution;

import com.nvhuong.devworkbench.performance.core.request.PerformanceRequestSnapshot;


import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.performance.plan.PerformanceRequestSampler;
import lombok.Value;

@Value
class PerformanceProtocolSamplerContext {
    PreparedRequest request;
    PerformanceRequestSampler requestSampler;
    PerformanceRequestSnapshot requestSnapshot;
    String requestBodyTemplate;
    PerformanceScriptRuntime scriptRuntime;
    PerformanceResponseCapturePlan capturePlan;

    String getRequestId() {
        return requestSnapshot == null ? "" : requestSnapshot.getId();
    }

    String getRequestName() {
        return requestSnapshot == null ? "" : requestSnapshot.getName();
    }
}
