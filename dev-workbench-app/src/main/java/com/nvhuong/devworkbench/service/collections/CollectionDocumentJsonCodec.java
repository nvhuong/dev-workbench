package com.nvhuong.devworkbench.service.collections;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import com.nvhuong.devworkbench.collection.model.CollectionNode;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.model.Variable;
import com.nvhuong.devworkbench.request.model.AuthApiKeyPlacement;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

@UtilityClass
public class CollectionDocumentJsonCodec {

    public CollectionDocument read(File file) {
        return fromJson(JSONUtil.readJSONArray(file, StandardCharsets.UTF_8));
    }

    /**
     * Writes through a temporary file in the same directory and then replaces the target, so a crash or a concurrent
     * reader (the MCP server, the desktop app) never sees a half-written collections.json.
     */
    public void write(File file, CollectionDocument document) throws IOException {
        java.nio.file.Path target = file.toPath().toAbsolutePath();
        java.nio.file.Path directory = target.getParent();
        if (directory != null) java.nio.file.Files.createDirectories(directory);
        java.nio.file.Path temp = java.nio.file.Files.createTempFile(directory, "." + target.getFileName(), ".tmp");
        try {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(temp.toFile()), StandardCharsets.UTF_8)) {
                writer.write(toJson(document).toStringPretty());
            }
            try {
                java.nio.file.Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                java.nio.file.Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            java.nio.file.Files.deleteIfExists(temp);
        }
    }

    public JSONArray toJson(CollectionDocument document) {
        JSONArray array = new JSONArray();
        if (document == null) {
            return array;
        }
        for (CollectionNode node : document.getRoots()) {
            if (node != null && node.isGroup()) {
                array.add(toGroupJson(node));
            }
        }
        return array;
    }

    public CollectionDocument fromJson(JSONArray array) {
        if (array == null) {
            return CollectionDocument.empty();
        }
        List<CollectionNode> roots = array.stream()
                .filter(JSONObject.class::isInstance)
                .map(JSONObject.class::cast)
                .map(CollectionDocumentJsonCodec::fromGroupJson)
                .toList();
        return new CollectionDocument(roots);
    }

    private JSONObject toGroupJson(CollectionNode node) {
        JSONObject groupJson = new JSONObject();
        groupJson.set("type", "group");

        RequestGroup group = node.asGroup();
        groupJson.set("id", group.getId());
        groupJson.set("name", group.getName());
        groupJson.set("description", group.getDescription());
        groupJson.set("authType", group.getAuthType());
        groupJson.set("authUsername", group.getAuthUsername());
        groupJson.set("authPassword", group.getAuthPassword());
        groupJson.set("authToken", group.getAuthToken());
        groupJson.set("authApiKeyName", group.getAuthApiKeyName());
        groupJson.set("authApiKeyValue", group.getAuthApiKeyValue());
        groupJson.set("authApiKeyPlacement", group.getAuthApiKeyPlacement());
        groupJson.set("prescript", group.getPrescript());
        groupJson.set("postscript", group.getPostscript());
        if (group.getHeaders() != null && !group.getHeaders().isEmpty()) {
            groupJson.set("headers", group.getHeaders());
        }
        if (group.getVariables() != null && !group.getVariables().isEmpty()) {
            groupJson.set("variables", group.getVariables());
        }

        JSONArray children = new JSONArray();
        for (CollectionNode child : node.getChildren()) {
            if (child.isGroup()) {
                children.add(toGroupJson(child));
            } else if (child.isRequest()) {
                children.add(toRequestJson(child.asRequest()));
            }
        }
        groupJson.set("children", children);
        return groupJson;
    }

    private JSONObject toRequestJson(HttpRequestItem requestItem) {
        SavedResponseSnapshotMapper.sanitizeSavedResponses(requestItem);
        JSONObject requestJson = new JSONObject();
        requestJson.set("type", "request");
        requestJson.set("data", JSONUtil.parseObj(requestItem));
        return requestJson;
    }

    private CollectionNode fromGroupJson(JSONObject groupJson) {
        String name = groupJson.getStr("name");
        RequestGroup group = new RequestGroup(name);

        String id = groupJson.getStr("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Collection group is missing required id: " + name);
        }
        group.setId(id);

        if (groupJson.containsKey("description")) {
            group.setDescription(groupJson.getStr("description", ""));
        }
        if (groupJson.containsKey("authType")) {
            group.setAuthType(groupJson.getStr("authType", ""));
            group.setAuthUsername(groupJson.getStr("authUsername", ""));
            group.setAuthPassword(groupJson.getStr("authPassword", ""));
            group.setAuthToken(groupJson.getStr("authToken", ""));
            group.setAuthApiKeyName(groupJson.getStr("authApiKeyName", ""));
            group.setAuthApiKeyValue(groupJson.getStr("authApiKeyValue", ""));
            group.setAuthApiKeyPlacement(groupJson.getStr(
                    "authApiKeyPlacement",
                    AuthApiKeyPlacement.HEADER.getConstant()
            ));
        }
        if (groupJson.containsKey("prescript")) {
            group.setPrescript(groupJson.getStr("prescript", ""));
        }
        if (groupJson.containsKey("postscript")) {
            group.setPostscript(groupJson.getStr("postscript", ""));
        }
        if (groupJson.containsKey("headers")) {
            JSONArray headersArray = groupJson.getJSONArray("headers");
            if (headersArray != null && !headersArray.isEmpty()) {
                group.setHeaders(JSONUtil.toList(headersArray, HttpHeader.class));
            }
        }
        if (groupJson.containsKey("variables")) {
            JSONArray variablesArray = groupJson.getJSONArray("variables");
            if (variablesArray != null && !variablesArray.isEmpty()) {
                group.setVariables(JSONUtil.toList(variablesArray, Variable.class));
            }
        }

        CollectionNode groupNode = CollectionNode.group(group);
        JSONArray children = groupJson.getJSONArray("children");
        if (children == null) {
            return groupNode;
        }
        for (Object child : children) {
            if (!(child instanceof JSONObject childJson)) {
                continue;
            }
            String type = childJson.getStr("type");
            if ("group".equals(type)) {
                groupNode.addChild(fromGroupJson(childJson));
            } else if ("request".equals(type)) {
                groupNode.addChild(fromRequestJson(childJson));
            }
        }
        return groupNode;
    }

    private CollectionNode fromRequestJson(JSONObject requestJson) {
        JSONObject dataJson = requestJson.getJSONObject("data");
        RequestProtocolStorageMigration.normalizeLegacySseProtocol(dataJson);
        HttpRequestItem item = JSONUtil.toBean(dataJson, HttpRequestItem.class);
        item.setBody(item.getBody() != null ? item.getBody() : "");
        SavedResponseSnapshotMapper.sanitizeSavedResponses(item);
        if (item.getId() == null || item.getId().isEmpty()) {
            throw new IllegalArgumentException("Collection request is missing required id: " + item.getName());
        }
        return CollectionNode.request(item);
    }
}
