package com.nvhuong.devworkbench.performance.core.plan;

import com.nvhuong.devworkbench.performance.core.model.NodeType;

public interface PerformancePlanElement {
    String getName();

    NodeType getType();
}
