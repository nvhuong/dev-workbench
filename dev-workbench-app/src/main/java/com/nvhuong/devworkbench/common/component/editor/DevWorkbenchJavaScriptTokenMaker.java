package com.nvhuong.devworkbench.common.component.editor;

import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rsyntaxtextarea.modes.JavaScriptTokenMaker;

import javax.swing.text.Segment;

/**
 * Sự mở rộng JavaScript TokenMaker，vì DevWorkbench Độc đáo API Thêm tô sáng cú pháp
 */
public class DevWorkbenchJavaScriptTokenMaker extends JavaScriptTokenMaker {

    /**
     * DevWorkbench Bảng ánh xạ từ khóa duy nhất
     * Bảng ánh xạ này có sẵn addToken Được sử dụng trong các phương pháp nhận dạng DevWorkbench Mã định danh duy nhất
     */
    private final TokenMap devworkbenchWordsToHighlight;

    public DevWorkbenchJavaScriptTokenMaker() {
        super();

        // khởi tạo DevWorkbench Ánh xạ từ khóa
        devworkbenchWordsToHighlight = new TokenMap();

        // ===== DevWorkbench cốt lõi API đối tượng - sử dụng RESERVED_WORD（màu xanh/Màu cam - quan trọng nhất)=====
        devworkbenchWordsToHighlight.put("pm", TokenTypes.RESERVED_WORD);

        // ===== Các đối tượng thư viện tích hợp - sử dụng FUNCTION（Màu vàng/Màu cam - ít quan trọng hơn)=====
        devworkbenchWordsToHighlight.put("console", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("CryptoJS", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("moment", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("cheerio", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("xml2Json", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("atob", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("btoa", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("_", TokenTypes.FUNCTION);  // Lodash
        devworkbenchWordsToHighlight.put("require", TokenTypes.FUNCTION);

        // ===== DevWorkbench các phương pháp cốt lõi - sử dụng FUNCTION =====
        devworkbenchWordsToHighlight.put("test", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("expect", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("uuid", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("getTimestamp", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("getResponseCookie", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("sendRequest", TokenTypes.FUNCTION);

        // ===== Chai Khẳng định từ khóa được xâu chuỗi - sử dụng RESERVED_WORD_2（Cùng cấp độ với từ khóa)=====
        devworkbenchWordsToHighlight.put("to", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("be", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("been", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("is", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("that", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("which", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("and", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("has", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("have", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("with", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("at", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("of", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("same", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("not", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("deep", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("any", TokenTypes.RESERVED_WORD_2);
        devworkbenchWordsToHighlight.put("all", TokenTypes.RESERVED_WORD_2);

        // ===== Chai Phương pháp khẳng định - sử dụng FUNCTION =====
        devworkbenchWordsToHighlight.put("equal", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("eql", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("above", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("least", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("below", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("most", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("within", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("instanceof", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("property", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("length", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("lengthOf", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("match", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("string", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("keys", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("include", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("includes", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("contain", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("contains", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("members", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("oneOf", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("change", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("increase", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("decrease", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("throw", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("respondTo", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("satisfy", TokenTypes.FUNCTION);
        devworkbenchWordsToHighlight.put("closeTo", TokenTypes.FUNCTION);

        // ===== Chai Khẳng định tài sản - sử dụng DATA_TYPE（phụ thuộc vào loại)=====
        devworkbenchWordsToHighlight.put("ok", TokenTypes.DATA_TYPE);
        devworkbenchWordsToHighlight.put("true", TokenTypes.DATA_TYPE);
        devworkbenchWordsToHighlight.put("false", TokenTypes.DATA_TYPE);
        devworkbenchWordsToHighlight.put("null", TokenTypes.DATA_TYPE);
        devworkbenchWordsToHighlight.put("undefined", TokenTypes.DATA_TYPE);
        devworkbenchWordsToHighlight.put("NaN", TokenTypes.DATA_TYPE);
        devworkbenchWordsToHighlight.put("exist", TokenTypes.DATA_TYPE);
        devworkbenchWordsToHighlight.put("empty", TokenTypes.DATA_TYPE);
        devworkbenchWordsToHighlight.put("arguments", TokenTypes.DATA_TYPE);

        // ===== DevWorkbench Thuộc tính chung/đối tượng - sử dụng ANNOTATION（Màu vàng/Màu sắc đặc biệt - định danh thuộc tính)=====
        devworkbenchWordsToHighlight.put("request", TokenTypes.ANNOTATION);
        devworkbenchWordsToHighlight.put("response", TokenTypes.ANNOTATION);
        devworkbenchWordsToHighlight.put("environment", TokenTypes.ANNOTATION);
        devworkbenchWordsToHighlight.put("globals", TokenTypes.ANNOTATION);
        devworkbenchWordsToHighlight.put("variables", TokenTypes.ANNOTATION);
        devworkbenchWordsToHighlight.put("cookies", TokenTypes.ANNOTATION);
        devworkbenchWordsToHighlight.put("iterationData", TokenTypes.ANNOTATION);
        devworkbenchWordsToHighlight.put("info", TokenTypes.ANNOTATION);
        devworkbenchWordsToHighlight.put("execution", TokenTypes.ANNOTATION);

        // ===== Các câu trả lời thường dùng/Yêu cầu thuộc tính - sử dụng VARIABLE =====
        devworkbenchWordsToHighlight.put("status", TokenTypes.VARIABLE);
        devworkbenchWordsToHighlight.put("code", TokenTypes.VARIABLE);
        devworkbenchWordsToHighlight.put("headers", TokenTypes.VARIABLE);
        devworkbenchWordsToHighlight.put("body", TokenTypes.VARIABLE);
        devworkbenchWordsToHighlight.put("text", TokenTypes.VARIABLE);
        devworkbenchWordsToHighlight.put("json", TokenTypes.VARIABLE);
        devworkbenchWordsToHighlight.put("responseTime", TokenTypes.VARIABLE);
        devworkbenchWordsToHighlight.put("size", TokenTypes.VARIABLE);
        devworkbenchWordsToHighlight.put("url", TokenTypes.VARIABLE);
        devworkbenchWordsToHighlight.put("method", TokenTypes.VARIABLE);
    }

    /**
     * Viết lại addToken phương pháp, thêm token Kiểm tra xem nó có trước đó không DevWorkbench Từ khóa
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
        // Nếu điều này token Được công nhận là định danh thông thường, hãy kiểm tra xem có đúng không DevWorkbench Từ khóa
        if (tokenType == TokenTypes.IDENTIFIER) {
            // sử dụng TokenMap.get() Phương pháp để tìm xem nó có trong bảng từ khóa của chúng tôi hay không
            int newTokenType = devworkbenchWordsToHighlight.get(segment, start, end);

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
        // Nếu điều này token Được công nhận là định danh thông thường, hãy kiểm tra xem có đúng không DevWorkbench Từ khóa
        if (tokenType == TokenTypes.IDENTIFIER) {
            // sử dụng TokenMap.get() Phương pháp để tìm xem nó có trong bảng từ khóa của chúng tôi hay không
            int newTokenType = devworkbenchWordsToHighlight.get(array, start, end);

            // Nếu tìm thấy (giá trị trả về là không -1），Sau đó sử dụng cái mới token loại
            if (newTokenType != -1) {
                tokenType = newTokenType;
            }
        }

        // Gọi phương thức lớp cha để thêm token
        super.addToken(array, start, end, tokenType, startOffset, hyperlink);
    }
}
