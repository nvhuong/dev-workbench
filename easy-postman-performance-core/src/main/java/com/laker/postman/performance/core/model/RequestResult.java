package com.laker.postman.performance.core.model;

/**
 * Yêu cầu ghi lại kết quả
 */
public class RequestResult {
    public long startTime;    // Thời gian bắt đầu (mili giây)
    public long endTime;      // Thời gian kết thúc (mili giây)
    public boolean success;   // Liệu nó có thành công hay không
    public String apiId;      // APIchỉ mộtID（để thống kê và truy vấn)
    public String apiName;    // APITên hiển thị (được mang trong thời gian chạy để tránh dựa vào siêu dữ liệu chung)
    public PerformanceProtocol protocol = PerformanceProtocol.HTTP;
    public int sentMessages;
    public int receivedMessages;
    public int matchedMessages;
    public long sentBytes;      // Số byte được gửi: tiêu đề yêu cầu + Nội dung yêu cầu, được sử dụng để tính toán Sent KB/s
    public long receivedBytes;  // Số byte nhận được: tiêu đề phản hồi + Nội dung phản hồi, được sử dụng để tính toán Received KB/s
    public long firstMessageLatencyMs = -1;

    /**
     * Nhà xây dựng chính
     */
    public RequestResult(long startTime, long endTime, boolean success, String apiId) {
        this(startTime, endTime, success, apiId, PerformanceProtocol.HTTP);
    }

    public RequestResult(long startTime, long endTime, boolean success, String apiId, PerformanceProtocol protocol) {
        this(startTime, endTime, success, apiId, null, protocol);
    }

    public RequestResult(long startTime,
                         long endTime,
                         boolean success,
                         String apiId,
                         String apiName,
                         PerformanceProtocol protocol) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.success = success;
        this.apiId = apiId;
        this.apiName = apiName;
        this.protocol = protocol == null ? PerformanceProtocol.HTTP : protocol;
    }

    /**
     * Nhận thời gian phản hồi (được tính toán động, không được lưu trữ)
     *
     * @return Thời gian đáp ứng (mili giây)
     */
    public long getResponseTime() {
        return endTime - startTime;
    }

    public String getApiName() {
        if (apiName != null && !apiName.isBlank()) {
            return apiName;
        }
        return apiId;
    }
}
