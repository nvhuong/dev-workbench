package com.nvhuong.devworkbench.plugin.sql;

import java.sql.SQLFeatureNotSupportedException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Builds the vendor-specific DDL that stores a table or column comment in the database. */
final class SqlCommentDdl {
    private static final Pattern MYSQL_COMMENT = Pattern.compile("(?i)\\s+COMMENT\\s+'(?:[^'\\\\]|\\\\.|'')*'");

    enum Dialect { COMMENT_ON, MYSQL, UNSUPPORTED }

    private SqlCommentDdl() { }

    static Dialect dialect(String productName) {
        String product = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
        if (product.contains("mysql") || product.contains("mariadb")) return Dialect.MYSQL;
        if (product.contains("oracle") || product.contains("postgres") || product.contains("h2")
                || product.contains("db2") || product.contains("hsql") || product.contains("dameng")
                || product.contains("kingbase") || product.contains("gauss") || product.contains("snowflake")
                || product.contains("redshift") || product.contains("cockroach")) {
            return Dialect.COMMENT_ON;
        }
        return Dialect.UNSUPPORTED;
    }

    static boolean supported(String productName) {
        return dialect(productName) != Dialect.UNSUPPORTED;
    }

    static String tableComment(String productName, String qualifiedTable, String comment)
            throws SQLFeatureNotSupportedException {
        return switch (dialect(productName)) {
            case COMMENT_ON -> "COMMENT ON TABLE " + qualifiedTable + " IS " + literal(comment, false);
            case MYSQL -> "ALTER TABLE " + qualifiedTable + " COMMENT = " + literal(comment, true);
            case UNSUPPORTED -> throw unsupported(productName);
        };
    }

    /**
     * {@code COMMENT ON COLUMN} for most databases. MySQL can only change a comment by restating the whole column, so
     * {@code mysqlColumnDefinition} is the column's line from {@code SHOW CREATE TABLE}; its existing comment is
     * replaced, keeping type, charset, default and other attributes exactly as they are.
     */
    static String columnComment(String productName, String qualifiedTable, String quotedColumn, String comment,
                                String mysqlColumnDefinition) throws SQLFeatureNotSupportedException {
        return switch (dialect(productName)) {
            case COMMENT_ON -> "COMMENT ON COLUMN " + qualifiedTable + "." + quotedColumn + " IS " + literal(comment, false);
            case MYSQL -> {
                if (mysqlColumnDefinition == null || mysqlColumnDefinition.isBlank()) {
                    throw new SQLFeatureNotSupportedException("Column definition not found");
                }
                String definition = mysqlColumnDefinition.strip();
                if (definition.endsWith(",")) definition = definition.substring(0, definition.length() - 1).strip();
                definition = MYSQL_COMMENT.matcher(definition).replaceAll("");
                yield "ALTER TABLE " + qualifiedTable + " MODIFY COLUMN " + definition + " COMMENT " + literal(comment, true);
            }
            case UNSUPPORTED -> throw unsupported(productName);
        };
    }

    /** The line defining {@code column} in a MySQL {@code SHOW CREATE TABLE} statement. */
    static String mysqlColumnDefinition(String createTable, String column) {
        if (createTable == null || column == null) return null;
        String prefix = "`" + column.replace("`", "``") + "`";
        for (String line : createTable.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.startsWith(prefix + " ")) return trimmed;
        }
        return null;
    }

    static String literal(String comment, boolean backslashEscapes) {
        String value = comment == null ? "" : comment;
        if (backslashEscapes) value = value.replace("\\", "\\\\");
        return "'" + value.replace("'", "''") + "'";
    }

    private static SQLFeatureNotSupportedException unsupported(String productName) {
        return new SQLFeatureNotSupportedException(SqlI18n.t("comment_unsupported", productName));
    }

}
