package com.laker.postman.service.collections;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.file.ResponseFileNames;
import com.laker.postman.http.runtime.file.ResponseFiles;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.util.FileExtensionUtil;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@UtilityClass
public class SavedResponseSnapshotMapper {
    private static final int MAX_ORIGINAL_REQUEST_BODY_PREVIEW_CHARS = 64 * 1024;

    public SavedResponse fromExchange(String name, PreparedRequest request, HttpResponse response) {
        SavedResponse saved = new SavedResponse();
        saved.setId(UUID.randomUUID().toString());
        saved.setName(name);
        saved.setTimestamp(System.currentTimeMillis());
        saved.setOriginalRequest(toOriginalRequest(request));

        if (response != null) {
            saved.setCode(response.code);
            saved.setHeaders(toSavedHeaders(response));
            if (ResponseFiles.isFileResponse(response)) {
                // Ví dụ này chỉ lưu siêu thông tin tệp: văn bản được giải mã không thể khôi phục nội dung nhị phân và sẽ chỉ bị cắt xén khi lưu
                saved.setFile(toFileInfo(response, false));
                saved.setBody("");
            } else if (ResponseFiles.isTextAttachment(response)) {
                // Tệp đính kèm văn bản (ví dụ: CSV）Bảo quản văn bản; chỉ lưu nội dung nếu byte gốc có thể được khôi phục không mất dữ liệu trong bộ ký tự được khai báo
                boolean restorable = ResponseFiles.canRestoreBytesFromBody(response);
                saved.setFile(toFileInfo(response, restorable));
                saved.setBody(restorable ? response.body : "");
            } else {
                saved.setBody(response.body);
            }
            saved.setCostMs(response.costMs);
            saved.setBodySize(response.bodySize);
            saved.setHeadersSize(response.headersSize);
            saved.setPreviewLanguage(detectPreviewLanguage(response));
        }
        return saved;
    }

    public HttpResponse toRuntimeResponse(SavedResponse savedResponse) {
        HttpResponse response = new HttpResponse();
        if (savedResponse == null) {
            response.headers = new LinkedHashMap<>();
            return response;
        }
        response.code = savedResponse.getCode();
        response.body = savedResponse.getBody();
        response.headers = new LinkedHashMap<>();
        List<HttpHeader> headers = savedResponse.getHeaders();
        if (headers != null) {
            for (HttpHeader header : headers) {
                if (header == null || header.getKey() == null) {
                    continue;
                }
                response.headers.put(header.getKey(), List.of(header.getValue()));
            }
        }
        response.costMs = savedResponse.getCostMs();
        response.bodySize = savedResponse.getBodySize();
        response.headersSize = savedResponse.getHeadersSize();
        applyFileInfo(savedResponse, response);
        return response;
    }

    private SavedResponse.FileInfo toFileInfo(HttpResponse response, boolean contentInBody) {
        SavedResponse.FileInfo fileInfo = new SavedResponse.FileInfo();
        fileInfo.setFileName(response.fileName);
        fileInfo.setExtension(response.detectedFileExtension);
        fileInfo.setAttachment(response.fileAttachment);
        fileInfo.setContentInBody(contentInBody);
        return fileInfo;
    }

    private void applyFileInfo(SavedResponse savedResponse, HttpResponse response) {
        SavedResponse.FileInfo fileInfo = savedResponse.getFile();
        if (fileInfo != null) {
            if (fileInfo.isContentInBody()) {
                response.fileName = fileInfo.getFileName();
                response.rawBodyBytes = ResponseFiles.encodeBody(response);
            } else {
                ResponseFiles.markContentOmitted(response, fileInfo.getExtension(), fileInfo.getFileName());
            }
            response.fileAttachment = fileInfo.isAttachment();
            return;
        }
        // Ví dụ về cách lưu tương thích với các phiên bản cũ hơn: Nội dung tệp được lưu dưới dạng văn bản và bị cắt xén. Sau khi xác định nó bằng tiêu đề tệp, thay vào đó, tổng quan về tệp sẽ được hiển thị
        String legacyExtension = ResponseFiles.detectFromLegacyText(savedResponse.getBody());
        if (legacyExtension != null) {
            ResponseFiles.markContentOmitted(response, legacyExtension, null);
            return;
        }
        if (isLegacyBinaryPlaceholder(savedResponse)) {
            ResponseFiles.markContentOmitted(response, null, null);
            return;
        }
        restoreLegacyTextAttachment(savedResponse, response);
    }

