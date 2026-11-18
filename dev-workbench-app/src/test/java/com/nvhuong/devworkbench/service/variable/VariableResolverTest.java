package com.nvhuong.devworkbench.service.variable;

import com.nvhuong.devworkbench.service.js.api.ScriptVariablesApi;
import com.nvhuong.devworkbench.model.Environment;
import com.nvhuong.devworkbench.service.EnvironmentService;
import com.nvhuong.devworkbench.service.GlobalVariablesService;
import com.nvhuong.devworkbench.variable.VariableType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.*;

/**
 * Lớp kiểm tra trình phân tích cú pháp biến - Kiểm tra chức năng phân tích cú pháp biến lồng nhau
 */
public class VariableResolverTest {

    private Environment testEnv;
    private String originalDataFilePath;
    private String originalGlobalDataFilePath;
    private Path tempEnvFile;
    private Path tempGlobalFile;

    @BeforeMethod
    public void setUp() {
        // Cô lập dữ liệu thử nghiệm: Chuyển sang tệp môi trường tạm thời để tránh làm ô nhiễm các biến môi trường thực của máy
        originalDataFilePath = EnvironmentService.getDataFilePath();
        try {
            tempEnvFile = Files.createTempFile("dev-workbench-env-test-", ".json");
            Files.writeString(tempEnvFile, "[]");
            tempGlobalFile = Files.createTempFile("dev-workbench-global-test-", ".json");
            Files.writeString(tempGlobalFile, "{}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize temporary environment file", e);
        }
        EnvironmentService.setDataFilePath(tempEnvFile.toString());
        originalGlobalDataFilePath = GlobalVariablesService.getInstance().getDataFilePath();
        GlobalVariablesService.getInstance().setDataFilePath(tempGlobalFile.toString());

        // Xóa bối cảnh thực thi để tránh chia sẻ nó giữa các lần kiểm tra pm.variables / pm.iterationData
        clearExecutionContext();

        // Tạo môi trường thử nghiệm
        testEnv = new Environment();
        testEnv.setId("test-env-id");
        testEnv.setName("Test Environment");

        // Đặt các biến lồng nhau
        testEnv.set("baseUrl", "https://api.example.com");
        testEnv.set("apiPath", "{{baseUrl}}/api");
        testEnv.set("userModule", "{{apiPath}}/user");

        // Thêm môi trường vào EnvironmentService và thiết lập làm môi trường hoạt động
        EnvironmentService.saveEnvironment(testEnv);
        EnvironmentService.setActiveEnvironment(testEnv.getId());
    }

    @AfterMethod
    public void tearDown() {
        try {
            // Dọn dẹp môi trường thử nghiệm
            if (testEnv != null && testEnv.getId() != null) {
                EnvironmentService.deleteEnvironment(testEnv.getId());
            }
            clearExecutionContext();
            RequestExecutionContext.clearCurrentScope();
        } finally {
            // Khôi phục đường dẫn tệp môi trường ban đầu
            if (originalDataFilePath != null && !originalDataFilePath.isBlank()) {
                EnvironmentService.setDataFilePath(originalDataFilePath);
            }
            if (originalGlobalDataFilePath != null && !originalGlobalDataFilePath.isBlank()) {
                GlobalVariablesService.getInstance().setDataFilePath(originalGlobalDataFilePath);
            }
            if (tempEnvFile != null) {
                try {
                    Files.deleteIfExists(tempEnvFile);
                } catch (Exception ignored) {
                    // ignore cleanup failures
                }
            }
            if (tempGlobalFile != null) {
                try {
                    Files.deleteIfExists(tempGlobalFile);
                } catch (Exception ignored) {
                    // ignore cleanup failures
                }
            }
        }
    }

    /**
     * Kiểm tra độ phân giải biến lồng nhau - chức năng cốt lõi
     */
    @Test
    public void testNestedVariableResolution() {
        // Kiểm tra một cấp độ lồng nhau
        String result1 = VariableResolver.resolve("{{apiPath}}");
        assertEquals("https://api.example.com/api", result1,
                "apiPath should resolve to https://api.example.com/api");

        // Kiểm tra lồng cấp độ thứ hai
        String result2 = VariableResolver.resolve("{{userModule}}");
        assertEquals("https://api.example.com/api/user", result2,
                "userModule should resolve to https://api.example.com/api/user");

        // Kiểm tra giao diện hoàn chỉnh URL
        String result3 = VariableResolver.resolve("{{userModule}}/list");
        assertEquals("https://api.example.com/api/user/list", result3,
                "{{userModule}}/list should resolve to https://api.example.com/api/user/list");
    }

