package com.nvhuong.devworkbench.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * HTTP Yêu cầu các hằng tiêu đề và các lớp tiện ích
 */
@UtilityClass
public class HttpHeaderConstants {

    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String DEV_WORKBENCH_CONTENT_LENGTH = "DevWorkbench-Content-Length";
    public static final String DEV_WORKBENCH_CONTENT_ENCODING = "DevWorkbench-Content-Encoding";

    /**
     * chungHTTPDanh sách tiêu đề yêu cầu (theo thứ tự bảng chữ cái)
     * Tham khảo DevWorkbench và tiêu chuẩn HTTP đặc điểm kỹ thuật
     */
    public static final List<String> COMMON_HEADERS = Collections.unmodifiableList(Arrays.asList(
            // A
            "Accept",
            "Accept-Charset",
            "Accept-Encoding",
            "Accept-Language",
            "Accept-Datetime",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Authorization",
            // C
            "Cache-Control",
            "Connection",
            "Content-Encoding",
            "Content-Length",
            "Content-MD5",
            "Content-Type",
            "Cookie",
            // D
            "Date",
            "DNT",
            // E
            "Expect",
            // F
            "Forwarded",
            "From",
            // H
            "Host",
            "HTTP2-Settings",
            // I
            "If-Match",
            "If-Modified-Since",
            "If-None-Match",
            "If-Range",
            "If-Unmodified-Since",
            // M
            "Max-Forwards",
            // O
            "Origin",
            // P
            "Pragma",
            "Proxy-Authorization",
            // R
            "Range",
            "Referer",
            // T
            "TE",
            "Trailer",
            "Transfer-Encoding",
            // U
            "User-Agent",
            "Upgrade",
            "Upgrade-Insecure-Requests",
            // V
            "Via",
            // W
            "Warning",
            // X (Tiêu đề tùy chỉnh phổ biến)
            "X-Requested-With",
            "X-Forwarded-For",
            "X-Forwarded-Host",
            "X-Forwarded-Proto",
            "X-CSRF-Token",
            "X-Request-ID",
            "X-Correlation-ID",
            "X-API-Key",
            "X-Auth-Token"
    ));

    /**
     * chung Content-Type giá trị
     */
    public static final List<String> COMMON_CONTENT_TYPES = Collections.unmodifiableList(Arrays.asList(
            "application/json",
            "application/xml",
            "application/x-www-form-urlencoded",
            "multipart/form-data",
            "text/plain",
            "text/html",
            "text/xml",
            "application/javascript",
            "application/pdf",
            "application/octet-stream",
            "image/jpeg",
            "image/png",
            "image/gif"
    ));

    /**
     * chung Accept giá trị
     */
    public static final List<String> COMMON_ACCEPT_VALUES = Collections.unmodifiableList(Arrays.asList(
            "*/*",
            "application/json",
            "application/xml",
            "text/html",
            "text/plain",
            "application/json, text/plain, */*"
    ));

    /**
     * chung Accept-Encoding giá trị
     */
    public static final List<String> COMMON_ACCEPT_ENCODING_VALUES = Collections.unmodifiableList(Arrays.asList(
            "gzip, deflate, br",
            "gzip, deflate",
            "gzip",
            "deflate",
            "br",
            "identity"
    ));

    /**
     * chung Cache-Control giá trị
     */
    public static final List<String> COMMON_CACHE_CONTROL_VALUES = Collections.unmodifiableList(Arrays.asList(
            "no-cache",
            "no-store",
            "max-age=0",
            "max-age=3600",
            "must-revalidate",
            "public",
            "private"
    ));

    /**
     * chung Connection giá trị
     */
    public static final List<String> COMMON_CONNECTION_VALUES = Collections.unmodifiableList(Arrays.asList(
            "keep-alive",
            "close",
            "upgrade"
    ));

    /**
     * Nhận danh sách giá trị chung tương ứng dựa trên tên tiêu đề yêu cầu
     */
    public static List<String> getCommonValuesForHeader(String headerName) {
        if (headerName == null || headerName.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Kết hợp không phân biệt chữ hoa chữ thường hỗ trợ đầu vào của người dùng content-type / Content-Type v.v. hình thức
        String normalizedHeader = headerName.trim().toLowerCase(java.util.Locale.ROOT);

        return switch (normalizedHeader) {
            case "content-type"    -> COMMON_CONTENT_TYPES;
            case "accept"          -> COMMON_ACCEPT_VALUES;
            case "accept-encoding" -> COMMON_ACCEPT_ENCODING_VALUES;
            case "cache-control"   -> COMMON_CACHE_CONTROL_VALUES;
            case "connection"      -> COMMON_CONNECTION_VALUES;
            default                -> Collections.emptyList();
        };
    }
}
