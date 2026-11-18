package com.nvhuong.devworkbench.performance.core.plan;

import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.performance.core.timer.TimerData;

public final class PerformanceTimerElement implements PerformancePlanElement {
    private final String name;
    private final TimerData timerData;

    public PerformanceTimerElement(String name, TimerData timerData) {
        this.name = name;
        this.timerData = PerformancePlanCoreDataCopies.copyTimerData(timerData);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.TIMER;
    }

    public TimerData getTimerData() {
        return PerformancePlanCoreDataCopies.copyTimerData(timerData);
    }
}
