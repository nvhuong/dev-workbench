package com.nvhuong.devworkbench.panel.performance.tree;

import com.nvhuong.devworkbench.performance.core.controller.LoopData;
import com.nvhuong.devworkbench.performance.core.model.SsePerformanceData;
import com.nvhuong.devworkbench.performance.core.model.WebSocketPerformanceData;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class PerformanceTreeNodeTitleFormatterTest {

    @Test(description = "WebSocket Tiêu đề lặp lại sẽ hiển thị số vòng và khoảng thời gian")
    public void shouldFormatWebSocketRepeatSendTitle() {
        WebSocketPerformanceData data = new WebSocketPerformanceData();
        data.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT;
        data.sendCount = 3;
        data.sendIntervalMs = 1000;

        String title = PerformanceTreeNodeTitleFormatter.webSocketSendTitle(data);

        assertTrue(title.contains("mỗi vòng 3 lần") || title.contains("Per loop 3x"), title);
        assertTrue(title.contains("1s"), title);
    }

    @Test(description = "WebSocket Tiêu đề nút đọc nên được sử dụng Read thuật ngữ")
    public void shouldFormatWebSocketReadTitle() {
        WebSocketPerformanceData data = new WebSocketPerformanceData();
        data.completionMode = WebSocketPerformanceData.CompletionMode.UNTIL_MATCH;
        data.firstMessageTimeoutMs = 10000;
        data.messageFilter = "ack";

        String title = PerformanceTreeNodeTitleFormatter.webSocketReadTitle(data);

        assertTrue(title.startsWith("WS Read"), title);
        assertTrue(title.contains("Until contains") || title.contains("Đọc có chứa văn bản"), title);
        assertTrue(title.contains("10s"), title);
        assertTrue(title.contains("contains=ack"), title);
    }

    @Test(description = "WebSocket Tiêu đề đã đọc theo số lượng tin nhắn sẽ hiển thị thời gian chờ đọc thống nhất")
    public void shouldFormatWebSocketMessageCountTitleWithReadTimeout() {
        WebSocketPerformanceData data = new WebSocketPerformanceData();
        data.completionMode = WebSocketPerformanceData.CompletionMode.MESSAGE_COUNT;
        data.targetMessageCount = 3;
        data.firstMessageTimeoutMs = 2000;
        data.holdConnectionMs = 15000;

        String title = PerformanceTreeNodeTitleFormatter.webSocketReadTitle(data);

        assertTrue(title.contains("3"), title);
        assertTrue(title.contains("2s"), title);
        assertTrue(!title.contains("15s"), title);
    }

    @Test(description = "SSE Tiêu đề tin nhắn phù hợp sẽ hiển thị các bộ lọc tin nhắn và sự kiện")
    public void shouldFormatSseMatchedMessageTitle() {
        SsePerformanceData data = new SsePerformanceData();
        data.completionMode = SsePerformanceData.CompletionMode.UNTIL_MATCH;
        data.firstMessageTimeoutMs = 10000;
        data.messageFilter = "ready";
        data.eventNameFilter = "open";

        String title = PerformanceTreeNodeTitleFormatter.sseReadTitle(data);

        assertTrue(title.startsWith("SSE Read"), title);
        assertTrue(title.contains("10s"), title);
        assertTrue(title.contains("contains=ready"), title);
        assertTrue(title.contains("event=open"), title);
    }

    @Test(description = "SSE Tiêu đề theo số lượng tin nhắn sẽ hiển thị thời gian chờ đọc thống nhất")
    public void shouldFormatSseMessageCountTitleWithReceiveTimeout() {
        SsePerformanceData data = new SsePerformanceData();
        data.completionMode = SsePerformanceData.CompletionMode.MESSAGE_COUNT;
        data.targetMessageCount = 3;
        data.firstMessageTimeoutMs = 2000;
        data.holdConnectionMs = 15000;

        String title = PerformanceTreeNodeTitleFormatter.sseReadTitle(data);

        assertTrue(title.contains("3"), title);
        assertTrue(title.contains("2s"), title);
        assertTrue(!title.contains("15s"), title);
    }

    @Test(description = "SSE Tiêu đề Đợi luồng đóng sẽ hiển thị thời gian chờ theo chế độ và đóng")
    public void shouldFormatSseStreamClosedTitle() {
        SsePerformanceData data = new SsePerformanceData();
        data.completionMode = SsePerformanceData.CompletionMode.STREAM_CLOSED;
        data.holdConnectionMs = 15000;

        String title = PerformanceTreeNodeTitleFormatter.sseReadTitle(data);

        assertTrue(title.contains("15s"), title);
        assertTrue(title.contains("Luồng đã đóng")
                || title.contains("Stream Closed")
                || title.contains("stream closes"), title);
    }

    @Test(description = "Loop Tiêu đề nên được chuẩn hóa theo số vòng lặp")
    public void shouldFormatLoopTitleWithNormalizedIterations() {
        LoopData data = new LoopData();
        data.iterations = 0;

        String title = PerformanceTreeNodeTitleFormatter.loopTitle(data);

        assertTrue(title.contains("1x"), title);
    }
}
