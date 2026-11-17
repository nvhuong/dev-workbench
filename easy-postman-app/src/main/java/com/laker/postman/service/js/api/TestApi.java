package com.laker.postman.service.js.api;

import com.laker.postman.script.model.TestResult;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import java.util.UUID;

/**
 * Bài kiểm tra API (pm.test)
 * <p>
 * cung cấp Postman Giao diện kiểm tra phong cách, hỗ trợ: .
 * - pm.test(name, function) - Thực hiện bài kiểm tra
 * </p>
 *
 * <h3>Ví dụ sử dụng:</h3>
 * <pre>{@code
 * // Thực hiện bài kiểm tra
 * pm.test("Status code is 200", function() {
 *     pm.expect(pm.response.code).to.equal(200);
 * });
 * }</pre>
 */
@Slf4j
public class TestApi implements ProxyExecutable {
    private final PostmanApiContext context;

    public TestApi(PostmanApiContext context) {
        this.context = context;
    }

    @Override
    public Object execute(Value... arguments) {
        String name = arguments != null && arguments.length > 0 && !arguments[0].isNull()
                ? arguments[0].asString()
                : "";
        Value testFunction = arguments != null && arguments.length > 1 ? arguments[1] : null;
        run(name, testFunction);
        return null;
    }

    /**
     * Thực hiện các xác nhận kiểm tra
     * Tương ứng với kịch bản: pm.test(name, function)
     *
     * @param name         Tên thử nghiệm
     * @param testFunction Chức năng kiểm tra
     */
    public void run(String name, Value testFunction) {
        String errorMessage = null;
        boolean passed = true;

        try {
            if (testFunction != null && testFunction.canExecute()) {
                testFunction.execute();
            }
        } catch (Exception e) {
            passed = false;
            errorMessage = e.getMessage();
            log.debug("Thử nghiệm thất bại: {}, sai lầm: {}", name, e.getMessage());
        }

        TestResult result = new TestResult(name, passed, errorMessage);
        result.id = UUID.randomUUID().toString();
        context.testResults.add(result);
    }
}
