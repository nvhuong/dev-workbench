package com.nvhuong.devworkbench.http.runtime.file;

import com.nvhuong.devworkbench.util.FileExtensionUtil;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phản hồi độ phân giải tên tệp tải xuống:Content-Disposition、URL Các quy tắc lựa chọn giữa kết quả nhận dạng đường dẫn và nội dung được tập trung ở đây.
 */
@UtilityClass
public class ResponseFileNames {

    public static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    /**
     * Trích xuất từ ​​tiêu đề phản hồi Content-Disposition（Không phân biệt chữ hoa chữ thường), được trả về nếu nó không tồn tại {@code null}。
     */
    public static String getContentDisposition(Map<String, List<String>> headers) {
        return ResponseFiles.firstHeader(headers, CONTENT_DISPOSITION_HEADER);
    }

    /**
     * Content-Disposition vì attachment , máy chủ rõ ràng muốn tải xuống dưới dạng tệp.
     * <p>
     * {@code inline; filename=...} Không tính: ví dụ: Spring MVC Sẽ cho {@code /list.do} của JSON Phản hồi cộng
     * {@code inline;filename=f.txt} Chỉ trong trường hợp RFD tấn công, những phản hồi như vậy vẫn phải được hiển thị dưới dạng văn bản thuần túy.
     * </p>
     */
    public static boolean isAttachment(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isBlank()) {
            return false;
        }
        String type = contentDisposition.split(";", 2)[0].trim();
        return "attachment".equalsIgnoreCase(type);
    }

    /**
     * Nhấn RFC 6266 Từ Content-Disposition Tên tệp được phân tích cú pháp và đường dẫn bị xóa và các ký tự không được hệ thống tệp cho phép sẽ được thay thế.
     * <ol>
     *     <li>{@code filename*}（RFC 5987，Chẳng hạn như {@code UTF-8''%E6%8A%A5%E8%A1%A8.xlsx}）Ưu tiên giải mã theo bộ ký tự được khai báo</li>
     *     <li>Nếu không thì sử dụng {@code filename}；Tương thích với các thông lệ trình duyệt phổ biến và giải mã các thông lệ hợp pháp UTF-8 Mã hóa phần trăm
     *     （Được sử dụng bởi nhiều máy chủ {@code URLEncoder} tên tệp tiếng Trung được mã hóa) và RFC 2047 Từ được mã hóa ({@code =?UTF-8?B?..?=}）</li>
     * </ol>
     *
     * @return Tên tệp được phân tích cú pháp được trả về khi không thể phân tích cú pháp được {@code null}
     */
    public static String parseFileNameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isBlank()) {
            return null;
        }
        Map<String, String> parameters = dispositionParameters(contentDisposition);
        String extended = decodeExtendedValue(parameters.get("filename*"));
        String fileName = sanitize(extended);
        if (fileName == null) {
            String plain = parameters.get("filename");
            fileName = plain == null ? null : sanitize(decodeEncodedWords(percentDecodeUtf8(plain)));
        }
        return fileName;
    }

    /**
     * Đặt máy chủ hoặc URL Tên đã cho sẽ trở thành tên tệp an toàn để sử dụng làm tên lưu mặc định: .
     * Xóa phần thư mục (RFC 6266 bắt buộc), thay thế các ký tự điều khiển và Windows Đối với các ký tự không được phép, khoảng trắng ở đầu và cuối cũng như dấu chấm ở cuối sẽ bị xóa.
     *
     * @return Tên tệp được xử lý; được trả về nếu không có ký tự nào {@code null}
     */
    public static String sanitize(String name) {
        if (name == null) {
            return null;
        }
        String baseName = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        StringBuilder result = new StringBuilder(baseName.length());
        for (int i = 0; i < baseName.length(); i++) {
            char c = baseName.charAt(i);
            result.append(c < 0x20 || c == 0x7F || INVALID_FILE_NAME_CHARS.indexOf(c) >= 0 ? '_' : c);
        }
        String cleaned = result.toString().strip();
        while (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).strip();
        }
        if (cleaned.isEmpty() || cleaned.chars().allMatch(c -> c == '_')) {
            return null;
        }
        String stem = cleaned.contains(".") ? cleaned.substring(0, cleaned.indexOf('.')) : cleaned;
        if (WINDOWS_RESERVED_NAMES.contains(stem.toUpperCase(Locale.ROOT))) {
            cleaned = "_" + cleaned;
        }
        return cleaned.length() > MAX_FILE_NAME_LENGTH ? truncateKeepingExtension(cleaned) : cleaned;
    }

    private static final String INVALID_FILE_NAME_CHARS = "<>:\"|?*";
    private static final int MAX_FILE_NAME_LENGTH = 200;
    private static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private static String truncateKeepingExtension(String name) {
        String extension = extensionOf(name);
        if (extension == null || extension.length() > 16) {
            return name.substring(0, MAX_FILE_NAME_LENGTH);
        }
        return name.substring(0, MAX_FILE_NAME_LENGTH - extension.length()) + extension;
    }

    /**
     * phân tích disposition Các tham số (tên viết thường), hỗ trợ các giá trị được trích dẫn và thoát dấu gạch chéo ngược; tham số đầu tiên có cùng tên sẽ được ưu tiên áp dụng.
     */
    private static Map<String, String> dispositionParameters(String contentDisposition) {
        Map<String, String> parameters = new HashMap<>();
        int length = contentDisposition.length();
        int i = contentDisposition.indexOf(';');
        String dispositionType = contentDisposition.substring(0, i < 0 ? length : i);
        if (dispositionType.contains("=")) {
            // Bị bỏ qua một cách bất thường disposition Nhập, ví dụ "filename=report.xlsx"
            i = -1;
        } else if (i < 0) {
            return parameters;
        }
        do {
            i++;
            int nameStart = i;
            while (i < length && contentDisposition.charAt(i) != '=' && contentDisposition.charAt(i) != ';') {
                i++;
            }
            String name = contentDisposition.substring(nameStart, i).trim().toLowerCase(Locale.ROOT);
            if (i >= length || contentDisposition.charAt(i) == ';') {
                continue;
            }
            i++;
            while (i < length && Character.isWhitespace(contentDisposition.charAt(i))) {
                i++;
            }
            String value;
            if (i < length && contentDisposition.charAt(i) == '"') {
                StringBuilder quoted = new StringBuilder();
                i++;
                while (i < length && contentDisposition.charAt(i) != '"') {
                    char c = contentDisposition.charAt(i);
                    if (c == '\\' && i + 1 < length) {
                        c = contentDisposition.charAt(++i);
                    }
                    quoted.append(c);
                    i++;
                }
                value = quoted.toString();
                i = contentDisposition.indexOf(';', i);
            } else {
                int end = contentDisposition.indexOf(';', i);
                value = contentDisposition.substring(i, end < 0 ? length : end).trim();
                i = end;
            }
            if (!name.isEmpty()) {
                parameters.putIfAbsent(name, value);
            }
        } while (i >= 0 && i < length);
        return parameters;
    }

    /**
     * giải mã RFC 5987 giá trị mở rộng {@code charset'language'percent-encoded}，Trả về nếu định dạng hoặc bộ ký tự không hợp lệ {@code null}。
     */
    private static String decodeExtendedValue(String value) {
        if (value == null) {
            return null;
        }
        int firstQuote = value.indexOf('\'');
        int secondQuote = firstQuote < 0 ? -1 : value.indexOf('\'', firstQuote + 1);
        if (secondQuote < 0) {
            return null;
        }
        try {
            Charset charset = Charset.forName(value.substring(0, firstQuote).trim());
            byte[] bytes = percentDecode(value.substring(secondQuote + 1));
            return bytes == null ? null : strictDecode(bytes, charset);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Chỉ mã hóa phần trăm tồn tại và kết quả giải mã là hợp pháp UTF-8 Chỉ giải mã khi cần thiết để tránh đặt chúng vào tên tệp thông thường % Vỡ.
     */
    private static String percentDecodeUtf8(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        byte[] bytes = percentDecode(value);
        String decoded = bytes == null ? null : strictDecode(bytes, StandardCharsets.UTF_8);
        return decoded == null ? value : decoded;
    }

    private static byte[] percentDecode(String value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%') {
                if (i + 2 >= value.length()) {
                    return null;
                }
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    return null;
                }
                out.write(high << 4 | low);
                i += 2;
            } else {
                out.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
            }
        }
        return out.toByteArray();
    }

    private static final Pattern ENCODED_WORD =
            Pattern.compile("=\\?([^?]+)\\?([BbQq])\\?([^?]*)\\?=");

    /**
     * giải mã RFC 2047 Các từ được mã hóa (một số máy chủ tuân theo phương pháp viết email, chẳng hạn như {@code =?UTF-8?B?5oql6KGoLnhsc3g=?=}）。
     */
    private static String decodeEncodedWords(String value) {
        if (!value.contains("=?")) {
            return value;
        }
        Matcher matcher = ENCODED_WORD.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String decoded;
            try {
                Charset charset = Charset.forName(matcher.group(1));
                byte[] bytes = "B".equalsIgnoreCase(matcher.group(2))
                        ? Base64.getDecoder().decode(matcher.group(3))
                        : percentDecode(matcher.group(3).replace('_', ' ').replace('=', '%'));
                decoded = bytes == null ? null : strictDecode(bytes, charset);
            } catch (IllegalArgumentException e) {
                decoded = null;
            }
            matcher.appendReplacement(result,
                    Matcher.quoteReplacement(decoded == null ? matcher.group() : decoded));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String strictDecode(byte[] bytes, Charset charset) {
        try {
            return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /**
     * Phân tích tên tệp đã tải xuống và sử dụng nó khi không thể xác định được nội dung thông qua tiêu đề tệp.
     *
     * @see #resolve(String, String, String, String)
     */
    public static String resolve(String contentDisposition, String urlEncodedPath, String extension) {
        return resolve(contentDisposition, urlEncodedPath, extension, (ResponseFileTypeDetector.Detection) null);
    }

    /**
     * Phân tích tên tệp đã tải xuống.
     * <ol>
     *     <li>Content-Disposition Khi mang tên tệp, nó được sử dụng trước và khi thiếu phần mở rộng, nó sẽ được thêm vào {@code extension}</li>
     *     <li>Nếu không thì sử dụng URL Đoạn cuối: thêm nếu không có phần mở rộng {@code extension}；
     *     Tuyến giao diện hoặc hậu tố lớp văn bản (/export.do、/download.json）Thay thế bằng  {@code extension}；
     *     Nhấn khi các hậu tố khác xung đột với kết quả nhận dạng tiêu đề tệp {@link ResponseFileTypeDetector#reconcile} Đúng(/photo.png Trên thực tế là có PDF → photo.pdf）</li>
     *     <li>Tạo tên tệp thông minh được đánh dấu thời gian khi không có sẵn</li>
     * </ol>
     *
     * @param contentDisposition Content-Disposition Tiêu đề phản hồi, có thể trống
     * @param urlEncodedPath     yêu cầu URL của path，Có thể trống
     * @param extension          Phần mở rộng tương ứng với nội dung (bao gồm cả dấu chấm) có thể để trống
     * @param detectedExtension  Phần mở rộng được nhận dạng bởi tiêu đề tệp (bao gồm cả dấu chấm), có thể trống
     */
    public static String resolve(String contentDisposition,
                                 String urlEncodedPath,
                                 String extension,
                                 String detectedExtension) {
        return resolve(contentDisposition, urlEncodedPath, extension,
                detectedExtension == null ? null : new ResponseFileTypeDetector.Detection(detectedExtension, false));
    }

    /**
     * Giống nhau {@link #resolve(String, String, String, String)}，Đối chiếu bằng cách sử dụng kết quả nhận dạng với thông báo xác nhận URL hậu tố.
     */
    public static String resolve(String contentDisposition,
                                 String urlEncodedPath,
                                 String extension,
                                 ResponseFileTypeDetector.Detection detection) {
        String detectedExtension = detection == null ? null : detection.extension();
        String target = extension != null ? extension : detectedExtension;
        String fileName = parseFileNameFromContentDisposition(contentDisposition);
        if (fileName != null && !fileName.isBlank()) {
            return fileName.contains(".") || target == null ? fileName : fileName + target;
        }

        fileName = fileNameFromUrlPath(urlEncodedPath);
        fileName = fileName == null ? null : sanitize(percentDecodeUtf8(fileName));
        if (fileName == null) {
            return FileExtensionUtil.generateSmartFileName(target);
        }
        String urlExtension = extensionOf(fileName);
        if (target == null) {
            return fileName;
        }
        if (urlExtension == null) {
            return fileName + target;
        }
        String baseName = fileName.substring(0, fileName.length() - urlExtension.length());
        if (REPLACEABLE_URL_EXTENSIONS.contains(urlExtension.toLowerCase(Locale.ROOT))) {
            return target.equalsIgnoreCase(urlExtension) ? fileName : baseName + target;
        }
        String reconciled = ResponseFileTypeDetector.reconcile(urlExtension, detection);
        return reconciled.equals(urlExtension) ? fileName : baseName + reconciled;
    }

    /**
     * Lấy URL path Đoạn cuối cùng được sử dụng làm tên tệp và bị xóa {@code ! ? #} Phần sau;path qua / Trở lại vào cuối {@code null}。
     */
    public static String fileNameFromUrlPath(String path) {
        if (path == null) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        String rawName = lastSlash < 0 ? path : path.substring(lastSlash + 1);
        if (rawName.isEmpty()) {
            return null;
        }
        int cut = rawName.length();
        for (char delimiter : new char[]{'!', '?', '#'}) {
            int index = rawName.indexOf(delimiter);
            if (index >= 0 && index < cut) {
                cut = index;
            }
        }
        return rawName.substring(0, cut);
    }

    /**
     * Trả về phần mở rộng (bao gồm dấu chấm) trong tên tệp hoặc trả về nếu không có phần mở rộng {@code null}。
     */
    public static String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1 ? fileName.substring(dot) : null;
    }

    /** Hậu tố định tuyến giao diện và hậu tố lớp văn bản: không thể hiện loại tệp thực tế được tải xuống và có thể được thay thế bằng nội dung. */
    private static final Set<String> REPLACEABLE_URL_EXTENSIONS = Set.of(
            ".do", ".action", ".php", ".jsp", ".jspx", ".asp", ".aspx", ".ashx", ".cgi", ".pl", ".py",
            ".rb", ".cfm", ".json", ".txt", ".html", ".htm", ".xhtml", ".xml", ".csv", ".js");

}
