package com.laker.postman.http.runtime.okhttp;

import com.laker.postman.certificate.TrustedCertificateEntry;
import com.laker.postman.http.runtime.config.HttpRuntimeSettings;
import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import com.laker.postman.http.runtime.observation.SafeSocketAddressFormatter;
import com.laker.postman.http.runtime.ssl.SSLConfigurationUtil;
import com.laker.postman.request.model.HttpRequestProxyPolicy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.Authenticator;

import java.io.File;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.laker.postman.request.util.HttpUrlUtil.extractBaseUri;

/**
 * OkHttpClient Trình quản lý, nhóm kết nối dùng chung và mỗi cấu hình truyền tải OkHttpClient。
 * OkHttp Nhóm kết nối sẽ nhấn Address Cô lập các mục tiêu khác nhau.Client Không cần phải nhấn lại host Tạo nhiều lần;
 * mTLS、Đại lý và SSL Các mẫu vẫn vượt qua bộ đệm key sự cách ly.
 */
@Slf4j
public class OkHttpClientManager {
    // Cấu hình vận chuyển tương tự được chia sẻ Client，Tránh tích lũy không giới hạn sau khi truy cập một số lượng lớn tên miền Dispatcher/ConnectionPool。
    private static final Map<String, OkHttpClient> clientMap = new ConcurrentHashMap<>();
    // Thông số nhóm kết nối
    private static final int MAX_IDLE_CONNECTIONS = 6;
    private static final long KEEP_ALIVE_DURATION = 90L;

    // Dispatcher Các tham số đồng thời (OkHttpMặc định: .maxRequests=64, maxRequestsPerHost=5）
    private static final int DEFAULT_MAX_REQUESTS = 64;
    private static final int DEFAULT_MAX_REQUESTS_PER_HOST = 5;

    private static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";
    // OkHttp sends a synthetic challenge with this scheme before opening an HTTPS tunnel.
    private static final String PREEMPTIVE_CHALLENGE_SCHEME = "OkHttp-Preemptive";

    // Bức tranh lớn CookieManager，Hỗ trợ tiêu chuẩn CookiePolicy
    private static final CookieManager GLOBAL_COOKIE_MANAGER = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private static final JavaNetCookieJar GLOBAL_COOKIE_JAR = new JavaNetCookieJar(GLOBAL_COOKIE_MANAGER);

    public record ProxyInspection(boolean resolved, Proxy proxy) {
        public boolean active() {
            return resolved && !isDirectProxy(proxy);
        }

        public String description() {
            return OkHttpClientManager.describeProxyForDisplay(proxy);
        }
    }

    /**
     * Xóa tất cả bộ nhớ đệm của máy khách để buộc tạo lại máy khách sau khi thay đổi cài đặt proxy
     */
    public static void clearClientCache() {
        for (OkHttpClient client : clientMap.values()) {
            shutdownClient(client);
        }
        clientMap.clear();
        SocksProxyAuthenticatorSupport.clearAllowedEndpoints();
    }

    public static boolean bypassesJvmSocketProxySelector(OkHttpClient client) {
        return client != null && client.socketFactory() == JvmProxyBypassingSocketFactory.INSTANCE;
    }

    private static void shutdownClient(OkHttpClient client) {
        client.dispatcher().cancelAll();
        client.dispatcher().executorService().shutdownNow();
        client.connectionPool().evictAll();
    }

    /**
     * Nhận hoặc tạo quy định baseUri của OkHttpClient Ví dụ
     */
    public static OkHttpClient getClient(String baseUri, boolean followRedirects) {
        return getClient(baseUri, followRedirects, HttpRequestProxyPolicy.DEFAULT);
    }

    public static OkHttpClient getClient(String baseUri, boolean followRedirects, HttpRequestProxyPolicy proxyPolicy) {
        HttpRequestProxyPolicy resolvedProxyPolicy = HttpRequestProxyPolicy.normalize(proxyPolicy);
        return getOrCreateClient(
                baseUri,
                followRedirects,
                resolveSslVerificationMode(baseUri, resolvedProxyPolicy),
                resolvedProxyPolicy
        );
    }

