package com.nvhuong.devworkbench.util;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.experimental.UtilityClass;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Json Công cụ
 */
@UtilityClass
public class JsonUtil {
    /**
     * JsonMapper Tùy chỉnh json Làm đẹp hình thức.4 Dấu cách được thụt lề, không có dấu cách nào được thêm sau tên thuộc tính và dấu cách được thêm trước giá trị thuộc tính
     * Hỗ trợ chú thích phân tích cú pháp JSON（JSON5）
     */
    private static final DefaultIndenter DEFAULT_INDENTER = new DefaultIndenter("    ", "\n");
    private static final DefaultPrettyPrinter DEFAULT_PRETTY_PRINTER = new DefaultPrettyPrinter(
            Separators.createDefaultInstance().withObjectNameValueSpacing(Separators.Spacing.AFTER))
            .withObjectIndenter(DEFAULT_INDENTER)
            .withArrayIndenter(DEFAULT_INDENTER);
    private static final JsonMapper mapper = JsonMapper.builder()
            .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
            .defaultPrettyPrinter(DEFAULT_PRETTY_PRINTER)
            .build();

    /**
     * Tạo ObjectNode
     *
     * @return ObjectNode
     */
    public static ObjectNode createJsonNode() {
        return mapper.createObjectNode();
    }

    /**
     * Tạo ArrayNode
     *
     * @return ArrayNode
     */
    public static ArrayNode createArrayNode() {
        return mapper.createArrayNode();
    }

    /**
     * Chuyển đối tượngjson
     *
     * @param object Đối tượng cần chuyển đổi
     * @return json
     */
    public static String toJsonStr(Object object) {
        return mapper.writeValueAsString(object);
    }

    /**
     * Chuyển đổi văn bản bình thường thành có thể nhúng JSON Văn bản thoát của nội dung chuỗi, không bao gồm dấu ngoặc kép bên ngoài
     *
     * @param text Văn bản gốc
     * @return Nội dung văn bản đã thoát
     */
    public static String escapeJsonStringContent(String text) {
        if (text == null) {
            return null;
        }
        String json = mapper.writeValueAsString(text);
        return json.length() >= 2 ? json.substring(1, json.length() - 1) : json;
    }

    /**
     * Vâng JSON Nội dung chuỗi được thoát trong một lớp duy nhất và không yêu cầu dấu ngoặc kép bên ngoài
     *
     * @param text Nội dung văn bản đã thoát
     * @return Văn bản không thoát
     */
    public static String unescapeJsonStringContent(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch != '\\' || i == text.length() - 1) {
                sb.append(ch);
                continue;
            }

            char next = text.charAt(++i);
            switch (next) {
                case '"':
                    sb.append('"');
                    break;
                case '\\':
                    sb.append('\\');
                    break;
                case '/':
                    sb.append('/');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'u':
                    if (i + 4 < text.length()) {
                        String hex = text.substring(i + 1, i + 5);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                            break;
                        } catch (NumberFormatException ignored) {
                            // Để nguyên như vậy
                        }
                    }
                    sb.append('\\').append('u');
                    break;
                default:
                    sb.append('\\').append(next);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * json Làm đẹp dây
     *
     * @param json Để được chuyển đổi json chuỗi
     * @return json
     */
    public static String toJsonPrettyStr(String json) {
        JsonNode rootNode = mapper.readTree(json);
        if (containsJsonComment(json)) {
            return formatJsonWithComments(json);
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }

    /**
     * jsonLàm đẹp đồ vật
     *
     * @param object Đối tượng cần chuyển đổi
     * @return json
     */
    public static String toJsonPrettyStr(Object object) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    /**
     * Dọn dẹp json Chú thích (trả về đơn giản sau khi xóa chú thích JSON）
     *
     * @param json Để được làm sạch json
     * @return Sau khi làm sạch json
     */
    public static String cleanJsonComments(String json) {
        return mapper.readTree(json).toString();
    }


    /**
     * Xác định xem chuỗi có hợp lệ hay không JSON（Chú thích được hỗ trợ JSON5）
     *
     * @param json Chuỗi để được đánh giá
     * @return Nó có hợp lệ không? JSON/JSON5
     */
    public static boolean isTypeJSON(String json) {
        if (CharSequenceUtil.isBlank(json)) return false;
        try {
            JsonNode rootNode = mapper.readTree(json);
            // Xác nhận nghiêm ngặt: chỉ cóJSONđối tượng hoặcJSONMảng được trả vềtrue
            return rootNode.isObject() || rootNode.isArray();
        } catch (JacksonException e) {
            return false;
        }
    }

    /**
     * Compares JSON objects or arrays by structure, ignoring object field order and formatting.
     */
    public static boolean isStructurallyEqual(String left, String right) {
        if (CharSequenceUtil.isBlank(left) || CharSequenceUtil.isBlank(right)) return false;
        try {
            JsonNode leftNode = mapper.readTree(left);
            JsonNode rightNode = mapper.readTree(right);
            boolean structured = (leftNode.isObject() || leftNode.isArray())
                    && (rightNode.isObject() || rightNode.isArray());
            return structured && leftNode.equals(rightNode);
        } catch (JacksonException exception) {
            return false;
        }
    }

    /**
     * Đọc json
     *
     * @param json Để được đọc json
     * @return JsonNode
     */
    public static JsonNode readTree(String json) {
        return mapper.readTree(json);
    }

    /**
     * Bản sao sâu sắc
     *
     * @param object Đối tượng cần sao chép
     * @param clazz  Loại đối tượng mục tiêu
     * @param <T>    Loại đối tượng mục tiêu
     * @return Đối tượng được sao chép
     */
    public static <T> T deepCopy(T object, Class<T> clazz) {
        if (object == null) {
            return null;
        }
        return mapper.readValue(mapper.writeValueAsBytes(object), clazz);
    }

    /**
     * Chuyển đổi kiểu
     *
     * @param fromValue   đối tượng nguồn
     * @param toValueType Loại chuyển đổi
     * @param <T>         Thẻ chung
     * @return Kết quả chuyển đổi
     */
    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        return mapper.convertValue(fromValue, toValueType);
    }

