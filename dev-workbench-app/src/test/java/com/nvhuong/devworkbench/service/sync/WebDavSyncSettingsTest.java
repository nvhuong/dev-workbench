package com.nvhuong.devworkbench.service.sync;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class WebDavSyncSettingsTest {

    @Test
    public void legacyConstructorShouldKeepAutoUploadDisabled() {
        WebDavSyncSettings settings = new WebDavSyncSettings(
                true,
                "https://example.com/dav",
                "DevWorkbench",
                "alice",
                "secret"
        );

        assertFalse(settings.autoUploadEnabled());
        assertEquals(settings.autoUploadIntervalMinutes(), WebDavSyncSettings.DEFAULT_AUTO_UPLOAD_INTERVAL_MINUTES);
    }

    @Test
    public void autoUploadIntervalShouldStayWithinSupportedBounds() {
        WebDavSyncSettings tooShort = new WebDavSyncSettings(
                true, "https://example.com/dav", "DevWorkbench", "", "", true, 1
        );
        WebDavSyncSettings tooLong = new WebDavSyncSettings(
                true, "https://example.com/dav", "DevWorkbench", "", "", true, Integer.MAX_VALUE
        );

        assertEquals(tooShort.autoUploadIntervalMinutes(), WebDavSyncSettings.MIN_AUTO_UPLOAD_INTERVAL_MINUTES);
        assertEquals(tooLong.autoUploadIntervalMinutes(), WebDavSyncSettings.MAX_AUTO_UPLOAD_INTERVAL_MINUTES);
    }
}
