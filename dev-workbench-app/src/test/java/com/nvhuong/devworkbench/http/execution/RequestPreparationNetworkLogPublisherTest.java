package com.nvhuong.devworkbench.http.execution;

import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.http.runtime.observation.NetworkLogEvent;
import com.nvhuong.devworkbench.http.runtime.observation.NetworkLogEventStage;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpRequestVersions;
import com.nvhuong.devworkbench.request.model.RequestBodyTypes;
import com.nvhuong.devworkbench.util.I18nUtil;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RequestPreparationNetworkLogPublisherTest {

    @DataProvider
    public Object[][] summaryLocales() {
        return new Object[][]{
                {Locale.ENGLISH, "Configured headers: 1 enabled / 2 total",
                        "Query parameters: 1 enabled / 2 total", "Path variables: 1 enabled / 1 total",
                        "Form-data rows: 1 enabled / 2 total", "Urlencoded rows: 0 enabled / 1 total",
                        "Pre-request script: present", "TLS verification: Disabled", "HTTP preference: Automatic negotiation"},
                {Locale.forLanguageTag("vi"), "Header request đã cấu hình: bật 1 / tổng 2",
                        "Tham số query: bật 1 / tổng 2", "Biến đường dẫn: bật 1 / tổng 1",
                        "Dòng form-data: bật 1 / tổng 2", "Dòng urlencoded: bật 0 / tổng 1",
                        "Script trước: đã cấu hình", "Xác minh chứng chỉ TLS: Tắt", "Giao thức HTTP: Tự động thương lượng"}
        };
    }

    @Test(dataProvider = "summaryLocales")
    public void shouldPublishLocalizedConfiguredCountsWithoutRepeatingRequestDetails(
            Locale locale, String headers, String params, String pathVariables, String formData,
            String urlencoded, String preScript, String tls, String httpPreference) {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            I18nUtil.setLocale(locale);
            String message = publish(configuredRequest());

            for (String expected : List.of(headers, params, pathVariables, formData, urlencoded,
                    preScript, tls, httpPreference, "5000 ms", "raw")) {
                assertTrue(message.contains(expected), message);
            }
            for (String repeatedOrPrivate : List.of("Method:", "URL:", "Header Names:",
                    "example.test", "Authorization", "Bearer token", "X-Disabled", "Proxy Policy:")) {
                assertFalse(message.contains(repeatedOrPrivate), message);
            }
            assertFalse(message.contains("Post-response script"), message);
            assertFalse(message.contains("Script sau"), message);
            assertFalse(message.contains("+0ms"), message);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    @Test
    public void shouldOmitRoutineHeadersEmptyRowsAndAbsentScriptsAndExplainUnlimitedTimeout() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            I18nUtil.setLocale(Locale.ENGLISH);
            PreparedRequest request = new PreparedRequest();
            request.bodyType = RequestBodyTypes.BODY_TYPE_NONE;
            request.headersList = List.of(new HttpHeader(true, "Accept", "*/*"),
                    new HttpHeader(true, "User-Agent", "DevWorkbench Client"),
                    new HttpHeader(false, " ", ""));
            request.paramsList = List.of(new HttpParam(true, "", "", ""));
            request.pathVariablesList = List.of();
            request.formDataList = List.of(new HttpFormData());
            request.urlencodedList = List.of(new HttpFormUrlencoded());
            request.prescript = " ";
            request.postscript = null;
            request.followRedirects = true;
            request.cookieJarEnabled = true;
            request.httpVersion = HttpRequestVersions.HTTP_2;

            String message = publish(request);

            assertEquals(message.strip().lines().count(), 2L, message);
            assertTrue(message.contains("Redirects: Enabled · Cookie jar: Enabled"), message);
            assertTrue(message.contains("TLS verification: Disabled · HTTP preference: HTTP/2 · Timeout: No timeout"), message);
            assertFalse(message.contains("0 enabled / 0 total"), message);
            assertFalse(message.contains("Body type:"), message);
            assertFalse(message.contains("Configured headers:"), message);
            assertFalse(message.contains("+0ms"), message);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    @Test
    public void shouldNotPublishWhenNetworkLogIsDisabled() {
        PreparedRequest request = new PreparedRequest();
        List<NetworkLogEvent> events = new ArrayList<>();
        request.networkLogSink = events::add;

        RequestPreparationNetworkLogPublisher.publish(request);

        assertTrue(events.isEmpty());
    }

    private static PreparedRequest configuredRequest() {
        PreparedRequest request = new PreparedRequest();
        request.method = "POST";
        request.url = "https://example.test/api?debug=true";
        request.bodyType = RequestBodyTypes.BODY_TYPE_RAW;
        request.headersList = new ArrayList<>();
        request.headersList.add(new HttpHeader(true, "Authorization", "Bearer token"));
        request.headersList.add(new HttpHeader(false, "X-Disabled", "no"));
        request.headersList.add(new HttpHeader(true, "", ""));
        request.paramsList = new ArrayList<>();
        request.paramsList.add(new HttpParam(true, "debug", "true", ""));
        request.paramsList.add(new HttpParam(false, "page", "2", ""));
        request.paramsList.add(null);
        request.pathVariablesList = List.of(new HttpParam(true, "id", "1", ""));
        request.formDataList = List.of(new HttpFormData(true, "file", HttpFormData.TYPE_FILE, "a.txt"),
                new HttpFormData(false, "label", HttpFormData.TYPE_TEXT, "sample"));
        request.urlencodedList = List.of(new HttpFormUrlencoded(false, "unused", "value"));
        request.prescript = "pm.environment.set('token', 'abc')";
        request.postscript = "";
        request.followRedirects = true;
        request.cookieJarEnabled = true;
        request.sslVerificationEnabled = false;
        request.httpVersion = HttpRequestVersions.AUTO;
        request.requestTimeoutMs = 5_000;
        return request;
    }

    private static String publish(PreparedRequest request) {
        request.enableNetworkLog = true;
        List<NetworkLogEvent> events = new ArrayList<>();
        request.networkLogSink = events::add;

        RequestPreparationNetworkLogPublisher.publish(request);

        assertEquals(events.size(), 1);
        NetworkLogEvent event = events.get(0);
        assertEquals(event.stage(), NetworkLogEventStage.REQUEST_PREPARED);
        return event.message();
    }
}
