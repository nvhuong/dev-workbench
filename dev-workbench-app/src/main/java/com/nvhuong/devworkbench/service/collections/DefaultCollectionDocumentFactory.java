package com.nvhuong.devworkbench.service.collections;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import com.nvhuong.devworkbench.collection.model.CollectionNode;
import com.nvhuong.devworkbench.collection.model.RequestGroup;
import com.nvhuong.devworkbench.http.request.HttpRequestFactory;
import com.nvhuong.devworkbench.request.model.HttpFormData;
import com.nvhuong.devworkbench.request.model.HttpFormUrlencoded;
import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.HttpParam;
import com.nvhuong.devworkbench.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

import static com.nvhuong.devworkbench.request.defaults.HttpRequestDefaults.APPLICATION_JSON;
import static com.nvhuong.devworkbench.request.defaults.HttpRequestDefaults.CONTENT_TYPE;
import static com.nvhuong.devworkbench.request.model.RequestBodyTypes.BODY_TYPE_RAW;

@UtilityClass
public class DefaultCollectionDocumentFactory {
    private static final String BASIC_HTTP_EXAMPLES_GROUP_NAME = "Basic HTTP Examples";
    private static final String SCRIPT_EXAMPLES_EN_GROUP_NAME = "Script Examples (EN)";
    private static final String SCRIPT_EXAMPLES_VI_GROUP_NAME = "Ví dụ tập lệnh (Tiếng Việt)";

    public static final String HTTPBIN_BASE_URL = "https://httpbin.org";
    public static final String HTTPBIN_POST_URL = HTTPBIN_BASE_URL + "/post";

    public CollectionDocument create() {
        // Bộ sưu tập mặc định là dữ liệu miền và không được tạo trực tiếp Swing nút; cách này CLI、Xuất nhập khẩu và tương lai JavaFX có thể sử dụng lại cùng một dữ liệu mẫu.
        return new CollectionDocument(List.of(
                createBasicHttpExamples(),
                createEnglishScriptExamples(),
                createVietnameseScriptExamples()
        ));
    }

