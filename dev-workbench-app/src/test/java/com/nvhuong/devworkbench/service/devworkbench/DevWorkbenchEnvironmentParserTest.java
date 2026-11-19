package com.nvhuong.devworkbench.service.devworkbench;

import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.model.Environment;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

/**
 * DevWorkbenchEnvironmentParser Kiểm tra đơn vị
 */
public class DevWorkbenchEnvironmentParserTest {

    @Test
    public void testParseDevWorkbenchEnvironments_SingleObject() {
        // Chuẩn bị dữ liệu thử nghiệm - Một đối tượng môi trường duy nhất
        String json = """
                {
                    "name": "Môi trường thử nghiệm",
                    "values": [
                        {"key": "baseUrl", "value": "https://api.example.com", "enabled": true},
                        {"key": "token", "value": "abc123", "enabled": true},
                        {"key": "disabled_var", "value": "value", "enabled": false}
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        List<Environment> envs = DevWorkbenchEnvironmentParser.parseDevWorkbenchEnvironments(json);

        // Xác minh kết quả
        assertNotNull(envs);
        assertEquals(envs.size(), 1);

        Environment env = envs.get(0);
        assertEquals(env.getName(), "Môi trường thử nghiệm");
        assertNotNull(env.getId());

        // Xác thực các biến
        assertEquals(env.getVariables().size(), 2); // disabled_var nên được bỏ qua
        assertEquals(env.getVariables().get("baseUrl"), "https://api.example.com");
        assertEquals(env.getVariables().get("token"), "abc123");
        assertNull(env.getVariables().get("disabled_var"));
    }

    @Test
    public void testParseDevWorkbenchEnvironments_Array() {
        // Chuẩn bị dữ liệu thử nghiệm - mảng môi trường
        String json = """
                [
                    {
                        "name": "môi trường phát triển",
                        "values": [
                            {"key": "url", "value": "http://dev.example.com", "enabled": true}
                        ]
                    },
                    {
                        "name": "môi trường sản xuất",
                        "values": [
                            {"key": "url", "value": "https://prod.example.com", "enabled": true}
                        ]
                    }
                ]
                """;

        // Thực hiện phân tích cú pháp
        List<Environment> envs = DevWorkbenchEnvironmentParser.parseDevWorkbenchEnvironments(json);

        // Xác minh kết quả
        assertNotNull(envs);
        assertEquals(envs.size(), 2);

        assertEquals(envs.get(0).getName(), "môi trường phát triển");
        assertEquals(envs.get(0).getVariables().get("url"), "http://dev.example.com");

        assertEquals(envs.get(1).getName(), "môi trường sản xuất");
        assertEquals(envs.get(1).getVariables().get("url"), "https://prod.example.com");
    }

    @Test
    public void testParseDevWorkbenchEnvironments_EmptyValues() {
        // Chuẩn bị dữ liệu thử nghiệm - Danh sách biến trống
        String json = """
                {
                    "name": "Môi trường trống rỗng",
                    "values": []
                }
                """;

        // Thực hiện phân tích cú pháp
        List<Environment> envs = DevWorkbenchEnvironmentParser.parseDevWorkbenchEnvironments(json);

        // Xác minh kết quả
        assertNotNull(envs);
        assertEquals(envs.size(), 1);
        assertEquals(envs.get(0).getName(), "Môi trường trống rỗng");
        assertTrue(envs.get(0).getVariables().isEmpty());
    }

    @Test
    public void testParseDevWorkbenchEnvironments_NoName() {
        // Chuẩn bị dữ liệu thử nghiệm - Không có tên
        String json = """
                {
                    "values": [
                        {"key": "test", "value": "value", "enabled": true}
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        List<Environment> envs = DevWorkbenchEnvironmentParser.parseDevWorkbenchEnvironments(json);

        // Xác minh kết quả
        assertNotNull(envs);
        assertEquals(envs.size(), 1);
        assertEquals(envs.get(0).getName(), "Môi trường không tên");
    }

    @Test
    public void testToDevWorkbenchEnvironmentJson() {
        // Chuẩn bị dữ liệu thử nghiệm
        Environment env = new Environment("Môi trường thử nghiệm");
        env.setId("test-id");
        env.addVariable("apiUrl", "https://api.example.com");
        env.addVariable("apiKey", "secret-key");

        // Thực hiện xuất khẩu
        String json = DevWorkbenchEnvironmentParser.toDevWorkbenchEnvironmentJson(env);

        // Xác minh kết quả
        assertNotNull(json);
        assertTrue(JSONUtil.isTypeJSON(json));

        // phân tíchJSONXác minh nội dung
        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(json);
        assertNotNull(obj.getStr("id"));
        assertEquals(obj.getStr("name"), "Môi trường thử nghiệm");
        assertEquals(obj.getStr("_postman_variable_scope"), "environment");
        assertEquals(obj.getStr("_postman_exported_using"), "Dev Workbench");
        assertNotNull(obj.getStr("_postman_exported_at"));

        // Xác thực các biến
        cn.hutool.json.JSONArray values = obj.getJSONArray("values");
        assertNotNull(values);
        assertEquals(values.size(), 2);

        boolean foundApiUrl = false;
        boolean foundApiKey = false;
        for (Object v : values) {
            cn.hutool.json.JSONObject vObj = (cn.hutool.json.JSONObject) v;
            if ("apiUrl".equals(vObj.getStr("key"))) {
                foundApiUrl = true;
                assertEquals(vObj.getStr("value"), "https://api.example.com");
                assertTrue(vObj.getBool("enabled"));
            } else if ("apiKey".equals(vObj.getStr("key"))) {
                foundApiKey = true;
                assertEquals(vObj.getStr("value"), "secret-key");
                assertTrue(vObj.getBool("enabled"));
            }
        }
        assertTrue(foundApiUrl && foundApiKey);
    }

    @Test
    public void testToDevWorkbenchEnvironmentJson_EmptyVariables() {
        // Chuẩn bị dữ liệu thử nghiệm - Biến trống
        Environment env = new Environment("Môi trường trống rỗng");
        env.setId("empty-id");

        // Thực hiện xuất khẩu
        String json = DevWorkbenchEnvironmentParser.toDevWorkbenchEnvironmentJson(env);

        // Xác minh kết quả
        assertNotNull(json);
        cn.hutool.json.JSONObject obj = JSONUtil.parseObj(json);
        cn.hutool.json.JSONArray values = obj.getJSONArray("values");
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void testRoundTrip() {
        // Sau khi thử nghiệm xuất và sau đó nhập, dữ liệu sẽ vẫn nhất quán
        Environment original = new Environment("Thử nghiệm chuyến đi khứ hồi");
        original.addVariable("var1", "value1");
        original.addVariable("var2", "value2");

        // Xuất khẩu
        String json = DevWorkbenchEnvironmentParser.toDevWorkbenchEnvironmentJson(original);

        // Nhập khẩu
        List<Environment> imported = DevWorkbenchEnvironmentParser.parseDevWorkbenchEnvironments(json);

        // Xác minh
        assertNotNull(imported);
        assertEquals(imported.size(), 1);
        Environment restored = imported.get(0);
        assertEquals(restored.getName(), original.getName());
        assertEquals(restored.getVariables().size(), original.getVariables().size());
        assertEquals(restored.getVariables().get("var1"), "value1");
        assertEquals(restored.getVariables().get("var2"), "value2");
    }
}

