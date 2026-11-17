package com.laker.postman.service.swagger;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.collection.model.CollectionParseResult;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Swagger/OpenAPI Định dạng mục nhập của trình phân tích cú pháp
 * Hỗ trợ Swagger 2.0、OpenAPI 3.0.x và OpenAPI 3.1.x định dạng
 */
@Slf4j
@UtilityClass
public class SwaggerParser {

    /**
     * phân tích Swagger/OpenAPI JSON tập tin và trả về kết quả phân tích cú pháp
     *
     * @param json Swagger/OpenAPI JSON chuỗi
     * @return Kết quả phân tích cú pháp, được trả về nếu phân tích cú pháp thất bại null
     */
    public static CollectionParseResult parseSwagger(String json) {
        try {
            JSONObject root = JSONUtil.parseObj(json);

            // Kiểm tra phiên bản
            String version = detectVersion(root);
            if (version == null) {
                log.error("Không thể nhận raSwagger/OpenAPIđịnh dạng");
                return null;
            }

            log.info("được phát hiện {} định dạng", version);

            // Phân phối tới trình phân tích cú pháp tương ứng theo phiên bản
            if (version.startsWith("Swagger 2.0")) {
                return Swagger2Parser.parse(root);
            } else if (version.startsWith("OpenAPI 3.0") || version.startsWith("OpenAPI 3.1")) {
                return OpenApi3Parser.parse(root);
            }

            return null;
        } catch (Exception e) {
            log.error("phân tíchSwaggerTệp không thành công", e);
            return null;
        }
    }

    /**
     * phát hiện Swagger/OpenAPI Phiên bản
     */
    private static String detectVersion(JSONObject root) {
        if (root.containsKey("swagger")) {
            String ver = root.getStr("swagger");
            if (ver.startsWith("2")) {
                return "Swagger 2.0";
            }
        } else if (root.containsKey("openapi")) {
            String ver = root.getStr("openapi");
            if (ver.startsWith("3.0")) {
                return "OpenAPI 3.0.x";
            } else if (ver.startsWith("3.1")) {
                return "OpenAPI 3.1.x";
            }
        }
        return null;
    }
}
