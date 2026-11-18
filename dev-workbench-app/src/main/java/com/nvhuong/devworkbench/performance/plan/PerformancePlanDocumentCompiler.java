package com.nvhuong.devworkbench.performance.plan;

import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.performance.core.plan.PerformanceCorePlanDocumentCompiler;
import com.nvhuong.devworkbench.performance.core.plan.PerformanceCoreRequestSampler;
import com.nvhuong.devworkbench.performance.core.plan.PerformanceTestPlan;


import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformancePlanDocumentCompiler {

    public PerformanceTestPlan compile(PerformancePlanDocument document) {
        PerformanceTestPlan corePlan = PerformanceCorePlanDocumentCompiler.compile(
                PerformanceCorePlanAdapter.toCoreDocument(document)
        );
        return PerformanceCorePlanAdapter.toExecutablePlan(corePlan);
    }

    public PerformanceTestPlan compile(PerformancePlanNode root) {
        PerformanceTestPlan corePlan = PerformanceCorePlanDocumentCompiler.compile(
                PerformanceCorePlanAdapter.toCoreNode(root)
        );
        return PerformanceCorePlanAdapter.toExecutablePlan(corePlan);
    }

    public PerformanceRequestSampler compileRequestSampler(PerformancePlanNode requestNode) {
        if (requestNode == null || !requestNode.isEnabled() || requestNode.getType() != NodeType.REQUEST) {
            return null;
        }
        PerformanceCoreRequestSampler coreSampler = PerformanceCorePlanDocumentCompiler.compileRequestSampler(
                PerformanceCorePlanAdapter.toCoreNode(requestNode)
        );
        return PerformanceCorePlanAdapter.toAppRequestSampler(coreSampler);
    }
}
