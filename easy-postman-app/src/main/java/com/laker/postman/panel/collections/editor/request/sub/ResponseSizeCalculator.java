package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.util.FileSizeDisplayUtil;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.http.runtime.model.HttpEventInfo;
import lombok.Data;
import lombok.experimental.UtilityClass;

import java.awt.*;

/**
 * Máy tính kích thước phản hồi
 * Chịu trách nhiệm tính toán kích thước phản hồi, tốc độ nén và các thông tin khác
 */
@UtilityClass
public class ResponseSizeCalculator {

    /**
     * Thông tin kích thước phản hồi
     */
    @Data
    public static class SizeInfo {
        private final String displayText;
        private final Color normalColor;
        private final Color hoverColor;
        private final boolean compressed;
        private final double compressionRatio;
        private final long savedBytes;
        /**
         * Các thuật toán nén thực tế, chẳng hạn như gzip / br / zstd / deflate，Khi chưa biết "compressed"
         */
        private final String encoding;
    }

    /**
     * Tính toán thông tin kích thước phản hồi
     *
     * @param encoding Content-Encoding Giá trị của tiêu đề, chẳng hạn như  "gzip" / "br" / "zstd"，Có thể được thực hiện null
     */
    public static SizeInfo calculate(long uncompressedBytes, HttpEventInfo httpEventInfo, String encoding) {
        // Kiểm tra xem phản hồi có được nén hay không
        // uncompressedBytes = Kích thước nội dung phản hồi không nén (từ  body.bytes() Nhận,OkHttp giải nén tự động)
        // bodyBytesReceived = Số byte thực tế mà lớp mạng nhận được (từ  OkHttp Thu thập trình xử lý sự kiện)
        //
        // Phải đảm bảo uncompressedBytes > bodyBytesReceived Nó được coi là nén vì những lý do sau:
        // 1. Chunked Mã hóa:bodyBytesReceived Chứa chunk Siêu dữ liệu tiêu đề
        // 2. HTTP/2 giao thức:bodyBytesReceived Chứa frame Tiêu đề trên cao
        // 3. Sự khác biệt trong phương pháp đếm: Trình xử lý sự kiện có thể tính chi phí bổ sung của lớp giao thức
        boolean isCompressed = httpEventInfo != null && uncompressedBytes > 0 &&
                httpEventInfo.getBodyBytesReceived() > 0 &&
                uncompressedBytes > httpEventInfo.getBodyBytesReceived();

        double compressionRatio = 0;
        long savedBytes = 0;

        if (isCompressed) {
            compressionRatio = (1 - (double) httpEventInfo.getBodyBytesReceived() / uncompressedBytes) * 100;
            savedBytes = uncompressedBytes - httpEventInfo.getBodyBytesReceived();
        }

        String displayText;
        Color normalColor;
        Color hoverColor;

        String enc = (encoding != null && !encoding.isBlank()) ? encoding.toLowerCase() : "compressed";

        if (isCompressed) {
            displayText = String.format("%s (%s %.0f%%)",
                    formatBytes(httpEventInfo.getBodyBytesReceived()), enc, compressionRatio);
            normalColor = ModernColors.getTextSecondary();
            hoverColor = ModernColors.getPrimary();
        } else {
            displayText = formatBytes(uncompressedBytes);
            normalColor = ModernColors.getTextHint();
            hoverColor = ModernColors.getPrimary();
            enc = null;
        }

        return new SizeInfo(displayText, normalColor, hoverColor, isCompressed, compressionRatio, savedBytes, enc);
    }

    /**
     * Định dạng kích thước byte thành văn bản có thể đọc được
     */
    public static String formatBytes(long bytes) {
        return FileSizeDisplayUtil.formatCompactSize(bytes);
    }
}