    @Test
    public void testRandomTfBuiltInVariableResolution() {
        String result = VariableResolver.resolve("{{$randomTF}}");

        assertTrue("T".equals(result) || "F".equals(result),
                "$randomTF should resolve to T or F, but was: " + result);
    }

    @Test
    public void testBuiltInVariableDescriptionsShouldHaveI18nMessages() {
        Map<String, String> descriptions = BuiltInFunctionService.getInstance().getAll();

        descriptions.forEach((name, description) -> {
            assertNotNull(description, name + " should have a description");
            assertFalse(description.startsWith("!builtin.var."), name + " is missing an i18n description: " + description);
            assertFalse(description.startsWith("Built-in function:"), name + " should not use fallback description");
        });
    }

    /**
     * Kiểm tra nhiều biến lồng nhau trong cùng một chuỗi
     */
    @Test
    public void testMultipleNestedVariables() {
        String input = "{{apiPath}}/admin and {{userModule}}/profile";
        String result = VariableResolver.resolve(input);
        assertEquals("https://api.example.com/api/admin and https://api.example.com/api/user/profile", result);
    }

    /**
     * Kiểm tra khả năng lồng sâu (3lớp trở lên)
     */
    @Test
    public void testDeepNesting() {
        testEnv.set("level3", "{{userModule}}/admin");
        testEnv.set("level4", "{{level3}}/settings");

        String result = VariableResolver.resolve("{{level4}}/profile");
        assertEquals(result, "https://api.example.com/api/user/admin/settings/profile");
    }

    /**
     * Mức độ ưu tiên của biến bối cảnh thực hiện thử nghiệm
     */
    @Test
    public void testExecutionScopedVariablePriority() {
        // Đặt các biến bối cảnh thực thi, ghi đè các biến môi trường
        Map<String, String> tempVars = new HashMap<>();
        tempVars.put("baseUrl", "https://temp.example.com");
        VariablesService.getInstance().setAll(tempVars);

        // Biến bối cảnh thực thi có mức độ ưu tiên cao hơn và nên sử dụng giá trị của nó
        String result = VariableResolver.resolve("{{userModule}}/list");
        assertEquals("https://temp.example.com/api/user/list", result);
    }

    @Test
    public void testIterationDataPriority() {
        IterationDataVariableService.getInstance().replaceAll(Map.of("baseUrl", "https://csv.example.com"));

        String result = VariableResolver.resolve("{{userModule}}/list");
        assertEquals("https://csv.example.com/api/user/list", result);
        assertEquals(VariableResolver.getVariableType("baseUrl"), VariableType.ITERATION_DATA);
    }

    @Test
    public void testClearVariablesClearsValuesButKeepsExecutionContextUsable() {
        VariablesService variablesService = VariablesService.getInstance();

        variablesService.set("token", "old-token");
        Map<String, String> originalContext = variablesService.getCurrentContextMap();
        assertNotNull(originalContext);

        variablesService.clearValues();

        assertTrue(variablesService.getAll().isEmpty(), "Execution-scoped values should be cleared");
        assertSame(variablesService.getCurrentContextMap(), originalContext,
                "clearVariables should keep the current execution context attached");

        variablesService.set("token", "new-token");
        assertEquals(VariableResolver.resolve("{{token}}"), "new-token");
        assertSame(variablesService.getCurrentContextMap(), originalContext,
                "Values recreated after clearVariables should stay on the same context map");
    }

    @Test
    public void testClearExecutionContextDetachesVariablesAndIterationData() {
        VariablesService variablesService = VariablesService.getInstance();
        IterationDataVariableService iterationDataService = IterationDataVariableService.getInstance();

        variablesService.set("requestId", "req-001");
        iterationDataService.replaceAll(Map.of("csvUserId", "user-001"));

        assertNotNull(variablesService.getCurrentContextMap());
        assertNotNull(iterationDataService.getCurrentContextMap());

        clearExecutionContext();

        assertNull(variablesService.getCurrentContextMap(),
                "Execution-scoped variable context should be detached after clearExecutionContext");
        assertNull(iterationDataService.getCurrentContextMap(),
                "Iteration-data context should be detached after clearExecutionContext");
        assertFalse(VariableResolver.isVariableDefined("requestId"));
        assertFalse(VariableResolver.isVariableDefined("csvUserId"));
    }

