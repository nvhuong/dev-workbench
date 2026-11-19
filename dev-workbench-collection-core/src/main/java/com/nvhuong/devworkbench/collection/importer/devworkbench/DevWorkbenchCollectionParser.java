package com.nvhuong.devworkbench.collection.importer.devworkbench;

import com.nvhuong.devworkbench.collection.importer.AuthParserUtil;
import com.nvhuong.devworkbench.collection.model.CollectionNode;
import com.nvhuong.devworkbench.collection.model.CollectionNodeType;
import com.nvhuong.devworkbench.collection.model.CollectionParseResult;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.model.Variable;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;
import com.nvhuong.devworkbench.request.model.SavedResponse;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_BASIC;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_API_KEY;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_BEARER;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_DIGEST;
import static com.nvhuong.devworkbench.request.model.RequestAuthTypes.AUTH_TYPE_NONE;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_BINARY;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_FORM_DATA;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_FORM_URLENCODED;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_RAW;

/**
 * DevWorkbench Collectiontrình phân tích cú pháp
 * Chịu trách nhiệm phân tíchDevWorkbench CollectionYêu cầu định dạng (v2.1định dạng)
 * Lớp này chỉ chịu trách nhiệm phân tích cú pháp và không liên quan UI lớp TreeNode Lắp ráp
 */
@Slf4j
@UtilityClass
public class DevWorkbenchCollectionParser {

    // Định nghĩa hằng số
    private static final String KEY_EVENT = "event";
    private static final String KEY_DISABLED = "disabled";
    private static final String KEY_VALUE = "value";
    private static final String KEY_HEADER = "header";
    private static final String MODE_FORMDATA = "formdata";
    private static final String MODE_URLENCODED = "urlencoded";


    /**
     * Phân tích hoàn tất DevWorkbench Collection JSON，Trả về kết quả phân tích cú pháp
     *
     * @param json DevWorkbench Collection JSON chuỗi
     * @return Kết quả phân tích cú pháp, được trả về nếu phân tích cú pháp thất bại null
     */
    public static CollectionParseResult parseDevWorkbenchCollection(String json) {
        try {
            JSONObject devworkbenchRoot = JSONUtil.parseObj(json);
            if (devworkbenchRoot.containsKey("info") && devworkbenchRoot.containsKey("item")) {
                // phân tích collection Tên và mô tả
                JSONObject info = devworkbenchRoot.getJSONObject("info");
                String collectionName = info.getStr("name", "Postman");
                RequestGroup collectionGroup = new RequestGroup(collectionName);

                // Phân tích mô tả bộ sưu tập
                String description = extractDescription(info.get("description"));
                if (description != null && !description.isEmpty()) {
                    collectionGroup.setDescription(description);
                }

                // Phân tích xác thực cấp độ bộ sưu tập
                if (devworkbenchRoot.containsKey("auth")) {
                    AuthParserUtil.parseDevWorkbenchAuthToGroup(devworkbenchRoot.getJSONObject("auth"), collectionGroup);
                }

                // Tập lệnh phân tích cấp độ bộ sưu tập
                if (devworkbenchRoot.containsKey(KEY_EVENT)) {
                    parseEventsToGroup(devworkbenchRoot.getJSONArray(KEY_EVENT), collectionGroup);
                }

                // Phân tích các biến cấp độ bộ sưu tập
                if (devworkbenchRoot.containsKey("variable")) {
                    collectionGroup.setVariables(parseVariables(devworkbenchRoot.getJSONArray("variable")));
                }

                JSONArray items = devworkbenchRoot.getJSONArray("item");
                CollectionParseResult result = new CollectionParseResult(collectionGroup);

                // Cấu trúc cây phân tích đệ quy
                List<CollectionNode> children = parseDevWorkbenchItems(items);
                for (CollectionNode child : children) {
                    result.addChild(child);
                }
                return result;
            }
        } catch (Exception e) {
            // trở lại null Cho biết phân tích cú pháp thất bại
            log.error("phân tíchDevWorkbench Collectionthất bại", e);
            return null;
        }
        return null;
    }

