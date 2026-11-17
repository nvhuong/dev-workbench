package com.laker.postman.service.update.plugin;

import com.laker.postman.plugin.manager.PluginManagementService;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.platform.update.source.UpdateSource;
import com.laker.postman.platform.update.source.UpdateSourceSelector;
import lombok.experimental.UtilityClass;

import java.util.Locale;

/**
 * Giải quyết địa chỉ thư mục thị trường plug-in theo cài đặt cập nhật tự động.
 */
@UtilityClass
public class PluginCatalogPreferenceResolver {

    private static final UpdateSourceSelector UPDATE_SOURCE_SELECTOR =
            new UpdateSourceSelector(SettingManager::getUpdateSourcePreference);

    public static String resolveEffectiveCatalogUrl() {
        return resolveEffectiveCatalogUrl(true);
    }

    public static String resolveEffectiveCatalogUrl(boolean autoDetectSource) {
        String savedCatalogUrl = PluginManagementService.getCatalogUrl();
        if (savedCatalogUrl == null || savedCatalogUrl.isBlank()) {
            return resolvePreferredOfficialCatalogUrl(autoDetectSource);
        }

        String officialSource = PluginManagementService.detectOfficialCatalogSource(savedCatalogUrl);
        if (!officialSource.isBlank()) {
            return resolvePreferredOfficialCatalogUrl(autoDetectSource);
        }
        return savedCatalogUrl.trim();
    }

    public static String resolvePreferredOfficialCatalogUrl() {
        return resolvePreferredOfficialCatalogUrl(true);
    }

    public static String resolvePreferredOfficialCatalogUrl(boolean autoDetectSource) {
        String preference = SettingManager.getUpdateSourcePreference();
        if ("github".equalsIgnoreCase(preference)) {
            return PluginManagementService.getOfficialCatalogUrl("github");
        }
        if ("gitee".equalsIgnoreCase(preference)) {
            return PluginManagementService.getOfficialCatalogUrl("gitee");
        }
        if (!autoDetectSource) {
            return PluginManagementService.getOfficialCatalogUrl("gitee");
        }

        UpdateSource source = UPDATE_SOURCE_SELECTOR.selectBestSource();
        String sourceName = source == null ? "" : source.getName().toLowerCase(Locale.ROOT);
        if (sourceName.contains("hub")) {
            return PluginManagementService.getOfficialCatalogUrl("github");
        }
        return PluginManagementService.getOfficialCatalogUrl("gitee");
    }
}
