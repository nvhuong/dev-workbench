package com.nvhuong.devworkbench.request.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * HttpRequestItem Lớp đại diện cho aHTTPMục cấu hình được yêu cầu
 * Chứa thông tin cơ bản của yêu cầu, tiêu đề, tham số, xác thực, v.v
 * Mỗi tham số yêu cầu một giá trị mặc định để hỗ trợ hiển thị và chỉnh sửa giao diện người dùng
 */
@Setter
@Getter
public class HttpRequestItem implements Serializable {
    public static final String HTTP_VERSION_AUTO = HttpRequestVersions.AUTO;
    public static final String HTTP_VERSION_HTTP_1_1 = HttpRequestVersions.HTTP_1_1;
    public static final String HTTP_VERSION_HTTP_2 = HttpRequestVersions.HTTP_2;
    public static final int DEFAULT_WEBSOCKET_PING_INTERVAL_MS = 30_000;
    public static final int DISABLED_WEBSOCKET_PING_INTERVAL_MS = 0;

    private String id = ""; // Mã định danh duy nhất
    private String name = ""; // Yêu cầu tên
    private String description = ""; // Yêu cầu mô tả (supportMarkdown）
    private String url = ""; // yêu cầuURL
    private String method = "GET"; // Phương thức yêu cầu (GET, POST, PUT, DELETEChờ đợi)
    private RequestItemProtocolEnum protocol = RequestItemProtocolEnum.HTTP; // Loại giao thức, mặc địnhHTTP
    private List<HttpHeader> headersList = new ArrayList<>();
    private String bodyType = ""; // Yêu cầu loại cơ thể
    private String body = ""; // Request body content (egJSON、dữ liệu biểu mẫu, v.v.)
    private List<HttpParam> pathVariablesList = new ArrayList<>();
    private List<HttpParam> paramsList = new ArrayList<>();
    private List<HttpFormData> formDataList = new ArrayList<>();
    private List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
    private String authType = AuthType.INHERIT.getConstant(); // Loại xác thực (inherit/none/apiKey/basic/bearer/digest），Được kế thừa theo mặc định
    private String authUsername = ""; // Basic/DigestTên người dùng
    private String authPassword = ""; // Basic/Digestmật khẩu
    private String authToken = "";    // Bearer Token
    private String authApiKeyName = ""; // API Key tên
    private String authApiKeyValue = ""; // API Key giá trị
    private String authApiKeyPlacement = AuthApiKeyPlacement.HEADER.getConstant(); // API Key Thêm vị trí
    private Boolean followRedirects; // Có tự động theo dõi các chuyển hướng hay không.null Cho biết các cài đặt chung sau đây
    private Boolean cookieJarEnabled; // Có bật hay không Cookie Jar，null Cho biết sử dụng giá trị mặc định
    private HttpRequestProxyPolicy proxyPolicy = HttpRequestProxyPolicy.DEFAULT; // Chính sách proxy, theo mặc định tuân theo cài đặt chung
    private String httpVersion = HTTP_VERSION_AUTO; // HTTP Tùy chọn giao thức
    private Integer requestTimeoutMs; // Yêu cầu thời gian chờ (mili giây).null Cho biết các cài đặt chung sau đây
    private Integer webSocketPingIntervalMs; // WebSocket thỏa thuận ping Khoảng thời gian (mili giây),null Cho biết mặc định 30 Thứ hai,0 có nghĩa là đóng cửa
    // Pre-script (thực thi trước khi có yêu cầu)
    private String prescript = "";
    // Postscript (được thực hiện sau khi phản hồi)
    private String postscript = "";
    // Danh sách các phản hồi đã lưu (tương tự như  DevWorkbench của Examples/Response）
    private List<SavedResponse> response = new ArrayList<>();

    public void setProtocol(RequestItemProtocolEnum protocol) {
        this.protocol = protocol == null ? RequestItemProtocolEnum.HTTP : protocol;
    }

    /**
     * Xác định xem yêu cầu có phải là yêu cầu mới (chưa được đặt tên) hay không
     */
    public boolean isNewRequest() {
        return name == null || name.trim().isEmpty();
    }

    public String resolveHttpVersion() {
        if (httpVersion == null || httpVersion.trim().isEmpty()) {
            return HTTP_VERSION_AUTO;
        }
        if (HTTP_VERSION_HTTP_1_1.equals(httpVersion) || HTTP_VERSION_HTTP_2.equals(httpVersion)) {
            return httpVersion;
        }
        return HTTP_VERSION_AUTO;
    }

    public HttpRequestProxyPolicy resolveProxyPolicy() {
        return HttpRequestProxyPolicy.normalize(proxyPolicy);
    }

    public void setProxyPolicy(HttpRequestProxyPolicy proxyPolicy) {
        this.proxyPolicy = HttpRequestProxyPolicy.normalize(proxyPolicy);
    }

}
