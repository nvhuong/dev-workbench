package com.laker.postman.performance.core.run;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceRunStatus {
    // Những giá trị này sẽ đi vào CLI result.json và worker HTTP/JSON Thỏa thuận, xác nhận trước khi thêm trạng thái mới master/GUI Ngữ nghĩa tổng hợp.
    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String STOPPING = "STOPPING";
    public static final String STOPPED = "STOPPED";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String UNKNOWN = "UNKNOWN";

    public boolean isTerminal(String status) {
        return SUCCESS.equals(status) || FAILED.equals(status) || STOPPED.equals(status);
    }
}
