package com.nvhuong.devworkbench.performance.execution;


import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;

record PerformancePreparedRequest(String requestId,
                                  String requestName,
                                  PreparedRequest request,
                                  String requestBodyTemplate,
                                  PerformanceScriptRuntime scriptRuntime) {
}
