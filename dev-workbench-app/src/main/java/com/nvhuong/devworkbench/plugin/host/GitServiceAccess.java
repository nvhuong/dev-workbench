package com.nvhuong.devworkbench.plugin.host;

import com.nvhuong.devworkbench.plugin.api.service.GitPluginService;
import com.nvhuong.devworkbench.plugin.git.GitWorkspacePluginService;

public final class GitServiceAccess {

    private static final GitPluginService BUILT_IN_SERVICE = new GitWorkspacePluginService();

    private GitServiceAccess() {
    }

    public static boolean isServiceAvailable() {
        return true;
    }

    public static GitPluginService requireService() {
        return BUILT_IN_SERVICE;
    }
}
