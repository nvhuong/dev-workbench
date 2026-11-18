package com.nvhuong.devworkbench.service.js;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.nvhuong.devworkbench.model.Workspace;
import com.nvhuong.devworkbench.service.WorkspaceService;
import com.nvhuong.devworkbench.service.setting.SettingManager;
import com.nvhuong.devworkbench.util.SystemUtil;

/**
 * JS Trình tải thư viện bên ngoài
 * <p>
 * Hỗ trợ tải tích hợp JS thư viện và các phần bên ngoài do người dùng định nghĩa JS thư viện, mô phỏng Node.js của require() cơ chế.
 * Và DevWorkbench Phong cách, giao hàng DevWorkbench Hỗ trợ thư viện tích hợp cho các kiểu.
 * </p>
 *
 * <h3>Các thư viện tích hợp được hỗ trợ: .</h3>
 * <ul>
 *   <li><b>crypto-js</b>: Thư viện tiền điện tử, được hỗ trợ AES、DES、MD5、SHA và các thuật toán khác</li>
 *   <li><b>lodash</b>: JavaScript Một thư viện công cụ thực tế cung cấp nhiều phương pháp phụ trợ lập trình chức năng</li>
 *   <li><b>moment</b>: Thư viện xử lý ngày và giờ, định dạng, phân tích và thao tác ngày tháng</li>
 * </ul>
 *
 * <h3>Java Các chức năng toàn cầu được triển khai (bởi  JsPolyfillInjector cung cấp):</h3>
 * <ul>
 *   <li><b>uuid()</b>: UUID máy phát điện</li>
 *   <li><b>atob/btoa</b>: Base64 Mã hóa và giải mã</li>
 *   <li><b>MD5/SHA1/SHA256/SHA512</b>: Hàm băm</li>
 *   <li><b>timestamp()</b>: chức năng dấu thời gian</li>
 *   <li><b>encodeURIComponent/decodeURIComponent</b>: URL Codec</li>
 * </ul>
 *
 * <h3>Cách sử dụng:</h3>
 * <pre>{@code
 * // cách1: Sử dụng trực tiếp các thư viện tích hợp (được tải tự động trên toàn cầu)
 * var encrypted = CryptoJS.AES.encrypt('message', 'secret');
 * var id = uuid.v4();
 * var now = moment().format('YYYY-MM-DD');
 * var result = _.filter([1, 2, 3], n => n > 1);
 *
 * // cách2: sử dụng require() trọng tải
 * var CryptoJS = require('crypto-js');
 * var _ = require('lodash');
 * var moment = require('moment');
 *
 * // cách3: Tải thư viện bên ngoài tùy chỉnh (từ đường dẫn tệp)
 * var myLib = require('/path/to/my-library.js');
 *
 * // cách4: Tải các thư viện bên ngoài tùy chỉnh (từ  URL）
 * var myLib = require('https://cdn.example.com/library.js');
 * }</pre>
 *
 * @author nvhuong
 */
@Slf4j
@UtilityClass
public class JsLibraryLoader {

    /**
     * Bộ đệm thư viện tích hợp - Tránh tải lặp đi lặp lại
     */
    private static final Map<String, String> BUILTIN_LIBRARY_CACHE = new ConcurrentHashMap<>();

    /**
     * Bí danh thư viện tích hợp -> Tên kinh điển
     */
    private static final Map<String, String> BUILTIN_LIBRARY_IDS = new HashMap<>();

    /**
     * Bảng ánh xạ thư viện tích hợp - Tên kinh điển -> Đường dẫn tài nguyên
     */
    private static final Map<String, String> BUILTIN_LIBRARY_PATHS = new HashMap<>();

    /**
     * Đường dẫn thư viện không đổi - Được lưu trữ thống nhất js-libs Thư mục
     */
    private static final String JS_LIBS_DIR = "/js-libs/";
    private static final String CRYPTO_JS_PATH = JS_LIBS_DIR + "crypto-js.min.js";
    private static final String LODASH_PATH = JS_LIBS_DIR + "lodash.min.js";
    private static final String MOMENT_PATH = JS_LIBS_DIR + "moment.min.js";

    static {
        // Đăng ký thư viện tích hợp - CryptoJS
        registerBuiltInLibrary("crypto-js", CRYPTO_JS_PATH, "crypto-js", "cryptojs", "CryptoJS");

        // Đăng ký thư viện tích hợp - Lodash
        registerBuiltInLibrary("lodash", LODASH_PATH, "lodash", "_");

        // Đăng ký thư viện tích hợp - Moment
        registerBuiltInLibrary("moment", MOMENT_PATH, "moment", "Moment");


        log.debug("Registered {} built-in JavaScript libraries", BUILTIN_LIBRARY_PATHS.size());
    }

