package com.nvhuong.devworkbench.service.devworkbench;

import com.nvhuong.devworkbench.collection.importer.devworkbench.DevWorkbenchCollectionParser;
import com.nvhuong.devworkbench.collection.model.CollectionParseResult;
import com.nvhuong.devworkbench.request.model.AuthApiKeyPlacement;
import com.nvhuong.devworkbench.request.model.AuthType;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import com.nvhuong.devworkbench.request.model.RequestBodyTypes;
import org.testng.annotations.Test;

import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_FORM_DATA;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_RAW;
import static org.testng.Assert.*;

/**
 * DevWorkbenchCollectionParser Kiểm tra đơn vị
 * Kiểm tra hoàn tất DevWorkbench Collection chức năng phân tích cú pháp
 */
public class DevWorkbenchCollectionParserTest {

    @Test
    public void testParseDevWorkbenchCollection_BasicCollection() {
        // Chuẩn bị dữ liệu thử nghiệm - Cơ bản Collection
        String json = """
                {
                    "info": {
                        "name": "Test Collection",
                        "schema": "https://schema.getdevworkbench.com/json/collection/v2.1.0/collection.json"
                    },
                    "item": [
                        {
                            "name": "Get Users",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com/users"
                            }
                        }
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả
        assertNotNull(result);
        assertNotNull(result.getGroup());
        assertEquals(result.getGroup().getName(), "Test Collection");
        assertEquals(result.getChildren().size(), 1);
    }

    @Test
    public void testParseDevWorkbenchCollection_WithFolder() {
        // Chuẩn bị dữ liệu thử nghiệm - Chứa các thư mục Collection
        String json = """
                {
                    "info": {
                        "name": "API Collection",
                        "schema": "https://schema.getdevworkbench.com/json/collection/v2.1.0/collection.json"
                    },
                    "item": [
                        {
                            "name": "User API",
                            "item": [
                                {
                                    "name": "Get User",
                                    "request": {
                                        "method": "GET",
                                        "url": "https://api.example.com/user/1"
                                    }
                                },
                                {
                                    "name": "Create User",
                                    "request": {
                                        "method": "POST",
                                        "url": "https://api.example.com/user"
                                    }
                                }
                            ]
                        }
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        // Xác minh thư mục
        var folderNode = result.getChildren().get(0);
        assertTrue(folderNode.isGroup());
        assertEquals(folderNode.asGroup().getName(), "User API");
        assertEquals(folderNode.getChildren().size(), 2);
    }

    @Test
    public void testParseDevWorkbenchCollection_WithDigestAuth() {
        String json = """
                {
                    "info": {
                        "name": "Digest Collection"
                    },
                    "item": [
                        {
                            "name": "Digest Request",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com/digest",
                                "auth": {
                                    "type": "digest",
                                    "digest": [
                                        {"key": "username", "value": "digest-user"},
                                        {"key": "password", "value": "digest-pass"}
                                    ]
                                }
                            }
                        }
                    ]
                }
                """;

        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);
        var request = result.getChildren().get(0).asRequest();
        assertEquals(request.getAuthType(), AuthType.DIGEST.getConstant());
        assertEquals(request.getAuthUsername(), "digest-user");
        assertEquals(request.getAuthPassword(), "digest-pass");
    }

    @Test
    public void testParseDevWorkbenchCollection_WithApiKeyAuth() {
        String json = """
                {
                    "info": {
                        "name": "API Key Collection"
                    },
                    "item": [
                        {
                            "name": "API Key Request",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com/keyed",
                                "auth": {
                                    "type": "apikey",
                                    "apikey": [
                                        {"key": "key", "value": "X-API-Key"},
                                        {"key": "value", "value": "secret"},
                                        {"key": "in", "value": "header"}
                                    ]
                                }
                            }
                        }
                    ]
                }
                """;

        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);
        var request = result.getChildren().get(0).asRequest();
        assertEquals(request.getAuthType(), AuthType.API_KEY.getConstant());
        assertEquals(request.getAuthApiKeyName(), "X-API-Key");
        assertEquals(request.getAuthApiKeyValue(), "secret");
        assertEquals(request.getAuthApiKeyPlacement(), AuthApiKeyPlacement.HEADER.getConstant());
    }

