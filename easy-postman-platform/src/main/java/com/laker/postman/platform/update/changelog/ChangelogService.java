package com.laker.postman.platform.update.changelog;

import cn.hutool.json.JSONArray;
import com.laker.postman.platform.update.UpdateSettingsProvider;
import com.laker.postman.platform.update.source.UpdateSource;
import com.laker.postman.platform.update.source.UpdateSourceSelector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Thay đổi dịch vụ nhật ký - Chịu trách nhiệm thu thập và định dạng nhật ký thay đổi
 */
@Slf4j
public class ChangelogService {

    /**
     * -- GETTER --
     *  Nhận bộ chọn nguồn cập nhật (để nhận Web URL）
     */
    @Getter
    private final UpdateSourceSelector sourceSelector;
    private final ChangelogFormatter formatter;
    private static final int DEFAULT_LIMIT = 10;

    public ChangelogService() {
        this(UpdateSettingsProvider.defaults());
    }

    public ChangelogService(UpdateSettingsProvider settingsProvider) {
        this.sourceSelector = new UpdateSourceSelector(settingsProvider);
        this.formatter = new ChangelogFormatter();
    }

    /**
     * Nhận nhật ký thay đổi được định dạng
     *
     * @return Văn bản nhật ký cập nhật được định dạng, được trả về khi không thành công null
     */
    public String getChangelog() {
        return getChangelog(DEFAULT_LIMIT);
    }

    /**
     * Nhận nhật ký thay đổi được định dạng
     *
     * @param limit Giới hạn số lượng phiên bản được trả về
     * @return Văn bản nhật ký cập nhật được định dạng, được trả về khi không thành công null
     */
    public String getChangelog(int limit) {
        // Chọn nguồn cập nhật tốt nhất
        UpdateSource primarySource = sourceSelector.selectBestSource();
        JSONArray releases = primarySource.fetchAllReleases(limit);

        if (releases == null) {
            // Hãy thử một nguồn thay thế
            UpdateSource fallbackSource = sourceSelector.getFallbackSource(primarySource);
            log.info("Primary source failed, trying fallback source: {}", fallbackSource.getName());
            releases = fallbackSource.fetchAllReleases(limit);
        }

        if (releases == null) {
            return null;
        }

        return formatter.format(releases);
    }

}
