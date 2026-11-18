package com.nvhuong.devworkbench.service.js.api;

import com.nvhuong.devworkbench.request.model.HttpParam;
import org.graalvm.polyglot.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * URL giấy gói - Được sử dụng trong JavaScript thăm vào pm.request.url
 * <p>
 * Cung cấp các cặp URL Truy cập tham số, mô phỏng DevWorkbench của pm.request.url đối tượng.
 * </p>
 *
 * <h3>Ví dụ sử dụng:</h3>
 * <pre>{@code
 * // JavaScript trong kịch bản
 * var password = pm.request.url.query.all()[1].value;
 * pm.request.url.query.all()[1].value = "newValue";
 * }</pre>
 */
public class UrlWrapper {

    /**
     * URL chuỗi
     */
    public final String url;

    private final String baseUrl;
    private final String fragment;

    /**
     * Trình bao bọc tham số truy vấn
     */
    public final QueryWrapper query;

    public UrlWrapper(String url, List<HttpParam> params) {
        this.url = url;
        String safeUrl = url == null ? "" : url;
        int fragmentIndex = safeUrl.indexOf('#');
        this.fragment = fragmentIndex >= 0 ? safeUrl.substring(fragmentIndex) : "";
        String withoutFragment = fragmentIndex >= 0 ? safeUrl.substring(0, fragmentIndex) : safeUrl;
        int queryIndex = withoutFragment.indexOf('?');
        this.baseUrl = queryIndex >= 0 ? withoutFragment.substring(0, queryIndex) : withoutFragment;

        List<HttpParam> target = params != null ? params : new ArrayList<>();
        DevWorkbenchQueryCodec.reconcileUrlWithParams(safeUrl, target);
        this.query = new QueryWrapper(target);
    }

