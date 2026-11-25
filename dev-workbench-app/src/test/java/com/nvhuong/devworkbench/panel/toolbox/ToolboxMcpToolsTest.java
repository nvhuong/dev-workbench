package com.nvhuong.devworkbench.panel.toolbox;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class ToolboxMcpToolsTest {
    private MockWebServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeMethod
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void elasticsearchReadsUseTheSavedProfileAndRejectWrites() throws Exception {
        ElasticsearchConnectionProfileStore store = new ElasticsearchConnectionProfileStore(Files.createTempFile("es", ".json"));
        store.saveProfiles(List.of(ElasticsearchConnectionProfile.builder().id("es1").name("Logs")
                .baseUrl(server.url("/").toString()).authEnabled(true).username("elastic").password("es-secret").build()), "es1");
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"hits\":{\"total\":3}}"));

        Map<String, Object> result = ToolboxMcpTools.esRequest(store, client,
                Map.of("profile", "logs", "method", "POST", "path", "/orders/_search", "body", "{\"size\":1}"), false);

        RecordedRequest request = server.takeRequest();
        assertEquals(request.getPath(), "/orders/_search");
        assertEquals(request.getHeader("Authorization"),
                "Basic " + Base64.getEncoder().encodeToString("elastic:es-secret".getBytes()));
        assertEquals(result.get("status"), 200);
        assertTrue(String.valueOf(result.get("body")).contains("\"total\":3"));
        assertFalse(ToolboxMcpTools.listEs(store).toString().contains("es-secret"));

        expectThrows(IllegalArgumentException.class, () -> ToolboxMcpTools.esRequest(store, client,
                Map.of("profile", "Logs", "method", "POST", "path", "/orders/_doc", "body", "{}"), false));
        assertTrue(ToolboxMcpTools.isEsRead("GET", "/_cat/indices"));
        assertTrue(ToolboxMcpTools.isEsRead("POST", "/a/_count?q=x"));
        assertFalse(ToolboxMcpTools.isEsRead("POST", "/a/_delete_by_query"));
        assertFalse(ToolboxMcpTools.isEsRead("DELETE", "/a"));
    }

    @Test
    public void influxQueriesAreReadOnlyAndNeverEchoCredentials() throws Exception {
        InfluxDbConnectionProfileStore store = new InfluxDbConnectionProfileStore(Files.createTempFile("influx", ".json"));
        store.saveProfiles(List.of(InfluxDbConnectionProfile.builder().id("i1").name("Metrics").mode("INFLUXQL_V1")
                .baseUrl(server.url("/").toString()).database("telegraf").username("reader").password("influx-secret")
                .build()), "i1");
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"results\":[]}"));

        Map<String, Object> result = ToolboxMcpTools.influxQuery(store, client,
                Map.of("profile", "Metrics", "query", "SELECT mean(usage) FROM cpu"));

        RecordedRequest request = server.takeRequest();
        assertEquals(request.getRequestUrl().queryParameter("db"), "telegraf");
        assertEquals(request.getRequestUrl().queryParameter("p"), "influx-secret");
        assertFalse(result.toString().contains("influx-secret"), result.toString());
        expectThrows(IllegalArgumentException.class, () -> ToolboxMcpTools.influxQuery(store, client,
                Map.of("profile", "Metrics", "query", "DROP DATABASE telegraf")));
        expectThrows(IllegalArgumentException.class, () -> ToolboxMcpTools.influxQuery(store, client,
                Map.of("profile", "Metrics", "query", "SELECT * FROM cpu; DROP DATABASE telegraf")));
    }

    @Test
    public void fluxWritesAreRejectedInTheReadTool() throws Exception {
        InfluxDbConnectionProfileStore store = new InfluxDbConnectionProfileStore(Files.createTempFile("influx2", ".json"));
        store.saveProfiles(List.of(InfluxDbConnectionProfile.builder().id("i2").name("Cloud").mode("FLUX_V2")
                .baseUrl(server.url("/").toString()).org("acme").token("flux-token").build()), "i2");

        expectThrows(IllegalArgumentException.class, () -> ToolboxMcpTools.influxQuery(store, client, Map.of("profile",
                "Cloud", "query", "from(bucket:\"a\") |> range(start:-1h) |> to(bucket:\"b\")")));

        server.enqueue(new MockResponse().setResponseCode(204));
        Map<String, Object> written = ToolboxMcpTools.influxWrite(store, client,
                Map.of("profile", "Cloud", "database", "metrics", "lines", "cpu usage=1\ncpu usage=2"));
        RecordedRequest request = server.takeRequest();
        assertEquals(request.getRequestUrl().encodedPath(), "/api/v2/write");
        assertEquals(request.getRequestUrl().queryParameter("bucket"), "metrics");
        assertEquals(request.getHeader("Authorization"), "Token flux-token");
        assertEquals(written.get("points"), 2L);
        assertFalse(ToolboxMcpTools.listInflux(store).toString().contains("flux-token"));
    }

    @Test
    public void redactsCredentialsInUrls() {
        assertEquals(ToolboxMcpTools.redactQuery("/query?db=x&u=admin&p=pw&q=SELECT"), "/query?db=x&u=<redacted>&p=<redacted>&q=SELECT");
        assertEquals(ToolboxMcpTools.stripUserInfo("https://user:pw@es.local:9200"), "https://es.local:9200");
    }
}
