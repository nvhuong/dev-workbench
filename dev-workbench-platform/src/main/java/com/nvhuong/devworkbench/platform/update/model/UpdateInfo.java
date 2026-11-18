package com.nvhuong.devworkbench.platform.update.model;

import cn.hutool.json.JSONObject;
import lombok.Data;

/**
 * Cập nhật lớp đóng gói thông tin
 */
@Data
public class UpdateInfo {

    public enum Status {
        UPDATE_AVAILABLE,           // Đã có phiên bản mới (và có gói cài đặt cho nền tảng tương ứng)
        UPDATE_AVAILABLE_NO_ASSET,  // Đã có phiên bản mới nhưng chưa có gói cài đặt cho nền tảng tương ứng (tài nguyên chưa được tải lên)
        NO_UPDATE,                  // Không có cập nhật
        CHECK_FAILED                // Kiểm tra không thành công
    }

    private final Status status;
    private final String currentVersion;
    private final String latestVersion;
    private final String message;
    private final JSONObject releaseInfo;

    private UpdateInfo(Status status, String currentVersion, String latestVersion, String message, JSONObject releaseInfo) {
        this.status = status;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.message = message;
        this.releaseInfo = releaseInfo;
    }

    /**
     * Tạo thông tin cập nhật khi nó có sẵn
     */
    public static UpdateInfo updateAvailable(String currentVersion, String latestVersion, JSONObject releaseInfo) {
        return new UpdateInfo(Status.UPDATE_AVAILABLE, currentVersion, latestVersion,
                "New version available: " + latestVersion, releaseInfo);
    }

    /**
     * Tạo thông tin có bản cập nhật nhưng không có gói cài đặt nền tảng (tài nguyên chưa được tải lên hoặc không hỗ trợ nền tảng hiện tại)
     */
    public static UpdateInfo updateAvailableNoAsset(String currentVersion, String latestVersion, JSONObject releaseInfo) {
        return new UpdateInfo(Status.UPDATE_AVAILABLE_NO_ASSET, currentVersion, latestVersion,
                "New version available (no asset yet): " + latestVersion, releaseInfo);
    }

    /**
     * Tạo thông tin mà không cần cập nhật
     */
    public static UpdateInfo noUpdateAvailable(String message) {
        return new UpdateInfo(Status.NO_UPDATE, null, null, message, null);
    }

    /**
     * Tạo một thông báo kiểm tra thất bại
     */
    public static UpdateInfo checkFailed(String errorMessage) {
        return new UpdateInfo(Status.CHECK_FAILED, null, null, errorMessage, null);
    }

    /**
     * Có bản cập nhật không?
     */
    public boolean isUpdateAvailable() {
        return status == Status.UPDATE_AVAILABLE;
    }

    /**
     * Có bản cập nhật nhưng không có gói cài đặt (tài nguyên chưa được tải lên)
     */
    public boolean isUpdateAvailableNoAsset() {
        return status == Status.UPDATE_AVAILABLE_NO_ASSET;
    }

    /**
     * Kiểm tra xem nó có thất bại không
     */
    public boolean isCheckFailed() {
        return status == Status.CHECK_FAILED;
    }
}
