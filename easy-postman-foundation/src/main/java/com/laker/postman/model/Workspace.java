package com.laker.postman.model;

import lombok.Data;

import java.io.File;

// WorkspaceLớp người mẫu
@Data
public class Workspace {
    private String id; // UUID
    private String name; // Tên không gian làm việc
    private String description; // Mô tả không gian làm việc
    private WorkspaceType type; // LOCAL/GIT
    private GitRepoSource gitRepoSource; // Nguồn kho:INITIALIZED（khởi tạo cục bộ)/ CLONED（nhân bản từ xa)

    /**
     * Đường dẫn cục bộ, luôn có  {@link File#separator}。
     * getter/setter Tất cả đều được chuẩn hóa và người gọi không cần phải xử lý dấu gạch chéo ở cuối theo cách thủ công.
     */
    private String path; // đường đi cục bộ

    private String gitRemoteUrl; // GitĐịa chỉ kho từ xa
    private String currentBranch; // Tên chi nhánh hiện tại
    private String remoteBranch; // Tên của nhánh từ xa hiện được theo dõi
    private String lastCommitId; // Đệ trình cuối cùngID
    private long createdAt;
    private long updatedAt;
    private String gitUsername;      // Git Tên người dùng
    private String gitPassword;      // Git Mật khẩu (lưu trữ được mã hóa)
    private String gitToken;         // Git Mã thông báo (thay thế cho mật khẩu, được ưu tiên)
    private GitAuthType gitAuthType; // Loại xác thực
    private String sshPrivateKeyPath; // SSH Đường dẫn tệp khóa riêng
    private String sshPassphrase;     // SSH Mật khẩu khóa riêng (tùy chọn)

    /**
     * Trở lại ban nhạc cuối {@link File#separator} con đường kinh điển.
     * Không vấn đề gì path Dù có dấu phân cách ở cuối khi lưu trữ hay không, phương thức này luôn trả về một định dạng thống nhất.
     */
    public String getPath() {
        if (path == null || path.isBlank()) {
            return path;
        }
        return hasTrailingSeparator(path) ? path : path + File.separator;
    }

    /**
     * Tự động chuẩn hóa các dấu phân cách ở cuối trước khi lưu trữ để duy trì tính nhất quán bên trong.
     */
    public void setPath(String path) {
        if (path == null || path.isBlank()) {
            this.path = path;
            return;
        }
        String trimmed = path.trim();
        this.path = hasTrailingSeparator(trimmed) ? trimmed : trimmed + File.separator;
    }

    private boolean hasTrailingSeparator(String value) {
        return value.endsWith("/") || value.endsWith("\\");
    }
}
