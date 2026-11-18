package com.nvhuong.devworkbench.performance.core.plan;

import com.nvhuong.devworkbench.performance.core.extractor.ExtractorData;
import com.nvhuong.devworkbench.performance.core.model.NodeType;

public final class PerformanceExtractorElement implements PerformancePlanElement {
    private final String name;
    private final ExtractorData extractorData;

    public PerformanceExtractorElement(String name, ExtractorData extractorData) {
        this.name = name;
        this.extractorData = PerformancePlanCoreDataCopies.copyExtractorData(extractorData);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.EXTRACTOR;
    }

    public ExtractorData getExtractorData() {
        return PerformancePlanCoreDataCopies.copyExtractorData(extractorData);
    }
}
