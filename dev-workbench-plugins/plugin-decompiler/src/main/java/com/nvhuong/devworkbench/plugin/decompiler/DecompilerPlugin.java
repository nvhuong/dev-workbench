package com.nvhuong.devworkbench.plugin.decompiler;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginContributionSupport;
import com.nvhuong.devworkbench.plugin.api.PluginContext;

public class DecompilerPlugin implements DevWorkbenchPlugin {

    @Override
    public void onLoad(PluginContext context) {
        context.registerI18nBundle(DecompilerI18n.BUNDLE_NAME);
        PluginContributionSupport.registerToolbox(
                context,
                "decompiler",
                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER),
                "icons/decompile.svg",
                MessageKeys.TOOLBOX_GROUP_DEV,
                DecompilerI18n.t(MessageKeys.TOOLBOX_GROUP_DEV),
                DecompilerPanel::new,
                DecompilerPlugin.class
        );
    }
}
