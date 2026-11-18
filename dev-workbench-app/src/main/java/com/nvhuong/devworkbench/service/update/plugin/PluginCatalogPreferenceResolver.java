package com.nvhuong.devworkbench.service.update.plugin;

import com.nvhuong.devworkbench.plugin.manager.PluginManagementService;
import lombok.experimental.UtilityClass;

/**
 * Giải quyết địa chỉ thư mục thị trường plug-in theo cài đặt cập nhật tự động.
 */
@UtilityClass
public class PluginCatalogPreferenceResolver {

    public static String resolveEffectiveCatalogUrl() {
        return resolveEffectiveCatalogUrl(true);
    }

    public static String resolveEffectiveCatalogUrl(boolean autoDetectSource) {
        String savedCatalogUrl = PluginManagementService.getCatalogUrl();
        if (savedCatalogUrl == null || savedCatalogUrl.isBlank()) {
            return PluginManagementService.getOfficialCatalogUrl("github");
        }

        String officialSource = PluginManagementService.detectOfficialCatalogSource(savedCatalogUrl);
        if (!officialSource.isBlank()) {
            return PluginManagementService.getOfficialCatalogUrl("github");
        }
        return savedCatalogUrl.trim();
    }

    public static String resolvePreferredOfficialCatalogUrl() {
        return resolvePreferredOfficialCatalogUrl(true);
    }

    public static String resolvePreferredOfficialCatalogUrl(boolean autoDetectSource) {
        return PluginManagementService.getOfficialCatalogUrl("github");
    }
}
