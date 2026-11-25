package com.nvhuong.devworkbench.panel.toolbox;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * MCP tools for the built-in Elasticsearch and InfluxDB toolbox panels. Servers are addressed by the connection
 * profiles saved in those panels, so passwords and tokens never pass through the AI client. Reads and writes are
 * separate tools; write tools are only served with --allow-writes.
 */
public final class ToolboxMcpTools {
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_BODY_CHARS = 200_000;
    /** Elasticsearch endpoints that only read data even when called with POST. */
    private static final Pattern ES_READ_POST = Pattern.compile(
            "(^|/)(_search|_count|_msearch|_mget|_validate/query|_field_caps|_explain(/[^/]+)?|_analyze|_sql|_eql/search)(\\?.*)?$");
    private static final Pattern INFLUXQL_READ = Pattern.compile("(?is)^\\s*(select|show|explain)\\b.*");
    private static final Pattern FLUX_WRITE = Pattern.compile("(?s).*\\bto\\s*\\(.*");

    private ToolboxMcpTools() {
    }

    public static List<McpToolContribution> tools() {
        ElasticsearchConnectionProfileStore es = new ElasticsearchConnectionProfileStore();
        InfluxDbConnectionProfileStore influx = new InfluxDbConnectionProfileStore();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        return tools(es, influx, client);
    }

