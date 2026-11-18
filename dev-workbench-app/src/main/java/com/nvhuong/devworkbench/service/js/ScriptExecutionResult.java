package com.nvhuong.devworkbench.service.js;

import com.nvhuong.devworkbench.script.model.TestResult;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả thực thi script
 * Đóng gói thông tin kết quả thực thi tập lệnh, bao gồm trạng thái thành công, kết quả kiểm tra, thông tin lỗi, v.v
 */
@Getter
@Builder
public class ScriptExecutionResult {
    /**
     * Việc thực hiện có thành công hay không
     */
    private final boolean success;

    /**
     * Danh sách kết quả kiểm tra (chỉ phần tái bút)
     */
    @Builder.Default
    private final List<TestResult> testResults = new ArrayList<>();

    /**
     * Thông báo lỗi nếu thực thi không thành công
     */
    private final String errorMessage;

    /**
     * Đối tượng ngoại lệ nếu thực thi thất bại
     */
    private final Exception exception;

    /**
     * Tập lệnh tiền lớp có sửa đổi nội dung yêu cầu một cách rõ ràng hay không.
     */
    private final boolean requestBodyMutated;

    /**
     * Tạo kết quả thành công
     */
    public static ScriptExecutionResult success() {
        return ScriptExecutionResult.builder()
                .success(true)
                .build();
    }

    /**
     * Tạo kết quả thành công (có kết quả kiểm tra)
     */
    public static ScriptExecutionResult success(List<TestResult> testResults) {
        return ScriptExecutionResult.builder()
                .success(true)
                .testResults(testResults != null ? new ArrayList<>(testResults) : new ArrayList<>())
                .build();
    }

    /**
     * Tạo kết quả thất bại
     */
    public static ScriptExecutionResult failure(String errorMessage, Exception exception) {
        return ScriptExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .exception(exception)
                .build();
    }

    /**
     * Tạo kết quả thất bại (có kết quả kiểm tra)
     */
    public static ScriptExecutionResult failure(String errorMessage, Exception exception, List<TestResult> testResults) {
        return ScriptExecutionResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .exception(exception)
                .testResults(testResults != null ? new ArrayList<>(testResults) : new ArrayList<>())
                .build();
    }

    /**
     * Có kết quả xét nghiệm nào không?
     */
    public boolean hasTestResults() {
        return !testResults.isEmpty();
    }

    /**
     * Tất cả các bài kiểm tra đã vượt qua
     * Lưu ý: Nếu không có bài kiểm tra, hãy trả lạitrue（Không có bài kiểm tra thất bại)
     */
    public boolean allTestsPassed() {
        // Nếu không có bài kiểm tra nào thì coi như đã đậu
        if (!hasTestResults()) {
            return true;
        }
        // Nếu có bài kiểm tra, hãy kiểm tra xem tất cả chúng có vượt qua không
        return testResults.stream().allMatch(test -> test.passed);
    }
}
