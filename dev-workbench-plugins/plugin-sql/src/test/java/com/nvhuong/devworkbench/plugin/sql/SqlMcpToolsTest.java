package com.nvhuong.devworkbench.plugin.sql;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SqlMcpToolsTest {
    private static final String URL = "jdbc:h2:mem:sql_mcp;DB_CLOSE_DELAY=-1";

    private static MemoryStorage storage() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "")) {
            connection.createStatement().execute("DROP TABLE IF EXISTS ORDERS");
            connection.createStatement().execute("CREATE TABLE ORDERS(ID INT PRIMARY KEY, NAME VARCHAR(20) NOT NULL)");
            connection.createStatement().execute("COMMENT ON COLUMN ORDERS.NAME IS 'Customer'");
            connection.createStatement().execute("INSERT INTO ORDERS VALUES (1,'a'),(2,'b'),(3,'c')");
        }
        MemoryStorage storage = new MemoryStorage();
        storage.files.put(SqlMcpTools.PROFILE_FILE, line("local", "GENERIC", URL, "org.h2.Driver", "", "sa", "") + "\n");
        return storage;
    }

    private static String line(String... fields) {
        return Stream.of(fields).map(f -> Base64.getUrlEncoder().withoutPadding().encodeToString(f.getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.joining("\t"));
    }

    private static McpToolContribution tool(MemoryStorage storage, String name) {
        return SqlMcpTools.tools(storage).stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    public void exposesReadToolsAndOneWriteTool() throws Exception {
        List<McpToolContribution> tools = SqlMcpTools.tools(storage());
        assertEquals(tools.stream().map(McpToolContribution::name).toList(),
                List.of("sql_list_profiles", "sql_list_tables", "sql_describe_table", "sql_query", "sql_execute"));
        assertEquals(tools.stream().filter(t -> !t.readOnly()).map(McpToolContribution::name).toList(), List.of("sql_execute"));
    }

    @Test
    public void listsProfilesWithoutSecrets() throws Exception {
        Map<String, Object> result = tool(storage(), "sql_list_profiles").handler().call(Map.of());
        String json = result.toString();
        assertTrue(json.contains("local"), json);
        assertFalse(json.toLowerCase().contains("password"), "profile passwords must never be returned: " + json);
        assertEquals(SqlMcpTools.redactUrl("jdbc:postgresql://db/app?user=a&password=s3cret&ssl=true"),
                "jdbc:postgresql://db/app?user=a&password=***&ssl=true");
    }

    @Test
    public void describesTablesAndRunsReadOnlyQueries() throws Exception {
        MemoryStorage storage = storage();
        Map<String, Object> tables = tool(storage, "sql_list_tables").handler().call(Map.of("profile", "local", "schema", "PUBLIC"));
        assertTrue(tables.toString().contains("ORDERS"), tables.toString());

        Map<String, Object> described = tool(storage, "sql_describe_table").handler().call(Map.of("profile", "LOCAL", "table", "ORDERS", "schema", "PUBLIC"));
        assertEquals(described.get("count"), 2);
        assertTrue(described.toString().contains("primaryKey=true") && described.toString().contains("Customer"), described.toString());

        Map<String, Object> rows = tool(storage, "sql_query").handler().call(Map.of("profile", "local", "sql", "SELECT * FROM ORDERS ORDER BY ID;", "maxRows", 2));
        assertEquals(rows.get("rowCount"), 2);
        assertEquals(rows.get("truncated"), true);
        assertEquals(rows.get("columns"), List.of("ID", "NAME"));
    }

    @Test
    public void queryRejectsWritesAndScripts() throws Exception {
        MemoryStorage storage = storage();
        McpToolContribution query = tool(storage, "sql_query");
        expectThrows(IllegalArgumentException.class, () -> query.handler().call(Map.of("profile", "local", "sql", "DELETE FROM ORDERS")));
        expectThrows(IllegalArgumentException.class, () -> query.handler().call(Map.of("profile", "local", "sql", "SELECT 1; DELETE FROM ORDERS")));
        expectThrows(IllegalArgumentException.class, () -> query.handler().call(Map.of("profile", "missing", "sql", "SELECT 1")));
        // A write disguised in a CTE is stopped by the read-only connection (or the driver) and never committed.
        try { query.handler().call(Map.of("profile", "local", "sql", "WITH x AS (SELECT 1) DELETE FROM ORDERS")); } catch (Exception expected) { }
        assertEquals(count(), 3);
    }

    @Test
    public void executeCommitsChanges() throws Exception {
        MemoryStorage storage = storage();
        Map<String, Object> result = tool(storage, "sql_execute").handler().call(Map.of("profile", "local", "sql", "INSERT INTO ORDERS VALUES (4, 'd')"));
        assertEquals(result.get("updateCount"), 1);
        assertEquals(result.get("committed"), true);
        assertEquals(count(), 4);
    }

    private static int count() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM ORDERS")) {
            result.next();
            return result.getInt(1);
        }
    }
}
