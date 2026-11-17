package com.laker.postman.plugin.remote;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginContributionSupport;

public class RemotePlugin implements EasyPostmanPlugin {
    @Override public void onLoad(PluginContext context) {
        context.registerI18nBundle(RemoteI18n.BUNDLE);
        PluginContributionSupport.registerToolbox(context, "remote", RemoteI18n.t("title"), "icons/remote.svg",
                "toolbox.group.dev", RemoteI18n.t("group"), () -> new RemotePanel(context.storage()), RemotePlugin.class);
    }
}
