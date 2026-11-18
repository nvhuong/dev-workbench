package com.nvhuong.devworkbench.plugin.clientcert;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginContributionSupport;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginSettingsContribution;
import com.nvhuong.devworkbench.plugin.api.service.ClientCertificatePluginService;

public class ClientCertificatePlugin implements DevWorkbenchPlugin {

    @Override
    public void onLoad(PluginContext context) {
        context.registerI18nBundle(ClientCertI18n.BUNDLE_NAME);
        ClientCertificatePluginServiceImpl certificateService = new ClientCertificatePluginServiceImpl();
        context.registerService(ClientCertificatePluginService.class, certificateService);
        PluginContributionSupport.registerSettingsContribution(
                context,
                "client-certificates",
                MessageKeys.CERT_TITLE,
                700,
                PluginSettingsContribution.CATEGORY_EXTENSIONS,
                ClientCertI18n.BUNDLE_NAME,
                settingsContext -> new ClientCertificateSettingsPanel(settingsContext.parentWindow(), certificateService),
                ClientCertificatePlugin.class
        );
    }
}
