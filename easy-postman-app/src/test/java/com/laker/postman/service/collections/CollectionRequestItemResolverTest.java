package com.laker.postman.service.collections;

import com.laker.postman.request.model.HttpRequestItem;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;

public class CollectionRequestItemResolverTest {

    @Test(description = "Nên đăng ký Collection Bấm vào nút gốc cây ID Tìm yêu cầu và trả lại một bản sao sâu")
    public void shouldResolveRequestItemFromRegisteredCollectionTreeRoot() {
        HttpRequestItem requestItem = new HttpRequestItem();
        requestItem.setId("req-registered");
        requestItem.setName("registered");
        requestItem.setUrl("https://example.com/registered");
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
        rootNode.add(CollectionTreeNodes.requestNode(requestItem));

        try {
            CollectionTreeRootRegistry.registerRootSupplier(() -> rootNode);

            HttpRequestItem result = CollectionRequestItemResolver.resolveCurrentRequest("req-registered")
                    .orElse(null);

            assertNotSame(result, requestItem);
            assertEquals(result.getId(), requestItem.getId());
            assertEquals(result.getUrl(), requestItem.getUrl());
        } finally {
            CollectionTreeRootRegistry.clear();
        }
    }

    @Test
    public void shouldReturnEmptyWhenRequestIsMissing() {
        assertTrue(CollectionRequestItemResolver.resolveCurrentRequest("missing").isEmpty());
    }
}