    /**
     * Phân tích đệ quyDevWorkbenchBộ sưu tập, trả về danh sách các nút
     *
     * @param items DevWorkbench collection của item mảng
     * @return Danh sách nút
     */
    private static List<CollectionNode> parseDevWorkbenchItems(JSONArray items) {
        List<CollectionNode> nodeList = new ArrayList<>();
        for (Object obj : items) {
            JSONObject item = (JSONObject) obj;

            // Xác định xem đó là một thư mục hay một yêu cầu
            // Thư mục: Có item lĩnh vực
            // Yêu cầu: Có request lĩnh vực
            if (item.containsKey("item")) {
                // Nút thư mục
                String folderName = item.getStr("name", "default group");
                RequestGroup group = new RequestGroup(folderName);

                // Phân tích mô tả thư mục
                String description = extractDescription(item.get("description"));
                if (description != null && !description.isEmpty()) {
                    group.setDescription(description);
                }

                if (item.containsKey("variable")) {
                    group.setVariables(parseVariables(item.getJSONArray("variable")));
                }

                // Phân tích xác thực cấp nhóm
                JSONObject auth = item.getJSONObject("auth");
                if (auth != null) {
                    AuthParserUtil.parseDevWorkbenchAuthToGroup(auth, group);
                }

                // Tập lệnh để phân tích cấp độ nhóm
                JSONArray events = item.getJSONArray("event");
                if (events != null && !events.isEmpty()) {
                    parseEventsToGroup(events, group);
                }

                CollectionNode folderNode = new CollectionNode(CollectionNodeType.GROUP, group);

                // Xử lý các nút con theo cách đệ quy
                JSONArray children = item.getJSONArray("item");
                List<CollectionNode> childNodes = parseDevWorkbenchItems(children);
                for (CollectionNode child : childNodes) {
                    folderNode.addChild(child);
                }
                nodeList.add(folderNode);
            } else if (item.containsKey("request")) {
                // Nút yêu cầu thuần túy
                HttpRequestItem req = parseDevWorkbenchSingleItem(item);
                CollectionNode requestNode = new CollectionNode(CollectionNodeType.REQUEST, req);

                nodeList.add(requestNode);
            }
        }
        return nodeList;
    }


    /**
     * phân tíchDevWorkbenchcủaeventsĐếnRequestGroup
     */
    private static void parseEventsToGroup(JSONArray events, RequestGroup group) {
        if (events == null || events.isEmpty()) {
            return;
        }

        ScriptPair scripts = parseEventScripts(events);
        if (scripts.preScript != null) {
            group.setPrescript(scripts.preScript);
        }
        if (scripts.postScript != null) {
            group.setPostscript(scripts.postScript);
        }
    }

    /**
     * TừeventsPhân tích các tập lệnh trước và sau trong mảng
     */
    private static ScriptPair parseEventScripts(JSONArray events) {
        StringBuilder testScript = new StringBuilder();
        StringBuilder preScript = new StringBuilder();

        for (Object e : events) {
            JSONObject eObj = (JSONObject) e;
            String listen = eObj.getStr("listen");
            JSONObject script = eObj.getJSONObject("script");
            if (script != null) {
                Object execObj = script.get("exec");
                if (execObj instanceof JSONArray exec) {
                    for (Object line : exec) {
                        if ("test".equals(listen)) {
                            testScript.append(line).append("\n");
                        } else if ("prerequest".equals(listen)) {
                            preScript.append(line).append("\n");
                        }
                    }
                } else if (execObj instanceof String exec) {
                    if ("test".equals(listen)) {
                        testScript.append(exec).append("\n");
                    } else if ("prerequest".equals(listen)) {
                        preScript.append(exec).append("\n");
                    }
                }
            }
        }

        return new ScriptPair(
                preScript.isEmpty() ? null : preScript.toString(),
                testScript.isEmpty() ? null : testScript.toString()
        );
    }

    /**
     * Cặp tập lệnh, chứa tập lệnh trước và sau
     */
    private static class ScriptPair {
        final String preScript;
        final String postScript;

        ScriptPair(String preScript, String postScript) {
            this.preScript = preScript;
            this.postScript = postScript;
        }
    }

