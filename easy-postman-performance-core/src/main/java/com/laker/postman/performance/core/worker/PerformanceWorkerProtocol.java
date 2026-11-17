package com.laker.postman.performance.core.worker;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceWorkerProtocol {
    // Điều khiển từ xa worker Phiên bản giao thức điều khiển;master Sử dụng nó để chặn những cái cũ trước khi gửi worker，Tránh cái cũ và cái mới JVM Chạy hỗn hợp tạo ra báo cáo lỗi.
    public static final String CURRENT_VERSION = "2026.06.02";
}
