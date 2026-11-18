package com.nvhuong.devworkbench.performance.core.plan;

import com.nvhuong.devworkbench.performance.core.model.NodeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceOnceOnlyController implements PerformanceElementContainer {
    private final String name;
    private final List<PerformancePlanElement> elements;

    public PerformanceOnceOnlyController(String name, List<PerformancePlanElement> elements) {
        this.name = name;
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.ONCE_ONLY;
    }

    @Override
    public List<PerformancePlanElement> getElements() {
        return elements;
    }
}
