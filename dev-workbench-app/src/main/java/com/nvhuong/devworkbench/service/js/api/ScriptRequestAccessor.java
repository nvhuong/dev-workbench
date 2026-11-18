package com.nvhuong.devworkbench.service.js.api;

import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.RequestBodyTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Trình truy cập yêu cầu tập lệnh (pm.request)
 * <p>
 * This category is JavaScript Kịch bản cung cấp các cặp HTTP Giao diện truy cập được yêu cầu, được mô phỏng DevWorkbench của pm.request đối tượng.
 * Chủ yếu được sử dụng để đọc hoặc sửa đổi các yêu cầu sắp tới trong các tập lệnh trước.
 * </p>
 *
 * <h3>Chức năng chính:</h3>
 * <ul>
 *   <li>Yêu cầu truy cập và sửa đổi URL、phương thức, tiêu đề,Body và các thông tin khác</li>
 *   <li>Tự động sửa đổi các tham số yêu cầu trong tập lệnh trước</li>
 *   <li>Hỗ trợ các hoạt động headers、formData、urlencoded Chờ thu thập</li>
 *   <li>cung cấp JavaScript Giao diện truy cập thân thiện</li>
 * </ul>
 *
 * <h3>Ví dụ sử dụng:</h3>
 * <pre>{@code
 * // Vào PreRequest Script ở giữa
 * console.log("Request URL:", pm.request.url);
 * console.log("Request Method:", pm.request.method);
 *
 * // Thêm tiêu đề yêu cầu
 * pm.request.headers.add({
 *     key: "Authorization",
 *     value: "Bearer " + pm.environment.get("token")
 * });
 *
 * // Ôn lại raw Nội dung yêu cầu (Collection SDK hình thái DevWorkbench phần mở rộng di chuyển)
 * pm.request.body.update(JSON.stringify({userId: 123}));
 *
 * // Thêm dữ liệu biểu mẫu
 * pm.request.body.formdata.add({
 *     key: "username",
 *     value: "john",
 *     type: "text"
 * });
 *
 * // Thêm vào URL Tham số truy vấn
 * pm.request.url.query.add({
 *     key: "timestamp",
 *     value: Date.now()
 * });
 * }</pre>
 *
 * @author nvhuong
 * @see PreparedRequest
 */
public class ScriptRequestAccessor {

    /**
     * Đối tượng yêu cầu ban đầu, chứa tất cả thông tin yêu cầu
     */
    public final PreparedRequest raw;

    /**
     * Yêu cầu trình bao bọc bộ sưu tập tiêu đề, được hỗ trợ JavaScript hoạt động
     */
    public JsListWrapper<HttpHeader> headers;

    /**
     * Dữ liệu biểu mẫu (multipart/form-data）Trình bao bọc bộ sưu tập
     */
    public JsListWrapper<HttpFormData> formData;

    /**
     * URL Mã hóa dữ liệu biểu mẫu (application/x-www-form-urlencoded）Trình bao bọc bộ sưu tập
     */
    public JsListWrapper<HttpFormUrlencoded> urlencoded;

    /**
     * URL Trình bao bọc bộ sưu tập tham số truy vấn
     */
    public JsListWrapper<HttpParam> params;

    /**
     * Yêu cầu một mã định danh duy nhất
     */
    public String id;

    /**
     * yêu cầu URL đối tượng (hỗ trợ pm.request.url.query.all() thăm)
     */
    public UrlWrapper url;

    /**
     * Phương pháp yêu cầu (GET, POST, PUT, DELETE Đợi đã)
     */
    public String method;

    /**
     * DevWorkbench RequestBody đối tượng. được khai báo là Object Đó là để lưu giữ Collection SDK body definition
     * hình thức và hỗ trợ DevWorkbench Tiện ích mở rộng di chuyển: Toàn bộ body definition hoặc gán chuỗi.
     */
    public Object body;

    /**
     * Cho dù đó là multipart yêu cầu
     */
    public boolean isMultipart;

    /**
     * Có nên theo chuyển hướng hay không
     */
    public boolean followRedirects;

    private final ScriptRequestMutationTracker mutationTracker = new ScriptRequestMutationTracker();
    private RequestSnapshot syncedSnapshot;
    private ScriptRequestBodyAccessor bodyAccessor;

    /**
     * Xây dựng một trình truy cập yêu cầu tập lệnh
     * <p>
     * Sẽ đảm bảo rằng tất cả các trường bộ sưu tập đều không có giá trị và có sẵn JavaScript Một trình bao bọc cho các hoạt động.
     * Các sửa đổi đối với các trình bao bọc này bằng các tập lệnh trước sẽ được phản ánh trực tiếp trong đối tượng yêu cầu ban đầu.
     * </p>
     *
     * @param req Đối tượng yêu cầu đã chuẩn bị
     */
    public ScriptRequestAccessor(PreparedRequest req) {
        this.raw = req;
        ensureRequestCollections();
        reloadViewFromRaw();
    }

