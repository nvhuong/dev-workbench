package com.nvhuong.devworkbench.service.js;

import lombok.Getter;

/**
 * Đoạn kịch bản
 * <p>
 * Đại diện cho một đoạn tập lệnh thực thi, bao gồm nội dung tập lệnh và nhận dạng nguồn.
 * Chủ yếu được sử dụng trong các tình huống kế thừa tập lệnh để phân biệt xem tập lệnh xuất phát từ chính yêu cầu hay được kế thừa từ nhóm mẹ.
 * </p>
 *
 * <h3>Tình huống sử dụng:</h3>
 * <ul>
 *   <li>Kế thừa tập lệnh cấp nhóm</li>
 *   <li>Kết hợp các tập lệnh từ nhiều nguồn</li>
 *   <li>Truy tìm và gỡ lỗi nguồn tập lệnh</li>
 * </ul>
 *
 * <h3>Ví dụ:</h3>
 * <pre>{@code
 * // Tạo một đoạn mã yêu cầu chính nó
 * ScriptFragment requestScript = new ScriptFragment(
 *     "pm.environment.set('token', 'abc123');",
 *     "Request: /api/login"
 * );
 *
 * // Tạo các đoạn script được kế thừa
 * ScriptFragment inheritedScript = new ScriptFragment(
 *     "pm.request.headers.add({key: 'X-API-Key', value: pm.environment.get('apiKey')});",
 *     "Group: API Requests"
 * );
 *
 * // Hợp nhất kịch bản
 * List<ScriptFragment> fragments = Arrays.asList(inheritedScript, requestScript);
 * String mergedScript = ScriptMerger.merge(fragments);
 * }</pre>
 *
 * @see ScriptMerger
 * @author nvhuong
 */
@Getter
public class ScriptFragment {
    /**
     * Nội dung tập lệnh (JavaScript mã số)
     */
    private final String content;

    /**
     * Mã định danh nguồn tập lệnh
     * <p>
     * Được sử dụng để xác định nguồn của tập lệnh để thuận tiện cho việc gỡ lỗi và truy tìm. Ví dụ:
     * <ul>
     *   <li>"Request: /api/users"</li>
     *   <li>"Group: API Requests"</li>
     *   <li>"Inherited: Root Collection"</li>
     * </ul>
     * </p>
     */
    private final String source;

    /**
     * Xây dựng một đoạn script (có nhận dạng nguồn)
     *
     * @param source Mã định danh nguồn tập lệnh
     * @param content Nội dung kịch bản
     */
    public ScriptFragment(String source, String content) {
        this.source = source;
        this.content = content;
    }

    /**
     * Xây dựng một đoạn script (không xác định nguồn)
     *
     * @param content Nội dung kịch bản
     */
    public ScriptFragment(String content) {
        this(null, content);
    }

    /**
     * Phương pháp xuất xưởng: Tạo một đoạn tập lệnh có nhận dạng nguồn
     *
     * @param source Mã định danh nguồn tập lệnh
     * @param content Nội dung kịch bản
     * @return Ví dụ về đoạn mã
     */
    public static ScriptFragment of(String source, String content) {
        return new ScriptFragment(source, content);
    }

    /**
     * Phương pháp xuất xưởng: Tạo một đoạn tập lệnh mà không cần xác định nguồn
     *
     * @param content Nội dung kịch bản
     * @return Ví dụ về đoạn mã
     */
    public static ScriptFragment of(String content) {
        return new ScriptFragment(content);
    }
}