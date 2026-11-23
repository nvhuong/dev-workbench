package com.nvhuong.devworkbench.plugin.sql;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginStorage;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

/**
 * MCP tools of the SQL plugin. Connections come from the profiles saved in the SQL tool (by name), so credentials never
 * pass through the AI client. Read tools open the connection read-only and only accept a single query statement.
 */
final class SqlMcpTools {
    static final String PROFILE_FILE = "connection-profiles.txt";
    private static final int DEFAULT_MAX_ROWS = 100;
    private static final int MAX_ROWS = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 60;
    private static final int MAX_CELL_CHARS = 2000;
    private static final Pattern READ_STATEMENT = Pattern.compile(
            "(?is)^\\s*(select|with|show|describe|desc|explain|values|table)\\b.*");
    private static final Pattern SECRET_IN_URL = Pattern.compile("(?i)(password|pwd|secret|token)=([^;&]*)");

    record Profile(String name, String databaseType, String jdbcUrl, String driverClass, String driverJar,
                   String username, String password) { }

    private SqlMcpTools() {
    }

    static void register(PluginContext context) {
        PluginStorage storage = context.storage();
        for (McpToolContribution tool : tools(storage)) {
            context.registerMcpTool(tool);
        }
    }

    static List<McpToolContribution> tools(PluginStorage storage) {
        return List.of(
                new McpToolContribution("sql_list_profiles", "List SQL connection profiles",
                        "Lists the JDBC connection profiles saved in the DevWorkbench SQL tool. Use a profile name with the other sql_* tools. Passwords are never returned.",
                        schema(Map.of(), List.of()), true, arguments -> listProfiles(storage)),
                new McpToolContribution("sql_list_tables", "List database tables",
                        "Lists tables, views and synonyms of a schema (default: the connection's current schema).",
                        schema(Map.of(
                                "profile", string("Saved SQL profile name (see sql_list_profiles)."),
                                "schema", string("Optional schema name."),
                                "pattern", string("Optional table name filter, SQL LIKE syntax (e.g. ORDER%).")),
                                List.of("profile")),
                        true, arguments -> listTables(storage, arguments)),
                new McpToolContribution("sql_describe_table", "Describe a database table",
                        "Returns the columns of a table: type, size, nullability, default, primary key and comment.",
                        schema(Map.of(
                                "profile", string("Saved SQL profile name."),
                                "table", string("Table name (exact case as stored, e.g. ORDERS on Oracle)."),
                                "schema", string("Optional schema name.")),
                                List.of("profile", "table")),
                        true, arguments -> describeTable(storage, arguments)),
                new McpToolContribution("sql_query", "Run a read-only SQL query",
                        "Runs ONE read-only statement (SELECT, WITH, SHOW, DESCRIBE, EXPLAIN) on a read-only connection and returns up to maxRows rows. Data-changing statements are rejected; use sql_execute (requires --allow-writes).",
                        schema(Map.of(
                                "profile", string("Saved SQL profile name."),
                                "sql", string("A single read-only SQL statement."),
                                "maxRows", integer("Maximum rows to return (default 100).", 1, MAX_ROWS)),
                                List.of("profile", "sql")),
                        true, arguments -> query(storage, arguments)),
                new McpToolContribution("sql_execute", "Execute a data-changing SQL statement",
                        "Executes ONE statement that changes data or schema (INSERT, UPDATE, DELETE, DDL) in its own transaction and commits it. Only available when the MCP server runs with --allow-writes.",
                        schema(Map.of(
                                "profile", string("Saved SQL profile name."),
                                "sql", string("A single SQL statement.")),
                                List.of("profile", "sql")),
                        false, arguments -> execute(storage, arguments)));
    }

    // ---- tools -------------------------------------------------------------------------------------------------

