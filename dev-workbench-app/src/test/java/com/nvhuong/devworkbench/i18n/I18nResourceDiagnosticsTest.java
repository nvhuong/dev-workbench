package com.nvhuong.devworkbench.i18n;

import com.nvhuong.devworkbench.util.I18nBundleDiagnostics;
import com.nvhuong.devworkbench.util.I18nDiagnosticsReport;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertFalse;

public class I18nResourceDiagnosticsTest {

    @Test
    public void appUiCommonAndOfficialPluginBundlesShouldStayAligned() {
        List<BundleLocation> bundles = List.of(
                new BundleLocation(Path.of("src/main/resources"), "messages"),
                new BundleLocation(Path.of("../dev-workbench-foundation/src/main/resources"), "common-messages"),
                new BundleLocation(Path.of("../dev-workbench-ui/src/main/resources"), "ui-messages"),
                new BundleLocation(Path.of("../dev-workbench-plugins/plugin-redis/src/main/resources"), "redis-messages"),
                new BundleLocation(Path.of("../dev-workbench-plugins/plugin-kafka/src/main/resources"), "kafka-messages"),
                new BundleLocation(Path.of("../dev-workbench-plugins/plugin-capture/src/main/resources"), "capture-messages"),
                new BundleLocation(Path.of("../dev-workbench-plugins/plugin-client-cert/src/main/resources"), "client-cert-messages"),
                new BundleLocation(Path.of("../dev-workbench-plugins/plugin-decompiler/src/main/resources"), "decompiler-messages")
        );

        StringBuilder issues = new StringBuilder();
        for (BundleLocation bundle : bundles) {
            I18nDiagnosticsReport report = I18nBundleDiagnostics.scanResourceDirectory(
                    bundle.resourcesRoot(),
                    List.of(bundle.bundleName())
            );
            if (report.hasIssues()) {
                issues.append(bundle.bundleName())
                        .append(" missing=")
                        .append(report.missingKeys())
                        .append(" duplicates=")
                        .append(report.duplicateKeys())
                        .append(System.lineSeparator());
            }
        }

        assertFalse(issues.length() > 0, issues.toString());
    }

    private record BundleLocation(Path resourcesRoot, String bundleName) {
    }
}
