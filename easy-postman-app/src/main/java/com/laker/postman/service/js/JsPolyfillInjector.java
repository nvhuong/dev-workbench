package com.laker.postman.service.js;

import com.laker.postman.request.util.HttpUrlUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * JSkịch bảnPolyfillĐầu phun
 * Cung cấp tính năng chèn chức năng chung (mã hóa, mã hóa, chức năng tiện ích, v.v.)
 * <p>
 * Các chức năng hỗ trợ:
 * - Base64Codec: btoa, atob
 * - Hàm băm: MD5, SHA1, SHA256, SHA512
 * - Chức năng công cụ: uuid, timestamp
 */
@Slf4j
@UtilityClass
public class JsPolyfillInjector {

    /**
     * Tiêm tất cả thông thường polyfill
     */
    public static void injectAll(Context context) {
        log.debug("Injecting JavaScript polyfills...");

        // Base64Codec
        injectBase64Functions(context);

        // Hàm băm
        injectHashFunctions(context);

        // Chức năng công cụ
        injectUtilityFunctions(context);

        // URL Chức năng mã hóa và giải mã
        injectUrlEncodingFunctions(context);

        // bên ngoài JS Hỗ trợ thư viện
        JsLibraryLoader.injectBuiltinLibraries(context);

        log.debug("Polyfills injected successfully");
    }

    /**
     * tiêmBase64Các chức năng liên quan
     */
    private static void injectBase64Functions(Context context) {
        // btoa - Chuyển đổi chuỗiBase64
        injectFunction(context, "btoa", args -> {
            String str = args[0].asString();
            return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
        });

        // atob - Base64Chuyển đổi thành chuỗi
        injectFunction(context, "atob", args -> {
            String str = args[0].asString();
            byte[] decoded = Base64.getDecoder().decode(str);
            return new String(decoded, StandardCharsets.UTF_8);
        });
    }

    /**
     * Tiêm hàm băm
     */
    private static void injectHashFunctions(Context context) {
        // MD5
        injectFunction(context, "MD5", args -> {
            String str = args[0].asString();
            return DigestUtils.md5Hex(str);
        });

        // SHA1
        injectFunction(context, "SHA1", args -> {
            String str = args[0].asString();
            return DigestUtils.sha1Hex(str);
        });

        // SHA256
        injectFunction(context, "SHA256", args -> {
            String str = args[0].asString();
            return DigestUtils.sha256Hex(str);
        });

        // SHA512
        injectFunction(context, "SHA512", args -> {
            String str = args[0].asString();
            return DigestUtils.sha512Hex(str);
        });
    }

    /**
     * Tiêm các chức năng tiện ích
     */
    private static void injectUtilityFunctions(Context context) {
        // uuid - phát raUUID
        injectFunction(context, "uuid", args -> UUID.randomUUID().toString());

        // timestamp - Lấy dấu thời gian hiện tại (mili giây)
        injectFunction(context, "timestamp", args -> System.currentTimeMillis());
    }

    /**
     * tiêm URL Chức năng mã hóa và giải mã
     */
    private static void injectUrlEncodingFunctions(Context context) {
        if (hasGlobalFunction(context, "encodeURIComponent")
                && hasGlobalFunction(context, "decodeURIComponent")
                && hasGlobalFunction(context, "encodeURI")
                && hasGlobalFunction(context, "decodeURI")) {
            log.debug("Using native JavaScript URL encoding functions");
            return;
        }

        // encodeURIComponent - URL Mã hóa
        injectFunction(context, "encodeURIComponent", args -> {
            String str = args[0].asString();
            return java.net.URLEncoder.encode(str, StandardCharsets.UTF_8)
                    .replace("+", "%20")  // Dấu cách được mã hóa dưới dạng  %20 Đúng hơn +
                    .replace("%21", "!")
                    .replace("%27", "'")
                    .replace("%28", "(")
                    .replace("%29", ")")
                    .replace("%7E", "~");
        });

        // decodeURIComponent - URL giải mã
        injectFunction(context, "decodeURIComponent", args -> {
            String str = args[0].asString();
            return HttpUrlUtil.decodeComponent(str);
        });

        // encodeURI - URL Mã hóa (ký tự đặc biệt dành riêng)
        injectFunction(context, "encodeURI", args -> {
            String str = args[0].asString();
            return java.net.URLEncoder.encode(str, StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("%3A", ":")
                    .replace("%2F", "/")
                    .replace("%3F", "?")
                    .replace("%23", "#")
                    .replace("%5B", "[")
                    .replace("%5D", "]")
                    .replace("%40", "@")
                    .replace("%21", "!")
                    .replace("%24", "$")
                    .replace("%26", "&")
                    .replace("%27", "'")
                    .replace("%28", "(")
                    .replace("%29", ")")
                    .replace("%2A", "*")
                    .replace("%2B", "+")
                    .replace("%2C", ",")
                    .replace("%3B", ";")
                    .replace("%3D", "=")
                    .replace("%7E", "~");
        });

        // decodeURI - URL giải mã
        injectFunction(context, "decodeURI", args -> {
            String str = args[0].asString();
            return decodeUriFallback(str);
        });
    }

    private static boolean hasGlobalFunction(Context context, String name) {
        try {
            Value result = context.eval("js", "typeof globalThis['" + escapeForJs(name) + "'] === 'function'");
            return result != null && result.asBoolean();
        } catch (Exception e) {
            log.debug("Failed to inspect global function {}: {}", name, e.getMessage());
            return false;
        }
    }

    private static String escapeForJs(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static String decodeUriFallback(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return HttpUrlUtil.decodeComponent(value)
                .replace("#", "%23")
                .replace("&", "%26")
                .replace("+", "%2B")
                .replace(",", "%2C")
                .replace("/", "%2F")
                .replace(":", "%3A")
                .replace(";", "%3B")
                .replace("=", "%3D")
                .replace("?", "%3F")
                .replace("@", "%40")
                .replace("$", "%24");
    }

    /**
     * Tiêm một chức năng duy nhất
     */
    private static void injectFunction(Context context, String name, ProxyExecutable function) {
        try {
            context.getBindings("js").putMember(name, function);
            log.trace("Injected function: {}", name);
        } catch (Exception e) {
            log.warn("Failed to inject function: {}, error: {}", name, e.getMessage());
        }
    }
}
