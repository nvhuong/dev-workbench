package com.nvhuong.devworkbench.http.runtime.model;

import lombok.Data;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * bộ sưu tập HTTP Thông tin sự kiện đầy đủ của quá trình
 */
@Data
public class HttpEventInfo {
    // Thông tin kết nối
    private String localAddress;
    private String remoteAddress;
    /** All concrete IPv4/IPv6 routes attempted by OkHttp during connection setup. */
    private final List<HttpRouteAttempt> routeAttempts = new CopyOnWriteArrayList<>();
    // Dấu thời gian cho từng giai đoạn
    private long queueStart; // newCalldấu thời gian trước đó Thời gian yêu cầu bổ sung do chính bạn xác định
    private long callStart;
    private long dispatcherQueueStart;
    private long dispatcherQueueEnd;
    private long proxySelectStart;
    private long proxySelectEnd;
    private long dnsStart;
    private long dnsEnd;
    private String dnsHost;
    private final List<String> dnsAddresses = new CopyOnWriteArrayList<>();
    private String dnsError;
    private long connectStart;
    private long secureConnectStart;
    private long secureConnectEnd;
    private long connectEnd;
    private long connectionAcquired;
    private long requestHeadersStart;
    private long requestHeadersEnd;
    private long requestBodyStart;
    private long requestBodyEnd;
    private long responseHeadersStart;
    private long responseHeadersEnd;
    private long responseBodyStart;
    private long responseBodyEnd;
    private long connectionReleased;
    private long callEnd;
    private long callFailed;
    private long canceled;

    // Thống kê tốn thời gian
    private long queueingCost; // Xếp hàng mất thời gian
    private long stalledCost; // Việc chặn cần có thời gian
    private int retryDecisionCount;
    private int retryCount;
    private int followUpDecisionCount;
    private int followUpCount;
    // thỏa thuận
    private String protocol;
    // TLS/giấy chứng nhận
    private List<Certificate> peerCertificates = new ArrayList<>();
    private List<Certificate> localCertificates = new ArrayList<>();
    private String tlsVersion;
    // Bộ mã hóa
    private String cipherName;
    // SSL Certificate verification warning information (such as expiration, domain name mismatch, self-signed, etc.)
    private String sslCertWarning;
    // bất thường
    private String errorMessage;
    private Throwable error;
    /** Nỗ lực ở giữa đã thất bại, nhưng OkHttp Thông báo lỗi được khôi phục sau khi thử lại tự động không có nghĩa là yêu cầu cuối cùng không thành công. */
    private String recoveredErrorMessage;
    // khác
    private String threadName;

    private long bodyBytesSent;
    private long bodyBytesReceived;
    private long headerBytesSent;
    private long headerBytesReceived;

    public void addRouteAttempt(HttpRouteAttempt routeAttempt) {
        if (routeAttempt != null) {
            routeAttempts.add(routeAttempt);
        }
    }

    public void replaceRouteAttempt(int index, HttpRouteAttempt routeAttempt) {
        if (routeAttempt != null && index >= 0 && index < routeAttempts.size()) {
            routeAttempts.set(index, routeAttempt);
        }
    }

    public void replaceDnsAddresses(List<String> addresses) {
        dnsAddresses.clear();
        if (addresses != null) {
            dnsAddresses.addAll(addresses);
        }
    }

    public synchronized void recordRetryDecision(boolean retry) {
        retryDecisionCount++;
        if (retry) {
            retryCount++;
        }
    }

    public synchronized void recordFollowUpDecision(boolean followUp) {
        followUpDecisionCount++;
        if (followUp) {
            followUpCount++;
        }
    }
}