    /**
     * phân tíchDevWorkbenchcủaqueryMảng tham số
     */
    private static List<HttpParam> parseQueryParams(JSONArray queryArr) {
        if (queryArr == null || queryArr.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpParam> paramsList = new ArrayList<>();
        for (Object q : queryArr) {
            JSONObject qObj = (JSONObject) q;
            boolean enabled = !qObj.getBool(KEY_DISABLED, false);
            paramsList.add(new HttpParam(
                    enabled,
                    qObj.getStr("key", ""),
                    qObj.getStr(KEY_VALUE, ""),
                    qObj.getStr("description", "")
            ));
        }
        return paramsList;
    }

    private static List<HttpParam> parseUrlVariables(JSONArray variableArr) {
        if (variableArr == null || variableArr.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpParam> pathVariables = new ArrayList<>();
        for (Object variable : variableArr) {
            if (!(variable instanceof JSONObject variableObj)) {
                continue;
            }
            String key = variableObj.getStr("key", "");
            if (key.isBlank()) {
                continue;
            }
            boolean enabled = !variableObj.getBool(KEY_DISABLED, false);
            pathVariables.add(new HttpParam(
                    enabled,
                    key,
                    variableObj.getStr(KEY_VALUE, ""),
                    variableObj.getStr("description", "")
            ));
        }
        return pathVariables;
    }

    /**
     * phân tíchDevWorkbenchcủaheadermảng
     */
    private static List<HttpHeader> parseHeaders(JSONArray headers) {
        if (headers == null || headers.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpHeader> headersList = new ArrayList<>();
        for (Object h : headers) {
            JSONObject hObj = (JSONObject) h;
            boolean enabled = !hObj.getBool(KEY_DISABLED, false);
            headersList.add(new HttpHeader(
                    enabled,
                    hObj.getStr("key", ""),
                    hObj.getStr(KEY_VALUE, ""),
                    hObj.getStr("description", "")
            ));
        }
        return headersList;
    }

    /**
     * phân tích DevWorkbench của header Chuỗi thô
     */
    private static List<HttpHeader> parseHeaders(String rawHeaders) {
        if (rawHeaders == null || rawHeaders.isBlank()) {
            return new ArrayList<>();
        }

        List<HttpHeader> headersList = new ArrayList<>();
        String[] lines = rawHeaders.split("\\r?\\n");
        for (String line : lines) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = line.substring(0, separatorIndex).trim();
            String value = line.substring(separatorIndex + 1).trim();
            if (!key.isEmpty()) {
                headersList.add(new HttpHeader(true, key, value));
            }
        }
        return headersList;
    }

    /**
     * phân tíchDevWorkbenchcủavariablemảng
     */
    private static List<Variable> parseVariables(JSONArray variables) {
        if (variables == null || variables.isEmpty()) {
            return new ArrayList<>();
        }

        List<Variable> variableList = new ArrayList<>();
        for (Object v : variables) {
            JSONObject vObj = (JSONObject) v;
            boolean enabled = !vObj.getBool(KEY_DISABLED, false);
            String key = vObj.getStr("key", "");
            String value = vObj.getStr(KEY_VALUE, "");
            variableList.add(new Variable(enabled, key, value));
        }
        return variableList;
    }

    /**
     * phân tíchDevWorkbenchcủaformdatamảng
     */
    private static List<HttpFormData> parseFormData(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpFormData> formDataList = new ArrayList<>();
        for (Object o : arr) {
            JSONObject oObj = (JSONObject) o;
            String formType = oObj.getStr("type", "text");
            String key = oObj.getStr("key", "");
            boolean enabled = !oObj.getBool(KEY_DISABLED, false);
            String description = oObj.getStr("description", "");
            if ("file".equals(formType)) {
                Object srcObj = oObj.get("src");
                if (srcObj instanceof JSONArray srcArray && !srcArray.isEmpty()) {
                    for (Object src : srcArray) {
                        formDataList.add(new HttpFormData(
                                enabled,
                                key,
                                HttpFormData.TYPE_FILE,
                                src == null ? "" : String.valueOf(src),
                                description
                        ));
                    }
                } else {
                    String src = srcObj == null ? "" : String.valueOf(srcObj);
                    formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_FILE, src, description));
                }
            } else {
                formDataList.add(new HttpFormData(
                        enabled,
                        key,
                        HttpFormData.TYPE_TEXT,
                        oObj.getStr(KEY_VALUE, ""),
                        description
                ));
            }
        }
        return formDataList;
    }

    /**
     * phân tíchDevWorkbenchcủaurlencodedmảng
     */
    private static List<HttpFormUrlencoded> parseUrlencoded(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return new ArrayList<>();
        }

