package com.laker.postman.service.js;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.SystemUtil;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.*;

/**
 * Postman Bộ thử nghiệm hoàn chỉnh của các thư viện tích hợp
 * <p>
 * Kiểm tra tất cả các tích hợp JavaScript Chức năng của thư viện được đảm bảo với  Postman phong cách
 * bao gồm: CryptoJS, Lodash, Moment, atob/btoa
 * </p>
 *
 * @author laker
 */
public class PostmanBuiltinLibrariesTest {

    private static final Logger log = LoggerFactory.getLogger(PostmanBuiltinLibrariesTest.class);

    private Context context;
    private HttpServer httpServer;
    private boolean originalRemoteRequireEnabled;
    private boolean originalAllowHttpRemoteRequire;
    private String originalAllowedRemoteHosts;
    private int originalRemoteConnectTimeoutMs;
    private int originalRemoteReadTimeoutMs;
    private int originalRemoteMaxBytes;
    private String originalEasyPostmanDataDir;

    @BeforeClass
    public void rememberSettings() throws IOException {
        Path tempDataDir = Files.createTempDirectory("easy-postman-js-settings-");
        originalEasyPostmanDataDir = System.getProperty("easyPostman.data.dir");
        System.setProperty("easyPostman.data.dir", tempDataDir.toString());
        SystemUtil.resetForTests();

        originalRemoteRequireEnabled = SettingManager.isRemoteJsRequireEnabled();
        originalAllowHttpRemoteRequire = SettingManager.isInsecureRemoteJsRequireEnabled();
        originalAllowedRemoteHosts = SettingManager.getRemoteJsRequireAllowedHosts();
        originalRemoteConnectTimeoutMs = SettingManager.getRemoteJsRequireConnectTimeoutMs();
        originalRemoteReadTimeoutMs = SettingManager.getRemoteJsRequireReadTimeoutMs();
        originalRemoteMaxBytes = SettingManager.getRemoteJsRequireMaxBytes();
    }

    @AfterClass
    public void restoreSettings() {
        restoreScriptSettings();
        if (originalEasyPostmanDataDir == null) {
            System.clearProperty("easyPostman.data.dir");
        } else {
            System.setProperty("easyPostman.data.dir", originalEasyPostmanDataDir);
        }
    }

    @BeforeMethod
    public void setUp() {
        log.info("khởi tạo JavaScript môi trường thực thi...");
        resetScriptSettingsToDefaults();
        context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> false)
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        // Tiêm tất cả polyfill（bao gồm Java nhận ra atob, btoa chức năng)
        JsPolyfillInjector.injectAll(context);