    /**
     * @deprecated use {@link #getClientForSslMode(String, boolean, SSLConfigurationUtil.SSLVerificationMode, HttpRequestProxyPolicy)}.
     */
    @Deprecated
    public static OkHttpClient createClientForSslMode(String baseUri,
                                                      boolean followRedirects,
                                                      SSLConfigurationUtil.SSLVerificationMode sslMode,
                                                      HttpRequestProxyPolicy proxyPolicy) {
        return getClientForSslMode(baseUri, followRedirects, sslMode, proxyPolicy);
    }

    public static OkHttpClient getClientForSslMode(String baseUri,
                                                   boolean followRedirects,
                                                   SSLConfigurationUtil.SSLVerificationMode sslMode,
                                                   HttpRequestProxyPolicy proxyPolicy) {
        HttpRequestProxyPolicy resolvedProxyPolicy = HttpRequestProxyPolicy.normalize(proxyPolicy);
        OkHttpClient baseClient = getClient(baseUri, followRedirects, resolvedProxyPolicy);
        String key = buildClientProfileKey(baseUri, followRedirects, sslMode, resolvedProxyPolicy);
        return clientMap.computeIfAbsent(key,
                ignored -> createClient(
                        baseUri,
                        followRedirects,
                        sslMode,
                        baseClient.dispatcher(),
                        resolvedProxyPolicy
                ));
    }

    private static OkHttpClient getOrCreateClient(String baseUri,
                                                  boolean followRedirects,
                                                  SSLConfigurationUtil.SSLVerificationMode sslMode,
                                                  HttpRequestProxyPolicy proxyPolicy) {
        String key = buildClientProfileKey(baseUri, followRedirects, sslMode, proxyPolicy);
        return clientMap.computeIfAbsent(key,
                ignored -> createClient(baseUri, followRedirects, sslMode, proxyPolicy));
    }

    private static String buildClientProfileKey(String baseUri,
                                                boolean followRedirects,
                                                SSLConfigurationUtil.SSLVerificationMode sslMode,
                                                HttpRequestProxyPolicy proxyPolicy) {
        URI uri = tryParseUri(baseUri, "HTTP client cache key");
        String transport = uri != null && isSecureScheme(uri.getScheme()) ? "secure" : "plain";
        return transport
                + "|redirects:" + followRedirects
                + "|sslMode:" + sslMode
                + "|" + getProxyConfigKey(baseUri, proxyPolicy);
    }

    /**
     * Dựa trên mục tiêu URL Nhận đại lý toàn cầu được áp dụng/SSL Định cấu hình máy khách và ghi đè các tham số thời gian chờ nếu cần.
     */
    public static OkHttpClient getClientForUrl(String url,
                                               boolean followRedirects,
                                               int connectTimeoutMs,
                                               int readTimeoutMs,
                                               int writeTimeoutMs) {
        OkHttpClient baseClient = getClient(extractBaseUri(url), followRedirects);
        return baseClient.newBuilder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Creates an isolated client for protocol integrations that must not reuse the
     * general request client's connection pool or cookie jar.
     *
     * <p>The client still uses the global proxy and SSL settings, but owns its
     * dispatcher, connection pool, and protocol selection.</p>
     */
    public static OkHttpClient createDedicatedClientForUrl(String url,
                                                           boolean followRedirects,
                                                           int connectTimeoutMs,
                                                           int readTimeoutMs,
                                                           int writeTimeoutMs,
                                                           List<Protocol> protocols) {
        String baseUri = extractBaseUri(url);
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(DEFAULT_MAX_REQUESTS);
        dispatcher.setMaxRequestsPerHost(DEFAULT_MAX_REQUESTS_PER_HOST);

        OkHttpClient client = createClient(
                baseUri,
                followRedirects,
                resolveSslVerificationMode(baseUri, HttpRequestProxyPolicy.DEFAULT),
                dispatcher,
                1,
                30L,
                true,
                CookieJar.NO_COOKIES,
                HttpRequestProxyPolicy.DEFAULT
        );
        OkHttpClient.Builder builder = client.newBuilder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS);
        if (protocols != null && !protocols.isEmpty()) {
            builder.protocols(protocols);
        }
        return builder.build();
    }

