package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.nvhuong.devworkbench.http.runtime.observation.NetworkLogEventStage;
import com.nvhuong.devworkbench.util.I18nUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class NetworkLogMessageFormatterTest {
    private boolean originalVietnamese;

    @BeforeMethod
    public void useVietnameseLocale() {
        originalVietnamese = I18nUtil.isVietnamese();
        I18nUtil.setLocale("zh");
    }

    @AfterMethod
    public void restoreLocale() {
        I18nUtil.setLocale(originalVietnamese ? "vi" : "en");
    }

    @Test
    public void shouldLocalizeStageDisplayNameWithoutChangingTechnicalName() {
        assertEquals(NetworkLogStage.CALL_START.getStageName(), "RequestStart");
        assertEquals(NetworkLogStage.CALL_START.getDisplayName(), "Yêu cầu bắt đầu");
        assertEquals(NetworkLogStage.CALL_END.getDisplayName(), "Yêu cầu kết thúc");
        assertEquals(NetworkLogStage.REQUEST_COMPLETE.getDisplayName(), "Quá trình yêu cầu đã hoàn tất");
        assertEquals(NetworkLogStage.CONNECTION_RELEASED.getDisplayName(), "Kết nối bị chiếm dụng đã được giải phóng");
    }

    @Test
    public void shouldLocalizeFollowUpDecisionAndKeepRequestDetails() {
        String formatted = NetworkLogMessageFormatter.format(
                NetworkLogEventStage.FOLLOW_UP_DECISION,
                "Follow-up: true, response: 401, next: GET https://example.test/private");

        assertEquals(formatted, "Yêu cầu tiếp theo: .GET https://example.test/private（Mã phản hồi 401）");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.FOLLOW_UP_DECISION,
                "Follow-up: false, response: 200, next: none"), "");
    }

    @Test
    public void shouldLocalizeProxyConfigurationAndPreserveRouteDetails() {
        assertEquals(NetworkLogStage.PROXY_SELECT.getDisplayName(), "Cấu hình đại lý");
        String formatted = NetworkLogMessageFormatter.format(
                NetworkLogEventStage.PROXY_SELECT,
                "requestPolicy=DEFAULT, appProxyEnabled=false, appProxyMode=MANUAL, "
                        + "clientProxy=DIRECT, directHttpSocketFactoryJvmSocksBypass=true, "
                        + "manualProxyConfig=NOT_USED");

        assertEquals(formatted, "Proxy khách hàng: kết nối trực tiếp · Chính sách yêu cầu: Sử dụng giá trị mặc định");
    }

    @Test
    public void shouldLocalizeSystemSelectorAndKeepManualProxyEndpoint() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                        "requestPolicy=USE_PROXY, appProxyEnabled=true, appProxyMode=SYSTEM, "
                                + "clientProxy=SYSTEM_SELECTOR, directHttpSocketFactoryJvmSocksBypass=true, "
                                + "manualProxyConfig=NOT_USED"),
                "Proxy máy khách: Bộ chọn proxy hệ thống · Chiến lược yêu cầu: Sử dụng proxy");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                        "requestPolicy=NO_PROXY, appProxyEnabled=true, appProxyMode=MANUAL, "
                                + "clientProxy=SOCKS:127.0.0.1:1080, directHttpSocketFactoryJvmSocksBypass=true, "
                                + "manualProxyConfig=FIELDS_PRESENT"),
                "Đại lý khách hàng: .SOCKS:127.0.0.1:1080 · Request policy: Do not use a proxy");
    }

    @Test
    public void shouldKeepProxyHostContainingCommaWhileLocalizingDiagnostic() {
        String formatted = NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                "requestPolicy=USE_PROXY, appProxyEnabled=true, appProxyMode=MANUAL, "
                        + "clientProxy=HTTP:proxy,internal:8080, directHttpSocketFactoryJvmSocksBypass=true, "
                        + "manualProxyConfig=FIELDS_PRESENT");

        assertEquals(formatted, "Đại lý khách hàng: .HTTP:proxy,internal:8080 · Chiến lược yêu cầu: Sử dụng proxy");
    }

    @Test
    public void shouldLocalizeManualProxyFallbackReasons() {
        String diagnostic = "requestPolicy=USE_PROXY, appProxyEnabled=true, appProxyMode=MANUAL, "
                + "clientProxy=DIRECT, directHttpSocketFactoryJvmSocksBypass=true, manualProxyConfig=";

        assertTrue(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                diagnostic + "MISSING_HOST").contains("Cấu hình proxy thủ công: Thiếu máy chủ"));
        assertTrue(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                diagnostic + "INVALID_HOST").contains("Cấu hình proxy thủ công: Định dạng máy chủ không hợp lệ"));
        assertTrue(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                diagnostic + "INVALID_PORT").contains("Cấu hình proxy thủ công: Cổng không hợp lệ"));
    }

    @Test
    public void shouldRetainJvmSocksDiagnosticWhenBypassIsUnavailable() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                "requestPolicy=DEFAULT, appProxyEnabled=false, appProxyMode=MANUAL, "
                        + "clientProxy=DIRECT, directHttpSocketFactoryJvmSocksBypass=false, "
                        + "manualProxyConfig=NOT_USED"),
                "Proxy khách hàng: kết nối trực tiếp · Chính sách yêu cầu: Sử dụng giá trị mặc định\nTầng trệt Socket Không được bỏ qua JVM SOCKS cài đặt");
    }

    @Test
    public void shouldDistinguishReusedAndNewConnectionWithoutRepeatingReleaseRoute() {
        String route = "proxy=DIRECT, protocol=h2, remote=example.test:443";
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.CONNECTION_ACQUIRED,
                "Connection reused: " + route), "Kết nối đa kênh:" + route);
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.CONNECTION_ACQUIRED,
                "Connection acquired: " + route), "Kết nối mới:" + route);
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.CONNECTION_RELEASED,
                "Connection use released"), "");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.CALL_END, "done"), "");
    }

    @Test
    public void shouldLocalizeDirectRouteInterceptionWarning() {
        String formatted = NetworkLogMessageFormatter.format(NetworkLogEventStage.ROUTE_INTERCEPTED,
                "Direct route intercepted: local=127.0.0.1:53000, remote=10.175.180.2:80");
        assertTrue(formatted.startsWith("Kết nối trực tiếp bị nghi ngờ bị chặn: địa chỉ cục bộ 127.0.0.1:53000"), formatted);
        assertTrue(formatted.contains("Mục tiêu 10.175.180.2:80"), formatted);
        assertEquals(NetworkLogStage.fromEventStage(NetworkLogEventStage.ROUTE_INTERCEPTED),
                NetworkLogStage.ROUTE_INTERCEPTED);
        assertEquals(NetworkLogStage.ROUTE_INTERCEPTED.getDisplayName(), "Kết nối trực tiếp bị nghi ngờ đã bị chặn");
    }

    @Test
    public void shouldExplainHeaderSnapshotWithoutChangingHeaderValues() {
        String headers = "\n:authority: example.test\nX-Custom: Status: 200";
        for (String leadingNewline : new String[]{"", "\n"}) {
            assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_HEADERS_END,
                    leadingNewline + "HTTP/2 header view (regular headers and :authority):"
                            + headers),
                    leadingNewline + "HTTP/2 Yêu cầu xem tiêu đề (tiêu đề thông thường so với tiêu đề tiêu đề) :authority）：" + headers);
            assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_HEADERS_END,
                    leadingNewline + "Header snapshot:" + headers),
                    leadingNewline + "Yêu cầu chụp nhanh tiêu đề: ." + headers);
            assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_HEADERS_END,
                    leadingNewline + "Headers unchanged"), leadingNewline + "Tiêu đề yêu cầu không thay đổi");
        }
    }

    @Test
    public void shouldLocalizeResponseWaitWithoutChangingResponseHeaders() {
        String headers = "\nWait: 123ms\nX-Custom: Wait: 456ms\ncontent-type: application/json";
        for (NetworkLogEventStage stage : new NetworkLogEventStage[]{
                NetworkLogEventStage.RESPONSE_HEADERS_END, NetworkLogEventStage.RESPONSE_HEADERS_END_REDIRECT}) {
            for (String leadingNewline : new String[]{"", "\n"}) {
                assertEquals(NetworkLogMessageFormatter.format(stage,
                        leadingNewline + "HTTP/2 200\nWait: 1000ms" + headers),
                        leadingNewline + "HTTP/2 200\nĐang chờ phản hồi:1000ms" + headers);
            }
        }
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.RESPONSE_HEADERS_END,
                "Wait: 1000ms" + headers), "Wait: 1000ms" + headers);
    }

    @Test
    public void shouldDistinguishRetryFromTerminalFailure() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.RETRY_DECISION,
                "Retry: true, reason: connection reset"), "Tiếp tục thử lại: Có · lý do:connection reset");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.RETRY_DECISION,
                "Retry: false, reason: canceled"), "Tiếp tục thử lại: Không · lý do:canceled");
    }

    @Test
    public void shouldExplainUnavailablePreviewWithoutChangingRealRequestBody() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_BODY_START,
                "Request body preview unavailable"), "Yêu cầu xem trước nội dung không có sẵn");
        String actualBody = "\nRequest body preview unavailable";
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_BODY_START, actualBody), actualBody);
    }

    @Test
    public void shouldLocalizeRedirectDetailsAndFlowSummary() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REDIRECT,
                        "Redirect #1\nStatus: 302\nFrom: POST https://example.test/redirect\n"
                                + "To: GET https://other.test/get\nCross-Origin: true\n"
                                + "Removed Headers: Authorization, Cookie\nMethod Changed: POST → GET"),
                "KHÔNG.  1 chuyển hướng\nMã trạng thái:302\nnguồn:POST https://example.test/redirect\n"
                        + "Mục tiêu:GET https://other.test/get\nNguồn gốc chéo: Có\n"
                        + "Đã xóa tiêu đề yêu cầu: .Authorization, Cookie\nYêu cầu thay đổi phương thức:POST → GET");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_COMPLETE,
                        "Status: 200\nFinal: GET https://other.test/get\nRedirects: 1\nTotal: 457ms"),
                "Mã trạng thái:200\nYêu cầu cuối cùng: .GET https://other.test/get\nSố lần chuyển hướng:1\nTổng thời gian thực hiện:457ms");
    }

    @Test
    public void shouldExplainWhyRedirectWasNotFollowed() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REDIRECT,
                        "Redirect stopped: disabled, status: 302"),
                "Không theo dõi: Chuyển hướng đã đóng (mã trạng thái 302）");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REDIRECT,
                        "Redirect stopped: missing Location, status: 302"),
                "Không theo dõi: thiếu Location Tiêu đề phản hồi (mã trạng thái 302）");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REDIRECT,
                        "Redirect stopped: max redirects reached, status: 302, max: 20"),
                "Không tiếp tục theo dõi: Đã đạt đến số lượng chuyển hướng tối đa (mã trạng thái 302）\nSố lần chuyển hướng tối đa:20");
    }

    @Test
    public void shouldUseConciseEnglishWording() {
        I18nUtil.setLocale("en");
        assertEquals(NetworkLogStage.REQUEST_COMPLETE.getDisplayName(), "Request flow finished");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                        "requestPolicy=DEFAULT, appProxyEnabled=false, appProxyMode=MANUAL, "
                                + "clientProxy=DIRECT, directHttpSocketFactoryJvmSocksBypass=true, "
                                + "manualProxyConfig=NOT_USED"),
                "Client proxy: direct · Request policy: Use Default");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_COMPLETE,
                        "Status: 200\nRedirects: 1\nTotal: 457ms"),
                "Status: 200\nRedirects: 1\nTotal: 457ms");
    }

    @Test
    public void shouldLocalizeUnavailableProxyDiagnostics() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                "proxy diagnostics unavailable"), "Không thể lấy được thông tin chẩn đoán tác nhân");
    }

    @Test
    public void shouldLocalizeTlsLabelsWithoutChangingCertificateValues() {
        String formatted = NetworkLogMessageFormatter.format(
                NetworkLogEventStage.SECURE_CONNECT_END,
                "SSL connection using TLS_1_2\n"
                        + "Server certificate:\n"
                        + " subject: CN=example.test\n"
                        + "SSL certificate verify ok.\n");

        assertTrue(formatted.contains("SSL Sự liên quan:TLS_1_2"));
        assertTrue(formatted.contains("Chứng chỉ máy chủ: ."));
        assertTrue(formatted.contains("chủ đề: CN=example.test"));
        assertTrue(formatted.contains("SSL Certificate verification passed."));
    }

    @Test
    public void shouldStateDisabledVerificationWithoutClaimingSuccess() {
        String tlsConnection = "TLSv1.2 / TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.SECURE_CONNECT_END,
                        "TLS connection: " + tlsConnection + "\nVerification: disabled"),
                "TLS Sự liên quan:" + tlsConnection + "\nCertificate verification: closed");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.SECURE_CONNECT_END,
                        "TLS connection: " + tlsConnection + "\nVerification: passed"),
                "TLS Sự liên quan:" + tlsConnection + "\nCertificate verification: passed");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.SECURE_CONNECT_END,
                        "TLS connection: " + tlsConnection + "\nVerification: enabled"),
                "TLS Sự liên quan:" + tlsConnection + "\nCertificate Verification: Enabled");
    }

    @Test
    public void shouldPreserveCertificateWarningAndUtcDates() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.SECURE_CONNECT_END,
                        "TLS connection: TLSv1.3 / TLS_AES_128_GCM_SHA256\nVerification: disabled\n"
                                + "Server certificate:\n subject: CN=example.test issuer: literal text\n"
                                + " start date: 2026-01-01T00:00:00Z\n expire date: 2027-01-01T00:00:00Z\n"
                                + " issuer: CN=Test CA\n⚠️  Certificate Warning: certificate expired"),
                "TLS Sự liên quan:TLSv1.3 / TLS_AES_128_GCM_SHA256\nCertificate verification: closed\n"
                        + "Chứng chỉ máy chủ: .\nchủ đề: CN=example.test issuer: literal text\n"
                        + "ngày bắt đầu: 2026-01-01T00:00:00Z\nNgày hết hạn: 2027-01-01T00:00:00Z\n"
                        + "Phát hành bởi: CN=Test CA\n⚠️  Certificate warning: .certificate expired");
    }
}
