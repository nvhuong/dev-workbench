package com.laker.postman.performance.model;

import com.laker.postman.performance.core.model.PerformanceProtocol;


import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;

import java.util.List;

public class ResultNodeInfo {

    /**
     * Tên giao diện
     */
    public final String name;

    /**
     * thông báo lỗi
     */
    public final String errorMsg;

    /**
     * yêu cầu
     */
    public final PreparedRequest req;

    /**
     * phản hồi
     */
    public final HttpResponse resp;

    /**
     * Khẳng định kết quả
     */
    public final List<TestResult> testResults;

    public final PerformanceProtocol protocol;

    /**
     * Thời gian thực hiện (mili giây) - Điều này tốt cho thời gian xây dựng
     */
    public final int costMs;

    /**
     * HTTPMã trạng thái phản hồi
     */
    public final int responseCode;

    /**
     * Liệu mức độ thực thi có thất bại rõ ràng hay không (sự cố tiền tập lệnh / HTTP Yêu cầu ngoại lệ / Sự cố của phần tái bút).
     * Phân biệt với lỗi khẳng định: lỗi khẳng định là do testResults Được phản ánh, lỗi thực thi được phản ánh trong trường này.
     */
    public final boolean executionFailed;
    public final boolean cancelled;

    /**
     * Thời gian bắt đầu, byte đầu tiên và thời gian kết nối được trích xuất từ ​​sự kiện phản hồi trong quá trình xây dựng để tránh mất mát sau khi đơn giản hóa hiển thị.
     */
    public final ResultTiming timing;

    public ResultNodeInfo(
            String name,
            String errorMsg,
            PreparedRequest req,
            HttpResponse resp,
            List<TestResult> testResults,
            boolean executionFailed) {
        this(name, errorMsg, req, resp, testResults, executionFailed, PerformanceProtocol.HTTP);
    }

    public ResultNodeInfo(
            String name,
            String errorMsg,
            PreparedRequest req,
            HttpResponse resp,
            List<TestResult> testResults,
            boolean executionFailed,
            PerformanceProtocol protocol) {
        this(name, errorMsg, req, resp, testResults, executionFailed, false, protocol);
    }

    public ResultNodeInfo(
            String name,
            String errorMsg,
            PreparedRequest req,
            HttpResponse resp,
            List<TestResult> testResults,
            boolean executionFailed,
            boolean cancelled,
            PerformanceProtocol protocol) {
        this(name, errorMsg, req, resp, testResults, executionFailed, cancelled, protocol,
                ResultTiming.fromResponse(resp, 0L));
    }

    public ResultNodeInfo(
            String name,
            String errorMsg,
            PreparedRequest req,
            HttpResponse resp,
            List<TestResult> testResults,
            boolean executionFailed,
            boolean cancelled,
            PerformanceProtocol protocol,
            ResultTiming timing) {
        this.name = name;
        this.errorMsg = errorMsg;
        this.req = req;
        this.resp = resp;
        this.testResults = testResults;
        this.executionFailed = executionFailed;
        this.cancelled = cancelled;
        this.protocol = protocol == null ? PerformanceProtocol.HTTP : protocol;
        this.timing = timing == null ? ResultTiming.EMPTY : timing;

        // ✅ Sự đoàn kết được tính ở đây ms（Không phụ thuộc costNs）
        this.costMs = resp != null ? (int) resp.costMs : 0;

        // Trích xuất mã phản hồi
        this.responseCode = resp != null ? resp.code : 0;
    }

    /**
     * Cho dù bất kỳ xác nhận nào không thành công
     */
    public boolean hasAssertionFailed() {
        if (testResults == null) return false;
        for (TestResult r : testResults) {
            if (!r.passed) return true;
        }
        return false;
    }

    /**
     * Thống nhất thành công/Đánh giá thất bại được chia sẻ ở ba vị trí (biểu đồ xu hướng, báo cáo và bảng kết quả):
     * 1. Lỗi rõ ràng ở cấp độ thực thi (tập lệnh trước bị lỗi, .HTTPNgoại lệ, sự cố sau tập lệnh)→ false
     * 2. Có kết quả khẳng định → Tùy thuộc vào các xác nhận (thành công chỉ đạt được nếu tất cả các xác nhận đều vượt qua)
     * 3. Không có khẳng định → qua HTTP Mã trạng thái sẽ chiếm ưu thế (2xx/3xx để thành công)
     * 4. Không có phản hồi → false
     */
    public boolean isActuallySuccessful() {
        // Phù hợp với hồ sơ lấy mẫu: yêu cầu bị hủy không được coi là thành công (ngay cả khi  WebSocket Bắt tay 101）
        return !cancelled && isActuallySuccessful(executionFailed, resp, testResults);
    }

    public static boolean isActuallySuccessful(boolean executionFailed,
                                               HttpResponse resp,
                                               List<TestResult> testResults) {
        // 1. Mức độ thực thi rõ ràng không thành công và trả về trực tiếp false
        if (executionFailed) {
            return false;
        }
        if (resp != null && resp.bodyIncomplete) {
            return false;
        }

        // 2. Nếu có kết quả khẳng định thì khẳng định đó sẽ được ưu tiên áp dụng
        if (testResults != null && !testResults.isEmpty()) {
            for (TestResult result : testResults) {
                if (!result.passed) {
                    return false;
                }
            }
            return true;
        }

        // 3. Nếu không có xác nhận, hãy sử dụng  HTTP Mã trạng thái sẽ được ưu tiên áp dụng
        if (resp != null && resp.code > 0) {
            return resp.code == 101 || (resp.code >= 200 && resp.code < 400);
        }

        // 4. Tóm lại: Quay lại nếu không có phản hồi false
        return false;
    }
}