    @Test
    public void testScriptVariablesDoesNotExposeBuiltInFunctionsAsStoredVariables() {
        ScriptVariablesApi scriptVariablesApi = new ScriptVariablesApi();

        assertFalse(scriptVariablesApi.has("$guid"));
        assertFalse(scriptVariablesApi.has("$timestamp"));
        assertNull(scriptVariablesApi.get("$guid"));
        assertNull(scriptVariablesApi.get("$timestamp"));
    }

    /**
     * Kiểm tra các biến toàn cục có thể được giải quyết khi các biến môi trường không bị ảnh hưởng
     */
    @Test
    public void testGlobalVariableResolution() {
        GlobalVariablesService.getInstance().getGlobalVariables().set("globalBaseUrl", "https://global.example.com");
        GlobalVariablesService.getInstance().saveGlobalVariables();

        String result = VariableResolver.resolve("{{globalBaseUrl}}/api");
        assertEquals(result, "https://global.example.com/api");
    }

    /**
     * Kiểm tra các hàm dựng sẵn kết hợp với các biến lồng nhau
     */
    @Test
    public void testBuiltInFunctionWithNesting() {
        testEnv.set("urlWithTimestamp", "{{baseUrl}}/api?t={{$timestamp}}");

        String result = VariableResolver.resolve("{{urlWithTimestamp}}");
        assertTrue(result.startsWith("https://api.example.com/api?t="),
                "Should resolve both baseUrl and $timestamp");
        assertTrue(result.matches("https://api.example.com/api\\?t=\\d+"),
                "Timestamp should be a number");
    }

    /**
     * Kiểm tra các biến không xác định vẫn như cũ
     */
    @Test
    public void testUndefinedVariableStaysUnchanged() {
        String input = "{{baseUrl}}/{{undefined}}/list";
        String result = VariableResolver.resolve(input);
        assertEquals("https://api.example.com/{{undefined}}/list", result,
                "Undefined variable should remain unchanged");
    }

    /**
     * Kiểm tra chuỗi trống và  null
     */
    @Test
    public void testNullAndEmptyString() {
        assertNull(VariableResolver.resolve(null));
        assertEquals("", VariableResolver.resolve(""));
    }

    /**
     * Kiểm tra phát hiện tham chiếu vòng tròn
     */
    @Test
    public void testCircularReferenceDetection() {
        Map<String, String> vars = testEnv.getVariables();
        vars.put("var1", "{{var2}}");
        vars.put("var2", "{{var1}}");

        // Nó sẽ dừng lại sau khi đạt đến số lần lặp tối đa và không lặp vô hạn
        String result = VariableResolver.resolve("{{var1}}");
        assertNotNull(result);
        // Do tham chiếu vòng tròn, kết quả sẽ vẫn chứa các phần giữ chỗ có thể thay đổi
        assertTrue(result.contains("{{"), "Circular reference should stop after max iterations");
    }

    /**
     * Kiểm tra việc xử lý ký tự đặc biệt
     */
    @Test
    public void testSpecialCharacters() {
        testEnv.set("special", "value$with\\backslash");
        testEnv.set("nested", "{{special}}/path");

        String result = VariableResolver.resolve("{{nested}}");
        assertEquals(result, "value$with\\backslash/path",
                "Special characters $ and \\ should be preserved");
    }

    /**
     * Kiểm tra hàm truy vấn biến
     */
    @Test
    public void testIsVariableDefined() {
        assertTrue(VariableResolver.isVariableDefined("baseUrl"));
        assertTrue(VariableResolver.isVariableDefined("apiPath"));
        assertTrue(VariableResolver.isVariableDefined("userModule"));
        assertFalse(VariableResolver.isVariableDefined("undefined"));
        assertFalse(VariableResolver.isVariableDefined(null));
        assertFalse(VariableResolver.isVariableDefined(""));
    }

    private void clearExecutionContext() {
        VariablesService.getInstance().detachContext();
        IterationDataVariableService.getInstance().detachContext();
    }

}
