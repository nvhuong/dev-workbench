package com.laker.postman.service.js;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.service.js.api.Cookie;
import com.laker.postman.service.js.api.PostmanApiContext;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;


/**
 * Bài kiểm tra pm.cookies.get() Có thể nhận được phản hồi chính xác Cookie
 */
public class CookieGetTest {

    private Context context;
    private PostmanApiContext pm;

    @BeforeMethod
    public void setUp() {
        context = Context.newBuilder("js")
                .allowAllAccess(true)
                .build();
        pm = new PostmanApiContext(null);
    }

    @AfterMethod
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testGetResponseCookieWorks() {
        // Tạo bao gồm Set-Cookie phản hồi tiêu đề
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Arrays.asList(
                "JSESSIONID=ABC123; Path=/; HttpOnly",
                "token=XYZ789; Domain=example.com; Secure"
        ));

        // Đặt phản hồi
        pm.setResponse(response);

        // Bài kiểm tra pm.getResponseCookie() - Nó sẽ hoạt động
        Cookie jsessionId = pm.getResponseCookie("JSESSIONID");
        assertNotNull(jsessionId, "pm.getResponseCookie('JSESSIONID') should return a Cookie");
        assertEquals("JSESSIONID", jsessionId.name);
        assertEquals("ABC123", jsessionId.value);
        assertEquals("/", jsessionId.path);
        assertTrue(jsessionId.httpOnly);

        Cookie token = pm.getResponseCookie("token");
        assertNotNull(token, "pm.getResponseCookie('token') should return a Cookie");
        assertEquals("token", token.name);
        assertEquals("XYZ789", token.value);
        assertEquals("example.com", token.domain);
        assertTrue(token.secure);
    }

    @Test
    public void testCookiesGetNowWorks() {
        // Tạo bao gồm Set-Cookie phản hồi tiêu đề
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Arrays.asList(
                "JSESSIONID=ABC123; Path=/; HttpOnly",
                "token=XYZ789; Domain=example.com; Secure"
        ));

        // Đặt phản hồi (sẽ được tự động điền cookies）
        pm.setResponse(response);

        // Bài kiểm tra pm.cookies.get() - Nó sẽ hoạt động ngay bây giờ! .
        Cookie jsessionId = pm.cookies.get("JSESSIONID");
        assertNotNull(jsessionId, "pm.cookies.get('JSESSIONID') should now return a Cookie after fix");
        assertEquals("JSESSIONID", jsessionId.name);
        assertEquals("ABC123", jsessionId.value);

        Cookie token = pm.cookies.get("token");
        assertNotNull(token, "pm.cookies.get('token') should now return a Cookie after fix");
        assertEquals("token", token.name);
        assertEquals("XYZ789", token.value);
    }

    @Test
    public void testCookiesAllWorks() {
        // Tạo có chứa nhiều Cookie phản hồi
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Arrays.asList(
                "JSESSIONID=ABC123; Path=/",
                "token=XYZ789; Domain=example.com",
                "user_id=12345; Secure"
        ));

        pm.setResponse(response);

        // Bài kiểm tra pm.cookies.getAll()
        java.util.List<Cookie> allCookies = pm.cookies.getAll();
        assertNotNull(allCookies);
        assertEquals(3, allCookies.size(), "Should have 3 cookies");

        // Xác minh tất cả Cookie Tất cả đều được lấp đầy
        Set<String> cookieNames = new HashSet<>();
        for (Cookie cookie : allCookies) {
            cookieNames.add(cookie.name);
        }
        assertTrue(cookieNames.contains("JSESSIONID"));
        assertTrue(cookieNames.contains("token"));
        assertTrue(cookieNames.contains("user_id"));
    }

    @Test
    public void testCookiesHasWorks() {
        // Tạo phản hồi
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Collections.singletonList("JSESSIONID=ABC123"));

        pm.setResponse(response);

        // Bài kiểm tra pm.cookies.has()
        assertTrue(pm.cookies.has("JSESSIONID"), "pm.cookies.has('JSESSIONID') should return true");
        assertFalse(pm.cookies.has("nonexistent"), "pm.cookies.has('nonexistent') should return false");
    }

    @Test
    public void testJavaScriptCookiesGet() {
        // Tạo phản hồi
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Arrays.asList(
                "JSESSIONID=ABC123; Path=/; HttpOnly",
                "token=XYZ789"
        ));

        pm.setResponse(response);

        // Vào JavaScript bài kiểm tra giữa kỳ
        context.getBindings("js").putMember("pm", pm);

        // Bài kiểm tra pm.cookies.get()
        Value result = context.eval("js",
                "var cookie = pm.cookies.get('JSESSIONID');" +
                        "cookie ? cookie.value : null"
        );
        assertNotNull(result);
        assertFalse(result.isNull(), "pm.cookies.get('JSESSIONID') should return a valid cookie");
        assertEquals("ABC123", result.asString());

        // Bài kiểm tra pm.cookies.getAll()
        Value allResult = context.eval("js",
                "var allCookies = pm.cookies.getAll();" +
                        "allCookies.length"
        );
        assertEquals(2, allResult.asInt(), "Should have 2 cookies");

        // Bài kiểm tra pm.cookies.has()
        Value hasResult = context.eval("js", "pm.cookies.has('JSESSIONID')");
        assertTrue(hasResult.asBoolean());
    }

    @Test
    public void testScriptComparison() {
        // Mô phỏng các tình huống do người dùng báo cáo
        HttpResponse response = new HttpResponse();
        response.code = 200;
        response.headers = new HashMap<>();
        response.headers.put("Set-Cookie", Collections.singletonList("JSESSIONID=SESSION123"));

        pm.setResponse(response);
        context.getBindings("js").putMember("pm", pm);

        // phương pháp1: pm.getResponseCookie() - Luôn luôn làm việc
        Value method1 = context.eval("js",
                "var jsessionId = pm.getResponseCookie('JSESSIONID');" +
                        "jsessionId ? jsessionId.value : null"
        );
        assertNotNull(method1);
        assertEquals("SESSION123", method1.asString(), "pm.getResponseCookie() should work");

        // phương pháp2: pm.cookies.get() - Nó cũng sẽ hoạt động ngay bây giờ
        Value method2 = context.eval("js",
                "var cookie = pm.cookies.get('JSESSIONID');" +
                        "cookie ? cookie.value : null"
        );
        assertNotNull(method2);
        assertEquals("SESSION123", method2.asString(), "pm.cookies.get() should now work after fix");

        // Cả hai phương pháp sẽ trả về cùng một giá trị
        assertEquals(method1.asString(), method2.asString(),
                "Both methods should return the same cookie value");
    }
}
