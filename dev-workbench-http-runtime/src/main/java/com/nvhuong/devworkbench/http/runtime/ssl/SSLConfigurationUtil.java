package com.nvhuong.devworkbench.http.runtime.ssl;

import com.nvhuong.devworkbench.certificate.TrustedCertificateEntry;
import com.nvhuong.devworkbench.http.runtime.config.HttpRuntimeSettingsProvider;
import com.nvhuong.devworkbench.http.runtime.observation.HttpLifecycleLogSink;
import com.nvhuong.devworkbench.request.util.HttpUrlUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SSLLớp công cụ cấu hình
 * Trách nhiệm: choOkHttpClientcấu hìnhSSLcài đặt, hỗ trợ chế độ nghiêm ngặt và chế độ thoải mái, cũng như chứng chỉ ứng dụng khách (mTLS）
 */
@Slf4j
public class SSLConfigurationUtil {

    /**
     * SSLChế độ xác thực
     */
    public enum SSLVerificationMode {
        /**
         * Chế độ nghiêm ngặt: Xác thực đầy đủ chứng chỉ
         */
        STRICT,
        /**
         * Chế độ thư giãn: xác thực nhưng không chặn kết nối
         */
        LENIENT,
        /**
         * Tin cậy tất cả: Bỏ qua hoàn toàn việc xác minh (không khuyến nghị)
         */
        TRUST_ALL
    }

    // Lưu trữ gần đây nhấtSSLKết quả xác minh, được phân biệt theo chủ đề
    private static final ThreadLocal<SSLValidationResult> lastValidationResult = new ThreadLocal<>();
    private static volatile HttpLifecycleLogSink lifecycleLogSink = HttpLifecycleLogSink.noop();
    private static volatile ClientCertificateProvider clientCertificateProvider = ClientCertificateProvider.noop();

    public static void setLifecycleLogSink(HttpLifecycleLogSink sink) {
        lifecycleLogSink = sink == null ? HttpLifecycleLogSink.noop() : sink;
    }

    public static void setClientCertificateProvider(ClientCertificateProvider provider) {
        clientCertificateProvider = provider == null ? ClientCertificateProvider.noop() : provider;
    }

    /**
     * Lấy chủ đề hiện tạiSSLXác minh kết quả
     */
    public static SSLValidationResult getLastValidationResult() {
        return lastValidationResult.get();
    }

    /**
     * Xóa chủ đề hiện tạiSSLXác minh kết quả
     */
    public static void clearValidationResult() {
        lastValidationResult.remove();
    }

    /**
     * vìOkHttpClientcấu hìnhSSLcài đặt (phương pháp này được khuyến khích)
     *
     * @param builder OkHttpClientngười xây dựng
     * @param mode    SSLChế độ xác thực
     */
    public static void configureSSL(OkHttpClient.Builder builder, SSLVerificationMode mode) {
        configureSSL(builder, mode, null, 0);
    }

    /**
     * vìOkHttpClientcấu hìnhSSLCài đặt để hỗ trợ chứng chỉ ứng dụng khách (mTLS）
     *
     * @param builder OkHttpClientngười xây dựng
     * @param mode    SSLChế độ xác thực
     * @param host    Tên máy chủ đích (được sử dụng để khớp với chứng chỉ ứng dụng khách)
     * @param port    Cổng đích (để khớp với chứng chỉ ứng dụng khách)
     */
    public static void configureSSL(OkHttpClient.Builder builder, SSLVerificationMode mode,
                                    String host, int port) {
        configureSSL(builder, mode, host, port, true);
    }

    public static void configureSSL(OkHttpClient.Builder builder,
                                    SSLVerificationMode mode,
                                    String host,
                                    int port,
                                    boolean consoleLoggingEnabled) {
        configureSSL(builder, mode, host, port,
                consoleLoggingEnabled ? lifecycleLogSink : HttpLifecycleLogSink.noop());
    }

    public static void configureSSL(OkHttpClient.Builder builder,
                                    SSLVerificationMode mode,
                                    String host,
                                    int port,
                                    HttpLifecycleLogSink logSink) {
        try {
            host = HttpUrlUtil.normalizeHost(host);
            // Tìm và tải chứng chỉ ứng dụng khách phù hợp
            KeyManager[] keyManagers = loadClientCertificate(host, port, logSink);

            // cấu hình TrustManager
            X509TrustManager trustManager = configureTrustManager(mode, keyManagers);

            applyHostnameVerifier(builder, mode);

            // Chế độ nghiêm ngặt và không có chứng chỉ ứng dụng khách: Sử dụng cấu hình mặc định
            if (trustManager == null) {
                return;
            }

            // khởi tạo SSL bối cảnh
            SSLContext sslContext = SSLContext.getInstance("TLS");
            // Khi nào keyManagers Đã vượt qua khi nó là một mảng trống null Phù hợp hơn
            KeyManager[] kms = (keyManagers != null && keyManagers.length > 0) ? keyManagers : null;
            sslContext.init(kms, new TrustManager[]{trustManager}, new java.security.SecureRandom());

            // Sử dụng tùy chỉnh SSLSocketFactory để nắm bắt thông tin chứng chỉ
            CertificateCapturingSSLSocketFactory socketFactory =
                    new CertificateCapturingSSLSocketFactory(sslContext);

            builder.sslSocketFactory(socketFactory, trustManager);

            if (mode != SSLVerificationMode.STRICT) {
                log.warn("SSL verification mode set to: {}. Use with caution in production.", mode);
            }

        } catch (Exception e) {
            log.error("Failed to configure SSL settings", e);
            throw new SSLException("Failed to configure SSL settings", e,
                    SSLException.SSLErrorType.CONFIGURATION_ERROR);
        }
    }

    /**
     * Tải chứng chỉ ứng dụng khách
     */
    private static KeyManager[] loadClientCertificate(String host, int port, HttpLifecycleLogSink logSink) {
        if (host == null || host.isEmpty()) {
            return new KeyManager[0];
        }
        try {
            KeyManager[] keyManagers = clientCertificateProvider.loadKeyManagers(host, port);
            return keyManagers == null ? new KeyManager[0] : keyManagers;
        } catch (Exception e) {
            log.error("Failed to load client certificate for host: {}", host, e);
            String message = MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.CERT_CONSOLE_LOAD_FAILED),
                    host, e.getMessage()
            );
            appendClientCertificateLoadFailure(message, logSink);

            return new KeyManager[0];
        }
    }

    private static void appendClientCertificateLoadFailure(String message, HttpLifecycleLogSink logSink) {
        HttpLifecycleLogSink resolvedSink = logSink == null ? HttpLifecycleLogSink.noop() : logSink;
        try {
            resolvedSink.append(message, HttpLifecycleLogSink.Level.ERROR);
        } catch (RuntimeException ignored) {
            // SSL setup must not fail because lifecycle logging is unavailable.
        }
    }

    public static String clientCertificateCacheKey(String host, int port) {
        host = HttpUrlUtil.normalizeHost(host);
        if (host == null || host.isBlank()) {
            return "clientCert:none";
        }
        try {
            String cacheKey = clientCertificateProvider.cacheKey(host, port);
            return cacheKey == null || cacheKey.isBlank() ? "clientCert:none" : cacheKey;
        } catch (RuntimeException e) {
            log.debug("Failed to inspect client certificate cache key for {}:{}", host, port, e);
        }
        return "clientCert:none";
    }

    /**
     * cấu hình TrustManager
     *
     * @return TrustManager hoặc null（có nghĩa là sử dụng cấu hình mặc định)
     */
    private static X509TrustManager configureTrustManager(SSLVerificationMode mode, KeyManager[] keyManagers)
            throws SSLConfigurationException {
        boolean hasCustomTrustMaterial = hasCustomTrustMaterial();
        if (mode == SSLVerificationMode.STRICT) {
            // Chế độ nghiêm ngặt: Không có chứng chỉ ứng dụng khách và không có tùy chỉnh trust material Cấu hình mặc định của nền tảng được sử dụng trực tiếp
            if ((keyManagers == null || keyManagers.length == 0) && !hasCustomTrustMaterial) {
                return null;
            }
            return buildEffectiveTrustManager(true);
        }

        // Chế độ thư giãn hoặc tin tưởng tất cả chế độ
        return createTrustManager(mode);
    }

    /**
     * TạoTrustManager
     */
    private static X509TrustManager createTrustManager(SSLVerificationMode mode) {
        try {
            if (mode == SSLVerificationMode.TRUST_ALL) {
                return new TrustAllManager();
            } else {
                return new LenientTrustManager(buildEffectiveTrustManager(false));
            }
        } catch (Exception e) {
            log.error("Failed to create TrustManager", e);
            throw new SSLException("Failed to create TrustManager", e,
                    SSLException.SSLErrorType.CONFIGURATION_ERROR);
        }
    }

    /**
     * Lấy mặc định của hệ thốngTrustManager
     */
    private static X509TrustManager getDefaultTrustManager() throws SSLConfigurationException {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            factory.init((java.security.KeyStore) null);

            for (TrustManager tm : factory.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509tm) {
                    return x509tm;
                }
            }
            throw new SSLConfigurationException("No X509TrustManager found");
        } catch (Exception e) {
            throw new SSLConfigurationException("Failed to get default TrustManager", e);
        }
    }

    private static boolean hasCustomTrustMaterial() {
        if (!HttpRuntimeSettingsProvider.get().isCustomTrustMaterialEnabled()) {
            return false;
        }
        for (TrustedCertificateEntry entry : HttpRuntimeSettingsProvider.get().getCustomTrustMaterialEntries()) {
            if (entry.isEnabled() && entry.hasUsablePath()) {
                return true;
            }
        }
        return false;
    }

    private static X509TrustManager buildEffectiveTrustManager(boolean failOnCustomTrustLoad)
            throws SSLConfigurationException {
        X509TrustManager defaultTrustManager = getDefaultTrustManager();
        X509TrustManager customTrustManager = loadCustomTrustManager(failOnCustomTrustLoad);
        if (customTrustManager == null) {
            return defaultTrustManager;
        }
        return createMergedTrustManager(defaultTrustManager, customTrustManager);
    }

    private static X509TrustManager loadCustomTrustManager(boolean failOnError)
            throws SSLConfigurationException {
        if (!hasCustomTrustMaterial()) {
            return null;
        }
        List<TrustedCertificateEntry> entries = HttpRuntimeSettingsProvider.get().getCustomTrustMaterialEntries();

        try {
            X509TrustManager trustManager = CustomTrustMaterialLoader.loadTrustManager(entries);
            log.info("Loaded {} custom trust material entries", entries.size());
            return trustManager;
        } catch (Exception e) {
            if (failOnError) {
                log.warn("Failed to load custom trust material in strict mode, falling back to system defaults", e);
                return null;
            }
            log.warn("Failed to load custom trust material in lenient mode, continuing without it", e);
            return null;
        }
    }

    static X509TrustManager createMergedTrustManager(X509TrustManager... trustManagers)
            throws SSLConfigurationException {
        try {
            KeyStore mergedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            mergedKeyStore.load(null, null);

            Set<String> seenCertificates = new LinkedHashSet<>();
            int aliasIndex = 0;
            X509TrustManager fallback = null;

            if (trustManagers != null) {
                for (X509TrustManager trustManager : trustManagers) {
                    if (trustManager == null) {
                        continue;
                    }
                    if (fallback == null) {
                        fallback = trustManager;
                    }
                    for (X509Certificate issuer : trustManager.getAcceptedIssuers()) {
                        String fingerprint = java.util.Base64.getEncoder().encodeToString(issuer.getEncoded());
                        if (seenCertificates.add(fingerprint)) {
                            mergedKeyStore.setCertificateEntry("merged-ca-" + aliasIndex++, issuer);
                        }
                    }
                }
            }

            if (aliasIndex == 0) {
                if (fallback != null) {
                    return fallback;
                }
                throw new SSLConfigurationException("No trust managers available");
            }

            TrustManagerFactory factory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            factory.init(mergedKeyStore);

            for (TrustManager tm : factory.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509TrustManager) {
                    return x509TrustManager;
                }
            }
            throw new SSLConfigurationException("No X509TrustManager found for merged trust material");
        } catch (SSLConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new SSLConfigurationException("Failed to merge trust managers", e);
        }
    }

    /**
     * TạoHostnameVerifier
     */
    private static HostnameVerifier createHostnameVerifier(SSLVerificationMode mode) {
        if (mode == SSLVerificationMode.TRUST_ALL) {
            return (hostname, session) -> true;
        } else if (mode == SSLVerificationMode.LENIENT) {
            return new LenientHostnameVerifier();
        }
        // Keep OkHttp's built-in strict verifier for TLS sessions created by OkHttp itself.
        return new OkHttpClient().hostnameVerifier();
    }

    private static void applyHostnameVerifier(OkHttpClient.Builder builder, SSLVerificationMode mode) {
        if (builder == null) {
            return;
        }
        builder.hostnameVerifier(createHostnameVerifier(mode));
    }

    /**
     * Lỏng lẻoTrustManager：Xác thực trước, ghi lại lỗi nhưng cho phép kết nối tiếp tục
     */
    private static class LenientTrustManager implements X509TrustManager {
        private final X509TrustManager defaultTrustManager;

        public LenientTrustManager(X509TrustManager defaultTrustManager) {
            this.defaultTrustManager = defaultTrustManager;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            log.debug("Client certificate check (lenient mode)");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            if (chain == null || chain.length == 0) {
                log.warn("No certificate chain provided");
                return;
            }

            // JSSE dispatches HandshakeCompletedListener on another thread. Capture here on
            // the handshake thread so the following EventListener can diagnose untrusted peers.
            CertificateCapturingSSLSocketFactory.rememberCapturedCertificates(java.util.Arrays.asList(chain));

            log.debug("Validating certificate chain with {} certificates", chain.length);
            X509Certificate serverCert = chain[0];
            log.debug("Server certificate: Subject={}, Issuer={}",
                    serverCert.getSubjectX500Principal().getName(),
                    serverCert.getIssuerX500Principal().getName());

            // Hãy thử sử dụng mặc địnhTrustManagerNhận xác minh thực sự
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
                lastValidationResult.remove(); // Xác minh đã được thông qua và lỗi đã được xóa
                log.debug("✅ Certificate validation passed");
            } catch (CertificateException e) {
                // Nếu xác thực không thành công, một ngoại lệ sẽ được ghi lại nhưng không được ném ra
                String errorMessage = CertificateErrorParser.extractUserFriendlyMessage(e);

                log.warn("⚠️ Certificate validation failed (allowing anyway): {}", errorMessage);

                // Lưu kết quả xác minh để sử dụng sau
                lastValidationResult.set(SSLValidationResult.failure(
                        java.util.Collections.singletonList(errorMessage)));
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return defaultTrustManager != null ?
                    defaultTrustManager.getAcceptedIssuers() :
                    new X509Certificate[0];
        }
    }

    /**
     * Tin tưởng tất cả các chứng chỉTrustManager（Không được khuyến khích)
     */
    private static class TrustAllManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            log.debug("Client certificate trusted (trust-all mode)");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            if (chain != null && chain.length > 0) {
                log.debug("Server certificate trusted (trust-all mode): {}",
                        chain[0].getSubjectX500Principal().getName());
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * Lỏng lẻoHostnameVerifier：Xác minh tên máy chủ nhưng cho phép kết nối không khớp
     */
    private static class LenientHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            try {
                java.security.cert.Certificate[] peerCertificates = session.getPeerCertificates();
                if (peerCertificates != null && peerCertificates.length > 0) {
                    java.security.cert.Certificate cert = peerCertificates[0];
                    if (cert instanceof X509Certificate x509) {
                        boolean valid = SSLCertificateValidator.isHostnameMatchPublic(x509, hostname);

                        if (!valid) {
                            log.warn("⚠️ Hostname verification failed (allowing anyway): " +
                                    "Certificate does not match hostname '{}'", hostname);

                            // Thêm lỗi tên máy chủ vào kết quả xác minh hiện có
                            SSLValidationResult existing = lastValidationResult.get();
                            String hostnameError = "Hostname mismatch: certificate does not match '" + hostname + "'";

                            if (existing != null && existing.hasErrors()) {
                                java.util.List<String> errors = new java.util.ArrayList<>(existing.getErrors());
                                errors.add(hostnameError);
                                lastValidationResult.set(SSLValidationResult.failure(errors));
                            } else {
                                lastValidationResult.set(SSLValidationResult.failure(
                                        java.util.Collections.singletonList(hostnameError)));
                            }
                        } else {
                            log.debug("✅ Hostname verification passed for: {}", hostname);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Error during hostname verification: {}", e.getMessage());
            }

            // luôn quay trở lại true，Cho phép kết nối tiếp tục
            return true;
        }
    }

    /**
     * SSLNgoại lệ cấu hình
     */
    private static class SSLConfigurationException extends RuntimeException {
        public SSLConfigurationException(String message) {
            super(message);
        }

        public SSLConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
