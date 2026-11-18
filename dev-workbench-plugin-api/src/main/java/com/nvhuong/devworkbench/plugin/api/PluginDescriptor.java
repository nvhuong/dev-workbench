package com.nvhuong.devworkbench.plugin.api;

/**
 * Siêu dữ liệu plugin.
 *
 * <p>Quy ước ngữ nghĩa trường:
 * <p>- min/maxAppVersion：Hạn chế phạm vi phát hành máy chủ. Bạn chỉ nên điền khi plug-in phụ thuộc vào một chức năng máy chủ nhất định
 * <p>- min/maxPlatformVersion：Nền tảng plugin hạn chế SPI Phạm vi, thuộc ranh giới tương thích nội bộ của cơ chế trình cắm
 * <p>- Nó sẽ được kiểm tra khi chạy app + platform，Việc tải chỉ được phép nếu cả hai đều hài lòng
 */
public record PluginDescriptor(
        String id,
        String name,
        String version,
        String entryClass,
        String description,
        String homepageUrl,
        String minAppVersion,
        String maxAppVersion,
        String minPlatformVersion,
        String maxPlatformVersion
) {

    public PluginDescriptor(String id, String name, String version, String entryClass) {
        this(id, name, version, entryClass, "", "", "", "", "", "");
    }

    public PluginDescriptor(String id, String name, String version, String entryClass,
                            String description, String homepageUrl) {
        this(id, name, version, entryClass, description, homepageUrl, "", "", "", "");
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    public boolean hasHomepageUrl() {
        return homepageUrl != null && !homepageUrl.isBlank();
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
