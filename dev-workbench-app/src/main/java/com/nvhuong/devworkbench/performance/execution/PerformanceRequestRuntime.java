package com.nvhuong.devworkbench.performance.execution;

import com.nvhuong.devworkbench.performance.core.request.PerformanceRequestSnapshot;


import com.nvhuong.devworkbench.performance.plan.PerformanceRequestSampler;
import com.nvhuong.devworkbench.service.variable.ExecutionVariableContext;

interface PerformanceRequestRuntime {
    PerformancePreparedRequest prepare(PerformanceRequestSnapshot requestSnapshot,
                                       PerformanceRequestSampler requestSampler,
                                       ExecutionVariableContext iterationContext,
                                       PerformanceExecutionConfig executionConfig);
}