    private CollectionNode createEnglishScriptExamples() {
        RequestGroup group = new RequestGroup(SCRIPT_EXAMPLES_EN_GROUP_NAME);
        group.setDescription("""
                Run requests 1 to 6 in order for the variable workflow examples.
                Requests 7 and 8 are stream-oriented checks for WebSocket and SSE callback scripts.
                Run request 1 before requests 7 and 8 so they can reuse the prepared environment values.

                Request 1 sets environment variables in script.
                Request 2 reuses them in query params, headers, body, and test script.
                Request 2 also creates execution-scoped variables for the current run.
                Request 3 creates an orderId for the current run and stores it in pm.variables.
                Request 4 reuses that orderId across params, headers, URL path, and test script.
                Request 5 extracts an orderId from the response body and stores it in pm.variables.
                Request 6 reuses that extracted orderId in the next request.
                Requests 7 and 8 verify that both pm.environment and pm.variables remain available in WebSocket and SSE callbacks.

                Scope:
                - pm.environment: shared across requests and can be reused later
                - pm.variables: shared within the current run context and reset after the run ends
                - pm.iterationData: current CSV/JSON row in a data-driven run
                - Authorization priority: script-defined Authorization > existing Authorization header > auth tab generated Authorization
                - Removing Authorization in pre-script does not disable auth tab; if no Authorization remains, the auth tab configuration is applied at send time
                """);
        CollectionNode groupNode = CollectionNode.group(group);

        HttpRequestItem setup = HttpRequestFactory.createDefaultRequest();
        setup.setName("1. Setup Variables");
        setup.setDescription("Creates shared environment variables and one execution-scoped variable in the pre-request script.");
        setup.setMethod("GET");
        setup.setUrl("{{baseUrl}}/anything/setup");
        setup.setHeadersList(withExtraHeaders(setup,
                new HttpHeader(true, "X-Setup-Trace", "{{setupTraceId}}")));
        setup.setParamsList(List.of(
                new HttpParam(true, "tenant", "{{tenantId}}")
        ));
        setup.setPrescript("""
                pm.environment.set('baseUrl', 'https://httpbin.org');
                pm.environment.set('tenantId', 'team-alpha');
                pm.environment.set('userId', 'user-1001');
                pm.environment.set('keyword', 'dev-workbench');

                pm.variables.set('setupTraceId', 'setup-' + Date.now());
                console.log('Environment ready:', JSON.stringify(pm.environment.toObject()));
                """);
        setup.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Setup request is successful', function () {
                    pm.response.to.have.status(200);
                    pm.expect(jsonData.args.tenant).to.eql(pm.environment.get('tenantId'));
                });
                """);
        addRequestChild(groupNode, setup);

        HttpRequestItem reuse = HttpRequestFactory.createDefaultRequest();
        reuse.setName("2. Reuse Variables");
        reuse.setDescription("Reuses environment variables in params, headers, body, and script. Execution-scoped variables are created in this run and reused immediately.");
        reuse.setMethod("POST");
        reuse.setUrl(HTTPBIN_POST_URL);
        reuse.setBodyType(BODY_TYPE_RAW);
        reuse.setHeadersList(withExtraHeaders(reuse,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON),
                new HttpHeader(true, "X-Tenant-Id", "{{tenantId}}"),
                new HttpHeader(true, "X-Trace-Id", "{{traceId}}")));
        reuse.setParamsList(List.of(
                new HttpParam(true, "keyword", "{{keyword}}"),
                new HttpParam(true, "trace", "{{traceId}}")
        ));
        reuse.setBody("""
                {
                  "tenantId": "{{tenantId}}",
                  "userId": "{{userId}}",
                  "traceId": "{{traceId}}",
                  "note": "{{note}}"
                }""");
        reuse.setPrescript("""
                pm.variables.set('traceId', 'trace-' + Date.now());
                pm.variables.set('note', 'execution variables work in body, header, and params');

                pm.request.headers.upsert({
                    key: 'X-Script-Flag',
                    value: 'ready'
                });
                """);
        reuse.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Reuse request is successful', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('Environment variables are reused', function () {
                    pm.expect(jsonData.json.tenantId).to.eql(pm.environment.get('tenantId'));
                    pm.expect(jsonData.json.userId).to.eql(pm.environment.get('userId'));
                    pm.expect(jsonData.args.keyword).to.eql(pm.environment.get('keyword'));
                });
                pm.test('Execution variables are reused in the current run', function () {
                    pm.expect(jsonData.args.trace).to.eql(pm.variables.get('traceId'));
                    pm.expect(jsonData.json.traceId).to.eql(pm.variables.get('traceId'));
                    pm.expect(jsonData.json.note).to.eql(pm.variables.get('note'));
                    pm.expect(jsonData.headers['X-Trace-Id']).to.eql(pm.variables.get('traceId'));
                });
                console.log('traceId =', pm.variables.get('traceId'));
                """);
        addRequestChild(groupNode, reuse);

        HttpRequestItem createOrder = HttpRequestFactory.createDefaultRequest();
        createOrder.setName("3. Create Order Context");
        createOrder.setDescription("Generates or accepts an orderId for the current run, echoes it through httpbin, and stores it in pm.variables for the next request.");
        createOrder.setMethod("POST");
        createOrder.setUrl(HTTPBIN_POST_URL);
        createOrder.setBodyType(BODY_TYPE_RAW);
        createOrder.setHeadersList(withExtraHeaders(createOrder,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON),
                new HttpHeader(true, "X-Order-Id", "{{pendingOrderId}}")));
        createOrder.setParamsList(List.of(
                new HttpParam(true, "stage", "create-order"),
                new HttpParam(true, "pendingOrderId", "{{pendingOrderId}}")
        ));
        createOrder.setBody("""
                {
                  "orderId": "{{pendingOrderId}}",
                  "tenantId": "{{tenantId}}",
                  "source": "default-script-example"
                }""");
        createOrder.setPrescript("""
                var incomingOrderId = pm.iterationData.get('orderId');
                var pendingOrderId = incomingOrderId || ('order-' + Date.now());
                pm.variables.set('pendingOrderId', pendingOrderId);
                console.log('pending orderId =', pendingOrderId);
                """);
        createOrder.setPostscript("""
                var jsonData = pm.response.json();
                var orderId = jsonData.json.orderId;
                pm.variables.set('orderId', orderId);
                pm.test('Order context request is successful', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('orderId is stored for the next request', function () {
                    pm.expect(orderId).to.eql(pm.variables.get('pendingOrderId'));
                    pm.expect(pm.variables.get('orderId')).to.eql(orderId);
                });
                """);
        addRequestChild(groupNode, createOrder);

        HttpRequestItem reuseOrder = HttpRequestFactory.createDefaultRequest();
        reuseOrder.setName("4. Reuse OrderId Across Requests");
        reuseOrder.setDescription("Consumes the orderId created by request 3. This is the default two-request chain for Functional and Performance testing.");
        reuseOrder.setMethod("GET");
        reuseOrder.setUrl(HTTPBIN_BASE_URL + "/anything/orders/{{orderId}}");
        reuseOrder.setHeadersList(withExtraHeaders(reuseOrder,
                new HttpHeader(true, "X-Order-Id", "{{orderId}}")));
        reuseOrder.setParamsList(List.of(
                new HttpParam(true, "orderId", "{{orderId}}"),
                new HttpParam(true, "tenantId", "{{tenantId}}")
        ));
        reuseOrder.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Reused orderId request is successful', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('orderId passes through to the next request', function () {
                    pm.expect(jsonData.args.orderId).to.eql(pm.variables.get('orderId'));
                    pm.expect(jsonData.headers['X-Order-Id']).to.eql(pm.variables.get('orderId'));
                    pm.expect(jsonData.url).to.include('/orders/' + pm.variables.get('orderId'));
                });
                console.log('reused orderId =', pm.variables.get('orderId'));
                """);
        addRequestChild(groupNode, reuseOrder);

        HttpRequestItem extractOrder = HttpRequestFactory.createDefaultRequest();
        extractOrder.setName("5. Extract OrderId From Response");
        extractOrder.setDescription("Simulates creating an order, then extracts the echoed orderId from the response body into pm.variables for the next request.");
        extractOrder.setMethod("POST");
        extractOrder.setUrl(HTTPBIN_POST_URL);
        extractOrder.setBodyType(BODY_TYPE_RAW);
        extractOrder.setHeadersList(withExtraHeaders(extractOrder,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON)));
        extractOrder.setBody("""
                {
                  "clientOrderId": "{{responseOrderCandidate}}",
                  "tenantId": "{{tenantId}}",
                  "scenario": "extract-from-response"
                }""");
        extractOrder.setPrescript("""
                pm.variables.set('responseOrderCandidate', 'resp-order-' + Date.now());
                console.log('response order candidate =', pm.variables.get('responseOrderCandidate'));
                """);
        extractOrder.setPostscript("""
                var jsonData = pm.response.json();
                var extractedOrderId = jsonData.json.clientOrderId;
                pm.variables.set('responseOrderId', extractedOrderId);
                pm.test('Response extraction request is successful', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('orderId is extracted from response JSON', function () {
                    pm.expect(extractedOrderId).to.eql(pm.variables.get('responseOrderCandidate'));
                    pm.expect(pm.variables.get('responseOrderId')).to.eql(extractedOrderId);
                });
                """);
        addRequestChild(groupNode, extractOrder);

        HttpRequestItem reuseExtractedOrder = HttpRequestFactory.createDefaultRequest();
        reuseExtractedOrder.setName("6. Reuse Extracted OrderId");
        reuseExtractedOrder.setDescription("Consumes the orderId extracted by request 5. Use this pair when you want a response-body extraction workflow.");
        reuseExtractedOrder.setMethod("GET");
        reuseExtractedOrder.setUrl(HTTPBIN_BASE_URL + "/anything/orders/extracted/{{responseOrderId}}");
        reuseExtractedOrder.setHeadersList(withExtraHeaders(reuseExtractedOrder,
                new HttpHeader(true, "X-Extracted-Order-Id", "{{responseOrderId}}")));
        reuseExtractedOrder.setParamsList(List.of(
                new HttpParam(true, "extractedOrderId", "{{responseOrderId}}"),
                new HttpParam(true, "source", "response-body")
        ));
        reuseExtractedOrder.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Reused extracted orderId request is successful', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('Extracted orderId passes through to the next request', function () {
                    pm.expect(jsonData.args.extractedOrderId).to.eql(pm.variables.get('responseOrderId'));
                    pm.expect(jsonData.headers['X-Extracted-Order-Id']).to.eql(pm.variables.get('responseOrderId'));
                    pm.expect(jsonData.url).to.include('/orders/extracted/' + pm.variables.get('responseOrderId'));
                });
                console.log('reused extracted orderId =', pm.variables.get('responseOrderId'));
                """);
        addRequestChild(groupNode, reuseExtractedOrder);

        HttpRequestItem websocket = HttpRequestFactory.createDefaultWebSocketRequest();
        websocket.setName("7. WebSocket Workflow Check");
        websocket.setDescription("Run request 1 first. Then click Connect and send the default message once if needed. The callback-thread checks focus on whether pm.environment and pm.variables stay available after WebSocket messages arrive.");
        websocket.setBodyType(BODY_TYPE_RAW);
        websocket.setBody("""
                {
                  "type": "context-check",
                  "source": "dev-workbench-websocket-example",
                  "message": "send-once-after-connect"
                }""");
        websocket.setPrescript("""
                pm.variables.set('streamTraceId', 'ws-' + Date.now());
                pm.variables.set('streamScenario', 'websocket-context');
                pm.variables.set('streamTenantId', pm.environment.get('tenantId') || '');
                console.log('WebSocket traceId =', pm.variables.get('streamTraceId'));
                console.log('tenantId =', pm.environment.get('tenantId'));
                console.log('Connect first, then send the default message once.');
                """);
        websocket.setPostscript("""
                var messageText = pm.response.text() || '';
                pm.variables.set('lastWebSocketMessage', messageText);
                pm.test('WebSocket keeps execution variables across message callbacks', function () {
                    pm.expect(pm.variables.get('streamTraceId')).to.exist();
                    pm.expect(pm.variables.get('streamScenario')).to.eql('websocket-context');
                    pm.expect(pm.variables.get('streamTenantId')).to.eql(pm.environment.get('tenantId'));
                });
                pm.test('WebSocket callback can still read environment values', function () {
                    pm.expect(pm.environment.get('tenantId')).to.exist();
                    pm.expect(pm.environment.get('userId')).to.exist();
                });
                pm.test('WebSocket callback receives non-empty text', function () {
                    pm.expect(messageText.length > 0).to.eql(true);
                });
                console.log('last websocket message =', messageText);
                """);
        addRequestChild(groupNode, websocket);

        HttpRequestItem sse = HttpRequestFactory.createDefaultEventStreamRequest();
        sse.setName("8. SSE Workflow Check");
        sse.setDescription("Run request 1 first. Then open the stream and wait for a few events. Each SSE event verifies that both environment values and execution variables remain available.");
        sse.setHeadersList(withExtraHeaders(sse,
                new HttpHeader(true, "X-Tenant-Id", "{{tenantId}}")));
        sse.setPrescript("""
                pm.variables.set('sseTraceId', 'sse-' + Date.now());
                pm.variables.set('sseScenario', 'sse-context');
                pm.variables.set('sseEventCount', '0');
                pm.variables.set('sseTenantId', pm.environment.get('tenantId') || '');
                console.log('SSE traceId =', pm.variables.get('sseTraceId'));
                console.log('Open the stream and wait for a few events.');
                """);
        sse.setPostscript("""
                var eventText = pm.response.text() || '';
                var count = parseInt(pm.variables.get('sseEventCount') || '0', 10) + 1;
                pm.variables.set('sseEventCount', String(count));
                pm.variables.set('lastSseEvent', eventText);
                pm.test('SSE keeps execution variables across event callbacks', function () {
                    pm.expect(pm.variables.get('sseTraceId')).to.exist();
                    pm.expect(pm.variables.get('sseScenario')).to.eql('sse-context');
                    pm.expect(pm.variables.get('sseTenantId')).to.eql(pm.environment.get('tenantId'));
                });
                pm.test('SSE callback can still read environment values', function () {
                    pm.expect(pm.environment.get('tenantId')).to.exist();
                    pm.expect(pm.environment.get('userId')).to.exist();
                });
                pm.test('SSE callback receives event text', function () {
                    pm.expect(eventText.length > 0).to.eql(true);
                });
                console.log('sse event #' + count + ', traceId = ' + pm.variables.get('sseTraceId'));
                """);
        addRequestChild(groupNode, sse);
        return groupNode;
    }

    private CollectionNode createVietnameseScriptExamples() {
        RequestGroup group = new RequestGroup(SCRIPT_EXAMPLES_VI_GROUP_NAME);
        group.setDescription("""
                Chạy lần lượt các yêu cầu 1 đến 6 để xem quy trình chia sẻ và tái sử dụng biến.
                Yêu cầu 7 và 8 kiểm tra biến trong callback WebSocket và SSE.
                Hãy chạy yêu cầu 1 trước yêu cầu 7 và 8 để tạo các biến môi trường cần thiết.

                Yêu cầu 1 đặt biến môi trường trong pre-request script.
                Yêu cầu 2 dùng lại biến trong tham số, header, body và test script; đồng thời tạo biến chỉ dùng trong lần chạy hiện tại.
                Yêu cầu 3 tạo orderId cho lần chạy hiện tại và lưu vào pm.variables.
                Yêu cầu 4 dùng lại orderId trong URL, tham số, header và test script.
                Yêu cầu 5 trích xuất orderId từ response body và lưu vào pm.variables.
                Yêu cầu 6 dùng lại orderId đã trích xuất.
                Yêu cầu 7 và 8 xác minh pm.environment và pm.variables vẫn dùng được trong callback WebSocket và SSE.

                Phạm vi biến:
                - pm.environment: dùng chung giữa các yêu cầu và có thể được tái sử dụng ở lần chạy sau.
                - pm.variables: dùng trong lần chạy hiện tại và được xóa khi lần chạy kết thúc.
                - pm.iterationData: hàng dữ liệu CSV/JSON hiện tại trong lần chạy theo dữ liệu.
                - Thứ tự ưu tiên Authorization: giá trị do script đặt > header Authorization hiện có > giá trị do tab Auth tạo.
                - Xóa Authorization trong pre-request script không tắt cấu hình tab Auth; cấu hình Auth sẽ được áp dụng khi gửi nếu chưa có Authorization.
                """);
        CollectionNode groupNode = CollectionNode.group(group);

        HttpRequestItem setup = HttpRequestFactory.createDefaultRequest();
        setup.setName("1. Đặt biến");
        setup.setDescription("Đặt các biến môi trường dùng chung trong tập lệnh trước và thể hiện một biến chạy.");
        setup.setMethod("GET");
        setup.setUrl("{{baseUrl}}/anything/setup-vi");
        setup.setHeadersList(withExtraHeaders(setup,
                new HttpHeader(true, "X-Setup-Trace", "{{setupTraceId}}")));
        setup.setParamsList(List.of(
                new HttpParam(true, "tenant", "{{tenantId}}")
        ));
        setup.setPrescript("""
                pm.environment.set('baseUrl', 'https://httpbin.org');
                pm.environment.set('tenantId', 'team-vn');
                pm.environment.set('userId', 'user-vn-1001');
                pm.environment.set('keyword', 'dev-workbench-vi');

                pm.variables.set('setupTraceId', 'setup-vi-' + Date.now());
                console.log('Các biến môi trường đã sẵn sàng:', JSON.stringify(pm.environment.toObject()));
                """);
        setup.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Đặt yêu cầu biến thành công', function () {
                    pm.response.to.have.status(200);
                    pm.expect(jsonData.args.tenant).to.eql(pm.environment.get('tenantId'));
                });
                """);
        addRequestChild(groupNode, setup);

        HttpRequestItem reuse = HttpRequestFactory.createDefaultRequest();
        reuse.setName("2. Tái sử dụng các biến");
        reuse.setDescription("Tái sử dụng các biến môi trường trong tham số yêu cầu, tiêu đề yêu cầu, nội dung yêu cầu và tập lệnh; cũng tạo và sử dụng các biến chạy của bối cảnh chạy hiện tại.");
        reuse.setMethod("POST");
        reuse.setUrl(HTTPBIN_POST_URL);
        reuse.setBodyType(BODY_TYPE_RAW);
        reuse.setHeadersList(withExtraHeaders(reuse,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON),
                new HttpHeader(true, "X-Tenant-Id", "{{tenantId}}"),
                new HttpHeader(true, "X-Trace-Id", "{{traceId}}")));
        reuse.setParamsList(List.of(
                new HttpParam(true, "keyword", "{{keyword}}"),
                new HttpParam(true, "trace", "{{traceId}}")
        ));
        reuse.setBody("""
                {
                  "tenantId": "{{tenantId}}",
                  "userId": "{{userId}}",
                  "traceId": "{{traceId}}",
                  "note": "{{note}}"
                }""");
        reuse.setPrescript("""
                pm.variables.set('traceId', 'trace-vi-' + Date.now());
                pm.variables.set('note', 'Các biến chạy có sẵn cho nội dung yêu cầu, tiêu đề yêu cầu và tham số yêu cầu');

                pm.request.headers.upsert({
                    key: 'X-Script-Flag',
                    value: 'ready-vi'
                });
                """);
        reuse.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Yêu cầu biến tái sử dụng đã thành công', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('Các biến môi trường được tái sử dụng thành công', function () {
                    pm.expect(jsonData.json.tenantId).to.eql(pm.environment.get('tenantId'));
                    pm.expect(jsonData.json.userId).to.eql(pm.environment.get('userId'));
                    pm.expect(jsonData.args.keyword).to.eql(pm.environment.get('keyword'));
                });
                pm.test('Biến đang chạy được sử dụng lại thành công trong lần chạy hiện tại', function () {
                    pm.expect(jsonData.args.trace).to.eql(pm.variables.get('traceId'));
                    pm.expect(jsonData.json.traceId).to.eql(pm.variables.get('traceId'));
                    pm.expect(jsonData.json.note).to.eql(pm.variables.get('note'));
                    pm.expect(jsonData.headers['X-Trace-Id']).to.eql(pm.variables.get('traceId'));
                });
                console.log('hiện tại traceId =', pm.variables.get('traceId'));
                """);
        addRequestChild(groupNode, reuse);

        HttpRequestItem createOrder = HttpRequestFactory.createDefaultRequest();
        createOrder.setName("3. Tạo bối cảnh đặt hàng");
        createOrder.setDescription("Tạo hoặc nhận một cái cho lần chạy hiện tại orderId，vượt qua httpbin Viết sau tiếng vang pm.variables，Nó có thể được sử dụng lại cho yêu cầu tiếp theo.");
        createOrder.setMethod("POST");
        createOrder.setUrl(HTTPBIN_POST_URL);
        createOrder.setBodyType(BODY_TYPE_RAW);
        createOrder.setHeadersList(withExtraHeaders(createOrder,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON),
                new HttpHeader(true, "X-Order-Id", "{{pendingOrderId}}")));
        createOrder.setParamsList(List.of(
                new HttpParam(true, "stage", "create-order"),
                new HttpParam(true, "pendingOrderId", "{{pendingOrderId}}")
        ));
        createOrder.setBody("""
                {
                  "orderId": "{{pendingOrderId}}",
                  "tenantId": "{{tenantId}}",
                  "source": "default-script-example-cn"
                }""");
        createOrder.setPrescript("""
                var incomingOrderId = pm.iterationData.get('orderId');
                var pendingOrderId = incomingOrderId || ('order-' + Date.now());
                pm.variables.set('pendingOrderId', pendingOrderId);
                console.log('Đang chờ truyền orderId =', pendingOrderId);
                """);
        createOrder.setPostscript("""
                var jsonData = pm.response.json();
                var orderId = jsonData.json.orderId;
                pm.variables.set('orderId', orderId);
                pm.test('Yêu cầu bối cảnh đặt hàng thành công', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('orderId Một biến chạy đã được viết sẽ có sẵn cho yêu cầu tiếp theo', function () {
                    pm.expect(orderId).to.eql(pm.variables.get('pendingOrderId'));
                    pm.expect(pm.variables.get('orderId')).to.eql(orderId);
                });
                """);
        addRequestChild(groupNode, createOrder);

        HttpRequestItem reuseOrder = HttpRequestFactory.createDefaultRequest();
        reuseOrder.setName("4. Tái sử dụng theo yêu cầu OrderId");
        reuseOrder.setDescription("yêu cầu tiêu thụ 3 Đã tạo orderId。Đây là mặc định và có thể được sử dụng trực tiếp Functional và Performance Ví dụ về nối hai yêu cầu.");
        reuseOrder.setMethod("GET");
        reuseOrder.setUrl(HTTPBIN_BASE_URL + "/anything/orders/{{orderId}}");
        reuseOrder.setHeadersList(withExtraHeaders(reuseOrder,
                new HttpHeader(true, "X-Order-Id", "{{orderId}}")));
        reuseOrder.setParamsList(List.of(
                new HttpParam(true, "orderId", "{{orderId}}"),
                new HttpParam(true, "tenantId", "{{tenantId}}")
        ));
        reuseOrder.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Tái sử dụng orderId Yêu cầu đã thành công', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('orderId Đã chuyển thành công tới yêu cầu tiếp theo', function () {
                    pm.expect(jsonData.args.orderId).to.eql(pm.variables.get('orderId'));
                    pm.expect(jsonData.headers['X-Order-Id']).to.eql(pm.variables.get('orderId'));
                    pm.expect(jsonData.url).to.include('/orders/' + pm.variables.get('orderId'));
                });
                console.log('Hiện đang được sử dụng lại orderId =', pm.variables.get('orderId'));
                """);
        addRequestChild(groupNode, reuseOrder);

        HttpRequestItem extractOrder = HttpRequestFactory.createDefaultRequest();
        extractOrder.setName("5. Trích xuất từ ​​phản hồi OrderId");
        extractOrder.setDescription("Sau khi mô phỏng việc tạo đơn hàng, hãy trích xuất tiếng vang từ nội dung phản hồi orderId，Viết pm.variables，Tái sử dụng cho yêu cầu tiếp theo.");
        extractOrder.setMethod("POST");
        extractOrder.setUrl(HTTPBIN_POST_URL);
        extractOrder.setBodyType(BODY_TYPE_RAW);
        extractOrder.setHeadersList(withExtraHeaders(extractOrder,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON)));
        extractOrder.setBody("""
                {
                  "clientOrderId": "{{responseOrderCandidate}}",
                  "tenantId": "{{tenantId}}",
                  "scenario": "extract-from-response"
                }""");
        extractOrder.setPrescript("""
                pm.variables.set('responseOrderCandidate', 'resp-order-' + Date.now());
                console.log('Để được trích xuất từ ​​phản hồi orderId =', pm.variables.get('responseOrderCandidate'));
                """);
        extractOrder.setPostscript("""
                var jsonData = pm.response.json();
                var extractedOrderId = jsonData.json.clientOrderId;
                pm.variables.set('responseOrderId', extractedOrderId);
                pm.test('Phản hồi yêu cầu tìm nạp đã thành công', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('orderId Đã nhận được phản hồi từ JSON được chiết xuất từ', function () {
                    pm.expect(extractedOrderId).to.eql(pm.variables.get('responseOrderCandidate'));
                    pm.expect(pm.variables.get('responseOrderId')).to.eql(extractedOrderId);
                });
                """);
        addRequestChild(groupNode, extractOrder);

        HttpRequestItem reuseExtractedOrder = HttpRequestFactory.createDefaultRequest();
        reuseExtractedOrder.setName("6. Tái sử dụng được trích xuất OrderId");
        reuseExtractedOrder.setDescription("yêu cầu tiêu thụ 5 Trích xuất từ ​​​​phần thân phản hồi orderId。Thích hợp để xác minh“Trích xuất các trường phản hồi và chuyển chúng cho yêu cầu tiếp theo”quá trình.");
        reuseExtractedOrder.setMethod("GET");
        reuseExtractedOrder.setUrl(HTTPBIN_BASE_URL + "/anything/orders/extracted/{{responseOrderId}}");
        reuseExtractedOrder.setHeadersList(withExtraHeaders(reuseExtractedOrder,
                new HttpHeader(true, "X-Extracted-Order-Id", "{{responseOrderId}}")));
        reuseExtractedOrder.setParamsList(List.of(
                new HttpParam(true, "extractedOrderId", "{{responseOrderId}}"),
                new HttpParam(true, "source", "response-body")
        ));
        reuseExtractedOrder.setPostscript("""
                var jsonData = pm.response.json();
                pm.test('Tái sử dụng được trích xuất orderId Yêu cầu đã thành công', function () {
                    pm.response.to.have.status(200);
                });
                pm.test('được chiết xuất orderId Đã chuyển qua yêu cầu tiếp theo', function () {
                    pm.expect(jsonData.args.extractedOrderId).to.eql(pm.variables.get('responseOrderId'));
                    pm.expect(jsonData.headers['X-Extracted-Order-Id']).to.eql(pm.variables.get('responseOrderId'));
                    pm.expect(jsonData.url).to.include('/orders/extracted/' + pm.variables.get('responseOrderId'));
                });
                console.log('Việc trích xuất tái sử dụng hiện tại orderId =', pm.variables.get('responseOrderId'));
                """);
        addRequestChild(groupNode, reuseExtractedOrder);

        HttpRequestItem websocket = HttpRequestFactory.createDefaultWebSocketRequest();
        websocket.setName("7. WebSocket Quá trình xác minh");
        websocket.setDescription("Nên thực hiện yêu cầu trước 1。Sau đó bấm Kết nối; nếu cần, hãy gửi lại nội dung thư mặc định. Ví dụ này tập trung vào việc xác minh WebSocket Việc đọc vẫn có thể được thực hiện trong chuỗi gọi lại pm.environment và pm.variables，Thay vì dựa vào việc thay thế phần giữ chỗ trong nội dung yêu cầu.");
        websocket.setBodyType(BODY_TYPE_RAW);
        websocket.setBody("""
                {
                  "type": "context-check",
                  "source": "dev-workbench-websocket-example",
                  "message": "send-once-after-connect"
                }""");
        websocket.setPrescript("""
                pm.variables.set('streamTraceId', 'ws-cn-' + Date.now());
                pm.variables.set('streamScenario', 'websocket-context');
                pm.variables.set('streamTenantId', pm.environment.get('tenantId') || '');
                console.log('WebSocket traceId =', pm.variables.get('streamTraceId'));
                console.log('tenantId =', pm.environment.get('tenantId'));
                console.log('Vui lòng nhấp vào kết nối trước rồi gửi lại nội dung thư mặc định.');
                """);
        websocket.setPostscript("""
                var messageText = pm.response.text() || '';
                pm.variables.set('lastWebSocketMessage', messageText);
                pm.test('WebSocket Các biến chạy vẫn có thể được đọc trong chuỗi gọi lại', function () {
                    pm.expect(pm.variables.get('streamTraceId')).to.exist();
                    pm.expect(pm.variables.get('streamScenario')).to.eql('websocket-context');
                    pm.expect(pm.variables.get('streamTenantId')).to.eql(pm.environment.get('tenantId'));
                });
                pm.test('WebSocket Các biến môi trường vẫn có thể được đọc trong chuỗi gọi lại', function () {
                    pm.expect(pm.environment.get('tenantId')).to.exist();
                    pm.expect(pm.environment.get('userId')).to.exist();
                });
                pm.test('WebSocket Cuộc gọi lại nhận được văn bản không trống', function () {
                    pm.expect(messageText.length > 0).to.eql(true);
                });
                console.log('Gần đây nhất WebSocket tin nhắn =', messageText);
                """);
        addRequestChild(groupNode, websocket);

        HttpRequestItem sse = HttpRequestFactory.createDefaultEventStreamRequest();
        sse.setName("8. SSE Quá trình xác minh");
        sse.setDescription("Nên thực hiện yêu cầu trước 1。Sau đó mở SSE Truyền phát và chờ đợi một số sự kiện. Mỗi sự kiện sẽ xác minh rằng các biến môi trường và các biến đang chạy không bị mất.");
        sse.setHeadersList(withExtraHeaders(sse,
                new HttpHeader(true, "X-Tenant-Id", "{{tenantId}}")));
        sse.setPrescript("""
                pm.variables.set('sseTraceId', 'sse-cn-' + Date.now());
                pm.variables.set('sseScenario', 'sse-context');
                pm.variables.set('sseEventCount', '0');
                pm.variables.set('sseTenantId', pm.environment.get('tenantId') || '');
                console.log('SSE traceId =', pm.variables.get('sseTraceId'));
                console.log('Please open the stream and wait for a few events.');
                """);
        sse.setPostscript("""
                var eventText = pm.response.text() || '';
                var count = parseInt(pm.variables.get('sseEventCount') || '0', 10) + 1;
                pm.variables.set('sseEventCount', String(count));
                pm.variables.set('lastSseEvent', eventText);
                pm.test('SSE Các biến đang chạy vẫn có thể được đọc trong lệnh gọi lại sự kiện', function () {
                    pm.expect(pm.variables.get('sseTraceId')).to.exist();
                    pm.expect(pm.variables.get('sseScenario')).to.eql('sse-context');
                    pm.expect(pm.variables.get('sseTenantId')).to.eql(pm.environment.get('tenantId'));
                });
                pm.test('SSE Các biến môi trường vẫn có thể được đọc trong lệnh gọi lại sự kiện', function () {
                    pm.expect(pm.environment.get('tenantId')).to.exist();
                    pm.expect(pm.environment.get('userId')).to.exist();
                });
                pm.test('SSE Cuộc gọi lại đã nhận được văn bản sự kiện', function () {
                    pm.expect(eventText.length > 0).to.eql(true);
                });
                console.log('KHÔNG.  ' + count + ' dải SSE sự kiện, traceId = ' + pm.variables.get('sseTraceId'));
                """);
        addRequestChild(groupNode, sse);
        return groupNode;
    }

    private CollectionNode createBasicHttpExamples() {
        RequestGroup defaultGroup = new RequestGroup(BASIC_HTTP_EXAMPLES_GROUP_NAME);
        defaultGroup.setDescription("""
                General-purpose examples for common request types.
                These requests are independent from the official script examples.
                """);
        CollectionNode defaultGroupNode = CollectionNode.group(defaultGroup);

        HttpRequestItem getExample = HttpRequestFactory.createDefaultRequest();
        getExample.setName("GET Example");
        getExample.setMethod("GET");
        getExample.setUrl(HTTPBIN_BASE_URL + "/get?q=devWorkbenchtools&lang=en&page=1&size=10&sort=desc&filter=active");
        getExample.setParamsList(List.of(
                new HttpParam(true, "q", "devWorkbenchtools"),
                new HttpParam(true, "lang", "en"),
                new HttpParam(true, "page", "1"),
                new HttpParam(true, "size", "10"),
                new HttpParam(true, "sort", "desc"),
                new HttpParam(true, "filter", "active")
        ));
        getExample.setPostscript("""
                pm.test('JSON value check', function () {
                    var jsonData = pm.response.json();
                    pm.expect(jsonData.args.size).to.eql("10");
                });
                """);
        addRequestChild(defaultGroupNode, getExample);

        HttpRequestItem postJson = HttpRequestFactory.createDefaultRequest();
        postJson.setName("POST JSON Example");
        postJson.setMethod("POST");
        postJson.setUrl(HTTPBIN_POST_URL);
        postJson.setBodyType(BODY_TYPE_RAW);
        postJson.setHeadersList(withExtraHeaders(postJson,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON)));
        postJson.setBody("""
                {
                  "key1": "value1",
                  "key2": "value2",
                  "key3": "value3"
                }""");
        postJson.setPostscript("""
                pm.test('JSON value check', function () {
                    var jsonData = pm.response.json();
                    pm.expect(jsonData.json.key1).to.eql("value1");
                });
                """);
        addRequestChild(defaultGroupNode, postJson);

        HttpRequestItem postFormData = HttpRequestFactory.createDefaultRequest();
        postFormData.setName("POST form-data Example");
        postFormData.setMethod("POST");
        postFormData.setUrl(HTTPBIN_POST_URL);
        postFormData.setHeadersList(withExtraHeaders(postFormData,
                new HttpHeader(true, CONTENT_TYPE, "multipart/form-data")));
        postFormData.setFormDataList(List.of(
                new HttpFormData(true, "key1", HttpFormData.TYPE_TEXT, "value1"),
                new HttpFormData(true, "key2", HttpFormData.TYPE_TEXT, "value2")
        ));
        postFormData.setPostscript("""
                pm.test('Form-data value check', function () {
                    var jsonData = pm.response.json();
                    pm.expect(jsonData.form.key1).to.eql("value1");
                });
                """);
        addRequestChild(defaultGroupNode, postFormData);

        HttpRequestItem postUrl = HttpRequestFactory.createDefaultRequest();
        postUrl.setName("POST x-www-form-urlencoded Example");
        postUrl.setMethod("POST");
        postUrl.setUrl(HTTPBIN_POST_URL);
        postUrl.setHeadersList(withExtraHeaders(postUrl,
                new HttpHeader(true, CONTENT_TYPE, "application/x-www-form-urlencoded")));
        postUrl.setUrlencodedList(List.of(
                new HttpFormUrlencoded(true, "key1", "value1"),
                new HttpFormUrlencoded(true, "key2", "value2")
        ));
        postUrl.setPostscript("""
                pm.test('URL encoded value check', function () {
                    var jsonData = pm.response.json();
                    pm.expect(jsonData.form.key1).to.eql("value1");
                });
                """);
        addRequestChild(defaultGroupNode, postUrl);

        HttpRequestItem put = HttpRequestFactory.createDefaultRequest();
        put.setName("PUT Example");
        put.setMethod("PUT");
        put.setUrl(HTTPBIN_BASE_URL + "/put");
        put.setBodyType(BODY_TYPE_RAW);
        put.setHeadersList(withExtraHeaders(put,
                new HttpHeader(true, CONTENT_TYPE, APPLICATION_JSON)));
        put.setBody("{\"update\":true}");
        addRequestChild(defaultGroupNode, put);

        HttpRequestItem delete = HttpRequestFactory.createDefaultRequest();
        delete.setName("DELETE Example");
        delete.setMethod("DELETE");
        delete.setUrl(HTTPBIN_BASE_URL + "/delete");
        addRequestChild(defaultGroupNode, delete);

        HttpRequestItem redirect = HttpRequestFactory.createDefaultRedirectRequest();
        redirect.setName("Redirect Example");
        addRequestChild(defaultGroupNode, redirect);

        HttpRequestItem websocket = HttpRequestFactory.createDefaultWebSocketRequest();
        websocket.setName("WebSocket Example");
        addRequestChild(defaultGroupNode, websocket);

        HttpRequestItem sse = HttpRequestFactory.createDefaultEventStreamRequest();
        addRequestChild(defaultGroupNode, sse);
        return defaultGroupNode;
    }

    private List<HttpHeader> withExtraHeaders(HttpRequestItem item, HttpHeader... extraHeaders) {
        List<HttpHeader> headers = new ArrayList<>(item.getHeadersList());
        headers.addAll(List.of(extraHeaders));
        return headers;
    }

    private void addRequestChild(CollectionNode parentNode, HttpRequestItem item) {
        parentNode.addChild(CollectionNode.request(item));
    }
}
