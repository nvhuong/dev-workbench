package com.nvhuong.devworkbench.util;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.Map;

/**
 * Lớp tiện ích mở rộng tập tin
 * Cơ sở quản lý thống nhất Content-Type Logic để suy ra phần mở rộng tập tin
 */
@UtilityClass
public class FileExtensionUtil {

    private static final Map<String, String> EXACT_MEDIA_TYPES = Map.ofEntries(
            Map.entry("application/vnd.ms-excel.sheet.macroenabled.12", ".xlsm"),
            Map.entry("application/vnd.ms-excel.sheet.binary.macroenabled.12", ".xlsb"),
            Map.entry("application/vnd.ms-excel.template.macroenabled.12", ".xltm"),
            Map.entry("application/vnd.ms-excel.addin.macroenabled.12", ".xlam"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.template", ".xltx"),
            Map.entry("application/vnd.ms-word.document.macroenabled.12", ".docm"),
            Map.entry("application/vnd.ms-word.template.macroenabled.12", ".dotm"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.template", ".dotx"),
            Map.entry("application/vnd.ms-powerpoint.presentation.macroenabled.12", ".pptm"),
            Map.entry("application/vnd.ms-powerpoint.template.macroenabled.12", ".potm"),
            Map.entry("application/vnd.ms-powerpoint.slideshow.macroenabled.12", ".ppsm"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.template", ".potx"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.slideshow", ".ppsx"),
            Map.entry("application/vnd.oasis.opendocument.text", ".odt"),
            Map.entry("application/vnd.oasis.opendocument.text-template", ".ott"),
            Map.entry("application/vnd.oasis.opendocument.spreadsheet", ".ods"),
            Map.entry("application/vnd.oasis.opendocument.spreadsheet-template", ".ots"),
            Map.entry("application/vnd.oasis.opendocument.presentation", ".odp"),
            Map.entry("application/vnd.oasis.opendocument.presentation-template", ".otp"),
            Map.entry("application/vnd.oasis.opendocument.graphics", ".odg"),
            Map.entry("application/vnd.oasis.opendocument.graphics-template", ".otg"),
            Map.entry("application/vnd.oasis.opendocument.text-master", ".odm"),
            Map.entry("application/vnd.oasis.opendocument.chart", ".odc"),
            Map.entry("application/vnd.oasis.opendocument.formula", ".odf"),
            Map.entry("application/vnd.oasis.opendocument.base", ".odb"),
            Map.entry("application/ofd", ".ofd"),
            Map.entry("application/epub+zip", ".epub"),
            Map.entry("application/vnd.android.package-archive", ".apk"),
            Map.entry("application/java-archive", ".jar"),
            Map.entry("application/x-java-archive", ".jar"));

    /**
     * theo Content-Type Suy ra phần mở rộng tập tin
     *
     * @param contentType HTTP Content-Type cái đầu
     * @return Phần mở rộng tệp (bao gồm dấu chấm), được trả về nếu không thể xác định được null
     */
    public static String guessExtension(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return null;
        }

        String ct = mediaType(contentType);

        // Trận đấu chính xác Office Vĩ mô/Các biến thể của mẫu,ODF、Java gói (phải đặt trước bên dưới ms-excel đối sánh rộng)
        String exact = EXACT_MEDIA_TYPES.get(ct);
        if (exact != null) return exact;

        // Loại văn bản (ưu tiên xét xử, tránh bị xml/json đánh giá sai các loại phổ biến)
        if (ct.contains("pdf")) return ".pdf";
        if (ct.contains("msword") && !ct.contains("wordprocessingml")) return ".doc";
        if (ct.contains("wordprocessingml.document")) return ".docx";
        if (ct.contains("ms-excel") && !ct.contains("spreadsheetml")) return ".xls";
        if (ct.contains("spreadsheetml.sheet")) return ".xlsx";
        if (ct.contains("ms-powerpoint") && !ct.contains("presentationml")) return ".ppt";
        if (ct.contains("presentationml.presentation")) return ".pptx";

        // Loại hình ảnh (đánh giá trước loại văn bản, tránh svg+xml Hãy là xml phán đoán sai)
        if (ct.contains("image/png")) return ".png";
        if (ct.contains("image/jpeg")) return ".jpg";
        if (ct.contains("image/gif")) return ".gif";
        if (ct.contains("image/bmp")) return ".bmp";
        if (ct.contains("image/webp")) return ".webp";
        if (ct.contains("image/svg")) return ".svg";
        if (ct.contains("image/ico") || ct.contains("image/x-icon")) return ".ico";

        // Kiểu văn bản
        if (ct.contains("json")) return ".json";
        if (ct.contains("xml")) return ".xml";
        if (ct.contains("html")) return ".html";
        if (ct.contains("javascript")) return ".js";
        if (ct.contains("css")) return ".css";
        if (ct.contains("csv")) return ".csv";
        if (ct.contains("text/plain")) return ".txt";
        if (ct.contains("yaml") || ct.contains("yml")) return ".yaml";

        // Các tập tin nén
        if (ct.contains("application/zip") || ct.contains("x-zip")) return ".zip";
        if (ct.contains("gzip")) return ".gz";
        if (ct.contains("x-tar")) return ".tar";
        if (ct.contains("x-7z-compressed")) return ".7z";
        if (ct.contains("x-rar-compressed")) return ".rar";
        if (ct.contains("x-bzip2")) return ".bz2";
        if (ct.contains("x-xz")) return ".xz";

        // đa phương tiện
        if (ct.contains("audio/mpeg") || ct.contains("audio/mp3")) return ".mp3";
        if (ct.contains("audio/wav") || ct.contains("audio/x-wav")) return ".wav";
        if (ct.contains("audio/ogg")) return ".ogg";
        if (ct.contains("audio/aac")) return ".aac";
        if (ct.contains("audio/mp4") || ct.contains("audio/x-m4a")) return ".m4a";
        if (ct.contains("audio/flac") || ct.contains("audio/x-flac")) return ".flac";
        if (ct.contains("audio/aiff") || ct.contains("audio/x-aiff")) return ".aiff";
        if (ct.contains("audio/basic")) return ".au";
        if (ct.contains("audio/webm")) return ".webm";
        if (ct.contains("audio/opus")) return ".opus";
        if (ct.contains("video/mp4")) return ".mp4";
        if (ct.contains("video/mpeg")) return ".mpeg";
        if (ct.contains("video/webm")) return ".webm";
        if (ct.contains("video/x-flv")) return ".flv";

        // khác
        if (ct.contains("apk")) return ".apk";
        if (ct.contains("x-apple-diskimage")) return ".dmg";
        if (ct.contains("x-iso9660-image")) return ".iso";
        if (ct.contains("x-msdownload")) return ".exe";
        if (ct.contains("x-debian-package")) return ".deb";
        if (ct.contains("x-redhat-package-manager")) return ".rpm";
        if (ct.contains("x-shockwave-flash")) return ".swf";

        return null;
    }

    /**
     * Tạo tên tệp thông minh dựa trên phần mở rộng tệp
     *
     * @param extension Đuôi file (có thể có hoặc không có dấu chấm)
     * @return Tên tệp được tạo thông minh (bao gồm cả dấu thời gian)
     */
    public static String generateSmartFileName(String extension) {
        if (extension == null || extension.isEmpty()) {
            extension = ".txt";
        }

        // Xóa dấu chấm khỏi phần mở rộng (nếu có)
        String ext = extension.startsWith(".") ? extension.substring(1) : extension;
        String extLower = ext.toLowerCase();

        // Tạo tiền tố tên tệp thân thiện dựa trên phần mở rộng
        String prefix = getFilePrefixByExtension(extLower);

        // Sử dụng dấu thời gian để đảm bảo tên tệp là duy nhất
        long timestamp = System.currentTimeMillis();
        return prefix + "_" + timestamp + "." + ext;
    }

    /**
     * Nhận tiền tố tập tin dựa trên phần mở rộng
     */
    private static String getFilePrefixByExtension(String ext) {
        return switch (ext) {
            case "json", "csv" -> "response_data";
            case "xml" -> "response_xml";
            case "html" -> "response_page";
            case "js" -> "response_script";
            case "css" -> "response_style";
            case "txt" -> "response_text";
            case "pdf" -> "document";
            case "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> "document";
            case "png", "jpg", "jpeg", "gif", "bmp", "webp", "svg", "ico" -> "image";
            case "zip", "gz", "tar", "7z", "rar", "bz2", "xz" -> "archive";
            case "mp3", "wav", "ogg", "aac", "m4a", "flac", "aiff", "au", "opus" -> "audio";
            case "mp4", "mpeg", "webm", "flv" -> "video";
            case "apk", "dmg", "iso", "exe", "deb", "rpm" -> "package";
            default -> "response";
        };
    }

    /**
     * Từ Content-Type Trích xuất loại chính (ví dụ text、image、application Chờ đợi)
     */
    public static String extractMainType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return null;
        }

