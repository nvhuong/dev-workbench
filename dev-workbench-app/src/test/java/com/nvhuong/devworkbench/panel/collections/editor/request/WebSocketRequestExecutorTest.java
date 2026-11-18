package com.nvhuong.devworkbench.panel.collections.editor.request;

import com.nvhuong.devworkbench.http.runtime.model.HttpCaptureProfile;
import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.http.runtime.transport.HttpExchangeOptions;
import com.nvhuong.devworkbench.http.runtime.transport.HttpTransport;
import com.nvhuong.devworkbench.http.runtime.transport.RealtimeConnectionHandle;
import com.nvhuong.devworkbench.http.runtime.transport.RealtimeConnectionOptions;
import com.nvhuong.devworkbench.http.runtime.transport.RealtimeWebSocketConnection;
import com.nvhuong.devworkbench.panel.collections.editor.request.sub.ResponsePanel;
import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.script.model.TestResult;
import com.nvhuong.devworkbench.service.js.ScriptExecutionPipeline;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSourceListener;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class WebSocketRequestExecutorTest {

    @Test(description = "WebSocket Quá trình gửi sẽ mở ra ảnh chụp nhanh yêu cầu bắt tay và nhật ký mạng, v.v HTTP/SSE Hãy nhất quán")
    public void shouldEnableNetworkCaptureForWebSocketRequests() {
        PreparedRequest request = new PreparedRequest();

        new WebSocketRequestExecutor(
                null,
                null,
                null,
                null,
                new RequestExecutionState()
        ).createWorker(request, null);

        assertSame(request.captureProfile, HttpCaptureProfile.COLLECTION_DIAGNOSTIC);
        assertTrue(request.collectBasicInfo);
        assertTrue(request.collectMetricsInfo);
        assertTrue(request.collectEventInfo);
        assertTrue(request.enableNetworkLog);
    }

    @Test(description = "Nó sẽ kết thúc khi đầu từ xa bắt đầu tắt máy WebSocket phiên và trả nút trên cùng về Kết nối")
    public void shouldResetConnectButtonWhenRemotePeerStartsClosing() throws Exception {
        RequestExecutionState requestState = new RequestExecutionState();
        ResponsePanel responsePanel = new ResponsePanel(RequestItemProtocolEnum.WEBSOCKET, false);
        FakeWebSocketConnectionUi connectionUi = new FakeWebSocketConnectionUi();
        FakeWebSocketResponseHandler responseHandler = new FakeWebSocketResponseHandler();
        FakeHttpTransport transport = new FakeHttpTransport();
        WebSocketRequestExecutor executor = new WebSocketRequestExecutor(
                responsePanel,
                connectionUi,
                new RequestStreamUiAppender(responsePanel, DateTimeFormatter.ofPattern("HH:mm:ss")),
                responseHandler,
                requestState,
                transport
        );
        PreparedRequest request = new PreparedRequest();
        request.url = "ws://example.test/socket";
        request.method = "GET";
        SwingWorker<Void, Void> worker = executor.createWorker(request, null);

        requestState.startWorker(worker);
        worker.execute();
        assertTrue(transport.awaitWebSocketOpen());
        assertTrue(awaitCondition(() -> requestState.currentWebSocket() != null));
        assertNotNull(requestState.currentWebSocketConnectionId());

        connectionUi.switchSendButtonToClose();
        connectionUi.setWebSocketConnected(true);
        transport.listener.onClosing(null, 1013, "session replacement failed");
        worker.get(5, TimeUnit.SECONDS);
        flushEdt();

        assertNull(requestState.currentWebSocket());
        assertNull(requestState.currentWebSocketConnectionId());
        assertTrue(awaitCondition(() -> requestState.currentWorker() == null));
        assertTrue(connectionUi.responseUpdated);
        assertTrue(connectionUi.resetSendButtonCalled);
        assertFalse(connectionUi.closeButtonVisible);
        assertFalse(connectionUi.webSocketConnected);
        assertTrue(responseHandler.historySaved);
    }

    private void flushEdt() throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private boolean awaitCondition(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(10);
        }
        return condition.getAsBoolean();
    }

    private static final class FakeWebSocketResponseHandler implements WebSocketResponseHandler {
        private boolean historySaved;

        @Override
        public List<TestResult> handleStreamMessage(ScriptExecutionPipeline pipeline, String message) {
            return List.of();
        }

        @Override
        public void saveHistory(PreparedRequest request, HttpResponse response, String label) {
            historySaved = true;
        }
    }

    private static final class FakeWebSocketConnectionUi implements WebSocketConnectionUi {
        private boolean responseUpdated;
        private boolean resetSendButtonCalled;
        private boolean closeButtonVisible;
        private boolean webSocketConnected;

        @Override
        public void updateUIForResponse(HttpResponse resp) {
            responseUpdated = true;
        }

        @Override
        public void resetSendButton() {
            resetSendButtonCalled = true;
            closeButtonVisible = false;
        }

        @Override
        public void switchSendButtonToClose() {
            closeButtonVisible = true;
        }

        @Override
        public void setWebSocketConnected(boolean connected) {
            webSocketConnected = connected;
        }

        @Override
        public void activateWebSocketBodyTab() {
        }
    }

    private static final class FakeHttpTransport implements HttpTransport {
        private final CountDownLatch webSocketOpen = new CountDownLatch(1);
        private volatile WebSocketListener listener;

        @Override
        public HttpResponse execute(PreparedRequest request, HttpExchangeOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RealtimeConnectionHandle openSse(PreparedRequest request,
                                                EventSourceListener listener,
                                                RealtimeConnectionOptions options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RealtimeWebSocketConnection openWebSocket(PreparedRequest request,
                                                        WebSocketListener listener,
                                                        RealtimeConnectionOptions options) {
            this.listener = listener;
            webSocketOpen.countDown();
            return new FakeWebSocketConnection();
        }

        private boolean awaitWebSocketOpen() throws InterruptedException {
            return webSocketOpen.await(5, TimeUnit.SECONDS);
        }
    }

    private static final class FakeWebSocketConnection implements RealtimeWebSocketConnection {
        @Override
        public boolean send(String text) {
            return true;
        }

        @Override
        public long queueSize() {
            return 0;
        }

        @Override
        public boolean close(int code, String reason) {
            return true;
        }

        @Override
        public void cancel() {
        }

        @Override
        public Object metricKey() {
            return this;
        }
    }
}
