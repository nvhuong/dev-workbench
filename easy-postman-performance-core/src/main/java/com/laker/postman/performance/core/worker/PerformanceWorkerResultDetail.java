package com.laker.postman.performance.core.worker;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class PerformanceWorkerResultDetail {
    String protocol;
    String name;
    String errorMsg;
    int responseCode;
    int costMs;
    boolean executionFailed;
    boolean cancelled;
    /** Yêu cầu thời gian bắt đầu (epoch ms），Không rõ cho -1。 */
    @Builder.Default
    long startTimeMs = -1L;
    /** Byte đầu tiên cần có thời gian (ms），Không rõ cho -1。 */
    @Builder.Default
    long latencyMs = -1L;
    /** Thời gian kết nối (bao gồm cả hàng đợi)/DNS/TCP/TLS，ms），Không rõ cho -1。 */
    @Builder.Default
    long connectMs = -1L;
    DetailRequest request;
    DetailResponse response;
    List<DetailTestResult> testResults;

    @Value
    @Builder(toBuilder = true)
    public static class DetailRequest {
        String method;
        String url;
        String body;
        Map<String, List<String>> headers;
    }

    @Value
    @Builder(toBuilder = true)
    public static class DetailResponse {
        int code;
        String protocol;
        Map<String, List<String>> headers;
        String body;
        long costMs;
        long endTimeMs;
        long bodySize;
        long headersSize;
        /** Tên tệp phản hồi, chỉ phản hồi tệp mới có giá trị. */
        String fileName;
        /** Tiêu đề tệp xác định và  Content-Type Phần mở rộng chưa từng có. */
        String detectedFileExtension;
        /** Văn bản của tệp không được truyền kèm theo chi tiết, chỉ có thông tin tệp được hiển thị. */
        boolean fileContentOmitted;
    }

    @Value
    @Builder(toBuilder = true)
    public static class DetailTestResult {
        String name;
        boolean passed;
        String message;
    }
}
