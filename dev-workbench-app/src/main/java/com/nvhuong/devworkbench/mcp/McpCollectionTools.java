package com.nvhuong.devworkbench.mcp;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import com.nvhuong.devworkbench.collection.model.CollectionNode;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.mcp.server.McpExtraTool;
import com.nvhuong.devworkbench.request.model.AuthApiKeyPlacement;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.RequestAuthTypes;
import com.nvhuong.devworkbench.request.model.RequestBodyTypes;
import com.nvhuong.devworkbench.service.collections.CollectionDocumentJsonCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MCP tools that edit the saved collections of a workspace: create collections, folders and requests, update and
 * delete requests. Every write rewrites collections.json atomically; the desktop app notices the change, reloads
 * its tree and keeps these additions even if it saves at the same moment. The write tools are only listed with
 * --allow-writes; list_folders is read-only.
 */
final class McpCollectionTools {
    static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
    private static final Map<String, String> AUTH_TYPES = Map.of(
            "inherit", RequestAuthTypes.AUTH_TYPE_INHERIT,
            "none", RequestAuthTypes.AUTH_TYPE_NONE,
            "bearer", RequestAuthTypes.AUTH_TYPE_BEARER,
            "basic", RequestAuthTypes.AUTH_TYPE_BASIC,
            "digest", RequestAuthTypes.AUTH_TYPE_DIGEST,
            "apikey", RequestAuthTypes.AUTH_TYPE_API_KEY);
    private static final Map<String, String> BODY_TYPES = Map.of(
            "none", RequestBodyTypes.BODY_TYPE_NONE,
            "raw", RequestBodyTypes.BODY_TYPE_RAW,
            "json", RequestBodyTypes.BODY_TYPE_RAW,
            "x-www-form-urlencoded", RequestBodyTypes.BODY_TYPE_FORM_URLENCODED,
            "form-data", RequestBodyTypes.BODY_TYPE_FORM_DATA);

    private final McpWorkspaceToolService service;
    private final ReentrantLock writeLock = new ReentrantLock();

    McpCollectionTools(McpWorkspaceToolService service) {
        this.service = service;
    }

    List<McpExtraTool> tools() {
        List<McpExtraTool> tools = new ArrayList<>();
        tools.add(new McpExtraTool("list_folders", "List collection folders",
                "Lists every collection and folder with its id and path, so a request can be created in the right "
                        + "place (parentId of create_request / create_folder).",
                schema(Map.of("workspaceId", string("Workspace id from list_workspaces."),
                        "collectionId", string("Optional collection id to list only that collection.")), List.of()),
                true, this::listFolders));
        if (!service.allowWrites()) {
            return tools;
        }
        tools.add(new McpExtraTool("create_collection", "Create a collection",
                "Creates a new top-level collection and returns its id. Only available with --allow-writes.",
                schema(Map.of("workspaceId", string("Workspace id."),
                        "name", string("Collection name."),
                        "description", string("Optional description (Markdown).")), List.of("name")),
                false, this::createCollection));
        tools.add(new McpExtraTool("create_folder", "Create a folder",
                "Creates a folder inside a collection or folder (parentId from list_collections or list_folders) and "
                        + "returns its id. Only available with --allow-writes.",
                schema(Map.of("workspaceId", string("Workspace id."),
                        "parentId", string("Id of the collection or folder that will contain the new folder."),
                        "name", string("Folder name.")), List.of("parentId", "name")),
                false, this::createFolder));
        tools.add(new McpExtraTool("create_request", "Create a request",
                "Saves a new HTTP request in a collection or folder and returns its requestId (usable with "
                        + "run_request). Put secrets in environment variables and reference them as {{name}} instead "
                        + "of writing them into the request. Only available with --allow-writes.",
                schema(requestProperties(true), List.of("parentId", "name", "url")),
                false, this::createRequest));
        tools.add(new McpExtraTool("update_request", "Update a request",
                "Changes a saved request. Only the fields you pass are changed; headers, params and formFields "
                        + "replace the whole list when given. Only available with --allow-writes.",
                schema(requestProperties(false), List.of("requestId")),
                false, this::updateRequest));
        tools.add(new McpExtraTool("delete_request", "Delete a request",
                "Deletes a saved request from its collection. Only available with --allow-writes.",
                schema(Map.of("workspaceId", string("Workspace id."),
                        "requestId", string("Request id from list_requests.")), List.of("requestId")),
                false, this::deleteRequest));
        return tools;
    }

