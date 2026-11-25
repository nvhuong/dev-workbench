package com.nvhuong.devworkbench.plugin.remote;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginContributionSupport;

public class RemotePlugin implements DevWorkbenchPlugin {
    @Override public void onLoad(PluginContext context) {
        context.registerI18nBundle(RemoteI18n.BUNDLE);
        PluginContributionSupport.registerToolbox(context, "remote", RemoteI18n.t("title"), "icons/remote.svg",
                "toolbox.group.dev", RemoteI18n.t("group"), () -> new RemotePanel(context.storage()), RemotePlugin.class);
        try {
            RemoteMcpTools.register(context);
        } catch (LinkageError olderHost) {
            // Hosts before the MCP tool API simply do not get the SSH tools.
        }
    }
}
