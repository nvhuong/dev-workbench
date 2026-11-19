package com.nvhuong.devworkbench.service.devworkbench;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nvhuong.devworkbench.model.Environment;

import java.util.*;

/**
 * DevWorkbenchTrình phân tích cú pháp biến môi trường
 * Chịu trách nhiệm phân tích và xuất khẩuDevWorkbenchCác biến môi trường (v2.1định dạng)
 */
public class DevWorkbenchEnvironmentParser {

    /**
     * phân tíchDevWorkbenchcác biến môi trườngJSON，Chuyển đổi sang Environmentdanh sách
     */
    public static List<Environment> parseDevWorkbenchEnvironments(String json) {
        List<Environment> result = new ArrayList<>();
        // Hỗ trợ một đối tượng hoặc mảng
        if (JSONUtil.isTypeJSONArray(json)) {
            JSONArray arr = JSONUtil.parseArray(json);
            for (Object obj : arr) {
                parseSingleEnvObj((JSONObject) obj, result);
            }
        } else {
            JSONObject envObj = JSONUtil.parseObj(json);
            parseSingleEnvObj(envObj, result);
        }
        return result;
    }

    /**
     * Giải quyết một đối tượng biến môi trường duy nhất
     */
    private static void parseSingleEnvObj(JSONObject envObj, List<Environment> result) {
        String name = envObj.getStr("name", "Môi trường không tên");
        Environment env = new Environment(name);
        env.setId(UUID.randomUUID().toString());
        JSONArray values = envObj.getJSONArray("values");
        if (values != null) {
            for (Object v : values) {
                JSONObject vObj = (JSONObject) v;
                String key = vObj.getStr("key", "");
                String value = vObj.getStr("value", "");
                boolean enabled = vObj.getBool("enabled", true);
                if (!key.isEmpty() && enabled) {
                    env.addVariable(key, value);
                }
            }
        }
        result.add(env);
    }

    /**
     * Xuất một môi trường duy nhất dưới dạng DevWorkbenchcác biến môi trườngJSONchuỗi
     */
    public static String toDevWorkbenchEnvironmentJson(Environment env) {
        JSONObject obj = new JSONObject();
        obj.put("id", UUID.randomUUID().toString());
        obj.put("name", env.getName());
        JSONArray values = new JSONArray();
        if (env.getVariables() != null) {
            for (Map.Entry<String, String> entry : env.getVariables().entrySet()) {
                JSONObject v = new JSONObject();
                v.put("key", entry.getKey());
                v.put("value", entry.getValue());
                v.put("enabled", true);
                values.add(v);
            }
        }
        obj.put("values", values);
        obj.put("_postman_variable_scope", "environment");
        obj.put("_postman_exported_at", java.time.ZonedDateTime.now().toString());
        obj.put("_postman_exported_using", "Dev Workbench");
        return obj.toStringPretty();
    }
}

