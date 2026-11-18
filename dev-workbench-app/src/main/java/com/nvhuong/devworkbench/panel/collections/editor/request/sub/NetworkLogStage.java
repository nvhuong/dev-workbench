package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.http.runtime.observation.NetworkLogEventStage;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.Getter;

import java.awt.*;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Bảng liệt kê giai đoạn nhật ký mạng
 * Quản lý thống nhất các biểu tượng, màu sắc và điểm nhấn tiêu đề mặc định cho các giai đoạn nhật ký.
 * HTTP Màu sắc của kết quả và quyết định thử lại được bảng nhật ký bổ sung dựa trên nội dung sự kiện.
 */
@Getter
public enum NetworkLogStage {
    // ==================== Lỗi và thất bại (đỏ, đậm)====================
    FAILED("Failed", "❌", ModernColors::getError, true),
    CALL_FAILED("CallFailed", "❌", ModernColors::getError, true),
    REQUEST_FAILED("RequestFailed", "❌", ModernColors::getError, true),
    RESPONSE_FAILED("ResponseFailed", "❌", ModernColors::getError, true),
    CANCELED("Canceled", "⏹", ModernColors::getTextSecondary, true),

    // ==================== Yêu cầu vòng đời và chẩn đoán ====================
    CALL_START("RequestStart", "▶", ModernColors::getPrimary, true),
    CALL_END("RequestEnd", "■", ModernColors::getTextSecondary, true),
    REQUEST_COMPLETE("RequestComplete", "■", ModernColors::getTextSecondary, true),
    DISPATCHER_QUEUE_START("DispatcherQueueStart", "⏳", ModernColors::getTextSecondary, false),
    DISPATCHER_QUEUE_END("DispatcherQueueEnd", "▶", ModernColors::getTextSecondary, false),
    RETRY_DECISION("RetryDecision", "🔁", ModernColors::getTextSecondary, false),
    FOLLOW_UP_DECISION("FollowUpDecision", "↪", ModernColors::getTextSecondary, false),
    CACHE_HIT("CacheHit", "💾", ModernColors::getTextSecondary, false),
    CACHE_MISS("CacheMiss", "💾", ModernColors::getTextSecondary, false),
    CACHE_CONDITIONAL_HIT("CacheConditionalHit", "💾", ModernColors::getTextSecondary, false),
    SATISFACTION_FAILURE("SatisfactionFailure", "⚠️", ModernColors::getWarning, false),
    REQUEST_PREPARED("RequestPrepared", "", ModernColors::getTextSecondary, false),

    // ==================== TLS ====================
    SECURE_CONNECT_START("TLSHandshakeStart", "🔒", ModernColors::getTextSecondary, false),
    SECURE_CONNECT_END("TLSHandshakeEnd", "🔒", ModernColors::getTextSecondary, false),

    // ==================== kết nối ====================
    CONNECT_START("ConnectStart", "🔌", ModernColors::getTextSecondary, false),
    // A failed route can be followed by another successful route; terminal errors use CALL_FAILED.
    CONNECT_FAILED("ConnectFailed", "⚠️", ModernColors::getWarning, true),
    CONNECT_END("ConnectEnd", "🔗", ModernColors::getTextSecondary, false),
    CONNECTION_ACQUIRED("ConnectionReady", "🔗", ModernColors::getTextSecondary, false),
    CONNECTION_RELEASED("ConnectionReleased", "↩", ModernColors::getTextSecondary, false),
    ROUTE_INTERCEPTED("RouteIntercepted", "⚠️", ModernColors::getWarning, true),

    // ==================== DNS ====================
    DNS_START("DNSStart", "🔍", ModernColors::getTextSecondary, false),
    DNS_END("DNSEnd", "📍", ModernColors::getTextSecondary, false),

