package com.laker.postman.request.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Đối tượng phản hồi đã lưu
 * Tương tự Postman đã lưu vào Example phản hồi
 */
@Data
public class SavedResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id; // Phản hồi định danh duy nhất
    private String name; // Tên phản hồi
    private long timestamp; // Lưu dấu thời gian

    // Thông tin yêu cầu ban đầu (để hiển thị và so sánh)
    private OriginalRequest originalRequest;

    // Thông tin phản hồi
    private int code; // mã trạng thái
    private String status; // Mô tả trạng thái, chẳng hạn như "OK"
    private List<HttpHeader> headers = new ArrayList<>(); // tiêu đề phản hồi
    private List<CookieInfo> cookies = new ArrayList<>(); // Cookies
    private String body; // cơ quan phản ứng
    private String previewLanguage; // Xem trước loại ngôn ngữ, ví dụ "json", "html", "xml" Đợi đã
    /**
     * Thông tin meta cho phản hồi loại tệp. Không trống có nghĩa đây là phản hồi của tệp. Ví dụ chỉ lưu thông tin meta và không lưu nội dung tệp ({@link #body} trống).
     */
    private FileInfo file;

    // địa phương Mock Sự mở rộng. Vẫn ở trạng thái tĩnh khi trống Example hành vi.
    private int mockDelayMs;
    private String mockScript = "";

    // Thông tin hiệu suất
    private long costMs; // Thời gian thực hiện yêu cầu (mili giây)
    private long bodySize; // Kích thước nội dung phản hồi (byte)
    private long headersSize; // Kích thước tiêu đề phản hồi (byte)

    /**
     * Thông tin meta phản hồi tệp. Tệp nhị phân chỉ lưu siêu thông tin; các tệp đính kèm văn bản có thể được khôi phục dễ dàng, lưu thêm nội dung body ở giữa
     */
    @Data
    public static class FileInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String fileName; // Tải xuống tên tập tin
        private String extension; // Các tiện ích mở rộng (bao gồm cả dấu chấm) được xác định dựa trên nội dung, ví dụ: ".xlsx"；Trống nếu không được công nhận
        private boolean attachment; // Content-Disposition vì attachment
        private boolean contentInBody; // Nội dung tập tin được lưu ở dạng văn bản body Trung bình (đính kèm văn bản), có thể nhấn Content-Type Phục hồi bộ ký tự
    }

    /**
     * Thông tin yêu cầu ban đầu (ảnh chụp nhanh đã lưu)
     */
    @Data
    public static class OriginalRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private String method; // Phương pháp yêu cầu
        private String url; // yêu cầuURL
        private List<HttpHeader> headers = new ArrayList<>(); // Tiêu đề yêu cầu
        private List<HttpParam> pathVariables = new ArrayList<>(); // Path Variables
        private List<HttpParam> params = new ArrayList<>(); // Query các thông số
        private String bodyType; // Yêu cầu loại cơ thể
        private String body; // Request body content
        private boolean bodyTruncated; // Whether the request body content only saves the preview
        private long originalBodySize; // Kích thước nội dung yêu cầu ban đầu (byte)
        private List<HttpFormData> formDataList = new ArrayList<>(); // FormData
        private List<HttpFormUrlencoded> urlencodedList = new ArrayList<>(); // URLHình thức mã hóa
    }

}
