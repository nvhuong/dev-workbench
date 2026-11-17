package com.laker.postman.service.har;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpRequestItem;

import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.service.collections.CollectionTreeNodes;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.laker.postman.request.model.RequestAuthTypes.AUTH_TYPE_BASIC;
import static com.laker.postman.request.model.RequestAuthTypes.AUTH_TYPE_BEARER;
import static com.laker.postman.request.model.RequestAuthTypes.AUTH_TYPE_NONE;
import static com.laker.postman.request.model.RequestBodyTypes.BODY_TYPE_FORM_DATA;
import static com.laker.postman.request.model.RequestBodyTypes.BODY_TYPE_FORM_URLENCODED;
import static com.laker.postman.request.model.RequestBodyTypes.BODY_TYPE_RAW;

/**
 * HAR (HTTP Archive) Trình phân tích cú pháp định dạng
 * Chịu trách nhiệm phân tích HAR 1.2 tập tin định dạng, được chuyển đổi thành cấu trúc dữ liệu nội bộ
 */
@Slf4j
public class HarParser {
    /**
     * Nhà xây dựng riêng để ngăn chặn việc khởi tạo
     */
    private HarParser() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * phân tích HAR JSON tập tin, trả về nút gốc
     *
     * @param json HAR JSON chuỗi
     * @return Nút gốc của bộ sưu tập, được trả về nếu phân tích cú pháp không thành công null
     */
    public static DefaultMutableTreeNode parseHar(String json) {
        try {
            JSONObject harRoot = JSONUtil.parseObj(json);
            if (!harRoot.containsKey("log")) {
                log.error("HARLỗi định dạng tệp: thiếu log lĩnh vực");
                return null;
            }

            JSONObject logObj = harRoot.getJSONObject("log");
            String version = logObj.getStr("version", "");
            if (!"1.2".equals(version)) {
                log.warn("HARphiên bản thì không1.2，Phiên bản hiện tại: {}", version);
            }

            // Tạo các nút nhóm
            String groupName = "HAR Import " + System.currentTimeMillis();
            RequestGroup collectionGroup = new RequestGroup(groupName);
            DefaultMutableTreeNode collectionNode = CollectionTreeNodes.groupNode(collectionGroup);

            // phân tích entries
            JSONArray entries = logObj.getJSONArray("entries");
            if (entries == null || entries.isEmpty()) {
                log.warn("HARKhông có mục yêu cầu trong tập tin");
                return collectionNode;
            }

            // Lặp lại qua tất cả các mục
            for (Object entryObj : entries) {
                JSONObject entry = (JSONObject) entryObj;
                HttpRequestItem requestItem = parseHarEntry(entry);
                if (requestItem != null) {
                    collectionNode.add(CollectionTreeNodes.requestNode(requestItem));
                }
            }

            return collectionNode;
        } catch (Exception e) {
            log.error("phân tíchHARTệp không thành công", e);
            return null;
        }
    }

