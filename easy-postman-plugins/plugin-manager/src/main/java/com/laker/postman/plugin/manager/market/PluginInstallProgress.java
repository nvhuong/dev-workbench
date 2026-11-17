package com.laker.postman.plugin.manager.market;

/**
 * Ảnh chụp tiến trình cài đặt plugin.
 */
public record PluginInstallProgress(
        Stage stage,
        String pluginId,
        String installUrl,
        long downloadedBytes,
        long totalBytes,
        double bytesPerSecond
) {

    public enum Stage {
        CONNECTING,
        DOWNLOADING,
        VERIFYING,
        INSTALLING,
        COMPLETED
    }

    public boolean hasKnownTotalBytes() {
        return totalBytes > 0;
    }
}