    /**
     * Tương thích với các ví dụ nhị phân được lưu bởi các phiên bản cũ hơn: những phiên bản được lưu tại thời điểm đó là“Đã lưu dưới dạng tập tin tạm thời”Văn bản nhắc nhở như thế này, thay vì nội dung tập tin.
     * Cơ sở được khai báo là nhị phân và kích thước phản hồi được ghi lại, nhưng số byte văn bản không khớp với nó; văn bản viết tay hoặc chỉnh sửa sẽ được cập nhật kích thước đồng thời và không bị ảnh hưởng.
     */
    public boolean isLegacyBinaryPlaceholder(SavedResponse savedResponse) {
        if (savedResponse == null || savedResponse.getFile() != null || savedResponse.getBodySize() <= 0) {
            return false;
        }
        String contentType = null;
        if (savedResponse.getHeaders() != null) {
            for (HttpHeader header : savedResponse.getHeaders()) {
                if (header != null && "Content-Type".equalsIgnoreCase(header.getKey())) {
                    contentType = header.getValue();
                    break;
                }
            }
        }
        if (!FileExtensionUtil.isBinaryType(contentType)) {
            return false;
        }
        String body = savedResponse.getBody() == null ? "" : savedResponse.getBody();
        return body.getBytes(StandardCharsets.UTF_8).length != savedResponse.getBodySize();
    }

    /**
     * Tương thích với các tệp đính kèm văn bản được lưu trong các phiên bản cũ hơn (ví dụ: CSV）：Không có thông tin tệp nhưng có tiêu đề và nội dung phản hồi, đồng thời có thể khôi phục thẻ đính kèm, tên tệp và byte thô.
     * Nếu văn bản chứa các ký tự thay thế, điều đó có nghĩa là nó đã bị hỏng trong quá trình lưu và các byte sẽ không được khôi phục.
     */
    private void restoreLegacyTextAttachment(SavedResponse savedResponse, HttpResponse response) {
        String contentDisposition = ResponseFileNames.getContentDisposition(response.headers);
        String contentType = ResponseFiles.contentType(response);
        if (!ResponseFileNames.isAttachment(contentDisposition) || FileExtensionUtil.isBinaryType(contentType)) {
            return;
        }
        response.fileAttachment = true;
        String extension = FileExtensionUtil.guessExtension(contentType);
        SavedResponse.OriginalRequest original = savedResponse.getOriginalRequest();
        response.fileName = ResponseFileNames.resolve(contentDisposition,
                urlPath(original == null ? null : original.getUrl()),
                extension != null ? extension : ".txt");
        if (response.body != null && response.body.indexOf('\uFFFD') < 0) {
            response.rawBodyBytes = ResponseFiles.encodeBody(response);
        }
    }

    private String urlPath(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String path = url.trim();
        int end = path.length();
        for (char delimiter : new char[]{'?', '#'}) {
            int index = path.indexOf(delimiter);
            if (index >= 0) {
                end = Math.min(end, index);
            }
        }
        path = path.substring(0, end);
        int scheme = path.indexOf("://");
        if (scheme >= 0) {
            int slash = path.indexOf('/', scheme + 3);
            path = slash >= 0 ? path.substring(slash) : "";
        }
        return path;
    }

    public void sanitizeSavedResponses(HttpRequestItem item) {
        if (item == null || item.getResponse() == null) {
            return;
        }
        for (SavedResponse savedResponse : item.getResponse()) {
            sanitizeSavedResponse(savedResponse);
        }
    }