        log.info("Tất cả các thư viện tích hợp đều được tải");
    }

    @AfterMethod
    public void tearDown() {
        stopHttpServer();
        if (context != null) {
            context.close();
            log.info("JavaScript Môi trường thực thi đã bị tắt");
        }

        JsLibraryLoader.clearCache();
        restoreScriptSettings();
    }

    // ==================== CryptoJS Bài kiểm tra ====================

    @Test(description = "Bài kiểm tra CryptoJS - MD5 Băm")
    public void testCryptoJS_MD5() {
        String script = "CryptoJS.MD5('Hello World').toString();";
        Value result = context.eval("js", script);

        String hash = result.asString();
        assertEquals(hash, "b10a8db164e0754105b7a99be72e3fe5", "MD5 Giá trị băm không chính xác");
        log.info("✓ CryptoJS MD5 Thử nghiệm đã trôi qua: {}", hash);
    }

    @Test(description = "Bài kiểm tra CryptoJS - SHA256 Băm")
    public void testCryptoJS_SHA256() {
        String script = "CryptoJS.SHA256('Hello World').toString();";
        Value result = context.eval("js", script);

        String hash = result.asString();
        assertEquals(hash, "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e");
        log.info("✓ CryptoJS SHA256 Thử nghiệm đã trôi qua");
    }

    @Test(description = "Bài kiểm tra CryptoJS - AES Mã hóa và giải mã")
    public void testCryptoJS_AES() {
        String script = """
                var message = 'Secret Message';
                var key = 'my-secret-key';
                var encrypted = CryptoJS.AES.encrypt(message, key).toString();
                var decrypted = CryptoJS.AES.decrypt(encrypted, key).toString(CryptoJS.enc.Utf8);
                JSON.stringify({ encrypted: encrypted, decrypted: decrypted, match: message === decrypted });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"match\":true"), "AES Mã hóa và giải mã không thành công");
        assertTrue(json.contains("\"decrypted\":\"Secret Message\""), "Tin nhắn được giải mã không khớp");
        log.info("✓ CryptoJS AES Quá trình kiểm tra mã hóa và giải mã đã vượt qua");
    }

    @Test(description = "Bài kiểm tra CryptoJS - HMAC-SHA256 dấu hiệu")
    public void testCryptoJS_HMAC() {
        String script = """
                var message = 'POST/api/users{"userId":123}';
                var secret = 'api-secret-key';
                var signature = CryptoJS.HmacSHA256(message, secret).toString();
                signature;
                """;

        Value result = context.eval("js", script);
        String signature = result.asString();

        assertNotNull(signature);
        assertEquals(signature.length(), 64, "HMAC-SHA256 Độ dài chữ ký phải là  64");
        log.info("✓ CryptoJS HMAC-SHA256 Thử nghiệm đã trôi qua: {}", signature);
    }

    @Test(description = "Bài kiểm tra CryptoJS - Base64 Mã hóa")
    public void testCryptoJS_Base64() {
        String script = """
                var text = 'Hello, World!';
                var encoded = CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(text));
                var decoded = CryptoJS.enc.Base64.parse(encoded).toString(CryptoJS.enc.Utf8);
                JSON.stringify({ encoded: encoded, decoded: decoded, match: text === decoded });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"match\":true"));
        assertTrue(json.contains("SGVsbG8sIFdvcmxkIQ=="));
        log.info("✓ CryptoJS Base64 Thử nghiệm đã trôi qua");
    }

    // ==================== Lodash Bài kiểm tra ====================

    @Test(description = "Bài kiểm tra Lodash - Chống trùng lặp mảng")
    public void testLodash_Uniq() {
        String script = """
                var arr = [1, 2, 2, 3, 3, 3, 4, 5];
                var unique = _.uniq(arr);
                JSON.stringify(unique);
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "[1,2,3,4,5]");
        log.info("✓ Lodash uniq Thử nghiệm đã trôi qua");
    }

    @Test(description = "Bài kiểm tra Lodash - Lọc mảng")
    public void testLodash_Filter() {
        String script = """
                var numbers = [1, 2, 3, 4, 5, 6];
                var evens = _.filter(numbers, n => n % 2 === 0);
                JSON.stringify(evens);
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "[2,4,6]");
        log.info("✓ Lodash filter Thử nghiệm đã trôi qua");
    }

    @Test(description = "Bài kiểm tra Lodash - Ánh xạ mảng")
    public void testLodash_Map() {
        String script = """
                var numbers = [1, 2, 3];
                var doubled = _.map(numbers, n => n * 2);
                JSON.stringify(doubled);
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "[2,4,6]");
        log.info("✓ Lodash map Thử nghiệm đã trôi qua");
    }

    @Test(description = "Bài kiểm tra Lodash - Hoạt động đối tượng")
    public void testLodash_Pick() {
        String script = """
                var obj = { name: 'John', age: 30, city: 'NY', country: 'USA' };
                var picked = _.pick(obj, ['name', 'age']);
                JSON.stringify(picked);
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"name\":\"John\""));
        assertTrue(json.contains("\"age\":30"));
        assertFalse(json.contains("city"));
        log.info("✓ Lodash pick Thử nghiệm đã trôi qua");
    }

    @Test(description = "Bài kiểm tra Lodash - Nhóm mảng")
    public void testLodash_GroupBy() {
        String script = """
                var numbers = [6.1, 4.2, 6.3, 4.5];
                var grouped = _.groupBy(numbers, Math.floor);
                JSON.stringify(grouped);
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"4\""));
        assertTrue(json.contains("\"6\""));
        log.info("✓ Lodash groupBy Thử nghiệm đã trôi qua");
    }

    // ==================== Moment Bài kiểm tra ====================

    @Test(description = "Bài kiểm tra Moment - Định dạng ngày")
    public void testMoment_Format() {
        String script = """
                var date = moment('2024-01-15 10:30:45');
                var formatted = date.format('YYYY-MM-DD HH:mm:ss');
                formatted;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2024-01-15 10:30:45");
        log.info("✓ Moment Đã vượt qua bài kiểm tra định dạng");
    }

    @Test(description = "Bài kiểm tra Moment - Phép cộng và phép trừ ngày tháng")
    public void testMoment_Add() {
        String script = """
                var date = moment('2024-01-15');
                var nextWeek = date.add(7, 'days').format('YYYY-MM-DD');
                nextWeek;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2024-01-22");
        log.info("✓ Moment Ngày kiểm tra phép cộng và trừ đã được thông qua");
    }

    @Test(description = "Bài kiểm tra Moment - Phép trừ ngày")
    public void testMoment_Subtract() {
        String script = """
                var date = moment('2024-01-15');
                var lastMonth = date.subtract(1, 'months').format('YYYY-MM-DD');
                lastMonth;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2023-12-15");
        log.info("✓ Moment Đã vượt qua bài kiểm tra phép trừ ngày");
    }

    @Test(description = "Bài kiểm tra Moment - So sánh ngày")
    public void testMoment_Compare() {
        String script = """
                var date1 = moment('2024-01-01');
                var date2 = moment('2024-12-31');
                var isBefore = date1.isBefore(date2);
                var isAfter = date1.isAfter(date2);
                JSON.stringify({ isBefore: isBefore, isAfter: isAfter });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"isBefore\":true"));
        assertTrue(json.contains("\"isAfter\":false"));
        log.info("✓ Moment Kiểm tra so sánh ngày đã vượt qua");
    }

    @Test(description = "Bài kiểm tra Moment - Phân tích ngày")
    public void testMoment_Parse() {
        String script = """
                var date = moment('15/01/2024', 'DD/MM/YYYY');
                var formatted = date.format('YYYY-MM-DD');
                formatted;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2024-01-15");
        log.info("✓ Moment Kiểm tra phân tích ngày vượt qua");
    }


    // ==================== atob/btoa kiểm tra (Java đã nhận ra)====================

    @Test(description = "Bài kiểm tra btoa - Base64 mã hóa (Java đã nhận ra)")
    public void testBtoa_Encode() {
        String script = "btoa('Hello, World!');";
        Value result = context.eval("js", script);

        assertEquals(result.asString(), "SGVsbG8sIFdvcmxkIQ==");
        log.info("✓ btoa Đã vượt qua bài kiểm tra mã hóa");
    }

    @Test(description = "Bài kiểm tra atob - Base64 giải mã")
    public void testAtob_Decode() {
        String script = "atob('SGVsbG8sIFdvcmxkIQ==');";
        Value result = context.eval("js", script);

        assertEquals(result.asString(), "Hello, World!");
        log.info("✓ atob Đã vượt qua thử nghiệm giải mã");
    }

    @Test(description = "Bài kiểm tra atob/btoa - Mã hóa và giải mã chuyến đi khứ hồi")
    public void testAtobBtoa_RoundTrip() {
        String script = """
                var original = 'The quick brown fox jumps over the lazy dog';
                var encoded = btoa(original);
                var decoded = atob(encoded);
                JSON.stringify({ match: original === decoded, original: original, decoded: decoded });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"match\":true"));
        log.info("✓ atob/btoa Đã vượt qua bài kiểm tra chuyến đi khứ hồi");
    }

    // ==================== require() Bài kiểm tra ====================

    @Test(description = "Bài kiểm tra require() - trọng tải CryptoJS")
    public void testRequire_CryptoJS() {
        String script = """
                var CryptoJS = require('crypto-js');
                var hash = CryptoJS.MD5('test').toString();
                hash;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "098f6bcd4621d373cade4e832627b4f6");
        log.info("✓ require('crypto-js') Thử nghiệm đã trôi qua");
    }

    @Test(description = "Bài kiểm tra require() - trọng tải Lodash")
    public void testRequire_Lodash() {
        String script = """
                var _ = require('lodash');
                var arr = _.uniq([1, 2, 2, 3]);
                JSON.stringify(arr);
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "[1,2,3]");
        log.info("✓ require('lodash') Thử nghiệm đã trôi qua");
    }

    @Test(description = "Bài kiểm tra require() - trọng tải Moment")
    public void testRequire_Moment() {
        String script = """
                var moment = require('moment');
                var date = moment('2024-01-15').format('YYYY-MM-DD');
                date;
                """;

        Value result = context.eval("js", script);
        assertEquals(result.asString(), "2024-01-15");
        log.info("✓ require('moment') Thử nghiệm đã trôi qua");
    }

    @Test(description = "Bài kiểm tra require() - Tải tập lệnh đường dẫn tuyệt đối cục bộ")
    public void testRequire_LocalFile() throws IOException {
        Path scriptFile = Files.createTempFile("easy-postman-local-lib-", ".js");
        Files.writeString(scriptFile, """
                module.exports = {
                    version: '1.0.0',
                    sign: function(input) {
                        return input + '-signed';
                    }
                };
                """, StandardCharsets.UTF_8);

        Value result = context.eval("js", "require('" + toJsString(scriptFile.toAbsolutePath().normalize().toString()) + "').sign('token');");
        assertEquals(result.asString(), "token-signed");
    }

    @Test(description = "Bài kiểm tra require() - Các tập lệnh cục bộ hỗ trợ nội bộ các phụ thuộc đường dẫn tương đối")
    public void testRequire_LocalFileRelativeDependency() throws IOException {
        Path tempDir = Files.createTempDirectory("easy-postman-local-module-");
        Path childModule = tempDir.resolve("child.js");
        Path parentModule = tempDir.resolve("parent.js");

        Files.writeString(childModule, "module.exports = { value: 'child-module' };", StandardCharsets.UTF_8);
        Files.writeString(parentModule, """
                var child = require('./child.js');
                module.exports = { value: 'parent:' + child.value };
                """, StandardCharsets.UTF_8);

        Value result = context.eval("js", "require('" + toJsString(parentModule.toAbsolutePath().normalize().toString()) + "').value;");
        assertEquals(result.asString(), "parent:child-module");
    }

    @Test(description = "Bài kiểm tra require() - Tập lệnh từ xa bị tắt theo mặc định")
    public void testRequire_RemoteDisabledByDefault() {
        Value result = context.eval("js", """
                try {
                    require('https://example.com/remote-lib.js');
                    'no-error';
                } catch (e) {
                    String(e.message);
                }
                """);
        assertTrue(result.asString().contains("Remote JavaScript require() is disabled"));
    }

    @Test(description = "Bài kiểm tra require() - Các tập lệnh từ xa có thể được tải khi được bật")
    public void testRequire_RemoteEnabledWithExplicitOptIn() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/remote-lib.js", exchange -> {
            byte[] body = "module.exports = { value: 'remote-ok' };".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/javascript; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        httpServer.start();

        SettingManager.setRemoteJsRequireEnabled(true);
        SettingManager.setInsecureRemoteJsRequireEnabled(true);
        SettingManager.setRemoteJsRequireConnectTimeoutMs(1000);
        SettingManager.setRemoteJsRequireReadTimeoutMs(1000);
        SettingManager.setRemoteJsRequireMaxBytes(65536);

        int port = httpServer.getAddress().getPort();
        Value result = context.eval("js", "require('http://127.0.0.1:" + port + "/remote-lib.js').value;");
        assertEquals(result.asString(), "remote-ok");
    }

    @Test(description = "Bài kiểm tra Context Khi hồ bơi được tái sử dụng require() Bộ đệm sẽ bị xóa sau mỗi lần thực hiện")
    public void testContextPoolClearsRequireCacheBetweenRuns() throws Exception {
        Path scriptFile = Files.createTempFile("easy-postman-cache-reset-", ".js");
        Files.writeString(scriptFile, "module.exports = { version: 'v1' };", StandardCharsets.UTF_8);

        JsContextPool pool = new JsContextPool(1);
        String normalizedPath = scriptFile.toAbsolutePath().normalize().toString();
        try {
            JsContextPool.PooledContext firstBorrow = pool.borrowContext(1000);
            Value firstResult = firstBorrow.getContext().eval("js", "require('" + toJsString(normalizedPath) + "').version;");
            assertEquals(firstResult.asString(), "v1");
            pool.returnContext(firstBorrow);

            Files.writeString(scriptFile, "module.exports = { version: 'v2' };", StandardCharsets.UTF_8);

            JsContextPool.PooledContext secondBorrow = pool.borrowContext(1000);
            Value secondResult = secondBorrow.getContext().eval("js", "require('" + toJsString(normalizedPath) + "').version;");
            assertEquals(secondResult.asString(), "v2");
            pool.returnContext(secondBorrow);
        } finally {
            pool.shutdown();
        }
    }

    // ==================== Kiểm tra kịch bản toàn diện ====================

    @Test(description = "Postman Kịch bản điển hình - API Request signature")
    public void testPostmanScenario_APISignature() {
        String script = """
                // 1. Tạo yêu cầu ID（sử dụng số ngẫu nhiên)
                var requestId = 'req_' + Math.random().toString(36).substring(2, 15);

                // 2. Nhận dấu thời gian
                var timestamp = moment().unix();

                // 3. Xây dựng dữ liệu chữ ký
                var signData = {
                    requestId: requestId,
                    timestamp: timestamp,
                    method: 'POST',
                    path: '/api/users'
                };

                // 4. sử dụng Lodash Sắp xếp và ghép nối
                var signString = _.map(
                    _.sortBy(_.toPairs(signData), 0),
                    pair => pair[0] + '=' + pair[1]
                ).join('&');

                // 5. phát ra HMAC-SHA256 dấu hiệu
                var signature = CryptoJS.HmacSHA256(signString, 'api-secret').toString();

                // 6. Trả về kết quả
                JSON.stringify({
                    requestId: requestId,
                    timestamp: timestamp,
                    signature: signature,
                    hasValidRequestId: requestId.startsWith('req_'),
                    hasValidSignature: signature.length === 64
                });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"hasValidRequestId\":true"));
        assertTrue(json.contains("\"hasValidSignature\":true"));
        log.info("✓ API Bài kiểm tra kịch bản chữ ký đã thông qua");
    }

    @Test(description = "Postman Kịch bản điển hình - Xác minh phản hồi")
    public void testPostmanScenario_ResponseValidation() {
        String script = """
                // mô phỏng API dữ liệu phản hồi
                var response = {
                    status: 'success',
                    data: {
                        userId: 12345,
                        username: 'john_doe',
                        email: 'john@example.com',
                        createdAt: '2024-01-15T10:30:00Z',
                        roles: ['user', 'admin']
                    }
                };

                // sử dụng Lodash Xác thực các trường bắt buộc
                var requiredFields = ['userId', 'username', 'email'];
                var hasAllFields = _.every(requiredFields, field => _.has(response.data, field));

                // sử dụng Moment Thời gian xác minh
                var createdDate = moment(response.data.createdAt);
                var isValidDate = createdDate.isValid();

                // Xác thực các loại cơ bản
                var isValidStatus = response.status === 'success';
                var isValidUserId = typeof response.data.userId === 'number';
                var isValidRoles = Array.isArray(response.data.roles) && response.data.roles.length === 2;

                JSON.stringify({
                    hasAllFields: hasAllFields,
                    isValidDate: isValidDate,
                    isValidStatus: isValidStatus,
                    isValidUserId: isValidUserId,
                    isValidRoles: isValidRoles,
                    allPassed: hasAllFields && isValidDate && isValidStatus && isValidUserId && isValidRoles
                });
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        assertTrue(json.contains("\"allPassed\":true"));
        log.info("✓ Đã vượt qua thử nghiệm kịch bản xác minh phản hồi");
    }

    @Test(description = "Kiểm tra toàn diện - Tất cả các kiểm tra tính sẵn có của thư viện")
    public void testAllLibrariesAvailable() {
        String script = """
                var libraries = {
                    // JavaScript thư viện
                    cryptojs: typeof CryptoJS !== 'undefined',
                    lodash: typeof _ !== 'undefined',
                    moment: typeof moment !== 'undefined',

                    // Java chức năng được thực hiện
                    atob: typeof atob === 'function',
                    btoa: typeof btoa === 'function',
                    MD5: typeof MD5 === 'function',
                    SHA256: typeof SHA256 === 'function',
                    timestamp: typeof timestamp === 'function',

                    // khác
                    require: typeof require === 'function'
                };

                JSON.stringify(libraries);
                """;

        Value result = context.eval("js", script);
        String json = result.asString();

        // Xác minh rằng tất cả các thư viện đã được tải
        assertTrue(json.contains("\"cryptojs\":true"), "CryptoJS Không được tải");
        assertTrue(json.contains("\"lodash\":true"), "Lodash Không được tải");
        assertTrue(json.contains("\"moment\":true"), "Moment Không được tải");
        assertTrue(json.contains("\"atob\":true"), "atob Không được tải");
        assertTrue(json.contains("\"btoa\":true"), "btoa Không được tải");
        assertTrue(json.contains("\"MD5\":true"), "MD5 Không được tải");
        assertTrue(json.contains("\"SHA256\":true"), "SHA256 Không được tải");
        assertTrue(json.contains("\"timestamp\":true"), "timestamp Không được tải");
        assertTrue(json.contains("\"require\":true"), "require Không được tải");

        log.info("✓ Tất cả các cuộc kiểm tra tính khả dụng của thư viện đều vượt qua: {}", json);
    }

    @Test(description = "Kiểm tra hiệu suất - Thời gian tải thư viện")
    public void testLibraryLoadingPerformance() {
        long startTime = System.currentTimeMillis();

        Context testContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> false)
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        JsLibraryLoader.injectBuiltinLibraries(testContext);
        JsLibraryLoader.injectRequireFunction(testContext);

        long loadTime = System.currentTimeMillis() - startTime;
        testContext.close();

        assertTrue(loadTime < 1000, "Tải thư viện mất quá nhiều thời gian: " + loadTime + "ms");
        log.info("✓ Bài kiểm tra hiệu suất tải thư viện đã vượt qua nhưng tốn nhiều thời gian: {}ms", loadTime);
    }

    private void stopHttpServer() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }
    }

    private void restoreScriptSettings() {
        SettingManager.setRemoteJsRequireEnabled(originalRemoteRequireEnabled);
        SettingManager.setInsecureRemoteJsRequireEnabled(originalAllowHttpRemoteRequire);
        SettingManager.setRemoteJsRequireAllowedHosts(originalAllowedRemoteHosts);
        SettingManager.setRemoteJsRequireConnectTimeoutMs(originalRemoteConnectTimeoutMs);
        SettingManager.setRemoteJsRequireReadTimeoutMs(originalRemoteReadTimeoutMs);
        SettingManager.setRemoteJsRequireMaxBytes(originalRemoteMaxBytes);
    }

    private void resetScriptSettingsToDefaults() {
        SettingManager.setRemoteJsRequireEnabled(false);
        SettingManager.setInsecureRemoteJsRequireEnabled(false);
        SettingManager.setRemoteJsRequireAllowedHosts("");
        SettingManager.setRemoteJsRequireConnectTimeoutMs(3000);
        SettingManager.setRemoteJsRequireReadTimeoutMs(5000);
        SettingManager.setRemoteJsRequireMaxBytes(512 * 1024);
    }

    private String toJsString(String raw) {
        return raw.replace("\\", "\\\\").replace("'", "\\'");
    }

}
