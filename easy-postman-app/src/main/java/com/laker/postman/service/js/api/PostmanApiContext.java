package com.laker.postman.service.js.api;

import com.laker.postman.model.Environment;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.plugin.host.PluginAccess;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.GlobalVariablesService;
import com.laker.postman.service.js.ScriptSendRequestExecutor;
import com.laker.postman.service.variable.IterationInfoService;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;

import java.util.*;

/**
 * Postman kịch bản API bối cảnh (pm đối tượng)
 * <p>
 * This type of simulation Postman trong <code>pm</code> đối tượng, cho PreRequest/PostRequest Kịch bản được cung cấp đầy đủ API Hỗ trợ.
 * Đối tượng này sẽ được đưa vào khi tập lệnh được thực thi JavaScript Trong công cụ, có thể truy cập mã script.
 * </p>
 *
 * <h3>Các module chức năng chính:</h3>
 * <ul>
 *   <li><b>Quản lý biến môi trường</b>: pm.environment.set/get - Thao tác với các biến môi trường hiện được kích hoạt</li>
 *   <li><b>Quản lý biến toàn cầu</b>: pm.globals.set/get - Thao tác các biến toàn cục cấp ứng dụng</li>
 *   <li><b>Thực hiện quản lý biến</b>: pm.variables.set/get - Thao tác các biến trong bối cảnh đang chạy hiện tại</li>
 *   <li><b>Quản lý dữ liệu lặp</b>: pm.iterationData.get - Truy cập hàng điều khiển dữ liệu hiện tại</li>
 *   <li><b>Cookie hoạt động</b>: pm.cookies.get/set/delete - Yêu cầu quản lý Cookie</li>
 *   <li><b>Kiểm tra khẳng định</b>: pm.test() - Thực hiện các xác nhận kiểm tra và ghi lại kết quả</li>
 *   <li><b>Yêu cầu quyền truy cập</b>: pm.request - Truy cập và sửa đổi thông tin yêu cầu hiện tại</li>
 *   <li><b>Trả lời quyền truy cập</b>: pm.response - Truy cập dữ liệu phản hồi (chỉ có sẵn cho phần tái bút)</li>
 * </ul>
 *
 * <h3>Ví dụ sử dụng:</h3>
 * <pre>{@code
 * // PreRequest Script
 * pm.environment.set("timestamp", Date.now());
 * pm.variables.set("requestId", pm.uuid());
 * pm.request.headers.add({key: "X-Request-ID", value: pm.variables.get("requestId")});
 *
 * // PostRequest Script
 * pm.test("Status code is 200", function() {
 *     pm.expect(pm.response.code).to.equal(200);
 * });
 * pm.environment.set("token", pm.response.json().access_token);
 * }</pre>
 *
 * @author laker
 * @see <a href="https://learning.postman.com/docs/writing-scripts/script-references/postman-sandbox-api-reference/">Postman Scripting API Reference</a>
 */
@Slf4j
public class PostmanApiContext {

    /**
     * Danh sách kết quả kiểm tra - Lưu trữ tất cả pm.test() Kết quả của cuộc gọi
     */
    public List<TestResult> testResults = new ArrayList<>();

    /**
     * Đối tượng biến môi trường - tương ứng pm.environment
     */
    public ScriptScopedVariablesApi environment;

    /**
     * Bí danh tương thích trong lịch sử - tương ứng pm.env。
     * Postman chính thức API sử dụng pm.environment，Các tập lệnh mới và đoạn mã tích hợp vẫn được ưu tiên pm.environment。
     */
    public ScriptScopedVariablesApi env;

    /**
     * Đối tượng biến toàn cục - tương ứng pm.globals
     */
    public ScriptScopedVariablesApi globals;

    /**
     * đối tượng phản hồi - tương ứng pm.response，Chỉ có sẵn trong phần tái bút
     */
    public ResponseAssertion response;

    /**
     * Thực thi trình quản lý biến - tương ứng pm.variables，Được sử dụng để lưu trữ các biến trong ngữ cảnh đang chạy hiện tại
     */
    public ScriptVariablesApi variables;

    /**
     * Trình quản lý dữ liệu lặp - tương ứng pm.iterationData，Dùng để lưu trữ dòng điện CSV/JSON dữ liệu hàng
     */
    public IterationDataApi iterationData = new IterationDataApi();

    /**
     * Chạy thông tin meta - tương ứng Postman của pm.info。
     */
    public PostmanInfoApi info;

    /**
     * Yêu cầu trình bao bọc đối tượng - tương ứng pm.request，Cung cấp theo yêu cầu JavaScript giao diện truy cập
     */
    public ScriptRequestAccessor request;

    /**
     * Cookie giám đốc - tương ứng pm.cookies，cung cấp Cookie Giao diện hoạt động
     */
    public CookieApi cookies;

    /**
     * Bài kiểm tra API - tương ứng pm.test。
     */
    public TestApi test;

