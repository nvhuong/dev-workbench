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
        I18nUtil.setLocale("vi");
    }

    @AfterMethod
    public void restoreLocale() {
        I18nUtil.setLocale(originalVietnamese ? "vi" : "en");
    }

    @Test
    public void shouldLocalizeStageDisplayNameWithoutChangingTechnicalName() {
        assertEquals(NetworkLogStage.CALL_START.getStageName(), "RequestStart");
        assertEquals(NetworkLogStage.CALL_START.getDisplayName(), "Bắt đầu request");
        assertEquals(NetworkLogStage.CALL_END.getDisplayName(), "Kết thúc request");
        assertEquals(NetworkLogStage.REQUEST_COMPLETE.getDisplayName(), "Hoàn tất luồng request");
        assertEquals(NetworkLogStage.CONNECTION_RELEASED.getDisplayName(), "Đã trả kết nối về pool");
    }

    @Test
    public void shouldLocalizeFollowUpDecisionAndKeepRequestDetails() {
        String formatted = NetworkLogMessageFormatter.format(
                NetworkLogEventStage.FOLLOW_UP_DECISION,
                "Follow-up: true, response: 401, next: GET https://example.test/private");

        assertEquals(formatted, "Request tiếp theo: GET https://example.test/private (mã phản hồi 401)");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.FOLLOW_UP_DECISION,
                "Follow-up: false, response: 200, next: none"), "");
    }

    @Test
    public void shouldLocalizeProxyConfigurationAndPreserveRouteDetails() {
        assertEquals(NetworkLogStage.PROXY_SELECT.getDisplayName(), "Cấu hình proxy");
        String formatted = NetworkLogMessageFormatter.format(
                NetworkLogEventStage.PROXY_SELECT,
                "requestPolicy=DEFAULT, appProxyEnabled=false, appProxyMode=MANUAL, "
                        + "clientProxy=DIRECT, directHttpSocketFactoryJvmSocksBypass=true, "
                        + "manualProxyConfig=NOT_USED");

        assertEquals(formatted, "Proxy client: Kết nối trực tiếp · Chính sách request: Sử dụng giá trị mặc định");
    }

    @Test
    public void shouldLocalizeSystemSelectorAndKeepManualProxyEndpoint() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                        "requestPolicy=USE_PROXY, appProxyEnabled=true, appProxyMode=SYSTEM, "
                                + "clientProxy=SYSTEM_SELECTOR, directHttpSocketFactoryJvmSocksBypass=true, "
                                + "manualProxyConfig=NOT_USED"),
                "Proxy client: Bộ chọn proxy hệ thống · Chính sách request: Sử dụng proxy");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                        "requestPolicy=NO_PROXY, appProxyEnabled=true, appProxyMode=MANUAL, "
                                + "clientProxy=SOCKS:127.0.0.1:1080, directHttpSocketFactoryJvmSocksBypass=true, "
                                + "manualProxyConfig=FIELDS_PRESENT"),
                "Proxy client: SOCKS:127.0.0.1:1080 · Chính sách request: Không dùng proxy");
    }

    @Test
    public void shouldKeepProxyHostContainingCommaWhileLocalizingDiagnostic() {
        String formatted = NetworkLogMessageFormatter.format(NetworkLogEventStage.PROXY_SELECT,
                "requestPolicy=USE_PROXY, appProxyEnabled=true, appProxyMode=MANUAL, "
                        + "clientProxy=HTTP:proxy,internal:8080, directHttpSocketFactoryJvmSocksBypass=true, "
                        + "manualProxyConfig=FIELDS_PRESENT");

        assertEquals(formatted, "Proxy client: HTTP:proxy,internal:8080 · Chính sách request: Sử dụng proxy");
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
                "Proxy client: Kết nối trực tiếp · Chính sách request: Sử dụng giá trị mặc định\nSocket bên dưới không bỏ qua cấu hình JVM SOCKS");
    }

    @Test
    public void shouldDistinguishReusedAndNewConnectionWithoutRepeatingReleaseRoute() {
        String route = "proxy=DIRECT, protocol=h2, remote=example.test:443";
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.CONNECTION_ACQUIRED,
                "Connection reused: " + route), "Dùng lại kết nối: " + route);
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.CONNECTION_ACQUIRED,
                "Connection acquired: " + route), "Kết nối mới: " + route);
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.CONNECTION_RELEASED,
                "Connection use released"), "");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.CALL_END, "done"), "");
    }

    @Test
    public void shouldLocalizeDirectRouteInterceptionWarning() {
        String formatted = NetworkLogMessageFormatter.format(NetworkLogEventStage.ROUTE_INTERCEPTED,
                "Direct route intercepted: local=127.0.0.1:53000, remote=10.175.180.2:80");
        assertTrue(formatted.startsWith("Kết nối trực tiếp có vẻ đã bị chặn: địa chỉ cục bộ 127.0.0.1:53000"), formatted);
        assertTrue(formatted.contains("đích 10.175.180.2:80"), formatted);
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
                    leadingNewline + "Header request HTTP/2 (header thường và :authority):" + headers);
            assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_HEADERS_END,
                    leadingNewline + "Header snapshot:" + headers),
                    leadingNewline + "Ảnh chụp header request:" + headers);
            assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_HEADERS_END,
                    leadingNewline + "Headers unchanged"), leadingNewline + "Header request không thay đổi");
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
                        leadingNewline + "HTTP/2 200\nĐang chờ phản hồi: 1000ms" + headers);
            }
        }
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.RESPONSE_HEADERS_END,
                "Wait: 1000ms" + headers), "Wait: 1000ms" + headers);
    }

    @Test
    public void shouldDistinguishRetryFromTerminalFailure() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.RETRY_DECISION,
                "Retry: true, reason: connection reset"), "Thử lại: Có · Lý do: connection reset");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.RETRY_DECISION,
                "Retry: false, reason: canceled"), "Thử lại: Không · Lý do: canceled");
    }

    @Test
    public void shouldExplainUnavailablePreviewWithoutChangingRealRequestBody() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_BODY_START,
                "Request body preview unavailable"), "Không có bản xem trước body request");
        String actualBody = "\nRequest body preview unavailable";
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_BODY_START, actualBody), actualBody);
    }

    @Test
    public void shouldLocalizeRedirectDetailsAndFlowSummary() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REDIRECT,
                        "Redirect #1\nStatus: 302\nFrom: POST https://example.test/redirect\n"
                                + "To: GET https://other.test/get\nCross-Origin: true\n"
                                + "Removed Headers: Authorization, Cookie\nMethod Changed: POST → GET"),
                "Chuyển hướng #1\nMã trạng thái: 302\nTừ: POST https://example.test/redirect\n"
                        + "Đến: GET https://other.test/get\nKhác origin: Có\n"
                        + "Header đã bị bỏ: Authorization, Cookie\nĐổi phương thức: POST → GET");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REQUEST_COMPLETE,
                        "Status: 200\nFinal: GET https://other.test/get\nRedirects: 1\nTotal: 457ms"),
                "Mã trạng thái: 200\nRequest cuối cùng: GET https://other.test/get\nSố lần chuyển hướng: 1\nTổng thời gian: 457ms");
    }

    @Test
    public void shouldExplainWhyRedirectWasNotFollowed() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REDIRECT,
                        "Redirect stopped: disabled, status: 302"),
                "Không theo chuyển hướng: Chuyển hướng đã tắt (mã trạng thái 302)");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REDIRECT,
                        "Redirect stopped: missing Location, status: 302"),
                "Không theo chuyển hướng: Thiếu header Location trong response (mã trạng thái 302)");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.REDIRECT,
                        "Redirect stopped: max redirects reached, status: 302, max: 20"),
                "Không theo chuyển hướng: Đã đạt đến số lượng chuyển hướng tối đa (mã trạng thái 302)\nSố lần chuyển hướng tối đa: 20");
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
                "proxy diagnostics unavailable"), "Không lấy được thông tin chẩn đoán proxy");
    }

    @Test
    public void shouldLocalizeTlsLabelsWithoutChangingCertificateValues() {
        String formatted = NetworkLogMessageFormatter.format(
                NetworkLogEventStage.SECURE_CONNECT_END,
                "SSL connection using TLS_1_2\n"
                        + "Server certificate:\n"
                        + " subject: CN=example.test\n"
                        + "SSL certificate verify ok.\n");

        assertTrue(formatted.contains("Kết nối SSL: TLS_1_2"));
        assertTrue(formatted.contains("Chứng chỉ máy chủ:"));
        assertTrue(formatted.contains("Chủ thể: CN=example.test"));
        assertTrue(formatted.contains("Xác minh chứng chỉ SSL thành công."));
    }

    @Test
    public void shouldStateDisabledVerificationWithoutClaimingSuccess() {
        String tlsConnection = "TLSv1.2 / TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256";
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.SECURE_CONNECT_END,
                        "TLS connection: " + tlsConnection + "\nVerification: disabled"),
                "Kết nối TLS: " + tlsConnection + "\nXác minh chứng chỉ: đã tắt");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.SECURE_CONNECT_END,
                        "TLS connection: " + tlsConnection + "\nVerification: passed"),
                "Kết nối TLS: " + tlsConnection + "\nXác minh chứng chỉ: thành công");
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.SECURE_CONNECT_END,
                        "TLS connection: " + tlsConnection + "\nVerification: enabled"),
                "Kết nối TLS: " + tlsConnection + "\nXác minh chứng chỉ: đã bật");
    }

    @Test
    public void shouldPreserveCertificateWarningAndUtcDates() {
        assertEquals(NetworkLogMessageFormatter.format(NetworkLogEventStage.SECURE_CONNECT_END,
                        "TLS connection: TLSv1.3 / TLS_AES_128_GCM_SHA256\nVerification: disabled\n"
                                + "Server certificate:\n subject: CN=example.test issuer: literal text\n"
                                + " start date: 2026-01-01T00:00:00Z\n expire date: 2027-01-01T00:00:00Z\n"
                                + " issuer: CN=Test CA\n⚠️  Certificate Warning: certificate expired"),
                "Kết nối TLS: TLSv1.3 / TLS_AES_128_GCM_SHA256\nXác minh chứng chỉ: đã tắt\n"
                        + "Chứng chỉ máy chủ:\nChủ thể: CN=example.test issuer: literal text\n"
                        + "Ngày bắt đầu: 2026-01-01T00:00:00Z\nNgày hết hạn: 2027-01-01T00:00:00Z\n"
                        + "Phát hành bởi: CN=Test CA\n⚠️  Cảnh báo chứng chỉ: certificate expired");
    }
}
