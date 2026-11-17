package com.laker.postman.http.runtime.okhttp;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.util.HttpUrlUtil;

import com.laker.postman.util.FileMimeTypeUtil;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;
import okhttp3.*;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * OkHttp Yêu cầu lớp công cụ xây dựng
 */
@UtilityClass
public class OkHttpRequestBuilder {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String DEFAULT_JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String DEFAULT_FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";
    private static final String DEFAULT_MIME_TYPE = FileMimeTypeUtil.DEFAULT_MIME_TYPE;
    private static final String APPLICATION_JSON = "application/json";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_HEAD = "HEAD";
    private static final byte[] EMPTY_BODY = new byte[0];

    /**
     * Xây dựng một yêu cầu bình thường
     */
    public static Request buildRequest(PreparedRequest req) {
        String methodUpper = req.method.toUpperCase();
        String contentType = extractContentType(req.headersList);
        RequestBody requestBody = buildRequestBody(req.body, methodUpper, contentType);

        Request.Builder builder = new Request.Builder()
                .url(HttpUrlUtil.normalizeIpv6Url(req.url))
                .method(methodUpper, requestBody);

        addHeadersFromList(builder, req.headersList);

        return builder.build();
    }

    /**
     * Xây dựng binary yêu cầu.req.body Lưu đường dẫn tệp cục bộ, .HTTP body Gửi byte thô của tệp.
     */
    public static Request buildBinaryRequest(PreparedRequest req) {
        String methodUpper = req.method.toUpperCase();
        String contentType = extractContentType(req.headersList);
        RequestBody requestBody = buildBinaryRequestBody(req.body, methodUpper, contentType);

        Request.Builder builder = new Request.Builder()
                .url(HttpUrlUtil.normalizeIpv6Url(req.url))
                .method(methodUpper, requestBody);

        addHeadersFromList(builder, req.headersList);

        return builder.build();
    }

    /**
     * Xây dựng multipart/form-data của OkHttp Request
     */
    public static Request buildMultipartRequest(PreparedRequest req) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        addFormDataPartsFromList(multipartBuilder, req.formDataList);

        Request.Builder builder = new Request.Builder()
                .url(HttpUrlUtil.normalizeIpv6Url(req.url))
                .method(req.method, multipartBuilder.build());

        addHeadersFromList(builder, req.headersList);

