package com.laker.postman.http.runtime.file;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.util.FileExtensionUtil;
import lombok.experimental.UtilityClass;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * phản hồi“Cho dù đó là một tập tin”Phán quyết thống nhất có thể được chia sẻ bởi bảng phản hồi, lưu mẫu, bản ghi lịch sử, kết xuất, v.v. để tránh sự mâu thuẫn trong các quy tắc ở mọi nơi.
 */
@UtilityClass
public class ResponseFiles {

    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    /**
     * Đọc giá trị tiêu đề phản hồi đầu tiên không phân biệt chữ hoa chữ thường, trả về nếu nó không tồn tại {@code null}。
     */
    public static String firstHeader(Map<String, List<String>> headers, String name) {
        if (headers == null || name == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
            }
        }
        return null;
    }

    /**
     * Đáp ứng Content-Type，Trả về một chuỗi trống nếu nó không tồn tại.
     */
    public static String contentType(HttpResponse response) {
        String contentType = response == null ? null : firstHeader(response.headers, CONTENT_TYPE_HEADER);
        return contentType == null ? "" : contentType;
    }

    /**
     * Nội dung phản hồi được xử lý dưới dạng tệp (thay vì văn bản): định dạng tệp được nhận dạng, nội dung tệp bị bỏ qua hoặc Content-Type Được khai báo là nhị phân và đã nhận được nội dung.
     * Loại phản ứng này {@link HttpResponse#body} Văn bản không thể đọc được, chỉ nên giữ lại thông tin meta của tệp khi vẫn tồn tại.
     * Khi tải xuống nhị phân không thành công (vượt quá giới hạn, kết nối bị gián đoạn, không có nội dung phản hồi) body Đó là một thông báo lỗi và được giữ lại dưới dạng văn bản để hỗ trợ khắc phục sự cố trong lịch sử.
     */
    public static boolean isFileResponse(HttpResponse response) {
        return response != null
                && (response.isRecognizedFile()
                || (FileExtensionUtil.isBinaryType(contentType(response)) && hasFileContent(response)));
    }

    private static boolean hasFileContent(HttpResponse response) {
        return response.bodySize > 0
                || response.rawBodyBytes != null
                || (response.filePath != null && !response.filePath.isBlank());
    }

    /**
     * Chỉ có nghĩa là“Một số loại dữ liệu nhị phân”、Định dạng cụ thể không được chỉ định Content-Type。
     */
    public static boolean isGenericBinaryContentType(String contentType) {
        String mediaType = contentType == null ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return GENERIC_BINARY_MEDIA_TYPES.contains(mediaType);
    }

    private static final Set<String> GENERIC_BINARY_MEDIA_TYPES = Set.of(
            "application/octet-stream", "binary/octet-stream", "application/binary", "application/unknown",
            "application/download", "application/force-download", "application/x-download");

    /**
     * Tệp đính kèm văn bản (ví dụ: CSV、GBK Đã mã hóa TXT）：Nội dung phản hồi là văn bản có thể đọc được nhưng máy chủ muốn tải xuống dưới dạng tệp.
     */
    public static boolean isTextAttachment(HttpResponse response) {
        return response != null && response.fileAttachment && !response.isRecognizedFile()
                && !FileExtensionUtil.isBinaryType(contentType(response));
    }

    /**
     * Content-Type Bộ ký tự được khai báo hoặc nếu không được khai báo hoặc không được nhận dạng UTF-8（Phù hợp với giá trị mặc định khi giải mã nội dung phản hồi).
     */
    public static Charset declaredCharset(HttpResponse response) {
        return charsetOf(contentType(response));
    }

    /**
     * Content-Type Bộ ký tự được khai báo bằng chuỗi hoặc nếu không được khai báo hoặc không được nhận dạng UTF-8。
     */
    public static Charset charsetOf(String contentType) {
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }
        for (String parameter : contentType.split(";")) {
            int eq = parameter.indexOf('=');
            if (eq > 0 && "charset".equalsIgnoreCase(parameter.substring(0, eq).trim())) {
                String name = parameter.substring(eq + 1).trim().replace("\"", "");
                try {
                    return Charset.forName(name);
                } catch (IllegalArgumentException e) {
                    return StandardCharsets.UTF_8;
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    /**
     * Khôi phục văn bản nội dung phản hồi thành byte trong bộ ký tự được khai báo.
     */
    public static byte[] encodeBody(HttpResponse response) {
        String body = response == null || response.body == null ? "" : response.body;
        return body.getBytes(declaredCharset(response));
    }

    /**
     * Liệu văn bản nội dung phản hồi có thể được khôi phục dễ dàng về byte gốc theo bộ ký tự đã khai báo hay không được sử dụng để xác định xem ví dụ chỉ lưu văn bản có thể khôi phục tệp hoàn chỉnh hay không.
     * Khi nội dung rơi vào một tập tin tạm thời {@link HttpResponse#body} Nó chỉ là một văn bản nhắc nhở và không thể được sử dụng để khôi phục.
     */
    public static boolean canRestoreBytesFromBody(HttpResponse response) {
        if (response == null || response.body == null
                || (response.filePath != null && !response.filePath.isBlank())) {
            return false;
        }
        // Việc không giữ lại các byte gốc cho biết văn bản đã được giải mã một cách dễ dàng UTF-8 văn bản
        return response.rawBodyBytes == null || Arrays.equals(encodeBody(response), response.rawBodyBytes);
    }

    /**
     * Khả năng tương thích với dữ liệu cũ: các tệp nhị phân đã được nhấn UTF-8 Giải mã và lưu dưới dạng văn bản.ASCII phần (ZIP Tên mục, .%PDF- hạng nhất) vẫn có thể nhận dạng được.
     *
     * @return Recognized extension (including dot), returned if unrecognized {@code null}
     */
    public static String detectFromLegacyText(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        return ResponseFileTypeDetector.detectExtension(body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Đánh dấu câu trả lời là “Chỉ có thông tin tập tin và không có nội dung tập tin”，Để khôi phục từ các ví dụ hoặc lịch sử.
     */
    public static void markContentOmitted(HttpResponse response, String extension, String fileName) {
        response.body = "";
        response.rawBodyBytes = null;
        response.fileContentOmitted = true;
        response.detectedFileExtension = blankToNull(extension);
        if (fileName != null) {
            response.fileName = blankToNull(fileName);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
