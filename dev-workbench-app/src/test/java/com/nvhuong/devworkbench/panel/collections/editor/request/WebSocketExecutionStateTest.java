package com.nvhuong.devworkbench.panel.collections.editor.request;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WebSocketExecutionStateTest {

    @Test(description = "Bình thường close Viết sau đó nên được cho phép history")
    public void testShouldSaveHistoryAfterClose() {
        WebSocketExecutionState state = new WebSocketExecutionState("conn-1");

        assertFalse(state.shouldSaveHistory(false));

        assertTrue(state.markClosed());
        assertTrue(state.shouldSaveHistory(false));
    }

    @Test(description = "failure Viết sau đó nên được cho phép history")
    public void testShouldSaveHistoryAfterFailure() {
        WebSocketExecutionState state = new WebSocketExecutionState("conn-1");

        assertTrue(state.markFailed());
        assertTrue(state.shouldSaveHistory(false));
    }

    @Test(description = "Sau khi kết nối xong history Sự phán xét không nên dựa vào current connection id")
    public void testShouldSaveHistoryAfterReconnect() {
        WebSocketExecutionState state = new WebSocketExecutionState("conn-1");

        assertTrue(state.shouldHandleActiveCallback("conn-1", false));

        assertTrue(state.markClosed());

        assertFalse(state.shouldHandleActiveCallback("conn-2", false));
        assertTrue(state.shouldSaveHistory(false));
    }

    @Test(description = "disposed Bảng điều khiển không nên được ghi vào history")
    public void testShouldNotSaveHistoryWhenDisposed() {
        WebSocketExecutionState state = new WebSocketExecutionState("conn-1");

        assertTrue(state.markClosed());
        assertFalse(state.shouldSaveHistory(true));
    }
}