    Map<String, Object> listFolders(Map<String, Object> arguments) {
        McpAuthorizedWorkspace workspace = service.resolveWorkspace(arguments);
        String collectionId = McpToolArguments.optionalString(arguments, "collectionId");
        CollectionDocument document = read(workspace);
        List<Map<String, Object>> folders = new ArrayList<>();
        for (CollectionNode root : document.getRoots()) {
            if (root.isGroup() && (collectionId == null || collectionId.equals(root.asGroup().getId()))) {
                collectFolders(root, root.asGroup().getName(), folders);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspaceId", workspace.id());
        result.put("folders", folders);
        result.put("count", folders.size());
        return result;
    }

    Map<String, Object> createCollection(Map<String, Object> arguments) throws Exception {
        String name = McpToolArguments.requiredString(arguments, "name");
        return edit(arguments, document -> {
            RequestGroup group = new RequestGroup(name);
            group.setId(UUID.randomUUID().toString());
            String description = McpToolArguments.optionalString(arguments, "description");
            if (description != null) group.setDescription(description);
            List<CollectionNode> roots = document.mutableRootsCopy();
            roots.add(CollectionNode.group(group));
            return new Edit(new CollectionDocument(roots), Map.of("collectionId", group.getId(), "name", name));
        });
    }

    Map<String, Object> createFolder(Map<String, Object> arguments) throws Exception {
        String parentId = McpToolArguments.requiredString(arguments, "parentId");
        String name = McpToolArguments.requiredString(arguments, "name");
        return edit(arguments, document -> {
            CollectionNode parent = findGroup(document, parentId);
            RequestGroup group = new RequestGroup(name);
            group.setId(UUID.randomUUID().toString());
            parent.addChild(CollectionNode.group(group));
            return new Edit(document, Map.of("folderId", group.getId(), "name", name, "parentId", parentId));
        });
    }

    Map<String, Object> createRequest(Map<String, Object> arguments) throws Exception {
        String parentId = McpToolArguments.requiredString(arguments, "parentId");
        return edit(arguments, document -> {
            CollectionNode parent = findGroup(document, parentId);
            HttpRequestItem request = new HttpRequestItem();
            request.setId(UUID.randomUUID().toString());
            request.setName(McpToolArguments.requiredString(arguments, "name"));
            request.setMethod("GET");
            request.setUrl(McpToolArguments.requiredString(arguments, "url"));
            request.setBodyType(RequestBodyTypes.BODY_TYPE_NONE);
            apply(request, arguments);
            parent.addChild(CollectionNode.request(request));
            return new Edit(document, summary(request, parentId));
        });
    }

    Map<String, Object> updateRequest(Map<String, Object> arguments) throws Exception {
        String requestId = McpToolArguments.requiredString(arguments, "requestId");
        return edit(arguments, document -> {
            Located located = findRequest(document, requestId);
            apply(located.request(), arguments);
            Map<String, Object> result = summary(located.request(), located.parent().asGroup().getId());
            result.put("updatedFields", arguments.keySet().stream()
                    .filter(key -> !Set.of("workspaceId", "requestId").contains(key)).sorted().toList());
            return new Edit(document, result);
        });
    }

    Map<String, Object> deleteRequest(Map<String, Object> arguments) throws Exception {
        String requestId = McpToolArguments.requiredString(arguments, "requestId");
        return edit(arguments, document -> {
            Located located = findRequest(document, requestId);
            located.parent().getChildren().remove(located.node());
            return new Edit(document, Map.of("deleted", requestId, "name", located.request().getName()));
        });
    }

    /** Applies the optional request fields present in the arguments. */
    static void apply(HttpRequestItem request, Map<String, Object> arguments) {
        String name = McpToolArguments.optionalString(arguments, "name");
        if (name != null) request.setName(name);
        String url = McpToolArguments.optionalString(arguments, "url");
        if (url != null) request.setUrl(url);
        String method = McpToolArguments.optionalString(arguments, "method");
        if (method != null) {
            String upper = method.toUpperCase(Locale.ROOT);
            if (!METHODS.contains(upper)) throw new IllegalArgumentException("Unsupported method: " + method);
            request.setMethod(upper);
        }
        String description = McpToolArguments.optionalString(arguments, "description");
        if (description != null) request.setDescription(description);
        if (arguments.containsKey("headers")) {
            List<HttpHeader> headers = new ArrayList<>();
            McpToolArguments.stringMap(arguments, "headers").forEach((key, value) -> headers.add(new HttpHeader(true, key, value)));
            request.setHeadersList(headers);
        }
        if (arguments.containsKey("params")) {
            List<HttpParam> params = new ArrayList<>();
            McpToolArguments.stringMap(arguments, "params").forEach((key, value) -> params.add(new HttpParam(true, key, value)));
            request.setParamsList(params);
        }
        String bodyType = McpToolArguments.optionalString(arguments, "bodyType");
        if (bodyType != null) {
            String mapped = BODY_TYPES.get(bodyType.toLowerCase(Locale.ROOT));
            if (mapped == null) throw new IllegalArgumentException("Unsupported bodyType: " + bodyType + "; use one of " + BODY_TYPES.keySet());
            request.setBodyType(mapped);
            if ("json".equalsIgnoreCase(bodyType) && request.getHeadersList().stream()
                    .noneMatch(header -> "content-type".equalsIgnoreCase(header.getKey()))) {
                List<HttpHeader> headers = new ArrayList<>(request.getHeadersList());
                headers.add(new HttpHeader(true, "Content-Type", "application/json"));
                request.setHeadersList(headers);
            }
        }
        if (arguments.containsKey("body")) {
            Object body = arguments.get("body");
            request.setBody(body == null ? "" : body instanceof String text ? text
                    : com.nvhuong.devworkbench.util.JsonUtil.toJsonPrettyStr(body));
            if (bodyType == null && RequestBodyTypes.BODY_TYPE_NONE.equals(request.getBodyType()) && !request.getBody().isBlank()) {
                request.setBodyType(RequestBodyTypes.BODY_TYPE_RAW);
            }
        }
        if (arguments.containsKey("formFields")) {
            Map<String, String> fields = McpToolArguments.stringMap(arguments, "formFields");
            if (RequestBodyTypes.BODY_TYPE_FORM_DATA.equals(request.getBodyType())) {
                List<HttpFormData> formData = new ArrayList<>();
                fields.forEach((key, value) -> formData.add(new HttpFormData(true, key, HttpFormData.TYPE_TEXT, value)));
                request.setFormDataList(formData);
            } else {
                List<HttpFormUrlencoded> encoded = new ArrayList<>();
                fields.forEach((key, value) -> encoded.add(new HttpFormUrlencoded(true, key, value)));
                request.setUrlencodedList(encoded);
                if (!RequestBodyTypes.BODY_TYPE_FORM_DATA.equals(request.getBodyType())) {
                    request.setBodyType(RequestBodyTypes.BODY_TYPE_FORM_URLENCODED);
                }
            }
        }
        Object auth = arguments.get("auth");
        if (auth instanceof Map<?, ?> authMap) {
            String type = authMap.get("type") == null ? null : String.valueOf(authMap.get("type"));
            String mapped = type == null ? null : AUTH_TYPES.get(type.toLowerCase(Locale.ROOT));
            if (mapped == null) throw new IllegalArgumentException("auth.type must be one of " + AUTH_TYPES.keySet());
            request.setAuthType(mapped);
            request.setAuthToken(text(authMap.get("token")));
            request.setAuthUsername(text(authMap.get("username")));
            request.setAuthPassword(text(authMap.get("password")));
            request.setAuthApiKeyName(text(authMap.get("keyName")));
            request.setAuthApiKeyValue(text(authMap.get("keyValue")));
            request.setAuthApiKeyPlacement("query".equalsIgnoreCase(text(authMap.get("placement")))
                    ? AuthApiKeyPlacement.QUERY_PARAMS.getConstant() : AuthApiKeyPlacement.HEADER.getConstant());
        }
        String prescript = McpToolArguments.optionalString(arguments, "preRequestScript");
        if (prescript != null) request.setPrescript(prescript);
        String postscript = McpToolArguments.optionalString(arguments, "testScript");
        if (postscript != null) request.setPostscript(postscript);
    }

    private record Edit(CollectionDocument document, Map<String, Object> result) {
    }

    @FunctionalInterface
    private interface Editor {
        Edit apply(CollectionDocument document) throws Exception;
    }

    private Map<String, Object> edit(Map<String, Object> arguments, Editor editor) throws Exception {
        if (!service.allowWrites()) {
            throw new IllegalStateException("Editing collections needs --allow-writes");
        }
        McpAuthorizedWorkspace workspace = service.resolveWorkspace(arguments);
        Path file = workspace.runWorkspace().collectionsFile();
        writeLock.lockInterruptibly();
        try {
            Edit edit = editor.apply(read(workspace));
            CollectionDocumentJsonCodec.write(file.toFile(), edit.document());
            Map<String, Object> result = new LinkedHashMap<>(edit.result());
            result.put("workspaceId", workspace.id());
            return result;
        } finally {
            writeLock.unlock();
        }
    }

    private static CollectionDocument read(McpAuthorizedWorkspace workspace) {
        Path file = workspace.runWorkspace().collectionsFile();
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("collections.json does not exist in this workspace");
        }
        return CollectionDocumentJsonCodec.read(file.toFile());
    }

    private static void collectFolders(CollectionNode group, String path, List<Map<String, Object>> folders) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", group.asGroup().getId());
        item.put("name", group.asGroup().getName());
        item.put("path", path);
        item.put("requests", group.getChildren().stream().filter(CollectionNode::isRequest).count());
        folders.add(item);
        for (CollectionNode child : group.getChildren()) {
            if (child.isGroup()) collectFolders(child, path + " / " + child.asGroup().getName(), folders);
        }
    }

