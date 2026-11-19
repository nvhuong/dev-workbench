package com.nvhuong.devworkbench.plugin.sql;

import org.testng.annotations.Test;

import java.sql.SQLFeatureNotSupportedException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.expectThrows;

public class SqlCommentDdlTest {

    @Test public void commentOnForOraclePostgresAndH2() throws Exception {
        assertEquals(SqlCommentDdl.columnComment("Oracle", "\"HR\".\"EMP\"", "\"NAME\"", "Tên nhân viên's", null),
                "COMMENT ON COLUMN \"HR\".\"EMP\".\"NAME\" IS 'Tên nhân viên''s'");
        assertEquals(SqlCommentDdl.tableComment("PostgreSQL", "\"public\".\"orders\"", "Đơn hàng"),
                "COMMENT ON TABLE \"public\".\"orders\" IS 'Đơn hàng'");
        assertEquals(SqlCommentDdl.tableComment("H2", "\"PUBLIC\".\"T\"", ""), "COMMENT ON TABLE \"PUBLIC\".\"T\" IS ''");
    }

    @Test public void mysqlRestatesTheExactColumnDefinition() throws Exception {
        String create = "CREATE TABLE `orders` (\n"
                + "  `id` bigint NOT NULL AUTO_INCREMENT,\n"
                + "  `note` varchar(200) CHARACTER SET utf8mb4 DEFAULT 'x' COMMENT 'old \\'one\\'',\n"
                + "  `notes2` text,\n"
                + "  PRIMARY KEY (`id`)\n"
                + ") ENGINE=InnoDB";
        String definition = SqlCommentDdl.mysqlColumnDefinition(create, "note");
        assertEquals(SqlCommentDdl.columnComment("MySQL", "`shop`.`orders`", "`note`", "Ghi chú \\ mới", definition),
                "ALTER TABLE `shop`.`orders` MODIFY COLUMN `note` varchar(200) CHARACTER SET utf8mb4 DEFAULT 'x'"
                        + " COMMENT 'Ghi chú \\\\ mới'");
        assertEquals(SqlCommentDdl.mysqlColumnDefinition(create, "notes2"), "`notes2` text,");
        assertEquals(SqlCommentDdl.tableComment("MariaDB", "`orders`", "a'b"), "ALTER TABLE `orders` COMMENT = 'a''b'");
    }

    @Test public void unsupportedDatabasesSaySo() {
        assertFalse(SqlCommentDdl.supported("SQLite"));
        expectThrows(SQLFeatureNotSupportedException.class,
                () -> SqlCommentDdl.columnComment("SQLite", "t", "c", "x", null));
    }
}
