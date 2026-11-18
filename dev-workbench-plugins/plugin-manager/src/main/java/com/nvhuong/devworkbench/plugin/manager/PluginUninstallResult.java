package com.nvhuong.devworkbench.plugin.manager;

public record PluginUninstallResult(
        boolean removed,
        boolean restartRequired
) {
}
