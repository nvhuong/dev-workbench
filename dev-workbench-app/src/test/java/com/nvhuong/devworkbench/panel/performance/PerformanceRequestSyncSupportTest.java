package com.nvhuong.devworkbench.panel.performance;

import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.model.Variable;
import com.nvhuong.devworkbench.performance.plan.PerformanceRequestSnapshotMapper;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;


import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.panel.collections.tree.adapter.SwingCollectionTreeDocumentMapper;
import com.nvhuong.devworkbench.service.collections.CollectionDocumentRegistry;
import com.nvhuong.devworkbench.service.collections.CollectionTreeNodes;
import com.nvhuong.devworkbench.service.collections.CollectionTreeRootRegistry;
import com.nvhuong.devworkbench.service.variable.RequestExecutionContext;
import com.nvhuong.devworkbench.service.variable.RequestExecutionScope;
import com.nvhuong.devworkbench.test.AbstractSwingUiTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

public class PerformanceRequestSyncSupportTest extends AbstractSwingUiTest {

    @AfterMethod
    public void clearCollectionRootAndRequestScope() {
        CollectionTreeRootRegistry.clear();
        CollectionDocumentRegistry.registerDocumentSupplier(com.nvhuong.devworkbench.collection.model.CollectionDocument::empty);
        RequestExecutionContext.clearCurrentScope();
    }

    @Test(description = "syncRequestItem Các yêu cầu phù hợp trong cây phải được cập nhật và nội dung của trình soạn thảo hiện tại được đồng bộ hóa")
    public void shouldSyncMatchingRequestAndRefreshCurrentEditor() {
        HttpRequestItem oldItem = requestItem("req-1", "Old Request", "https://old.example.com");
        TestContext context = newTestContext(oldItem);
        List<String> syncEvents = new ArrayList<>();
        AtomicReference<HttpRequestItem> switchedItem = new AtomicReference<>();
        PerformanceRequestSyncSupport support = new PerformanceRequestSyncSupport(
                context.treeModel,
                new JTree(context.treeModel),
                (node, data) -> syncEvents.add(data.httpRequestItem.getId())
        );
        HttpRequestItem latestItem = requestItem("req-1", "New Request", "https://new.example.com");

        support.syncRequestItem(context.rootNode, latestItem, context.requestNode, switchedItem::set);

        PerformanceTreeNode requestData = (PerformanceTreeNode) context.requestNode.getUserObject();
        assertNotSame(requestData.httpRequestItem, latestItem);
        assertEquals(requestData.httpRequestItem.getUrl(), "https://new.example.com");
        assertEquals(requestData.name, "New Request");
        assertEquals(syncEvents, List.of("req-1"));
        assertSame(switchedItem.get(), requestData.httpRequestItem);
    }

    @Test(description = "syncRequestItem Nên tránh phạm vi biến nhóm của yêu cầu hiện tại tooltip Sử dụng ảnh chụp nhanh cũ")
    public void shouldRefreshCurrentRequestScopeWhenSyncingMatchingRequest() {
        HttpRequestItem oldItem = requestItem("req-1", "Old Request", "https://old.example.com?test={{testname}}");
        TestContext context = newTestContext(oldItem);
        PerformanceTreeNode requestData = (PerformanceTreeNode) context.requestNode.getUserObject();
        requestData.requestExecutionScope = RequestExecutionScope.fromGroupVariables(Map.of("testname", "333"));
        requestData.requestSnapshot = PerformanceRequestSnapshotMapper.fromHttpRequestItem(
                oldItem,
                requestData.requestExecutionScope
        );
        RequestExecutionContext.setCurrentScope(requestData.requestExecutionScope);

        HttpRequestItem latestItem = requestItem("req-1", "New Request", "https://new.example.com?test={{testname}}");
        registerCollectionRequest(latestItem, "888");
        PerformanceRequestSyncSupport support = new PerformanceRequestSyncSupport(
                context.treeModel,
                new JTree(context.treeModel),
                (node, data) -> {
                }
        );

        support.syncRequestItem(context.rootNode, latestItem, context.requestNode, ignored -> {
        });

        assertEquals(requestData.requestExecutionScope.getGroupVariable("testname"), "888");
        assertEquals(requestData.requestSnapshot.getExecutionScope().getGroupVariable("testname"), "888");
        assertEquals(RequestExecutionContext.getCurrentScope().getGroupVariable("testname"), "888");
    }