    private final Map<String, Object> pluginApis = new LinkedHashMap<>();
    private final ScriptSendRequestExecutor sendRequestExecutor;

    /**
     * Elasticsearch API - tương ứng pm.elasticsearch
     */
    public ScriptElasticsearchApi elasticsearch;

    /**
     * InfluxDB API - tương ứng pm.influxdb
     */
    public ScriptInfluxDbApi influxdb;

    /**
     * kết cấu Postman API bối cảnh
     *
     * @param environment Đối tượng môi trường hiện đang hoạt động
     */
    public PostmanApiContext(Environment environment) {
        this(
                environment,
                GlobalVariablesService.getInstance().getGlobalVariables(),
                createEnvironmentPersistAction(environment)
        );
    }

    /**
     * kết cấu headless / run-scoped Bối cảnh kịch bản.
     * Sử dụng nó trực tiếp ở đây plan Các biến môi trường và toàn cầu được mang bên trong đều được tránh worker Dự phòng khi được thực thi GUI Dịch vụ không gian làm việc.
     */
    public static PostmanApiContext scoped(Environment environment, Environment globals) {
        return new PostmanApiContext(environment, globals, null, false);
    }

    /**
     * Creates a run-scoped context whose pm.variables lookup follows Postman runner
     * precedence and therefore includes the current iteration-data row.
     */
    public static PostmanApiContext scopedWithIterationData(Environment environment, Environment globals) {
        return new PostmanApiContext(environment, globals, null, true);
    }

    private PostmanApiContext(Environment environment,
                              Environment globals,
                              Runnable environmentPersistAction) {
        this(environment, globals, environmentPersistAction, false);
    }

    private PostmanApiContext(Environment environment,
                              Environment globals,
                              Runnable environmentPersistAction,
                              boolean includeIterationDataInVariables) {
        this(
                environment,
                globals,
                environmentPersistAction,
                new ScriptSendRequestExecutor(),
                includeIterationDataInVariables
        );
    }

    private PostmanApiContext(Environment environment,
                              Environment globals,
                              Runnable environmentPersistAction,
                              ScriptSendRequestExecutor sendRequestExecutor,
                              boolean includeIterationDataInVariables) {
        this.sendRequestExecutor = sendRequestExecutor == null
                ? new ScriptSendRequestExecutor()
                : sendRequestExecutor;
        this.variables = includeIterationDataInVariables
                ? ScriptVariablesApi.withIterationData()
                : new ScriptVariablesApi();
        this.environment = new ScriptScopedVariablesApi(
                environment,
                environmentPersistAction
        );
        this.env = this.environment;
        this.globals = new ScriptScopedVariablesApi(
                globals,
                null
        );
        this.cookies = new CookieApi(); // khởi tạo cookies
        this.test = new TestApi(this); // khởi tạo test API
        this.info = new PostmanInfoApi(IterationInfoService.getInstance().getCurrentInfo());
        this.elasticsearch = new ScriptElasticsearchApi();
        this.influxdb = new ScriptInfluxDbApi();
        // cốt lõi pm Các khả năng đầu tiên được tích hợp vào máy chủ và sau đó được mở rộng trong sổ đăng ký plug-in API Tự động treo vào.
        // Bằng cách này, lớp tập lệnh sẽ nhìn thấy một chế độ xem thống nhất pm thay vào đó là đối tượng“chủ nhà API + plugin API”Hai bộ lối vào.
        PluginAccess.createScriptApis().forEach(this::registerPluginApi);
    }

    private static Runnable createEnvironmentPersistAction(Environment environment) {
        if (!EnvironmentService.isManagedEnvironment(environment)) {
            return null;
        }
        return () -> EnvironmentService.saveEnvironment(environment);
    }

    private void registerPluginApi(String alias, Object api) {
        if (alias == null || api == null) {
            return;
        }
        pluginApis.put(alias, api);
    }

    public Object plugin(String alias) {
        // Plugin hợp nhất API Lối vào truy cập có thể được thông qua trong tập lệnh pm.plugin("kafka") Phương pháp này có được khả năng theo yêu cầu.
        return pluginApis.get(alias);
    }

    public boolean hasPlugin(String alias) {
        return alias != null && pluginApis.containsKey(alias);
    }

    /**
     * Đặt đối tượng yêu cầu
     * Được khung gọi trước khi thực thi tập lệnh, đưa thông tin yêu cầu hiện tại vào
     *
     * @param preparedRequest Đối tượng yêu cầu đã chuẩn bị
     */
    public void setRequest(PreparedRequest preparedRequest) {
        this.request = new ScriptRequestAccessor(preparedRequest);
        if (this.info != null) {
            this.info.setRequest(preparedRequest);
        }
    }