    private void ensureRequestCollections() {
        if (raw.headersList == null) {
            raw.headersList = new ArrayList<>();
        }
        if (raw.formDataList == null) {
            raw.formDataList = new ArrayList<>();
        }
        if (raw.urlencodedList == null) {
            raw.urlencodedList = new ArrayList<>();
        }
        if (raw.paramsList == null) {
            raw.paramsList = new ArrayList<>();
        }
    }

    private void reloadViewFromRaw() {
        Runnable bodyMutation = mutationTracker.bodyWriteCallback();
        this.headers = new JsListWrapper<>(raw.headersList, JsListWrapper.ListType.HEADER);
        this.formData = new JsListWrapper<>(raw.formDataList, JsListWrapper.ListType.FORM_DATA, bodyMutation);
        this.urlencoded = new JsListWrapper<>(raw.urlencodedList, JsListWrapper.ListType.URLENCODED, bodyMutation);
        this.id = raw.id;
        this.url = new UrlWrapper(raw.url, raw.paramsList);
        this.params = this.url.query.asListWrapper();
        this.method = raw.method;
        this.bodyAccessor = ScriptRequestBodyAccessor.hasBody(raw)
                ? new ScriptRequestBodyAccessor(raw, formData, urlencoded, mutationTracker)
                : null;
        this.body = bodyAccessor;
        this.isMultipart = raw.isMultipart;
        this.followRedirects = raw.followRedirects;
        this.syncedSnapshot = snapshot();
    }

    /**
     * Mirrors the DevWorkbench Collection SDK {@code Request.update(options)} method for the request
     * properties that DevWorkbench's HTTP runtime can send.
     */
    public void update(Object options) {
        Object converted = ScriptValueConverter.toJavaObject(options);
        if (!(converted instanceof Map<?, ?> definition)) {
            return;
        }

        if (definition.containsKey("url")) {
            UrlWrapper.ResolvedUrl resolvedUrl = UrlWrapper.resolveDefinition(definition.get("url"));
            raw.url = resolvedUrl.url();
            raw.paramsList.clear();
            raw.paramsList.addAll(resolvedUrl.params());
            this.url = new UrlWrapper(raw.url, raw.paramsList);
            this.params = this.url.query.asListWrapper();
        }
        if (definition.containsKey("method")) {
            Object requestedMethod = definition.get("method");
            this.method = requestedMethod == null ? "GET" : requestedMethod.toString().toUpperCase(Locale.ROOT);
        }
        if (definition.containsKey("header") && isDevWorkbenchTruthy(definition.get("header"))) {
            replaceHeaders(definition.get("header"));
        }
        if (definition.containsKey("body")) {
            replaceBody(definition.get("body"));
        }
    }

    /**
     * Sẽ JavaScript Các sửa đổi đối với các trường công khai được đồng bộ hóa trở lại yêu cầu thực tế được gửi.
     * <p>
     * Phương pháp thu thập hoạt động trực tiếp trên lớp bên dưới List，Proxy phần tử bộ sưu tập, trường vô hướng và  URL Proxy tham số truy vấn
     * Nó sẽ được viết lại sau khi kịch bản trước kết thúc. Trong quá trình đồng bộ hóa, chỉ những trường đã được tập lệnh sửa đổi mới bị ghi đè.
     * Tránh ghi đè thẻ {@code pm.request.raw} hoặc {@code request} Sửa đổi hoàn thành trực tiếp.
     * </p>
     */
    public boolean syncToRaw() {
        headers.sync();
        formData.sync();
        urlencoded.sync();
        if (!Objects.equals(method, syncedSnapshot.method())) {
            raw.method = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
        }
        boolean bodyMutated = syncBody();
        bodyMutated |= mutationTracker.consumeBodyWrite();
        if (followRedirects != syncedSnapshot.followRedirects()) {
            raw.followRedirects = followRedirects;
        }

        syncUrl();

        boolean rawMultipartChangedDirectly = raw.isMultipart != syncedSnapshot.isMultipart();
        if (isMultipart != syncedSnapshot.isMultipart()) {
            raw.isMultipart = isMultipart;
        } else if (!rawMultipartChangedDirectly) {
            raw.isMultipart = hasEnabledFormData();
        }

        reloadViewFromRaw();
        return bodyMutated;
    }

    private boolean hasEnabledFormData() {
        return raw.formDataList != null && raw.formDataList.stream()
                .anyMatch(item -> item != null && item.isEnabled() && (item.isText() || item.isFile()));
    }

    private void syncUrl() {
        if (url == null || url.query == null) {
            return;
        }
        url.query.sync();
        String currentUrl = url.toString();
        if (!Objects.equals(currentUrl, syncedSnapshot.url())) {
            raw.url = currentUrl;
        } else if (!Objects.equals(raw.url, syncedSnapshot.rawUrl())) {
            url = new UrlWrapper(raw.url, raw.paramsList);
            params = url.query.asListWrapper();
        }
    }

