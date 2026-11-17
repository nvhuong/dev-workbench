package com.laker.postman.service.js;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình bối cảnh thực thi tập lệnh
 * <p>
 * Đóng gói tất cả thông tin cấu hình và dữ liệu thời gian chạy cần thiết để thực thi tập lệnh, bao gồm nội dung tập lệnh và các ràng buộc biến.
 * Loại tập lệnh, v.v. Lớp này đóng vai trò là phương tiện truyền dữ liệu giữa các thành phần trong quá trình thực thi tập lệnh.
 * </p>
 *
 * <h3>Trách nhiệm chính:</h3>
 * <ul>
 *   <li>Lưu trữ nội dung và loại tập lệnh (thêm trước/ở phía sau)</li>
 *   <li>Quản lý các ràng buộc biến để thực thi tập lệnh (ví dụ: pm）</li>
 *   <li>Chế độ Builder được cung cấp để tạo điều kiện thuận lợi cho việc tạo các phiên bản</li>
 * </ul>
 *
 * <h3>Ví dụ sử dụng:</h3>
 * <pre>{@code
 * // Sử dụng trình xây dựng để tạo bối cảnh thực thi
 * ScriptExecutionContext context = ScriptExecutionContext.builder()
 *     .script("pm.environment.set('timestamp', Date.now());")
 *     .scriptType(ScriptType.PRE_REQUEST)
 *     .bindings(bindings)
 *     .build();
 *
 * // Thực thi kịch bản
 * JsScriptExecutor.execute(context);
 * }</pre>
 *
 * @see JsScriptExecutor
 * @see ScriptType
 * @author laker
 */
@Getter
@Builder
public class ScriptExecutionContext {
    /**
     * Nội dung tập lệnh (JavaScript mã số)
     */
    private final String script;

    /**
     * Loại tập lệnh (được sử dụng để nhận dạng nhật ký và theo dõi lỗi)
     * @see ScriptType
     */
    private final ScriptType scriptType;

    /**
     * Ràng buộc biến đổi Map
     * <p>
     * Chứa tất cả các biến có thể truy cập được khi tập lệnh được thực thi, ví dụ: .
     * <ul>
     *   <li>"pm" → PostmanApiContext Ví dụ</li>
     * </ul>
     * </p>
     */
    @Builder.Default
    private final Map<String, Object> bindings = new HashMap<>();

    /**
     * Đầu ra gọi lại
     */
    private final JsScriptExecutor.OutputCallback outputCallback;

    /**
     * Có hiển thị hộp thoại về lỗi hay không
     */
    @Builder.Default
    private final boolean showErrorDialog = false;

    /**
     * Kiểu liệt kê tập lệnh
     */
    @Getter
    public enum ScriptType {
        PRE_REQUEST("PreScript"),
        POST_REQUEST("PostScript"),
        TEST("Test"),
        CUSTOM("Custom");

        private final String displayName;

        ScriptType(String displayName) {
            this.displayName = displayName;
        }

    }

    /**
     * Thêm các biến liên kết
     */
    public void addBinding(String name, Object value) {
        bindings.put(name, value);
    }

    /**
     * Thêm các biến liên kết theo đợt
     */
    public void addBindings(Map<String, Object> newBindings) {
        if (newBindings != null) {
            bindings.putAll(newBindings);
        }
    }
}
