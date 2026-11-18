package com.nvhuong.devworkbench.service.js.api;

import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class DevWorkbenchResponseCookieSupportTest {

    @Test
    public void shouldCollectEveryCaseInsensitiveSetCookieHeader() {
        HttpResponse response = new HttpResponse();
        response.headers = new LinkedHashMap<>();
        response.headers.put("Set-Cookie", List.of("session=abc; Path=/; HttpOnly"));
        response.headers.put("set-cookie", List.of("theme=dark; Secure; SameSite=Lax"));

        List<Cookie> cookies = DevWorkbenchResponseCookieSupport.extract(response);

        assertEquals(cookies.size(), 2);
        assertEquals(cookies.get(0).name, "session");
        assertEquals(cookies.get(0).path, "/");
        assertTrue(cookies.get(0).httpOnly);
        assertEquals(cookies.get(1).name, "theme");
        assertTrue(cookies.get(1).secure);
        assertEquals(cookies.get(1).sameSite, "Lax");
    }

    @Test
    public void shouldParseAttributesWithoutDependingOnDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Cookie cookie = DevWorkbenchResponseCookieSupport.parse(
                    "token=value; DOMAIN=.example.com; MAX-AGE=60; secure; httponly"
            );

            assertEquals(cookie.domain, ".example.com");
            assertEquals(cookie.maxAge, Integer.valueOf(60));
            assertTrue(cookie.secure);
            assertTrue(cookie.httpOnly);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void shouldIgnoreInvalidMaxAgeAndMissingResponses() {
        Cookie cookie = DevWorkbenchResponseCookieSupport.parse("token=value; Max-Age=invalid");

        assertNull(cookie.maxAge);
        assertNull(DevWorkbenchResponseCookieSupport.find(null, "token"));
        assertFalse(DevWorkbenchResponseCookieSupport.extract(null).iterator().hasNext());
    }
}
