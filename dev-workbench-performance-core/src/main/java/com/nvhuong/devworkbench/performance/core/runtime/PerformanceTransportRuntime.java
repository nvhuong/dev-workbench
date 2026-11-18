package com.nvhuong.devworkbench.performance.core.runtime;

import com.nvhuong.devworkbench.performance.core.model.PerformanceSampleRecord;
import com.nvhuong.devworkbench.performance.core.request.PerformanceOutboundRequest;

public interface PerformanceTransportRuntime extends PerformanceNetworkControl {

    PerformanceSampleRecord execute(PerformanceOutboundRequest request);
}
