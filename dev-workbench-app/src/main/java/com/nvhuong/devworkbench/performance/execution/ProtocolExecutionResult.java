package com.nvhuong.devworkbench.performance.execution;


import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.script.model.TestResult;

import java.util.List;

record ProtocolExecutionResult(
        HttpResponse response,
        String errorMsg,
        boolean executionFailed,
        boolean interrupted,
        List<TestResult> testResults
) {
}
