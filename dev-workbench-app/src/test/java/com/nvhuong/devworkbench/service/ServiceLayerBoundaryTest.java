package com.nvhuong.devworkbench.service;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;

public class ServiceLayerBoundaryTest {

    @Test
    public void serviceLayerShouldNotImportRequestEditorPanelsForConstants() throws IOException {
        Path serviceDir = moduleDir().resolve("src/main/java/com/nvhuong/devworkbench/service");
        try (var paths = Files.walk(serviceDir)) {
            for (Path sourceFile : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(sourceFile);
                assertFalse(source.contains("panel.collections.editor.request.sub.AuthTabPanel"),
                        sourceFile + " must use domain auth constants, not Swing panel constants");
                assertFalse(source.contains("panel.collections.editor.request.sub.RequestBodyPanel"),
                        sourceFile + " must use domain body constants, not Swing panel constants");
                if (sourceFile.endsWith("FunctionalPersistenceService.java")) {
                    assertFalse(source.contains("RunnerRowData"),
                            sourceFile + " must persist neutral functional config rows, not UI/runtime runner rows");
                    assertFalse(source.contains("PreparedRequest"),
                            sourceFile + " must not build execution requests while loading persisted config");
                }
            }
        }
    }

    @Test
    public void exitUiCoordinationShouldNotLiveInServicePackage() {
        assertFalse(Files.exists(moduleDir().resolve("src/main/java/com/nvhuong/devworkbench/service/ExitService.java")),
                "exit UI coordination should live in panel/lifecycle layer");
    }

    @Test
    public void workspaceTransferUiCoordinationShouldNotLiveInServicePackage() {
        assertFalse(Files.exists(moduleDir().resolve(
                        "src/main/java/com/nvhuong/devworkbench/service/workspace/WorkspaceTransferHelper.java")),
                "workspace transfer UI coordination should live in panel/workspace layer");
    }

    @Test
    public void httpExecutionLayerShouldNotWriteNetworkLogsThroughRequestEditorUi() throws IOException {
        for (String relativePath : new String[]{
                "dev-workbench-http-runtime/src/main/java/com/nvhuong/devworkbench/http/runtime/redirect/HttpRedirectExecutor.java",
                "dev-workbench-http-runtime/src/main/java/com/nvhuong/devworkbench/http/runtime/okhttp/OkHttpExchangeEventListener.java"
        }) {
            Path sourceFile = projectRootDir().resolve(relativePath);
            String source = Files.readString(sourceFile);
            assertFalse(source.contains("com.nvhuong.devworkbench.common.UiSingletonFactory"),
                    sourceFile + " must publish network log events instead of looking up UI singletons");
            assertFalse(source.contains("com.nvhuong.devworkbench.panel.collections.editor.RequestEditorPanel"),
                    sourceFile + " must not depend on the request editor UI");
            assertFalse(source.contains("com.nvhuong.devworkbench.panel.collections.editor.request.sub.NetworkLogPanel"),
                    sourceFile + " must not depend on the Swing network log panel");
            assertFalse(source.contains("com.nvhuong.devworkbench.panel.collections.editor.request.sub.NetworkLogStage"),
                    sourceFile + " must not use UI log stage styling from the service layer");
        }
    }

    @Test
    public void scriptExecutionPipelineShouldNotWriteThroughSwingConsole() throws IOException {
        Path sourceFile = moduleDir().resolve("src/main/java/com/nvhuong/devworkbench/service/js/ScriptExecutionPipeline.java");
        String source = Files.readString(sourceFile);
        assertFalse(source.contains("com.nvhuong.devworkbench.panel.sidebar.ConsolePanel"),
                sourceFile + " must receive script output through an injected callback");
        assertFalse(source.contains("ConsolePanel.appendLog"),
                sourceFile + " must not write directly to the Swing console");
        assertFalse(source.contains("ConsolePanel.LogType"),
                sourceFile + " must not map service output to Swing console types");
    }

    private Path moduleDir() {
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(moduleDir.resolve("src/main/java"))) {
            moduleDir = moduleDir.resolve("dev-workbench-app");
        }
        return moduleDir;
    }

    private Path projectRootDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        if (Files.exists(currentDir.resolve("dev-workbench-app"))) {
            return currentDir;
        }
        return currentDir.getParent();
    }
}
