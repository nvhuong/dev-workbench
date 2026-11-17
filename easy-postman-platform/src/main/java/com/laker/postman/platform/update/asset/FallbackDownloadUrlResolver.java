package com.laker.postman.platform.update.asset;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.platform.update.source.UpdateSource;
import com.laker.postman.platform.update.source.UpdateSourceSelector;
import lombok.extern.slf4j.Slf4j;

/**
 * Tìm địa chỉ gói cài đặt của cùng phiên bản và nền tảng từ dữ liệu phát hành thực của nguồn cập nhật thay thế.
 */
@Slf4j
public class FallbackDownloadUrlResolver {

    private final UpdateSourceSelector sourceSelector;
    private final PlatformDownloadUrlResolver downloadUrlResolver;

    public FallbackDownloadUrlResolver(UpdateSourceSelector sourceSelector) {
        this(sourceSelector, new PlatformDownloadUrlResolver());
    }

    FallbackDownloadUrlResolver(UpdateSourceSelector sourceSelector,
                                PlatformDownloadUrlResolver downloadUrlResolver) {
        this.sourceSelector = sourceSelector;
        this.downloadUrlResolver = downloadUrlResolver;
    }

    /**
     * Truy vấn nguồn thay thế cho gói cài đặt nền tảng hiện tại tương ứng chính xác với phiên bản mong muốn.
     *
     * @return Địa chỉ tải xuống thay thế; được trả về khi nguồn thay thế không xác định, chưa được đồng bộ hóa hoặc không có tệp đính kèm cho nền tảng hiện tại {@code null}
     */
    public String resolve(String expectedVersion, String primarySourceName) {
        if (expectedVersion == null || expectedVersion.isBlank()) {
            return null;
        }

        UpdateSource fallbackSource = sourceSelector.getFallbackSource(primarySourceName);
        if (fallbackSource == null) {
            log.info("No fallback update source for primary source: {}", primarySourceName);
            return null;
        }

        try {
            JSONObject releaseInfo = fallbackSource.fetchReleaseByTag(expectedVersion);
            if (releaseInfo == null) {
                log.info("Fallback source {} has not published release {} yet",
                        fallbackSource.getName(), expectedVersion);
                return null;
            }

            String tagName = releaseInfo.getStr("tag_name");
            if (!expectedVersion.equals(tagName)) {
                log.warn("Fallback source {} returned unexpected release tag: expected {}, actual {}",
                        fallbackSource.getName(), expectedVersion, tagName);
                return null;
            }

            JSONArray assets = releaseInfo.getJSONArray("assets");
            if (assets == null || assets.isEmpty()) {
                log.info("Fallback release {} has no assets", tagName);
                return null;
            }
            String downloadUrl = downloadUrlResolver.resolveDownloadUrl(assets);
            if (downloadUrl == null) {
                log.info("Fallback release {} has no installer for the current platform", tagName);
                return null;
            }

            log.info("Resolved fallback download URL from {} for {}: {}",
                    fallbackSource.getName(), expectedVersion, downloadUrl);
            return downloadUrl;
        } catch (Exception ex) {
            log.warn("Failed to resolve fallback download URL from {}: {}",
                    fallbackSource.getName(), ex.getMessage());
            return null;
        }
    }
}
