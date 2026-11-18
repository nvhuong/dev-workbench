package com.nvhuong.devworkbench.http.runtime.ssl;

import lombok.extern.slf4j.Slf4j;

import java.security.cert.CertificateException;

/**
 * Certificate error message parser
 * Chịu trách nhiệm chuyển đổi các ngoại lệ chứng chỉ khác nhau thành thông báo lỗi mà con người có thể đọc được
 */
@Slf4j
public class CertificateErrorParser {

    private CertificateErrorParser() {
        // Lớp tiện ích, hàm tạo ẩn
    }

    /**
     * Ngoại lệ trong việc phân tích chứng chỉ, trả về loại lỗi
     */
    public static SSLException.SSLErrorType parseErrorType(CertificateException e) {
        String message = e.getMessage();
        if (message == null) {
            return SSLException.SSLErrorType.UNKNOWN;
        }

        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("certificate expired") || lowerMessage.contains("notafter")) {
            return SSLException.SSLErrorType.CERTIFICATE_EXPIRED;
        } else if (lowerMessage.contains("notbefore") || lowerMessage.contains("not yet valid")) {
            return SSLException.SSLErrorType.CERTIFICATE_NOT_YET_VALID;
        } else if (lowerMessage.contains("unable to find valid certification path") ||
                   lowerMessage.contains("pkix path building failed")) {
            return SSLException.SSLErrorType.UNTRUSTED_CERTIFICATE;
        } else if (lowerMessage.contains("pkix path validation failed")) {
            return SSLException.SSLErrorType.PATH_VALIDATION_FAILED;
        }

        return SSLException.SSLErrorType.UNKNOWN;
    }

    /**
     * Trích xuất các thông báo lỗi thân thiện với người dùng
     */
    public static String extractUserFriendlyMessage(CertificateException e) {
        SSLException.SSLErrorType errorType = parseErrorType(e);

        return switch (errorType) {
            case CERTIFICATE_EXPIRED -> "Certificate has expired";
            case CERTIFICATE_NOT_YET_VALID -> "Certificate is not yet valid";
            case UNTRUSTED_CERTIFICATE -> "Certificate is not trusted (untrusted root certificate)";
            case PATH_VALIDATION_FAILED -> "Certificate path validation failed";
            default -> {
                String message = e.getMessage();
                yield message != null ? message : "Certificate validation failed";
            }
        };
    }
}
