package com.nvhuong.devworkbench.plugin.sql;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SqlHistoryTest {

    @Test public void persistsNewestFirstAndSurvivesReload() {
        MemoryStorage storage = new MemoryStorage();
        SqlHistory history = new SqlHistory(storage);
        history.add(new SqlHistory.Entry(1, "prod", "SELECT 1", true, 5, "1 row"));
        history.add(new SqlHistory.Entry(2, "prod", "SELECT *\n\tFROM orders\nWHERE note = 'a\tb'", false, 7, "ORA-00942"));

        List<SqlHistory.Entry> reloaded = new SqlHistory(storage).entries();
        assertEquals(reloaded.size(), 2);
        assertEquals(reloaded.get(0).sql(), "SELECT *\n\tFROM orders\nWHERE note = 'a\tb'");
        assertFalse(reloaded.get(0).success());
        assertEquals(reloaded.get(0).summary(), "ORA-00942");
        assertEquals(reloaded.get(1).durationMillis(), 5);
    }

    @Test public void rerunningTheNewestStatementDoesNotDuplicateIt() {
        SqlHistory history = new SqlHistory(new MemoryStorage());
        history.add(new SqlHistory.Entry(1, "p", "SELECT 1", true, 1, ""));
        history.add(new SqlHistory.Entry(2, "p", "SELECT 1 ", true, 1, ""));
        history.add(new SqlHistory.Entry(3, "other", "SELECT 1", true, 1, ""));
        assertEquals(history.entries().size(), 2);
        assertEquals(history.entries().get(1).executedAt(), 2);
    }

    @Test public void keepsAtMostMaxEntriesAndSearches() {
        SqlHistory history = new SqlHistory(new MemoryStorage());
        for (int i = 0; i < SqlHistory.MAX_ENTRIES + 20; i++) {
            history.add(new SqlHistory.Entry(i, "p", "SELECT " + i + " FROM dual", true, 1, ""));
        }
        assertEquals(history.entries().size(), SqlHistory.MAX_ENTRIES);
        assertEquals(history.entries().get(0).sql(), "SELECT " + (SqlHistory.MAX_ENTRIES + 19) + " FROM dual");
        assertEquals(history.search("select 519 ").size(), 1);
        history.clear();
        assertTrue(history.entries().isEmpty());
    }

    @Test public void skipsCorruptLines() {
        MemoryStorage storage = new MemoryStorage();
        new SqlHistory(storage).add(new SqlHistory.Entry(1, "p", "SELECT 1", true, 1, ""));
        storage.files.put(SqlHistory.FILE, "garbage\n%%%\t%%\t\t\t\t\n" + storage.files.get(SqlHistory.FILE));
        assertEquals(new SqlHistory(storage).entries().size(), 1);
    }
}
