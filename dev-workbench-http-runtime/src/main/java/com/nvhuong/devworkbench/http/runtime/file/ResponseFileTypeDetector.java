package com.nvhuong.devworkbench.http.runtime.file;

import com.nvhuong.devworkbench.util.FileExtensionUtil;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Loại tệp được xác định dựa trên các byte ở đầu phần nội dung phản hồi và kết quả chỉ được sử dụng cho lời nhắc giao diện và tên tệp mặc định.
 * <p>
 * Chỉ nhìn vào tiêu đề tập tin và một đoạn nhỏ ở đầu (nhiều nhất là  {@link #HEAD_BYTES} byte), không đi qua thư mục gói nén hoặc cấu trúc tài liệu phức hợp: nó sẽ được trả về nếu không thể xác định được {@code null}，
 * Không có ngoại lệ sẽ được ném. Tải xuống luôn lưu byte thô, bất kể kết quả nhận dạng.
 * </p>
 */
@UtilityClass
public class ResponseFileTypeDetector {

    /** The maximum number of bytes viewed during identification. */
    static final int HEAD_BYTES = 64 * 1024;
    /** WHATWG MIME Sniffing Số byte cần xem xét khi đánh giá nhị phân. */
    private static final int BINARY_SNIFF_BYTES = 1445;

    private static final byte[] PDF = ascii("%PDF-");
    private static final byte[] ZIP = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] ZIP_EMPTY = {0x50, 0x4B, 0x05, 0x06};
    private static final byte[] OLE2 = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final byte[] GZIP = {0x1F, (byte) 0x8B, 0x08};
    private static final byte[] SEVEN_ZIP = {0x37, 0x7A, (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C};
    private static final byte[] RAR = {0x52, 0x61, 0x72, 0x21, 0x1A, 0x07};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87A = ascii("GIF87a");
    private static final byte[] GIF89A = ascii("GIF89a");
    private static final byte[] RIFF = ascii("RIFF");
    private static final byte[] WEBP = ascii("WEBP");

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16_BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] UTF16_LE_BOM = {(byte) 0xFF, (byte) 0xFE};

    /** Office Open XML：ZIP Tên mục nhập theo sau tiêu đề tệp cục bộ và có thể được phân biệt theo tên. */
    private static final byte[] OOXML_CONTENT_TYPES = ascii("[Content_Types].xml");
    private static final byte[][] OOXML_PARTS = {
            ascii("xl/workbook.bin"), ascii("xl/workbook.xml"), ascii("word/document.xml"), ascii("ppt/presentation.xml")
    };
    private static final String[] OOXML_EXTENSIONS = {".xlsb", ".xlsx", ".docx", ".pptx"};

    private static final byte[] OFD_ENTRY = ascii("OFD.xml");

    /** OOXML：[Content_Types].xml Loại nội dung của thành phần chính, xác định các định dạng và macro tiêu chuẩn/Các biến thể của mẫu. */
    private static final Map<String, String> OOXML_MAIN_TYPES = Map.ofEntries(
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml", ".xlsx"),
            Map.entry("application/vnd.ms-excel.sheet.binary.macroEnabled.main", ".xlsb"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml", ".docx"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml", ".pptx"),
            Map.entry("application/vnd.ms-excel.sheet.macroEnabled.main+xml", ".xlsm"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.template.main+xml", ".xltx"),
            Map.entry("application/vnd.ms-excel.template.macroEnabled.main+xml", ".xltm"),
            Map.entry("application/vnd.ms-excel.addin.macroEnabled.main+xml", ".xlam"),
            Map.entry("application/vnd.ms-word.document.macroEnabled.main+xml", ".docm"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.template.main+xml", ".dotx"),
            Map.entry("application/vnd.ms-word.template.macroEnabledTemplate.main+xml", ".dotm"),
            Map.entry("application/vnd.ms-powerpoint.presentation.macroEnabled.main+xml", ".pptm"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.slideshow.main+xml", ".ppsx"),
            Map.entry("application/vnd.ms-powerpoint.slideshow.macroEnabled.main+xml", ".ppsm"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.template.main+xml", ".potx"),
            Map.entry("application/vnd.ms-powerpoint.template.macroEnabled.main+xml", ".potm"));

    /** ODF/EPUB：Đặc điểm kỹ thuật yêu cầu mục đầu tiên không được nén mimetype tập tin và nội dung của nó {@link FileExtensionUtil} Phần mở rộng bản đồ. */
    private static final byte[] ODF_MIMETYPE_ENTRY = ascii("mimetype");

    /** Phiên bản cũ Office Tài liệu tổng hợp (OLE2）：Nổi tiếng vì UTF-16LE Được lưu trữ trong thư mục;Book Vâng Excel 5/95 luồng sổ làm việc. */
    private static final byte[][] OLE2_STREAMS = {
            utf16le("WordDocument"), utf16le("PowerPoint Document"), utf16le("Workbook"), utf16le("Book")
    };
    private static final String[] OLE2_EXTENSIONS = {".doc", ".ppt", ".xls", ".xls"};

    /** OOXML Phần mở rộng cơ sở phổ biến được sử dụng để liệt kê các hậu tố tương thích với vùng chứa. */
    private static final Set<String> OOXML_EXTENSIONS_ALL = Set.of(
            ".xlsx", ".xlsm", ".xlsb", ".xltx", ".xltm", ".xlam", ".docx", ".docm", ".dotx", ".dotm",
            ".pptx", ".pptm", ".ppsx", ".ppsm", ".potx", ".potm");

    /**
     * Các tiện ích mở rộng hiện có có thể được giữ lại (danh sách được phép) khi chỉ các vùng chứa chung được nhận dạng: chính các định dạng này ZIP / OLE2 thùng chứa,
     * Tiêu đề tập tin không thể được phân biệt thêm. Các hậu tố còn lại (.yaml/.exe/.mp4/.png v.v.) phải tuân theo kết quả công nhận.
     */
    private static final Map<String, Set<String>> CONTAINER_COMPATIBLE = Map.of(
            ".zip", union(OOXML_EXTENSIONS_ALL, Set.of(
                    ".jar", ".war", ".ear", ".apk", ".aar", ".aab", ".xapk", ".apks", ".ipa", ".xpi", ".crx",
                    ".whl", ".egg", ".nupkg", ".vsix", ".msix", ".msixbundle", ".appx", ".appxbundle",
                    ".epub", ".ofd", ".xps", ".oxps", ".3mf", ".kmz", ".cbz", ".sketch", ".xmind", ".idml",
                    ".pbix", ".pbit", ".twbx", ".hwpx", ".usdz", ".pages", ".numbers", ".key",
                    ".odt", ".ods", ".odp", ".odg", ".odc", ".odf", ".odb", ".odm", ".ott", ".ots", ".otp", ".otg")),
            // OLE2：Outlook Email, gói cài đặt,Visio/Publisher/Project、WPS/Hàn Quốc Office，Và được mã hóa OOXML
            ".bin", union(OOXML_EXTENSIONS_ALL, Set.of(
                    ".msg", ".msi", ".msp", ".vsd", ".pub", ".mpp", ".wps", ".et", ".dps", ".hwp",
                    ".doc", ".dot", ".xls", ".xlt", ".xla", ".ppt", ".pps", ".pot")));

    /** Các cách viết khác nhau có cùng định dạng luôn giữ lại phần mở rộng hiện có. */
    private static final Map<String, Set<String>> SYNONYMS = Map.of(
            ".jpg", Set.of(".jpeg", ".jpe", ".jfif"),
            ".gz", Set.of(".tgz", ".gzip"));

    /**
     * Office Vĩ mô/Biến thể mẫu: Kết quả nhận dạng không được xác nhận (chỉ định dạng cơ bản được nhận dạng theo từ khóa .xlsx、hoặc OLE2 của .xls）giờ,
     * Giữ các tiện ích mở rộng hiện có cụ thể hơn (.xlsm）；Đã thực hiện [Content_Types].xml Kết quả nhận dạng sẽ được ưu tiên áp dụng khi xác nhận.
     */
    private static final Map<String, Set<String>> OFFICE_VARIANTS = Map.of(
            ".xlsx", Set.of(".xlsm", ".xltx", ".xltm", ".xlam"),
            ".docx", Set.of(".docm", ".dotx", ".dotm"),
            ".pptx", Set.of(".pptm", ".ppsx", ".ppsm", ".potx", ".potm"),
            ".xls", Set.of(".xlt", ".xla"),
            ".doc", Set.of(".dot"),
            ".ppt", Set.of(".pps", ".pot"));

    /**
     * Recognition results.
     *
     * @param extension Recognized extension (including dot)
     * @param exact     Liệu loại phụ cụ thể đã được xác định hay chưa; vâng {@code false} có thể cụ thể hơn Office Vĩ mô/Các biến thể của mẫu
     */
    public record Detection(String extension, boolean exact) {
    }

    /**
     * Trả về phần mở rộng được nhận dạng (bao gồm cả dấu chấm), nếu không thể xác định được {@code null}。
     * ZIP、OLE2 Nếu không thể xác định được loại tài liệu cụ thể, nó sẽ được trả về riêng {@code .zip}、{@code .bin}。
     */
    public static String detectExtension(byte[] bytes) {
        return extensionOf(detect(bytes));
    }

    /**
     * Xác định nội dung và trả lại nếu không thể xác định được {@code null}，Không có ngoại lệ sẽ được ném.
     */
    public static Detection detect(byte[] bytes) {
        if (bytes == null || bytes.length < 3) {
            return null;
        }
        try {
            return detectSignature(bytes);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Identify the response file that has been placed on disk and only read the beginning {@link #HEAD_BYTES} Byte; được trả về nếu đọc không thành công {@code null}。
     */
    public static String detectExtension(Path file) {
        return extensionOf(detect(file));
    }

    /**
     * Identify the response file that has been placed on disk and only read the beginning {@link #HEAD_BYTES} Byte; được trả về nếu đọc không thành công {@code null}。
     */
    public static Detection detect(Path file) {
        if (file == null) {
            return null;
        }
        try (InputStream in = Files.newInputStream(file)) {
            return detect(in.readNBytes(HEAD_BYTES));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static String extensionOf(Detection detection) {
        return detection == null ? null : detection.extension();
    }

    /**
     * Blend đã có phần mở rộng (từ  Content-Type hoặc URL）Với kết quả nhận dạng tiêu đề tệp, hãy trả về phần mở rộng nên sử dụng:
     * <ul>
     *     <li>Cả hai đều nhất quán hoặc được viết ở cùng định dạng (.jpeg/.jpg），Giữ các tiện ích mở rộng hiện có</li>
     *     <li>The recognition result is that the subtype is not confirmed and the existing extension is its macro/biến thể mẫu (được xác định .xlsx、Đã có rồi .xlsm）, phần mở rộng hiện tại được giữ lại</li>
     *     <li>Chỉ các vùng chứa phổ quát (.zip/.bin）Khi , chỉ các định dạng vùng chứa từ danh sách được phép (.jar、.docx、.msg Chờ đợi)</li>
     *     <li>Trong các trường hợp khác, tiêu đề tệp sẽ chiếm ưu thế (/photo.png Trên thực tế là có PDF → .pdf；tuyên bố .xlsx Trên thực tế là có .xlsm → .xlsm；
     *     tuyên bố .xlsm Và [Content_Types].xml Xác nhận là tiêu chuẩn xlsx → .xlsx）</li>
     * </ul>
     */
    public static String reconcile(String claimedExtension, Detection detection) {
        if (detection == null) {
            return claimedExtension;
        }
        String detectedExtension = detection.extension();
        if (claimedExtension == null) {
            return detectedExtension;
        }
        String claimed = claimedExtension.toLowerCase(Locale.ROOT);
        if (claimed.equals(detectedExtension)
                || SYNONYMS.getOrDefault(detectedExtension, Set.of()).contains(claimed)
                || (!detection.exact() && OFFICE_VARIANTS.getOrDefault(detectedExtension, Set.of()).contains(claimed))
                || CONTAINER_COMPATIBLE.getOrDefault(detectedExtension, Set.of()).contains(claimed)) {
            return claimedExtension;
        }
        return detectedExtension;
    }

    /**
     * Đối chiếu kết quả nhận dạng theo kiểu con chưa được xác nhận, xem {@link #reconcile(String, Detection)}。
     */
    public static String reconcile(String claimedExtension, String detectedExtension) {
        return reconcile(claimedExtension, detectedExtension == null ? null : new Detection(detectedExtension, false));
    }

    /**
     * The recognition result is only a general container (.zip/.bin），Không thể xác định định dạng chính xác.
     */
    public static boolean isGenericContainer(String extension) {
        return extension != null && CONTAINER_COMPATIBLE.containsKey(extension);
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> result = new java.util.HashSet<>(first);
        result.addAll(second);
        return Set.copyOf(result);
    }

    /**
     * Nhấn WHATWG MIME Sniffing Phán quyết nhị phân: Khi một byte điều khiển không xuất hiện trong văn bản lại xuất hiện trong đoạn đầu tiên, nó được coi là nhị phân.
     * mang đến UTF-8/UTF-16 BOM Nội dung được coi là văn bản.
     */
    public static boolean looksBinary(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        if (startsWith(bytes, UTF8_BOM) || startsWith(bytes, UTF16_BE_BOM) || startsWith(bytes, UTF16_LE_BOM)) {
            return false;
        }
        int limit = Math.min(bytes.length, BINARY_SNIFF_BYTES);
        for (int i = 0; i < limit; i++) {
            int b = bytes[i] & 0xFF;
            if (b <= 0x08 || b == 0x0B || (b >= 0x0E && b <= 0x1A) || (b >= 0x1C && b <= 0x1F)) {
                return true;
            }
        }
        return false;
    }

    private static Detection detectSignature(byte[] bytes) {
        String extension = signatureExtension(bytes);
        if (extension == null) {
            return null;
        }
        if (startsWith(bytes, ZIP) || startsWith(bytes, ZIP_EMPTY)) {
            return detectZip(bytes);
        }
        // OLE2 Không thể phân biệt được .xls Và .xlt và các mẫu khác
        return new Detection(extension, !startsWith(bytes, OLE2));
    }

    private static String signatureExtension(byte[] bytes) {
        if (startsWith(bytes, PDF)) {
            return ".pdf";
        }
        if (startsWith(bytes, ZIP) || startsWith(bytes, ZIP_EMPTY)) {
            return ".zip";
        }
        if (startsWith(bytes, OLE2)) {
            return firstContained(bytes, OLE2_STREAMS, OLE2_EXTENSIONS, ".bin");
        }
        if (startsWith(bytes, GZIP)) {
            return ".gz";
        }
        if (startsWith(bytes, SEVEN_ZIP)) {
            return ".7z";
        }
        if (startsWith(bytes, RAR)) {
            return ".rar";
        }
        if (startsWith(bytes, PNG)) {
            return ".png";
        }
        if (startsWith(bytes, JPEG)) {
            return ".jpg";
        }
        if (startsWith(bytes, GIF87A) || startsWith(bytes, GIF89A)) {
            return ".gif";
        }
        if (startsWith(bytes, RIFF) && regionMatches(bytes, 8, WEBP)) {
            return ".webp";
        }
        return null;
    }

    private static Detection detectZip(byte[] bytes) {
        String odf = odfMimeType(bytes);
        if (odf != null) {
            boolean known = odf.startsWith("application/vnd.oasis.opendocument.") || "application/epub+zip".equals(odf);
            String extension = known ? FileExtensionUtil.guessExtension(odf) : null;
            return new Detection(extension != null ? extension : ".zip", true);
        }
        if (contains(bytes, OOXML_CONTENT_TYPES)) {
            String mainType = ooxmlVariant(bytes);
            return mainType != null
                    ? new Detection(mainType, true)
                    : new Detection(firstContained(bytes, OOXML_PARTS, OOXML_EXTENSIONS, ".zip"), false);
        }
        return new Detection(contains(bytes, OFD_ENTRY) ? ".ofd" : ".zip", true);
    }

    /** Mục đầu tiên được đặt tên mimetype Khi giải nén, nội dung của nó sẽ được trả về. */
    private static String odfMimeType(byte[] bytes) {
        if (bytes.length < 38 || u16(bytes, 8) != 0 || !regionMatches(bytes, 30, ODF_MIMETYPE_ENTRY)
                || u16(bytes, 26) != ODF_MIMETYPE_ENTRY.length) {
            return null;
        }
        int start = 30 + ODF_MIMETYPE_ENTRY.length + u16(bytes, 28);
        int size = u16(bytes, 18);
        if (size <= 0 || size > 100 || u16(bytes, 20) != 0 || start + size > bytes.length) {
            return null;
        }
        return new String(bytes, start, size, StandardCharsets.US_ASCII);
    }

    /** Tìm thấy trong đoạn mở đầu [Content_Types].xml tiêu đề tệp cục bộ và giải nén nó, đồng thời xác định định dạng cụ thể theo loại nội dung của thành phần chính. */
    private static String ooxmlVariant(byte[] bytes) {
        int header = contentTypesLocalHeader(bytes);
        if (header < 0) {
            return null;
        }
        int dataStart = header + 30 + OOXML_CONTENT_TYPES.length + u16(bytes, header + 28);
        int available = Math.min(bytes.length, HEAD_BYTES) - dataStart;
        if (available <= 0) {
            return null;
        }
        String contentTypes;
        int method = u16(bytes, header + 8);
        if (method == 0) {
            contentTypes = new String(bytes, dataStart, available, StandardCharsets.UTF_8);
        } else if (method == 8) {
            Inflater inflater = new Inflater(true);
            try {
                inflater.setInput(bytes, dataStart, available);
                byte[] out = new byte[HEAD_BYTES];
                int length = inflater.inflate(out);
                contentTypes = new String(out, 0, length, StandardCharsets.UTF_8);
            } catch (DataFormatException e) {
                return null;
            } finally {
                inflater.end();
            }
        } else {
            return null;
        }
        for (Map.Entry<String, String> variant : OOXML_MAIN_TYPES.entrySet()) {
            if (contentTypes.contains(variant.getKey())) {
                return variant.getValue();
            }
        }
        return null;
    }

    /** Tên mục nhập sau 30 byte sau tiêu đề tệp cục bộ; trả về phần bù của tiêu đề tệp nếu không tìm thấy -1。 */
    private static int contentTypesLocalHeader(byte[] bytes) {
        int limit = Math.min(bytes.length, HEAD_BYTES) - OOXML_CONTENT_TYPES.length;
        for (int name = 30; name <= limit; name++) {
            int header = name - 30;
            if (regionMatches(bytes, name, OOXML_CONTENT_TYPES) && regionMatches(bytes, header, ZIP)
                    && u16(bytes, header + 26) == OOXML_CONTENT_TYPES.length) {
                return header;
            }
        }
        return -1;
    }

    private static int u16(byte[] bytes, int offset) {
        return offset + 1 < bytes.length ? (bytes[offset] & 0xFF) | (bytes[offset + 1] & 0xFF) << 8 : -1;
    }

    private static String firstContained(byte[] bytes, byte[][] markers, String[] extensions, String fallback) {
        for (int i = 0; i < markers.length; i++) {
            if (contains(bytes, markers[i])) {
                return extensions[i];
            }
        }
        return fallback;
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return regionMatches(bytes, 0, prefix);
    }

    private static boolean regionMatches(byte[] bytes, int offset, byte[] target) {
        if (offset < 0 || offset + target.length > bytes.length) {
            return false;
        }
        for (int i = 0; i < target.length; i++) {
            if (bytes[offset + i] != target[i]) {
                return false;
            }
        }
        return true;
    }

    /** Chỉ lúc đầu thôi {@link #HEAD_BYTES} Tìm kiếm trong byte và chỉ quét phần này của nội dung phản hồi lớn trong bộ nhớ. */
    private static boolean contains(byte[] bytes, byte[] target) {
        int limit = Math.min(bytes.length, HEAD_BYTES) - target.length;
        for (int i = 0; i <= limit; i++) {
            if (regionMatches(bytes, i, target)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] utf16le(String value) {
        return value.getBytes(StandardCharsets.UTF_16LE);
    }
}