    private static CollectionNode findGroup(CollectionDocument document, String id) {
        for (CollectionNode root : document.getRoots()) {
            CollectionNode found = findGroup(root, id);
            if (found != null) return found;
        }
        throw new IllegalArgumentException("Collection or folder not found: " + id + ". Use list_collections or list_folders.");
    }

    private static CollectionNode findGroup(CollectionNode node, String id) {
        if (!node.isGroup()) return null;
        if (id.equals(node.asGroup().getId())) return node;
        for (CollectionNode child : node.getChildren()) {
            CollectionNode found = findGroup(child, id);
            if (found != null) return found;
        }
        return null;
    }

    private record Located(CollectionNode parent, CollectionNode node, HttpRequestItem request) {
    }

    private static Located findRequest(CollectionDocument document, String requestId) {
        for (CollectionNode root : document.getRoots()) {
            Located found = findRequest(root, requestId);
            if (found != null) return found;
        }
        throw new IllegalArgumentException("Request not found: " + requestId + ". Use list_requests.");
    }

    private static Located findRequest(CollectionNode group, String requestId) {
        if (!group.isGroup()) return null;
        for (CollectionNode child : group.getChildren()) {
            if (child.isRequest() && requestId.equals(child.asRequest().getId())) {
                return new Located(group, child, child.asRequest());
            }
            Located nested = findRequest(child, requestId);
            if (nested != null) return nested;
        }
        return null;
    }