    /**
     * Đưa mục truy cập thư viện tích hợp vào JS bối cảnh.
     * <p>
     * Các thư viện tích hợp lớn được hiển thị thông qua các thuộc tính tải chậm toàn cầu và người dùng có thể truy cập trực tiếp CryptoJS、_、moment hoặc require()
     * Chỉ khi đó mã thư viện tương ứng mới thực sự được tải và thực thi. Kịch bản kiểm tra căng thẳng nếu chỉ được sử dụng pm.variables、Math.random và các khả năng cơ bản khác.
     * Không dành cho tất cả mọi người Context Tải bổ sung lodash / crypto-js / moment。
     * </p>
     *
     * @param context GraalVM JS bối cảnh
     */
    public static void injectBuiltinLibraries(Context context) {
        log.debug("Registering lazy built-in JavaScript libraries...");

        // tiêm crypto polyfill（vì CryptoJS Cung cấp khả năng tạo số ngẫu nhiên)
        injectCryptoPolyfill(context);

        injectRequireFunction(context);
        installLazyBuiltinGlobal(context, "crypto-js", "CryptoJS");
        installLazyBuiltinGlobal(context, "lodash", "_");
        installLazyBuiltinGlobal(context, "moment", "moment");

        log.debug("Lazy built-in JavaScript libraries registered successfully");
    }

    private static void installLazyBuiltinGlobal(Context context, String libraryName, String globalName) {
        try {
            String script = """
                    (function() {
                        var moduleName = '%s';
                        var globalName = '%s';
                        if (Object.prototype.hasOwnProperty.call(globalThis, globalName)) {
                            return;
                        }
                        Object.defineProperty(globalThis, globalName, {
                            configurable: true,
                            get: function() {
                                var value = globalThis.require(moduleName);
                                Object.defineProperty(globalThis, globalName, {
                                    value: value,
                                    writable: true,
                                    configurable: true
                                });
                                return value;
                            }
                        });
                    })();
                    """.formatted(escapeForJs(libraryName), escapeForJs(globalName));
            context.eval("js", script);
        } catch (Exception e) {
            log.warn("Failed to register lazy library {}: {}", libraryName, e.getMessage());
        }
    }

    /**
     * tiêm crypto polyfill，vì CryptoJS Cung cấp khả năng tạo số ngẫu nhiên
     */
    private static void injectCryptoPolyfill(Context context) {
        String cryptoPolyfill = """
                if (typeof crypto === 'undefined') {
                    globalThis.crypto = {
                        getRandomValues: function(array) {
                            for (var i = 0; i < array.length; i++) {
                                array[i] = Math.floor(Math.random() * 256);
                            }
                            return array;
                        }
                    };
                }
                """;
        try {
            context.eval("js", cryptoPolyfill);
            log.debug("Injected crypto polyfill");
        } catch (Exception e) {
            log.warn("Failed to inject crypto polyfill: {}", e.getMessage());
        }
    }

    /**
     * tiêm require() Chức năng, hỗ trợ tải động bên ngoài JS thư viện
     * <p>
     * Người dùng có thể sử dụng nó trong các tập lệnh require('library-name') hoặc require('/path/to/file.js')
     * để tải các thư viện tích hợp hoặc thư viện tùy chỉnh.
     * </p>
     *
     * @param context GraalVM JS bối cảnh
     */
    public static void injectRequireFunction(Context context) {
        // Tạo require() Thực hiện chức năng
        String requireFunction = """
                if (!globalThis.__epRequireCache) {
                    globalThis.__epRequireCache = Object.create(null);
                }

                globalThis.__devWorkbenchRequire = function(moduleName, parentModuleId) {
                    var loadedModule;
                    try {
                        loadedModule = __loadLibrary(moduleName, parentModuleId || '');
                    } catch (e) {
                        throw new Error(e.message || ('Cannot find module: ' + moduleName));
                    }
                    if (!loadedModule || !loadedModule.id || !loadedModule.code) {
                        throw new Error('Cannot find module: ' + moduleName);
                    }

                    var resolvedId = loadedModule.id;
                    if (Object.prototype.hasOwnProperty.call(globalThis.__epRequireCache, resolvedId)) {
                        return globalThis.__epRequireCache[resolvedId];
                    }

                    var module = { exports: {}, id: resolvedId };
                    globalThis.__epRequireCache[resolvedId] = module.exports;
                    var localRequire = function(childModuleName) {
                        return globalThis.__devWorkbenchRequire(childModuleName, resolvedId);
                    };

                    try {
                        var moduleFactory = new Function('module', 'exports', 'require', loadedModule.code);
                        moduleFactory(module, module.exports, localRequire);
                    } catch (e) {
                        delete globalThis.__epRequireCache[resolvedId];
                        throw new Error('Failed to load module ' + moduleName + ': ' + e.message);
                    }

                    globalThis.__epRequireCache[resolvedId] = module.exports;
                    return module.exports;
                };

                globalThis.require = function(moduleName) {
                    return globalThis.__devWorkbenchRequire(moduleName, '');
                };
                """;

        // tiêm __loadLibrary Chức năng phụ trợ (Java kết thúc thực hiện)
        ProxyExecutable loadLibraryFunc = args -> {
            String moduleName = args[0].asString();
            String parentModuleId = args.length > 1 ? args[1].asString() : null;
            try {
                LoadedLibrary library = loadLibrary(moduleName, parentModuleId);
                return library.toJsObject();
            } catch (Exception e) {
                log.debug("Failed to load library {}: {}", moduleName, e.getMessage());
                throw new IllegalStateException(e.getMessage(), e);
            }
        };
        context.getBindings("js").putMember("__loadLibrary", loadLibraryFunc);

        // tiêm require() chức năng
        context.eval("js", requireFunction);
        log.debug("Injected require() function");
    }