    /**
     * Phân tích một đơn HAR entry vì HttpRequestItem
     */
    private static HttpRequestItem parseHarEntry(JSONObject entry) {
        try {
            HttpRequestItem req = new HttpRequestItem();
            req.setId(UUID.randomUUID().toString());

            JSONObject request = entry.getJSONObject("request");
            if (request == null) {
                log.warn("HAR entry thiếu request lĩnh vực");
                return null;
            }

            // phương pháp
            req.setMethod(request.getStr("method", "GET"));

            // URL
            String url = request.getStr("url", "");
            if (url.isEmpty()) {
                log.warn("HAR entry của URL trống rỗng");
                return null;
            }
            req.setUrl(url);

            // Tên yêu cầu (từ comment hoặc URL khai thác)
            String comment = entry.getStr("comment", "");
            if (!comment.isEmpty()) {
                req.setName(comment);
            } else {
                // Từ URL Trích xuất tên
                try {
                    URI uri = new URI(url);
                    String path = uri.getPath();
                    if (path != null && !path.isEmpty()) {
                        String[] parts = path.split("/");
                        String name = parts[parts.length - 1];
                        if (name.isEmpty() && parts.length > 1) {
                            name = parts[parts.length - 2];
                        }
                        req.setName(name.isEmpty() ? url : name);
                    } else {
                        req.setName(url);
                    }
                } catch (Exception e) {
                    req.setName(url);
                }
            }

            // Headers
            JSONArray headers = request.getJSONArray("headers");
            if (headers != null && !headers.isEmpty()) {
                List<HttpHeader> headersList = new ArrayList<>();
                for (Object h : headers) {
                    JSONObject hObj = (JSONObject) h;
                    String name = hObj.getStr("name", "");
                    String value = hObj.getStr("value", "");
                    // Kiểm tra xem đó có phải là tiêu đề xác thực hay không
                    if ("Authorization".equalsIgnoreCase(name)) {
                        if (value.startsWith("Basic ")) {
                            req.setAuthType(AUTH_TYPE_BASIC);
                            Pair<String, String> basicAuth = parseBasicAuth(value);
                            if (basicAuth != null) {
                                req.setAuthUsername(basicAuth.getKey());
                                req.setAuthPassword(basicAuth.getValue());
                            }
                        } else if (value.startsWith("Bearer ")) {
                            req.setAuthType(AUTH_TYPE_BEARER);
                            req.setAuthToken(value.substring(7));
                        } else {
                            req.setAuthType(AUTH_TYPE_NONE);
                            headersList.add(new HttpHeader(true, name, value));
                        }
                    } else {
                        headersList.add(new HttpHeader(true, name, value));
                    }
                }
                req.setHeadersList(headersList);
            }

            // Query String
            JSONArray queryString = request.getJSONArray("queryString");
            if (queryString != null && !queryString.isEmpty()) {
                List<HttpParam> paramsList = new ArrayList<>();
                for (Object q : queryString) {
                    JSONObject qObj = (JSONObject) q;
                    String name = qObj.getStr("name", "");
                    String value = qObj.getStr("value", "");
                    paramsList.add(new HttpParam(true, name, value));
                }
                req.setParamsList(paramsList);
            }

            // Post Data
            JSONObject postData = request.getJSONObject("postData");
            if (postData != null) {
                String mimeType = postData.getStr("mimeType", "");
                String text = postData.getStr("text", "");

                if (mimeType.contains("application/json")) {
                    req.setBody(text);
                    req.setBodyType(BODY_TYPE_RAW);
                } else if (mimeType.contains("application/x-www-form-urlencoded")) {
                    // phân tích urlencoded dữ liệu
                    List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
                    JSONArray params = postData.getJSONArray("params");
                    if (params != null && !params.isEmpty()) {
                        for (Object p : params) {
                            JSONObject pObj = (JSONObject) p;
                            String name = pObj.getStr("name", "");
                            String value = pObj.getStr("value", "");
                            urlencodedList.add(new HttpFormUrlencoded(true, name, value));
                        }
                    } else if (!text.isEmpty()) {
                        // Nếu không params Mảng, thử từ text phân tích
                        String[] pairs = text.split("&");
                        for (String pair : pairs) {
                            String[] kv = pair.split("=", 2);
                            String name = kv.length > 0 ? URLDecoder.decode(kv[0], StandardCharsets.UTF_8) : "";
                            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                            urlencodedList.add(new HttpFormUrlencoded(true, name, value));
                        }
                    }
                    req.setUrlencodedList(urlencodedList);
                    req.setBodyType(BODY_TYPE_FORM_URLENCODED);
                } else if (mimeType.contains("multipart/form-data")) {
                    // phân tích multipart/form-data
                    List<HttpFormData> formDataList = new ArrayList<>();
                    JSONArray params = postData.getJSONArray("params");
                    if (params != null && !params.isEmpty()) {
                        for (Object p : params) {
                            JSONObject pObj = (JSONObject) p;
                            String name = pObj.getStr("name", "");
                            String fileName = pObj.getStr("fileName", "");
                            String value = pObj.getStr("value", "");
                            if (!fileName.isEmpty()) {
                                formDataList.add(new HttpFormData(true, name, HttpFormData.TYPE_FILE, fileName));
                            } else {
                                formDataList.add(new HttpFormData(true, name, HttpFormData.TYPE_TEXT, value));
                            }
                        }
                    }
                    req.setFormDataList(formDataList);
                    req.setBodyType(BODY_TYPE_FORM_DATA);
                } else if (!text.isEmpty()) {
                    // Các loại khác như raw body
                    req.setBody(text);
                    req.setBodyType(BODY_TYPE_RAW);
                }
            }
            return req;
        } catch (Exception e) {
            log.error("phân tíchHAR entrythất bại", e);
            return null;
        }
    }

    /**
     * phân tích Basic Auth
     *
     * @param value Basic Auth Giá trị, ở định dạng "Basic base64(username:password)"
     * @return Chứa tên người dùng và mật khẩu Pair đối tượng
     */
    private static Pair<String, String> parseBasicAuth(String value) {
        String credentials = value.substring(6).trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(credentials));
        } catch (IllegalArgumentException e) {
            log.error("phân tích Basic auth Tin nhắn không thành công value: {}", value, e);
            return null;
        }
        String[] parts = decoded.split(":", 2);
        if (parts.length == 2) {
            return new Pair<>(parts[0], parts[1]);
        } else {
            log.error("phân tích Basic auth Tin nhắn không thành công value: {}", value);
            return null;
        }
    }
}