        return builder.build();
    }

    /**
     * Xây dựng application/x-www-form-urlencoded yêu cầu
     */
    public static Request buildFormRequest(PreparedRequest req) {
        FormBody.Builder formBuilder = new FormBody.Builder();

        addFormUrlEncodedPartsFromList(formBuilder, req.urlencodedList);

        Request.Builder builder = new Request.Builder()
                .url(HttpUrlUtil.normalizeIpv6Url(req.url))
                .method(req.method, formBuilder.build());

        boolean hasContentType = addHeadersFromList(builder, req.headersList);

        if (!hasContentType) {
            builder.addHeader(CONTENT_TYPE, DEFAULT_FORM_CONTENT_TYPE);
        }

        return builder.build();
    }

    /**
     * Từ headersList được chiết xuất từ Content-Type
     */
    private static String extractContentType(List<HttpHeader> headersList) {
        if (headersList == null) {
            return null;
        }

        for (HttpHeader header : headersList) {
            if (header.isEnabled() && CONTENT_TYPE.equalsIgnoreCase(header.getKey())) {
                String value = header.getValue();
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Xây dựng phần thân yêu cầu
     */
    private static RequestBody buildRequestBody(String body, String method, String contentType) {
        if (METHOD_GET.equals(method) || METHOD_HEAD.equals(method)) {
            return null;
        }

        if (body != null && !body.isEmpty()) {
            return createRequestBodyWithContent(body, contentType);
        }

        return createEmptyRequestBody(contentType);
    }

    private static RequestBody buildBinaryRequestBody(String filePath, String method, String contentType) {
        if (METHOD_GET.equals(method) || METHOD_HEAD.equals(method)) {
            return null;
        }

        if (filePath == null || filePath.isBlank()) {
            return createEmptyRequestBody(contentType != null ? contentType : DEFAULT_MIME_TYPE);
        }

        File file = FileMimeTypeUtil.toFile(filePath);
        if (!FileMimeTypeUtil.isReadableRegularFile(file)) {
            throw new IllegalArgumentException("Binary request body file does not exist: " + filePath);
        }

        String actualContentType = contentType != null ? contentType : detectMimeType(file);
        return RequestBody.create(file, MediaType.parse(actualContentType));
    }

    /**
     * Tạo nội dung yêu cầu chứa nội dung
     */
    private static RequestBody createRequestBodyWithContent(String body, String contentType) {
        String actualContentType = contentType != null ? contentType : DEFAULT_JSON_CONTENT_TYPE;
        String processedBody = processBodyContent(body, actualContentType);
        MediaType mediaType = MediaType.parse(actualContentType);
        Charset charset = mediaType != null ? mediaType.charset(StandardCharsets.UTF_8) : StandardCharsets.UTF_8;

        return RequestBody.create(processedBody.getBytes(charset), mediaType);
    }

    /**
     * Normalize raw content for both transport and code snippet generation.
     * JSON comments are removed only when parsing succeeds; other content is preserved.
     */
    public static String processBodyContent(String body, String contentType) {
        if (isJsonContentType(contentType) && containsJsonComment(body)) {
            return cleanJsonComments(body);
        }
        return body;
    }

    private static boolean containsJsonComment(String body) {
        if (body == null || body.length() < 2) {
            return false;
        }
        boolean inString = false;
        boolean escaping = false;
        for (int i = 0; i < body.length(); i++) {
            char current = body.charAt(i);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '/' && i + 1 < body.length()) {
                char next = body.charAt(i + 1);
                if (next == '/' || next == '*') {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Xác định xem có phải như vậy không JSON Content-Type
     */
    private static boolean isJsonContentType(String contentType) {
        return contentType != null && contentType.toLowerCase().contains(APPLICATION_JSON);
    }

    /**
     * Dọn dẹp JSON chú thích (được hỗ trợ JSON5）
     */
    private static String cleanJsonComments(String json) {
        try {
            return JsonUtil.cleanJsonComments(json);
        } catch (Exception e) {
            // Nếu việc dọn dẹp không thành công, nội dung gốc sẽ được trả về
            return json;
        }
    }

    /**
     * Tạo một nội dung yêu cầu trống
     */
    private static RequestBody createEmptyRequestBody(String contentType) {
        MediaType mediaType = contentType != null ? MediaType.parse(contentType) : null;
        return RequestBody.create(EMPTY_BODY, mediaType);
    }


    /**
     * Phát hiện tập tin MIME loại
     */
    private static String detectMimeType(File file) {
        return FileMimeTypeUtil.detectMimeType(file);
    }

    /**
     * phán xét header name Nó có hợp pháp không? ASCII ký tự và không chứa các ký tự không hợp lệ
     */
    private static boolean isValidHeaderName(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            // Chỉ được phép 33~126 phạm vi ASCII ký tự và không thể chứa dấu hai chấm
            if (c < 33 || c > 126 || c == ':') {
                return false;
            }
        }
        return true;
    }

    /**
     * Từ List Thêm vào HTTP Tiêu đề yêu cầu (hỗ trợ tương tự key）
     */
    private static boolean addHeadersFromList(Request.Builder builder, List<HttpHeader> headersList) {
        if (headersList == null || headersList.isEmpty()) {
            return false;
        }

        boolean hasContentType = false;
        for (HttpHeader header : headersList) {
            if (!header.isEnabled()) {
                continue;
            }

            String key = header.getKey();
            String value = header.getValue();

            if (isValidHeaderName(key)) {
                builder.addHeader(key, value != null ? value : "");

                if (CONTENT_TYPE.equalsIgnoreCase(key)) {
                    hasContentType = true;
                }
            }
        }

        return hasContentType;
    }

    /**
     * Từ List Thêm vào Form-Data（Hỗ trợ tương tự key）
     */
    private static void addFormDataPartsFromList(MultipartBody.Builder builder, List<HttpFormData> formDataList) {
        if (formDataList == null || formDataList.isEmpty()) {
            return;
        }

        for (HttpFormData formData : formDataList) {
            if (!formData.isEnabled()) {
                continue;
            }

            String key = formData.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }

            if (formData.isText()) {
                String value = formData.getValue() != null ? formData.getValue() : "";
                builder.addFormDataPart(key, value);
            } else if (formData.isFile()) {
                String filePath = formData.getValue();
                if (filePath != null && !filePath.isEmpty()) {
                    File file = FileMimeTypeUtil.toFile(filePath);
                    if (FileMimeTypeUtil.isReadableRegularFile(file)) {
                        String mimeType = detectMimeType(file);
                        builder.addFormDataPart(
                                key,
                                file.getName(),
                                RequestBody.create(file, MediaType.parse(mimeType))
                        );
                    } else {
                        // Khi tệp không tồn tại hoặc không phải là tệp thông thường, hãy thêm phần giữ chỗ cho tệp trống
                        // Bằng cách này, người dùng có thể nhìn thấy trường trong phản hồi thay vì âm thầm thất bại
                        builder.addFormDataPart(
                                key,
                                "",
                                RequestBody.create(EMPTY_BODY, MediaType.parse(DEFAULT_MIME_TYPE))
                        );
                    }
                }
            }
        }
    }

    /**
     * Từ List Thêm vào URL-Encoded Form（Hỗ trợ tương tự key）
     */
    private static void addFormUrlEncodedPartsFromList(FormBody.Builder builder, List<HttpFormUrlencoded> urlencodedList) {
        if (urlencodedList == null || urlencodedList.isEmpty()) {
            return;
        }

        for (HttpFormUrlencoded urlencoded : urlencodedList) {
            if (!urlencoded.isEnabled()) {
                continue;
            }

            String key = urlencoded.getKey();
            if (key != null && !key.isEmpty()) {
                String value = urlencoded.getValue() != null ? urlencoded.getValue() : "";
                builder.add(key, value);
            }
        }
    }
}
