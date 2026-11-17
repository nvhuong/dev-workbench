package com.laker.postman.performance.core.model;

// Định nghĩa loại nút
public enum NodeType {
    ROOT,
    THREAD_GROUP,
    CSV_DATA_SET,
    SIMPLE,
    LOOP,
    CONDITION,
    WHILE,
    ONCE_ONLY,
    REQUEST,
    ASSERTION,
    EXTRACTOR,
    TIMER,
    SSE_CONNECT,
    SSE_READ,
    WS_CONNECT,
    WS_SEND,
    WS_READ,
    WS_CLOSE
}