    /**
     * Lấy URL Phần đường dẫn (không bao gồm các tham số giao thức, tên miền, cổng và truy vấn)
     * <p>
     * Ví dụ: {@code "https://api.example.com:8080/users/123?id=1" -> "/users/123"}
     * </p>
     *
     * @return URL Đường dẫn hoặc một chuỗi trống nếu phân tích cú pháp không thành công
     */
    public String getPath() {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            String currentUrl = toString();
            String urlWithoutProtocol = currentUrl;
            int protocolEnd = currentUrl.indexOf("://");
            if (protocolEnd > 0) {
                urlWithoutProtocol = currentUrl.substring(protocolEnd + 3);
            }

            int pathStart = urlWithoutProtocol.indexOf('/');
            if (pathStart == -1) {
                return "/";
            }

            int queryStart = urlWithoutProtocol.indexOf('?', pathStart);
            int fragmentStart = urlWithoutProtocol.indexOf('#', pathStart);
            int pathEnd = firstNonNegative(queryStart, fragmentStart, urlWithoutProtocol.length());
            return urlWithoutProtocol.substring(pathStart, pathEnd);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Lấy URL Phần tên máy chủ (không bao gồm giao thức và cổng)
     * <p>
     * Ví dụ: {@code "https://api.example.com:8080/users" -> "api.example.com"}
     * </p>
     *
     * @return Tên máy chủ hoặc một chuỗi trống nếu phân tích cú pháp không thành công
     */
    public String getHost() {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            String currentUrl = toString();
            String urlWithoutProtocol = currentUrl;
            int protocolEnd = currentUrl.indexOf("://");
            if (protocolEnd > 0) {
                urlWithoutProtocol = currentUrl.substring(protocolEnd + 3);
            }

            int authorityEnd = firstNonNegative(
                    urlWithoutProtocol.indexOf('/'),
                    urlWithoutProtocol.indexOf('?'),
                    urlWithoutProtocol.indexOf('#'),
                    urlWithoutProtocol.length()
            );
            String hostPart = urlWithoutProtocol.substring(0, authorityEnd);
            int credentialsEnd = hostPart.lastIndexOf('@');
            if (credentialsEnd >= 0) {
                hostPart = hostPart.substring(credentialsEnd + 1);
            }

            if (hostPart.startsWith("[")) {
                int ipv6End = hostPart.indexOf(']');
                return ipv6End >= 0 ? hostPart.substring(0, ipv6End + 1) : hostPart;
            }
            int portStart = hostPart.lastIndexOf(':');
            return portStart > 0 ? hostPart.substring(0, portStart) : hostPart;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Lấy chuỗi tham số truy vấn (không bao gồm  '?'）
     * <p>
     * Ví dụ: {@code "https://api.example.com/users?id=1&name=test" -> "id=1&name=test"}
     * </p>
     *
     * @return Chuỗi tham số truy vấn hoặc chuỗi trống nếu không có tham số truy vấn
     */
    public String getQueryString() {
        if (url == null || url.isEmpty()) {
            return "";
        }

        try {
            return buildQueryString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Nhận tham số đường dẫn và truy vấn (từ tham số đầu tiên '/' bắt đầu)
     * <p>
     * Ví dụ: {@code "https://api.example.com/users?id=1" -> "/users?id=1"}
     * </p>
     *
     * @return Các tham số đường dẫn và truy vấn, được trả về nếu phân tích cú pháp không thành công "/"
     */
    public String getPathWithQuery() {
        if (url == null || url.isEmpty()) {
            return "/";
        }

        try {
            String queryString = getQueryString();
            return getPath() + (!queryString.isEmpty() ? "?" + queryString : "");
        } catch (Exception e) {
            return "/";
        }
    }

    private static int firstNonNegative(int... candidates) {
        int result = Integer.MAX_VALUE;
        for (int candidate : candidates) {
            if (candidate >= 0 && candidate < result) {
                result = candidate;
            }
        }
        return result == Integer.MAX_VALUE ? -1 : result;
    }

    /**
     * Nhận phiên bản đầy đủ URL chuỗi
     *
     * @return URL chuỗi
     */
    public String toString() {
        String queryString = buildQueryString();
        return baseUrl + (hasEnabledQueryParameter() ? "?" + queryString : "") + fragment;
    }

    private String buildQueryString() {
        List<HttpParam> enabled = new ArrayList<>();
        for (HttpParam param : query.params) {
            if (param != null && param.isEnabled()) {
                enabled.add(param);
            }
        }
        return DevWorkbenchQueryCodec.build(enabled);
    }

    private boolean hasEnabledQueryParameter() {
        return query.params.stream().anyMatch(param -> param != null && param.isEnabled());
    }

    /**
     * Resolves the String-or-definition contract accepted by DevWorkbench's {@code Url.update}.
     * A lone {@code raw} field is accepted as a permissive collection-export fallback.
     */
    static ResolvedUrl resolveDefinition(Object definition) {
        Object converted = ScriptValueConverter.toJavaObject(definition);
        if (converted instanceof UrlWrapper wrapper) {
            String resolved = wrapper.toString();
            return new ResolvedUrl(resolved, DevWorkbenchQueryCodec.parseUrl(resolved));
        }
        if (converted instanceof CharSequence text) {
            String resolved = text.toString();
            return new ResolvedUrl(resolved, DevWorkbenchQueryCodec.parseUrl(resolved));
        }
        if (!(converted instanceof Map<?, ?> map)) {
            String resolved = Objects.toString(converted, "");
            return new ResolvedUrl(resolved, DevWorkbenchQueryCodec.parseUrl(resolved));
        }

        if (!hasParsedUrlFields(map)) {
            String resolved = Objects.toString(javaValue(map, "raw"), "");
            return new ResolvedUrl(resolved, DevWorkbenchQueryCodec.parseUrl(resolved));
        }

        List<HttpParam> queryParams = parseQueryDefinition(javaValue(map, "query"));
        String resolved = buildUrlFromDefinition(map, queryParams);
        return new ResolvedUrl(resolved, queryParams);
    }

    private static boolean hasParsedUrlFields(Map<?, ?> definition) {
        return definition.containsKey("protocol")
                || definition.containsKey("auth")
                || definition.containsKey("host")
                || definition.containsKey("port")
                || definition.containsKey("path")
                || definition.containsKey("query")
                || definition.containsKey("hash")
                || definition.containsKey("variable");
    }

    private static String buildUrlFromDefinition(Map<?, ?> definition, List<HttpParam> queryParams) {
        StringBuilder result = new StringBuilder();
        Object protocol = javaValue(definition, "protocol");
        if (protocol != null && !protocol.toString().isEmpty()) {
            String protocolText = protocol.toString();
            result.append(protocolText);
            if (!protocolText.endsWith("://")) {
                result.append("://");
            }
        }

        appendAuth(result, javaValue(definition, "auth"));
        appendJoined(result, javaValue(definition, "host"), ".", false);

        Object port = javaValue(definition, "port");
        if (port != null) {
            result.append(':').append(port);
        }

        Object path = javaValue(definition, "path");
        if (path instanceof CharSequence text) {
            String pathText = text.toString();
            if ("/".equals(pathText)) {
                result.append('/');
            } else if (!pathText.isEmpty()) {
                result.append('/').append(pathText.startsWith("/") ? pathText.substring(1) : pathText);
            }
        } else if (path instanceof Collection<?> pathSegments) {
            result.append('/');
            appendJoined(result, pathSegments, "/", true);
        }

        List<HttpParam> enabledQueryParams = queryParams.stream()
                .filter(Objects::nonNull)
                .filter(HttpParam::isEnabled)
                .toList();
        if (!enabledQueryParams.isEmpty()) {
            result.append('?').append(DevWorkbenchQueryCodec.build(enabledQueryParams));
        }

        Object hash = javaValue(definition, "hash");
        if (hash instanceof CharSequence) {
            result.append('#').append(hash);
        }
        return result.toString();
    }

    private static void appendAuth(StringBuilder result, Object authDefinition) {
        if (!(authDefinition instanceof Map<?, ?> auth)) {
            return;
        }
        Object user = javaValue(auth, "user");
        Object password = javaValue(auth, "password");
        if (!(user instanceof CharSequence) && !(password instanceof CharSequence)) {
            return;
        }
        if (user instanceof CharSequence) {
            result.append(user);
        }
        if (password instanceof CharSequence) {
            result.append(':').append(password);
        }
        result.append('@');
    }

    private static void appendJoined(StringBuilder result,
                                     Object value,
                                     String separator,
                                     boolean emptyCollectionAllowed) {
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty() && !emptyCollectionAllowed) {
                return;
            }
            StringJoiner joiner = new StringJoiner(separator);
            collection.forEach(item -> joiner.add(Objects.toString(
                    ScriptValueConverter.toJavaObject(item), "")));
            result.append(joiner);
        } else if (value != null) {
            result.append(value);
        }
    }

    private static List<HttpParam> parseQueryDefinition(Object queryDefinition) {
        Object converted = ScriptValueConverter.toJavaObject(queryDefinition);
        if (converted instanceof CharSequence queryString) {
            return DevWorkbenchQueryCodec.parse(queryString.toString());
        }
        if (converted instanceof Collection<?> collection) {
            List<HttpParam> result = new ArrayList<>();
            collection.forEach(item -> addQueryItem(result, item));
            return result;
        }
        if (converted instanceof Map<?, ?> queryMap) {
            List<HttpParam> result = new ArrayList<>();
            queryMap.forEach((key, value) -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("key", Objects.toString(key, ""));
                item.put("value", ScriptValueConverter.toJavaObject(value));
                addQueryItem(result, item);
            });
            return result;
        }
        return new ArrayList<>();
    }

    private static void addQueryItem(List<HttpParam> target, Object definition) {
        Object converted = ScriptValueConverter.toJavaObject(definition);
        if (converted instanceof CharSequence text) {
            target.addAll(DevWorkbenchQueryCodec.parse(text.toString()));
            return;
        }
        if (!(converted instanceof Map<?, ?> item)) {
            return;
        }
        Object disabled = javaValue(item, "disabled");
        Object enabled = javaValue(item, "enabled");
        boolean itemEnabled;
        if (disabled instanceof Boolean disabledFlag) {
            itemEnabled = !disabledFlag;
        } else {
            itemEnabled = !(enabled instanceof Boolean enabledFlag) || enabledFlag;
        }
        Object value = javaValue(item, "value");
        target.add(new HttpParam(
                itemEnabled,
                devworkbenchQueryString(javaValue(item, "key")),
                devworkbenchQueryString(value),
                Objects.toString(javaValue(item, "description"), "")
        ));
    }

    private static Object javaValue(Map<?, ?> map, String key) {
        return ScriptValueConverter.toJavaObject(map.get(key));
    }

    private static String devworkbenchQueryString(Object value) {
        return value instanceof CharSequence text ? text.toString() : null;
    }

    record ResolvedUrl(String url, List<HttpParam> params) {
    }

    /**
     * Lớp trình bao bọc tham số truy vấn
     */
    public static class QueryWrapper {
        private final List<HttpParam> params;
        private final JsListWrapper<HttpParam> delegate;

        public QueryWrapper(List<HttpParam> params) {
            this.params = params != null ? params : new ArrayList<>();
            this.delegate = new JsListWrapper<>(this.params, JsListWrapper.ListType.PARAM);
        }

        /**
         * Trả về tất cả tham số truy vấn JavaScript Danh sách đóng gói thân thiện
         * Lưu ý: Những gì được trả về là danh sách proxy được lưu trong bộ nhớ đệm và các sửa đổi đối với trường proxy sẽ được giữ lại
         */
        public List<JsListWrapper.ItemProxy> all() {
            return delegate.all();
        }

        public void add(Map<String, Object> item) {
            delegate.add(item);
        }

        public Boolean upsert(Map<String, Object> item) {
            return delegate.upsert(item);
        }

        public void remove(String key) {
            delegate.remove(key);
        }

        public String get(String key) {
            sync();
            return delegate.get(key);
        }

        public boolean has(String key) {
            sync();
            return delegate.has(key);
        }

        public boolean has(String key, Object value) {
            sync();
            return delegate.has(key, value);
        }

        public int count() {
            return params.size();
        }

        public void clear() {
            delegate.clear();
        }

        public JsListWrapper.ItemProxy one(String key) {
            return delegate.one(key);
        }

        public JsListWrapper.ItemProxy idx(int index) {
            return delegate.idx(index);
        }

        public void each(Value callback) {
            if (callback == null || !callback.canExecute()) {
                return;
            }
            delegate.each(callback);
        }

        public Map<String, Object> toObject() {
            sync();
            return delegate.toObject();
        }

        /**
         * Đồng bộ hóa tất cả các thay đổi của tác nhân trở lại lớp bên dưới HttpParam đối tượng
         */
        public void sync() {
            delegate.sync();
        }

        JsListWrapper<HttpParam> asListWrapper() {
            return delegate;
        }
    }
}
