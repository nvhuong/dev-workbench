package com.nvhuong.devworkbench.http.runtime.model;

import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.HttpRequestProxyPolicy;
import com.nvhuong.devworkbench.request.model.TransportAuth;
import com.nvhuong.devworkbench.http.runtime.interaction.DownloadProgressSinkFactory;
import com.nvhuong.devworkbench.http.runtime.interaction.ResponseSizeLimitWarningSink;
import com.nvhuong.devworkbench.http.runtime.observation.HttpLifecycleLogSink;
import com.nvhuong.devworkbench.http.runtime.observation.NetworkLogSink;

import java.util.ArrayList;
import java.util.List;

/**
 * Đối tượng yêu cầu được chuẩn bị sẵn chứa tất cả thông tin cần thiết cho yêu cầu và nội dung sau khi thay thế các biến.
 */
public class PreparedRequest {
    public String id;
    public String name;
    public String url;
    public String method;

    public String body;
    public String bodyType;

    public List<HttpHeader> headersList;
    public List<HttpFormData> formDataList;
    public List<HttpFormUrlencoded> urlencodedList;
    public List<HttpParam> pathVariablesList;
    public List<HttpParam> paramsList;

    public boolean isMultipart;
    public boolean followRedirects = true; // Tự động chuyển hướng theo mặc định
    public boolean cookieJarEnabled = true; // Được bật theo mặc định Cookie Jar
    public HttpRequestProxyPolicy proxyPolicy = HttpRequestProxyPolicy.DEFAULT; // Mặc định tuân theo cài đặt proxy toàn cầu
    public boolean sslVerificationEnabled = false; // Bị tắt theo mặc định SSL Kiểm tra
    public String httpVersion = HttpRequestItem.HTTP_VERSION_AUTO; // HTTP Tùy chọn giao thức
    public int requestTimeoutMs = 0; // 0 Cho biết không có thời gian chờ
    public int webSocketPingIntervalMs = HttpRequestItem.DEFAULT_WEBSOCKET_PING_INTERVAL_MS; // 0 Cho biết việc kết thúc thỏa thuận ping
    public TransportAuth transportAuth; // Cần có siêu dữ liệu xác thực lớp vận chuyển trong giai đoạn gửi (ví dụ: Digest challenge chứng nhận)

    // Kiểm soát giám sát sự kiện (kiểm soát tốt)
    public HttpCaptureProfile captureProfile; // Thực hiện chiến lược thu thập cảnh; khi trống, nó tương thích với các kết hợp trường Boolean cũ
    public boolean collectBasicInfo = true; // Thu thập thông tin cơ bản (headers、body），Được bật theo mặc định
    public boolean collectMetricsInfo = false; // Chỉ các chỉ số nhẹ (dấu thời gian, gửi) cần thiết cho số liệu thống kê kiểm tra sức chịu đựng mới được thu thập/nhận byte)
    public boolean collectEventInfo = true; // Thu thập thông tin sự kiện đầy đủ (DNS、kết nối,SSLv.v.), được bật theo mặc định
    public boolean enableNetworkLog = false; // Bật đầu ra bảng nhật ký mạng, tắt theo mặc định
    public transient NetworkLogSink networkLogSink = NetworkLogSink.noop(); // Cổng đầu ra nhật ký mạng, bởi UI Các lớp được tiêm theo yêu cầu
    public transient HttpLifecycleLogSink lifecycleLogSink = HttpLifecycleLogSink.noop();
    public transient DownloadProgressSinkFactory downloadProgressSinkFactory = DownloadProgressSinkFactory.noop();
    public transient ResponseSizeLimitWarningSink responseSizeLimitWarningSink = ResponseSizeLimitWarningSink.noop();
    public boolean notifyCookieChanges = true; // Có xuất bản sau khi yêu cầu được hoàn thành hay không Cookie Thay đổi sự kiện
    public ResponseBodyMode responseBodyMode = ResponseBodyMode.FULL;
    public int responseBodyPreviewLimitBytes = 64 * 1024;

    // Trường tập lệnh (được áp dụng group kế thừa)
    public String prescript;
    public String postscript;

    public String sentUrl; // thực sự đã được gửi URL Ảnh chụp nhanh
    public String sentMethod; // thực sự đã được gửi HTTP Ảnh chụp nhanh phương pháp
    public List<HttpHeader> sentHeadersList; // Ảnh chụp nhanh các tiêu đề yêu cầu thực tế được gửi
    public String sentRequestBody; // Nội dung nội dung yêu cầu thực tế đã được gửi
    public boolean sentRequestBodyReplayable; // Liệu ảnh chụp nhanh nội dung yêu cầu thực tế có đầy đủ và khả dụng hay không cURL Tái phát
    public transient volatile HttpEventInfo exchangeEventInfo; // Chỉ thông tin sự kiện thời gian chạy SSE/WS Việc đọc gọi lại không đồng bộ không tham gia vào quá trình kiên trì