        List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
        for (Object o : arr) {
            JSONObject oObj = (JSONObject) o;
            boolean enabled = !oObj.getBool(KEY_DISABLED, false);
            urlencodedList.add(new HttpFormUrlencoded(
                    enabled,
                    oObj.getStr("key", ""),
                    oObj.getStr(KEY_VALUE, ""),
                    oObj.getStr("description", "")
            ));
        }
        return urlencodedList;
    }

    /**
     * Phân tích một đơnDevWorkbenchyêu cầuitemvìHttpRequestItem
     */
    private static HttpRequestItem parseDevWorkbenchSingleItem(JSONObject item) {
        HttpRequestItem req = new HttpRequestItem();
        req.setId(UUID.randomUUID().toString());
        req.setName(item.getStr("name", "Yêu cầu không tên"));
        Object requestObj = item.get("request");
        if (requestObj instanceof String requestUrl) {
            req.setMethod("GET");
            req.setUrl(requestUrl);
        } else if (requestObj instanceof JSONObject request) {
            // Phân tích mô tả yêu cầu
            String description = extractDescription(request.get("description"));
            if (description != null && !description.isEmpty()) {
                req.setDescription(description);
            }

            req.setMethod(request.getStr("method", "GET"));
            // url
            Object urlObj = request.get("url");
            if (urlObj instanceof JSONObject urlJson) {
                req.setUrl(buildRawUrl(urlJson));
                List<HttpParam> pathVariables = parseUrlVariables(urlJson.getJSONArray("variable"));
                if (!pathVariables.isEmpty()) {
                    req.setPathVariablesList(pathVariables);
                }
                // phân tíchquerycác thông số
                JSONArray queryArr = urlJson.getJSONArray("query");
                List<HttpParam> params = parseQueryParams(queryArr);
                if (!params.isEmpty()) {
                    req.setParamsList(params);
                }
            } else if (urlObj instanceof String urlStr) {
                req.setUrl(urlStr);
            }
            // headers
            List<HttpHeader> headersList = new ArrayList<>();
            Object headersObj = request.get(KEY_HEADER);
            if (headersObj instanceof JSONArray headers) {
                headersList = parseHeaders(headers);
            } else if (headersObj instanceof String rawHeaders) {
                headersList = parseHeaders(rawHeaders);
            }
            if (headersList != null && !headersList.isEmpty()) {
                req.setHeadersList(headersList);
            }
            // auth
            JSONObject auth = request.getJSONObject("auth");
            if (auth != null) {
                String authType = auth.getStr("type", "");
                if ("basic".equals(authType)) {
                    req.setAuthType(AUTH_TYPE_BASIC);
                    JSONArray basicArr = auth.getJSONArray("basic");
                    String username = null, password = null;
                    if (basicArr != null) {
                        for (Object o : basicArr) {
                            JSONObject oObj = (JSONObject) o;
                            if ("username".equals(oObj.getStr("key"))) username = oObj.getStr("value", "");
                            if ("password".equals(oObj.getStr("key"))) password = oObj.getStr("value", "");
                        }
                    }
                    req.setAuthUsername(username);
                    req.setAuthPassword(password);
                } else if ("apikey".equals(authType)) {
                    req.setAuthType(AUTH_TYPE_API_KEY);
                    AuthParserUtil.applyDevWorkbenchApiKeyAuth(
                            auth.getJSONArray("apikey"),
                            req::setAuthApiKeyName,
                            req::setAuthApiKeyValue,
                            req::setAuthApiKeyPlacement
                    );
                } else if ("bearer".equals(authType)) {
                    req.setAuthType(AUTH_TYPE_BEARER);
                    JSONArray bearerArr = auth.getJSONArray("bearer");
                    if (bearerArr != null && !bearerArr.isEmpty()) {
                        for (Object o : bearerArr) {
                            JSONObject oObj = (JSONObject) o;
                            if ("token".equals(oObj.getStr("key"))) {
                                req.setAuthToken(oObj.getStr("value", ""));
                            }
                        }
                    }
                } else if ("digest".equals(authType)) {
                    req.setAuthType(AUTH_TYPE_DIGEST);
                    JSONArray digestArr = auth.getJSONArray("digest");
                    if (digestArr != null) {
                        for (Object o : digestArr) {
                            JSONObject oObj = (JSONObject) o;
                            if ("username".equals(oObj.getStr("key"))) {
                                req.setAuthUsername(oObj.getStr("value", ""));
                            }
                            if ("password".equals(oObj.getStr("key"))) {
                                req.setAuthPassword(oObj.getStr("value", ""));
                            }
                        }
                    }
                } else if ("noauth".equals(authType)) {
                    req.setAuthType(AUTH_TYPE_NONE);
                } else {
                    req.setAuthType(AUTH_TYPE_NONE);
                }
            }
            // body
            JSONObject body = request.getJSONObject("body");
            if (body != null) {
                String mode = body.getStr("mode", "");
                if ("raw".equals(mode)) {
                    req.setBodyType(BODY_TYPE_RAW);
                    req.setBody(body.getStr("raw", ""));
                } else if (MODE_FORMDATA.equals(mode)) {
                    req.setBodyType(BODY_TYPE_FORM_DATA);
                    List<HttpFormData> formDataList = parseFormData(body.getJSONArray(MODE_FORMDATA));
                    if (!formDataList.isEmpty()) {
                        req.setFormDataList(formDataList);
                    }
                } else if (MODE_URLENCODED.equals(mode)) {
                    req.setBodyType(BODY_TYPE_FORM_URLENCODED);
                    List<HttpFormUrlencoded> urlencodedList = parseUrlencoded(body.getJSONArray(MODE_URLENCODED));
                    if (!urlencodedList.isEmpty()) {
                        req.setUrlencodedList(urlencodedList);
                    }
                } else if ("graphql".equals(mode)) {
                    req.setBodyType(BODY_TYPE_RAW);
                    JSONObject graphql = body.getJSONObject("graphql");
                    if (graphql != null) {
                        req.setBody(graphql.toString());
                    }
                } else if ("file".equals(mode)) {
                    req.setBodyType(BODY_TYPE_BINARY);
                    JSONObject file = body.getJSONObject("file");
                    if (file != null) {
                        req.setBody(file.getStr("src", ""));
                    }
                }
            }
        }
        // event（Chẳng hạn nhưtestkịch bản,pre-requestkịch bản)
        JSONArray events = item.getJSONArray("event");
        if (events != null && !events.isEmpty()) {
            ScriptPair scripts = parseEventScripts(events);
            if (scripts.preScript != null) {
                req.setPrescript(scripts.preScript);
            }
            if (scripts.postScript != null) {
                req.setPostscript(scripts.postScript);
            }
        }

        // phân tích response（DevWorkbench của Examples/Saved Responses）
        JSONArray responses = item.getJSONArray("response");
        if (responses != null && !responses.isEmpty()) {
            List<SavedResponse> savedResponsesList = new ArrayList<>();
            for (Object respObj : responses) {
                JSONObject respJson = (JSONObject) respObj;
                SavedResponse savedResponse = parseDevWorkbenchResponse(respJson);
                if (savedResponse != null) {
                    savedResponsesList.add(savedResponse);
                }
            }
            req.setResponse(savedResponsesList);
        }

        return req;
    }

    private static String extractDescription(Object descriptionObj) {
        if (descriptionObj instanceof String description) {
            return description;
        }
        if (descriptionObj instanceof JSONObject description) {
            String content = description.getStr("content", "");
            if (!content.isEmpty()) {
                return content;
            }
        }
        return null;
    }

    private static String buildRawUrl(JSONObject urlJson) {
        String raw = urlJson.getStr("raw", "");
        if (!raw.isBlank()) {
            return raw;
        }

        StringBuilder url = new StringBuilder();
        String protocol = urlJson.getStr("protocol", "");
        if (!protocol.isBlank()) {
            url.append(protocol).append("://");
        }

        Object hostObj = urlJson.get("host");
        if (hostObj instanceof JSONArray hostArray) {
            for (int i = 0; i < hostArray.size(); i++) {
                if (i > 0) {
                    url.append(".");
                }
                url.append(hostArray.getStr(i));
            }
        } else if (hostObj instanceof String host) {
            url.append(host);
        }

        String port = urlJson.getStr("port", "");
        if (!port.isBlank()) {
            url.append(":").append(port);
        }

        Object pathObj = urlJson.get("path");
        if (pathObj instanceof JSONArray pathArray) {
            for (Object segment : pathArray) {
                url.append("/");
                if (segment instanceof JSONObject pathVar) {
                    String key = pathVar.getStr("key", "");
                    String type = pathVar.getStr("type", "");
                    if ("param".equals(type) && !key.isBlank()) {
                        url.append(":").append(key);
                    } else {
                        url.append(pathVar.getStr("value", ""));
                    }
                } else {
                    url.append(String.valueOf(segment));
                }
            }
        } else if (pathObj instanceof String path) {
            if (!path.startsWith("/")) {
                url.append("/");
            }
            url.append(path);
        }

        JSONArray query = urlJson.getJSONArray("query");
        if (query != null && !query.isEmpty()) {
            boolean first = true;
            for (Object q : query) {
                JSONObject qObj = (JSONObject) q;
                if (qObj.getBool(KEY_DISABLED, false)) {
                    continue;
                }
                url.append(first ? "?" : "&");
                first = false;
                url.append(qObj.getStr("key", ""));
                if (qObj.containsKey(KEY_VALUE)) {
                    url.append("=").append(qObj.getStr(KEY_VALUE, ""));
                }
            }
        }

        return url.toString();
    }

    /**
     * phân tích DevWorkbench Một phản hồi duy nhất (Example/Saved Response）
     */
    private static SavedResponse parseDevWorkbenchResponse(JSONObject respJson) {
        SavedResponse savedResponse = new SavedResponse();

        // Thông tin cơ bản
        savedResponse.setId(UUID.randomUUID().toString());
        savedResponse.setName(respJson.getStr("name", "Response"));
        savedResponse.setTimestamp(System.currentTimeMillis());

        // thông tin trạng thái
        savedResponse.setCode(respJson.getInt("code", 200));
        savedResponse.setStatus(respJson.getStr("status", "OK"));
        savedResponse.setPreviewLanguage(respJson.getStr("_postman_previewlanguage",
                respJson.getStr("_devworkbench_previewlanguage", "text")));

        // tiêu đề phản hồi
        JSONArray headers = respJson.getJSONArray(KEY_HEADER);
        List<HttpHeader> headersList = parseHeaders(headers);
        if (!headersList.isEmpty()) {
            savedResponse.setHeaders(headersList);
        }

        // cơ quan phản ứng
        savedResponse.setBody(respJson.getStr("body", ""));

        // Thông tin yêu cầu ban đầu
        JSONObject originalRequest = respJson.getJSONObject("originalRequest");
        if (originalRequest != null) {
            SavedResponse.OriginalRequest origReq = new SavedResponse.OriginalRequest();
            origReq.setMethod(originalRequest.getStr("method", "GET"));

            // phân tích URL
            Object urlObj = originalRequest.get("url");
            if (urlObj instanceof JSONObject urlJson) {
                origReq.setUrl(urlJson.getStr("raw", ""));

                List<HttpParam> pathVariables = parseUrlVariables(urlJson.getJSONArray("variable"));
                if (!pathVariables.isEmpty()) {
                    origReq.setPathVariables(pathVariables);
                }

                // phân tích query các thông số
                List<HttpParam> params = parseQueryParams(urlJson.getJSONArray("query"));
                if (!params.isEmpty()) {
                    origReq.setParams(params);
                }
            } else if (urlObj instanceof String urlStr) {
                origReq.setUrl(urlStr);
            }

            // Phân tích tiêu đề yêu cầu
            List<HttpHeader> reqHeaders = parseHeaders(originalRequest.getJSONArray(KEY_HEADER));
            if (!reqHeaders.isEmpty()) {
                origReq.setHeaders(reqHeaders);
            }

            // Phân tích nội dung yêu cầu
            JSONObject body = originalRequest.getJSONObject("body");
            if (body != null) {
                String mode = body.getStr("mode", "");
                origReq.setBodyType("file".equals(mode) ? BODY_TYPE_BINARY : mode);

                if ("raw".equals(mode)) {
                    origReq.setBody(body.getStr("raw", ""));
                } else if ("file".equals(mode)) {
                    JSONObject file = body.getJSONObject("file");
                    if (file != null) {
                        origReq.setBody(file.getStr("src", ""));
                    }
                } else if (MODE_FORMDATA.equals(mode)) {
                    List<HttpFormData> formDataList = parseFormData(body.getJSONArray(MODE_FORMDATA));
                    if (!formDataList.isEmpty()) {
                        origReq.setFormDataList(formDataList);
                    }
                } else if (MODE_URLENCODED.equals(mode)) {
                    List<HttpFormUrlencoded> urlencodedList = parseUrlencoded(body.getJSONArray(MODE_URLENCODED));
                    if (!urlencodedList.isEmpty()) {
                        origReq.setUrlencodedList(urlencodedList);
                    }
                }
            }

            savedResponse.setOriginalRequest(origReq);
        }

        return savedResponse;
    }
}
