package com.nvhuong.devworkbench.plugin.manager.market;

import com.nvhuong.devworkbench.plugin.runtime.PluginRuntime;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

public class PluginCatalogServiceTest {

    private Path dataDir;

    @BeforeMethod
    public void setUp() throws IOException {
        dataDir = Files.createTempDirectory("plugin-catalog-test");
        System.setProperty("devWorkbench.data.dir", dataDir.toString());
        PluginRuntime.resetForTests();
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty("devWorkbench.data.dir");
        PluginRuntime.resetForTests();
    }

    @Test
    public void shouldRejectLocalCatalogLocations() {
        assertThrows(IllegalArgumentException.class,
                () -> PluginCatalogService.normalizeCatalogLocation("/tmp/catalog.json"));
        assertThrows(IllegalArgumentException.class,
                () -> PluginCatalogService.normalizeCatalogLocation("file:///tmp/catalog.json"));
    }

    @Test
    public void shouldExposeOfficialCatalogUrls() {
        assertEquals(
                PluginCatalogService.getOfficialCatalogUrl("github"),
                "https://raw.githubusercontent.com/nvhuong/dev-workbench/main/plugin-catalog/catalog-github.json"
        );
        assertEquals(
                PluginCatalogService.getOfficialCatalogUrl("auto"),
                "https://raw.githubusercontent.com/nvhuong/dev-workbench/main/plugin-catalog/catalog-github.json"
        );
        assertEquals(
                PluginCatalogService.detectOfficialCatalogSource(PluginCatalogService.getOfficialCatalogUrl("github")),
                "github"
        );
        assertEquals(PluginCatalogService.detectOfficialCatalogSource("https://gitee.com/old/catalog.json"), "");
    }

    @Test
    public void shouldLoadBundledOfficialCatalog() throws Exception {
        List<PluginCatalogEntry> githubEntries = PluginCatalogService.loadBundledOfficialCatalog("github");
        Set<String> requiredPluginIds = Set.of(
                "plugin-capture",
                "plugin-redis",
                "plugin-kafka",
                "plugin-decompiler",
                "plugin-client-cert",
                "plugin-remote",
                "plugin-sql",
                "plugin-minio"
        );
        Set<String> githubIds = githubEntries.stream()
                .map(PluginCatalogEntry::id)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(githubIds.containsAll(requiredPluginIds));
        assertTrue(githubEntries.stream().noneMatch(entry -> "plugin-git".equals(entry.id())));

        PluginCatalogEntry githubKafka = githubEntries.stream()
                .filter(entry -> "plugin-kafka".equals(entry.id()))
                .findFirst()
                .orElse(null);
        assertNotNull(githubKafka);
        assertTrue(githubKafka.installUrl().startsWith("https://github.com/"));
        assertTrue(githubKafka.installUrl().contains("/releases/download/"));
        assertNotNull(githubKafka.version());
        assertFalse(githubKafka.version().isBlank());
        assertFalse(githubKafka.minPlatformVersion().isBlank());
    }

    @Test
    public void shouldKeepPublicAndBundledOfficialCatalogsInSync() throws Exception {
        assertCatalogCopiesInSync("catalog-github.json");
    }

    @Test
    public void shouldSelectHighestCompatibleReleaseFromNestedCatalog() throws Exception {
        String json = """
                {
                  "plugins": [
                    {
                      "id": "plugin-kafka",
                      "name": "Kafka Plugin",
                      "description": "Kafka toolbox panel",
                      "homepage": "https://example.com",
                      "releases": [
                        {
                          "version": "5.3.18",
                          "downloadUrl": "https://example.com/plugin-kafka-5.3.18.jar",
                          "sha256": "sha-518",
                          "minAppVersion": "5.3.18",
                          "minPlatformVersion": "1.0.0",
                          "maxPlatformVersion": "1.0.0"
                        },
                        {
                          "version": "5.3.17",
                          "downloadUrl": "https://example.com/plugin-kafka-5.3.17.jar",
                          "sha256": "sha-517",
                          "minPlatformVersion": "1.0.0",
                          "maxPlatformVersion": "1.0.0"
                        }
                      ]
                    }
                  ]
                }
                """;

        List<PluginCatalogEntry> entries = PluginCatalogService.parseCatalogForTests(
                json,
                "https://example.com/catalog-github.json",
                "5.3.18",
                "1.0.0"
        );

        assertEquals(entries.size(), 1);
        assertEquals(entries.get(0).id(), "plugin-kafka");
        assertEquals(entries.get(0).version(), "5.3.18");
        assertEquals(entries.get(0).installUrl(), "https://example.com/plugin-kafka-5.3.18.jar");
    }

    private static void assertCatalogCopiesInSync(String fileName) throws IOException {
        Path root = findRepositoryRoot();
        Path publicCatalog = root.resolve("plugin-catalog").resolve(fileName);
        Path bundledCatalog = root.resolve("dev-workbench-plugins/plugin-manager/src/main/resources/plugin-catalog")
                .resolve(fileName);

        assertEquals(Files.readString(bundledCatalog), Files.readString(publicCatalog));
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("plugin-catalog/catalog-github.json"))
                    && Files.exists(current.resolve("dev-workbench-plugins/plugin-manager/pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root not found");
    }
}
