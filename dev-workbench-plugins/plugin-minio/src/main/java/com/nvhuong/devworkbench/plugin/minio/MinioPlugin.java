package com.nvhuong.devworkbench.plugin.minio;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginContributionSupport;

public class MinioPlugin implements DevWorkbenchPlugin {
    @Override
    public void onLoad(PluginContext context) {
        context.registerI18nBundle(MinioI18n.BUNDLE);
        try {
            MinioMcpTools.register(context);
        } catch (LinkageError olderHost) {
            // Hosts without MCP tool support keep the toolbox.
        }
        PluginContributionSupport.registerToolbox(
                context,
                "minio",
                MinioI18n.t("title"),
                "icons/minio.svg",
                "toolbox.group.database",
                MinioI18n.t("group"),
                () -> new MinioPanel(context.storage()),
                MinioPlugin.class
        );
    }
}
