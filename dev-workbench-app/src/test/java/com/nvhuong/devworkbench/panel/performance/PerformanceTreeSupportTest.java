package com.nvhuong.devworkbench.panel.performance;

import com.nvhuong.devworkbench.request.model.RequestItemProtocolEnum;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.performance.core.controller.LoopData;
import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.performance.core.model.PerformanceProtocol;
import com.nvhuong.devworkbench.performance.core.model.SsePerformanceData;
import com.nvhuong.devworkbench.performance.core.model.WebSocketPerformanceData;
import com.nvhuong.devworkbench.performance.core.request.PerformanceRequestProtocol;
import com.nvhuong.devworkbench.panel.performance.tree.PerformanceSwingTreePlanAdapter;
import com.nvhuong.devworkbench.performance.plan.PerformancePlanNode;
import org.testng.annotations.Test;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class PerformanceTreeSupportTest {

    @Test(description = "SSE Các yêu cầu sẽ tự động hoàn thành các nút cố định và hợp nhất các xác nhận vào chúng Read dưới nút")
    public void shouldCreateSseStructureAndMoveAssertionsUnderReadNode() {
        TestContext context = newSseTestContext();
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        context.treeModel.insertNodeInto(assertionNode, context.requestNode, context.requestNode.getChildCount());

        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.SSE_CONNECT, NodeType.SSE_READ));
        DefaultMutableTreeNode readNode = findChild(context.requestNode, NodeType.SSE_READ);
        assertNotNull(readNode);
        assertEquals(childTypesOf(readNode), List.of(NodeType.ASSERTION));
        assertSame(readNode.getChildAt(0), assertionNode);
        assertNotNull(((PerformanceTreeNode) findChild(context.requestNode, NodeType.SSE_CONNECT).getUserObject()).ssePerformanceData);
        assertNotNull(((PerformanceTreeNode) readNode.getUserObject()).ssePerformanceData);
    }

    @Test(description = "SSE Các nút giai đoạn nên sử dụng cấu hình mặc định độc lập và không đọc cấu hình cũ từ yêu cầu chính")
    public void shouldInitializeSseStageNodesWithIndependentDefaultData() {
        TestContext context = newSseTestContext();
        context.requestData.ssePerformanceData = new SsePerformanceData();
        context.requestData.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.STREAM_CLOSED;
        context.requestData.ssePerformanceData.holdConnectionMs = 30000;

        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode connectNode = findChild(context.requestNode, NodeType.SSE_CONNECT);
        DefaultMutableTreeNode readNode = findChild(context.requestNode, NodeType.SSE_READ);
        PerformanceTreeNode connectData = (PerformanceTreeNode) connectNode.getUserObject();
        PerformanceTreeNode readData = (PerformanceTreeNode) readNode.getUserObject();
        assertNotNull(connectData.ssePerformanceData);
        assertNotNull(readData.ssePerformanceData);
        assertNotSame(connectData.ssePerformanceData, context.requestData.ssePerformanceData);
        assertNotSame(readData.ssePerformanceData, context.requestData.ssePerformanceData);
        assertEquals(readData.ssePerformanceData.completionMode, SsePerformanceData.CompletionMode.SINGLE_MESSAGE);
        assertTrue(readData.name.contains("10s"), readData.name);
        assertFalse(readData.name.contains("30s"), readData.name);
    }

    @Test(description = "SSE Read Tiêu đề nên theo sau Read cấu hình nút thay vì cấu hình cũ theo yêu cầu gốc")
    public void shouldRefreshSseReadTitleFromReadStageData() {
        TestContext context = newSseTestContext();
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode readNode = findChild(context.requestNode, NodeType.SSE_READ);
        PerformanceTreeNode readData = (PerformanceTreeNode) readNode.getUserObject();
        readData.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.STREAM_CLOSED;
        readData.ssePerformanceData.holdConnectionMs = 30000;

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertTrue(readData.name.contains("30s"), readData.name);
        assertFalse(readData.name.contains("10s"), readData.name);
    }

    @Test(description = "SSE Mẫu tin nhắn phù hợp phải có Receive Tiêu đề nút hiển thị các điều kiện lọc tin nhắn")
    public void shouldShowSseMatchedMessageFilterInReadNodeTitle() {
        TestContext context = newSseTestContext();
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode readNode = findChild(context.requestNode, NodeType.SSE_READ);
        assertNotNull(readNode);
        PerformanceTreeNode readData = (PerformanceTreeNode) readNode.getUserObject();
        readData.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.UNTIL_MATCH;
        readData.ssePerformanceData.firstMessageTimeoutMs = 10000;
        readData.ssePerformanceData.messageFilter = "done";

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertTrue(readData.name.contains("10s"), readData.name);
        assertTrue(readData.name.contains("contains=done"), readData.name);
    }

    @Test(description = "SSE Không nên bật chế độ thời lượng cố định Receive Tiêu đề nút hiển thị các điều kiện lọc sự kiện")
    public void shouldHideSseEventFilterInFixedDurationReadNodeTitle() {
        TestContext context = newSseTestContext();
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode readNode = findChild(context.requestNode, NodeType.SSE_READ);
        assertNotNull(readNode);
        PerformanceTreeNode readData = (PerformanceTreeNode) readNode.getUserObject();
        readData.ssePerformanceData.completionMode = SsePerformanceData.CompletionMode.FIXED_DURATION;
        readData.ssePerformanceData.holdConnectionMs = 30000;
        readData.ssePerformanceData.eventNameFilter = "done";

        context.treeSupport.syncRequestStructure(context.requestNode, context.requestData);

        assertTrue(readData.name.contains("30s"), readData.name);
        assertFalse(readData.name.contains("event=done"), readData.name);
    }

    @Test(description = "WebSocket Không nên sử dụng thời gian chờ cố định Read Tiêu đề nút hiển thị các điều kiện lọc tin nhắn")
    public void shouldHideWebSocketMessageFilterInFixedDurationReadNodeTitle() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode readNode = newNode("Read", NodeType.WS_READ);
        PerformanceTreeNode readData = (PerformanceTreeNode) readNode.getUserObject();
        readData.webSocketPerformanceData = new com.nvhuong.devworkbench.performance.core.model.WebSocketPerformanceData();
        readData.webSocketPerformanceData.completionMode = com.nvhuong.devworkbench.performance.core.model.WebSocketPerformanceData.CompletionMode.FIXED_DURATION;
        readData.webSocketPerformanceData.holdConnectionMs = 30000;
        readData.webSocketPerformanceData.messageFilter = "done";
        context.treeModel.insertNodeInto(readNode, context.requestNode, context.requestNode.getChildCount());

        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        assertTrue(readData.name.contains("30s"), readData.name);
        assertFalse(readData.name.contains("contains=done"), readData.name);
    }

    @Test(description = "SSE Yêu cầu chuyển lại HTTP nên được loại bỏ sau này SSE Ghim nút và khôi phục xác nhận vào nút yêu cầu")
    public void shouldRemoveSseNodesAndRestoreAssertionsWhenSwitchingBackToHttp() {
        TestContext context = newTestContext(RequestItemProtocolEnum.HTTP);
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        context.treeModel.insertNodeInto(assertionNode, context.requestNode, context.requestNode.getChildCount());

        context.treeSupport.setSsePerformanceMode(context.requestNode, true);
        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.SSE_CONNECT, NodeType.SSE_READ));
        assertEquals(context.requestData.requestSnapshot.getProtocol(), PerformanceRequestProtocol.HTTP);
        assertSame(findChild(context.requestNode, NodeType.SSE_READ).getChildAt(0), assertionNode);
        PerformancePlanNode persistedNode = PerformanceSwingTreePlanAdapter.toDocumentNode(context.requestNode);
        assertEquals(persistedNode.getRequestSnapshot().getProtocol(), PerformanceRequestProtocol.HTTP);
        DefaultMutableTreeNode restoredNode = PerformanceSwingTreePlanAdapter.toTreeNode(persistedNode);
        assertTrue(((PerformanceTreeNode) restoredNode.getUserObject()).ssePerformanceEnabled);

        context.treeSupport.setSsePerformanceMode(context.requestNode, false);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.ASSERTION));
        assertEquals(context.requestData.requestSnapshot.getProtocol(), PerformanceRequestProtocol.HTTP);
        assertSame(context.requestNode.getChildAt(0), assertionNode);
        assertNull(findChild(context.requestNode, NodeType.SSE_CONNECT));
        assertNull(findChild(context.requestNode, NodeType.SSE_READ));
    }

    @Test(description = "WebSocket Yêu cầu sẽ tạo một nút kết nối và khởi tạo dữ liệu stress test mặc định")
    public void shouldCreateWebSocketConnectNodeAndDefaults() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);

        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.WS_CONNECT));
        assertNotNull(context.requestData.webSocketPerformanceData);
        assertNotNull(findChild(context.requestNode, NodeType.WS_CONNECT));
    }

    @Test(description = "WebSocket Nó không nên được sử dụng khi đồng bộ hóa cấu trúc Read Các xác nhận dưới nút được chuyển trở lại nút yêu cầu")
    public void shouldKeepWebSocketReadAssertionsWhenSyncingStructure() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode readNode = newNode("Read", NodeType.WS_READ);
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        readNode.add(assertionNode);
        context.treeModel.insertNodeInto(readNode, context.requestNode, context.requestNode.getChildCount());

        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        assertSame(assertionNode.getParent(), readNode);
        assertEquals(childTypesOf(readNode), List.of(NodeType.ASSERTION));
    }

    @Test(description = "WebSocket Yêu cầu chuyển lại HTTP Sau đó, nút bước sẽ được dọn sạch và đặt Read Khẳng định thấp hơn được khôi phục")
    public void shouldRemoveWebSocketNodesAndRestoreAssertionsWhenSwitchingBackToHttp() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode readNode = newNode("Read", NodeType.WS_READ);
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        readNode.add(assertionNode);
        context.treeModel.insertNodeInto(newNode("Send", NodeType.WS_SEND), context.requestNode, context.requestNode.getChildCount());
        context.treeModel.insertNodeInto(readNode, context.requestNode, context.requestNode.getChildCount());
        context.treeModel.insertNodeInto(newNode("Close", NodeType.WS_CLOSE), context.requestNode, context.requestNode.getChildCount());

        context.requestData.httpRequestItem.setProtocol(RequestItemProtocolEnum.HTTP);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.ASSERTION));
        assertSame(context.requestNode.getChildAt(0), assertionNode);
        assertNull(findChild(context.requestNode, NodeType.WS_CONNECT));
        assertNull(findChild(context.requestNode, NodeType.WS_SEND));
        assertNull(findChild(context.requestNode, NodeType.WS_READ));
        assertNull(findChild(context.requestNode, NodeType.WS_CLOSE));
    }

    @Test(description = "WebSocket Cắt lại HTTP nên được làm sạch theo thời gian Loop bước vào và khôi phục lồng nhau Read khẳng định")
    public void shouldRemoveWebSocketLoopAndRestoreNestedReadAssertionsWhenSwitchingBackToHttp() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode loopNode = newLoopNode(2);
        DefaultMutableTreeNode readNode = newNode("Read", NodeType.WS_READ);
        DefaultMutableTreeNode assertionNode = newNode("Assertion", NodeType.ASSERTION);
        readNode.add(assertionNode);
        loopNode.add(newNode("Send", NodeType.WS_SEND));
        loopNode.add(readNode);
        context.treeModel.insertNodeInto(loopNode, context.requestNode, context.requestNode.getChildCount());

        context.requestData.httpRequestItem.setProtocol(RequestItemProtocolEnum.HTTP);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.ASSERTION));
        assertSame(context.requestNode.getChildAt(0), assertionNode);
        assertNull(findChild(context.requestNode, NodeType.LOOP));
    }

    @Test(description = "WebSocket Loop Nên được sử dụng làm nơi chứa bước để tiếp tục thêm vào nó Send/Read/Timer")
    public void shouldResolveWebSocketLoopAsStepParent() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode loopNode = newLoopNode(2);
        DefaultMutableTreeNode sendNode = newNode("Send", NodeType.WS_SEND);
        loopNode.add(sendNode);
        context.treeModel.insertNodeInto(loopNode, context.requestNode, context.requestNode.getChildCount());

        assertSame(context.treeSupport.resolveWebSocketStepParent(loopNode), loopNode);
        assertSame(context.treeSupport.resolveWebSocketStepParent(sendNode), loopNode);
        assertFalse(context.treeSupport.isRequestContainerLoop(loopNode));
    }

    @Test(description = "WebSocket Send Số lần lặp lại cần được ghi nhận là số hiệp trong mỗi hiệp, tránh và các lớp bên ngoài Loop Tổng số lần bị nhầm lẫn")
    public void shouldShowWebSocketSendRepeatCountAsPerLoopCount() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        DefaultMutableTreeNode sendNode = newNode("Send", NodeType.WS_SEND);
        PerformanceTreeNode sendData = (PerformanceTreeNode) sendNode.getUserObject();
        sendData.webSocketPerformanceData = new WebSocketPerformanceData();
        sendData.webSocketPerformanceData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT;
        sendData.webSocketPerformanceData.sendCount = 3;
        context.treeModel.insertNodeInto(sendNode, context.requestNode, context.requestNode.getChildCount());

        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        assertTrue(sendData.name.contains("mỗi vòng 3 lần") || sendData.name.contains("Per loop 3x"), sendData.name);
        assertFalse(sendData.name.contains(" | 3x | "), sendData.name);
    }

    @Test(description = "dưới nhóm chủ đề Loop Nên được sử dụng như một thùng chứa yêu cầu HTTP/SSE/WS Yêu cầu sử dụng lại")
    public void shouldIdentifyLoopUnderThreadGroupAsRequestContainer() {
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode groupNode = newNode("Group", NodeType.THREAD_GROUP);
        DefaultMutableTreeNode loopNode = newLoopNode(3);
        root.add(groupNode);
        groupNode.add(loopNode);
        PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(new DefaultTreeModel(root));

        assertTrue(treeSupport.isRequestContainerLoop(loopNode));
    }

    @Test(description = "Nút yêu cầu sao chép sẽ sao chép toàn bộ cây con và tạo ra một yêu cầu mới id")
    public void shouldCopyRequestSubtreeWithIndependentRequestId() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.requestData.httpRequestItem.setId("original-request");
        context.requestData.httpRequestItem.setUrl("ws://localhost:8080/ws");
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode sendNode = newNode("Send", NodeType.WS_SEND);
        PerformanceTreeNode sendData = (PerformanceTreeNode) sendNode.getUserObject();
        sendData.webSocketPerformanceData = new WebSocketPerformanceData();
        sendData.webSocketPerformanceData.customSendBody = "hello";
        context.treeModel.insertNodeInto(sendNode, context.requestNode, context.requestNode.getChildCount());

        List<DefaultMutableTreeNode> copied = context.treeSupport.copyNodes(paths(context.requestNode));

        assertEquals(copied.size(), 1);
        PerformanceTreeNode copiedRequestData = (PerformanceTreeNode) copied.get(0).getUserObject();
        assertNotSame(copiedRequestData.httpRequestItem, context.requestData.httpRequestItem);
        assertNotEquals(copiedRequestData.httpRequestItem.getId(), context.requestData.httpRequestItem.getId());
        assertEquals(copiedRequestData.httpRequestItem.getUrl(), "ws://localhost:8080/ws");
        assertEquals(childTypesOf(copied.get(0)), List.of(NodeType.WS_CONNECT, NodeType.WS_SEND));
        PerformanceTreeNode copiedSendData = (PerformanceTreeNode) ((DefaultMutableTreeNode) copied.get(0).getChildAt(1)).getUserObject();
        assertNotSame(copiedSendData.webSocketPerformanceData, sendData.webSocketPerformanceData);
        assertEquals(copiedSendData.webSocketPerformanceData.customSendBody, "hello");
    }

    @Test(description = "Khi sao chép nhiều lựa chọn, các nút con trùng lặp đã có trong cây con của nút cha phải được lọc")
    public void shouldCopyOnlyTopLevelNodesFromMultiSelection() {
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode groupNode = newNode("Group", NodeType.THREAD_GROUP);
        DefaultMutableTreeNode loopNode = newLoopNode(3);
        DefaultMutableTreeNode nestedRequest = newRequestNode("nested", "Nested");
        DefaultMutableTreeNode siblingRequest = newRequestNode("sibling", "Sibling");
        root.add(groupNode);
        groupNode.add(loopNode);
        loopNode.add(nestedRequest);
        groupNode.add(siblingRequest);
        PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(new DefaultTreeModel(root));

        List<DefaultMutableTreeNode> copied = treeSupport.copyNodes(paths(loopNode, nestedRequest, siblingRequest));

        assertEquals(copied.size(), 2);
        assertEquals(nodeType(copied.get(0)), NodeType.LOOP);
        assertEquals(nodeType(copied.get(1)), NodeType.REQUEST);
        assertEquals(childTypesOf(copied.get(0)), List.of(NodeType.REQUEST));
    }

    @Test(description = "Việc dán một yêu cầu vào một yêu cầu khác phải được chèn dưới dạng anh chị em sau mục tiêu")
    public void shouldPasteRequestAfterSelectedSiblingWhenTargetCannotContainRequest() {
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode groupNode = newNode("Group", NodeType.THREAD_GROUP);
        DefaultMutableTreeNode firstRequest = newRequestNode("first", "First");
        DefaultMutableTreeNode secondRequest = newRequestNode("second", "Second");
        root.add(groupNode);
        groupNode.add(firstRequest);
        groupNode.add(secondRequest);
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(treeModel);
        List<DefaultMutableTreeNode> copied = treeSupport.copyNodes(paths(firstRequest));

        List<DefaultMutableTreeNode> pasted = treeSupport.pasteNodes(new JTree(treeModel), secondRequest, copied);

        assertEquals(pasted.size(), 1);
        assertSame(groupNode.getChildAt(2), pasted.get(0));
        PerformanceTreeNode pastedData = (PerformanceTreeNode) pasted.get(0).getUserObject();
        assertNotEquals(pastedData.httpRequestItem.getId(), "first");
        assertNotEquals(pastedData.httpRequestItem.getId(), ((PerformanceTreeNode) copied.get(0).getUserObject()).httpRequestItem.getId());
    }

    @Test(description = "Dán WebSocket Các bước tới WebSocket Steps should be inserted on request and the step configuration preserved")
    public void shouldPasteWebSocketStepIntoWebSocketRequest() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode sendNode = newNode("Send", NodeType.WS_SEND);
        PerformanceTreeNode sendData = (PerformanceTreeNode) sendNode.getUserObject();
        sendData.webSocketPerformanceData = new WebSocketPerformanceData();
        sendData.webSocketPerformanceData.customSendBody = "payload";
        context.treeModel.insertNodeInto(sendNode, context.requestNode, context.requestNode.getChildCount());
        List<DefaultMutableTreeNode> copied = context.treeSupport.copyNodes(paths(sendNode));

        List<DefaultMutableTreeNode> pasted = context.treeSupport.pasteNodes(new JTree(context.treeModel), context.requestNode, copied);

        assertEquals(pasted.size(), 1);
        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.WS_CONNECT, NodeType.WS_SEND, NodeType.WS_SEND));
        PerformanceTreeNode pastedData = (PerformanceTreeNode) pasted.get(0).getUserObject();
        assertNotSame(pastedData.webSocketPerformanceData, sendData.webSocketPerformanceData);
        assertEquals(pastedData.webSocketPerformanceData.customSendBody, "payload");
    }

    @Test(description = "SSE Các nút giai đoạn cố định cũng phải hỗ trợ sao chép riêng biệt")
    public void shouldCopySseStageNodesIndividually() {
        TestContext context = newSseTestContext();
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode connectNode = findChild(context.requestNode, NodeType.SSE_CONNECT);
        DefaultMutableTreeNode readNode = findChild(context.requestNode, NodeType.SSE_READ);

        assertTrue(context.treeSupport.hasCopyableNodes(paths(connectNode)));
        assertTrue(context.treeSupport.hasCopyableNodes(paths(readNode)));
        assertEquals(context.treeSupport.copyNodes(paths(connectNode, readNode)).size(), 2);
    }

    @Test(description = "Dán SSE Nút giai đoạn đến SSE The stage node should be inserted when the request is made")
    public void shouldPasteSseStageNodeIntoSseRequest() {
        TestContext context = newSseTestContext();
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode readNode = findChild(context.requestNode, NodeType.SSE_READ);
        PerformanceTreeNode readData = (PerformanceTreeNode) readNode.getUserObject();
        readData.ssePerformanceData = new SsePerformanceData();
        readData.ssePerformanceData.messageFilter = "ready";
        List<DefaultMutableTreeNode> copied = context.treeSupport.copyNodes(paths(readNode));

        List<DefaultMutableTreeNode> pasted = context.treeSupport.pasteNodes(new JTree(context.treeModel), context.requestNode, copied);

        assertEquals(pasted.size(), 1);
        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.SSE_CONNECT, NodeType.SSE_READ, NodeType.SSE_READ));
        PerformanceTreeNode pastedData = (PerformanceTreeNode) pasted.get(0).getUserObject();
        assertNotSame(pastedData.ssePerformanceData, readData.ssePerformanceData);
        assertEquals(pastedData.ssePerformanceData.messageFilter, "ready");
    }

    @Test(description = "WebSocket Connect Các nút nên hỗ trợ sao chép cá nhân")
    public void shouldCopyWebSocketConnectNodeIndividually() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode connectNode = findChild(context.requestNode, NodeType.WS_CONNECT);

        assertTrue(context.treeSupport.hasCopyableNodes(paths(connectNode)));
        assertEquals(context.treeSupport.copyNodes(paths(connectNode)).size(), 1);
    }

    @Test(description = "Dán WebSocket Connect Đến WebSocket The connection node should be inserted when the request is made")
    public void shouldPasteWebSocketConnectIntoWebSocketRequest() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode connectNode = findChild(context.requestNode, NodeType.WS_CONNECT);
        PerformanceTreeNode connectData = (PerformanceTreeNode) connectNode.getUserObject();
        connectData.webSocketPerformanceData = new WebSocketPerformanceData();
        connectData.webSocketPerformanceData.connectTimeoutMs = 15000;
        List<DefaultMutableTreeNode> copied = context.treeSupport.copyNodes(paths(connectNode));

        List<DefaultMutableTreeNode> pasted = context.treeSupport.pasteNodes(new JTree(context.treeModel), context.requestNode, copied);

        assertEquals(pasted.size(), 1);
        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.WS_CONNECT, NodeType.WS_CONNECT));
        PerformanceTreeNode pastedData = (PerformanceTreeNode) pasted.get(0).getUserObject();
        assertNotSame(pastedData.webSocketPerformanceData, connectData.webSocketPerformanceData);
        assertEquals(pastedData.webSocketPerformanceData.connectTimeoutMs, 15000);
    }

    @Test(description = "WebSocket Connect Bám sát hiện trường Loop Khi đi lên, nó sẽ quay trở lại nút giai đoạn cấp yêu cầu")
    public void shouldPasteWebSocketConnectAtRequestLevelWhenTargetingScenarioLoop() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);
        DefaultMutableTreeNode connectNode = findChild(context.requestNode, NodeType.WS_CONNECT);
        DefaultMutableTreeNode loopNode = newLoopNode(2);
        context.treeModel.insertNodeInto(loopNode, context.requestNode, context.requestNode.getChildCount());
        List<DefaultMutableTreeNode> copied = context.treeSupport.copyNodes(paths(connectNode));

        List<DefaultMutableTreeNode> pasted = context.treeSupport.pasteNodes(new JTree(context.treeModel), loopNode, copied);

        assertEquals(pasted.size(), 1);
        assertEquals(childTypesOf(loopNode), List.of());
        assertSame(pasted.get(0).getParent(), context.requestNode);
        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.WS_CONNECT, NodeType.WS_CONNECT, NodeType.LOOP));
    }

    @Test(description = "Từ WebSocket Loop Được thêm vào một cách rõ ràng bên trong Connect nên được bổ sung vào nút gốc yêu cầu")
    public void shouldAddWebSocketConnectAtRequestLevelWhenSelectionIsScenarioLoop() {
        TestContext context = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        DefaultMutableTreeNode loopNode = newLoopNode(2);
        context.treeModel.insertNodeInto(loopNode, context.requestNode, context.requestNode.getChildCount());
        JTree tree = new JTree(context.treeModel);
        tree.setSelectionPath(new TreePath(loopNode.getPath()));

        context.treeSupport.addWebSocketStepNode(tree, NodeType.WS_CONNECT, () -> {
        });

        assertEquals(childTypesOf(loopNode), List.of());
        assertEquals(childTypesOf(context.requestNode), List.of(NodeType.WS_CONNECT, NodeType.LOOP));
    }

    @Test(description = "SSE Connect、SSE Read、WebSocket Connect Các nút giai đoạn nên hỗ trợ xóa")
    public void shouldDeleteProtocolStageNodes() {
        TestContext sseContext = newSseTestContext();
        sseContext.treeSupport.ensureRequestStructure(sseContext.requestNode, sseContext.requestData);
        DefaultMutableTreeNode sseConnectNode = findChild(sseContext.requestNode, NodeType.SSE_CONNECT);
        DefaultMutableTreeNode sseReadNode = findChild(sseContext.requestNode, NodeType.SSE_READ);

        assertTrue(sseContext.treeSupport.hasDeletableNodes(paths(sseConnectNode, sseReadNode)));
        List<DefaultMutableTreeNode> deletedSseNodes = sseContext.treeSupport.deleteNodes(paths(sseConnectNode, sseReadNode));

        assertEquals(deletedSseNodes.size(), 2);
        assertEquals(childTypesOf(sseContext.requestNode), List.of());

        TestContext wsContext = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        wsContext.treeSupport.ensureRequestStructure(wsContext.requestNode, wsContext.requestData);
        DefaultMutableTreeNode wsConnectNode = findChild(wsContext.requestNode, NodeType.WS_CONNECT);

        assertTrue(wsContext.treeSupport.hasDeletableNodes(paths(wsConnectNode)));
        List<DefaultMutableTreeNode> deletedWsNodes = wsContext.treeSupport.deleteNodes(paths(wsConnectNode));

        assertEquals(deletedWsNodes.size(), 1);
        assertEquals(childTypesOf(wsContext.requestNode), List.of());
    }

    @Test(description = "Khi xóa một nút có nhiều lựa chọn, các nút con trùng lặp trong cây con của nút cha sẽ được lọc")
    public void shouldDeleteOnlyTopLevelNodesFromMultiSelection() {
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode groupNode = newNode("Group", NodeType.THREAD_GROUP);
        DefaultMutableTreeNode requestNode = newRequestNode("request", "Request");
        DefaultMutableTreeNode timerNode = newNode("Timer", NodeType.TIMER);
        root.add(groupNode);
        groupNode.add(requestNode);
        requestNode.add(timerNode);
        PerformanceTreeSupport treeSupport = new PerformanceTreeSupport(new DefaultTreeModel(root));

        List<DefaultMutableTreeNode> deletedNodes = treeSupport.deleteNodes(paths(root, requestNode, timerNode));

        assertEquals(deletedNodes.size(), 1);
        assertSame(deletedNodes.get(0), requestNode);
        assertEquals(childTypesOf(groupNode), List.of());
        assertFalse(treeSupport.hasDeletableNodes(paths(root)));
    }

    @Test(description = "Sau khi xóa nút giai đoạn giao thức, việc đồng bộ hóa thông thường sẽ không tự động bù đắp cho nó")
    public void shouldNotRecreateDeletedProtocolStagesDuringSync() {
        TestContext sseContext = newSseTestContext();
        sseContext.treeSupport.ensureRequestStructure(sseContext.requestNode, sseContext.requestData);
        sseContext.treeSupport.deleteNodes(paths(
                findChild(sseContext.requestNode, NodeType.SSE_CONNECT),
                findChild(sseContext.requestNode, NodeType.SSE_READ)
        ));

        sseContext.treeSupport.syncRequestStructure(sseContext.requestNode, sseContext.requestData);

        assertEquals(childTypesOf(sseContext.requestNode), List.of());

        TestContext wsContext = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        wsContext.treeSupport.ensureRequestStructure(wsContext.requestNode, wsContext.requestData);
        wsContext.treeSupport.deleteNodes(paths(findChild(wsContext.requestNode, NodeType.WS_CONNECT)));

        wsContext.treeSupport.syncRequestStructure(wsContext.requestNode, wsContext.requestData);

        assertEquals(childTypesOf(wsContext.requestNode), List.of());
    }

    @Test(description = "Các nút pha giao thức sẽ có thể được bổ sung bằng cách bổ sung rõ ràng")
    public void shouldAddProtocolStageNodesExplicitly() {
        TestContext sseContext = newSseTestContext();
        JTree sseTree = new JTree(sseContext.treeModel);
        sseTree.setSelectionPath(new TreePath(sseContext.requestNode.getPath()));

        sseContext.treeSupport.addSseStageNode(sseTree, NodeType.SSE_CONNECT, () -> {
        });
        sseContext.treeSupport.addSseStageNode(sseTree, NodeType.SSE_READ, () -> {
        });

        assertEquals(childTypesOf(sseContext.requestNode), List.of(NodeType.SSE_CONNECT, NodeType.SSE_READ));

        TestContext wsContext = newTestContext(RequestItemProtocolEnum.WEBSOCKET);
        JTree wsTree = new JTree(wsContext.treeModel);
        wsTree.setSelectionPath(new TreePath(wsContext.requestNode.getPath()));

        wsContext.treeSupport.addWebSocketStepNode(wsTree, NodeType.WS_CONNECT, () -> {
        });

        assertEquals(childTypesOf(wsContext.requestNode), List.of(NodeType.WS_CONNECT));
    }

    @Test(description = "Accept Tiêu đề không được ngầm thay đổi kịch bản kiểm tra căng thẳng.SSE Chế độ được người dùng lựa chọn rõ ràng")
    public void shouldNotInferSsePerformanceModeFromAcceptHeader() {
        TestContext context = newTestContext(RequestItemProtocolEnum.HTTP);
        context.requestData.httpRequestItem.setHeadersList(List.of(
                new HttpHeader(true, "Accept", "text/event-stream")
        ));

        assertEquals(context.treeSupport.resolvePerformanceProtocol(context.requestData), PerformanceProtocol.HTTP);

        context.requestData.ssePerformanceEnabled = true;
        context.treeSupport.ensureRequestStructure(context.requestNode, context.requestData);

        assertEquals(context.treeSupport.resolvePerformanceProtocol(context.requestData), PerformanceProtocol.SSE);
    }

    private static TestContext newTestContext(RequestItemProtocolEnum protocol) {
        HttpRequestItem item = new HttpRequestItem();
        item.setName("Request");
        item.setProtocol(protocol);
        PerformanceTreeNode requestData = new PerformanceTreeNode("Request", NodeType.REQUEST, item);
        DefaultMutableTreeNode root = newNode("Plan", NodeType.ROOT);
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(requestData);
        root.add(requestNode);
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        return new TestContext(new PerformanceTreeSupport(treeModel), treeModel, requestNode, requestData);
    }

    private static TestContext newSseTestContext() {
        TestContext context = newTestContext(RequestItemProtocolEnum.HTTP);
        context.requestData.ssePerformanceEnabled = true;
        return context;
    }

    private static DefaultMutableTreeNode newNode(String name, NodeType type) {
        return new DefaultMutableTreeNode(new PerformanceTreeNode(name, type));
    }

    private static DefaultMutableTreeNode newRequestNode(String id, String name) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        return new DefaultMutableTreeNode(new PerformanceTreeNode(name, NodeType.REQUEST, item));
    }

    private static DefaultMutableTreeNode newLoopNode(int iterations) {
        PerformanceTreeNode loopData = new PerformanceTreeNode("Loop", NodeType.LOOP);
        loopData.loopData = new LoopData();
        loopData.loopData.iterations = iterations;
        return new DefaultMutableTreeNode(loopData);
    }

    private static DefaultMutableTreeNode findChild(DefaultMutableTreeNode parent, NodeType type) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object userObject = child.getUserObject();
            if (userObject instanceof PerformanceTreeNode node && node.type == type) {
                return child;
            }
        }
        return null;
    }

    private static List<NodeType> childTypesOf(DefaultMutableTreeNode parent) {
        List<NodeType> types = new ArrayList<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            Object userObject = ((DefaultMutableTreeNode) parent.getChildAt(i)).getUserObject();
            if (userObject instanceof PerformanceTreeNode node) {
                types.add(node.type);
            }
        }
        return types;
    }

    private static TreePath[] paths(DefaultMutableTreeNode... nodes) {
        TreePath[] paths = new TreePath[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            paths[i] = new TreePath(nodes[i].getPath());
        }
        return paths;
    }

    private static NodeType nodeType(DefaultMutableTreeNode node) {
        return ((PerformanceTreeNode) node.getUserObject()).type;
    }

    private record TestContext(
            PerformanceTreeSupport treeSupport,
            DefaultTreeModel treeModel,
            DefaultMutableTreeNode requestNode,
            PerformanceTreeNode requestData
    ) {
    }
}
