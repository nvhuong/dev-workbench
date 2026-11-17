package com.laker.postman.http.runtime.ssl;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tùy chỉnh SSLSocketFactory，để bắt giữSSLThông tin chứng chỉ trong quá trình bắt tay
 */
@Slf4j
public class CertificateCapturingSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;

    // Được sử dụng để chuyển chứng chỉ được thu thập gần đây nhất (dưới dạng dự phòng), với tính năng cách ly theo từng luồng để tránh dữ liệu chuỗi đồng thời
    private static final ThreadLocal<List<Certificate>> lastCapturedCertificates =
            ThreadLocal.withInitial(Collections::emptyList);

    public CertificateCapturingSSLSocketFactory(SSLContext sslContext) {
        this.delegate = sslContext.getSocketFactory();
    }

    /**
     * Nhận chứng chỉ được chụp gần đây nhất (thay thế)
     */
    public static List<Certificate> getLastCapturedCertificates() {
        return new ArrayList<>(lastCapturedCertificates.get());
    }

    static void rememberCapturedCertificates(List<Certificate> certificates) {
        if (certificates == null || certificates.isEmpty()) {
            clearLastCapturedCertificates();
            return;
        }
        lastCapturedCertificates.set(new ArrayList<>(certificates));
    }

    public static void clearLastCapturedCertificates() {
        lastCapturedCertificates.remove();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(s, host, port, autoClose);
        return wrapSocket(socket);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(host, port);
        return wrapSocket(socket);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(host, port, localHost, localPort);
        return wrapSocket(socket);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(host, port);
        return wrapSocket(socket);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(address, port, localAddress, localPort);
        return wrapSocket(socket);
    }

    /**
     * Bưu kiện SSLSocket để nắm bắt thông tin chứng chỉ
     */
    private SSLSocket wrapSocket(SSLSocket socket) {
        socket.setUseClientMode(true);

        // Thêm trình nghe bắt tay để nắm bắt thông tin chứng chỉ
        socket.addHandshakeCompletedListener(event -> {
            try {
                captureCertificates(event);
            } catch (Exception e) {
                log.error("Failed to capture certificates: {}", e.getMessage(), e);
            }
        });

        return socket;
    }

    /**
     * Nắm bắt thông tin chứng chỉ từ các sự kiện bắt tay
     */
    private void captureCertificates(HandshakeCompletedEvent event) {
        try {
            SSLSession session = event.getSession();
            String sessionId = bytesToHex(session.getId());

            log.debug("SSL Handshake completed - Protocol: {}, Cipher: {}, SessionId: {}",
                    session.getProtocol(), session.getCipherSuite(), sessionId);

            // Từ SSLSession Nhận giấy chứng nhận
            Certificate[] certs = session.getPeerCertificates();
            if (certs != null && certs.length > 0) {
                List<Certificate> certList = new ArrayList<>(java.util.Arrays.asList(certs));
                rememberCapturedCertificates(certList);

                log.debug("Captured {} certificates from SSLSession (SessionId: {})",
                        certs.length, sessionId);

                // Ghi lại các chi tiết của chứng chỉ đầu tiên
                if (certs[0] instanceof java.security.cert.X509Certificate x509) {
                    log.debug("Certificate Subject: {}",
                            x509.getSubjectX500Principal().getName());
                    log.debug("Certificate Issuer: {}",
                            x509.getIssuerX500Principal().getName());
                }
            } else {
                clearLastCapturedCertificates();
                log.warn("No certificates in SSLSession");
            }
        } catch (SSLPeerUnverifiedException e) {
            clearLastCapturedCertificates();
            log.debug("Peer unverified: {}", e.getMessage());
        }
    }

    /**
     * Chuyển đổi mảng byte thành chuỗi thập lục phân
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
