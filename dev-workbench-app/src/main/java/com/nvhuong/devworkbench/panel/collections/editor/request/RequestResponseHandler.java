package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.common.UiSingletonFactory;
import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.script.model.TestResult;
import com.nvhuong.devworkbench.panel.collections.editor.request.sub.ResponsePanel;
import com.nvhuong.devworkbench.panel.history.HistoryPanel;
import com.nvhuong.devworkbench.panel.sidebar.ConsolePanel;
import com.nvhuong.devworkbench.service.js.ScriptExecutionPipeline;
import com.nvhuong.devworkbench.service.js.ScriptExecutionResult;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class RequestResponseHandler implements WebSocketResponseHandler {
    private final Component owner;
    private final ResponsePanel responsePanel;
    private final Consumer<List<TestResult>> testResultsConsumer;
    private final BiConsumer<PreparedRequest, HttpResponse> exchangeRecorder;

    void handleResponse(ScriptExecutionPipeline pipeline, PreparedRequest request, HttpResponse response) {
        if (response == null) {
            log.error("Response is null, cannot handle response.");
            return;
        }

        recordExchange(request, response);
        responsePanel.setRequestDetails(request);
        responsePanel.setResponseDetails(response);

        try {
            ScriptExecutionResult postResult = pipeline.executePostScript(response);
            if (!postResult.isSuccess()) {
                String errorMessage = I18nUtil.getMessage(MessageKeys.SCRIPT_POSTSCRIPT_EXECUTION_FAILED,
                        postResult.getErrorMessage());
                String errorTitle = I18nUtil.getMessage(MessageKeys.SCRIPT_POSTSCRIPT_ERROR_TITLE);
                JOptionPane.showMessageDialog(owner, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
            }
            testResultsConsumer.accept(postResult.getTestResults());
        } catch (Exception ex) {
            log.error("Error executing post-script: {}", ex.getMessage(), ex);
            ConsolePanel.appendLog("[Error] Post-script execution failed: " + ex.getMessage(), ConsolePanel.LogType.ERROR);
        }

        saveHistory(request, response, "request");
    }

    public List<TestResult> handleStreamMessage(ScriptExecutionPipeline pipeline, String message) {
        HttpResponse response = new HttpResponse();
        response.body = message;
        response.bodySize = message != null ? message.length() : 0;

        ScriptExecutionResult postResult = pipeline.executePostScript(response);
        if (!postResult.isSuccess()) {
            log.warn("Post-script execution failed for stream message: {}", postResult.getErrorMessage());
        }
        return postResult.getTestResults();
    }

    void recordExchange(PreparedRequest request, HttpResponse response) {
        exchangeRecorder.accept(request, response);
    }

    public void saveHistory(PreparedRequest request, HttpResponse response, String label) {
        try {
            UiSingletonFactory.getInstance(HistoryPanel.class).addRequestHistory(request, response);
        } catch (Exception ex) {
            log.error("Error saving {} to history: {}", label, ex.getMessage(), ex);
            appendHistoryWarning(label, ex);
        }
    }

    private void appendHistoryWarning(String label, Exception ex) {
        try {
            ConsolePanel.appendLog("[Warning] Failed to save " + label + " to history: " + ex.getMessage(),
                    ConsolePanel.LogType.WARN);
        } catch (RuntimeException consoleEx) {
            log.debug("Failed to append {} history warning to console: {}", label, consoleEx.getMessage(), consoleEx);
        }
    }
}