    private void replaceHeaders(Object headerDefinition) {
        List<HttpHeader> replacements = new ArrayList<>();
        JsListWrapper<HttpHeader> replacementHeaders = new JsListWrapper<>(
                replacements,
                JsListWrapper.ListType.HEADER
        );
        Object converted = ScriptValueConverter.toJavaObject(headerDefinition);
        if (converted instanceof CharSequence headerLines) {
            for (String line : headerLines.toString().split("\\R")) {
                if (!line.isBlank()) {
                    replacementHeaders.add(line);
                }
            }
        } else if (converted instanceof Collection<?> collection) {
            for (Object item : collection) {
                addHeaderDefinition(replacementHeaders, item);
            }
        } else if (converted instanceof Map<?, ?> map) {
            // DevWorkbench's PropertyList.populate treats every top-level plain object as a key/value
            // map, even when the object itself contains properties named "key" and "value".
            map.forEach((key, value) -> addHeaderMapEntry(replacementHeaders, key, value));
        } else {
            addHeaderDefinition(replacementHeaders, converted);
        }

        // Convert first so aliases such as pm.request.raw.headersList and
        // pm.request.headers.all() stay readable until every replacement has been captured.
        raw.headersList.clear();
        raw.headersList.addAll(replacements);
    }

    private void addHeaderDefinition(JsListWrapper<HttpHeader> target, Object definition) {
        Object converted = ScriptValueConverter.toJavaObject(definition);
        if (converted instanceof JsListWrapper.ItemProxy proxy) {
            Object item = proxy.unwrap();
            if (item instanceof HttpHeader header) {
                target.getList().add(header);
            } else {
                target.add(proxy.toDefinition());
            }
            return;
        }
        if (converted instanceof HttpHeader header) {
            target.getList().add(header);
            return;
        }
        if (converted instanceof CharSequence headerLine) {
            target.add(headerLine.toString());
            return;
        }
        if (converted instanceof Map<?, ?> map) {
            Map<String, Object> header = new LinkedHashMap<>();
            map.forEach((key, value) -> header.put(String.valueOf(key), value));
            target.add(header);
        }
    }

    private void addHeaderMapEntry(JsListWrapper<HttpHeader> target, Object key, Object value) {
        Object convertedValue = ScriptValueConverter.toJavaObject(value);
        if (convertedValue == null
                || (convertedValue instanceof Double number && number.isNaN())
                || (convertedValue instanceof Float number && number.isNaN())) {
            return;
        }
        if (convertedValue instanceof CharSequence text) {
            target.add(String.valueOf(key), text.toString());
            return;
        }
        if (convertedValue instanceof Map<?, ?> definition) {
            Map<String, Object> header = new LinkedHashMap<>();
            definition.forEach((itemKey, itemValue) -> header.put(String.valueOf(itemKey), itemValue));
            target.add(header);
            return;
        }

        // Header.create(value, key) only uses the map key when value is a string. Other scalar
        // values construct an empty Header definition in devworkbench-collection.
        target.add("", "");
    }

    private void replaceBody(Object definition) {
        ScriptRequestBodyAccessor replacement = new ScriptRequestBodyAccessor(
                raw,
                null,
                null,
                mutationTracker
        );
        if (!replacement.replaceDefinition(definition)) {
            return;
        }

        if (bodyAccessor != null) {
            bodyAccessor.detach();
        }
        Runnable bodyMutation = mutationTracker.bodyWriteCallback();
        formData = new JsListWrapper<>(raw.formDataList, JsListWrapper.ListType.FORM_DATA, bodyMutation);
        urlencoded = new JsListWrapper<>(raw.urlencodedList, JsListWrapper.ListType.URLENCODED, bodyMutation);
        bodyAccessor = replacement;
        body = bodyAccessor;
    }

    private static boolean isDevWorkbenchTruthy(Object value) {
        Object converted = ScriptValueConverter.toJavaObject(value);
        if (converted == null || Boolean.FALSE.equals(converted)) {
            return false;
        }
        if (converted instanceof Number number) {
            double numericValue = number.doubleValue();
            return numericValue != 0 && !Double.isNaN(numericValue);
        }
        return !(converted instanceof CharSequence text) || !text.isEmpty();
    }

    private boolean syncBody() {
        if (body == bodyAccessor) {
            return bodyAccessor != null && bodyAccessor.syncToRaw();
        }

        mutationTracker.recordBodyWrite();
        if (body == null) {
            raw.body = null;
            raw.bodyType = RequestBodyTypes.BODY_TYPE_NONE;
            raw.formDataList = new ArrayList<>();
            raw.urlencodedList = new ArrayList<>();
            raw.isMultipart = false;
            return true;
        }

        ScriptRequestBodyAccessor replacement = new ScriptRequestBodyAccessor(
                raw,
                null,
                null,
                mutationTracker
        );
        replacement.update(body);
        replacement.syncToRaw();
        return true;
    }

    private RequestSnapshot snapshot() {
        return new RequestSnapshot(method, isMultipart, followRedirects, url.toString(), raw.url);
    }

    private record RequestSnapshot(String method,
                                   boolean isMultipart,
                                   boolean followRedirects,
                                   String url,
                                   String rawUrl) {
    }
}