    /**
     * Cấu hình proxy mạng
     */
    private static void configureProxy(OkHttpClient.Builder builder, String baseUri, HttpRequestProxyPolicy proxyPolicy) {
        HttpRequestProxyPolicy resolvedProxyPolicy = HttpRequestProxyPolicy.normalize(proxyPolicy);
        if (resolvedProxyPolicy == HttpRequestProxyPolicy.NO_PROXY || !isProxyEnabledForPolicy(resolvedProxyPolicy)) {
            // The JVM may still expose OS proxy settings via the default ProxySelector.
            // Force direct connections when the in-app proxy toggle is off.
            builder.proxy(Proxy.NO_PROXY);
            return;
        }

        if (settings().isSystemProxyMode()) {
            configureSystemProxy(builder, baseUri, resolvedProxyPolicy);
            return;
        }

        try {
            Proxy proxy = createManualProxy();
            if (proxy == null) {
                builder.proxy(Proxy.NO_PROXY);
                return;
            }

            builder.proxy(proxy);
            SocksProxyAuthenticatorSupport.configureFor(proxy, shouldAllowSocksAuthenticationWhenGlobalProxyDisabled(resolvedProxyPolicy));
            configureProxyAuthenticator(builder);
        } catch (Exception e) {
            log.error("Failed to configure proxy for {}: {}", baseUri, e.getMessage(), e);
            builder.proxy(Proxy.NO_PROXY);
        }
    }

    private static Proxy createManualProxy() {
        String proxyHost = normalizedProxyHost();
        if (proxyHost.isEmpty()) {
            return null;
        }

        int proxyPort = settings().getProxyPort();
        if (proxyPort <= 0 || proxyPort > 65535) {
            return null;
        }
        String proxyType = settings().getProxyType();
        Proxy.Type type = HttpRuntimeSettings.PROXY_TYPE_SOCKS.equalsIgnoreCase(proxyType) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        return new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
    }

    private static void configureSystemProxy(OkHttpClient.Builder builder,
                                             String baseUri,
                                             HttpRequestProxyPolicy proxyPolicy) {
        ProxySelector selector = getSystemProxySelector();
        if (selector == null) {
            log.debug("System proxy auto-detection is unavailable, using direct connection");
            return;
        }

        builder.proxySelector(selector);
        configureSystemSocksProxyAuthentication(selector, baseUri, shouldAllowSocksAuthenticationWhenGlobalProxyDisabled(proxyPolicy));
        configureProxyAuthenticator(builder);
    }

    private static void configureSystemSocksProxyAuthentication(ProxySelector selector,
                                                                String baseUri,
                                                                boolean allowWhenGlobalProxyDisabled) {
        URI uri = tryParseUri(baseUri, "system SOCKS proxy authentication");
        if (uri == null) {
            return;
        }
        List<Proxy> proxies = selectProxyCandidates(selector, uri, baseUri);
        if (proxies == null || proxies.isEmpty()) {
            return;
        }
        for (Proxy proxy : proxies) {
            SocksProxyAuthenticatorSupport.configureFor(proxy, allowWhenGlobalProxyDisabled);
        }
    }

    private static boolean shouldAllowSocksAuthenticationWhenGlobalProxyDisabled(HttpRequestProxyPolicy proxyPolicy) {
        return proxyPolicy == HttpRequestProxyPolicy.USE_PROXY && !settings().isProxyEnabled();
    }

