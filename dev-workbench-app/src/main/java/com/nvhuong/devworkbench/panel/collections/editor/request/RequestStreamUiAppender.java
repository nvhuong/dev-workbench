package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.panel.collections.editor.request.sub.ResponsePanel;
import com.nvhuong.devworkbench.script.model.TestResult;
import com.nvhuong.devworkbench.stream.MessageType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class RequestStreamUiAppender {
    /**
     * Upper bound for the raw SSE/WebSocket transcript kept as the response body (~4 MB of UTF-16). Streams can stay
     * open for hours; without a bound every event was retained in memory and copied again when the stream closed.
     */
    static final int MAX_RAW_TRANSCRIPT_CHARS = 2_000_000;
    static final String TRUNCATED_TRANSCRIPT_MARKER = "[... earlier stream events truncated ...]\n\n";

    private final ResponsePanel responsePanel;
    private final DateTimeFormatter timeFormatter;

    void appendWebSocketMessage(MessageType type, String text) {
        appendWebSocketMessage(type, text, null);
    }

    void appendWebSocketMessage(MessageType type, String text, List<TestResult> testResults) {
        if (responsePanel != null
                && responsePanel.getProtocol().isWebSocketProtocol()
                && responsePanel.getWebSocketResponsePanel() != null) {
            long timestampMs = System.currentTimeMillis();
            responsePanel.getWebSocketResponsePanel().addMessage(
                    type,
                    formatTimestamp(timestampMs),
                    timestampMs,
                    text,
                    testResults
            );
        }
    }

    void appendWebSocketRawEvent(StringBuilder webSocketBodyBuilder, MessageType type, String text) {
        if (webSocketBodyBuilder == null) {
            return;
        }
        String normalizedText = normalizeWebSocketTranscriptText(text);
        long timestampMs = System.currentTimeMillis();
        webSocketBodyBuilder
                .append('[')
                .append(formatTimestamp(timestampMs))
                .append("] ")
                .append(StreamMessageUiMetadata.display(type))
                .append(": ")
                .append(normalizedText)
                .append('\n');
        boundTranscript(webSocketBodyBuilder);
    }

    /** Keeps the newest part of a transcript; trims in batches so appends stay cheap once the bound is reached. */
    static void boundTranscript(StringBuilder transcript) {
        if (transcript.length() <= MAX_RAW_TRANSCRIPT_CHARS * 5 / 4) {
            return;
        }
        int keep = MAX_RAW_TRANSCRIPT_CHARS * 3 / 4;
        int cut = transcript.length() - keep;
        // Start the kept part at an SSE event boundary (blank line), else at a line boundary (WebSocket transcript),
        // so no half event remains.
        int eventBoundary = transcript.indexOf("\n\n", cut);
        if (eventBoundary >= 0 && eventBoundary - cut < 64 * 1024) {
            cut = eventBoundary + 2;
        } else {
            int lineBoundary = transcript.indexOf("\n", cut);
            cut = lineBoundary < 0 ? cut : lineBoundary + 1;
        }
        transcript.delete(0, cut);
        transcript.insert(0, TRUNCATED_TRANSCRIPT_MARKER);
    }

    void appendSseMessage(MessageType type, String eventId, String eventType, Long retryMs,
                          String text, List<TestResult> testResults) {
        if (responsePanel == null || responsePanel.getSseResponsePanel() == null) {
            return;
        }
        long timestampMs = System.currentTimeMillis();
        responsePanel.getSseResponsePanel().addMessage(
                type,
                formatTimestamp(timestampMs),
                timestampMs,
                eventId,
                eventType,
                retryMs,
                text,
                testResults
        );
    }

    void appendSseRawEvent(StringBuilder sseBodyBuilder, String id, String type, String data) {
        if (sseBodyBuilder == null || data == null) {
            return;
        }
        if (id != null && !id.isBlank()) {
            sseBodyBuilder.append("id: ").append(id).append('\n');
        }
        if (type != null && !type.isBlank()) {
            sseBodyBuilder.append("event: ").append(type).append('\n');
        }
        for (String line : data.split("\\R", -1)) {
            sseBodyBuilder.append("data: ").append(line).append('\n');
        }
        sseBodyBuilder.append('\n');
        boundTranscript(sseBodyBuilder);
    }

    void finalizeSseResponse(HttpResponse response, StringBuilder sseBodyBuilder, long queueStartMs) {
        if (response == null) {
            return;
        }
        response.isSse = true;
        response.body = sseBodyBuilder != null ? sseBodyBuilder.toString() : "";
        response.bodySize = response.body.getBytes(StandardCharsets.UTF_8).length;
        response.costMs = System.currentTimeMillis() - queueStartMs;
        response.endTime = System.currentTimeMillis();
    }

    void finalizeWebSocketResponse(HttpResponse response, StringBuilder webSocketBodyBuilder, long queueStartMs) {
        if (response == null) {
            return;
        }
        response.body = webSocketBodyBuilder != null ? webSocketBodyBuilder.toString() : "";
        response.bodySize = response.body.getBytes(StandardCharsets.UTF_8).length;
        response.costMs = System.currentTimeMillis() - queueStartMs;
        response.endTime = System.currentTimeMillis();
    }

    private String formatTimestamp(long timestampMs) {
        return Instant.ofEpochMilli(timestampMs)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .format(timeFormatter);
    }

    private String normalizeWebSocketTranscriptText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        normalized = trimEdgeNewlines(normalized);
        return normalized.replace("\n", "\n  ");
    }

    private String trimEdgeNewlines(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && text.charAt(start) == '\n') {
            start++;
        }
        while (end > start && text.charAt(end - 1) == '\n') {
            end--;
        }
        return text.substring(start, end);
    }
}
