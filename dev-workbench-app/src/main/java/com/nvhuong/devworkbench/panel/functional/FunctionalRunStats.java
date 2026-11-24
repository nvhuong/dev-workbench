package com.nvhuong.devworkbench.panel.functional;

import com.nvhuong.devworkbench.functional.model.AssertionResult;
import com.nvhuong.devworkbench.functional.model.BatchExecutionHistory;
import com.nvhuong.devworkbench.functional.model.RequestResult;

import java.util.Objects;

/**
 * Summary numbers of one functional run, shared by the results overview and the exported report image so both
 * always show the same values.
 */
record FunctionalRunStats(int iterations, int requests, long passed, long failed, long totalTimeMs,
                          long averageTimeMs, double successRate) {

    static FunctionalRunStats of(BatchExecutionHistory history, String skippedStatus) {
        Objects.requireNonNull(history, "history");
        int iterations = history.getIterations().size();
        int requests = history.getIterations().stream().mapToInt(iteration -> iteration.getRequestResults().size()).sum();
        long passed = history.getIterations().stream()
                .flatMap(iteration -> iteration.getRequestResults().stream())
                .filter(request -> AssertionResult.PASS.equals(request.getAssertion()))
                .count();
        long failed = history.getIterations().stream()
                .flatMap(iteration -> iteration.getRequestResults().stream())
                .filter(request -> isFailure(request, skippedStatus))
                .count();
        long withAssertions = passed + failed;
        double successRate = withAssertions > 0 ? (double) passed / withAssertions * 100 : 0;
        long totalTime = history.getExecutionTime();
        long average = requests > 0 ? totalTime / requests : 0;
        return new FunctionalRunStats(iterations, requests, passed, failed, totalTime, average, successRate);
    }

    static boolean isFailure(RequestResult request, String skippedStatus) {
        AssertionResult assertion = request.getAssertion();
        return AssertionResult.FAIL.equals(assertion)
                || (assertion != null && !AssertionResult.PASS.equals(assertion)
                && !AssertionResult.NO_TESTS.equals(assertion)
                && !Objects.equals(skippedStatus, request.getStatus()));
    }
}