    /**
     * Đặt đối tượng phản hồi
     * Được gọi bởi khung trước khi phần tái bút được thực thi, thông tin phản hồi sẽ được đưa vào
     *
     * @param httpResponse HTTP đối tượng phản hồi
     */
    public void setResponse(HttpResponse httpResponse) {
        this.response = new ResponseAssertion(httpResponse);

        // Tự động trả lời Cookie Điền vào pm.cookies，Làm cho pm.cookies.get() Có khả năng làm việc
        populateResponseCookies(httpResponse);
    }

    /**
     * Trích xuất tất cả từ tiêu đề phản hồi Cookie và điền vào pm.cookies
     *
     * @param httpResponse HTTP đối tượng phản hồi
     */
    private void populateResponseCookies(HttpResponse httpResponse) {
        if (cookies == null) {
            return;
        }
        for (Cookie cookie : PostmanResponseCookieSupport.extract(httpResponse)) {
            cookies.set(cookie);
            log.debug("Populated response cookie to pm.cookies: {} = {}", cookie.name, cookie.value);
        }
    }

    /**
     * phát ra UUID
     * Tương ứng với kịch bản: pm.uuid()
     *
     * @return UUID chuỗi
     */
    public String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Lấy dấu thời gian hiện tại (mili giây)
     * Tương ứng với kịch bản: pm.getTimestamp()
     *
     * @return Dấu thời gian hiện tại (mili giây)
     */
    public long getTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * Tạo một đối tượng kỳ vọng khẳng định
     * Tương ứng với kịch bản: pm.expect(actual)
     *
     * <p>Ví dụ:
     * <pre>{@code
     * pm.expect(pm.response.code).to.equal(200);
     * pm.expect(pm.response.json().name).to.eql("John");
     * pm.expect(pm.response.text()).to.include("success");
     * pm.expect(pm.response.responseTime).to.be.below(1000);
     * }</pre>
     *
     * @param actual Giá trị thực tế cần khẳng định
     * @return Expectation Đối tượng, hỗ trợ các xác nhận theo chuỗi
     */
    public Expectation expect(Object actual) {
        return new Expectation(actual);
    }

    public void setEnvironmentVariable(String key, Object value) {
        environment.set(key, value);
    }

    public String getEnvironmentVariable(String key) {
        return environment.get(key);
    }

    public void clearEnvironmentVariable(String key) {
        environment.unset(key);
    }

    public void clearEnvironmentVariables() {
        environment.clear();
    }

    public void setGlobalVariable(String key, Object value) {
        globals.set(key, value);
    }

    public String getGlobalVariable(String key) {
        return globals.get(key);
    }

    public void clearGlobalVariable(String key) {
        globals.unset(key);
    }

    public void clearGlobalVariables() {
        globals.clear();
    }

    /**
     * Lấy tên được chỉ định từ tiêu đề phản hồi Cookie
     * Tương ứng với kịch bản: pm.getResponseCookie(name)
     *
     * <p>This method starts from HTTP Đáp ứng Set-Cookie Phân tích cú pháp và trả về tên được chỉ định trong tiêu đề Cookie。
     * Thường được sử dụng để trích xuất cài đặt máy chủ trong các tập lệnh sau Cookie giá trị, ví dụ session ID、token Đợi đã.</p>
     *
     * <p>Ví dụ:
     * <pre>{@code
     * // Nhận được phản hồi JSESSIONID cookie
     * var jsessionId = pm.getResponseCookie('JSESSIONID');
     * if (jsessionId) {
     *     pm.environment.set('session_id', jsessionId.value);
     *     console.log('JSESSIONID:', jsessionId.value);
     * }
     * }</pre>
     *
     * @param name Cookie tên
     * @return Cookie Đối tượng chứa name、value、domain và các thuộc tính khác; nếu nó không tồn tại, nó sẽ được trả về null
     */
    public Cookie getResponseCookie(String name) {
        if (name == null || response == null) {
            return null;
        }
        return PostmanResponseCookieSupport.find(response.getHttpResponse(), name);
    }

    /**
     * gửi HTTP yêu cầu
     * Tương ứng với kịch bản: pm.sendRequest(requestOptions, callback)
     *
     * @param requestOptions Yêu cầu cấu hình, có thể được URL Một chuỗi hoặc đối tượng chứa chi tiết yêu cầu
     * @param callback       Chức năng gọi lại, nhận (error, response) Hai tham số
     */
    public void sendRequest(Object requestOptions, Value callback) {
        if (requestOptions == null) {
            log.warn("pm.sendRequest: requestOptions is null");
            return;
        }

        try {
            sendRequestExecutor.sendRequest(requestOptions, callback);
        } catch (Exception e) {
            log.error("pm.sendRequest failed: {}", e.getMessage(), e);

            if (callback != null && callback.canExecute()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", e.getMessage());
                error.put("name", e.getClass().getSimpleName());

                try {
                    callback.execute(Value.asValue(error), Value.asValue(null));
                } catch (Exception callbackEx) {
                    log.error("Error executing callback: {}", callbackEx.getMessage());
                }
            }
        }
    }

}
