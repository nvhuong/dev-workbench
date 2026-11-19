package com.nvhuong.devworkbench.plugin.sql;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Line-based persistence shared by the SQL history and saved queries: one record per line, Base64 fields. */
final class SqlRecords {
    private SqlRecords() { }

    static String encodeLine(String... fields) {
        StringBuilder line = new StringBuilder();
        for (String field : fields) {
            if (line.length() > 0) line.append('\t');
            line.append(Base64.getUrlEncoder().withoutPadding()
                    .encodeToString((field == null ? "" : field).getBytes(StandardCharsets.UTF_8)));
        }
        return line.toString();
    }

    /** Decoded fields of {@code line}, or {@code null} for a corrupt line (skipped instead of losing the whole file). */
    static List<String> decodeLine(String line, int expectedFields) {
        String[] parts = line.split("\t", -1);
        if (parts.length < expectedFields) return null;
        List<String> fields = new ArrayList<>();
        try {
            for (String part : parts) {
                fields.add(new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException corrupt) {
            return null;
        }
        return fields;
    }

    static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }
}
