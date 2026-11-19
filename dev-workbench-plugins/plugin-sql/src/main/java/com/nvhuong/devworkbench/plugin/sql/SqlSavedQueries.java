package com.nvhuong.devworkbench.plugin.sql;

import com.nvhuong.devworkbench.plugin.api.PluginStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Named SQL scripts (one or many statements) the user saved, sorted by name. */
final class SqlSavedQueries {
    static final String FILE = "sql-saved-queries.tsv";

    record SavedQuery(String name, String sql, long updatedAt) { }

    private final PluginStorage storage;
    private final List<SavedQuery> queries = new ArrayList<>();

    SqlSavedQueries(PluginStorage storage) {
        this.storage = storage == null ? PluginStorage.noop() : storage;
        load();
    }

    synchronized List<SavedQuery> list() {
        return List.copyOf(queries);
    }

    synchronized List<SavedQuery> search(String text) {
        if (text == null || text.isBlank()) return List.copyOf(queries);
        String needle = text.trim().toLowerCase(Locale.ROOT);
        return queries.stream()
                .filter(query -> query.name().toLowerCase(Locale.ROOT).contains(needle)
                        || query.sql().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    synchronized Optional<SavedQuery> find(String name) {
        return queries.stream().filter(query -> query.name().equalsIgnoreCase(name == null ? "" : name.trim()))
                .findFirst();
    }

    /** Saves {@code sql} under {@code name}, replacing a query of the same name (case-insensitive). */
    synchronized SavedQuery save(String name, String sql, long now) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("name");
        queries.removeIf(query -> query.name().equalsIgnoreCase(trimmed));
        SavedQuery saved = new SavedQuery(trimmed, sql == null ? "" : sql, now);
        queries.add(saved);
        sort();
        persist();
        return saved;
    }

    /** Renames a query; returns false if {@code newName} is blank or already used by another query. */
    synchronized boolean rename(String oldName, String newName, long now) {
        String target = newName == null ? "" : newName.trim();
        Optional<SavedQuery> existing = find(oldName);
        if (target.isEmpty() || existing.isEmpty()) return false;
        if (!target.equalsIgnoreCase(existing.get().name()) && find(target).isPresent()) return false;
        queries.remove(existing.get());
        queries.add(new SavedQuery(target, existing.get().sql(), now));
        sort();
        persist();
        return true;
    }

    synchronized void delete(String name) {
        if (queries.removeIf(query -> query.name().equalsIgnoreCase(name == null ? "" : name.trim()))) persist();
    }

    private void sort() {
        queries.sort(Comparator.comparing(SavedQuery::name, String.CASE_INSENSITIVE_ORDER));
    }

    private void load() {
        try {
            for (String line : storage.readString(FILE).orElse("").lines().toList()) {
                List<String> fields = SqlRecords.decodeLine(line, 3);
                if (fields == null || fields.get(0).isBlank()) continue;
                queries.add(new SavedQuery(fields.get(0), fields.get(1), SqlRecords.parseLong(fields.get(2))));
            }
        } catch (IOException ignored) {
            // an unreadable file must not block the SQL tool
        }
        sort();
    }

    private void persist() {
        StringBuilder content = new StringBuilder();
        for (SavedQuery query : queries) {
            content.append(SqlRecords.encodeLine(query.name(), query.sql(), String.valueOf(query.updatedAt())))
                    .append('\n');
        }
        try {
            storage.writeString(FILE, content.toString());
        } catch (IOException ignored) {
            // keep the in-memory list; the next successful write persists it
        }
    }
}
