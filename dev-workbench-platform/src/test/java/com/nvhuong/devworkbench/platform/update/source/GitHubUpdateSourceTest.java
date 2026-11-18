package com.nvhuong.devworkbench.platform.update.source;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class GitHubUpdateSourceTest {

    @Test
    public void shouldBuildReleaseByTagApi() {
        String tagApiUrl = new GitHubUpdateSource().buildReleaseByTagApiUrl("v6.3.1");

        assertEquals(tagApiUrl,
                "https://api.github.com/repos/nvhuong/dev-workbench/releases/tags/v6.3.1");
    }
}
