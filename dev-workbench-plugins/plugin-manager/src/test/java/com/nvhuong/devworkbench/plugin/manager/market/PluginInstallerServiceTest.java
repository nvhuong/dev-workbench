package com.nvhuong.devworkbench.plugin.manager.market;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public class PluginInstallerServiceTest {
    private static final String TEMP_PREFIX = "dev-workbench-plugin-";

    @Test
    public void shouldDeleteTempFileWhenDownloadCannotConnect() throws Exception {
        Set<Path> before = tempPluginFiles();
        // Port 1 on loopback refuses connections immediately, simulating an offline catalog host.
        PluginCatalogEntry entry = new PluginCatalogEntry("plugin-offline", "Offline", "1.0.0", "",
                "http://127.0.0.1:1/plugin-offline-1.0.0.jar", "", "");

        expectThrows(Exception.class, () -> PluginInstallerService.installCatalogPlugin(entry));

        Set<Path> leaked = tempPluginFiles();
        leaked.removeAll(before);
        assertEquals(leaked, Set.of(), "A failed download must not leave temporary plugin files behind");
    }

    private static Set<Path> tempPluginFiles() throws IOException {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> files = Files.list(tempDir)) {
            return files.filter(path -> path.getFileName().toString().startsWith(TEMP_PREFIX))
                    .collect(Collectors.toSet());
        }
    }
}
