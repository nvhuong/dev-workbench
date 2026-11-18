package com.nvhuong.devworkbench.performance.runtime;

import com.nvhuong.devworkbench.performance.core.plan.PerformanceThreadGroupPlan;



import java.util.Map;

@FunctionalInterface
public interface PerformanceIterationDataProvider {
    Map<String, String> dataForVirtualUser(PerformanceThreadGroupPlan groupPlan, int virtualUserIndex);

    static PerformanceIterationDataProvider empty() {
        return (groupPlan, virtualUserIndex) -> null;
    }
}
