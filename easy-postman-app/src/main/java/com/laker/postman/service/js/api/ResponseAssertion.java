package com.laker.postman.service.js.api;

import cn.hutool.json.JSONUtil;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Đối tượng xác nhận phản hồi (pm.response)
 * <p>
 * This category is JavaScript Các kịch bản sau được cung cấp cho HTTP Giao diện truy cập và xác nhận cho các phản hồi, bị chế nhạo Postman của pm.response đối tượng.
 * Hỗ trợ cú pháp xác nhận theo chuỗi để làm cho tập lệnh kiểm tra ngắn gọn và dễ đọc hơn.
 * </p>
 *
 * <h3>Chức năng chính:</h3>
 * <ul>
 *   <li><b>Xác nhận mã trạng thái</b>: pm.response.to.have.status(200)</li>
 *   <li><b>Xác nhận tiêu đề phản hồi</b>: pm.response.to.have.header('Content-Type')</li>
 *   <li><b>Khẳng định thời gian đáp ứng</b>: pm.expect(pm.response.responseTime).to.be.below(1000)</li>
 *   <li><b>JSON Truy cập dữ liệu</b>: pm.response.json()</li>
 *   <li><b>Truy cập văn bản nội dung phản hồi</b>: pm.response.text()</li>
 *   <li><b>Truy cập tiêu đề phản hồi</b>: pm.response.headers.get('Content-Type')</li>
 * </ul>
 *
 * <h3>Ví dụ về các xác nhận có chuỗi:</h3>
 * <pre>{@code
 * // Xác nhận mã trạng thái
 * pm.response.to.have.status(200);
 *
 * // Xác nhận tiêu đề phản hồi
 * pm.response.to.have.header('Content-Type');
 * pm.response.to.have.header('Authorization');
 *
 * // Khẳng định thời gian đáp ứng
 * pm.expect(pm.response.responseTime).to.be.below(1000);
 *
 * // JSON Truy cập dữ liệu và xác nhận
 * var jsonData = pm.response.json();
 * pm.expect(jsonData.status).to.equal("success");
 * pm.environment.set("token", jsonData.token);
 *
 * // Truy cập tiêu đề phản hồi
 * var contentType = pm.response.headers.get('Content-Type');
 * console.log("Content-Type:", contentType);
 * }</pre>
 *
 * <h3>Cú pháp chuỗi được hỗ trợ: .</h3>
 * <ul>
 *   <li>pm.response.to.have.status(code) - Khẳng định mã trạng thái</li>
 *   <li>pm.response.to.have.header(name) - Khẳng định rằng tiêu đề phản hồi tồn tại</li>
 *   <li>pm.expect(value).to.be.below(max) - Khẳng định rằng một giá trị nhỏ hơn một giá trị được chỉ định</li>
 * </ul>
 *
 * @see <a href="https://learning.postman.com/docs/writing-scripts/test-scripts/">Postman Test Scripts</a>
 * @author laker
 */
public class ResponseAssertion {
    /** HTTP đối tượng phản hồi */
    private HttpResponse response;

    /** Trình truy cập tiêu đề phản hồi - tương ứng pm.response.headers */
    public Headers headers;

    /** Hỗ trợ cú pháp xác nhận theo chuỗi - tương ứng pm.response.to */
    public ResponseAssertion to = this;

    /** Hỗ trợ cú pháp xác nhận theo chuỗi - tương ứng pm.response.to.have */
    public ResponseAssertion have = this;

    /** Hỗ trợ cú pháp xác nhận theo chuỗi - tương ứng pm.response.to.be */
    public ResponseAssertion be = this;

    /** Thời gian đáp ứng (mili giây) - tương ứng pm.response.responseTime */
    public long responseTime;

    /** Mã trạng thái phản hồi - tương ứng pm.response.code */
    public int code;

    /** Văn bản trạng thái phản hồi - tương ứng pm.response.status */
    public String status;

