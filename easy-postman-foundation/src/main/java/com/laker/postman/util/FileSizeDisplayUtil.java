package com.laker.postman.util;

import lombok.experimental.UtilityClass;

/**
 * Lớp công cụ hiển thị kích thước tập tin
 * Cung cấp chuyển đổi số byte thành định dạng hiển thị thân thiện hơn với người dùng
 */
@UtilityClass
public class FileSizeDisplayUtil {

    /**
     * Hiển thị kích thước tệp được định dạng, tự động chọn đơn vị, giữ lại hai chữ số thập phân và loại bỏ những chữ số vô nghĩa.00
     *
     * @param sizeBytes Số byte
     * @return Các chuỗi hiển thị thân thiện như  1.23 MB
     */
    public static String formatSize(int sizeBytes) {
        return formatSize((long) sizeBytes);
    }

    /**
     * Formats long-running transfers without overflowing the byte count.
     */
    public static String formatSize(long sizeBytes) {
        String unit;
        double value;
        if (sizeBytes >= 1024L * 1024 * 1024 * 1024) {
            unit = "TB";
            value = sizeBytes / (1024.0 * 1024 * 1024 * 1024);
        } else if (sizeBytes >= 1024 * 1024 * 1024) {
            unit = "GB";
            value = sizeBytes / (1024.0 * 1024 * 1024);
        } else if (sizeBytes >= 1024 * 1024) {
            unit = "MB";
            value = sizeBytes / (1024.0 * 1024);
        } else if (sizeBytes >= 1024) {
            unit = "KB";
            value = sizeBytes / 1024.0;
        } else {
            unit = "B";
            value = sizeBytes;
        }
        if (unit.equals("B")) {
            return String.format("%d %s", sizeBytes, unit);
        } else {
            return String.format("%.2f %s", value, unit);
        }
    }

    /**
     * Hiển thị nhỏ gọn kích thước nội dung phản hồi (B / KB Một chữ số thập phân / MB Hai chữ số thập phân), thanh trạng thái phản hồi, tổng quan về tệp và chi tiết lịch sử được chia sẻ.
     */
    public static String formatCompactSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
    }

    /**
     * Tiến trình tải xuống tệp được định dạng được hiển thị
     *
     * @param totalBytes    Số byte được tải xuống
     * @param contentLength Tổng số byte
     * @return Chuỗi hiển thị thân thiện
     */
    public static String formatDownloadSize(int totalBytes, int contentLength) {
        if (contentLength > 0) {
            return String.format("Downloaded: %s / %s", formatSize(totalBytes), formatSize(contentLength));
        } else {
            return String.format("Downloaded: %s", formatSize(totalBytes));
        }
    }
}
