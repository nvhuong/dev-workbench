package com.nvhuong.devworkbench.functional.execution;

import com.nvhuong.devworkbench.functional.model.AssertionResult;
import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.script.model.TestResult;
import lombok.Value;

import java.util.List;

@Value
public class FunctionalRequestExecutionResult {
    PreparedRequest request;
    HttpResponse response;
    long cost;
    String status;
    String errorMessage;
    AssertionResult assertion;
    List<TestResult> testResults;

    public static FunctionalRequestExecutionResult skipped() {
        return new FunctionalRequestExecutionResult(
                null,
                null,
                0L,
                "",
                null,
                AssertionResult.NO_TESTS,
                null
        );
    }
}
