package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.script.model.TestResult;
import com.nvhuong.devworkbench.service.js.ScriptExecutionPipeline;

import java.util.List;

interface WebSocketResponseHandler {
    List<TestResult> handleStreamMessage(ScriptExecutionPipeline pipeline, String message);

    void saveHistory(PreparedRequest request, HttpResponse response, String label);
}