    @Test(description = "Làm mới collection Ảnh chụp nhanh phải được thực thi đồng bộ khi được yêu cầu để tránh trình chỉnh sửa hiển thị ảnh mới URL Nhưng việc gửi thực tế đã cũ URL")
    public void shouldRefreshRequestSnapshotWhenRefreshingFromCollections() {
        HttpRequestItem oldItem = requestItem("req-1", "GET Example", "https://httpbin.org/get?test={{testname}}");
        TestContext context = newTestContext(oldItem);
        PerformanceTreeNode requestData = (PerformanceTreeNode) context.requestNode.getUserObject();
        requestData.requestExecutionScope = RequestExecutionScope.fromGroupVariables(Map.of("testname", "333"));
        requestData.requestSnapshot = PerformanceRequestSnapshotMapper.fromHttpRequestItem(
                oldItem,
                requestData.requestExecutionScope
        );

        HttpRequestItem latestItem = requestItem("req-1", "GET Example", "https://httpbingo.org/get?test={{testname}}");
        registerCollectionRequest(latestItem, "888");
        PerformanceRequestSyncSupport support = new PerformanceRequestSyncSupport(
                context.treeModel,
                new JTree(context.treeModel),
                (node, data) -> {
                }
        );

        support.refreshRequestsFromCollections(
                context.requestNode,
                ignored -> {
                },
                () -> {
                },
                () -> {
                },
                () -> {
                }
        );

        assertEquals(requestData.httpRequestItem.getUrl(), "https://httpbingo.org/get?test={{testname}}");
        assertEquals(requestData.requestSnapshot.getUrl(), "https://httpbingo.org/get?test={{testname}}");
        assertEquals(requestData.requestSnapshot.getExecutionScope().getGroupVariable("testname"), "888");
    }

    @Test(description = "syncRequestItem Đã gặp phải yêu cầu trống hoặc không có giá trị id nên đơn giản là bỏ qua")
    public void shouldIgnoreRequestWithoutId() {
        HttpRequestItem oldItem = requestItem("req-1", "Old Request", "https://old.example.com");
        TestContext context = newTestContext(oldItem);
        List<String> syncEvents = new ArrayList<>();
        AtomicReference<HttpRequestItem> switchedItem = new AtomicReference<>();
        PerformanceRequestSyncSupport support = new PerformanceRequestSyncSupport(
                context.treeModel,
                new JTree(context.treeModel),
                (node, data) -> syncEvents.add(data.httpRequestItem.getId())
        );
        HttpRequestItem invalidItem = new HttpRequestItem();
        invalidItem.setName("No Id");

        support.syncRequestItem(context.rootNode, invalidItem, context.requestNode, switchedItem::set);

        PerformanceTreeNode requestData = (PerformanceTreeNode) context.requestNode.getUserObject();
        assertSame(requestData.httpRequestItem, oldItem);
        assertEquals(requestData.name, "Old Request");
        assertEquals(syncEvents.size(), 0);
        assertNull(switchedItem.get());
    }

    private static TestContext newTestContext(HttpRequestItem item) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new PerformanceTreeNode("Plan", NodeType.ROOT));
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new PerformanceTreeNode("Group", NodeType.THREAD_GROUP));
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new PerformanceTreeNode(item.getName(), NodeType.REQUEST, item));
        rootNode.add(groupNode);
        groupNode.add(requestNode);
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        return new TestContext(rootNode, requestNode, treeModel);
    }

    private static HttpRequestItem requestItem(String id, String name, String url) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(id);
        item.setName(name);
        item.setUrl(url);
        return item;
    }

    private static void registerCollectionRequest(HttpRequestItem item, String variableValue) {
        RequestGroup group = new RequestGroup("Group");
        group.setVariables(List.of(new Variable(true, "testname", variableValue)));
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
        groupNode.add(CollectionTreeNodes.requestNode(item));
        rootNode.add(groupNode);
        CollectionTreeRootRegistry.registerRootSupplier(() -> rootNode);
        CollectionDocumentRegistry.registerDocumentSupplier(() -> SwingCollectionTreeDocumentMapper.fromRoot(rootNode));
    }

    private record TestContext(
            DefaultMutableTreeNode rootNode,
            DefaultMutableTreeNode requestNode,
            DefaultTreeModel treeModel
    ) {
    }
}
