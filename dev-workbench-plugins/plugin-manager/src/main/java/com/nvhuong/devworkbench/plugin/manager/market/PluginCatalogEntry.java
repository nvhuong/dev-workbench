package com.nvhuong.devworkbench.plugin.manager.market;

/**
 * Gia nhập thị trường plugin.
 *
 * <p>Đây rồi catalog.json Mô hình siêu dữ liệu được hiển thị bên ngoài.
 * <p>- resolvedDownloadUrl：Địa chỉ tải xuống cuối cùng sau khi phân tích địa chỉ tương đối được ưu tiên cho cài đặt thực tế
 * <p>- min/maxAppVersion：Cung cấp cho người dùng sự thể hiện“hiện tại DevWorkbench Phiên bản phân phối có thể được cài đặt?”
 * <p>- min/maxPlatformVersion：Cung cấp biểu thức thời gian chạy“Plugin hiện tại SPI Nó có thể được lắp ráp?”
 */
public record PluginCatalogEntry(
        String id,
        String name,
        String version,
        String description,
        String downloadUrl,
        String homepageUrl,
        String sha256,
        String resolvedDownloadUrl,
        String minAppVersion,
        String maxAppVersion,
        String minPlatformVersion,
        String maxPlatformVersion
) {

    public PluginCatalogEntry(String id,
                              String name,
                              String version,
                              String description,
                              String downloadUrl,
                              String homepageUrl,
                              String sha256) {
        this(id, name, version, description, downloadUrl, homepageUrl, sha256, downloadUrl, "", "", "", "");
    }

    public PluginCatalogEntry(String id,
                              String name,
                              String version,
                              String description,
                              String downloadUrl,
                              String homepageUrl,
                              String sha256,
                              String resolvedDownloadUrl) {
        this(id, name, version, description, downloadUrl, homepageUrl, sha256, resolvedDownloadUrl, "", "", "", "");
    }

    public boolean isPlaceholder() {
        return "empty".equals(id);
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    public boolean hasHomepageUrl() {
        return homepageUrl != null && !homepageUrl.isBlank();
    }

    // Ưu tiên sử dụng catalog Địa chỉ tải xuống tuyệt đối được giải quyết tránh việc xử lý lặp lại các đường dẫn tương đối trong giai đoạn cài đặt.
    public String installUrl() {
        return resolvedDownloadUrl != null && !resolvedDownloadUrl.isBlank() ? resolvedDownloadUrl : downloadUrl;
    }

    public boolean hasMinAppVersion() {
        return minAppVersion != null && !minAppVersion.isBlank();
    }

    public boolean hasMaxAppVersion() {
        return maxAppVersion != null && !maxAppVersion.isBlank();
    }

    public boolean hasMinPlatformVersion() {
        return minPlatformVersion != null && !minPlatformVersion.isBlank();
    }

    public boolean hasMaxPlatformVersion() {
        return maxPlatformVersion != null && !maxPlatformVersion.isBlank();
    }
}
