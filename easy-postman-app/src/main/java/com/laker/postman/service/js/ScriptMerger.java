package com.laker.postman.service.js;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp công cụ hợp nhất tập lệnh
 * Được sử dụng để hợp nhất các tập lệnh từ các cấp độ khác nhau (nhóm tập lệnh, tập lệnh yêu cầu, v.v.)
 */
@UtilityClass
public class ScriptMerger {

    private static final String SEPARATOR = "\n\n";
    private static final String SECTION_COMMENT_TEMPLATE = "// === %s ===\n\n";

    /**
     * Kết hợp nhiều đoạn script
     *
     * @param scripts Danh sách các đoạn script (theo thứ tự thực hiện)
     * @return Kịch bản hợp nhất
     */
    public static String merge(List<ScriptFragment> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            return null;
        }

        // Lọc các tập lệnh trống
        List<ScriptFragment> validScripts = scripts.stream()
                .filter(s -> s != null && s.getContent() != null && !s.getContent().trim().isEmpty())
                .toList();

        if (validScripts.isEmpty()) {
            return null;
        }

        if (validScripts.size() == 1) {
            return validScripts.get(0).getContent();
        }

        // Kết hợp nhiều tập lệnh
        StringBuilder merged = new StringBuilder();
        for (int i = 0; i < validScripts.size(); i++) {
            ScriptFragment fragment = validScripts.get(i);

            // Thêm mã định danh chú thích (nếu tên nguồn được cung cấp)
            if (fragment.getSource() != null && !fragment.getSource().isEmpty()) {
                merged.append(String.format(SECTION_COMMENT_TEMPLATE, fragment.getSource()));
            }

            // Thêm nội dung tập lệnh
            merged.append(fragment.getContent());

            // Nếu không phải là tập lệnh cuối cùng, hãy thêm dấu phân cách
            if (i < validScripts.size() - 1) {
                merged.append(SEPARATOR);
            }
        }

        return merged.toString();
    }

    /**
     * Hợp nhất các tập lệnh trước (thứ tự từ ngoài vào trong)
     *
     * @param outerScripts  Kịch bản bên ngoài (phân nhóm theo thứ bậc, từ ngoài vào trong)
     * @param requestScript Yêu cầu kịch bản riêng của mình
     * @return Hợp nhất tiền kịch bản
     */
    public static String mergePreScripts(List<ScriptFragment> outerScripts, String requestScript) {
        List<ScriptFragment> allScripts = new ArrayList<>();

        // Thêm các tập lệnh bên ngoài trước (theo thứ tự từ ngoài vào trong)
        if (outerScripts != null) {
            allScripts.addAll(outerScripts);
        }

        // Cuối cùng thêm tập lệnh yêu cầu
        if (requestScript != null && !requestScript.trim().isEmpty()) {
            allScripts.add(new ScriptFragment("Kịch bản cấp độ yêu cầu", requestScript));
        }

        return merge(allScripts);
    }

    /**
     * Hợp nhất các tập lệnh bài đăng (thứ tự từ trong ra ngoài)
     *
     * @param requestScript Yêu cầu kịch bản riêng của mình
     * @param outerScripts  Kịch bản bên ngoài (phân nhóm theo thứ bậc, từ trong ra ngoài)
     * @return Đã hợp nhất phần tái bút
     */
    public static String mergePostScripts(String requestScript, List<ScriptFragment> outerScripts) {
        List<ScriptFragment> allScripts = new ArrayList<>();

        // Đầu tiên thêm tập lệnh yêu cầu
        if (requestScript != null && !requestScript.trim().isEmpty()) {
            allScripts.add(new ScriptFragment("Kịch bản cấp độ yêu cầu", requestScript));
        }

        // Sau đó thêm script bên ngoài (theo thứ tự từ trong ra ngoài)
        if (outerScripts != null) {
            allScripts.addAll(outerScripts);
        }

        return merge(allScripts);
    }


}

