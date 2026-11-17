package com.laker.postman.model;

// Định nghĩa liệt kê
public enum GitRepoSource {
    INITIALIZED, // Đẩy vào điều khiển từ xa sau khi khởi tạo cục bộ
    CLONED       // Sao chép từ kho lưu trữ từ xa
}