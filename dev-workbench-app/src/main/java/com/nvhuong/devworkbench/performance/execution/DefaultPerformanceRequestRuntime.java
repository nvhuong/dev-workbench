package com.nvhuong.devworkbench.performance.execution;

import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.performance.core.request.PerformanceRequestSnapshot;


import com.nvhuong.devworkbench.performance.plan.PerformanceRequestSampler;
import com.nvhuong.devworkbench.performance.plan.PerformanceRequestSnapshotMapper;
import com.nvhuong.devworkbench.http.request.PreparedRequestFactory;
import com.nvhuong.devworkbench.service.js.ScriptExecutionPipeline;
import com.nvhuong.devworkbench.service.variable.ExecutionVariableContext;

final class DefaultPerformanceRequestRuntime implements PerformanceRequestRuntime {

    @Override
    public PerformancePreparedRequest prepare(PerformanceRequestSnapshot requestSnapshot,
                                              PerformanceRequestSampler requestSampler,
                                              ExecutionVariableContext iterationContext,
                                              PerformanceExecutionConfig executionConfig) {
        HttpRequestItem requestItem = PerformanceRequestSnapshotMapper.toHttpRequestItem(requestSnapshot);
        if (requestItem == null) {
            return null;
        }

        PerformanceExecutionConfig resolvedConfig = executionConfig == null
                ? PerformanceExecutionConfig.DEFAULT
                : executionConfig;
        PreparedRequest request = PreparedRequestFactory.buildWithoutInheritance(requestItem);
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript(request.prescript)
                .postScript(request.postscript)
                .sharedExecutionContext(iterationContext)
                .requestExecutionScope(requestSampler == null ? null : requestSampler.getRequestExecutionScope())
                .deferredAuthorization(PreparedRequestFactory.resolveDeferredAuthorizationWithoutInheritance(requestItem))
                .outputCallback(resolvedConfig.scriptOutputCallback())
                .environmentSupplier(resolvedConfig.environmentSupplier())
                .scriptExecutor(resolvedConfig.scriptExecutor())
                .build();

        return new PerformancePreparedRequest(
                requestItem.getId(),
                requestItem.getName(),
                request,
                request.body,
                new DefaultPerformanceScriptRuntime(pipeline)
        );
    }
}
