package com.nvhuong.devworkbench.plugin.decompiler;

import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DecompilerPanelSecurityTest {

    /**
     * Archive entry names are attacker-controlled. Decompiling an entry such as
     * "../../escape-x/Evil.class" must not write outside the decompiler's temporary directory (zip slip).
     */
    @Test
    public void decompilingTraversalEntryNameDoesNotWriteOutsideTempDirectory() throws Exception {
        byte[] classBytes = sampleClassBytes();
        String escapeDir = "escape-" + UUID.randomUUID();
        // Each decompilation runs in tmp/cfr-decompileNNN/, so "../<dir>" resolves to tmp/<dir>.
        Path escapeTarget = Path.of(System.getProperty("java.io.tmpdir")).resolve(escapeDir);
        String hostileEntry = "../" + escapeDir + "/Sample.class";

        String code = decompile(classBytes, hostileEntry);

        try {
            assertFalse(Files.exists(escapeTarget),
                    "decompiler wrote outside its temp directory: " + escapeTarget);
            assertTrue(code.contains("class Sample"), "the entry should still be decompiled:\n" + code);
        } finally {
            if (Files.exists(escapeTarget)) {
                try (var files = Files.walk(escapeTarget)) {
                    files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
                }
            }
        }
    }

    @Test
    public void decompilesRegularEntryInPackage() throws Exception {
        String code = decompile(sampleClassBytes(), "com/nvhuong/devworkbench/plugin/decompiler/Sample.class");
        assertTrue(code.contains("class Sample"), code);
    }

    private static String decompile(byte[] classBytes, String entryName) throws Exception {
        DecompilerPanel[] panel = new DecompilerPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new DecompilerPanel());
        Method decompile = DecompilerPanel.class.getDeclaredMethod("decompileClass", byte[].class, String.class);
        decompile.setAccessible(true);
        return (String) decompile.invoke(panel[0], classBytes, entryName);
    }

    private static byte[] sampleClassBytes() throws Exception {
        try (InputStream input = Objects.requireNonNull(
                DecompilerPanelSecurityTest.class.getResourceAsStream("Sample.class"))) {
            return input.readAllBytes();
        }
    }
}

/** Small class decompiled by the tests above. */
class Sample {
    int answer() {
        return 42;
    }
}
