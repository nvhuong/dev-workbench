package com.laker.postman.service.ideahttp;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.Environment;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * IntelliJ IDEA HTTP Client Trình phân tích cú pháp biến môi trường
 * Chịu trách nhiệm phân tích IntelliJ IDEA HTTP Client của http-client.env.json định dạng
 */
@UtilityClass
public class IntelliJHttpEnvParser {

    /**
     * phân tích IntelliJ IDEA HTTP Client các biến môi trườngJSON，Chuyển đổi sang Environmentdanh sách
     * <p>
     * Ví dụ về các định dạng được hỗ trợ: .
     * {
     * "dev": {
     * "host": "localhost",
     * "port": "8080"
     * },
     * "prod": {
     * "host": "example.com",
     * "port": "443"
     * }
     * }
     *
     * @param json IntelliJ IDEA HTTP Client các biến môi trườngJSONchuỗi
     * @return Environmentdanh sách
     */
    public static List<Environment> parseIntelliJEnvironments(String json) {
        List<Environment> result = new ArrayList<>();

        try {
            // tiền xử lýJSON: Loại bỏ các lỗi cú pháp có thể xảy ra (chẳng hạn như dấu chấm phẩy ở cuối dòng)
            String cleanedJson = preprocessJson(json);

            JSONObject rootObj = JSONUtil.parseObj(cleanedJson);

            // Lặp lại qua từng môi trường (mỗi môi trường ở cấp cao nhấtkeyTất cả đều giống nhau về môi trường)
            for (Map.Entry<String, Object> entry : rootObj.entrySet()) {
                String envName = entry.getKey();
                Object envValue = entry.getValue();

                // Bỏ qua các giá trị không phải là loại đối tượng
                if (!(envValue instanceof JSONObject envVariables)) {
                    continue;
                }

                Environment env = new Environment(envName);
                env.setId("env-" + UUID.randomUUID());

                // Thêm tất cả các cặp khóa-giá trị trong môi trường này dưới dạng biến môi trường
                for (Map.Entry<String, Object> varEntry : envVariables.entrySet()) {
                    String key = varEntry.getKey();
                    Object value = varEntry.getValue();

                    // Chuyển đổi giá trị thành chuỗi
                    String valueStr = value != null ? value.toString() : "";
                    env.addVariable(key, valueStr);
                }

                result.add(env);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse IntelliJ IDEA HTTP environment file: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * tiền xử lýJSONChuỗi, sửa các lỗi cú pháp thường gặp
     * Ví dụ: Thay thế dấu chấm phẩy sau giá trị chuỗi bằng dấu phẩy "value"; -> "value",
     */
    private static String preprocessJson(String json) {
        // Thay thế dấu chấm phẩy ngay sau chuỗi bằng dấu phẩy (đây là IntelliJ IDEA HTTPcó thể có lỗi trong tệp)
        // Mô hình phù hợp: "Bất kỳ nội dung nào";  Thay thế bằng : "Bất kỳ nội dung nào",
        return json.replaceAll("\";", "\",");
    }

    /**
     * Xuất một môi trường duy nhất dưới dạng  IntelliJ IDEA HTTP Client Định dạng biến môi trường
     *
     * @param env Environmentđối tượng
     * @return IntelliJ IDEA HTTP Client định dạngJSONchuỗi
     */
    public static String toIntelliJEnvironmentJson(Environment env) {
        JSONObject rootObj = new JSONObject();
        JSONObject envObj = new JSONObject();

        if (env.getVariables() != null) {
            envObj.putAll(env.getVariables());
        }

        rootObj.set(env.getName(), envObj);
        return rootObj.toStringPretty();
    }

    /**
     * Xuất nhiều môi trường dưới dạng IntelliJ IDEA HTTP Client Định dạng biến môi trường
     *
     * @param environments Environmentdanh sách
     * @return IntelliJ IDEA HTTP Client định dạngJSONchuỗi
     */
    public static String toIntelliJEnvironmentsJson(List<Environment> environments) {
        JSONObject rootObj = new JSONObject();

        for (Environment env : environments) {
            JSONObject envObj = new JSONObject();
            if (env.getVariables() != null) {
                envObj.putAll(env.getVariables());
            }
            rootObj.set(env.getName(), envObj);
        }

        return rootObj.toStringPretty();
    }
}
