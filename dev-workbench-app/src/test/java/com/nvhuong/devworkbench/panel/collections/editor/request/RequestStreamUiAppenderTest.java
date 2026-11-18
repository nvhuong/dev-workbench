package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.stream.MessageType;
import org.testng.annotations.Test;

import java.time.format.DateTimeFormatter;

import static org.testng.Assert.assertTrue;

public class RequestStreamUiAppenderTest {

    @Test(description = "WebSocket Nội dung phản hồi không được để trống sau khi nhận được luồng tin nhắn")
    public void testFinalizeWebSocketResponseShouldPopulateBody() {
        RequestStreamUiAppender appender = new RequestStreamUiAppender(null, DateTimeFormatter.ofPattern("HH:mm:ss"));
        HttpResponse response = new HttpResponse();
        StringBuilder bodyBuilder = new StringBuilder();
        long queueStartMs = System.currentTimeMillis() - 10;

        appender.appendWebSocketRawEvent(bodyBuilder, MessageType.CONNECTED, "Switching Protocols");
        appender.appendWebSocketRawEvent(bodyBuilder, MessageType.RECEIVED, "{\"ok\":true}");
        appender.finalizeWebSocketResponse(response, bodyBuilder, queueStartMs);

        assertTrue(response.body.contains("CONNECTED") || response.body.contains(StreamMessageUiMetadata.display(MessageType.CONNECTED)));
        assertTrue(response.body.contains("RECEIVED") || response.body.contains(StreamMessageUiMetadata.display(MessageType.RECEIVED)));
        assertTrue(response.bodySize > 0);
        assertTrue(response.endTime > 0);
        assertTrue(response.costMs >= 0);
    }

    @Test(description = "WebSocket transcript Những dòng trống thừa ở đầu và cuối nên được loại bỏ để tránh history body Dòng đầu tiên bị đặt sai chỗ")
    public void testAppendWebSocketRawEventShouldTrimEdgeNewlines() {
        RequestStreamUiAppender appender = new RequestStreamUiAppender(null, DateTimeFormatter.ofPattern("HH:mm:ss"));
        StringBuilder bodyBuilder = new StringBuilder();

        appender.appendWebSocketRawEvent(bodyBuilder, MessageType.CONNECTED, "Switching Protocols\n");
        appender.appendWebSocketRawEvent(bodyBuilder, MessageType.RECEIVED, "line1\nline2");

        String transcript = bodyBuilder.toString();
        assertTrue(!transcript.contains("Protocols\n\n["));
        assertTrue(transcript.contains("line1\n  line2"));
    }

    @Test(description = "thiếu ResponsePanel dòng chảy thời gian UI Append nên được hạ cấp một cách an toàn xuống  no-op")
    public void testAppendUiMessageShouldIgnoreMissingResponsePanel() {
        RequestStreamUiAppender appender = new RequestStreamUiAppender(null, DateTimeFormatter.ofPattern("HH:mm:ss"));

        appender.appendWebSocketMessage(MessageType.RECEIVED, "hello");
        appender.appendSseMessage(MessageType.RECEIVED, null, "message", null, "hello", null);
        appender.appendSseRawEvent(null, null, "message", "hello");
        appender.finalizeSseResponse(new HttpResponse(), null, System.currentTimeMillis());
    }
}
