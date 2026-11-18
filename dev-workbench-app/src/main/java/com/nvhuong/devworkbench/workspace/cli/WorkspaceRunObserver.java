package com.nvhuong.devworkbench.workspace.cli;

import com.nvhuong.devworkbench.functional.execution.FunctionalRequestExecutionResult;

@FunctionalInterface
public interface WorkspaceRunObserver {
    WorkspaceRunObserver NO_OP = (iteration, request, execution, report) -> {
    };

    void onRequestCompleted(int iteration,
                            WorkspaceRunSelectedRequest request,
                            FunctionalRequestExecutionResult execution,
                            WorkspaceRunReport.RequestResult report);
}
