package com.nvhuong.devworkbench.service.devworkbench;

import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.model.Variable;
import com.nvhuong.devworkbench.request.model.AuthApiKeyPlacement;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;
import com.nvhuong.devworkbench.request.model.SavedResponse;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.nvhuong.devworkbench.service.collections.CollectionTreeNodes;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_API_KEY;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_BASIC;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_BEARER;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_DIGEST;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_INHERIT;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_NONE;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_BINARY;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_FORM_DATA;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_FORM_URLENCODED;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_RAW;

/**
 * DevWorkbench CollectionNhà xuất khẩu
 * Chịu trách nhiệm xuất cấu trúc dữ liệu nội bộ dưới dạngDevWorkbench Collectionđịnh dạng (v2.1định dạng)
 */
@UtilityClass
public class DevWorkbenchCollectionExporter {

    /**
     * Xây dựng đệ quy từ các nút cây DevWorkbench Collection v2.1 JSON
     *
     * @param groupNode Các nút nhóm
     * @param groupName Tên nhóm (nếu nút gốc có RequestGroup，sẽ sử dụng tên của nó)
     * @return DevWorkbench Collection v2.1 JSONObject
     */
    public static JSONObject buildDevWorkbenchCollectionFromTreeNode(DefaultMutableTreeNode groupNode, String groupName) {
        JSONObject collection = new JSONObject();
        JSONObject info = new JSONObject();
        info.put("_postman_id", UUID.randomUUID().toString());

        RequestGroup rootGroup = CollectionTreeNodes.group(groupNode).orElse(null);
        if (rootGroup != null) {
            groupName = rootGroup.getName();
        }

        info.put("name", groupName);
        info.put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");

        // Xuất mô tả bộ sưu tập
        if (rootGroup != null && rootGroup.getDescription() != null && !rootGroup.getDescription().isEmpty()) {
            info.put("description", rootGroup.getDescription());
        }

        collection.put("info", info);

        // Xuất chứng chỉ cấp bộ sưu tập (từ nút gốc)
        if (rootGroup != null && rootGroup.hasAuth()) {
            JSONObject auth = authJson(
                    rootGroup.getAuthType(),
                    rootGroup.getAuthUsername(),
                    rootGroup.getAuthPassword(),
                    rootGroup.getAuthToken(),
                    rootGroup.getAuthApiKeyName(),
                    rootGroup.getAuthApiKeyValue(),
                    rootGroup.getAuthApiKeyPlacement()
            );
            if (auth != null) {
                collection.put("auth", auth);
            }
        }

        // Xuất tập lệnh cấp bộ sưu tập (từ nút gốc)
        if (rootGroup != null && (rootGroup.hasPreScript() || rootGroup.hasPostScript())) {
            JSONArray events = new JSONArray();
            if (rootGroup.hasPreScript()) {
                JSONObject pre = new JSONObject();
                pre.put("listen", "prerequest");
                JSONObject script = new JSONObject();
                script.put("type", "text/javascript");
                script.put("exec", Arrays.asList(rootGroup.getPrescript().split("\n")));
                pre.put("script", script);
                events.add(pre);
            }
            if (rootGroup.hasPostScript()) {
                JSONObject post = new JSONObject();
                post.put("listen", "test");
                JSONObject script = new JSONObject();
                script.put("type", "text/javascript");
                script.put("exec", Arrays.asList(rootGroup.getPostscript().split("\n")));
                post.put("script", script);
                events.add(post);
            }
            collection.put("event", events);
        }

        // Xuất các biến cấp độ bộ sưu tập (từ nút gốc)
        if (rootGroup != null && rootGroup.hasVariables()) {
            JSONArray variables = new JSONArray();
            for (Variable variable : rootGroup.getVariables()) {
                JSONObject varObj = new JSONObject();
                varObj.put("key", variable.getKey());
                varObj.put("value", variable.getValue());
                if (!variable.isEnabled()) {
                    varObj.put("disabled", true);
                }
                variables.add(varObj);
            }
            collection.put("variable", variables);
        }

        collection.put("item", buildDevWorkbenchItemsFromNode(groupNode));
        return collection;
    }

