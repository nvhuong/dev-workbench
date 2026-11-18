package com.nvhuong.devworkbench.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Cấu hình chứng chỉ ứng dụng khách
 * để được hỗ trợ mTLS (mutual TLS) Chứng nhận
 */
@Data
public class ClientCertificate implements Serializable {
    private String id; // Mã định danh duy nhất
    private String name; // Certificate configuration name (optional)
    private String host; // Mẫu khớp tên máy chủ hoặc tên miền (được hỗ trợ ký tự đại diện, ví dụ: *.example.com）
    private int port; // Số cổng,0 Cho biết phù hợp với tất cả các cổng
    private String certPath; // Certificate file path (PFX/P12 hoặc PEM định dạng)
    private String certType; // Certificate type:PFX, PEM
    private String certPassword; // Certificate password (for PFX/P12）
    private String keyPath; // Đường dẫn tệp khóa riêng (PEM định dạng)
    private String keyPassword; // Mật khẩu khóa riêng (tùy chọn)
    private boolean enabled; // Có bật cấu hình chứng chỉ này hay không
    private long createdAt; // Thời gian sáng tạo
    private long updatedAt; // Cập nhật thời gian

    // Certificate type constant
    public static final String CERT_TYPE_PFX = "PFX";
    public static final String CERT_TYPE_PEM = "PEM";

    public ClientCertificate() {
        this.enabled = true;
        this.port = 0; // Mặc định khớp với tất cả các cổng
        this.certType = CERT_TYPE_PFX;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Kiểm tra xem chứng chỉ này có khớp với máy chủ và cổng được chỉ định hay không
     */
    public boolean matches(String targetHost, int targetPort) {
        if (!enabled) {
            return false;
        }

        // Kiểm tra kết nối cổng
        if (port != 0 && port != targetPort) {
            return false;
        }

        // Kiểm tra sự trùng khớp của tên máy chủ
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        String pattern = host.trim().toLowerCase();
        String target = targetHost.toLowerCase();

        // Kết hợp ký tự đại diện được hỗ trợ
        if (pattern.startsWith("*.")) {
            String domain = pattern.substring(2);
            // cuộc thi đấu "api.example.com" hoặc "example.com" chính nó
            return target.endsWith("." + domain) || target.equals(domain);
        }

        // Trận đấu chính xác
        return pattern.equals(target);
    }
}

