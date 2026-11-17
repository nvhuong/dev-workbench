package com.laker.postman.http.runtime.ssl;

/**
 * SSLĐóng gói thống nhất các ngoại lệ liên quan
 */
public class SSLException extends RuntimeException {

    private final SSLErrorType errorType;

    public SSLException(String message, SSLErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public SSLException(String message, Throwable cause, SSLErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    public SSLErrorType getErrorType() {
        return errorType;
    }

    /**
     * SSLLoại lỗi enum
     */
    public enum SSLErrorType {
        /** The certificate has expired */
        CERTIFICATE_EXPIRED("Certificate expired"),
        /** The certificate is not valid */
        CERTIFICATE_NOT_YET_VALID("Certificate not yet valid"),
        /** Chứng chỉ không đáng tin cậy */
        UNTRUSTED_CERTIFICATE("Untrusted certificate"),
        /** Tên máy chủ không khớp */
        HOSTNAME_MISMATCH("Hostname mismatch"),
        /** The certificate chain is incomplete */
        INCOMPLETE_CHAIN("Incomplete certificate chain"),
        /** Giấy chứng nhận tự ký */
        SELF_SIGNED_CERTIFICATE("Self-signed certificate"),
        /** Certification path verification failed */
        PATH_VALIDATION_FAILED("Certificate path validation failed"),
        /** SSLLỗi cấu hình */
        CONFIGURATION_ERROR("SSL configuration error"),
        /** Không rõSSLsai lầm */
        UNKNOWN("Unknown SSL error");

        private final String description;

        SSLErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}