    @Test
    public void testParseDevWorkbenchCollection_ComplexStructure() {
        // Chuẩn bị dữ liệu thử nghiệm - Cấu trúc lồng nhau phức tạp
        String json = """
                {
                    "info": {
                        "name": "Complex Collection"
                    },
                    "item": [
                        {
                            "name": "Auth",
                            "item": [
                                {
                                    "name": "Login",
                                    "request": {
                                        "method": "POST",
                                        "url": "https://api.example.com/login",
                                        "auth": {
                                            "type": "basic",
                                            "basic": [
                                                {"key": "username", "value": "admin"},
                                                {"key": "password", "value": "secret"}
                                            ]
                                        }
                                    }
                                }
                            ]
                        },
                        {
                            "name": "Direct Request",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com/public"
                            }
                        }
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 2);

        // Xác minh nút con đầu tiên (thư mục)
        var authFolder = result.getChildren().get(0);
        assertTrue(authFolder.isGroup());
        assertEquals(authFolder.asGroup().getName(), "Auth");
        assertEquals(authFolder.getChildren().size(), 1);

        // Xác minh nút con thứ hai (yêu cầu)
        var directRequest = result.getChildren().get(1);
        assertTrue(directRequest.isRequest());
    }

    @Test
    public void testParseDevWorkbenchCollection_WithQueryParams() {
        // Chuẩn bị dữ liệu thử nghiệm - Yêu cầu với các tham số truy vấn
        String json = """
                {
                    "info": {
                        "name": "Query Test"
                    },
                    "item": [
                        {
                            "name": "Search API",
                            "request": {
                                "method": "GET",
                                "url": {
                                    "raw": "https://api.example.com/search?q=test&limit=10",
                                    "query": [
                                        {"key": "q", "value": "test", "disabled": false},
                                        {"key": "limit", "value": "10", "disabled": false}
                                    ]
                                }
                            }
                        }
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        var requestNode = result.getChildren().get(0);
        assertTrue(requestNode.isRequest());

        // Xác minh HttpRequestItem
        var req = requestNode.asRequest();
        assertEquals(req.getName(), "Search API");
        assertEquals(req.getMethod(), "GET");
        assertEquals(req.getUrl(), "https://api.example.com/search?q=test&limit=10");
        assertNotNull(req.getParamsList());
        assertEquals(req.getParamsList().size(), 2);
    }

    @Test
    public void testParseDevWorkbenchCollection_WithPathVariables() {
        String json = """
                {
                    "info": {
                        "name": "Path Variable Test"
                    },
                    "item": [
                        {
                            "name": "Get User",
                            "request": {
                                "method": "GET",
                                "url": {
                                    "raw": "https://api.example.com/users/:userId/orders/:orderId",
                                    "host": ["api", "example", "com"],
                                    "path": ["users", ":userId", "orders", ":orderId"],
                                    "variable": [
                                        {"key": "userId", "value": "42", "description": "User id"},
                                        {"key": "orderId", "value": "A-100"}
                                    ]
                                }
                            }
                        }
                    ]
                }
                """;

        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        assertNotNull(result);
        HttpRequestItem req = result.getChildren().get(0).asRequest();
        assertEquals(req.getUrl(), "https://api.example.com/users/:userId/orders/:orderId");
        assertEquals(req.getPathVariablesList().size(), 2);
        assertEquals(req.getPathVariablesList().get(0).getKey(), "userId");
        assertEquals(req.getPathVariablesList().get(0).getValue(), "42");
        assertEquals(req.getPathVariablesList().get(0).getDescription(), "User id");
        assertEquals(req.getPathVariablesList().get(1).getKey(), "orderId");
    }

    @Test
    public void testParseDevWorkbenchCollection_WithHeaders() {
        // Chuẩn bị dữ liệu thử nghiệm - Yêu cầu với tiêu đề yêu cầu
        String json = """
                {
                    "info": {
                        "name": "Header Test"
                    },
                    "item": [
                        {
                            "name": "API Request",
                            "request": {
                                "method": "POST",
                                "url": "https://api.example.com/data",
                                "header": [
                                    {"key": "Content-Type", "value": "application/json", "disabled": false},
                                    {"key": "Authorization", "value": "Bearer token", "disabled": false}
                                ]
                            }
                        }
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả
        assertNotNull(result);
        var requestNode = result.getChildren().get(0);
        var req = requestNode.asRequest();

        assertNotNull(req.getHeadersList());
        assertEquals(req.getHeadersList().size(), 2);
        assertEquals(req.getHeadersList().get(0).getKey(), "Content-Type");
        assertEquals(req.getHeadersList().get(0).getValue(), "application/json");
    }

    @Test
    public void testParseDevWorkbenchCollection_WithScripts() {
        // Chuẩn bị dữ liệu thử nghiệm - Các yêu cầu theo kịch bản
        String json = """
                {
                    "info": {
                        "name": "Script Test"
                    },
                    "item": [
                        {
                            "name": "API With Scripts",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com/data"
                            },
                            "event": [
                                {
                                    "listen": "prerequest",
                                    "script": {
                                        "exec": ["console.log('before');"]
                                    }
                                },
                                {
                                    "listen": "test",
                                    "script": {
                                        "exec": ["pm.test('Status is 200', function() {", "    pm.response.to.have.status(200);", "});"]
                                    }
                                }
                            ]
                        }
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả
        assertNotNull(result);
        var requestNode = result.getChildren().get(0);
        var req = requestNode.asRequest();

        assertNotNull(req.getPrescript());
        assertTrue(req.getPrescript().contains("console.log('before')"));

        assertNotNull(req.getPostscript());
        assertTrue(req.getPostscript().contains("Status is 200"));
    }

    @Test
    public void testParseDevWorkbenchCollection_InvalidJson() {
        // Chuẩn bị dữ liệu thử nghiệm - Không hợp lệ JSON
        String json = "invalid json";

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả - nên được trả lại null
        assertNull(result);
    }

    @Test
    public void testParseDevWorkbenchCollection_MissingInfo() {
        // Chuẩn bị dữ liệu thử nghiệm - thiếu info lĩnh vực
        String json = """
                {
                    "item": [
                        {
                            "name": "Test",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com"
                            }
                        }
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả - nên được trả lại null
        assertNull(result);
    }

    @Test
    public void testParseDevWorkbenchCollection_MissingItem() {
        // Chuẩn bị dữ liệu thử nghiệm - thiếu item lĩnh vực
        String json = """
                {
                    "info": {
                        "name": "Test Collection"
                    }
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả - nên được trả lại null
        assertNull(result);
    }

    @Test
    public void testParseDevWorkbenchCollection_EmptyCollection() {
        // Chuẩn bị dữ liệu thử nghiệm - Trống Collection
        String json = """
                {
                    "info": {
                        "name": "Empty Collection"
                    },
                    "item": []
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả
        assertNotNull(result);
        assertEquals(result.getGroup().getName(), "Empty Collection");
        assertEquals(result.getChildren().size(), 0);
    }

    @Test
    public void testParseDevWorkbenchCollection_NoCollectionName() {
        // Chuẩn bị dữ liệu thử nghiệm - Không collection tên
        String json = """
                {
                    "info": {
                        "schema": "https://schema.getdevworkbench.com/json/collection/v2.1.0/collection.json"
                    },
                    "item": [
                        {
                            "name": "Test",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com"
                            }
                        }
                    ]
                }
                """;

        // Thực hiện phân tích cú pháp
        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        // Xác minh kết quả - Nên sử dụng tên mặc định
        assertNotNull(result);
        assertEquals(result.getGroup().getName(), "DevWorkbench");
    }

    @Test
    public void testParseDevWorkbenchCollection_RequestStringAndFolderVariables() {
        String json = """
                {
                  "info": {
                    "name": "String Request Collection",
                    "schema": "https://schema.getdevworkbench.com/collection/v2.1.0/collection.json"
                  },
                  "item": [
                    {
                      "name": "Folder A",
                      "variable": [
                        {"key": "baseUrl", "value": "https://api.example.com"}
                      ],
                      "item": [
                        {
                          "name": "Ping",
                          "request": "https://api.example.com/ping"
                        }
                      ]
                    }
                  ]
                }
                """;

        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        var folder = result.getChildren().get(0);
        assertTrue(folder.isGroup());
        assertEquals(folder.asGroup().getVariables().size(), 1);
        assertEquals(folder.asGroup().getVariables().get(0).getKey(), "baseUrl");
        assertEquals(folder.getChildren().get(0).asRequest().getUrl(), "https://api.example.com/ping");
    }

    @Test
    public void testParseDevWorkbenchCollection_ScriptStringAndHeaderString() {
        String json = """
                {
                  "info": {
                    "name": "Header String Collection",
                    "schema": "https://schema.getdevworkbench.com/collection/v2.1.0/collection.json"
                  },
                  "item": [
                    {
                      "name": "Create User",
                      "event": [
                        {
                          "listen": "prerequest",
                          "script": {
                            "exec": "console.log('single line');"
                          }
                        }
                      ],
                      "request": {
                        "method": "POST",
                        "header": "Content-Type: application/json\\nX-Trace: abc",
                        "url": {
                          "protocol": "https",
                          "host": ["api", "example", "com"],
                          "path": ["users"]
                        },
                        "body": {
                          "mode": "raw",
                          "raw": "{\\"name\\":\\"Alice\\"}"
                        }
                      }
                    }
                  ]
                }
                """;

        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);
        assertNotNull(result);

        var request = result.getChildren().get(0).asRequest();
        assertEquals(request.getUrl(), "https://api.example.com/users");
        assertEquals(request.getBodyType(), BODY_TYPE_RAW);
        assertEquals(request.getPrescript(), "console.log('single line');\n");
        assertTrue(request.getHeadersList().stream().anyMatch(h -> "Content-Type".equals(h.getKey())));
        assertTrue(request.getHeadersList().stream().anyMatch(h -> "X-Trace".equals(h.getKey()) && "abc".equals(h.getValue())));
    }

    @Test
    public void testParseDevWorkbenchCollection_FormDataFileArrayAndGraphql() {
        String json = """
                {
                  "info": {
                    "name": "Advanced Body Collection",
                    "schema": "https://schema.getdevworkbench.com/collection/v2.1.0/collection.json"
                  },
                  "item": [
                    {
                      "name": "Upload Avatar",
                      "request": {
                        "method": "POST",
                        "url": "https://api.example.com/upload",
                        "body": {
                          "mode": "formdata",
                          "formdata": [
                            {"key": "avatar", "type": "file", "src": ["./front.png", "./back.png"]}
                          ]
                        }
                      }
                    },
                    {
                      "name": "GraphQL Query",
                      "request": {
                        "method": "POST",
                        "url": "https://api.example.com/graphql",
                        "body": {
                          "mode": "graphql",
                          "graphql": {
                            "query": "query Ping { ping }",
                            "variables": "{\\"id\\":1}"
                          }
                        }
                      }
                    }
                  ]
                }
                """;

        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);
        assertNotNull(result);
        assertEquals(result.getChildren().size(), 2);

        var upload = result.getChildren().get(0).asRequest();
        assertEquals(upload.getBodyType(), BODY_TYPE_FORM_DATA);
        assertEquals(upload.getFormDataList().size(), 2);
        assertEquals(upload.getFormDataList().get(0).getKey(), "avatar");
        assertEquals(upload.getFormDataList().get(0).getValue(), "./front.png");
        assertEquals(upload.getFormDataList().get(1).getKey(), "avatar");
        assertEquals(upload.getFormDataList().get(1).getValue(), "./back.png");

        var graphql = result.getChildren().get(1).asRequest();
        assertEquals(graphql.getBodyType(), BODY_TYPE_RAW);
        assertTrue(graphql.getBody().contains("query Ping"));
    }

    @Test
    public void testParseDevWorkbenchCollection_ExamplesGetUniqueIds() {
        String json = """
                {
                    "info": {"name": "Examples", "schema": "https://schema.getdevworkbench.com/json/collection/v2.1.0/collection.json"},
                    "item": [{
                        "name": "Get User",
                        "request": {"method": "GET", "url": "https://api.example.com/user"},
                        "response": [
                            {"name": "OK", "code": 200, "_devworkbench_previewlanguage": "json", "body": "{}"},
                            {"name": "Not Found", "code": 404, "_devworkbench_previewlanguage": "json", "body": "{}"}
                        ]
                    }]
                }
                """;

        CollectionParseResult result = DevWorkbenchCollectionParser.parseDevWorkbenchCollection(json);

        var responses = result.getChildren().get(0).asRequest().getResponse();
        assertEquals(responses.size(), 2);
        assertNotEquals(responses.get(0).getId(), "json");
        assertNotEquals(responses.get(0).getId(), responses.get(1).getId());
        assertEquals(responses.get(0).getPreviewLanguage(), "json");
    }
}
