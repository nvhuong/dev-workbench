package com.laker.postman.http.runtime.ssl;

import lombok.extern.slf4j.Slf4j;

import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * SSL Certificate verification tool class
 * Trách nhiệm: Kiểm tra tính hợp lệ của chứng chỉ và xác minh khớp tên máy chủ
 */
@Slf4j
public class SSLCertificateValidator {

    /**
     * Xác minh chuỗi chứng chỉ và trả về kết quả xác minh
     * @param certificates Certificate chain
     * @param hostname Tên máy chủ mục tiêu
     * @return Đối tượng kết quả xác nhận
     */
    public static SSLValidationResult validateCertificates(List<Certificate> certificates, String hostname) {
        if (certificates == null || certificates.isEmpty()) {
            return SSLValidationResult.success();
        }

        Certificate cert = certificates.get(0);
        if (!(cert instanceof X509Certificate)) {
            return SSLValidationResult.success();
        }

        X509Certificate x509 = (X509Certificate) cert;
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // 1. Kiểm tra thời hạn hiệu lực của chứng chỉ
        validateCertificateValidity(x509, errors);

        // 2. Kiểm tra xem chứng chỉ sắp hết hạn chưa (30trong vòng vài ngày)
        checkCertificateExpiringSoon(x509, warnings);

        // 3. Kiểm tra chứng chỉ tự ký
        if (isSelfSigned(x509)) {
            warnings.add("Self-signed certificate detected");
        }

        // 4. Kiểm tra sự trùng khớp của tên máy chủ
        if (hostname != null && !hostname.isEmpty()) {
            if (!isHostnameMatch(x509, hostname)) {
                errors.add("Hostname mismatch: certificate does not match '" + hostname + "'");
            }
        }

        // 5. Kiểm tra tính toàn vẹn của chuỗi chứng chỉ
        if (certificates.size() == 1 && !isSelfSigned(x509)) {
            warnings.add("Incomplete certificate chain");
        }

        if (!errors.isEmpty()) {
            return warnings.isEmpty() ?
                   SSLValidationResult.failure(errors) :
                   SSLValidationResult.failureWithWarnings(errors, warnings);
        }

        return warnings.isEmpty() ?
               SSLValidationResult.success() :
               SSLValidationResult.successWithWarnings(warnings);
    }

    /**
     * Xác minh tính hợp lệ của chứng chỉ
     */
    private static void validateCertificateValidity(X509Certificate cert,
                                                    List<String> errors) {
        try {
            cert.checkValidity();
        } catch (CertificateExpiredException e) {
            errors.add("Certificate expired on " + cert.getNotAfter());
        } catch (CertificateNotYetValidException e) {
            errors.add("Certificate not yet valid (valid from " + cert.getNotBefore() + ")");
        }
    }

    /**
     * Kiểm tra xem chứng chỉ sắp hết hạn chưa
     */
    private static void checkCertificateExpiringSoon(X509Certificate cert, List<String> warnings) {
        Date now = new Date();
        Date expiryDate = cert.getNotAfter();
        long daysUntilExpiry = (expiryDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);

        if (daysUntilExpiry > 0 && daysUntilExpiry <= 30) {
            warnings.add("Certificate expires soon (in " + daysUntilExpiry + " days)");
        }
    }

    /**
     * Kiểm tra xem chứng chỉ có được tự ký hay không
     */
    private static boolean isSelfSigned(X509Certificate cert) {
        try {
            return cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal());
        } catch (Exception e) {
            log.debug("Error checking self-signed status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Kiểm tra xem tên máy chủ có khớp với chứng chỉ không (phương thức công khai)
     */
    public static boolean isHostnameMatchPublic(X509Certificate cert, String hostname) {
        return isHostnameMatch(cert, hostname);
    }

    /**
     * Kiểm tra xem tên máy chủ có khớp với chứng chỉ không
     */
    private static boolean isHostnameMatch(X509Certificate cert, String hostname) {
        try {
            // 1. nghiên cứu CN (Common Name)
            String dn = cert.getSubjectX500Principal().getName();
            String cn = extractCN(dn);
            if (cn != null && hostnameMatches(hostname, cn)) {
                return true;
            }

            // 2. nghiên cứu Subject Alternative Names (SAN)
            var altNames = cert.getSubjectAlternativeNames();
            if (altNames != null) {
                for (List<?> altName : altNames) {
                    if (altName.size() >= 2) {
                        Integer type = (Integer) altName.get(0);
                        // Type 2 is dNSName
                        if (type == 2) {
                            String dnsName = (String) altName.get(1);
                            if (hostnameMatches(hostname, dnsName)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("Failed to inspect certificate hostname for '{}': {}", hostname, e.getMessage());
            return false;
        }
    }

    /**
     * Từ DN Trích xuất từ chuỗi CN
     */
    private static String extractCN(String dn) {
        if (dn == null) return null;

        String[] parts = dn.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }
        return null;
    }

    /**
     * Kiểm tra xem tên máy chủ có khớp không (hỗ trợ ký tự đại diện và  www tiền tố)
     */
    private static boolean hostnameMatches(String hostname, String pattern) {
        if (hostname == null || pattern == null) {
            return false;
        }

        hostname = hostname.toLowerCase();
        pattern = pattern.toLowerCase();

        // 1. Một trận đấu chính xác
        if (hostname.equals(pattern)) {
            return true;
        }

        // 2. www Kết hợp tiền tố
        // Được phép csdn.com Giấy chứng nhận phù hợp CN=www.csdn.com
        // Được phép www.csdn.com Giấy chứng nhận phù hợp CN=csdn.com
        if (hostname.startsWith("www.") && pattern.equals(hostname.substring(4))) {
            return true;
        }
        if (pattern.startsWith("www.") && hostname.equals(pattern.substring(4))) {
            return true;
        }

        // 3. Kết hợp ký tự đại diện (*.example.com)
        if (pattern.startsWith("*.")) {
            String patternSuffix = pattern.substring(1); // .example.com

            if (!hostname.endsWith(patternSuffix)) {
                return false;
            }

            // Ký tự đại diện chỉ khớp với tên miền phụ cấp một
            String prefix = hostname.substring(0, hostname.length() - patternSuffix.length());

            // prefix Không thể để trống (loại trừ example.com cuộc thi đấu *.example.com）
            if (prefix.isEmpty()) {
                return false;
            }

            // prefix Không được chứa dấu chấm (không bao gồm tên miền phụ đa cấp)
            return !prefix.contains(".");
        }

        return false;
    }
}
