package com.nvhuong.devworkbench.plugin.sql;

import com.nvhuong.devworkbench.plugin.api.PluginStorage;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link PluginStorage} for tests. */
final class MemoryStorage implements PluginStorage {
    final Map<String, String> files = new HashMap<>();

    @Override public Optional<String> readString(String relativePath) { return Optional.ofNullable(files.get(relativePath)); }
    @Override public void writeString(String relativePath, String content) { files.put(relativePath, content); }
    @Override public void delete(String relativePath) { files.remove(relativePath); }
    @Override public Path dataDirectory() { return Path.of(System.getProperty("java.io.tmpdir")); }
}
