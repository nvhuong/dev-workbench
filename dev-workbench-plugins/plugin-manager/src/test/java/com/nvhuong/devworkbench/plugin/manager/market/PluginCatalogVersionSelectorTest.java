package com.nvhuong.devworkbench.plugin.manager.market;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class PluginCatalogVersionSelectorTest {

    /** rc.10 is newer than rc.9; comparing qualifiers as strings picked rc.9 as the latest catalog entry. */
    @Test public void picksHighestPreReleaseNumerically() {
        List<PluginCatalogEntry> selected = PluginCatalogVersionSelector.selectForHost(
                List.of(entry("1.2.0-rc.9", null), entry("1.2.0-rc.10", null)), "1.0.0", "1.0.0");
        assertEquals(selected.get(0).version(), "1.2.0-rc.10");
    }

    /** Build metadata does not make a host older than the max version it is compatible with. */
    @Test public void buildMetadataDoesNotBreakCompatibilityRange() {
        List<PluginCatalogEntry> selected = PluginCatalogVersionSelector.selectForHost(
                List.of(entry("2.0.0", "1.5.0"), entry("1.0.0", null)), "1.5.0+20261119", "1.0.0");
        assertEquals(selected.get(0).version(), "2.0.0");
    }

    private static PluginCatalogEntry entry(String version, String maxAppVersion) {
        return new PluginCatalogEntry("demo", "Demo", version, null, null, null, null, null,
                null, maxAppVersion, null, null);
    }
}