    // ==================== đại lý ====================
    PROXY_SELECT("ProxySelect", "🌐", ModernColors::getTextSecondary, false),
    PROXY_SELECT_START("ProxySelectStart", "🌐", ModernColors::getTextSecondary, false),
    PROXY_SELECT_END("ProxySelectEnd", "🌐", ModernColors::getTextSecondary, false),

    // ==================== Yêu cầu đã được gửi ====================
    REQUEST_HEADERS_START("RequestHeadersStart", "↑", ModernColors::getTextSecondary, false),
    REQUEST_HEADERS_END("RequestHeadersEnd", "↑", ModernColors::getTextSecondary, false),
    REQUEST_BODY_START("RequestBodyStart", "↑", ModernColors::getTextSecondary, false),
    REQUEST_BODY_END("RequestBodyEnd", "↑", ModernColors::getTextSecondary, false),

    // ==================== Đã nhận được phản hồi ====================
    RESPONSE_HEADERS_START("ResponseHeadersStart", "↓", ModernColors::getTextSecondary, false),
    RESPONSE_HEADERS_END("ResponseHeadersEnd", "↓", ModernColors::getTextSecondary, false),
    RESPONSE_HEADERS_END_REDIRECT("ResponseHeadersEnd:Redirect", "↓", ModernColors::getTextSecondary, false),
    RESPONSE_BODY_START("ResponseBodyStart", "↓", ModernColors::getTextSecondary, false),
    RESPONSE_BODY_END("ResponseBodyEnd", "↓", ModernColors::getTextSecondary, false),

    // ==================== Chuyển hướng ====================
    REDIRECT("Redirect", "↪", ModernColors::getPrimary, true),

    // ==================== Mặc định ====================
    DEFAULT("Default", "", ModernColors::getTextPrimary, false);

    private final String stageName;
    private final String emoji;
    private final Supplier<Color> colorProvider;
    private final boolean bold;

    NetworkLogStage(String stageName, String emoji, Supplier<Color> colorProvider, boolean bold) {
        this.stageName = stageName;
        this.emoji = emoji;
        this.colorProvider = colorProvider;
        this.bold = bold;
    }

    /**
     * Lấy màu phù hợp với chủ đề hiện tại
     */
    public Color getColor() {
        return colorProvider.get();
    }

    /**
     * Lấy tên sân khấu mà người dùng có thể đọc được cho ngôn ngữ hiện tại. Nhận dạng kỹ thuật vẫn còn {@link #getStageName()} Bảo lưu,
     * Sẽ rất thuận tiện cho các nhà phát triển trong việc xác định vị trí các sự kiện cơ bản trong nhật ký.
     */
    public String getDisplayName() {
        return I18nUtil.getMessage(MessageKeys.NETWORK_LOG_STAGE_PREFIX
                + name().toLowerCase(Locale.ROOT));
    }

    /**
     * Xác định xem đó là lỗi hay loại lỗi
     */
    public boolean isError() {
        return this == FAILED || this == CALL_FAILED || this == REQUEST_FAILED
                || this == RESPONSE_FAILED;
    }

    /**
     * Xác định xem đây có phải là giai đoạn mà kết quả thành công có thể được chứng minh hay không; kết quả thực tế được xác định bởi trạng thái phản hồi.
     */
    public boolean isSuccess() {
        return this == REQUEST_COMPLETE;
    }

    /**
     * Sẽ HTTP Giai đoạn nhật ký của lớp thực thi được ánh xạ tới UI Giai đoạn kết xuất.
     * <p>
     * Đây rồi service sự kiện lớp và Swing Hiển thị điểm chuyển tiếp duy nhất giữa các kiểu để tránh sự phụ thuộc ngược vào lớp thực thi UI liệt kê.
     */
    public static NetworkLogStage fromEventStage(NetworkLogEventStage stage) {
        if (stage == null) {
            return DEFAULT;
        }
        try {
            return NetworkLogStage.valueOf(stage.name());
        } catch (IllegalArgumentException ex) {
            return DEFAULT;
        }
    }
}
