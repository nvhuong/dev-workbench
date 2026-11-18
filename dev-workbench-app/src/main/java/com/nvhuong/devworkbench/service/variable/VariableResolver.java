package com.nvhuong.devworkbench.service.variable;

import com.nvhuong.devworkbench.variable.VariableInfo;
import com.nvhuong.devworkbench.service.GlobalVariablesService;
import com.nvhuong.devworkbench.variable.VariableType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dịch vụ có độ phân giải thay đổi
 * <p>
 * Phân tích chuỗi {{variableName}} Giữ chỗ, hỗ trợ phân tích cú pháp lồng nhau
 * <p>
 * Mức độ ưu tiên của biến: biến thực thi > Lặp lại dữ liệu > Nhóm các biến > các biến môi trường > Các biến toàn cầu > Các chức năng tích hợp
 */
@Slf4j
@UtilityClass
public class VariableResolver {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    /**
     * Danh sách các nhà cung cấp biến (sắp xếp theo mức độ ưu tiên)
     */
    private static final List<VariableProvider> PROVIDERS;

    static {
        List<VariableProvider> providers = new ArrayList<>(Arrays.asList(
                VariablesService.getInstance(),
                IterationDataVariableService.getInstance(),
                GroupVariableService.getInstance(),
                EnvironmentVariableService.getInstance(),
                GlobalVariablesService.getInstance(),
                BuiltInFunctionService.getInstance()
        ));
        providers.sort(Comparator.comparingInt(VariableProvider::getPriority));
        PROVIDERS = Collections.unmodifiableList(providers);
    }

    /**
     * Replace variable placeholders in text (nested parsing supported)
     * <p>
     * Ví dụ: {{baseUrl}}/api/users -> http://api.example.com/api/users
     */
    public static String resolve(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Nhiều vòng phân tích cú pháp hỗ trợ các biến lồng nhau, tối đa  10 Bánh xe ngăn chặn các tham chiếu vòng tròn
        String result = text;
        int maxIterations = 10;
        int iteration = 0;

        while (iteration < maxIterations) {
            String beforeResolve = result;
            result = resolveOnce(result);

            if (result.equals(beforeResolve)) {
                break; // Không có thay đổi nào, quá trình phân tích cú pháp đã hoàn tất
            }

            iteration++;
        }

        if (iteration >= maxIterations) {
            log.warn("Độ phân giải thay đổi đạt đến số lần lặp tối đa({}), Có thể có tài liệu tham khảo vòng tròn: {}", maxIterations, text);
        }

        return result;
    }

    /**
     * Thực hiện một vòng phân tích biến
     */
    private static String resolveOnce(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = VAR_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = resolveVariable(varName);

            if (value == null) {
                // Biến không tồn tại và được giữ nguyên
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Phân tích một biến duy nhất (tìm theo mức độ ưu tiên)
     */
    public static String resolveVariable(String varName) {
        if (varName == null || varName.isEmpty()) {
            return null;
        }

        for (VariableProvider provider : PROVIDERS) {
            if (provider.has(varName)) {
                String value = provider.get(varName);
                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }

    /**
     * Kiểm tra xem biến đã được xác định chưa
     */
    public static boolean isVariableDefined(String varName) {
        if (varName == null || varName.isEmpty()) {
            return false;
        }

        return PROVIDERS.stream().anyMatch(provider -> provider.has(varName));
    }

    /**
     * Lấy loại biến
     *
     * @param varName tên biến
     * @return Kiểu biến, trả về nếu biến không tồn tại null
     */
    public static VariableType getVariableType(String varName) {
        if (varName == null || varName.isEmpty()) {
            return null;
        }

        for (VariableProvider provider : PROVIDERS) {
            if (provider.has(varName)) {
                return provider.getType();
            }
        }

        return null;
    }

    /**
     * Nhận tất cả các biến có sẵn (chứa thông tin loại)
     *
     * @return Danh sách thông tin thay đổi
     */
    public static List<VariableInfo> getAllAvailableVariablesWithType() {
        Map<String, VariableInfo> varMap = new LinkedHashMap<>();

        // Thêm theo mức độ ưu tiên, sử dụng putIfAbsent Tránh bị ghi đè bởi mức độ ưu tiên thấp
        for (VariableProvider provider : PROVIDERS) {
            Map<String, String> providerVars = provider.getAll();
            for (Map.Entry<String, String> entry : providerVars.entrySet()) {
                varMap.putIfAbsent(entry.getKey(),
                        new VariableInfo(entry.getKey(), entry.getValue(), provider.getType()));
            }
        }

        return new ArrayList<>(varMap.values());
    }

    /**
     * Lọc danh sách các biến (chứa thông tin loại) dựa trên tiền tố
     *
     * @param prefix Lọc tiền tố
     * @return Danh sách thông tin thay đổi
     */
    public static List<VariableInfo> filterVariablesWithType(String prefix) {
        List<VariableInfo> allVars = getAllAvailableVariablesWithType();
        if (prefix == null || prefix.isEmpty()) {
            return allVars;
        }

        List<VariableInfo> filtered = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();
        for (VariableInfo varInfo : allVars) {
            if (varInfo.getName().toLowerCase().startsWith(lowerPrefix)) {
                filtered.add(varInfo);
            }
        }
        return filtered;
    }
}
