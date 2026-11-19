package com.nvhuong.devworkbench.service.devworkbench;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.collection.importer.devworkbench.DevWorkbenchCollectionParser;
import com.nvhuong.devworkbench.model.Environment;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.SavedResponse;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * The project rename replaced "postman" inside identifiers of the Postman file format itself, so exports were no longer
 * recognised by Postman and Postman's response preview language was dropped on import.
 */
public class PostmanFormatCompatibilityTest {

    @Test
    public void exportedCollectionUsesPostmanSchemaAndIds() {
        JSONObject collection = DevWorkbenchCollectionExporter.buildDevWorkbenchCollectionFromTreeNode(
                new DefaultMutableTreeNode(), "Orders");
        JSONObject info = collection.getJSONObject("info");

        assertEquals(info.getStr("schema"), "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
        assertNotNull(info.getStr("_postman_id"));
        assertFalse(info.containsKey("_devworkbench_id"));
    }

    @Test
    public void exportedResponseUsesPostmanPreviewLanguage() {
        HttpRequestItem item = new HttpRequestItem();
        item.setName("Get order");
        item.setMethod("GET");
        item.setUrl("https://api.example.com/orders/1");
        SavedResponse saved = new SavedResponse();
        saved.setName("OK");
        saved.setCode(200);
        saved.setPreviewLanguage("json");
        item.setResponse(List.of(saved));

        String json = DevWorkbenchCollectionExporter.toDevWorkbenchItem(item).toString();

        assertTrue(json.contains("\"_postman_previewlanguage\":\"json\""), json);
        assertFalse(json.contains("_devworkbench_previewlanguage"), json);
    }

    @Test
    public void exportedEnvironmentUsesPostmanMarkers() {
        Environment env = new Environment();
        env.setName("Staging");
        JSONObject obj = JSONUtil.parseObj(DevWorkbenchEnvironmentParser.toDevWorkbenchEnvironmentJson(env));

        assertEquals(obj.getStr("_postman_variable_scope"), "environment");
        assertNotNull(obj.getStr("_postman_exported_at"));
        assertNotNull(obj.getStr("_postman_exported_using"));
    }

    @Test
    public void importReadsPostmanPreviewLanguageAndLegacyRenamedKey() {
        String json = """
                {"info": {"schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
                 "item": [{"name": "Get", "request": {"method": "GET", "url": "https://api.example.com"},
                   "response": [{"name": "Postman", "code": 200, "_postman_previewlanguage": "json", "body": "{}"},
                                {"name": "Old export", "code": 200, "_devworkbench_previewlanguage": "xml", "body": ""}]}]}
                """;

        var parsed = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);
        var responses = parsed.getChildren().get(0).asRequest().getResponse();

        assertEquals(responses.get(0).getPreviewLanguage(), "json");
        assertEquals(responses.get(1).getPreviewLanguage(), "xml");
        assertEquals(parsed.getGroup().getName(), "Postman", "unnamed Postman collections keep the Postman default name");
    }
}
