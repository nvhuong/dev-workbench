package com.nvhuong.devworkbench.service.js;

import com.nvhuong.devworkbench.model.Environment;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;


import com.nvhuong.devworkbench.service.js.api.DevWorkbenchApiContext;
import com.nvhuong.devworkbench.service.js.api.ScriptRequestAccessor;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.testng.Assert.*;

/**
 * CryptoJS AES Kiểm tra mã hóa - Hoàn thành thử nghiệm kịch bản
 * Bài kiểm tra pm.environment、pm.request、CryptoJS、btoa、encodeURIComponent Sử dụng tích hợp các chức năng
 */
public class CryptoJSEncryptionTest {

    @Test(description = "Kiểm tra hoàn tất CryptoJS AES Kịch bản tập lệnh được mã hóa")
    public void testCryptoJSEncryptionWithPmContext() {
        // 1. Chuẩn bị các biến môi trường
        Environment env = new Environment();
        env.setName("Test Environment");
        env.set("secret_key", "1234567890123456"); // 16Khóa byte
        env.set("app_code", "myAppCode");
        env.set("app_secret", "myAppSecret");

        // 2. Chuẩn bị đối tượng yêu cầu
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.id = "test-request-001";
        preparedRequest.url = "https://api.example.com/login?username=testuser&password=mypassword123";
        preparedRequest.method = "POST";
        preparedRequest.headersList = new ArrayList<>();
        preparedRequest.paramsList = new ArrayList<>();

        // Thêm tham số truy vấn
        HttpParam usernameParam = new HttpParam();
        usernameParam.setKey("username");
        usernameParam.setValue("testuser");
        usernameParam.setEnabled(true);
        preparedRequest.paramsList.add(usernameParam);

        HttpParam passwordParam = new HttpParam();
        passwordParam.setKey("password");
        passwordParam.setValue("mypassword123");
        passwordParam.setEnabled(true);
        preparedRequest.paramsList.add(passwordParam);

        // 3. Tạo DevWorkbenchApiContext
        DevWorkbenchApiContext pm = new DevWorkbenchApiContext(env);
        pm.request = new ScriptRequestAccessor(preparedRequest);

        // 4. thực hiện JavaScript kịch bản
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .option("js.ecmascript-version", "2021")
                .out(System.out)
                .err(System.err)
                .build()) {

            // tiêm CryptoJS và Polyfill
            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);

            // tiêm pm đối tượng
            context.getBindings("js").putMember("pm", pm);

            // Thực thi tập lệnh mã hóa hoàn chỉnh
            String script = """
                    // Nhận cấu hình từ các biến môi trường
                    var secret_key = pm.environment.get("secret_key");
                    var app_code = pm.environment.get("app_code");
                    var app_secret = pm.environment.get("app_secret");

                    // phát ra Basic Tiêu đề xác thực
                    var authz_header = "Basic " + btoa(app_code + ":" + app_secret);

                    // Lấy tham số mật khẩu (sử dụng  DevWorkbench tiêu chuẩn API）
                    var password = pm.request.url.query.all()[1].value;

                    // Phân tích chìa khóa
                    var key = CryptoJS.enc.Latin1.parse(secret_key);
                    var iv = key;

                    // AES Mã hóa
                    var encrypted = CryptoJS.AES.encrypt(
                        password,
                        key, {
                            iv: iv,
                            mode: CryptoJS.mode.CBC,
                            padding: CryptoJS.pad.ZeroPadding
                        }
                    );

                    // URL Mã hóa
                    var encrypted_password = encodeURIComponent(encrypted.toString());

                    // Nhật ký đầu ra
                    console.log("Original Password: " + password);
                    console.log("Encrypted Password: " + encrypted_password);
                    console.log("Authorization Header: " + authz_header);

                    // Sửa đổi các tham số yêu cầu (bằng cách sử dụng DevWorkbench tiêu chuẩn API）
                    pm.request.url.query.all()[1].value = encrypted_password;

                    // Thêm tiêu đề xác thực
                    pm.request.headers.add({
                        key: 'Authorization',
                        value: authz_header
                    });

                    // Trả về mật khẩu được mã hóa để xác minh
                    encrypted_password;
                    """;

            Value result = context.eval("js", script);
            String encryptedPassword = result.asString();