    private static Map<String, Object> summary(HttpRequestItem request, String parentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requestId", request.getId());
        result.put("name", request.getName());
        result.put("method", request.getMethod());
        result.put("url", McpSensitiveDataRedactor.url(request.getUrl()));
        result.put("parentId", parentId);
        return result;
    }

    private static Map<String, Object> requestProperties(boolean create) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("workspaceId", string("Workspace id."));
        if (create) {
            properties.put("parentId", string("Id of the collection or folder (list_collections / list_folders)."));
        } else {
            properties.put("requestId", string("Request id from list_requests."));
        }
        properties.put("name", string("Request name."));
        properties.put("method", Map.of("type", "string", "enum", METHODS.stream().sorted().toList()));
        properties.put("url", string("URL; may use {{variables}}, e.g. {{baseUrl}}/orders/{{orderId}}."));
        properties.put("description", string("Optional description (Markdown)."));
        properties.put("headers", stringMap("Headers as name → value."));
        properties.put("params", stringMap("Query parameters as name → value."));
        properties.put("bodyType", Map.of("type", "string",
                "enum", List.of("none", "raw", "json", "x-www-form-urlencoded", "form-data"),
                "description", "json = raw body with Content-Type application/json."));
        properties.put("body", Map.of("description", "Raw body: a string, or a JSON object/array that is saved pretty-printed."));
        properties.put("formFields", stringMap("Text fields for form-data or x-www-form-urlencoded bodies."));
        properties.put("auth", Map.of("type", "object", "description",
                "Auth: {type: inherit|none|bearer|basic|digest|apikey, token, username, password, keyName, keyValue, placement: header|query}. Prefer {{variables}} for secrets.",
                "properties", Map.of("type", Map.of("type", "string"))));
        properties.put("preRequestScript", string("JavaScript run before the request (pm API)."));
        properties.put("testScript", string("JavaScript run after the response, e.g. pm.test(...)."));
        return properties;
    }

    private static Map<String, Object> stringMap(String description) {
        return Map.of("type", "object", "additionalProperties", Map.of("type", "string"), "description", description);
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
