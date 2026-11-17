package com.laker.postman.common.component.editor;

import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.JavaScriptTokenMaker;

import javax.swing.text.Segment;

/**
 * Sự mở rộng JavaScript TokenMaker，vì Postman Độc đáo API Thêm tô sáng cú pháp
 */
public class PostmanJavaScriptTokenMaker extends JavaScriptTokenMaker {

    /**
     * Postman Bảng ánh xạ từ khóa duy nhất
     * Bảng ánh xạ này có sẵn addToken Được sử dụng trong các phương pháp nhận dạng Postman Mã định danh duy nhất
     */
    private final TokenMap postmanWordsToHighlight;

    public PostmanJavaScriptTokenMaker() {
        super();

        // khởi tạo Postman Ánh xạ từ khóa
        postmanWordsToHighlight = new TokenMap();

        // ===== Postman cốt lõi API đối tượng - sử dụng RESERVED_WORD（màu xanh/Màu cam - quan trọng nhất)=====
        postmanWordsToHighlight.put("pm", TokenTypes.RESERVED_WORD);

        // ===== Các đối tượng thư viện tích hợp - sử dụng FUNCTION（Màu vàng/Màu cam - ít quan trọng hơn)=====
        postmanWordsToHighlight.put("console", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("CryptoJS", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("moment", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("cheerio", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("xml2Json", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("atob", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("btoa", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("_", TokenTypes.FUNCTION);  // Lodash
        postmanWordsToHighlight.put("require", TokenTypes.FUNCTION);

        // ===== Postman các phương pháp cốt lõi - sử dụng FUNCTION =====
        postmanWordsToHighlight.put("test", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("expect", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("uuid", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("getTimestamp", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("getResponseCookie", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("sendRequest", TokenTypes.FUNCTION);

        // ===== Chai Khẳng định từ khóa được xâu chuỗi - sử dụng RESERVED_WORD_2（Cùng cấp độ với từ khóa)=====
        postmanWordsToHighlight.put("to", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("be", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("been", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("is", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("that", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("which", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("and", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("has", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("have", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("with", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("at", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("of", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("same", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("not", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("deep", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("any", TokenTypes.RESERVED_WORD_2);
        postmanWordsToHighlight.put("all", TokenTypes.RESERVED_WORD_2);

        // ===== Chai Phương pháp khẳng định - sử dụng FUNCTION =====
        postmanWordsToHighlight.put("equal", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("eql", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("above", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("least", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("below", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("most", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("within", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("instanceof", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("property", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("length", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("lengthOf", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("match", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("string", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("keys", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("include", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("includes", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("contain", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("contains", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("members", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("oneOf", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("change", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("increase", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("decrease", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("throw", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("respondTo", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("satisfy", TokenTypes.FUNCTION);
        postmanWordsToHighlight.put("closeTo", TokenTypes.FUNCTION);

        // ===== Chai Khẳng định tài sản - sử dụng DATA_TYPE（phụ thuộc vào loại)=====
        postmanWordsToHighlight.put("ok", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("true", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("false", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("null", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("undefined", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("NaN", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("exist", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("empty", TokenTypes.DATA_TYPE);
        postmanWordsToHighlight.put("arguments", TokenTypes.DATA_TYPE);

        // ===== Postman Thuộc tính chung/đối tượng - sử dụng ANNOTATION（Màu vàng/Màu sắc đặc biệt - định danh thuộc tính)=====
        postmanWordsToHighlight.put("request", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("response", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("environment", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("globals", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("variables", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("cookies", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("iterationData", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("info", TokenTypes.ANNOTATION);
        postmanWordsToHighlight.put("execution", TokenTypes.ANNOTATION);

        // ===== Các câu trả lời thường dùng/Yêu cầu thuộc tính - sử dụng VARIABLE =====
        postmanWordsToHighlight.put("status", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("code", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("headers", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("body", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("text", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("json", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("responseTime", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("size", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("url", TokenTypes.VARIABLE);
        postmanWordsToHighlight.put("method", TokenTypes.VARIABLE);
    }

    /**
     * Viết lại addToken phương pháp, thêm token Kiểm tra xem nó có trước đó không Postman Từ khóa
     * <p>
     * Đây là RSyntaxTextArea Khuyến nghị thực hiện đúng:
     * 1. Vào token Chuyển đổi loại được thực hiện trước khi được thêm vào danh sách
     * 2. hơn là vào token Hãy thử sửa đổi nó sau khi tạo nó
     *
     * @param segment     đoạn văn bản
     * @param start       vị trí bắt đầu
     * @param end         vị trí kết thúc
     * @param tokenType   token loại
     * @param startOffset Bắt đầu bù đắp
     */
    @Override
    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
        // Nếu điều này token Được công nhận là định danh thông thường, hãy kiểm tra xem có đúng không Postman Từ khóa
        if (tokenType == TokenTypes.IDENTIFIER) {
            // sử dụng TokenMap.get() Phương pháp để tìm xem nó có trong bảng từ khóa của chúng tôi hay không
            int newTokenType = postmanWordsToHighlight.get(segment, start, end);

            // Nếu tìm thấy (giá trị trả về là không -1），Sau đó sử dụng cái mới token loại
            if (newTokenType != -1) {
                tokenType = newTokenType;
            }
        }

        // Gọi phương thức lớp cha để thêm token
        super.addToken(segment, start, end, tokenType, startOffset);
    }

    /**
     * Viết lại addToken của char[] Phiên bản
     * một số TokenMaker Việc triển khai sẽ gọi phiên bản này
     *
     * @param array       Mảng ký tự
     * @param start       vị trí bắt đầu
     * @param end         vị trí kết thúc
     * @param tokenType   token loại
     * @param startOffset Bắt đầu bù đắp
     * @param hyperlink   Cho dù đó là một siêu liên kết
     */
    @Override
    public void addToken(char[] array, int start, int end, int tokenType, int startOffset, boolean hyperlink) {
        // Nếu điều này token Được công nhận là định danh thông thường, hãy kiểm tra xem có đúng không Postman Từ khóa
        if (tokenType == TokenTypes.IDENTIFIER) {
            // sử dụng TokenMap.get() Phương pháp để tìm xem nó có trong bảng từ khóa của chúng tôi hay không
            int newTokenType = postmanWordsToHighlight.get(array, start, end);

            // Nếu tìm thấy (giá trị trả về là không -1），Sau đó sử dụng cái mới token loại
            if (newTokenType != -1) {
                tokenType = newTokenType;
            }
        }

        // Gọi phương thức lớp cha để thêm token
        super.addToken(array, start, end, tokenType, startOffset, hyperlink);
    }
}
