package com.nvhuong.devworkbench.platform.update.version;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class VersionComparatorTest {

    /** Pre-release identifiers compare numerically (SemVer 11.4); build metadata is ignored (SemVer 10). */
    @Test public void comparesPreReleaseIdentifiersNumericallyAndIgnoresBuildMetadata() {
        assertTrue(VersionComparator.compare("1.2.0-rc.10", "1.2.0-rc.9") > 0);
        assertTrue(VersionComparator.compare("1.2.0-rc10", "1.2.0-rc9") > 0);
        assertTrue(VersionComparator.compare("1.2.0-alpha", "1.2.0-alpha.1") < 0);
        assertTrue(VersionComparator.compare("1.2.0-1", "1.2.0-alpha") < 0);
        assertTrue(VersionComparator.compare("1.2.0-rc.1", "1.2.0") < 0);
        assertEquals(VersionComparator.compare("1.2.0+build.5", "1.2.0"), 0);
        assertTrue(VersionComparator.compare("1.2.0-rc.2+build.1", "1.2.0-rc.1") > 0);
        assertEquals(VersionComparator.compare("V1.2.0", "1.2.0"), 0);
        assertTrue(VersionComparator.isNewer("1.10.0", "1.9.9"));
    }
}