    private static void configureProxyAuthenticator(OkHttpClient.Builder builder) {
        String proxyUsername = settings().getProxyUsername();
        String proxyPassword = settings().getProxyPassword();

        if (proxyUsername.trim().isEmpty() || proxyPassword.trim().isEmpty()) {
            return;
        }

        // Auto-detected proxies only receive credentials after an explicit Basic challenge.
        boolean allowPreemptive = !settings().isSystemProxyMode();
        String credential = Credentials.basic(proxyUsername.trim(), proxyPassword);
        Authenticator proxyAuthenticator = (route, response) ->
                buildProxyAuthorizationRequest(response, credential, allowPreemptive);
        builder.proxyAuthenticator(proxyAuthenticator);
    }

    static Request buildProxyAuthorizationRequest(Response response, String credential, boolean allowPreemptive) {
        if (response.request().header(PROXY_AUTHORIZATION_HEADER) != null) {
            // Credentials were already rejected; surface the 407 instead of retrying until OkHttp's follow-up limit.
            return null;
        }
        boolean challengeSupported = false;
        for (Challenge challenge : response.challenges()) {
            String scheme = challenge.scheme();
            if ("Basic".equalsIgnoreCase(scheme)
                    || (allowPreemptive && PREEMPTIVE_CHALLENGE_SCHEME.equalsIgnoreCase(scheme))) {
                challengeSupported = true;
                break;
            }
        }
        if (!challengeSupported) {
            return null;
        }
        return response.request().newBuilder()
                .header(PROXY_AUTHORIZATION_HEADER, credential)
                .build();
    }