    public SavedResponse.OriginalRequest toOriginalRequest(HttpRequestItem item) {
        SavedResponse.OriginalRequest original = new SavedResponse.OriginalRequest();
        if (item == null) return original;
        original.setMethod(item.getMethod());
        original.setUrl(item.getUrl());
        original.setHeaders(item.getHeadersList() != null ? new ArrayList<>(item.getHeadersList()) : new ArrayList<>());
        original.setPathVariables(item.getPathVariablesList() != null ? new ArrayList<>(item.getPathVariablesList()) : new ArrayList<>());
        original.setParams(item.getParamsList() != null ? new ArrayList<>(item.getParamsList()) : new ArrayList<>());
        original.setBodyType(item.getBodyType());
        original.setBody(item.getBody());
        original.setFormDataList(item.getFormDataList() != null ? new ArrayList<>(item.getFormDataList()) : new ArrayList<>());
        original.setUrlencodedList(item.getUrlencodedList() != null ? new ArrayList<>(item.getUrlencodedList()) : new ArrayList<>());
        sanitizeOriginalRequest(original);
        return original;
    }

    public void sanitizeSavedResponse(SavedResponse savedResponse) {
        if (savedResponse == null) {
            return;
        }
        sanitizeOriginalRequest(savedResponse.getOriginalRequest());
    }

    public void sanitizeOriginalRequest(SavedResponse.OriginalRequest originalRequest) {
        if (originalRequest == null || originalRequest.getBody() == null) {
            return;
        }

        String body = originalRequest.getBody();
        long bodySize = body.getBytes(StandardCharsets.UTF_8).length;
        if (body.length() <= MAX_ORIGINAL_REQUEST_BODY_PREVIEW_CHARS) {
            if (!originalRequest.isBodyTruncated()) {
                originalRequest.setOriginalBodySize(bodySize);
            }
            return;
        }

        long originalSize = Math.max(originalRequest.getOriginalBodySize(), bodySize);
        originalRequest.setBody(body.substring(0, MAX_ORIGINAL_REQUEST_BODY_PREVIEW_CHARS));
        originalRequest.setBodyTruncated(true);
        originalRequest.setOriginalBodySize(originalSize);
    }

    private SavedResponse.OriginalRequest toOriginalRequest(PreparedRequest request) {
        SavedResponse.OriginalRequest originalRequest = new SavedResponse.OriginalRequest();
        if (request == null) {
            return originalRequest;
        }
        originalRequest.setMethod(request.method);
        originalRequest.setUrl(request.url);
        originalRequest.setHeaders(request.headersList != null ? new ArrayList<>(request.headersList) : new ArrayList<>());
        originalRequest.setPathVariables(request.pathVariablesList != null ? new ArrayList<>(request.pathVariablesList) : new ArrayList<>());
        originalRequest.setParams(request.paramsList != null ? new ArrayList<>(request.paramsList) : new ArrayList<>());
        originalRequest.setBodyType(request.bodyType);
        originalRequest.setBody(request.body);
        originalRequest.setFormDataList(request.formDataList != null ? new ArrayList<>(request.formDataList) : new ArrayList<>());
        originalRequest.setUrlencodedList(request.urlencodedList != null ? new ArrayList<>(request.urlencodedList) : new ArrayList<>());
        sanitizeOriginalRequest(originalRequest);
        return originalRequest;
    }

    private List<HttpHeader> toSavedHeaders(HttpResponse response) {
        List<HttpHeader> headers = new ArrayList<>();
        if (response.headers == null) {
            return headers;
        }
        for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                headers.add(new HttpHeader(true, key, String.join(", ", values)));
            }
        }
        return headers;
    }

    private String detectPreviewLanguage(HttpResponse response) {
        String contentType = findContentType(response);
        if (contentType != null) {
            if (contentType.contains("json")) {
                return "json";
            }
            if (contentType.contains("xml")) {
                return "xml";
            }
            if (contentType.contains("html")) {
                return "html";
            }
            if (contentType.contains("javascript")) {
                return "javascript";
            }
            if (contentType.contains("css")) {
                return "css";
            }
            if (contentType.contains("text")) {
                return "text";
            }
        }

        String body = response.body;
        if (body != null && !body.isEmpty()) {
            String trimmed = body.trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                    || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                return "json";
            }
            if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
                String lower = trimmed.toLowerCase();
                if (lower.contains("<html")) {
                    return "html";
                }
                if (lower.contains("<?xml")) {
                    return "xml";
                }
            }
        }

        return "text";
    }

    private String findContentType(HttpResponse response) {
        String contentType = ResponseFiles.firstHeader(response == null ? null : response.headers,
                ResponseFiles.CONTENT_TYPE_HEADER);
        return contentType == null ? null : contentType.toLowerCase(Locale.ROOT);
    }
}
