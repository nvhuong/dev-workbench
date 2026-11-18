package com.nvhuong.devworkbench.platform.update.source;

import com.nvhuong.devworkbench.platform.update.UpdateSettingsProvider;

/** Selects the GitHub release source used by update checks. */
public class UpdateSourceSelector {
    private final UpdateSource githubSource = new GitHubUpdateSource();

    public UpdateSourceSelector() {
        this(UpdateSettingsProvider.defaults());
    }

    public UpdateSourceSelector(UpdateSettingsProvider ignoredSettingsProvider) {
    }

    public UpdateSource selectBestSource() {
        return githubSource;
    }

    public UpdateSource getFallbackSource(UpdateSource primarySource) {
        return null;
    }

    public UpdateSource getFallbackSource(String primarySourceName) {
        return null;
    }

    public void clearCache() {
    }
}
