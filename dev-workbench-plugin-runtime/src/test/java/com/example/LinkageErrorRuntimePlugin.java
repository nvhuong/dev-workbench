package com.example;

import com.nvhuong.devworkbench.plugin.api.DevWorkbenchPlugin;
import com.nvhuong.devworkbench.plugin.api.PluginContext;

public class LinkageErrorRuntimePlugin implements DevWorkbenchPlugin {

    @Override
    public void onLoad(PluginContext context) {
        throw new NoClassDefFoundError("com/nvhuong/devworkbench/plugin/bridge/ClientCertificatePluginService");
    }
}
