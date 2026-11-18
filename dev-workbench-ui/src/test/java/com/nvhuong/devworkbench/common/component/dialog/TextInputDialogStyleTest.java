package com.nvhuong.devworkbench.common.component.dialog;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TextInputDialogStyleTest {
    private static final Path ROOT = projectRoot();

    @Test
    public void textInputDialogShouldReuseSharedFormAndDialogControls() throws IOException {
        Path sourcePath = ROOT.resolve(
                "dev-workbench-ui/src/main/java/com/nvhuong/devworkbench/common/component/dialog/TextInputDialog.java");

        assertTrue(Files.exists(sourcePath), "Shared text input dialog should live in dev-workbench-ui");

        String source = Files.readString(sourcePath);
        assertTrue(source.contains("SettingsInputStyle.apply(nameField)"),
                "TextInputDialog should reuse the same input style as collection/env forms");
        assertTrue(source.contains("ModernButtonFactory.createButton"),
                "TextInputDialog should reuse shared dialog buttons");
        assertTrue(source.contains("ToolWindowSurfaceStyle.applyDialogFooter"),
                "TextInputDialog should use shared dialog footer styling");
        assertTrue(source.contains("CommonI18n.get(CommonMessageKeys.BUTTON_CANCEL)"),
                "TextInputDialog should use common labels instead of app-owned message bundles");
        assertTrue(source.contains("CommonI18n.get(CommonMessageKeys.LABEL_NAME)"),
                "TextInputDialog should centralize the shared name field label");
        assertFalse(source.contains("import com.nvhuong.devworkbench.util.MessageKeys"),
                "Shared UI components must not depend on app-owned MessageKeys");
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("pom.xml"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Unable to locate project root");
        }
        if (Files.exists(current.resolve("dev-workbench-ui/pom.xml"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.exists(parent.resolve("dev-workbench-ui/pom.xml"))) {
            return parent;
        }
        return current;
    }
}