            // đồng bộ JavaScript Sửa đổi các trường được trả về đối tượng cơ bản
            pm.request.url.query.sync();

            // 5. Xác minh kết quả
            assertNotNull(encryptedPassword, "Mật khẩu được mã hóa không được để trống");
            assertFalse(encryptedPassword.isEmpty(), "Mật khẩu được mã hóa không được là một chuỗi trống");
            assertTrue(encryptedPassword.contains("%"), "URL Nên được bao gồm sau khi mã hóa %");
            assertNotEquals(encryptedPassword, "mypassword123", "Mật khẩu phải được mã hóa");

            // Xác minh rằng các tham số yêu cầu đã được sửa đổi
            assertEquals(preparedRequest.paramsList.get(1).getValue(), encryptedPassword,
                    "The password parameter in the request should have been updated with the encrypted value");

            // Xác minh Authorization Tiêu đề đã được thêm vào
            assertEquals(preparedRequest.headersList.size(), 1, "Một tiêu đề yêu cầu nên được thêm vào");
            HttpHeader authHeader = preparedRequest.headersList.get(0);
            assertEquals(authHeader.getKey(), "Authorization", "Tên tiêu đề yêu cầu phải là Authorization");
            assertTrue(authHeader.getValue().startsWith("Basic "), "Nó nên như vậy Basic Chứng nhận");

            // Xác minh Basic Nội dung của tiêu đề xác thực
            String expectedAuth = "Basic bXlBcHBDb2RlOm15QXBwU2VjcmV0"; // Base64("myAppCode:myAppSecret")
            assertEquals(authHeader.getValue(), expectedAuth, "Basic Tiêu đề xác thực phải được mã hóa chính xác");

