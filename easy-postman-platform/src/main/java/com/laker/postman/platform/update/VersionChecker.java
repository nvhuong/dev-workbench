package com.laker.postman.platform.update;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.platform.update.asset.PlatformDownloadUrlResolver;
import com.laker.postman.platform.update.model.UpdateInfo;
import com.laker.postman.platform.update.source.UpdateSource;
import com.laker.postman.platform.update.source.UpdateSourceSelector;
import com.laker.postman.platform.update.version.VersionComparator;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Trình kiểm tra phiên bản - Chịu trách nhiệm kiểm tra thông tin phiên bản từ xa
 *
 * <p>Phiên bản được tái cấu trúc: Sử dụng mẫu chiến lược để phân tách việc triển khai các nguồn cập nhật khác nhau nhằm cải thiện khả năng bảo trì và mở rộng mã.</p>
 */
@Slf4j
public class VersionChecker {

    private final UpdateSourceSelector sourceSelector;
    private final PlatformDownloadUrlResolver downloadUrlResolver;

    public VersionChecker() {
        this(UpdateSettingsProvider.defaults());
    }

    public VersionChecker(UpdateSettingsProvider settingsProvider) {
        this.sourceSelector = new UpdateSourceSelector(settingsProvider);
        this.downloadUrlResolver = new PlatformDownloadUrlResolver();
    }

    /**
     * Kiểm tra thông tin cập nhật
     */
    public UpdateInfo checkForUpdate() {
        try {
            String currentVersion = SystemUtil.getCurrentVersion();

            // Chọn nguồn cập nhật tốt nhất
            UpdateSource primarySource = sourceSelector.selectBestSource();
            JSONObject releaseInfo = primarySource.fetchLatestReleaseInfo();

            if (releaseInfo == null) {
                // Hãy thử một nguồn thay thế
                UpdateSource fallbackSource = sourceSelector.getFallbackSource(primarySource);
                log.info("Primary source failed, trying fallback source: {}", fallbackSource.getName());
                releaseInfo = fallbackSource.fetchLatestReleaseInfo();
            }

            if (releaseInfo == null) {
                return UpdateInfo.noUpdateAvailable(I18nUtil.getMessage(MessageKeys.UPDATE_FETCH_RELEASE_FAILED));
            }

            String latestVersion = releaseInfo.getStr("tag_name");
            String sourceName = releaseInfo.getStr(UpdateSource.SOURCE_METADATA_KEY, "unknown");
            log.info("Current version: {}, Latest version: {} (from {})", currentVersion, latestVersion, sourceName);

            if (latestVersion == null) {
                return UpdateInfo.noUpdateAvailable(I18nUtil.getMessage(MessageKeys.UPDATE_NO_VERSION_INFO));
            }

            if (VersionComparator.isNewer(latestVersion, currentVersion)) {
                String downloadUrl = getDownloadUrl(releaseInfo);
                if (CharSequenceUtil.isNotBlank(downloadUrl)) {
                    return UpdateInfo.updateAvailable(currentVersion, latestVersion, releaseInfo);
                } else {
                    // Đã có phiên bản mới nhưng chưa có gói cài đặt tương ứng cho nền tảng hiện tại (có thể vẫn tải lên được)
                    log.info("Newer version {} found, but no download asset available for current platform", latestVersion);
                    return UpdateInfo.updateAvailableNoAsset(currentVersion, latestVersion, releaseInfo);
                }
            } else {
                return UpdateInfo.noUpdateAvailable(I18nUtil.getMessage(MessageKeys.UPDATE_ALREADY_LATEST, currentVersion));
            }

        } catch (Exception e) {
            log.debug("Failed to check for updates", e);
            return UpdateInfo.checkFailed(e.getMessage());
        }
    }

    /**
     * Nhận liên kết tải xuống
     */
    public String getDownloadUrl(JSONObject releaseInfo) {
        if (releaseInfo == null) {
            return null;
        }

        JSONArray assets = releaseInfo.getJSONArray("assets");
        if (assets == null || assets.isEmpty()) {
            log.info("Release assets is empty");
            return null;
        }

        String source = releaseInfo.getStr(UpdateSource.SOURCE_METADATA_KEY, "unknown");
        String downloadUrl = downloadUrlResolver.resolveDownloadUrl(assets);

        if (downloadUrl != null) {
            log.info("Found download URL from {}: {}", source, downloadUrl);
        }

        return downloadUrl;
    }
}