    /**
     * cấu hìnhSSLCài đặt (Xử lý tất cả trong mộtHTTPSĐã kết nốiSSLcấu hình)
     */
    private static OkHttpClient createClient(String baseUri,
                                             boolean followRedirects,
                                             SSLConfigurationUtil.SSLVerificationMode sslMode,
                                             HttpRequestProxyPolicy proxyPolicy) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(DEFAULT_MAX_REQUESTS);
        dispatcher.setMaxRequestsPerHost(DEFAULT_MAX_REQUESTS_PER_HOST);
        return createClient(
                baseUri,
                followRedirects,
                sslMode,
                dispatcher,
                MAX_IDLE_CONNECTIONS,
                KEEP_ALIVE_DURATION,
                true,
                GLOBAL_COOKIE_JAR,
                proxyPolicy
        );
    }

    public static OkHttpClient createClientForRuntimeConfig(String baseUri,
                                                            boolean followRedirects,
                                                            SSLConfigurationUtil.SSLVerificationMode sslMode,
                                                            HttpClientRuntimeConfig config,
                                                            CookieJar cookieJar,
                                                            HttpRequestProxyPolicy proxyPolicy) {
        HttpClientRuntimeConfig resolvedConfig = config == null ? HttpClientRuntimeConfig.defaults() : config;
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(resolvedConfig.maxRequests());
        dispatcher.setMaxRequestsPerHost(resolvedConfig.maxRequestsPerHost());
        return createClient(
                baseUri,
                followRedirects,
                sslMode,
                dispatcher,
                resolvedConfig.maxIdleConnections(),
                resolvedConfig.keepAliveDurationSeconds(),
                false,
                cookieJar,
                proxyPolicy
        );
    }

    private static OkHttpClient createClient(String baseUri,
                                             boolean followRedirects,
                                             SSLConfigurationUtil.SSLVerificationMode sslMode,
                                             Dispatcher dispatcher,
                                             HttpRequestProxyPolicy proxyPolicy) {
        return createClient(
                baseUri,
                followRedirects,
                sslMode,
                dispatcher,
                MAX_IDLE_CONNECTIONS,
                KEEP_ALIVE_DURATION,
                true,
                GLOBAL_COOKIE_JAR,
                proxyPolicy
        );
    }

    private static OkHttpClient createClient(String baseUri,
                                             boolean followRedirects,
                                             SSLConfigurationUtil.SSLVerificationMode sslMode,
                                             Dispatcher dispatcher,
                                             int poolMaxIdleConnections,
                                             long poolKeepAliveDurationSeconds,
                                             boolean sslConsoleLoggingEnabled,
                                             CookieJar cookieJar,
                                             HttpRequestProxyPolicy proxyPolicy) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .socketFactory(JvmProxyBypassingSocketFactory.INSTANCE)
                .connectTimeout(0, TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .writeTimeout(0, TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(
                        poolMaxIdleConnections,
                        poolKeepAliveDurationSeconds,
                        TimeUnit.SECONDS
                ))
                .retryOnConnectionFailure(true)
                // Prefer IPv6 on dual-stack hosts while quickly falling back to IPv4
                // when the IPv6 path is unavailable (Happy Eyeballs / RFC 8305).
                .fastFallback(true)
                .followRedirects(followRedirects)
                .cache(null)
                // Ordinary HTTP/2 calls should not keep idle connections alive forever.
                // Realtime requests apply their own ping policy in HttpClientResolver.
                .pingInterval(0, TimeUnit.SECONDS);

        builder.cookieJar(cookieJar == null ? GLOBAL_COOKIE_JAR : cookieJar);
        configureProxy(builder, baseUri, proxyPolicy);
        configureSSLSettings(builder, baseUri, sslMode, sslConsoleLoggingEnabled);
        return builder.build();
    }

    private static void configureSSLSettings(OkHttpClient.Builder builder,
                                             String baseUri,
                                             SSLConfigurationUtil.SSLVerificationMode mode,
                                             boolean sslConsoleLoggingEnabled) {
        URI uri = tryParseUri(baseUri, "SSL configuration");
        if (uri == null || !isSecureScheme(uri.getScheme())) {
            return;
        }

        if (mode == SSLConfigurationUtil.SSLVerificationMode.LENIENT) {
            log.warn("SSL verification disabled by user settings");
        }

        SSLConfigurationUtil.configureSSL(builder, mode, uri.getHost(), resolveSecurePort(uri), sslConsoleLoggingEnabled);
    }

    public static String runtimeSettingsCacheKey(String baseUri) {
        return getProxyConfigKey(baseUri);
    }

    public static String runtimeSettingsCacheKey(String baseUri, HttpRequestProxyPolicy proxyPolicy) {
        return getProxyConfigKey(baseUri, proxyPolicy);
    }

    private static String getProxyConfigKey(String baseUri) {
        return getProxyConfigKey(baseUri, HttpRequestProxyPolicy.DEFAULT);
    }

    private static String getProxyConfigKey(String baseUri, HttpRequestProxyPolicy proxyPolicy) {
        HttpRequestProxyPolicy resolvedProxyPolicy = HttpRequestProxyPolicy.normalize(proxyPolicy);
        String proxyPart = buildProxyConfigKeyPart(baseUri, resolvedProxyPolicy);

        StringBuilder trustPart = new StringBuilder();
        for (TrustedCertificateEntry entry : settings().getCustomTrustMaterialEntries()) {
            String trustPath = entry.getPath();
            File trustFile = (trustPath == null || trustPath.isBlank()) ? null : new File(trustPath);
            long trustLastModified = (trustFile != null && trustFile.exists()) ? trustFile.lastModified() : -1L;
            long trustFileLength = (trustFile != null && trustFile.exists()) ? trustFile.length() : -1L;
            trustPart.append(entry.isEnabled())
                    .append(':')
                    .append(trustPath)
                    .append(':')
                    .append(trustLastModified)
                    .append(':')
                    .append(trustFileLength)
                    .append(':')
                    .append(entry.getPassword() == null ? 0 : entry.getPassword().hashCode())
                    .append(';');
        }
        boolean effectiveProxySslDisabled = isProxyActiveForBaseUri(baseUri, resolvedProxyPolicy)
                && settings().isProxySslVerificationDisabled();
        return String.format("%s|ssl:%b:%b|customTrust:%b:%s|%s",
                proxyPart,
                effectiveProxySslDisabled,
                settings().isRequestSslVerificationDisabled(),
                settings().isCustomTrustMaterialEnabled(),
                trustPart,
                buildClientCertificateCachePart(baseUri));
    }

    private static String buildProxyConfigKeyPart(String baseUri, HttpRequestProxyPolicy proxyPolicy) {
        if (proxyPolicy == HttpRequestProxyPolicy.NO_PROXY) {
            return "proxy:request-disabled";
        }
        if (!isProxyEnabledForPolicy(proxyPolicy)) {
            return "proxy:disabled";
        }
        try {
            return buildProxyConfigPart(baseUri);
        } catch (Exception e) {
            log.warn("Failed to build proxy cache key for {}, using direct proxy fallback", baseUri, e);
            return "proxy:unavailable";
        }
    }

    private static String buildProxyConfigPart(String baseUri) {
        if (settings().isSystemProxyMode()) {
            return "proxy:system:" + describeProxy(selectSystemProxy(baseUri)) + ":"
                    + settings().getProxyUsername() + ":" + settings().getProxyPassword().hashCode();
        }

        return String.format("proxy:manual:%s:%s:%d:%s",
                settings().getProxyType(),
                settings().getProxyHost(),
                settings().getProxyPort(),
                settings().getProxyUsername() + ":" + settings().getProxyPassword().hashCode());
    }

    private static String buildClientCertificateCachePart(String baseUri) {
        URI uri = tryParseUri(baseUri, "client certificate cache key");
        if (uri == null || !isSecureScheme(uri.getScheme())) {
            return "clientCert:none";
        }
        return SSLConfigurationUtil.clientCertificateCacheKey(uri.getHost(), resolveSecurePort(uri));
    }

    private static ProxySelector getSystemProxySelector() {
        return ProxySelector.getDefault();
    }

    static Proxy selectSystemProxy(String baseUri) {
        ProxySelector selector = getSystemProxySelector();
        if (selector == null) {
            return Proxy.NO_PROXY;
        }

        URI uri = tryParseUri(baseUri, "system proxy selection");
        if (uri == null) {
            return Proxy.NO_PROXY;
        }

        List<Proxy> proxies = selectProxyCandidates(selector, uri, baseUri);
        if (proxies == null || proxies.isEmpty()) {
            return Proxy.NO_PROXY;
        }

        for (Proxy proxy : proxies) {
            if (proxy == null) {
                continue;
            }
            if (isDirectProxy(proxy)) {
                return Proxy.NO_PROXY;
            }
            if (proxy.address() instanceof InetSocketAddress) {
                return proxy;
            }
        }
        return Proxy.NO_PROXY;
    }

    public static ProxyInspection inspectSystemProxyForUrl(String url) {
        if (url == null || url.isBlank()) {
            return new ProxyInspection(false, Proxy.NO_PROXY);
        }

        String baseUri = extractBaseUriSafely(url, "system proxy inspection");
        if (baseUri == null) {
            return new ProxyInspection(false, Proxy.NO_PROXY);
        }

        return new ProxyInspection(true, selectSystemProxy(baseUri));
    }

    /**
     * Resolves the proxy the default request policy would use for the URL under the current settings.
     */
    public static ProxyInspection inspectEffectiveProxyForUrl(String url) {
        String baseUri = extractBaseUriSafely(url, "effective proxy inspection");
        if (baseUri == null) {
            return new ProxyInspection(false, Proxy.NO_PROXY);
        }
        if (!isProxyActiveForBaseUri(baseUri, HttpRequestProxyPolicy.DEFAULT)) {
            return new ProxyInspection(true, Proxy.NO_PROXY);
        }
        Proxy proxy = settings().isSystemProxyMode() ? selectSystemProxy(baseUri) : createManualProxy();
        return new ProxyInspection(true, proxy == null ? Proxy.NO_PROXY : proxy);
    }

    public static boolean isProxyActiveForUrl(String url, HttpRequestProxyPolicy proxyPolicy) {
        String baseUri = extractBaseUriSafely(url, "proxy check");
        if (baseUri == null) {
            return false;
        }

        return isProxyActiveForBaseUri(baseUri, proxyPolicy);
    }

    private static boolean isProxyActiveForBaseUri(String baseUri, HttpRequestProxyPolicy proxyPolicy) {
        try {
            return resolveProxyActiveForBaseUri(baseUri, proxyPolicy);
        } catch (Exception e) {
            log.warn("Failed to resolve proxy activity for {}, treating proxy as inactive", baseUri, e);
            return false;
        }
    }

    private static boolean resolveProxyActiveForBaseUri(String baseUri, HttpRequestProxyPolicy proxyPolicy) {
        HttpRequestProxyPolicy resolvedProxyPolicy = HttpRequestProxyPolicy.normalize(proxyPolicy);
        if (resolvedProxyPolicy == HttpRequestProxyPolicy.NO_PROXY || !isProxyEnabledForPolicy(resolvedProxyPolicy)) {
            return false;
        }
        if (!settings().isSystemProxyMode()) {
            return createManualProxy() != null;
        }

        Proxy proxy = selectSystemProxy(baseUri);
        return !isDirectProxy(proxy);
    }

    private static String describeProxy(Proxy proxy) {
        if (isDirectProxy(proxy)) {
            return "direct";
        }

        SocketAddress address = proxy.address();
        if (address instanceof InetSocketAddress socketAddress) {
            return proxy.type() + ":" + socketAddress.getHostString() + ":" + socketAddress.getPort();
        }
        return proxy.type() + ":unknown";
    }

    private static String describeProxyForDisplay(Proxy proxy) {
        if (isDirectProxy(proxy)) {
            return "direct";
        }
        if (proxy.address() instanceof InetSocketAddress socketAddress) {
            return proxy.type() + ":" + SafeSocketAddressFormatter.hostPort(socketAddress);
        }
        return proxy.type() + ":unknown";
    }

    public static CookieManager getGlobalCookieManager() {
        return GLOBAL_COOKIE_MANAGER;
    }

    private static SSLConfigurationUtil.SSLVerificationMode resolveSslVerificationMode(String baseUri, HttpRequestProxyPolicy proxyPolicy) {
        boolean proxySslDisabled = isProxyActiveForBaseUri(baseUri, proxyPolicy)
                && settings().isProxySslVerificationDisabled();
        if (settings().isRequestSslVerificationDisabled() || proxySslDisabled) {
            return SSLConfigurationUtil.SSLVerificationMode.LENIENT;
        }
        return SSLConfigurationUtil.SSLVerificationMode.STRICT;
    }

    private static int resolveSecurePort(URI uri) {
        int port = uri.getPort();
        return port == -1 ? 443 : port;
    }

    private static boolean isSecureScheme(String scheme) {
        return "https".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme);
    }

    private static String normalizedProxyHost() {
        String proxyHost = settings().getProxyHost();
        return proxyHost == null ? "" : proxyHost.trim();
    }

    private static URI tryParseUri(String value, String context) {
        try {
            return URI.create(value);
        } catch (Exception e) {
            log.debug("Failed to parse URI for {}: {}", context, value, e);
            return null;
        }
    }

    private static List<Proxy> selectProxyCandidates(ProxySelector selector, URI uri, String rawTarget) {
        try {
            return selector.select(uri);
        } catch (Exception e) {
            log.warn("Failed to auto-detect system proxy for {}", rawTarget, e);
            return null;
        }
    }

    private static String extractBaseUriSafely(String url, String context) {
        try {
            return extractBaseUri(url);
        } catch (Exception e) {
            log.debug("Failed to extract baseUri for {}: {}", context, url, e);
            return null;
        }
    }

    private static boolean isDirectProxy(Proxy proxy) {
        return proxy == null || proxy == Proxy.NO_PROXY || proxy.type() == Proxy.Type.DIRECT;
    }

    private static boolean isProxyEnabledForPolicy(HttpRequestProxyPolicy proxyPolicy) {
        return proxyPolicy == HttpRequestProxyPolicy.USE_PROXY || settings().isProxyEnabled();
    }

    private static HttpRuntimeSettings settings() {
        return HttpRuntimeSettingsProvider.get();
    }
}
