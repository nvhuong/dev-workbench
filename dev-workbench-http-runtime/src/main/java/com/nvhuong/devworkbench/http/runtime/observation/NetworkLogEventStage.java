package com.nvhuong.devworkbench.http.runtime.observation;

/**
 * HTTP Giai đoạn nhật ký mạng được tạo bởi lớp thực thi.
 * <p>
 * Ở đây chỉ có ngữ nghĩa truyền tải được giữ lại và màu sắc, biểu tượng, v.v. không được mang theo Swing hiển thị thông tin;UI Lớp này chịu trách nhiệm ánh xạ các giai đoạn này thành các kiểu cụ thể.
 */
public enum NetworkLogEventStage {
    FAILED,
    CALL_FAILED,
    REQUEST_FAILED,
    RESPONSE_FAILED,
    CONNECT_FAILED,
    CANCELED,
    CALL_START,
    CALL_END,
    REQUEST_COMPLETE,
    DISPATCHER_QUEUE_START,
    DISPATCHER_QUEUE_END,
    RETRY_DECISION,
    FOLLOW_UP_DECISION,
    CACHE_HIT,
    CACHE_MISS,
    CACHE_CONDITIONAL_HIT,
    SATISFACTION_FAILURE,
    REQUEST_PREPARED,
    SECURE_CONNECT_START,
    SECURE_CONNECT_END,
    CONNECT_START,
    CONNECT_END,
    CONNECTION_ACQUIRED,
    CONNECTION_RELEASED,
    ROUTE_INTERCEPTED,
    DNS_START,
    DNS_END,
    PROXY_SELECT,
    PROXY_SELECT_START,
    PROXY_SELECT_END,
    REQUEST_HEADERS_START,
    REQUEST_HEADERS_END,
    REQUEST_BODY_START,
    REQUEST_BODY_END,
    RESPONSE_HEADERS_START,
    RESPONSE_HEADERS_END,
    RESPONSE_HEADERS_END_REDIRECT,
    RESPONSE_BODY_START,
    RESPONSE_BODY_END,
    REDIRECT,
    DEFAULT
}
