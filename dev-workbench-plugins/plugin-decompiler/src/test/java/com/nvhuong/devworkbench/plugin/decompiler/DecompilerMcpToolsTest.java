package com.nvhuong.devworkbench.plugin.decompiler;

import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class DecompilerMcpToolsTest {
    private static final String ENTRY = "com/nvhuong/devworkbench/plugin/decompiler/DecompilerI18n.class";

    private static byte[] classBytes() throws Exception {
        try (InputStream input = DecompilerMcpToolsTest.class.getClassLoader().getResourceAsStream(ENTRY)) {
            return input.readAllBytes();
        }
    }

    private static byte[] jar(String entry, byte[] content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream out = new JarOutputStream(bytes)) {
            out.putNextEntry(new JarEntry(entry));
            out.write(content);
            out.closeEntry();
        }
        return bytes.toByteArray();
    }

    @Test
    public void listsAndDecompilesClassesIncludingNestedJars() throws Exception {
        File archive = File.createTempFile("mcp-decompile-", ".jar");
        archive.deleteOnExit();
        ByteArrayOutputStream outer = new ByteArrayOutputStream();
        try (JarOutputStream out = new JarOutputStream(outer)) {
            out.putNextEntry(new JarEntry(ENTRY));
            out.write(classBytes());
            out.closeEntry();
            out.putNextEntry(new JarEntry("BOOT-INF/lib/inner.jar"));
            out.write(jar(ENTRY, classBytes()));
            out.closeEntry();
            out.putNextEntry(new JarEntry("readme.txt"));
            out.write("x".getBytes());
            out.closeEntry();
        }
        Files.write(archive.toPath(), outer.toByteArray());

        Map<String, Object> listed = DecompilerMcpTools.listClasses(Map.of("archivePath", archive.getPath()));
        assertEquals(listed.get("entries"), List.of(ENTRY, "BOOT-INF/lib/inner.jar"));

        Map<String, Object> direct = DecompilerMcpTools.decompile(Map.of("archivePath", archive.getPath(),
                "className", "com.nvhuong.devworkbench.plugin.decompiler.DecompilerI18n"));
        assertEquals(direct.get("failed"), false, String.valueOf(direct.get("source")));
        assertTrue(String.valueOf(direct.get("source")).contains("class DecompilerI18n"), String.valueOf(direct.get("source")));

        Map<String, Object> nested = DecompilerMcpTools.decompile(Map.of("archivePath", archive.getPath(),
                "className", "BOOT-INF/lib/inner.jar!/" + ENTRY));
        assertTrue(String.valueOf(nested.get("source")).contains("class DecompilerI18n"), String.valueOf(nested.get("source")));
    }

    @Test
    public void rejectsNonArchivesAndMissingEntries() throws Exception {
        File text = File.createTempFile("mcp-decompile-", ".txt");
        text.deleteOnExit();
        expectThrows(IllegalArgumentException.class, () -> DecompilerMcpTools.listClasses(Map.of("archivePath", text.getPath())));
        File archive = File.createTempFile("mcp-decompile-", ".jar");
        archive.deleteOnExit();
        Files.write(archive.toPath(), jar("a/B.class", new byte[]{1, 2}));
        expectThrows(IllegalArgumentException.class, () -> DecompilerMcpTools.decompile(Map.of("archivePath", archive.getPath(), "className", "a.Missing")));
        assertFalse(DecompilerMcpTools.tools().stream().anyMatch(tool -> !tool.readOnly()));
    }
}
