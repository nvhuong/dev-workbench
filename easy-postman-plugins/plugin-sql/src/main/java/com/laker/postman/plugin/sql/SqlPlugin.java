package com.laker.postman.plugin.sql;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.PluginContributionSupport;

public class SqlPlugin implements EasyPostmanPlugin {
    @Override public void onLoad(PluginContext context) {
        context.registerI18nBundle(SqlI18n.BUNDLE);
        PluginContributionSupport.registerToolbox(context, "sql", SqlI18n.t("title"), "icons/sql.svg",
                "toolbox.group.database", SqlI18n.t("group"), () -> new SqlPanel(context.storage()), SqlPlugin.class);
    }
}