        int slashIndex = contentType.indexOf('/');
        if (slashIndex > 0) {
            return contentType.substring(0, slashIndex).trim();
        }

        return null;
    }

    /**
     * Kiểm tra xem nó có thuộc loại văn bản không Content-Type
     */
    public static boolean isTextType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return false;
        }

        String ct = mediaType(contentType);

        // Loại văn bản rõ ràng
        if (ct.startsWith("text/")) {
            return true;
        }

        // application Kiểu văn bản bên dưới
        // Lưu ý: Hãy sử dụng kết quả khớp chính xác để tránh đánh giá sai
        // - "application/xml" hoặc "text/xml" là văn bản
        // - Nhưng "image/svg+xml" hoặc "application/vnd...openxmlformats..." Không phải văn bản
        boolean isXml = ct.equals("application/xml") || ct.equals("text/xml");

        return ct.contains("json")
                || isXml
                || ct.contains("javascript")
                || ct.contains("x-www-form-urlencoded")
                || ct.contains("x-sh")
                || ct.contains("x-shellscript")
                || ct.contains("x-python")
                || (ct.contains("x-java") && !ct.contains("archive"));
    }

    /**
     * Kiểm tra xem nó có thuộc loại nhị phân không Content-Type
     */
    public static boolean isBinaryType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return false;
        }

        String ct = mediaType(contentType);
        if (ct.isEmpty()) {
            return false;
        }

        // Ưu tiên nhận dạng các loại văn bản có cấu trúc, ví dụ: application/vnd.*+json。
        if (isTextType(contentType)) {
            return false;
        }

        // Kiểm tra loại nhị phân rõ ràng.
        // Ví dụ:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet Chứa "xml" Nhưng nhị phân
        if (ct.contains("application/octet-stream")
                || ct.contains("image/")
                || ct.contains("audio/")
                || ct.contains("video/")
                || ct.contains("application/pdf")
                || ct.contains("application/zip")
                || ct.contains("application/gzip")
                || ct.contains("application/msword")
                || ct.contains("application/vnd.")) {
            return true;
        }

        return true;
    }

    /**
     * Lấy Content-Type Phần loại phương tiện (chữ thường, loại bỏ tham số), tránh {@code ; name=data.json} Các thông số như vậy ảnh hưởng đến sự phán đoán.
     */
    private static String mediaType(String contentType) {
        int semicolon = contentType.indexOf(';');
        return (semicolon >= 0 ? contentType.substring(0, semicolon) : contentType).trim().toLowerCase(Locale.ROOT);
    }
}
