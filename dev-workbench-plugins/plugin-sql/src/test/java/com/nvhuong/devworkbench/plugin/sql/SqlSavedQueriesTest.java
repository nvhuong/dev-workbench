package com.nvhuong.devworkbench.plugin.sql;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SqlSavedQueriesTest {

    @Test public void savesOverwritesRenamesAndDeletes() {
        MemoryStorage storage = new MemoryStorage();
        SqlSavedQueries saved = new SqlSavedQueries(storage);
        saved.save("Báo cáo tháng", "SELECT * FROM orders;\nSELECT COUNT(*) FROM orders;", 1);
        saved.save("adhoc", "SELECT 1", 2);
        saved.save("ADHOC", "SELECT 2", 3);

        SqlSavedQueries reloaded = new SqlSavedQueries(storage);
        assertEquals(reloaded.list().size(), 2);
        assertEquals(reloaded.list().get(0).name(), "ADHOC");
        assertEquals(reloaded.find("adhoc").orElseThrow().sql(), "SELECT 2");
        assertEquals(reloaded.find("báo cáo tháng").orElseThrow().sql(),
                "SELECT * FROM orders;\nSELECT COUNT(*) FROM orders;");

        assertFalse(reloaded.rename("adhoc", "Báo cáo tháng", 4), "must not overwrite another query");
        assertTrue(reloaded.rename("adhoc", "Kiểm tra", 4));
        assertTrue(new SqlSavedQueries(storage).find("Kiểm tra").isPresent());
        reloaded.delete("kiểm tra");
        assertEquals(new SqlSavedQueries(storage).list().size(), 1);
        assertEquals(reloaded.search("count(").size(), 1);
    }
}