            System.out.println("✅ Đã vượt qua bài kiểm tra tập lệnh mã hóa hoàn chỉnh!");
            System.out.println("   Mật khẩu gốc: mypassword123");
            System.out.println("   Sau khi mã hóa: " + encryptedPassword);
            System.out.println("   Tiêu đề xác thực: " + authHeader.getValue());
        }
    }

    @Test(description = "Bài kiểm tra CryptoJS AES Khả năng đảo ngược của mã hóa")
    public void testCryptoJSEncryptionDecryption() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .out(System.out)
                .err(System.err)
                .build()) {

            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);

            String script = """
                    var secret_key = "1234567890123456";
                    var password = "mypassword123";

                    // Mã hóa
                    var key = CryptoJS.enc.Latin1.parse(secret_key);
                    var iv = key;

                    var encrypted = CryptoJS.AES.encrypt(
                        password,
                        key, {
                            iv: iv,
                            mode: CryptoJS.mode.CBC,
                            padding: CryptoJS.pad.ZeroPadding
                        }
                    );

                    var encryptedStr = encrypted.toString();
                    console.log("Encrypted: " + encryptedStr);

                    // Giải mã
                    var decrypted = CryptoJS.AES.decrypt(
                        encryptedStr,
                        key, {
                            iv: iv,
                            mode: CryptoJS.mode.CBC,
                            padding: CryptoJS.pad.ZeroPadding
                        }
                    );

                    var decryptedStr = decrypted.toString(CryptoJS.enc.Utf8).replace(/\\0+$/, '');
                    console.log("Decrypted: " + decryptedStr);

                    decryptedStr;
                    """;

            Value result = context.eval("js", script);
            String decrypted = result.asString();

            assertEquals(decrypted, "mypassword123", "Mật khẩu ban đầu sẽ được khôi phục sau khi giải mã");
            System.out.println("✅ Quá trình kiểm tra mã hóa và giải mã đã vượt qua!");
        }
    }

    @Test(description = "Kiểm tra độ dài khóa khác nhau AES Mã hóa")
    public void testDifferentKeyLengths() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .out(System.out)
                .err(System.err)
                .build()) {

            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);

            // Bài kiểm tra 128-bit (16Byte)
            String script128 = """
                    var key = CryptoJS.enc.Latin1.parse("1234567890123456");
                    var encrypted = CryptoJS.AES.encrypt("test", key, {iv: key, mode: CryptoJS.mode.CBC});
                    encrypted.toString();
                    """;
            Value result128 = context.eval("js", script128);
            assertNotNull(result128.asString(), "128-bit Mã hóa khóa sẽ thành công");

            // Bài kiểm tra 192-bit (24Byte)
            String script192 = """
                    var key = CryptoJS.enc.Latin1.parse("123456789012345678901234");
                    var encrypted = CryptoJS.AES.encrypt("test", key, {iv: key, mode: CryptoJS.mode.CBC});
                    encrypted.toString();
                    """;
            Value result192 = context.eval("js", script192);
            assertNotNull(result192.asString(), "192-bit Mã hóa khóa sẽ thành công");

            // Bài kiểm tra 256-bit (32Byte)
            String script256 = """
                    var key = CryptoJS.enc.Latin1.parse("12345678901234567890123456789012");
                    var encrypted = CryptoJS.AES.encrypt("test", key, {iv: key, mode: CryptoJS.mode.CBC});
                    encrypted.toString();
                    """;
            Value result256 = context.eval("js", script256);
            assertNotNull(result256.asString(), "256-bit Mã hóa khóa sẽ thành công");

            System.out.println("✅ Đã vượt qua các thử nghiệm với độ dài khóa khác nhau!");
        }
    }

    @Test(description = "Bài kiểm tra btoa và atob Chức năng")
    public void testBtoaAndAtob() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            JsPolyfillInjector.injectAll(context);

            // Bài kiểm tra btoa
            String btoaScript = "btoa('myAppCode:myAppSecret')";
            Value encoded = context.eval("js", btoaScript);
            assertEquals(encoded.asString(), "bXlBcHBDb2RlOm15QXBwU2VjcmV0", "btoa Việc mã hóa phải chính xác");

            // Bài kiểm tra atob
            String atobScript = "atob('bXlBcHBDb2RlOm15QXBwU2VjcmV0')";
            Value decoded = context.eval("js", atobScript);
            assertEquals(decoded.asString(), "myAppCode:myAppSecret", "atob Giải mã phải chính xác");

            System.out.println("✅ btoa/atob Thử nghiệm đã trôi qua!");
        }
    }

    @Test(description = "Bài kiểm tra encodeURIComponent và decodeURIComponent")
    public void testEncodeDecodeURIComponent() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            JsPolyfillInjector.injectAll(context);

            String testString = "Hello World! Xin chào thế giới #special@chars&=+";

            String encodeScript = String.format("encodeURIComponent('%s')",
                    testString.replace("'", "\\'"));
            Value encoded = context.eval("js", encodeScript);
            String encodedStr = encoded.asString();

            assertTrue(encodedStr.contains("%"), "Nên được bao gồm sau khi mã hóa %");
            assertFalse(encodedStr.contains(" "), "Dấu cách phải được mã hóa");
            assertFalse(encodedStr.contains("#"), "# nên được mã hóa");

            // Giải mã thử nghiệm
            String decodeScript = String.format("decodeURIComponent('%s')", encodedStr);
            Value decoded = context.eval("js", decodeScript);
            assertEquals(decoded.asString(), testString, "Chuỗi gốc sẽ được khôi phục sau khi giải mã");

            System.out.println("✅ encodeURIComponent/decodeURIComponent Thử nghiệm đã trôi qua!");
        }
    }

    @Test(description = "Bài kiểm tra decodeURIComponent Giữ dấu cộng")
    public void testDecodeURIComponentKeepsPlusSign() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            JsPolyfillInjector.injectAll(context);

            Value decoded = context.eval("js", "decodeURIComponent('a+b%20c')");
            assertEquals(decoded.asString(), "a+b c", "decodeURIComponent Không nên sử dụng + Được giải mã dưới dạng khoảng trắng");
        }
    }

    @Test(description = "Bài kiểm tra decodeURI Đã đặt trước URL Ký tự dành riêng")
    public void testDecodeUriKeepsReservedCharactersEscaped() {
        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            JsPolyfillInjector.injectAll(context);

            Value decoded = context.eval("js", "decodeURI('https://example.com/a%2Fb?q=a%2Bb%20c&x=%E6%B5%8B%E8%AF%95#frag')");
            assertEquals(
                    decoded.asString(),
                    "https://example.com/a%2Fb?q=a%2Bb c&x=Bài kiểm tra#frag",
                    "decodeURI Mã hóa ký tự dành riêng trong đường dẫn và truy vấn phải được giữ nguyên"
            );
        }
    }

    @Test(description = "Kiểm tra việc nhận và cài đặt biến môi trường")
    public void testEnvironmentVariables() {
        Environment env = new Environment();
        env.setName("Test Env");
        env.set("key1", "value1");
        env.set("key2", "value2");

        DevWorkbenchApiContext pm = new DevWorkbenchApiContext(env);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            context.getBindings("js").putMember("pm", pm);

            // Kiểm tra để thu được các biến môi trường
            String getScript = "pm.environment.get('key1')";
            Value value1 = context.eval("js", getScript);
            assertEquals(value1.asString(), "value1", "Bạn sẽ có thể nhận được các biến môi trường");

            // Kiểm tra các biến môi trường cài đặt
            context.eval("js", "pm.environment.set('key3', 'value3')");
            assertEquals(env.get("key3"), "value3", "Có thể thiết lập các biến môi trường");

            System.out.println("✅ Đã vượt qua bài kiểm tra biến môi trường!");
        }
    }

    @Test(description = "Kiểm tra quyền truy cập và sửa đổi các tham số yêu cầu")
    public void testRequestParamsAccess() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.url = "https://example.com?param1=value1&param2=value2";
        preparedRequest.paramsList = new ArrayList<>();

        HttpParam param1 = new HttpParam();
        param1.setKey("param1");
        param1.setValue("value1");
        param1.setEnabled(true);
        preparedRequest.paramsList.add(param1);

        HttpParam param2 = new HttpParam();
        param2.setKey("param2");
        param2.setValue("value2");
        param2.setEnabled(true);
        preparedRequest.paramsList.add(param2);

        Environment env = new Environment();
        DevWorkbenchApiContext pm = new DevWorkbenchApiContext(env);
        pm.request = new ScriptRequestAccessor(preparedRequest);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            context.getBindings("js").putMember("pm", pm);

            // Kiểm tra các tham số truy cập (sử dụng  DevWorkbench tiêu chuẩn API）
            String accessScript = "pm.request.url.query.all()[0].value";
            Value value = context.eval("js", accessScript);
            assertEquals(value.asString(), "value1", "Tham số đầu tiên phải có thể truy cập được");

            // Kiểm tra các tham số đã sửa đổi (sử dụng  DevWorkbench tiêu chuẩn API）
            context.eval("js", "pm.request.url.query.all()[1].value = 'newvalue2'");

            // Sửa đổi đồng bộ
            pm.request.url.query.sync();

            assertEquals(preparedRequest.paramsList.get(1).getValue(), "newvalue2",
                    "Giá trị tham số nên được sửa đổi");

            // Bài kiểm tra nhận được số lượng tham số
            String countScript = "pm.request.url.query.all().length";
            Value count = context.eval("js", countScript);
            assertEquals(count.asInt(), 2, "Nên có2các thông số");

            System.out.println("✅ Kiểm tra truy cập tham số yêu cầu đã vượt qua!");
        }
    }

    @Test(description = "Kiểm tra việc bổ sung các tiêu đề yêu cầu")
    public void testRequestHeadersAdd() {
        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.headersList = new ArrayList<>();

        Environment env = new Environment();
        DevWorkbenchApiContext pm = new DevWorkbenchApiContext(env);
        pm.request = new ScriptRequestAccessor(preparedRequest);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .build()) {

            context.getBindings("js").putMember("pm", pm);

            // Thêm tiêu đề yêu cầu
            context.eval("js", """
                    pm.request.headers.add({
                        key: 'Authorization',
                        value: 'Bearer token123'
                    })
                    """);

            assertEquals(preparedRequest.headersList.size(), 1, "Một tiêu đề yêu cầu nên được thêm vào");
            assertEquals(preparedRequest.headersList.get(0).getKey(), "Authorization");
            assertEquals(preparedRequest.headersList.get(0).getValue(), "Bearer token123");

            // Thêm nhiều tiêu đề yêu cầu
            context.eval("js", """
                    pm.request.headers.add({
                        key: 'Content-Type',
                        value: 'application/json'
                    })
                    """);

            assertEquals(preparedRequest.headersList.size(), 2, "Nên có2tiêu đề yêu cầu");

            System.out.println("✅ Kiểm tra bổ sung tiêu đề yêu cầu đã vượt qua!");
        }
    }

    @Test(description = "Người dùng thử nghiệm được cung cấp thô DevWorkbench kịch bản - Được sử dụng đầy đủ .value tài sản")
    public void testUserOriginalDevWorkbenchScript() {
        // Mô phỏng đồ thật DevWorkbench bối cảnh
        Environment env = new Environment();
        env.setName("Production");
        env.set("secret_key", "1234567890123456");
        env.set("app_code", "myAppCode");
        env.set("app_secret", "myAppSecret");

        PreparedRequest preparedRequest = new PreparedRequest();
        preparedRequest.url = "https://api.example.com/login?username=admin&password=mypassword123";
        preparedRequest.method = "POST";
        preparedRequest.headersList = new ArrayList<>();
        preparedRequest.paramsList = new ArrayList<>();

        // Thêm tham số truy vấn
        HttpParam usernameParam = new HttpParam();
        usernameParam.setKey("username");
        usernameParam.setValue("admin");
        usernameParam.setEnabled(true);
        preparedRequest.paramsList.add(usernameParam);

        HttpParam passwordParam = new HttpParam();
        passwordParam.setKey("password");
        passwordParam.setValue("mypassword123");
        passwordParam.setEnabled(true);
        preparedRequest.paramsList.add(passwordParam);

        DevWorkbenchApiContext pm = new DevWorkbenchApiContext(env);
        pm.request = new ScriptRequestAccessor(preparedRequest);

        try (Context context = Context.newBuilder("js")
                .allowAllAccess(true)
                .allowNativeAccess(true)
                .option("js.ecmascript-version", "2021")
                .out(System.out)
                .err(System.err)
                .build()) {

            JsLibraryLoader.injectBuiltinLibraries(context);
            JsPolyfillInjector.injectAll(context);
            context.getBindings("js").putMember("pm", pm);

            // Bản gốc của người dùng DevWorkbench Script (hoàn toàn không được sửa đổi, hãy sử dụng .value thuộc tính)
            String userScript = """
                    var secret_key = pm.environment.get("secret_key");
                    var app_code = pm.environment.get("app_code");
                    var app_secret = pm.environment.get("app_secret");

                    var authz_header = "Basic " + btoa(app_code + ":" + app_secret);
                    var password = pm.request.url.query.all()[1].value;

                    var key = CryptoJS.enc.Latin1.parse(secret_key);
                    var iv = key;

                    // Mã hóa
                    var encrypted = CryptoJS.AES.encrypt(
                      password,
                      key, {
                        iv: iv,
                        mode: CryptoJS.mode.CBC,
                        padding: CryptoJS.pad.ZeroPadding
                      }
                    );
                    var encrypted_password = encodeURIComponent(encrypted.toString());

                    console.log(encrypted_password);
                    pm.request.url.query.all()[1].value = encrypted_password;
                    pm.request.headers.add(
                        {key: 'Authorization', value: authz_header }
                    );

                    encrypted_password;
                    """;

            Value result = context.eval("js", userScript);
            String encryptedPassword = result.asString();

            // đồng bộ JavaScript Sửa đổi các trường
            pm.request.url.query.sync();

            // Xác minh kết quả
            assertNotNull(encryptedPassword, "Mật khẩu được mã hóa không được để trống");
            assertTrue(encryptedPassword.contains("%"), "URL Nên được bao gồm sau khi mã hóa %");
            assertNotEquals(encryptedPassword, "mypassword123", "Mật khẩu phải được mã hóa");

            // Xác minh rằng tham số mật khẩu đã được sửa đổi
            assertEquals(preparedRequest.paramsList.get(1).getValue(), encryptedPassword,
                    "The password parameter in the request should have been updated with the encrypted value");

            // Xác minh tiêu đề xác thực
            assertEquals(preparedRequest.headersList.size(), 1, "Một tiêu đề yêu cầu nên được thêm vào");
            HttpHeader authHeader = preparedRequest.headersList.get(0);
            assertEquals(authHeader.getKey(), "Authorization");
            assertTrue(authHeader.getValue().startsWith("Basic "));

            System.out.println("✅ Bản gốc của người dùng DevWorkbench Bài kiểm tra kịch bản đã trôi qua!");
            System.out.println("   Mật khẩu được mã hóa: " + encryptedPassword);
            System.out.println("   Tiêu đề xác thực: " + authHeader.getValue());
        }
    }
}
