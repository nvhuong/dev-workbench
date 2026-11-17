package com.laker.postman.service.js;

import org.graalvm.polyglot.Context;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * CryptoJS Kiểm tra chức năng - sử dụng TestNG
 */
public class CryptoJSTest {

    @Test(description = "sử dụng CryptoJS（được tải tự động)")
    public void testDirectUsage() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            // Thư viện tiêm
            JsLibraryLoader.injectBuiltinLibraries(context);

            // Bài kiểm tra AES Mã hóa
            String script = """
                    var encrypted = CryptoJS.AES.encrypt('Hello World', 'secret key 123');
                    encrypted.toString();
                    """;

            String result = context.eval("js", script).asString();
            assertNotNull(result, "Kết quả mã hóa không được để trống");
            assertFalse(result.isEmpty(), "Kết quả mã hóa không được là một chuỗi trống");
            System.out.println("✅ Mã hóa thành công: " + result);
        }
    }

    @Test(description = "Kịch bản gốc (đầy đủ chức năng)")
    public void testOriginalScript() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .out(System.out)
                .err(System.err)
                .build()) {

            // Tiêm thư viện và  polyfill
            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);

            // Tập lệnh gốc của bạn (phiên bản đơn giản hóa)
            String script = """
                    var secret_key = "1234567890123456";
                    var app_code = "myapp";
                    var app_secret = "mysecret";
                    var password = "mypassword";

                    // Bài kiểm tra btoa
                    var authz_header = "Basic " + btoa(app_code + ":" + app_secret);
                    console.log("Authorization: " + authz_header);

                    // Bài kiểm tra CryptoJS
                    var key = CryptoJS.enc.Latin1.parse(secret_key);
                    var iv = key;

                    var encrypted = CryptoJS.AES.encrypt(
                        password,
                        key,
                        {
                            iv: iv,
                            mode: CryptoJS.mode.CBC,
                            padding: CryptoJS.pad.ZeroPadding
                        }
                    );

                    console.log("Encrypted: " + encrypted.toString());

                    // Bài kiểm tra encodeURIComponent
                    var encrypted_password = encodeURIComponent(encrypted.toString());
                    console.log("Encrypted & Encoded: " + encrypted_password);

                    encrypted_password;
                    """;

            String result = context.eval("js", script).asString();
            assertNotNull(result, "Kết quả cuối cùng không được để trống");
            assertFalse(result.isEmpty(), "Kết quả cuối cùng không được là một chuỗi trống");
            assertTrue(result.contains("%"), "URL Nên được bao gồm sau khi mã hóa %");
            System.out.println("✅ Kết quả cuối cùng: " + result);
        }
    }
}