    /**
     * Xây dựng đệ quy DevWorkbench items mảng
     */
    private static JSONArray buildDevWorkbenchItemsFromNode(DefaultMutableTreeNode node) {
        JSONArray items = new JSONArray();
        for (int i = 0; i < node.getChildCount(); i++) {
            javax.swing.tree.DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            RequestGroup group = CollectionTreeNodes.group(child).orElse(null);
            if (group != null) {
                JSONObject folder = buildDevWorkbenchFolder(child, group);
                items.add(folder);
            } else {
                CollectionTreeNodes.request(child).ifPresent(item -> items.add(toDevWorkbenchItem(item)));
            }
        }
        return items;
    }

    private static JSONObject buildDevWorkbenchFolder(DefaultMutableTreeNode child, RequestGroup group) {
        JSONObject folder = new JSONObject();
        folder.put("name", group.getName());

        if (group.getDescription() != null && !group.getDescription().isEmpty()) {
            folder.put("description", group.getDescription());
        }

        if (group.hasAuth()) {
            JSONObject auth = authJson(
                    group.getAuthType(),
                    group.getAuthUsername(),
                    group.getAuthPassword(),
                    group.getAuthToken(),
                    group.getAuthApiKeyName(),
                    group.getAuthApiKeyValue(),
                    group.getAuthApiKeyPlacement()
            );
            if (auth != null) {
                folder.put("auth", auth);
            }
        }

        JSONArray events = new JSONArray();
        if (group.hasPreScript()) {
            JSONObject pre = new JSONObject();
            pre.put("listen", "prerequest");
            JSONObject script = new JSONObject();
            script.put("type", "text/javascript");
            script.put("exec", Arrays.asList(group.getPrescript().split("\n")));
            pre.put("script", script);
            events.add(pre);
        }
        if (group.hasPostScript()) {
            JSONObject post = new JSONObject();
            post.put("listen", "test");
            JSONObject script = new JSONObject();
            script.put("type", "text/javascript");
            script.put("exec", Arrays.asList(group.getPostscript().split("\n")));
            post.put("script", script);
            events.add(post);
        }
        if (!events.isEmpty()) {
            folder.put("event", events);
        }

        folder.put("item", buildDevWorkbenchItemsFromNode(child));
        return folder;
    }

    /**
     * HttpRequestItem thay đổi DevWorkbench Yêu cầu duy nhất item
     */
    public static JSONObject toDevWorkbenchItem(HttpRequestItem item) {
        JSONObject devworkbenchItem = new JSONObject();
        devworkbenchItem.put("name", item.getName());
        JSONObject request = new JSONObject();

        // Mô tả yêu cầu xuất
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            request.put("description", item.getDescription());
        }

