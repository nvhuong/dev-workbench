package com.nvhuong.devworkbench.performance.runtime;

import com.nvhuong.devworkbench.performance.core.plan.PerformanceTestPlan;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerformanceRunRequest {
    PerformanceTestPlan plan;
    PerformanceResultSink resultSink;
}
