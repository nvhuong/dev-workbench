package com.nvhuong.devworkbench.service.collections;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;

public class CollectionTreeQueryServiceBoundaryTest {

    @Test
    public void shouldNotDependOnUiSingletonsOrDialogs() throws IOException {
        assertNoUiDependencies(readMainSource("com/nvhuong/devworkbench/service/collections/CollectionTreeQueryService.java"));
    }

    @Test
    public void openedRequestsStoreShouldOnlyHandlePersistence() throws IOException {
        assertNoUiDependencies(readMainSource("com/nvhuong/devworkbench/service/collections/OpenedRequestTabsStore.java"));
    }

    @Test
    public void collectionServicePackageShouldNotImportPanelLayer() throws IOException {
        Path collectionsPackage = mainSourcePath("com/nvhuong/devworkbench/service/collections");
        try (var paths = Files.walk(collectionsPackage)) {
            for (Path sourceFile : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertNoUiDependencies(Files.readString(sourceFile));
            }
        }
    }

    @Test
    public void uiTabCoordinationShouldNotUseServiceNames() {
        assertFalse(Files.exists(mainSourcePath("com/nvhuong/devworkbench/service/collections/RequestsTabsService.java")),
                "request tab UI coordination must live in panel layer");
        assertFalse(Files.exists(mainSourcePath("com/nvhuong/devworkbench/service/collections/OpenedRequestsService.java")),
                "opened request tab UI coordination must live in panel layer");
        assertFalse(Files.exists(mainSourcePath("com/nvhuong/devworkbench/service/collections/DefaultTreeNodeRepository.java")),
                "active collection tree lookup should use the registered root repository name");
    }

    @Test
    public void swingCollectionAdaptersShouldStayInPanelLayer() {
        for (String fileName : java.util.List.of(
                "SwingCollectionTreeDocumentMapper.java",
                "SwingCollectionTreeQueries.java",
                "SwingCollectionTreePersistence.java",
                "SwingCollectionRequestSaveCoordinator.java",
                "SwingCollectionRequestMutation.java",
                "SwingSavedResponseTreeMutation.java"
        )) {
            assertFalse(Files.exists(mainSourcePath("com/nvhuong/devworkbench/service/collections/" + fileName)),
                    fileName + " is a Swing adapter and must live under panel.collections.tree.adapter");
        }
    }

    private String readMainSource(String relativePath) throws IOException {
        return Files.readString(mainSourcePath(relativePath));
    }

    private Path mainSourcePath(String relativePath) {
        return moduleDir().resolve("src/main/java").resolve(relativePath);
    }

    private Path moduleDir() {
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(moduleDir.resolve("src/main/java"))) {
            moduleDir = moduleDir.resolve("dev-workbench-app");
        }
        return moduleDir;
    }

    private void assertNoUiDependencies(String source) {
        assertFalse(source.contains("UiSingletonFactory"), "service layer must not create UI singletons");
        assertFalse(source.contains("JDialog"), "service layer must not own Swing dialogs");
        assertFalse(source.contains("JOptionPane"), "service layer must not own Swing dialogs");
        assertFalse(source.contains("MainFrame"), "service layer must not depend on app windows");
        assertFalse(source.contains("RequestEditSubPanel"), "service layer must not open request editor tabs");
        assertFalse(source.contains("com.nvhuong.devworkbench.panel"), "service layer must not import panel classes");
    }
}
