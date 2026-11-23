package com.nvhuong.devworkbench.plugin.decompiler;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;
import com.nvhuong.devworkbench.plugin.api.PluginContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/** MCP tools of the decompiler plugin: list the classes of an archive and decompile one class with CFR. */
final class DecompilerMcpTools {
    private static final Set<String> ARCHIVES = Set.of(".jar", ".war", ".ear", ".zip");
    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 5000;
    private static final long MAX_CLASS_BYTES = 16L * 1024 * 1024;

    private DecompilerMcpTools() {
    }

    static void register(PluginContext context) {
        for (McpToolContribution tool : tools()) {
            context.registerMcpTool(tool);
        }
    }

    static List<McpToolContribution> tools() {
        return List.of(
                new McpToolContribution("decompiler_list_classes", "List classes in a Java archive",
                        "Lists .class entries (and nested .jar files) of a local JAR/WAR/EAR/ZIP, optionally filtered by a case-insensitive substring.",
                        schema(Map.of(
                                "archivePath", string("Absolute path of a local .jar, .war, .ear or .zip file."),
                                "filter", Map.of("type", "string", "description", "Optional substring, e.g. com/acme/Order."),
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", MAX_LIMIT,
                                        "description", "Maximum entries to return (default 500).")),
                                List.of("archivePath")),
                        true, DecompilerMcpTools::listClasses),
                new McpToolContribution("decompiler_decompile_class", "Decompile a Java class",
                        "Decompiles one class with CFR and returns Java source. className is an entry path (com/acme/Order.class), a dotted name (com.acme.Order) or nested 'lib/x.jar!/com/acme/Order.class'. archivePath may also point to a single .class file.",
                        schema(Map.of(
                                "archivePath", string("Absolute path of a local .jar/.war/.ear/.zip or .class file."),
                                "className", Map.of("type", "string", "description", "Class to decompile (required for archives).")),
                                List.of("archivePath")),
                        true, DecompilerMcpTools::decompile));
    }

    static Map<String, Object> listClasses(Map<String, Object> arguments) throws IOException {
        Path archive = archive(arguments, false);
        String filter = optional(arguments, "filter");
        String needle = filter == null ? null : filter.toLowerCase(Locale.ROOT);
        int limit = Math.max(1, Math.min(MAX_LIMIT, integer(arguments, "limit", DEFAULT_LIMIT)));
        List<String> entries = new ArrayList<>();
        int matched = 0;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            for (ZipEntry entry : java.util.Collections.list(zip.entries())) {
                String name = entry.getName();
                boolean relevant = name.endsWith(".class") || name.endsWith(".jar");
                if (entry.isDirectory() || !relevant) continue;
                if (needle != null && !name.toLowerCase(Locale.ROOT).contains(needle)) continue;
                matched++;
                if (entries.size() < limit) entries.add(name);
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("archive", archive.toString());
        response.put("entries", entries);
        response.put("count", entries.size());
        response.put("matched", matched);
        response.put("truncated", matched > entries.size());
        return response;
    }

    static Map<String, Object> decompile(Map<String, Object> arguments) throws IOException {
        Path path = archive(arguments, true);
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        byte[] classBytes;
        String className;
        if (fileName.endsWith(".class")) {
            classBytes = Files.readAllBytes(path);
            className = path.getFileName().toString();
        } else {
            className = entryName(required(arguments, "className"));
            classBytes = readEntry(path, className);
        }
        String source = DecompilerPanel.decompileClass(classBytes, className);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("className", className);
        response.put("source", source);
        response.put("failed", source.startsWith("// Decompilation failed") || source.startsWith("// Failed to decompile"));
        return response;
    }

    /** com.acme.Order -> com/acme/Order.class; entry paths are kept. */
    static String entryName(String className) {
        String name = className.trim().replace('\\', '/');
        if (name.contains("!/") || name.endsWith(".class") || name.contains("/")) {
            return name.endsWith(".class") ? name : name + ".class";
        }
        return name.replace('.', '/') + ".class";
    }

    private static byte[] readEntry(Path archive, String entryName) throws IOException {
        int nested = entryName.indexOf("!/");
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            ZipEntry outer = zip.getEntry(nested < 0 ? entryName : entryName.substring(0, nested));
            if (outer == null) throw new IllegalArgumentException("Entry not found: " + entryName);
            try (InputStream input = zip.getInputStream(outer)) {
                if (nested < 0) return readLimited(input);
                String inner = entryName.substring(nested + 2);
                try (ZipInputStream nestedJar = new ZipInputStream(new ByteArrayInputStream(readLimited(input)))) {
                    for (ZipEntry entry; (entry = nestedJar.getNextEntry()) != null; ) {
                        if (entry.getName().equals(inner)) return readLimited(nestedJar);
                    }
                }
                throw new IllegalArgumentException("Entry not found in nested archive: " + entryName);
            }
        }
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes((int) MAX_CLASS_BYTES + 1);
        if (bytes.length > MAX_CLASS_BYTES) throw new IllegalArgumentException("Entry is larger than 16 MB");
        return bytes;
    }

    private static Path archive(Map<String, Object> arguments, boolean allowClassFile) {
        Path path = Path.of(required(arguments, "archivePath")).toAbsolutePath().normalize();
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean archive = ARCHIVES.stream().anyMatch(name::endsWith);
        if (!archive && !(allowClassFile && name.endsWith(".class"))) {
            throw new IllegalArgumentException("Only .jar, .war, .ear, .zip" + (allowClassFile ? " or .class" : "") + " files are supported");
        }
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException("File not found: " + path);
        return path;
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description, "minLength", 1);
    }

    private static String required(Map<String, Object> arguments, String name) {
        String value = optional(arguments, name);
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String optional(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    private static int integer(Map<String, Object> arguments, String name, int defaultValue) {
        Object value = arguments == null ? null : arguments.get(name);
        if (value == null) return defaultValue;
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value).trim());
    }
}