    static Map<String, Object> listProfiles(PluginStorage storage) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Profile profile : readProfiles(storage)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", profile.name());
            item.put("databaseType", profile.databaseType());
            item.put("jdbcUrl", redactUrl(profile.jdbcUrl()));
            item.put("username", profile.username());
            items.add(item);
        }
        return Map.of("profiles", items, "count", items.size());
    }

    static Map<String, Object> listTables(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        Profile profile = profile(storage, arguments);
        try (Session session = open(profile, true)) {
            DatabaseMetaData metadata = session.connection().getMetaData();
            String schema = optional(arguments, "schema");
            if (schema == null) schema = currentSchema(session.connection());
            String pattern = optional(arguments, "pattern");
            List<Map<String, Object>> tables = new ArrayList<>();
            try (ResultSet result = metadata.getTables(null, schema, pattern == null ? "%" : pattern, null)) {
                while (result.next() && tables.size() < 2000) {
                    String type = result.getString("TABLE_TYPE");
                    if (type != null && type.toUpperCase(Locale.ROOT).contains("SYSTEM")) continue;
                    Map<String, Object> table = new LinkedHashMap<>();
                    table.put("schema", result.getString("TABLE_SCHEM"));
                    table.put("name", result.getString("TABLE_NAME"));
                    table.put("type", type);
                    String remarks = result.getString("REMARKS");
                    if (remarks != null && !remarks.isBlank()) table.put("comment", remarks);
                    tables.add(table);
                }
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("profile", profile.name());
            response.put("schema", schema);
            response.put("tables", tables);
            response.put("count", tables.size());
            return response;
        }
    }

    static Map<String, Object> describeTable(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        Profile profile = profile(storage, arguments);
        String table = required(arguments, "table");
        try (Session session = open(profile, true)) {
            DatabaseMetaData metadata = session.connection().getMetaData();
            String schema = optional(arguments, "schema");
            if (schema == null) schema = currentSchema(session.connection());
            java.util.Set<String> primaryKeys = new java.util.HashSet<>();
            try (ResultSet keys = metadata.getPrimaryKeys(null, schema, table)) {
                while (keys.next()) primaryKeys.add(keys.getString("COLUMN_NAME"));
            } catch (Exception ignored) {
                // views and synonyms have no primary key metadata on some drivers
            }
            List<Map<String, Object>> columns = new ArrayList<>();
            try (ResultSet result = metadata.getColumns(null, escape(schema, metadata), escape(table, metadata), null)) {
                while (result.next()) {
                    if (!table.equals(result.getString("TABLE_NAME"))) continue;
                    Map<String, Object> column = new LinkedHashMap<>();
                    String name = result.getString("COLUMN_NAME");
                    column.put("name", name);
                    column.put("type", result.getString("TYPE_NAME"));
                    column.put("size", result.getInt("COLUMN_SIZE"));
                    column.put("scale", result.getInt("DECIMAL_DIGITS"));
                    column.put("nullable", result.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls);
                    column.put("primaryKey", primaryKeys.contains(name));
                    String defaultValue = result.getString("COLUMN_DEF");
                    if (defaultValue != null) column.put("default", defaultValue.trim());
                    String remarks = result.getString("REMARKS");
                    if (remarks != null && !remarks.isBlank()) column.put("comment", remarks.trim());
                    columns.add(column);
                }
            }
            if (columns.isEmpty()) {
                throw new IllegalArgumentException("Table not found or not visible: " + (schema == null ? "" : schema + ".") + table);
            }
            return Map.of("profile", profile.name(), "table", table, "columns", columns, "count", columns.size());
        }
    }

    static Map<String, Object> query(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        Profile profile = profile(storage, arguments);
        String sql = singleStatement(required(arguments, "sql"));
        if (!READ_STATEMENT.matcher(stripLeadingComments(sql)).matches()) {
            throw new IllegalArgumentException("sql_query only runs read-only statements (SELECT, WITH, SHOW, DESCRIBE, EXPLAIN). Use sql_execute for changes.");
        }
        int maxRows = integerArgument(arguments, "maxRows", DEFAULT_MAX_ROWS, 1, MAX_ROWS);
        try (Session session = open(profile, true);
             Statement statement = session.connection().createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.setMaxRows(maxRows + 1);
            try {
                if (!statement.execute(sql)) {
                    throw new IllegalArgumentException("The statement did not return rows.");
                }
                try (ResultSet result = statement.getResultSet()) {
                    return rows(result, maxRows, profile.name());
                }
            } finally {
                session.connection().rollback(); // nothing to keep: the connection is read-only
            }
        }
    }

    static Map<String, Object> execute(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        Profile profile = profile(storage, arguments);
        String sql = singleStatement(required(arguments, "sql"));
        try (Session session = open(profile, false);
             Statement statement = session.connection().createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try {
                boolean hasRows = statement.execute(sql);
                Map<String, Object> response;
                if (hasRows) {
                    try (ResultSet result = statement.getResultSet()) {
                        response = new LinkedHashMap<>(rows(result, DEFAULT_MAX_ROWS, profile.name()));
                    }
                } else {
                    response = new LinkedHashMap<>();
                    response.put("profile", profile.name());
                    response.put("updateCount", statement.getUpdateCount());
                }
                session.connection().commit();
                response.put("committed", true);
                return response;
            } catch (Exception failure) {
                session.connection().rollback();
                throw failure;
            }
        }
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    private static Map<String, Object> rows(ResultSet result, int maxRows, String profileName) throws Exception {
        ResultSetMetaData metadata = result.getMetaData();
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) columns.add(metadata.getColumnLabel(i));
        List<List<Object>> rows = new ArrayList<>();
        boolean truncated = false;
        while (result.next()) {
            if (rows.size() >= maxRows) {
                truncated = true;
                break;
            }
            List<Object> row = new ArrayList<>(columns.size());
            for (int i = 1; i <= columns.size(); i++) row.add(cell(result.getObject(i)));
            rows.add(row);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profile", profileName);
        response.put("columns", columns);
        response.put("rows", rows);
        response.put("rowCount", rows.size());
        response.put("truncated", truncated);
        return response;
    }

    private static Object cell(Object value) throws Exception {
        if (value == null || value instanceof Number || value instanceof Boolean) return value;
        String text;
        if (value instanceof java.sql.Clob clob) {
            text = clob.getSubString(1, (int) Math.min(clob.length(), MAX_CELL_CHARS + 1L));
        } else if (value instanceof byte[] bytes) {
            text = "<" + bytes.length + " bytes>";
        } else if (value instanceof java.sql.Blob blob) {
            text = "<" + blob.length() + " bytes>";
        } else {
            text = String.valueOf(value);
        }
        return text.length() > MAX_CELL_CHARS ? text.substring(0, MAX_CELL_CHARS) + "…" : text;
    }

    /** Rejects scripts: the tools run exactly one statement (a trailing ';' is allowed, PL/SQL END; kept). */
    static String singleStatement(String sql) {
        String statement = SqlPanel.executableSql(sql);
        boolean quoted = false;
        for (int i = 0; i < statement.length(); i++) {
            char ch = statement.charAt(i);
            if (ch == '\'') quoted = !quoted;
            if (ch == ';' && !quoted && !statement.substring(i + 1).isBlank()
                    && !statement.toUpperCase(Locale.ROOT).contains("BEGIN")) {
                throw new IllegalArgumentException("Run one statement per call.");
            }
        }
        if (statement.isBlank()) throw new IllegalArgumentException("sql is empty");
        return statement;
    }

    private static String stripLeadingComments(String sql) {
        String text = sql.stripLeading();
        while (true) {
            if (text.startsWith("--")) {
                int end = text.indexOf('\n');
                text = end < 0 ? "" : text.substring(end + 1).stripLeading();
            } else if (text.startsWith("/*")) {
                int end = text.indexOf("*/");
                text = end < 0 ? "" : text.substring(end + 2).stripLeading();
            } else {
                return text;
            }
        }
    }

    static String redactUrl(String url) {
        return url == null ? "" : SECRET_IN_URL.matcher(url).replaceAll("$1=***");
    }

    private static String currentSchema(Connection connection) {
        String schema = SqlPanel.currentSchemaOf(connection);
        return schema == null || schema.isBlank() ? null : schema;
    }

    private static String escape(String name, DatabaseMetaData metadata) {
        if (name == null) return null;
        try {
            String escape = metadata.getSearchStringEscape();
            return SqlPanel.escapePattern(name, escape);
        } catch (Exception ignored) {
            return name;
        }
    }

    private static Profile profile(PluginStorage storage, Map<String, Object> arguments) throws Exception {
        String name = required(arguments, "profile");
        return readProfiles(storage).stream()
                .filter(profile -> profile.name().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SQL profile: " + name + ". Use sql_list_profiles."));
    }

    static List<Profile> readProfiles(PluginStorage storage) throws Exception {
        List<Profile> all = new ArrayList<>();
        for (String line : storage.readString(PROFILE_FILE).orElse("").lines().toList()) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 7 && fields.length != 8) continue;
            all.add(new Profile(decode(fields[0]), decode(fields[1]), decode(fields[2]), decode(fields[3]),
                    decode(fields[4]), decode(fields[5]), decode(fields[6])));
        }
        return all;
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    /** An open JDBC connection plus the driver class loader that must be closed with it. */
    record Session(Connection connection, URLClassLoader loader) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            try {
                connection.close();
            } finally {
                if (loader != null) loader.close();
            }
        }
    }

    static Session open(Profile profile, boolean readOnly) throws Exception {
        URLClassLoader loader = null;
        String jar = profile.driverJar() == null ? "" : profile.driverJar().trim();
        if (!jar.isBlank()) {
            List<URL> urls = new ArrayList<>();
            for (String part : jar.split(";")) {
                if (part.isBlank()) continue;
                Path path = Path.of(part.trim());
                if (!path.toFile().isFile()) throw new IllegalArgumentException("Driver JAR not found: " + path);
                urls.add(path.toUri().toURL());
            }
            loader = new URLClassLoader(urls.toArray(URL[]::new), SqlMcpTools.class.getClassLoader());
        }
        try {
            Properties properties = new Properties();
            if (profile.username() != null && !profile.username().isBlank()) properties.setProperty("user", profile.username());
            if (profile.password() != null && !profile.password().isEmpty()) properties.setProperty("password", profile.password());
            String url = profile.jdbcUrl();
            if (url.startsWith("jdbc:oracle:")) {
                properties.putIfAbsent("includeSynonyms", "true");
                properties.putIfAbsent("remarksReporting", "true");
            }
            Driver driver = driver(loader, profile.driverClass(), url);
            Connection connection = driver == null ? DriverManager.getConnection(url, properties) : driver.connect(url, properties);
            if (connection == null) throw new IllegalArgumentException("The JDBC driver does not accept this URL");
            try {
                connection.setAutoCommit(false);
                if (readOnly) connection.setReadOnly(true);
            } catch (Exception | AbstractMethodError unsupported) {
                // best effort: read tools still only accept read statements
            }
            URLClassLoader owned = loader;
            loader = null;
            return new Session(connection, owned);
        } finally {
            if (loader != null) loader.close();
        }
    }

    private static Driver driver(ClassLoader loader, String className, String url) throws Exception {
        if (className != null && !className.isBlank()) {
            ClassLoader effective = loader == null ? SqlMcpTools.class.getClassLoader() : loader;
            return (Driver) Class.forName(className.trim(), true, effective).getDeclaredConstructor().newInstance();
        }
        if (loader != null) {
            for (Driver driver : ServiceLoader.load(Driver.class, loader)) {
                if (driver.acceptsURL(url)) return driver;
            }
        }
        return null;
    }

    // ---- schema helpers ------------------------------------------------------------------------------------------

    static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description, "minLength", 1);
    }

    static Map<String, Object> integer(String description, int minimum, int maximum) {
        return Map.of("type", "integer", "description", description, "minimum", minimum, "maximum", maximum);
    }

    static String required(Map<String, Object> arguments, String name) {
        String value = optional(arguments, name);
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    static String optional(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    static int integerArgument(Map<String, Object> arguments, String name, int defaultValue, int min, int max) {
        Object value = arguments == null ? null : arguments.get(name);
        if (value == null) return defaultValue;
        int parsed = value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value).trim());
        return Math.max(min, Math.min(max, parsed));
    }
}
