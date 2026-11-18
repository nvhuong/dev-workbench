package com.nvhuong.devworkbench.performance.execution;


import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.service.js.ScriptExecutionResult;

interface PerformanceScriptRuntime {

    ScriptExecutionResult executePreScript();

    void finalizeRequest();

    <T> T withExecutionContextThrowing(ThrowingAction<T> action) throws Exception;

    void withExecutionContext(Runnable action);

    ScriptExecutionResult executePostScript(HttpResponse response);

    ScriptExecutionResult executeWebSocketSendScript(String script,
                                                     int sendIndex,
                                                     int sendCount,
                                                     String stepName);

    @FunctionalInterface
    interface ThrowingAction<T> {
        T execute() throws Exception;
    }
}