    /**
     * trọng tải JS Thư viện (tích hợp hoặc bên ngoài)
     *
     * @param moduleName Tên thư viện, đường dẫn tệp hoặc  URL
     * @param parentModuleId Mã định danh mô-đun chính, được sử dụng để giải quyết các đường dẫn tương đối
     * @return thư viện JavaScript Mã và thông số kỹ thuật ID
     */
    private static LoadedLibrary loadLibrary(String moduleName, String parentModuleId) throws IOException {
        String builtInId = resolveBuiltinLibraryId(moduleName);
        if (builtInId != null) {
            return new LoadedLibrary("builtin:" + builtInId, loadBuiltinLibrary(builtInId));
        }

        if (isRemoteModule(moduleName, parentModuleId)) {
            String normalizedUrl = resolveRemoteUrl(moduleName, parentModuleId);
            return loadFromUrl(normalizedUrl);
        }

        Path filePath = resolveFilePath(moduleName, parentModuleId);
        return loadFromFile(filePath);
    }

    /**
     * Tải các thư viện tích hợp
     *
     * @param libraryName Tên thư viện
     * @return thư viện JavaScript mã
     */
    private static String loadBuiltinLibrary(String libraryName) throws IOException {
        // Kiểm tra bộ đệm
        String cached = BUILTIN_LIBRARY_CACHE.get(libraryName);
        if (cached != null) {
            return cached;
        }

        String resourcePath = BUILTIN_LIBRARY_PATHS.get(libraryName);
        if (resourcePath == null) {
            throw new IOException("Unknown built-in library: " + libraryName);
        }

        // Từ resources trọng tải
        try (InputStream is = JsLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Built-in library not found: " + resourcePath);
            }
            String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            BUILTIN_LIBRARY_CACHE.put(libraryName, code);
            log.debug("Loaded built-in library from resources: {}", resourcePath);
            return code;
        }
    }

    /**
     * Tải từ đường dẫn tập tin JS thư viện
     *
     * @param filePath Đường dẫn tập tin
     * @return Nội dung tập tin
     */
    private static LoadedLibrary loadFromFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }
        String code = Files.readString(path, StandardCharsets.UTF_8);
        String normalizedPath = path.toAbsolutePath().normalize().toString();
        log.debug("Loaded library from file: {}", normalizedPath);
        return new LoadedLibrary(normalizedPath, code);
    }

    /**
     * Từ URL trọng tải JS thư viện
     *
     * @param urlString URL địa chỉ
     * @return URL nội dung
     */
    private static LoadedLibrary loadFromUrl(String urlString) throws IOException {
        validateRemoteUrl(urlString);
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(SettingManager.getRemoteJsRequireConnectTimeoutMs());
        connection.setReadTimeout(SettingManager.getRemoteJsRequireReadTimeoutMs());
        try (InputStream is = connection.getInputStream()) {
            String code = readUtf8WithLimit(is, SettingManager.getRemoteJsRequireMaxBytes());
            log.debug("Loaded library from URL: {}", urlString);
            return new LoadedLibrary(url.toExternalForm(), code);
        }
    }

    /**
     * Xóa bộ đệm thư viện (để kiểm tra)
     */
    public static void clearCache() {
        BUILTIN_LIBRARY_CACHE.clear();
        log.debug("Library cache cleared");
    }

    static boolean isBuiltinLibraryCachedForTests(String libraryName) {
        return BUILTIN_LIBRARY_CACHE.containsKey(libraryName);
    }

    /**
     * Lấy danh sách các thư viện tích hợp đã đăng ký
     *
     * @return Danh sách tên thư viện tích hợp
     */
    public static String[] getBuiltinLibraries() {
        return BUILTIN_LIBRARY_PATHS.keySet().toArray(new String[0]);
    }

    private static void registerBuiltInLibrary(String canonicalId, String resourcePath, String... aliases) {
        BUILTIN_LIBRARY_PATHS.put(canonicalId, resourcePath);
        Arrays.stream(aliases).forEach(alias -> BUILTIN_LIBRARY_IDS.put(alias, canonicalId));
    }

    private static String resolveBuiltinLibraryId(String moduleName) {
        String direct = BUILTIN_LIBRARY_IDS.get(moduleName);
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, String> entry : BUILTIN_LIBRARY_IDS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(moduleName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean isRemoteModule(String moduleName, String parentModuleId) {
        return moduleName.startsWith("http://")
                || moduleName.startsWith("https://")
                || (isRemoteModuleId(parentModuleId) && isRelativePath(moduleName));
    }

    private static boolean isRemoteModuleId(String moduleId) {
        return moduleId != null && (moduleId.startsWith("http://") || moduleId.startsWith("https://"));
    }

    private static boolean isRelativePath(String moduleName) {
        return moduleName.startsWith("./") || moduleName.startsWith("../");
    }

    private static String resolveRemoteUrl(String moduleName, String parentModuleId) throws IOException {
        if (moduleName.startsWith("http://") || moduleName.startsWith("https://")) {
            return new URL(moduleName).toExternalForm();
        }
        if (!isRemoteModuleId(parentModuleId)) {
            throw new IOException("Cannot resolve remote module: " + moduleName);
        }
        return new URL(new URL(parentModuleId), moduleName).toExternalForm();
    }

    private static Path resolveFilePath(String moduleName, String parentModuleId) throws IOException {
        if (moduleName.startsWith("/")) {
            return Paths.get(moduleName).toAbsolutePath().normalize();
        }

        if (!isRelativePath(moduleName)) {
            throw new IOException("Cannot find module: " + moduleName);
        }

        Path baseDirectory = resolveBaseDirectory(parentModuleId);
        return baseDirectory.resolve(moduleName).normalize().toAbsolutePath();
    }

    private static Path resolveBaseDirectory(String parentModuleId) {
        if (parentModuleId != null && !parentModuleId.isBlank() && !isRemoteModuleId(parentModuleId)
                && !parentModuleId.startsWith("builtin:")) {
            Path parentPath = Paths.get(parentModuleId);
            Path parentDirectory = Files.isDirectory(parentPath) ? parentPath : parentPath.getParent();
            if (parentDirectory != null) {
                return parentDirectory;
            }
        }

        try {
            Workspace currentWorkspace = WorkspaceService.getInstance().getCurrentWorkspace();
            if (currentWorkspace != null && currentWorkspace.getPath() != null && !currentWorkspace.getPath().isBlank()) {
                return Paths.get(currentWorkspace.getPath()).toAbsolutePath().normalize();
            }
        } catch (Exception e) {
            log.debug("Falling back to default directory for local JS require(): {}", e.getMessage());
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            return Paths.get(userDir).toAbsolutePath().normalize();
        }

        return Paths.get(SystemUtil.getDevWorkbenchPath()).toAbsolutePath().normalize();
    }

    private static void validateRemoteUrl(String urlString) throws IOException {
        if (!SettingManager.isRemoteJsRequireEnabled()) {
            throw new IOException("Remote JavaScript require() is disabled. Enable it in Settings > Request > Script Loading.");
        }

        URL url = new URL(urlString);
        String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
        boolean allowInsecureRemote = SettingManager.isInsecureRemoteJsRequireEnabled();
        if (!"https".equals(protocol) && !(allowInsecureRemote && "http".equals(protocol))) {
            throw new IOException("Only HTTPS remote modules are allowed by default: " + urlString);
        }

        Set<String> allowedHosts = getAllowedRemoteHosts();
        if (!allowedHosts.isEmpty() && !allowedHosts.contains(url.getHost().toLowerCase(Locale.ROOT))) {
            throw new IOException("Remote host is not in the allowlist: " + url.getHost());
        }
    }

    private static Set<String> getAllowedRemoteHosts() {
        String configuredHosts = SettingManager.getRemoteJsRequireAllowedHosts();
        if (configuredHosts.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> hosts = ConcurrentHashMap.newKeySet();
        Arrays.stream(configuredHosts.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .map(host -> host.toLowerCase(Locale.ROOT))
                .forEach(hosts::add);
        return hosts;
    }

    private static String readUtf8WithLimit(InputStream inputStream, int maxBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            totalRead += bytesRead;
            if (totalRead > maxBytes) {
                throw new IOException("Remote JavaScript module exceeds max size: " + maxBytes + " bytes");
            }
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private static String escapeForJs(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("'", "\\'");
    }

    private record LoadedLibrary(String id, String code) {
        private Map<String, String> toJsObject() {
            Map<String, String> result = new HashMap<>();
            result.put("id", id);
            result.put("code", code);
            return result;
        }
    }
}
