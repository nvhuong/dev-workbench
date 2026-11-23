package com.nvhuong.devworkbench.plugin.sql;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginContributionSupport;

public class SqlPlugin implements DevWorkbenchPlugin {
    @Override public void onLoad(PluginContext context) {
        context.registerI18nBundle(SqlI18n.BUNDLE);
        PluginContributionSupport.registerToolbox(context, "sql", SqlI18n.t("title"), "icons/sql.svg",
                "toolbox.group.database", SqlI18n.t("group"), () -> new SqlPanel(context.storage()), SqlPlugin.class);
        try {
            SqlMcpTools.register(context);
        } catch (LinkageError olderHost) {
            // Hosts without MCP tool support: the toolbox keeps working, the tools are just not exposed.
        }
    }
}
