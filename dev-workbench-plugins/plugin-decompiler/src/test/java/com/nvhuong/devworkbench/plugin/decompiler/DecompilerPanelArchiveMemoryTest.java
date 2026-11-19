package com.nvhuong.devworkbench.plugin.decompiler;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Opening an archive read every entry into memory, so a large (fat) JAR could exhaust the heap. Entries must be read
 * on demand, including the ones of nested JARs, while text and class viewing keeps working.
 */
public class DecompilerPanelArchiveMemoryTest {
    private static final int BIG_ENTRY_BYTES = 48 * 1024 * 1024;

    @Test(timeOut = 60_000)
    public void openingArchiveDoesNotLoadEveryEntryIntoMemory() throws Exception {
        File jar = File.createTempFile("decompiler-memory-", ".jar");
        jar.deleteOnExit();
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
            put(out, "readme.txt", "top level".getBytes(StandardCharsets.UTF_8));
            out.putNextEntry(new JarEntry("data/big.bin"));
            byte[] chunk = new byte[1024 * 1024];
            for (int i = 0; i < BIG_ENTRY_BYTES / chunk.length; i++) out.write(chunk); // compresses to ~50 KB
            out.closeEntry();
            put(out, "lib/inner.jar", innerJar());
        }

        DecompilerPanel[] panel = new DecompilerPanel[1];
        SwingUtilities.invokeAndWait(() -> panel[0] = new DecompilerPanel());
        Method load = DecompilerPanel.class.getDeclaredMethod("loadJarFile", File.class);
        load.setAccessible(true);
        load.invoke(panel[0], jar);

        @SuppressWarnings("unchecked")
        Map<String, byte[]> cache = (Map<String, byte[]>) field(panel[0], "classFileCache");
        long cached = cache.values().stream().mapToLong(bytes -> bytes.length).sum();
        assertTrue(cached < 1024 * 1024, "opening the archive cached " + cached + " bytes");

        RSyntaxTextArea code = (RSyntaxTextArea) field(panel[0], "codeArea");
        invokeOnEdt(panel[0], "showTextFile", "readme.txt");
        assertEquals(onEdtText(code), "top level");

        DefaultMutableTreeNode nestedNode = new DefaultMutableTreeNode("lib/inner.jar");
        Method loadNested = DecompilerPanel.class.getDeclaredMethod("loadNestedJar", String.class, DefaultMutableTreeNode.class);
        loadNested.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                loadNested.invoke(panel[0], "lib/inner.jar", nestedNode);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
        long deadline = System.currentTimeMillis() + 20_000;
        while (nestedNode.getChildCount() == 0 && System.currentTimeMillis() < deadline) Thread.sleep(50);
        assertTrue(nestedNode.getChildCount() > 0, "nested jar was not expanded");
        invokeOnEdt(panel[0], "showTextFile", "lib/inner.jar!/inner.txt");
        assertEquals(onEdtText(code), "inside nested");
        cached = cache.values().stream().mapToLong(bytes -> bytes.length).sum();
        assertTrue(cached < 1024 * 1024, "expanding the nested jar cached " + cached + " bytes");
    }

    private static byte[] innerJar() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream out = new JarOutputStream(bytes)) {
            put(out, "inner.txt", "inside nested".getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }

    private static void put(JarOutputStream out, String name, byte[] content) throws Exception {
        out.putNextEntry(new JarEntry(name));
        ((OutputStream) out).write(content);
        out.closeEntry();
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = DecompilerPanel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void invokeOnEdt(Object target, String name, String arg) throws Exception {
        Method method = DecompilerPanel.class.getDeclaredMethod(name, String.class);
        method.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                method.invoke(target, arg);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static String onEdtText(RSyntaxTextArea area) throws Exception {
        String[] text = new String[1];
        SwingUtilities.invokeAndWait(() -> text[0] = area.getText());
        return text[0];
    }
}