    /**
     * Xây dựng một đối tượng xác nhận phản hồi
     *
     * @param response HTTP đối tượng phản hồi
     */
    public ResponseAssertion(HttpResponse response) {
        this.response = response;
        this.responseTime = response != null ? response.costMs : -1;
        this.code = response != null ? response.code : -1;
        this.status = response != null ? getStatusText(response.code) : "Unknown";
        this.headers = new Headers(this);
    }

    /**
     * Nhận văn bản trạng thái dựa trên mã trạng thái
     *
     * @param code HTTP mã trạng thái
     * @return Văn bản trạng thái
     */
    private String getStatusText(int code) {
        switch (code) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            default: return "HTTP " + code;
        }
    }

    /**
     * Lấy cơ bản HTTP đối tượng phản hồi
     *
     * @return HTTP đối tượng phản hồi
     */
    public HttpResponse getHttpResponse() {
        return response;
    }

    /**
     * Trả về đối tượng hiện tại để hỗ trợ các cuộc gọi theo chuỗi
     * Tương ứng với kịch bản: pm.response.to
     *
     * @return hiện tại ResponseAssertion đối tượng
     */
    public ResponseAssertion to() {
        return this;
    }

    /**
     * Khẳng định mã trạng thái phản hồi
     * Tương ứng với kịch bản: pm.response.to.have.status(200)
     *
     * @param code Mã trạng thái mong muốn
     * @throws AssertionError Nếu mã trạng thái thực tế không khớp
     */
    public void status(int code) {
        if (response == null || response.code != code) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_STATUS_FAILED, code, response == null ? null : response.code));
        }
    }

    /**
     * Khẳng định rằng tiêu đề phản hồi tồn tại
     * Tương ứng với kịch bản: pm.response.to.have.header('Content-Type')
     *
     * @param name Tên tiêu đề phản hồi (không phân biệt chữ hoa chữ thường)
     * @throws AssertionError Nếu tiêu đề phản hồi không xuất hiện
     */
    public void header(String name) {
        if (response == null || response.headers == null) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_HEADER_NOT_FOUND));
        }
        boolean found = false;
        for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_HEADER_NOT_FOUND_WITH_NAME, name));
        }
    }

    /**
     * Khẳng định rằng một giá trị nhỏ hơn một giá trị được chỉ định (thường được sử dụng cho thời gian phản hồi)
     * Tương ứng với kịch bản: pm.expect(pm.response.responseTime).to.be.below(1000)
     *
     * @param ms Giá trị tối đa mong muốn tính bằng mili giây
     * @throws AssertionError Nếu giá trị thực tế lớn hơn hoặc bằng giá trị được chỉ định
     */
    public void below(long ms) {
        if (response == null || response.costMs >= ms) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_BELOW_FAILED, ms, response == null ? null : response.costMs));
        }
    }

    /**
     * Nhận văn bản nội dung phản hồi
     * Tương ứng với kịch bản: pm.response.text()
     *
     * @return Chuỗi nội dung phản hồi, được trả về nếu phản hồi trống null
     */
    public String text() {
        return response != null ? response.body : null;
    }

    /**
     * Phân tích nội dung phản hồi dưới dạng JSON đối tượng
     * Tương ứng với kịch bản: pm.response.json()
     *
     * @return JSON Đối tượng, có sẵn tại JavaScript Truy cập các thuộc tính trong
     * @throws AssertionError Nếu nội dung phản hồi không hợp lệ JSON
     */
    public Object json() {
        try {
            if (response != null && response.body != null) {
                return JSONUtil.parse(response.body);
            }
        } catch (Exception e) {
            throw new AssertionError(I18nUtil.getMessage(MessageKeys.RESPONSE_ASSERTION_INVALID_JSON, e.getMessage()));
        }
        return null;
    }

    /**
     * Nhận thông tin kích thước phản hồi
     * Tương ứng với kịch bản: pm.response.size()
     *
     * @return ResponseSize Đối tượng chứa body và header thông tin kích thước
     */
    public ResponseSize size() {
        return new ResponseSize(this);
    }

    /**
     * Nhận giá trị tiêu đề phản hồi của tên được chỉ định
     * Tương ứng với kịch bản: pm.response.headers.get('Content-Type')
     *
     * @param name Tên tiêu đề phản hồi (không phân biệt chữ hoa chữ thường)
     * @return Giá trị tiêu đề phản hồi, được trả về nếu nó không tồn tại null
     */
    public String getHeader(String name) {
        if (response == null || response.headers == null) return null;
        for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
            }
        }
        return null;
    }

    /**
     * Tạo đối tượng xác nhận kỳ vọng (Chai.js phong cách)
     * Tương ứng với kịch bản: pm.expect(value)
     *
     * @param actual Giá trị thực tế cần khẳng định
     * @return Mong đợi các đối tượng xác nhận, hỗ trợ các xác nhận theo chuỗi
     */
    public Expectation expect(Object actual) {
        return new Expectation(actual);
    }

    /**
     * Trình truy cập tiêu đề phản hồi (pm.response.headers)
     * <p>
     * Cung cấp giao diện truy cập tiêu đề phản hồi thuận tiện và hỗ trợ truy vấn tên tiêu đề phản hồi không phân biệt chữ hoa chữ thường.
     * </p>
     *
     * <h3>Ví dụ sử dụng:</h3>
     * <pre>{@code
     * var contentType = pm.response.headers.get('Content-Type');
     * var authorization = pm.response.headers.get('authorization'); // Không phân biệt chữ hoa chữ thường
     *
     * // nghiên cứu header tồn tại
     * if (pm.response.headers.has('Set-Cookie')) {
     *     console.log('Cookie was set');
     * }
     *
     * // Lấy header Số lượng
     * var count = pm.response.headers.count();
     *
     * // Đi qua tất cả headers
     * pm.response.headers.each(function(header) {
     *     console.log(header.key + ': ' + header.value);
     * });
     * }</pre>
     */
    public static class Headers {
        private final ResponseAssertion responseAssertion;

        /**
         * Xây dựng trình truy cập tiêu đề phản hồi
         *
         * @param responseAssertion Đối tượng xác nhận phản hồi
         */
        public Headers(ResponseAssertion responseAssertion) {
            this.responseAssertion = responseAssertion;
        }

        /**
         * Nhận giá trị tiêu đề phản hồi của tên được chỉ định
         *
         * @param name Tên tiêu đề phản hồi (không phân biệt chữ hoa chữ thường)
         * @return Giá trị tiêu đề phản hồi, được trả về nếu nó không tồn tại null
         */
        public String get(String name) {
            return responseAssertion.getHeader(name);
        }

        /**
         * Kiểm tra xem tiêu đề phản hồi có tên được chỉ định có tồn tại hay không
         *
         * @param name Tên tiêu đề phản hồi (không phân biệt chữ hoa chữ thường)
         * @return Trả về nếu tiêu đề phản hồi tồn tại true，Còn không thì quay lại false
         */
        public boolean has(String name) {
            return responseAssertion.getHeader(name) != null;
        }

        /**
         * Lấy số lượng tiêu đề phản hồi
         *
         * @return Số lượng tiêu đề phản hồi
         */
        public int count() {
            if (responseAssertion.response == null || responseAssertion.response.headers == null) {
                return 0;
            }
            return responseAssertion.response.headers.size();
        }

        /**
         * Lặp lại qua tất cả các tiêu đề phản hồi
         *
         * @param callback Chức năng gọi lại, nhận header Đối tượng (chứa key và value thuộc tính)
         */
        public void each(org.graalvm.polyglot.Value callback) {
            if (responseAssertion.response == null || responseAssertion.response.headers == null) {
                return;
            }
            if (callback == null || !callback.canExecute()) {
                return;
            }

            for (Map.Entry<String, List<String>> entry : responseAssertion.response.headers.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    // Tạo header đối tượng
                    HeaderEntry headerEntry = new HeaderEntry(entry.getKey(), entry.getValue().get(0));
                    callback.executeVoid(headerEntry);
                }
            }
        }

        /**
         * Nhận tất cả các tiêu đề phản hồi
         *
         * @return Mảng tiêu đề phản hồi
         */
        public HeaderEntry[] all() {
            if (responseAssertion.response == null || responseAssertion.response.headers == null) {
                return new HeaderEntry[0];
            }

           List<HeaderEntry> headerList = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : responseAssertion.response.headers.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    headerList.add(new HeaderEntry(entry.getKey(), entry.getValue().get(0)));
                }
            }
            return headerList.toArray(new HeaderEntry[0]);
        }
    }

    /**
     * Header Lớp mục nhập được sử dụng để thể hiện một tiêu đề phản hồi duy nhất
     */
    public static class HeaderEntry {
        public String key;
        public String value;

        public HeaderEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + ": " + value;
        }
    }

    /**
     * Lớp thông tin kích thước phản hồi (pm.response.size())
     * <p>
     * Cung cấp thông tin kích thước nội dung và tiêu đề phản hồi.
     * </p>
     *
     * <h3>Ví dụ sử dụng:</h3>
     * <pre>{@code
     * var responseSize = pm.response.size();
     * console.log("Kích thước cơ thể phản ứng:", responseSize.body, "bytes");
     * console.log("Kích thước tiêu đề phản hồi:", responseSize.header, "bytes");
     * console.log("Tổng kích thước:", responseSize.total, "bytes");
     * }</pre>
     */
    public static class ResponseSize {
        /** Kích thước nội dung phản hồi (byte) */
        public long body;

        /** Kích thước tiêu đề phản hồi (byte) */
        public long header;

        /** Tổng kích thước (byte) */
        public long total;

        /**
         * Xây dựng một đối tượng kích thước phản hồi
         *
         * @param responseAssertion Đối tượng xác nhận phản hồi
         */
        public ResponseSize(ResponseAssertion responseAssertion) {
            // Kiểm tra căng thẳng metadata-only Chế độ sẽ được tính bodySize Nhưng không được bảo lưu body văn bản.
            HttpResponse currentResponse = responseAssertion.response;
            if (currentResponse != null && hasCollectedBodySize(currentResponse)) {
                this.body = currentResponse.bodySize;
            } else if (currentResponse != null && currentResponse.body != null) {
                this.body = currentResponse.body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            } else {
                this.body = 0;
            }

            // Tính toán kích thước tiêu đề phản hồi
            this.header = 0;
            if (responseAssertion.response != null && responseAssertion.response.headers != null) {
                for (Map.Entry<String, List<String>> entry : responseAssertion.response.headers.entrySet()) {
                    if (entry.getKey() != null) {
                        this.header += entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    }
                    if (entry.getValue() != null) {
                        for (String value : entry.getValue()) {
                            if (value != null) {
                                this.header += value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                            }
                        }
                    }
                    // Thêm vào ": " và "\r\n" kích thước
                    this.header += 4;
                }
            }

            // Tổng kích thước
            this.total = this.body + this.header;
        }

        @Override
        public String toString() {
            return String.format("body: %d bytes, header: %d bytes, total: %d bytes", body, header, total);
        }

        private static boolean hasCollectedBodySize(HttpResponse response) {
            if (response.bodySize > 0) {
                return true;
            }
            return response.body != null
                    && response.body.equals(I18nUtil.getMessage(
                            MessageKeys.RESPONSE_BODY_SKIPPED_PERFORMANCE,
                            response.bodySize
                    ));
        }
    }
}