        request.put("method", item.getMethod());
        // url
        JSONObject url = new JSONObject();
        String rawUrl = item.getUrl();
        url.put("raw", rawUrl);
        try {
            java.net.URI uri = new java.net.URI(rawUrl);
            // protocol
            if (uri.getScheme() != null) {
                url.put("protocol", uri.getScheme());
            }
            // host
            if (uri.getHost() != null) {
                String[] hostParts = uri.getHost().split("\\.");
                url.put("host", Arrays.asList(hostParts));
            }
            // path
            if (uri.getPath() != null && !uri.getPath().isEmpty()) {
                String[] pathParts = uri.getPath().replaceFirst("^/", "").split("/");
                url.put("path", Arrays.asList(pathParts));
            }
            // query
            if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
                JSONArray queryArr = new JSONArray();
                String[] pairs = uri.getQuery().split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    JSONObject q = new JSONObject();
                    q.put("key", kv[0]);
                    q.put("value", kv.length > 1 ? kv[1] : "");
                    queryArr.add(q);
                }
                url.put("query", queryArr);
            }
        } catch (Exception ignore) {
        }
        // Sự hợp nhất bổ sung params
        if (item.getParamsList() != null && !item.getParamsList().isEmpty()) {
            JSONArray queryArr = url.containsKey("query") ? url.getJSONArray("query") : new JSONArray();
            for (HttpParam param : item.getParamsList()) {
                if (param.isEnabled()) {
                    JSONObject q = new JSONObject();
                    q.put("key", param.getKey());
                    q.put("value", param.getValue());
                    putDescription(q, param.getDescription());
                    queryArr.add(q);
                }
            }
            url.put("query", queryArr);
        }
        addPathVariables(url, item.getPathVariablesList());
        request.put("url", url);
        // headers
        if (item.getHeadersList() != null && !item.getHeadersList().isEmpty()) {
            JSONArray headerArr = new JSONArray();
            for (HttpHeader header : item.getHeadersList()) {
                if (header.isEnabled()) {
                    JSONObject h = new JSONObject();
                    h.put("key", header.getKey());
                    h.put("value", header.getValue());
                    putDescription(h, header.getDescription());
                    headerArr.add(h);
                }
            }
            request.put("header", headerArr);
        }
        // body
        if (BODY_TYPE_BINARY.equals(item.getBodyType()) && item.getBody() != null && !item.getBody().isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("mode", "file");
            JSONObject file = new JSONObject();
            file.put("src", item.getBody());
            body.put("file", file);
            request.put("body", body);
        } else if (item.getBody() != null && !item.getBody().isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("mode", "raw");
            body.put("raw", item.getBody());
            request.put("body", body);
        } else if (item.getFormDataList() != null && !item.getFormDataList().isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("mode", "formdata");
            JSONArray arr = new JSONArray();
            for (HttpFormData formData : item.getFormDataList()) {
                if (formData.isEnabled()) {
                    JSONObject o = new JSONObject();
                    o.put("key", formData.getKey());
                    if (formData.isText()) {
                        o.put("value", formData.getValue());
                        o.put("type", "text");
                    } else if (formData.isFile()) {
                        o.put("src", formData.getValue());
                        o.put("type", "file");
                    }
                    putDescription(o, formData.getDescription());
                    arr.add(o);
                }
            }
            body.put("formdata", arr);
            request.put("body", body);
        }
        // urlencoded
        else if (item.getUrlencodedList() != null && !item.getUrlencodedList().isEmpty()) {
            JSONObject body = new JSONObject();
            body.put("mode", "urlencoded");
            JSONArray arr = new JSONArray();
            for (HttpFormUrlencoded encoded : item.getUrlencodedList()) {
                if (encoded.isEnabled()) {
                    JSONObject o = new JSONObject();
                    o.put("key", encoded.getKey());
                    o.put("value", encoded.getValue());
                    o.put("type", "text");
                    putDescription(o, encoded.getDescription());
                    arr.add(o);
                }
            }
            body.put("urlencoded", arr);
            request.put("body", body);
        }
        // auth
        JSONObject auth = authJson(
                item.getAuthType(),
                item.getAuthUsername(),
                item.getAuthPassword(),
                item.getAuthToken(),
                item.getAuthApiKeyName(),
                item.getAuthApiKeyValue(),
                item.getAuthApiKeyPlacement()
        );
        if (auth != null) {
            request.put("auth", auth);
        }
        devworkbenchItem.put("request", request);
        // kịch bản event
        JSONArray events = new JSONArray();
        if (item.getPrescript() != null && !item.getPrescript().isEmpty()) {
            JSONObject pre = new JSONObject();
            pre.put("listen", "prerequest");
            JSONObject script = new JSONObject();
            script.put("type", "text/javascript");
            script.put("exec", Arrays.asList(item.getPrescript().split("\n")));
            pre.put("script", script);
            events.add(pre);
        }
        if (item.getPostscript() != null && !item.getPostscript().isEmpty()) {
            JSONObject post = new JSONObject();
            post.put("listen", "test");
            JSONObject script = new JSONObject();
            script.put("type", "text/javascript");
            script.put("exec", Arrays.asList(item.getPostscript().split("\n")));
            post.put("script", script);
            events.add(post);
        }
        if (!events.isEmpty()) {
            devworkbenchItem.put("event", events);
        }

        // Xuất khẩu response（DevWorkbench của Examples/Saved Responses）
        if (item.getResponse() != null && !item.getResponse().isEmpty()) {
            JSONArray responsesArray = new JSONArray();
            for (SavedResponse savedResp : item.getResponse()) {
                JSONObject respJson = toDevWorkbenchResponse(savedResp);
                responsesArray.add(respJson);
            }
            devworkbenchItem.put("response", responsesArray);
        }

        return devworkbenchItem;
    }

    private static void putDescription(JSONObject target, String description) {
        if (description != null && !description.trim().isEmpty()) {
            target.put("description", description);
        }
    }

    private static JSONObject authJson(String authType,
                                       String username,
                                       String password,
                                       String token,
                                       String apiKeyName,
                                       String apiKeyValue,
                                       String apiKeyPlacement) {
        if (authType == null || AUTH_TYPE_NONE.equals(authType) || AUTH_TYPE_INHERIT.equals(authType)) {
            return null;
        }

        JSONObject auth = new JSONObject();
        if (AUTH_TYPE_BASIC.equals(authType)) {
            auth.put("type", "basic");
            JSONArray arr = new JSONArray();
            arr.add(new JSONObject().put("key", "username").put("value", username));
            arr.add(new JSONObject().put("key", "password").put("value", password));
            auth.put("basic", arr);
            return auth;
        }
        if (AUTH_TYPE_API_KEY.equals(authType)) {
            auth.put("type", "apikey");
            JSONArray arr = new JSONArray();
            arr.add(new JSONObject().put("key", "key").put("value", apiKeyName));
            arr.add(new JSONObject().put("key", "value").put("value", apiKeyValue));
            arr.add(new JSONObject()
                    .put("key", "in")
                    .put("value", AuthApiKeyPlacement.fromConstant(apiKeyPlacement).getDevworkbenchValue()));
            auth.put("apikey", arr);
            return auth;
        }
        if (AUTH_TYPE_BEARER.equals(authType)) {
            auth.put("type", "bearer");
            JSONArray arr = new JSONArray();
            arr.add(new JSONObject().put("key", "token").put("value", token));
            auth.put("bearer", arr);
            return auth;
        }
        if (AUTH_TYPE_DIGEST.equals(authType)) {
            auth.put("type", "digest");
            JSONArray arr = new JSONArray();
            arr.add(new JSONObject().put("key", "username").put("value", username));
            arr.add(new JSONObject().put("key", "password").put("value", password));
            auth.put("digest", arr);
            return auth;
        }
        return null;
    }

    /**
     * Sẽ SavedResponse Chuyển đổi sang  DevWorkbench Định dạng phản hồi
     */
    private static JSONObject toDevWorkbenchResponse(SavedResponse savedResp) {
        JSONObject respJson = new JSONObject();

        // Thông tin cơ bản
        respJson.put("name", savedResp.getName());
        respJson.put("status", savedResp.getStatus());
        respJson.put("code", savedResp.getCode());

        if (savedResp.getPreviewLanguage() != null && !savedResp.getPreviewLanguage().isEmpty()) {
            respJson.put("_postman_previewlanguage", savedResp.getPreviewLanguage());
        }

        // tiêu đề phản hồi
        if (savedResp.getHeaders() != null && !savedResp.getHeaders().isEmpty()) {
            JSONArray headersArray = new JSONArray();
            for (HttpHeader header : savedResp.getHeaders()) {
                JSONObject headerObj = new JSONObject();
                headerObj.put("key", header.getKey());
                headerObj.put("value", header.getValue());
                headersArray.add(headerObj);
            }
            respJson.put("header", headersArray);
        }

        // Cookies (Nếu vậy)
        if (savedResp.getCookies() != null && !savedResp.getCookies().isEmpty()) {
            JSONArray cookiesArray = new JSONArray();
            // DevWorkbench cookie Định dạng có thể được thêm vào đây
            respJson.put("cookie", cookiesArray);
        }

        // cơ quan phản ứng
        if (savedResp.getBody() != null) {
            respJson.put("body", savedResp.getBody());
        }

        // Thông tin yêu cầu ban đầu
        if (savedResp.getOriginalRequest() != null) {
            SavedResponse.OriginalRequest origReq = savedResp.getOriginalRequest();
            JSONObject originalRequest = new JSONObject();

            originalRequest.put("method", origReq.getMethod());

            // URL
            JSONObject url = new JSONObject();
            url.put("raw", origReq.getUrl());

            // phân tích URL các bộ phận khác nhau của
            try {
                java.net.URI uri = new java.net.URI(origReq.getUrl());
                if (uri.getScheme() != null) {
                    url.put("protocol", uri.getScheme());
                }
                if (uri.getHost() != null) {
                    String[] hostParts = uri.getHost().split("\\.");
                    url.put("host", Arrays.asList(hostParts));
                }
                if (uri.getPath() != null && !uri.getPath().isEmpty()) {
                    String[] pathParts = uri.getPath().replaceFirst("^/", "").split("/");
                    url.put("path", Arrays.asList(pathParts));
                }
            } catch (Exception ignore) {
            }

            // Query các thông số
            if (origReq.getParams() != null && !origReq.getParams().isEmpty()) {
                JSONArray queryArray = new JSONArray();
                for (HttpParam param : origReq.getParams()) {
                    JSONObject paramObj = new JSONObject();
                    paramObj.put("key", param.getKey());
                    paramObj.put("value", param.getValue());
                    if (!param.isEnabled()) {
                        paramObj.put("disabled", true);
                    }
                    queryArray.add(paramObj);
                }
                url.put("query", queryArray);
            }
            addPathVariables(url, origReq.getPathVariables());

            originalRequest.put("url", url);

            // Tiêu đề yêu cầu
            if (origReq.getHeaders() != null && !origReq.getHeaders().isEmpty()) {
                JSONArray headersArray = new JSONArray();
                for (HttpHeader header : origReq.getHeaders()) {
                    JSONObject headerObj = new JSONObject();
                    headerObj.put("key", header.getKey());
                    headerObj.put("value", header.getValue());
                    if (!header.isEnabled()) {
                        headerObj.put("disabled", true);
                    }
                    headersArray.add(headerObj);
                }
                originalRequest.put("header", headersArray);
            }

            // Request body
            if (origReq.getBodyType() != null && !origReq.getBodyType().isEmpty()) {
                JSONObject body = new JSONObject();
                String bodyType = origReq.getBodyType();
                body.put("mode", BODY_TYPE_BINARY.equals(bodyType) ? "file" : bodyType);

                if (BODY_TYPE_BINARY.equals(bodyType) && origReq.getBody() != null) {
                    JSONObject file = new JSONObject();
                    file.put("src", origReq.getBody());
                    body.put("file", file);
                } else if (BODY_TYPE_RAW.equals(bodyType) && origReq.getBody() != null) {
                    body.put("raw", origReq.getBody());
                } else if (BODY_TYPE_FORM_DATA.equals(bodyType) && origReq.getFormDataList() != null) {
                    JSONArray formDataArray = new JSONArray();
                    for (HttpFormData formData : origReq.getFormDataList()) {
                        JSONObject formDataObj = new JSONObject();
                        formDataObj.put("key", formData.getKey());
                        if (formData.isText()) {
                            formDataObj.put("value", formData.getValue());
                            formDataObj.put("type", "text");
                        } else if (formData.isFile()) {
                            formDataObj.put("src", formData.getValue());
                            formDataObj.put("type", "file");
                        }
                        if (!formData.isEnabled()) {
                            formDataObj.put("disabled", true);
                        }
                        formDataArray.add(formDataObj);
                    }
                    body.put("formdata", formDataArray);
                } else if (BODY_TYPE_FORM_URLENCODED.equals(bodyType) && origReq.getUrlencodedList() != null) {
                    JSONArray urlencodedArray = new JSONArray();
                    for (HttpFormUrlencoded encoded : origReq.getUrlencodedList()) {
                        JSONObject encodedObj = new JSONObject();
                        encodedObj.put("key", encoded.getKey());
                        encodedObj.put("value", encoded.getValue());
                        if (!encoded.isEnabled()) {
                            encodedObj.put("disabled", true);
                        }
                        urlencodedArray.add(encodedObj);
                    }
                    body.put("urlencoded", urlencodedArray);
                }

                originalRequest.put("body", body);
            }

            respJson.put("originalRequest", originalRequest);
        }

        return respJson;
    }

    private static void addPathVariables(JSONObject url, List<HttpParam> pathVariables) {
        if (pathVariables == null || pathVariables.isEmpty()) {
            return;
        }

        JSONArray variableArray = new JSONArray();
        for (HttpParam pathVariable : pathVariables) {
            if (pathVariable == null || pathVariable.getKey() == null || pathVariable.getKey().isBlank()) {
                continue;
            }
            JSONObject variable = new JSONObject();
            variable.put("key", pathVariable.getKey());
            variable.put("value", pathVariable.getValue() == null ? "" : pathVariable.getValue());
            putDescription(variable, pathVariable.getDescription());
            if (!pathVariable.isEnabled()) {
                variable.put("disabled", true);
            }
            variableArray.add(variable);
        }
        if (!variableArray.isEmpty()) {
            url.put("variable", variableArray);
        }
    }
}
