package com.nvhuong.devworkbench.plugin.sql;

import com.nvhuong.devworkbench.plugin.api.PluginStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Persistent history of executed SQL statements, newest first. */
final class SqlHistory {
    static final String FILE = "sql-history.tsv";
    static final int MAX_ENTRIES = 500;

    record Entry(long executedAt, String profile, String sql, boolean success, long durationMillis, String summary) { }

    private final PluginStorage storage;
    private final List<Entry> entries = new ArrayList<>();

    SqlHistory(PluginStorage storage) {
        this.storage = storage == null ? PluginStorage.noop() : storage;
        load();
    }

    synchronized List<Entry> entries() {
        return List.copyOf(entries);
    }

    /** Entries whose SQL or profile contains {@code text} (case-insensitive); all entries for a blank filter. */
    synchronized List<Entry> search(String text) {
        if (text == null || text.isBlank()) return List.copyOf(entries);
        String needle = text.trim().toLowerCase(Locale.ROOT);
        return entries.stream()
                .filter(entry -> entry.sql().toLowerCase(Locale.ROOT).contains(needle)
                        || entry.profile().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    /** Records a run; re-running the newest statement replaces it instead of filling the history with copies. */
    synchronized void add(Entry entry) {
        if (entry == null || entry.sql() == null || entry.sql().isBlank()) return;
        Entry normalized = new Entry(entry.executedAt(), entry.profile() == null ? "" : entry.profile(),
                entry.sql().strip(), entry.success(), entry.durationMillis(),
                entry.summary() == null ? "" : entry.summary());
        if (!entries.isEmpty() && entries.get(0).sql().equals(normalized.sql())
                && entries.get(0).profile().equals(normalized.profile())) {
            entries.remove(0);
        }
        entries.add(0, normalized);
        while (entries.size() > MAX_ENTRIES) entries.remove(entries.size() - 1);
        save();
    }

    synchronized void remove(Entry entry) {
        if (entries.remove(entry)) save();
    }

    synchronized void clear() {
        entries.clear();
        save();
    }

    private void load() {
        try {
            for (String line : storage.readString(FILE).orElse("").lines().toList()) {
                List<String> fields = SqlRecords.decodeLine(line, 6);
                if (fields == null || fields.get(2).isBlank()) continue;
                entries.add(new Entry(SqlRecords.parseLong(fields.get(0)), fields.get(1), fields.get(2),
                        Boolean.parseBoolean(fields.get(3)), SqlRecords.parseLong(fields.get(4)), fields.get(5)));
                if (entries.size() >= MAX_ENTRIES) break;
            }
        } catch (IOException ignored) {
            // an unreadable history must not block the SQL tool
        }
    }

    private void save() {
        StringBuilder content = new StringBuilder();
        for (Entry entry : entries) {
            content.append(SqlRecords.encodeLine(String.valueOf(entry.executedAt()), entry.profile(), entry.sql(),
                    String.valueOf(entry.success()), String.valueOf(entry.durationMillis()), entry.summary()))
                    .append('\n');
        }
        try {
            storage.writeString(FILE, content.toString());
        } catch (IOException ignored) {
            // keep the in-memory history; the next successful write persists it
        }
    }
}
