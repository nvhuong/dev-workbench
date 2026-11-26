package com.nvhuong.devworkbench.mcp;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import com.nvhuong.devworkbench.collection.model.CollectionNode;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.service.collections.CollectionDocumentJsonCodec;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class McpCollectionToolsTest {

    @Test
    public void writeToolsAreOnlyListedWithAllowWrites() throws Exception {
        Path workspace = workspace();
        McpWorkspaceToolService readOnly = new McpWorkspaceToolService(new McpServeOptions(workspace.toString(), 65536, false));
        List<String> names = new McpCollectionTools(readOnly).tools().stream().map(tool -> tool.name()).toList();
        assertEquals(names, List.of("list_folders"));

        McpWorkspaceToolService writable = new McpWorkspaceToolService(new McpServeOptions(workspace.toString(), 65536, true));
        List<String> writeNames = new McpCollectionTools(writable).tools().stream().map(tool -> tool.name()).toList();
        assertTrue(writeNames.containsAll(List.of("create_collection", "create_folder", "create_request",
                "update_request", "delete_request")), writeNames.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void createUpdateDeleteRequestRoundTripThroughCollectionsJson() throws Exception {
        Path workspace = workspace();
        McpWorkspaceToolService service = new McpWorkspaceToolService(new McpServeOptions(workspace.toString(), 65536, true));
        McpCollectionTools tools = new McpCollectionTools(service);

        Map<String, Object> folders = tools.listFolders(Map.of());
        String folderId = ((List<Map<String, Object>>) folders.get("folders")).get(0).get("id").toString();

        Map<String, Object> created = tools.createRequest(Map.of("parentId", folderId, "name", "Create order",
                "method", "post", "url", "{{baseUrl}}/orders",
                "bodyType", "json", "body", Map.of("sku", "A-1", "qty", 2),
                "headers", Map.of("X-Trace", "mcp"),
                "auth", Map.of("type", "bearer", "token", "{{apiToken}}")));
        String requestId = created.get("requestId").toString();

        CollectionDocument saved = CollectionDocumentJsonCodec.read(workspace.resolve("collections.json").toFile());
        HttpRequestItem request = findRequest(saved, requestId);
        assertEquals(request.getMethod(), "POST");
        assertEquals(request.getBodyType(), "raw");
        assertTrue(request.getBody().contains("\"sku\""), request.getBody());
        assertEquals(request.getAuthToken(), "{{apiToken}}");
        assertTrue(request.getHeadersList().stream().anyMatch(header -> "Content-Type".equals(header.getKey())));

        tools.updateRequest(Map.of("requestId", requestId, "url", "{{baseUrl}}/orders/v2"));
        saved = CollectionDocumentJsonCodec.read(workspace.resolve("collections.json").toFile());
        assertEquals(findRequest(saved, requestId).getUrl(), "{{baseUrl}}/orders/v2");
        assertEquals(findRequest(saved, requestId).getName(), "Create order", "untouched fields stay");

        tools.deleteRequest(Map.of("requestId", requestId));
        saved = CollectionDocumentJsonCodec.read(workspace.resolve("collections.json").toFile());
        assertEquals(findRequest(saved, requestId), null);
        assertTrue(Files.readString(workspace.resolve("collections.json")).contains("Basic Examples"));
    }

    @Test
    public void createCollectionAndFolderAndRejectUnknownParents() throws Exception {
        Path workspace = workspace();
        McpWorkspaceToolService service = new McpWorkspaceToolService(new McpServeOptions(workspace.toString(), 65536, true));
        McpCollectionTools tools = new McpCollectionTools(service);

        Map<String, Object> collection = tools.createCollection(Map.of("name", "Payments"));
        Map<String, Object> folder = tools.createFolder(Map.of("parentId", collection.get("collectionId"), "name", "Refunds"));
        assertEquals(folder.get("parentId"), collection.get("collectionId"));

        expectThrows(IllegalArgumentException.class, () -> tools.createFolder(Map.of("parentId", "missing", "name", "x")));
        expectThrows(IllegalArgumentException.class, () -> tools.createRequest(Map.of("parentId", "missing",
                "name", "x", "url", "https://example.test")));
        expectThrows(IllegalArgumentException.class, () -> tools.createRequest(Map.of("parentId",
                folder.get("folderId"), "name", "x", "url", "https://example.test", "method", "TRACE")));
        CollectionDocument saved = CollectionDocumentJsonCodec.read(workspace.resolve("collections.json").toFile());
        assertFalse(saved.getRoots().stream().anyMatch(node -> node.isGroup() && "x".equals(node.asGroup().getName())));
    }

    @Test
    public void editingNeedsAllowWrites() throws Exception {
        Path workspace = workspace();
        McpWorkspaceToolService readOnly = new McpWorkspaceToolService(new McpServeOptions(workspace.toString(), 65536, false));
        McpCollectionTools tools = new McpCollectionTools(readOnly);
        expectThrows(IllegalStateException.class, () -> tools.createCollection(Map.of("name", "Nope")));
    }

    private static HttpRequestItem findRequest(CollectionDocument document, String id) {
        for (CollectionNode root : document.getRoots()) {
            HttpRequestItem found = findRequest(root, id);
            if (found != null) return found;
        }
        return null;
    }

    private static HttpRequestItem findRequest(CollectionNode node, String id) {
        if (node.isRequest()) return id.equals(node.asRequest().getId()) ? node.asRequest() : null;
        for (CollectionNode child : node.getChildren()) {
            HttpRequestItem found = findRequest(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private static Path workspace() throws Exception {
        Path workspace = Files.createTempDirectory("dev-workbench-mcp-collections-");
        RequestGroup group = new RequestGroup("Basic Examples");
        group.setId("folder-1");
        CollectionNode root = CollectionNode.group(group);
        HttpRequestItem existing = new HttpRequestItem();
        existing.setId("request-1");
        existing.setName("Health");
        existing.setMethod("GET");
        existing.setUrl("https://example.test/health");
        root.addChild(CollectionNode.request(existing));
        CollectionDocumentJsonCodec.write(workspace.resolve("collections.json").toFile(),
                new CollectionDocument(List.of(root)));
        return workspace;
    }
}
