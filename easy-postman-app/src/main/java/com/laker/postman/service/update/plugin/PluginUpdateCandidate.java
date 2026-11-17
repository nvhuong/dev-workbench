package com.laker.postman.service.update.plugin;

/**
 * Ứng viên cập nhật plug-in.
 */
public record PluginUpdateCandidate(
        String pluginId,
        String pluginName,
        String installedVersion,
        String latestVersion
) {
}