    static List<McpToolContribution> tools(ElasticsearchConnectionProfileStore es,
                                           InfluxDbConnectionProfileStore influx,
                                           HttpClient client) {
        return List.of(
                new McpToolContribution("elasticsearch_list_profiles", "List Elasticsearch connection profiles",
                        "Lists the Elasticsearch profiles saved in the DevWorkbench toolbox. Passwords are never returned.",
                        schema(Map.of(), List.of()), true, arguments -> listEs(es)),
                new McpToolContribution("elasticsearch_request", "Read from Elasticsearch",
                        "Sends a read request to Elasticsearch: GET/HEAD to any path, or POST to read endpoints such as "
                                + "_search, _count, _msearch, _mget, _sql. Example: path=/orders/_search with a query body.",
                        schema(Map.of(
                                "profile", string("Profile name (see elasticsearch_list_profiles)."),
                                "method", Map.of("type", "string", "enum", List.of("GET", "HEAD", "POST")),
                                "path", string("Path such as /_cat/indices?format=json or /orders/_search."),
                                "body", string("Optional JSON body.")),
                                List.of("profile", "path")),
                        true, arguments -> esRequest(es, client, arguments, false)),
                new McpToolContribution("elasticsearch_write", "Write to Elasticsearch",
                        "Sends any request to Elasticsearch (index, update, delete documents or indices). "
                                + "Only available with --allow-writes.",
                        schema(Map.of(
                                "profile", string("Profile name."),
                                "method", Map.of("type", "string", "enum", List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE")),
                                "path", string("Request path."),
                                "body", string("Optional JSON (or NDJSON for _bulk) body.")),
                                List.of("profile", "method", "path")),
                        false, arguments -> esRequest(es, client, arguments, true)),
                new McpToolContribution("influxdb_list_profiles", "List InfluxDB connection profiles",
                        "Lists the InfluxDB profiles saved in the DevWorkbench toolbox (URL, mode, org, database). "
                                + "Tokens and passwords are never returned.",
                        schema(Map.of(), List.of()), true, arguments -> listInflux(influx)),
                new McpToolContribution("influxdb_query", "Query InfluxDB",
                        "Runs a read query: InfluxQL SELECT/SHOW for 1.x profiles, or a Flux query for 2.x profiles "
                                + "(Flux to() is rejected because it writes).",
                        schema(Map.of(
                                "profile", string("Profile name (see influxdb_list_profiles)."),
                                "query", string("InfluxQL or Flux query."),
                                "database", string("InfluxQL database; defaults to the profile database.")),
                                List.of("profile", "query")),
                        true, arguments -> influxQuery(influx, client, arguments)),
                new McpToolContribution("influxdb_write", "Write to InfluxDB",
                        "Writes points in line protocol (precision ms) to a 1.x database or a 2.x bucket. "
                                + "Only available with --allow-writes.",
                        schema(Map.of(
                                "profile", string("Profile name."),
                                "lines", string("Line protocol, one point per line."),
                                "database", string("1.x database or 2.x bucket; defaults to the profile database.")),
                                List.of("profile", "lines")),
                        false, arguments -> influxWrite(influx, client, arguments)));
    }

    static Map<String, Object> listEs(ElasticsearchConnectionProfileStore store) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ElasticsearchConnectionProfile profile : store.loadProfiles()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", profile.getName());
            item.put("baseUrl", stripUserInfo(profile.getBaseUrl()));
            item.put("authenticated", profile.isAuthEnabled());
            items.add(item);
        }
        return Map.of("profiles", items, "count", items.size());
    }

    static Map<String, Object> esRequest(ElasticsearchConnectionProfileStore store, HttpClient client,
                                         Map<String, Object> arguments, boolean write) throws Exception {
        ElasticsearchConnectionProfile profile = store.loadProfiles().stream()
                .filter(candidate -> matches(arguments, candidate.getName(), candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Elasticsearch profile: "
                        + text(arguments, "profile") + ". Use elasticsearch_list_profiles."));
        String method = text(arguments, "method");
        method = method == null ? (write ? "POST" : "GET") : method.toUpperCase(Locale.ROOT);
        String path = required(arguments, "path");
        if (!path.startsWith("/")) path = "/" + path;
        if (!write && !isEsRead(method, path)) {
            throw new IllegalArgumentException(method + " " + path + " can change data; use elasticsearch_write "
                    + "(needs --allow-writes).");
        }
        String body = text(arguments, "body");
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(trimSlash(profile.getBaseUrl()) + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/json");
        if (profile.isAuthEnabled() && profile.getUsername() != null && !profile.getUsername().isBlank()) {
            String credentials = profile.getUsername() + ":" + (profile.getPassword() == null ? "" : profile.getPassword());
            request.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        if (body != null) {
            request.header("Content-Type", path.contains("_bulk") || path.contains("_msearch")
                    ? "application/x-ndjson" : "application/json");
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return response(profile.getName(), method, path, client.send(request.build(), HttpResponse.BodyHandlers.ofString()));
    }

    static boolean isEsRead(String method, String path) {
        if (Set.of("GET", "HEAD").contains(method)) return true;
        return "POST".equals(method) && ES_READ_POST.matcher(path).find();
    }

    static Map<String, Object> listInflux(InfluxDbConnectionProfileStore store) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (InfluxDbConnectionProfile profile : store.loadProfiles()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", profile.getName());
            item.put("baseUrl", stripUserInfo(profile.getBaseUrl()));
            item.put("mode", isFlux(profile) ? "flux" : "influxql");
            item.put("org", profile.getOrg());
            item.put("database", profile.getDatabase());
            items.add(item);
        }
        return Map.of("profiles", items, "count", items.size());
    }

    static Map<String, Object> influxQuery(InfluxDbConnectionProfileStore store, HttpClient client,
                                           Map<String, Object> arguments) throws Exception {
        InfluxDbConnectionProfile profile = influxProfile(store, arguments);
        String query = required(arguments, "query");
        HttpRequest.Builder request;
        String target;
        if (isFlux(profile)) {
            if (FLUX_WRITE.matcher(query).matches()) {
                throw new IllegalArgumentException("Flux to() writes data; use influxdb_write (needs --allow-writes).");
            }
            target = "/api/v2/query?org=" + enc(profile.getOrg());
            request = HttpRequest.newBuilder(URI.create(trimSlash(profile.getBaseUrl()) + target))
                    .header("Content-Type", "application/vnd.flux")
                    .header("Accept", "text/csv")
                    .POST(HttpRequest.BodyPublishers.ofString(query));
        } else {
            if (!INFLUXQL_READ.matcher(query).matches() || query.contains(";")) {
                throw new IllegalArgumentException("Only one SELECT, SHOW or EXPLAIN statement is allowed here.");
            }
            String database = text(arguments, "database");
            target = "/query?db=" + enc(database == null ? profile.getDatabase() : database)
                    + "&q=" + enc(query) + "&epoch=ms" + v1Auth(profile);
            request = HttpRequest.newBuilder(URI.create(trimSlash(profile.getBaseUrl()) + target))
                    .header("Accept", "application/json").GET();
        }
        authorize(request, profile);
        return response(profile.getName(), "QUERY", redactQuery(target),
                client.send(request.timeout(TIMEOUT).build(), HttpResponse.BodyHandlers.ofString()));
    }

    static Map<String, Object> influxWrite(InfluxDbConnectionProfileStore store, HttpClient client,
                                           Map<String, Object> arguments) throws Exception {
        InfluxDbConnectionProfile profile = influxProfile(store, arguments);
        String lines = required(arguments, "lines");
        String database = text(arguments, "database");
        String bucket = database == null ? profile.getDatabase() : database;
        String target = isFlux(profile)
                ? "/api/v2/write?org=" + enc(profile.getOrg()) + "&bucket=" + enc(bucket) + "&precision=ms"
                : "/write?db=" + enc(bucket) + "&precision=ms" + v1Auth(profile);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(trimSlash(profile.getBaseUrl()) + target))
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(lines));
        authorize(request, profile);
        Map<String, Object> result = response(profile.getName(), "WRITE", redactQuery(target),
                client.send(request.timeout(TIMEOUT).build(), HttpResponse.BodyHandlers.ofString()));
        result.put("points", lines.lines().filter(line -> !line.isBlank() && !line.startsWith("#")).count());
        return result;
    }

    private static InfluxDbConnectionProfile influxProfile(InfluxDbConnectionProfileStore store, Map<String, Object> arguments) {
        return store.loadProfiles().stream()
                .filter(candidate -> matches(arguments, candidate.getName(), candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown InfluxDB profile: "
                        + text(arguments, "profile") + ". Use influxdb_list_profiles."));
    }

    private static boolean isFlux(InfluxDbConnectionProfile profile) {
        return "FLUX_V2".equals(profile.getMode());
    }

    private static void authorize(HttpRequest.Builder request, InfluxDbConnectionProfile profile) {
        if (profile.getToken() != null && !profile.getToken().isBlank()) {
            request.header("Authorization", "Token " + profile.getToken());
        }
    }

    private static String v1Auth(InfluxDbConnectionProfile profile) {
        StringBuilder auth = new StringBuilder();
        if (profile.getUsername() != null && !profile.getUsername().isBlank()) auth.append("&u=").append(enc(profile.getUsername()));
        if (profile.getPassword() != null && !profile.getPassword().isBlank()) auth.append("&p=").append(enc(profile.getPassword()));
        return auth.toString();
    }

    /** Never echo v1 credentials passed as query parameters. */
    static String redactQuery(String target) {
        return target.replaceAll("([?&])(u|p)=[^&]*", "$1$2=<redacted>");
    }

    private static Map<String, Object> response(String profile, String method, String path, HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body();
        boolean truncated = body.length() > MAX_BODY_CHARS;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", profile);
        result.put("request", method + " " + path);
        result.put("status", response.statusCode());
        result.put("body", truncated ? body.substring(0, MAX_BODY_CHARS) : body);
        result.put("bodyTruncated", truncated);
        return result;
    }

    private static boolean matches(Map<String, Object> arguments, String name, String id) {
        String wanted = required(arguments, "profile");
        return wanted.equalsIgnoreCase(name) || wanted.equals(id);
    }

    static String stripUserInfo(String url) {
        return url == null ? null : url.replaceAll("(?i)^(https?://)[^/@]*@", "$1");
    }

    private static String trimSlash(String url) {
        return url == null ? "" : url.replaceAll("/+$", "");
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static String required(Map<String, Object> arguments, String name) {
        String value = text(arguments, name);
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String text(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }
}