    private static boolean containsJsonComment(String json) {
        if (json == null || json.length() < 2) {
            return false;
        }

        boolean inString = false;
        boolean escaping = false;
        for (int i = 0; i < json.length(); i++) {
            char current = json.charAt(i);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '/' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '/' || next == '*') {
                    return true;
                }
            }
        }
        return false;
    }

    private static String formatJsonWithComments(String json) {
        List<JsonFormatToken> tokens = tokenizeJsonWithComments(json);
        StringBuilder formatted = new StringBuilder(json.length());
        int indentLevel = 0;
        boolean atLineStart = true;

        for (int i = 0; i < tokens.size(); i++) {
            JsonFormatToken token = tokens.get(i);
            JsonFormatToken previous = i > 0 ? tokens.get(i - 1) : null;
            JsonFormatToken next = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
            switch (token.type()) {
                case OPEN_OBJECT, OPEN_ARRAY -> {
                    atLineStart = appendIndentIfNeeded(formatted, indentLevel, atLineStart);
                    formatted.append(token.text());
                    atLineStart = false;
                    indentLevel++;
                    if (isMatchingClose(token, next)) {
                        continue;
                    }
                    if (isLineCommentOnSameLine(token, next)) {
                        formatted.append(' ');
                    } else {
                        atLineStart = appendNewLine(formatted);
                    }
                }
                case CLOSE_OBJECT, CLOSE_ARRAY -> {
                    indentLevel = Math.max(0, indentLevel - 1);
                    if (!isMatchingClose(previous, token) && !atLineStart) {
                        atLineStart = appendNewLine(formatted);
                    }
                    atLineStart = appendIndentIfNeeded(formatted, indentLevel, atLineStart);
                    formatted.append(token.text());
                    atLineStart = false;
                }
                case COLON -> {
                    formatted.append(": ");
                    atLineStart = false;
                }
                case COMMA -> {
                    atLineStart = appendIndentIfNeeded(formatted, indentLevel, atLineStart);
                    formatted.append(',');
                    atLineStart = false;
                    if (isLineCommentOnSameLine(token, next)) {
                        formatted.append(' ');
                    } else {
                        atLineStart = appendNewLine(formatted);
                    }
                }
                case LINE_COMMENT -> {
                    atLineStart = appendIndentIfNeeded(formatted, indentLevel, atLineStart);
                    appendSpaceIfNeeded(formatted);
                    formatted.append(token.text().trim());
                    atLineStart = appendNewLine(formatted);
                }
                case BLOCK_COMMENT -> atLineStart = appendBlockComment(formatted, token.text(), indentLevel, atLineStart);
                case VALUE -> {
                    atLineStart = appendIndentIfNeeded(formatted, indentLevel, atLineStart);
                    formatted.append(token.text());
                    atLineStart = false;
                }
            }
        }

        trimTrailingNewLines(formatted);
        return formatted.toString();
    }

    private static boolean isMatchingClose(JsonFormatToken token, JsonFormatToken next) {
        return next != null
                && ((token.type() == JsonFormatTokenType.OPEN_OBJECT && next.type() == JsonFormatTokenType.CLOSE_OBJECT)
                || (token.type() == JsonFormatTokenType.OPEN_ARRAY && next.type() == JsonFormatTokenType.CLOSE_ARRAY));
    }

    private static boolean isLineCommentOnSameLine(JsonFormatToken token, JsonFormatToken next) {
        return next != null
                && next.type() == JsonFormatTokenType.LINE_COMMENT
                && token.endLine() == next.startLine();
    }

    private static boolean appendIndentIfNeeded(StringBuilder formatted, int indentLevel, boolean atLineStart) {
        if (!atLineStart) {
            return false;
        }
        formatted.append(DEFAULT_INDENTER.getIndent().repeat(Math.max(0, indentLevel)));
        return false;
    }

    private static boolean appendNewLine(StringBuilder formatted) {
        trimTrailingSpaces(formatted);
        if (formatted.length() == 0 || formatted.charAt(formatted.length() - 1) != '\n') {
            formatted.append('\n');
        }
        return true;
    }

    private static void appendSpaceIfNeeded(StringBuilder formatted) {
        if (formatted.length() == 0) {
            return;
        }
        char last = formatted.charAt(formatted.length() - 1);
        if (!Character.isWhitespace(last)) {
            formatted.append(' ');
        }
    }

    private static boolean appendBlockComment(StringBuilder formatted, String comment, int indentLevel, boolean atLineStart) {
        boolean standalone = atLineStart;
        if (comment.indexOf('\n') >= 0 || comment.indexOf('\r') >= 0) {
            if (!atLineStart) {
                atLineStart = appendNewLine(formatted);
            }
            String normalized = comment.replace("\r\n", "\n").replace('\r', '\n');
            String[] lines = normalized.split("\n", -1);
            for (String line : lines) {
                if (line.isEmpty()) {
                    continue;
                }
                appendIndentIfNeeded(formatted, indentLevel, true);
                formatted.append(line.stripLeading());
                atLineStart = appendNewLine(formatted);
            }
            return atLineStart;
        }

        atLineStart = appendIndentIfNeeded(formatted, indentLevel, atLineStart);
        appendSpaceIfNeeded(formatted);
        formatted.append(comment.trim());
        if (standalone) {
            return appendNewLine(formatted);
        }
        return false;
    }

    private static void trimTrailingSpaces(StringBuilder formatted) {
        while (formatted.length() > 0) {
            char last = formatted.charAt(formatted.length() - 1);
            if (last != ' ' && last != '\t') {
                return;
            }
            formatted.deleteCharAt(formatted.length() - 1);
        }
    }

    private static void trimTrailingNewLines(StringBuilder formatted) {
        while (formatted.length() > 0) {
            char last = formatted.charAt(formatted.length() - 1);
            if (last != '\n' && last != '\r') {
                return;
            }
            formatted.deleteCharAt(formatted.length() - 1);
        }
    }

    private static List<JsonFormatToken> tokenizeJsonWithComments(String json) {
        List<JsonFormatToken> tokens = new ArrayList<>();
        int line = 1;
        int index = 0;
        while (index < json.length()) {
            char current = json.charAt(index);
            if (Character.isWhitespace(current)) {
                if (current == '\r') {
                    if (index + 1 < json.length() && json.charAt(index + 1) == '\n') {
                        index++;
                    }
                    line++;
                } else if (current == '\n') {
                    line++;
                }
                index++;
                continue;
            }

            int startLine = line;
            switch (current) {
                case '{' -> {
                    tokens.add(new JsonFormatToken(JsonFormatTokenType.OPEN_OBJECT, "{", startLine, startLine));
                    index++;
                }
                case '}' -> {
                    tokens.add(new JsonFormatToken(JsonFormatTokenType.CLOSE_OBJECT, "}", startLine, startLine));
                    index++;
                }
                case '[' -> {
                    tokens.add(new JsonFormatToken(JsonFormatTokenType.OPEN_ARRAY, "[", startLine, startLine));
                    index++;
                }
                case ']' -> {
                    tokens.add(new JsonFormatToken(JsonFormatTokenType.CLOSE_ARRAY, "]", startLine, startLine));
                    index++;
                }
                case ':' -> {
                    tokens.add(new JsonFormatToken(JsonFormatTokenType.COLON, ":", startLine, startLine));
                    index++;
                }
                case ',' -> {
                    tokens.add(new JsonFormatToken(JsonFormatTokenType.COMMA, ",", startLine, startLine));
                    index++;
                }
                case '"' -> {
                    TokenReadResult result = readJsonString(json, index, line);
                    tokens.add(new JsonFormatToken(JsonFormatTokenType.VALUE, result.text(), startLine, result.endLine()));
                    index = result.nextIndex();
                    line = result.endLine();
                }
                case '/' -> {
                    if (index + 1 < json.length() && json.charAt(index + 1) == '/') {
                        TokenReadResult result = readLineComment(json, index, line);
                        tokens.add(new JsonFormatToken(JsonFormatTokenType.LINE_COMMENT, result.text(), startLine, result.endLine()));
                        index = result.nextIndex();
                    } else if (index + 1 < json.length() && json.charAt(index + 1) == '*') {
                        TokenReadResult result = readBlockComment(json, index, line);
                        tokens.add(new JsonFormatToken(JsonFormatTokenType.BLOCK_COMMENT, result.text(), startLine, result.endLine()));
                        index = result.nextIndex();
                        line = result.endLine();
                    } else {
                        TokenReadResult result = readJsonLiteral(json, index, line);
                        tokens.add(new JsonFormatToken(JsonFormatTokenType.VALUE, result.text(), startLine, result.endLine()));
                        index = result.nextIndex();
                    }
                }
                default -> {
                    TokenReadResult result = readJsonLiteral(json, index, line);
                    tokens.add(new JsonFormatToken(JsonFormatTokenType.VALUE, result.text(), startLine, result.endLine()));
                    index = result.nextIndex();
                }
            }
        }
        return tokens;
    }

    private static TokenReadResult readJsonString(String json, int startIndex, int startLine) {
        boolean escaping = false;
        int line = startLine;
        int index = startIndex + 1;
        while (index < json.length()) {
            char current = json.charAt(index);
            if (current == '\r') {
                if (index + 1 < json.length() && json.charAt(index + 1) == '\n') {
                    index++;
                }
                line++;
            } else if (current == '\n') {
                line++;
            }

            if (escaping) {
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else if (current == '"') {
                index++;
                break;
            }
            index++;
        }
        return new TokenReadResult(json.substring(startIndex, index), index, line);
    }

    private static TokenReadResult readLineComment(String json, int startIndex, int startLine) {
        int index = startIndex + 2;
        while (index < json.length() && json.charAt(index) != '\n' && json.charAt(index) != '\r') {
            index++;
        }
        return new TokenReadResult(json.substring(startIndex, index), index, startLine);
    }

    private static TokenReadResult readBlockComment(String json, int startIndex, int startLine) {
        int line = startLine;
        int index = startIndex + 2;
        while (index < json.length()) {
            char current = json.charAt(index);
            if (current == '\r') {
                if (index + 1 < json.length() && json.charAt(index + 1) == '\n') {
                    index++;
                }
                line++;
            } else if (current == '\n') {
                line++;
            }
            if (current == '*' && index + 1 < json.length() && json.charAt(index + 1) == '/') {
                index += 2;
                break;
            }
            index++;
        }
        return new TokenReadResult(json.substring(startIndex, index), index, line);
    }

    private static TokenReadResult readJsonLiteral(String json, int startIndex, int startLine) {
        int index = startIndex;
        while (index < json.length()) {
            char current = json.charAt(index);
            if (Character.isWhitespace(current)
                    || current == '{'
                    || current == '}'
                    || current == '['
                    || current == ']'
                    || current == ':'
                    || current == ','
                    || current == '"'
                    || (current == '/' && index + 1 < json.length()
                    && (json.charAt(index + 1) == '/' || json.charAt(index + 1) == '*'))) {
                break;
            }
            index++;
        }
        return new TokenReadResult(json.substring(startIndex, index), index, startLine);
    }

    private enum JsonFormatTokenType {
        OPEN_OBJECT,
        CLOSE_OBJECT,
        OPEN_ARRAY,
        CLOSE_ARRAY,
        COLON,
        COMMA,
        LINE_COMMENT,
        BLOCK_COMMENT,
        VALUE
    }

    private record JsonFormatToken(JsonFormatTokenType type, String text, int startLine, int endLine) {
    }

    private record TokenReadResult(String text, int nextIndex, int endLine) {
    }

}
