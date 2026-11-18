package com.nvhuong.devworkbench.variable;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp công cụ phân tích cú pháp biến
 * <p>
 * Chịu trách nhiệm phân tích các phần giữ chỗ biến trong chuỗi {{variableName}}
 */
@UtilityClass
public class VariableParser {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    /**
     * Lấy đoạn biến trong chuỗi
     *
     * @param value Một chuỗi chứa các biến
     * @return Danh sách các phân đoạn có thể thay đổi
     */
    public static List<VariableSegment> getVariableSegments(String value) {
        List<VariableSegment> segments = new ArrayList<>();
        if (value == null) {
            return segments;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(value);
        while (matcher.find()) {
            segments.add(new VariableSegment(matcher.start(), matcher.end(), matcher.group(1)));
        }
        return segments;
    }
}
