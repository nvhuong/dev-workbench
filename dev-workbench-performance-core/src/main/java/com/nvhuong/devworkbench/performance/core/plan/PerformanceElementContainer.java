package com.nvhuong.devworkbench.performance.core.plan;

import java.util.List;

public interface PerformanceElementContainer extends PerformancePlanElement {

    List<PerformancePlanElement> getElements();
}
