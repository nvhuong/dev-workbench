package com.nvhuong.devworkbench.performance.execution;


import com.nvhuong.devworkbench.http.runtime.transport.DefaultHttpTransport;
import com.nvhuong.devworkbench.http.runtime.transport.HttpExchangeOptions;
import com.nvhuong.devworkbench.http.runtime.transport.HttpTransport;

import java.util.List;

final class HttpSamplerExecutor implements PerformanceProtocolSamplerExecutor {
    private final PerformanceNetworkRuntime networkRuntime;
    private final HttpTransport httpTransport;

    HttpSamplerExecutor() {
        this(new DefaultPerformanceNetworkRuntime());
    }

    HttpSamplerExecutor(PerformanceNetworkRuntime networkRuntime) {
        this(networkRuntime, new DefaultHttpTransport());
    }

    HttpSamplerExecutor(PerformanceNetworkRuntime networkRuntime, HttpTransport httpTransport) {
        this.networkRuntime = networkRuntime == null ? new DefaultPerformanceNetworkRuntime() : networkRuntime;
        this.httpTransport = httpTransport == null ? new DefaultHttpTransport() : httpTransport;
    }

    @Override
    public ProtocolExecutionResult execute(PerformanceProtocolSamplerContext context) throws Exception {
        return new ProtocolExecutionResult(
                httpTransport.execute(
                        context.getRequest(),
                        HttpExchangeOptions.builder()
                                .callTracker(networkRuntime)
                                .baseClientProvider(networkRuntime)
                                .build()
                ),
                "",
                false,
                false,
                List.of()
        );
    }
}
