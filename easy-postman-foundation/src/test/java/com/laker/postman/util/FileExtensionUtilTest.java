package com.laker.postman.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * FileExtensionUtil Lớp kiểm tra đơn vị
 * Kiểm tra khả năng suy luận phần mở rộng tệp, tạo tên tệp và xác định loại
 */
public class FileExtensionUtilTest {

    // ==================== guessExtension Bài kiểm tra ====================

    @DataProvider(name = "documentContentTypes")
    public Object[][] documentContentTypes() {
        return new Object[][]{
                // Office Tài liệu (định dạng hiện đại)
                {"application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"},
                {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"},
                {"application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"},
                // Office Tài liệu (định dạng cũ)
                {"application/msword", ".doc"},
                {"application/vnd.ms-excel", ".xls"},
                {"application/vnd.ms-powerpoint", ".ppt"},
                // PDF
                {"application/pdf", ".pdf"},
        };
    }

    @Test(dataProvider = "documentContentTypes", description = "Kiểm tra suy luận mở rộng cho các loại tài liệu")
    public void testGuessExtension_Documents(String contentType, String expectedExt) {
        assertEquals(FileExtensionUtil.guessExtension(contentType), expectedExt,
                "Content-Type: " + contentType + " Phần mở rộng phải được trả lại: " + expectedExt);
    }

    @DataProvider(name = "textContentTypes")
    public Object[][] textContentTypes() {
        return new Object[][]{
                {"application/json", ".json"},
                {"text/json", ".json"},
                {"application/xml", ".xml"},
                {"text/xml", ".xml"},
                {"text/html", ".html"},
                {"application/javascript", ".js"},
                {"text/javascript", ".js"},
                {"text/css", ".css"},
                {"text/csv", ".csv"},
                {"text/plain", ".txt"},
                {"application/yaml", ".yaml"},
                {"text/yaml", ".yaml"},
        };
    }

    @Test(dataProvider = "textContentTypes", description = "Kiểm tra suy luận mở rộng cho các loại văn bản")
    public void testGuessExtension_TextTypes(String contentType, String expectedExt) {
        assertEquals(FileExtensionUtil.guessExtension(contentType), expectedExt);
    }

    @DataProvider(name = "imageContentTypes")
    public Object[][] imageContentTypes() {
        return new Object[][]{
                {"image/png", ".png"},
                {"image/jpeg", ".jpg"},
                {"image/gif", ".gif"},
                {"image/bmp", ".bmp"},
                {"image/webp", ".webp"},
                {"image/svg+xml", ".svg"},
                {"image/x-icon", ".ico"},
        };
    }

    @Test(dataProvider = "imageContentTypes", description = "Kiểm tra suy luận mở rộng cho các loại hình ảnh")
    public void testGuessExtension_Images(String contentType, String expectedExt) {
        assertEquals(FileExtensionUtil.guessExtension(contentType), expectedExt);
    }

    @DataProvider(name = "archiveContentTypes")
    public Object[][] archiveContentTypes() {
        return new Object[][]{
                {"application/zip", ".zip"},
                {"application/x-zip-compressed", ".zip"},
                {"application/gzip", ".gz"},
                {"application/x-tar", ".tar"},
                {"application/x-7z-compressed", ".7z"},
                {"application/x-rar-compressed", ".rar"},
        };
    }

    @Test(dataProvider = "archiveContentTypes", description = "Kiểm tra suy luận mở rộng cho các loại tệp nén")
    public void testGuessExtension_Archives(String contentType, String expectedExt) {
        assertEquals(FileExtensionUtil.guessExtension(contentType), expectedExt);
    }

    @DataProvider(name = "mediaContentTypes")
    public Object[][] mediaContentTypes() {
        return new Object[][]{
                // Âm thanh
                {"audio/mpeg", ".mp3"},
                {"audio/mp3", ".mp3"},
                {"audio/wav", ".wav"},
                {"audio/x-wav", ".wav"},
                {"audio/ogg", ".ogg"},
                {"audio/aac", ".aac"},
                {"audio/mp4", ".m4a"},
                {"audio/x-m4a", ".m4a"},
                {"audio/flac", ".flac"},
                {"audio/aiff", ".aiff"},
                {"audio/basic", ".au"},
                {"audio/webm", ".webm"},
                {"audio/opus", ".opus"},
                // băng hình
                {"video/mp4", ".mp4"},
                {"video/mpeg", ".mpeg"},
                {"video/webm", ".webm"},
        };
    }

    @Test(dataProvider = "mediaContentTypes", description = "Kiểm tra suy luận mở rộng cho các loại đa phương tiện")
    public void testGuessExtension_Media(String contentType, String expectedExt) {
        assertEquals(FileExtensionUtil.guessExtension(contentType), expectedExt);
    }

    @Test(description = "Kiểm tra các giá trị null hoặc không được nhận dạng Content-Type")
    public void testGuessExtension_NullAndUnknown() {
        assertNull(FileExtensionUtil.guessExtension(null), "null nên được trả lại null");
        assertNull(FileExtensionUtil.guessExtension(""), "Một chuỗi trống sẽ được trả về null");
        assertNull(FileExtensionUtil.guessExtension("unknown/type"), "Các loại không xác định nên được trả lại null");
    }

    @Test(description = "Bài kiểm tra Content-Type Với các tham số bộ ký tự")
    public void testGuessExtension_WithCharset() {
        assertEquals(FileExtensionUtil.guessExtension("application/json; charset=UTF-8"), ".json");
        assertEquals(FileExtensionUtil.guessExtension("text/html; charset=UTF-8"), ".html");
        assertEquals(FileExtensionUtil.guessExtension("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=binary"), ".xlsx");
    }

    // ==================== generateSmartFileName Bài kiểm tra ====================

    @Test(description = "Kiểm tra tạo tên tập tin thông minh")
    public void testGenerateSmartFileName() {
        String fileName = FileExtensionUtil.generateSmartFileName(".json");
        assertTrue(fileName.startsWith("response_data_"), "JSON Tên tệp phải kết thúc bằng  response_data_ Bắt đầu");
        assertTrue(fileName.endsWith(".json"), "Tên tệp phải kết thúc bằng  .json Sự kết thúc");

        fileName = FileExtensionUtil.generateSmartFileName("xlsx");
        assertTrue(fileName.startsWith("document_"), "Excel Tên tệp phải kết thúc bằng  document_ Bắt đầu");
        assertTrue(fileName.endsWith(".xlsx"), "Tên tệp phải kết thúc bằng  .xlsx Sự kết thúc");

        fileName = FileExtensionUtil.generateSmartFileName(".png");
        assertTrue(fileName.startsWith("image_"), "Tên tệp hình ảnh phải kết thúc bằng  image_ Bắt đầu");
        assertTrue(fileName.endsWith(".png"), "Tên tệp phải kết thúc bằng  .png Sự kết thúc");
    }

    @Test(description = "Kiểm tra trường hợp phần mở rộng tệp trống khi tạo tên tệp")
    public void testGenerateSmartFileName_NullExtension() {
        String fileName = FileExtensionUtil.generateSmartFileName(null);
        assertTrue(fileName.endsWith(".txt"), "null Tiện ích mở rộng phải mặc định là  .txt");

        fileName = FileExtensionUtil.generateSmartFileName("");
        assertTrue(fileName.endsWith(".txt"), "Phần mở rộng trống sẽ được mặc định là  .txt");
    }

    @Test(description = "Kiểm tra tính duy nhất của tên tệp được tạo")
    public void testGenerateSmartFileName_Uniqueness() throws InterruptedException {
        String fileName1 = FileExtensionUtil.generateSmartFileName(".json");
        Thread.sleep(1); // Đảm bảo dấu thời gian khác nhau
        String fileName2 = FileExtensionUtil.generateSmartFileName(".json");
        assertNotEquals(fileName1, fileName2, "Tên tệp được tạo hai lần phải khác nhau (chứa các dấu thời gian khác nhau)");
    }

    // ==================== extractMainType Bài kiểm tra ====================

    @Test(description = "Kiểm tra để trích xuất loại chính")
    public void testExtractMainType() {
        assertEquals(FileExtensionUtil.extractMainType("text/plain"), "text");
        assertEquals(FileExtensionUtil.extractMainType("application/json"), "application");
        assertEquals(FileExtensionUtil.extractMainType("image/png"), "image");
        assertEquals(FileExtensionUtil.extractMainType("video/mp4"), "video");
    }

    @Test(description = "Kiểm tra các trường hợp ngoại lệ khi trích xuất loại chính")
    public void testExtractMainType_EdgeCases() {
        assertNull(FileExtensionUtil.extractMainType(null), "null nên được trả lại null");
        assertNull(FileExtensionUtil.extractMainType(""), "Một chuỗi trống sẽ được trả về null");
        assertNull(FileExtensionUtil.extractMainType("invalid"), "Một chuỗi không có dấu gạch chéo sẽ được trả về null");
    }

    // ==================== isTextType Bài kiểm tra ====================

    @DataProvider(name = "textTypes")
    public Object[][] textTypes() {
        return new Object[][]{
                {"text/plain"},
                {"text/html"},
                {"text/css"},
                {"application/json"},
                {"application/xml"},
                {"application/javascript"},
                {"application/x-www-form-urlencoded"},
                {"application/x-sh"},
                {"application/x-python"},
        };
    }

    @Test(dataProvider = "textTypes", description = "Kiểm tra phán đoán loại văn bản")
    public void testIsTextType_TextTypes(String contentType) {
        assertTrue(FileExtensionUtil.isTextType(contentType),
                contentType + " Nên được công nhận là loại văn bản");
    }

    @DataProvider(name = "nonTextTypes")
    public Object[][] nonTextTypes() {
        return new Object[][]{
                {"image/png"},
                {"video/mp4"},
                {"audio/mpeg"},
                {"application/pdf"},
                {"application/zip"},
                {"application/octet-stream"},
        };
    }

    @Test(dataProvider = "nonTextTypes", description = "Kiểm tra phán đoán loại phi văn bản")
    public void testIsTextType_NonTextTypes(String contentType) {
        assertFalse(FileExtensionUtil.isTextType(contentType),
                contentType + " Không nên được công nhận là một loại văn bản");
    }

    @Test(description = "Kiểm tra các trường hợp cạnh để đánh giá loại văn bản")
    public void testIsTextType_EdgeCases() {
        assertFalse(FileExtensionUtil.isTextType(null));
        assertFalse(FileExtensionUtil.isTextType(""));
    }

    // ==================== isBinaryType Bài kiểm tra ====================

    @DataProvider(name = "binaryTypes")
    public Object[][] binaryTypes() {
        return new Object[][]{
                {"application/octet-stream"},
                {"image/png"},
                {"image/jpeg"},
                {"audio/mpeg"},
                {"video/mp4"},
                {"application/pdf"},
                {"application/zip"},
                {"application/gzip"},
                {"application/msword"},
                // Kiểm tra chính:Office Tài liệu (bao gồm xml Nhưng nó phải là nhị phân)
                {"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                {"application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
                {"application/vnd.openxmlformats-officedocument.presentationml.presentation"},
                {"application/vnd.ms-excel"},
        };
    }

    @Test(dataProvider = "binaryTypes", description = "Kiểm tra phán đoán kiểu nhị phân")
    public void testIsBinaryType_BinaryTypes(String contentType) {
        assertTrue(FileExtensionUtil.isBinaryType(contentType),
                contentType + " Nên được công nhận là loại nhị phân");
    }

    @DataProvider(name = "nonBinaryTypes")
    public Object[][] nonBinaryTypes() {
        return new Object[][]{
                {"text/plain"},
                {"text/html"},
                {"application/json"},
                {"application/xml"},
                {"application/javascript"},
        };
    }

    @Test(dataProvider = "nonBinaryTypes", description = "Kiểm tra phán đoán loại không nhị phân")
    public void testIsBinaryType_NonBinaryTypes(String contentType) {
        assertFalse(FileExtensionUtil.isBinaryType(contentType),
                contentType + " Không nên được công nhận là loại nhị phân");
    }

    @Test(description = "Kiểm tra các trường hợp cạnh để phán đoán loại nhị phân")
    public void testIsBinaryType_EdgeCases() {
        assertFalse(FileExtensionUtil.isBinaryType(null));
        assertFalse(FileExtensionUtil.isBinaryType(""));
    }

    // ==================== Kiểm tra hồi quy các vấn đề chính ====================

    @Test(description = "Kiểm tra hồi quy:xlsx Tài liệu không nên được xác định sai xml văn bản")
    public void testXlsxNotMisidentifiedAsXml() {
        String xlsxContentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        // 1. Phần mở rộng chính xác sẽ được trả lại
        assertEquals(FileExtensionUtil.guessExtension(xlsxContentType), ".xlsx",
                "xlsx nên được trả lại .xlsx Thay vào đó .xml");

        // 2. Nên được công nhận là loại nhị phân
        assertTrue(FileExtensionUtil.isBinaryType(xlsxContentType),
                "xlsx Nên được công nhận là loại nhị phân");

        // 3. Không nên được công nhận là một loại văn bản
        assertFalse(FileExtensionUtil.isTextType(xlsxContentType),
                "xlsx Không nên được công nhận là một loại văn bản");
    }

    @Test(description = "Kiểm tra hồi quy:docx Tài liệu không nên được xác định sai xml văn bản")
    public void testDocxNotMisidentifiedAsXml() {
        String docxContentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        assertEquals(FileExtensionUtil.guessExtension(docxContentType), ".docx");
        assertTrue(FileExtensionUtil.isBinaryType(docxContentType));
        assertFalse(FileExtensionUtil.isTextType(docxContentType));
    }

    @Test(description = "Kiểm tra hồi quy:pptx Tài liệu không nên được xác định sai xml văn bản")
    public void testPptxNotMisidentifiedAsXml() {
        String pptxContentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

        assertEquals(FileExtensionUtil.guessExtension(pptxContentType), ".pptx");
        assertTrue(FileExtensionUtil.isBinaryType(pptxContentType));
        assertFalse(FileExtensionUtil.isTextType(pptxContentType));
    }

    @Test(description = "Kiểm tra hồi quy: có thật XML phải được nhận dạng chính xác dưới dạng văn bản")
    public void testRealXmlIdentifiedAsText() {
        String xmlContentType = "application/xml";

        assertEquals(FileExtensionUtil.guessExtension(xmlContentType), ".xml");
        assertTrue(FileExtensionUtil.isTextType(xmlContentType));
        assertFalse(FileExtensionUtil.isBinaryType(xmlContentType));
    }

    @Test(description = "Kiểm tra hồi quy:Vendor JSON Phản hồi phải được công nhận là văn bản")
    public void testVendorJsonIdentifiedAsText() {
        String contentType = "application/vnd.spring-boot.actuator.v3+json";

        assertEquals(FileExtensionUtil.guessExtension(contentType), ".json");
        assertTrue(FileExtensionUtil.isTextType(contentType));
        assertFalse(FileExtensionUtil.isBinaryType(contentType));
    }

    @Test(description = "Kiểm tra hồi quy: xác minh tính loại trừ lẫn nhau của các phán đoán kiểu")
    public void testTypeExclusivity() {
        String[] allContentTypes = {
                "text/plain",
                "application/json",
                "application/xml",
                "image/png",
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/octet-stream"
        };

        for (String contentType : allContentTypes) {
            boolean isText = FileExtensionUtil.isTextType(contentType);
            boolean isBinary = FileExtensionUtil.isBinaryType(contentType);

            // một Content-Type Phải là văn bản, nhị phân hoặc không (unknown），Nhưng nó không nên là cả hai cùng một lúc
            assertFalse(isText && isBinary,
                    contentType + " Không nên đánh giá cả hai loại văn bản và nhị phân cùng một lúc");
        }
    }

    @Test(description = "Chỉ đánh giá theo loại phương tiện, bỏ qua các tham số")
    public void testContentTypeParametersAreIgnored() {
        String contentType = "application/octet-stream; name=\"data.json\"";

        assertFalse(FileExtensionUtil.isTextType(contentType));
        assertTrue(FileExtensionUtil.isBinaryType(contentType));
        assertNull(FileExtensionUtil.guessExtension(contentType));
        assertTrue(FileExtensionUtil.isTextType("Application/JSON; charset=UTF-8"));
        assertTrue(FileExtensionUtil.isTextType("application/xml; charset=UTF-8"));
        assertFalse(FileExtensionUtil.isBinaryType(" ; charset=UTF-8"));
    }

    @Test
    public void shouldMapOfficeVariantsOdfAndJavaArchive() {
        assertEquals(FileExtensionUtil.guessExtension("application/vnd.ms-excel.sheet.macroEnabled.12"), ".xlsm");
        assertEquals(FileExtensionUtil.guessExtension("application/vnd.ms-excel.sheet.binary.macroEnabled.12"), ".xlsb");
        assertEquals(FileExtensionUtil.guessExtension(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.template"), ".dotx");
        assertEquals(FileExtensionUtil.guessExtension("application/vnd.ms-powerpoint.slideshow.macroEnabled.12"), ".ppsm");
        assertEquals(FileExtensionUtil.guessExtension("application/vnd.oasis.opendocument.text-template"), ".ott");
        assertEquals(FileExtensionUtil.guessExtension("application/vnd.oasis.opendocument.spreadsheet"), ".ods");
        assertEquals(FileExtensionUtil.guessExtension("application/java-archive"), ".jar");
        assertEquals(FileExtensionUtil.guessExtension("application/x-java-archive"), ".jar");
        assertEquals(FileExtensionUtil.guessExtension("application/vnd.ms-excel"), ".xls");
        assertFalse(FileExtensionUtil.isTextType("application/x-java-archive"));
        assertTrue(FileExtensionUtil.isBinaryType("application/x-java-archive"));
    }
}
