package com.laker.postman.plugin.minio;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginContributionSupport;

public class MinioPlugin implements EasyPostmanPlugin {
    @Override
    public void onLoad(PluginContext context) {
        context.registerI18nBundle(MinioI18n.BUNDLE);
        PluginContributionSupport.registerToolbox(
                context,
                "minio",
                MinioI18n.t("title"),
                "icons/minio.svg",
                "toolbox.group.database",
                MinioI18n.t("group"),
                () -> new MinioPanel(),
                MinioPlugin.class
        );
    }
}
