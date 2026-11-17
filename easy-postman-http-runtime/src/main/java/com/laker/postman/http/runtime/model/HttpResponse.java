package com.laker.postman.http.runtime.model;

import com.laker.postman.http.runtime.file.ResponseFiles;
import com.laker.postman.util.HttpHeaderConstants;

import java.util.List;
import java.util.Map;

/**
 * HTTP Lớp phản hồi
 */
public class HttpResponse {
    public Map<String, List<String>> headers;
    public String body;
    /**
     * Các byte nội dung phản hồi thô, chỉ khi được đọc hoàn toàn, không có tệp tạm thời và được nhận dạng dưới dạng tệp/Các tệp đính kèm hoặc văn bản không thể được khôi phục một cách dễ dàng khi các byte gốc được giữ lại.
     * bình thường UTF-8 Phản hồi văn bản không được bảo tồn.{@link #body} Nhấn UTF-8 Mã hóa khôi phục các byte gốc.
     * Trường này phải được sử dụng trước tiên khi tải xuống phản hồi để tránh việc chuyển đổi bộ ký tự của văn bản xem trước làm phá hủy nội dung gốc.
     */
    public transient byte[] rawBodyBytes;
    /**
     * Các phần mở rộng tệp (chứa dấu chấm) được xác định với độ tin cậy cao dựa trên các đặc điểm của nội dung phản hồi khi không được xác định null。
     */
    public transient String detectedFileExtension;
    /**
     * Content-Disposition được khai báo là attachment，Cho biết rằng máy chủ muốn tải xuống dưới dạng tệp (inline Không tính).
     */
    public transient boolean fileAttachment;
    /**
     * Nội dung của phản hồi tệp không được giữ nguyên (chẳng hạn như khôi phục từ một ví dụ chỉ lưu thông tin meta). Tại thời điểm này, chỉ có thể hiển thị thông tin tệp và không thể tải xuống.
     */
    public transient boolean fileContentOmitted;
    /**
     * Đã nhận được tiêu đề phản hồi nhưng kết nối bị gián đoạn khi đọc nội dung phản hồi.{@link #body} Chỉ là một thông báo lỗi chứ không phải nội dung thực sự.
     */
    public transient boolean bodyIncomplete;
    /**
     * PREVIEW Nội dung phản hồi vượt quá giới hạn đọc trong chế độ.{@link #body} Chỉ có tiền tố được giữ lại (không có văn bản nhắc nhở nào được thêm vào).
     */
    public transient boolean bodyPreviewTruncated;
    public int code; // Thêm trường mã trạng thái phản hồi
    public String threadName; // Thêm trường tên chủ đề
    public String filePath; // Trường đường dẫn tải xuống tệp tạm thời
    public String fileName; // Nếu đó là tệp tải xuống, trường tên tệp được lấy từ tiêu đề phản hồi
    public long costMs; // Yêu cầu mất thời gian tính bằng mili giây
    public long endTime; // Thời gian kết thúc phản hồi, tính bằng mili giây
    public String protocol; // Trường loại giao thức, ví dụ HTTP/1.1 hoặc HTTP/2
    public int idleConnectionCount; // Số lượng kết nối nhàn rỗi
    public int connectionCount; // Tổng số kết nối
    public HttpEventInfo httpEventInfo;
    public long bodySize; // Số byte nội dung phản hồi
    public long headersSize; // Số byte tiêu đề phản hồi
    public boolean isSse = false; // Cho dù đó làSSEphản hồi
    public boolean isImage = false; // Có phản hồi hình ảnh hay không (để xem trước)

    public void addHeader(String name, List<String> value) {
        if (headers == null) {
            return;
        }
        if (HttpHeaderConstants.EASY_CONTENT_ENCODING.equalsIgnoreCase(name)) {
            headers.put(HttpHeaderConstants.CONTENT_ENCODING, value);
            headers.remove(HttpHeaderConstants.EASY_CONTENT_ENCODING);
        } else if (HttpHeaderConstants.EASY_CONTENT_LENGTH.equalsIgnoreCase(name)) {
            headers.put(HttpHeaderConstants.CONTENT_LENGTH, value);
            headers.remove(HttpHeaderConstants.EASY_CONTENT_LENGTH);
        } else {
            headers.put(name, value);
        }
    }

    /**
     * Nội dung phản hồi được nhận dạng dưới dạng tệp nhị phân (hoặc nội dung tệp đã bị bỏ qua) {@link #body} Văn bản không phù hợp để hiển thị trực tiếp.
     */
    public boolean isRecognizedFile() {
        return detectedFileExtension != null || fileContentOmitted;
    }

    /**
     * Đơn giản hóa đối tượng và đặt các trường không cần thiết trong quá trình kết xuất thành  null，Giảm mức sử dụng bộ nhớ.
     * Các trường dành riêng:code, protocol, threadName, httpEventInfo, headers, body, costMs, endTime, bodySize, headersSize,
     * idleConnectionCount, connectionCount, detectedFileExtension, fileAttachment；
     * Đặt thành null Các trường: .filePath, rawBodyBytes；fileName Dành riêng để hiển thị thông tin tập tin.
     * Phản hồi của tệp (xem {@link ResponseFiles#isFileResponse}）Các ký tự bị cắt xén hoặc lời nhắc tệp tạm thời được giải mã bằng văn bản sẽ không được giữ lại.
     * Và đánh dấu nội dung là bị lược bỏ, khi kết xuất chỉ hiển thị thông tin file.
     */
    public void simplify() {
        // Tuyên bố đúng đắn MIME tệp nhị phân (PDF/PNG v.v.) sẽ không được thiết lập detectedFileExtension，Điều tương tự được thực hiện bằng tập tin
        boolean file = ResponseFiles.isFileResponse(this);
        this.filePath = null;    // Đường dẫn tải file, không cần thiết khi kết xuất
        this.rawBodyBytes = null; // Byte nội dung phản hồi thô, không bắt buộc khi kết xuất
        if (file) {
            this.body = "";
            this.fileContentOmitted = true;
        }
        // isSse, idleConnectionCount, connectionCount Dự trữ(Timingcác tab có thể được yêu cầu)
    }
}
