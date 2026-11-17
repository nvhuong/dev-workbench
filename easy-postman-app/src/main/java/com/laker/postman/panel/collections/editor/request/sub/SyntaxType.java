package com.laker.postman.panel.collections.editor.request.sub;

import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

/**
 * Bảng liệt kê kiểu cú pháp, quản lý thống nhất các kiểu tô sáng cú pháp
 * Việc thêm một kiểu cú pháp mới chỉ cần thêm một mục vào bảng liệt kê này
 */
@Getter
public enum SyntaxType {
    AUTO_DETECT("Auto", null),
    JSON("JSON", SyntaxConstants.SYNTAX_STYLE_JSON),
    XML("XML", SyntaxConstants.SYNTAX_STYLE_XML),
    HTML("HTML", SyntaxConstants.SYNTAX_STYLE_HTML),
    JAVASCRIPT("JavaScript", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT),
    CSS("CSS", SyntaxConstants.SYNTAX_STYLE_CSS),
    PLAIN_TEXT("Text", SyntaxConstants.SYNTAX_STYLE_NONE);

    private final String displayName;
    private final String syntaxStyle;

    SyntaxType(String displayName, String syntaxStyle) {
        this.displayName = displayName;
        this.syntaxStyle = syntaxStyle;
    }

    /**
     * Nhận một mảng tất cả các tên hiển thị (đối với hộp thả xuống)
     */
    public static String[] getDisplayNames() {
        SyntaxType[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }

    /**
     * Lấy loại cú pháp dựa trên chỉ mục
     */
    public static SyntaxType getByIndex(int index) {
        SyntaxType[] values = values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        return AUTO_DETECT;
    }

    /**
     * Lấy loại cú pháp dựa trên kiểu cú pháp
     */
    public static SyntaxType getBySyntaxStyle(String syntaxStyle) {
        if (syntaxStyle == null) {
            return AUTO_DETECT;
        }
        for (SyntaxType type : values()) {
            if (syntaxStyle.equals(type.syntaxStyle)) {
                return type;
            }
        }
        return AUTO_DETECT;
    }

    /**
     * Lấy vị trí chỉ mục
     */
    public int getIndex() {
        return ordinal();
    }
}

