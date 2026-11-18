package com.nvhuong.devworkbench.plugin.api;

import java.awt.Window;

/**
 * Context available when a plugin menu action runs.
 */
public record PluginMenuActionContext(Window parentWindow) {
}