    /**
     * Tạo một bản sao nông của đối tượng hiện tại
     * Lưu ý:List và Map Bản thân đối tượng không được sao chép sâu, chỉ sao chép tham chiếu
     */
    public PreparedRequest shallowCopy() {
        PreparedRequest copy = new PreparedRequest();
        copy.id = this.id;
        copy.name = this.name;
        copy.url = this.url;
        copy.method = this.method;
        copy.sentUrl = this.sentUrl;
        copy.sentMethod = this.sentMethod;
        copy.sentHeadersList = this.sentHeadersList == null ? null : new ArrayList<>(this.sentHeadersList);
        // Trạng thái chạy của một gói duy nhất trace Không sao chép cấu hình với các yêu cầu để tránh chuyển hướng/Gửi lại thông tin kết nối hop trước đó.
        copy.exchangeEventInfo = null;
        copy.body = this.body;
        copy.bodyType = this.bodyType;
        copy.sentRequestBody = this.sentRequestBody;
        copy.sentRequestBodyReplayable = this.sentRequestBodyReplayable;
        copy.isMultipart = this.isMultipart;
        copy.followRedirects = this.followRedirects;
        copy.cookieJarEnabled = this.cookieJarEnabled;
        copy.proxyPolicy = this.proxyPolicy;
        copy.sslVerificationEnabled = this.sslVerificationEnabled;
        copy.httpVersion = this.httpVersion;
        copy.requestTimeoutMs = this.requestTimeoutMs;
        copy.webSocketPingIntervalMs = this.webSocketPingIntervalMs;
        copy.transportAuth = this.transportAuth != null ? this.transportAuth.shallowCopy() : null;
        copy.captureProfile = this.captureProfile;
        copy.collectBasicInfo = this.collectBasicInfo;
        copy.collectMetricsInfo = this.collectMetricsInfo;
        copy.collectEventInfo = this.collectEventInfo;
        copy.enableNetworkLog = this.enableNetworkLog;
        copy.networkLogSink = this.networkLogSink;
        copy.lifecycleLogSink = this.lifecycleLogSink;
        copy.downloadProgressSinkFactory = this.downloadProgressSinkFactory;
        copy.responseSizeLimitWarningSink = this.responseSizeLimitWarningSink;
        copy.notifyCookieChanges = this.notifyCookieChanges;
        copy.responseBodyMode = this.responseBodyMode;
        copy.responseBodyPreviewLimitBytes = this.responseBodyPreviewLimitBytes;
        copy.prescript = this.prescript;
        copy.postscript = this.postscript;
        copy.headersList = this.headersList == null ? null : new ArrayList<>(this.headersList);
        copy.formDataList = this.formDataList == null ? null : new ArrayList<>(this.formDataList);
        copy.urlencodedList = this.urlencodedList == null ? null : new ArrayList<>(this.urlencodedList);
        copy.pathVariablesList = this.pathVariablesList == null ? null : new ArrayList<>(this.pathVariablesList);
        copy.paramsList = this.paramsList == null ? null : new ArrayList<>(this.paramsList);
        return copy;
    }

    /**
     * Đơn giản hóa đối tượng và đặt các trường không cần thiết trong quá trình kết xuất thành  null，Giảm mức sử dụng bộ nhớ
     * Các trường dành riêng:url, method, sentUrl, sentMethod, sentHeadersList, formDataList, urlencodedList, sentRequestBody, sentRequestBodyReplayable
     * Đặt thành null Các trường: .id, name, body, bodyType, transportAuth, headersList,
     * pathVariablesList, paramsList, exchangeEventInfo
     */
    public void simplify() {
        this.id = null;
        this.name = null;
        this.body = null;
        this.bodyType = null;
        this.transportAuth = null;
        this.headersList = null;  // Việc hiển thị được thực hiện bằng cách sử dụng  sentHeadersList
        this.pathVariablesList = null;
        this.paramsList = null;   // Không hiển thị khi kết xuất
        this.exchangeEventInfo = null;
        // isMultipart, followRedirects và công tắc thu nhận là loại cơ bản và không chiếm bộ nhớ chính
    }

    public enum ResponseBodyMode {
        FULL,
        PREVIEW,
        METADATA_ONLY
    }
}
