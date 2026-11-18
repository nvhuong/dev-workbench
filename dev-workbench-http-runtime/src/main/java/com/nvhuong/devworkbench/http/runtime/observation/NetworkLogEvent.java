package com.nvhuong.devworkbench.http.runtime.observation;

/**
 * HTTP Các sự kiện nhật ký mạng được lớp thực thi xuất bản ra bên ngoài.
 *
 * @param stage      giai đoạn thực hiện
 * @param message    Ghi lại nội dung
 * @param elapsedMs  So với thời điểm này callStart Tốn thời gian; chuyển hướng các sự kiện bên ngoài như thế này có thể trống
 * @param durationMs Bản thân giai đoạn hiện tại cần có thời gian;start Để trống nếu sự kiện, sự kiện bên ngoài hoặc không thể tính toán được
 */
public record NetworkLogEvent(NetworkLogEventStage stage, String message, Long elapsedMs, Long durationMs) {

    public NetworkLogEvent(NetworkLogEventStage stage, String message, Long elapsedMs) {
        this(stage, message, elapsedMs, null);
    }
}
