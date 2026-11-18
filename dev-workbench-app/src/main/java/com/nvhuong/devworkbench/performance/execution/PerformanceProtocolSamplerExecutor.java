package com.nvhuong.devworkbench.performance.execution;


@FunctionalInterface
interface PerformanceProtocolSamplerExecutor {
    ProtocolExecutionResult execute(PerformanceProtocolSamplerContext context) throws Exception;
}
