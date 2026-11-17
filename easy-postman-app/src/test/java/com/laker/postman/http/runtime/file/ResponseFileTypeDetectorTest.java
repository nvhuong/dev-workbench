package com.laker.postman.http.runtime.file;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ResponseFileTypeDetectorTest {

    @DataProvider
    public Object[][] knownFiles() throws IOException {
        return new Object[][]{
                {ResponseFileFixtures.xlsx(), ".xlsx"},
                {ResponseFileFixtures.ooxml("xl/workbook.bin"), ".xlsb"},
                {ResponseFileFixtures.ooxml("word/document.xml"), ".docx"},
                {ResponseFileFixtures.ooxml("ppt/presentation.xml"), ".pptx"},
                {zip("data.csv", "a,b"), ".zip"},
                {"%PDF-1.7\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1), ".pdf"},
                {ole2("Workbook"), ".xls"},
                {ole2("WordDocument"), ".doc"},
                {ole2("PowerPoint Document"), ".ppt"},
                {ole2("Book"), ".xls"},
                {ole2("Unknown"), ".bin"},
                {ResponseFileFixtures.ooxml("xl/workbook.xml",
                        "application/vnd.ms-excel.sheet.macroEnabled.main+xml"), ".xlsm"},
                {ResponseFileFixtures.ooxml("xl/workbook.xml",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.template.main+xml"), ".xltx"},
                {ResponseFileFixtures.ooxml("word/document.xml",
                        "application/vnd.ms-word.document.macroEnabled.main+xml"), ".docm"},
                {ResponseFileFixtures.ooxml("ppt/presentation.xml",
                        "application/vnd.openxmlformats-officedocument.presentationml.slideshow.main+xml"), ".ppsx"},
                {odf("application/vnd.oasis.opendocument.text"), ".odt"},
                {odf("application/vnd.oasis.opendocument.spreadsheet-template"), ".ots"},
                {odf("application/vnd.oasis.opendocument.text-master"), ".odm"},
                {odf("application/epub+zip"), ".epub"},
                {odf("application/vnd.oasis.opendocument.graphics-template"), ".otg"},
                {odf("application/vnd.oasis.opendocument.chart"), ".odc"},
                {odf("application/vnd.oasis.opendocument.formula"), ".odf"},
                {odf("text/plain"), ".zip"},
                {odf("application/vnd.oasis.opendocument.unknown"), ".zip"},
                {zip("OFD.xml", "<ofd:OFD/>"), ".ofd"},
                {gzip("hello"), ".gz"},
                {bytes(0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C, 0x00, 0x04), ".7z"},
                {bytes(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00), ".rar"},
                {bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00), ".png"},
                {bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10), ".jpg"},
                {"GIF89a....".getBytes(StandardCharsets.US_ASCII), ".gif"},
                {"RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII), ".webp"},
        };
    }

    @Test(dataProvider = "knownFiles")
    public void shouldDetectFileTypeFromSignature(byte[] content, String expectedExtension) {
        assertEquals(ResponseFileTypeDetector.detectExtension(content), expectedExtension);
    }

    @Test
    public void shouldNotDetectPlainTextOrIncompleteSignatures() {
        assertNull(ResponseFileTypeDetector.detectExtension((byte[]) null));
        assertNull(ResponseFileTypeDetector.detectExtension((java.nio.file.Path) null));
        assertNull(ResponseFileTypeDetector.detectExtension(new byte[0]));
        assertNull(ResponseFileTypeDetector.detectExtension("{\"ok\":true}".getBytes(StandardCharsets.UTF_8)));
        assertNull(ResponseFileTypeDetector.detectExtension("<html>PK</html>".getBytes(StandardCharsets.UTF_8)));
        assertNull(ResponseFileTypeDetector.detectExtension("%PD".getBytes(StandardCharsets.US_ASCII)));
        assertNull(ResponseFileTypeDetector.detectExtension("RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void shouldFallBackToKeywordsWhenZipDirectoryIsUnreadable() throws IOException {
        // Các phiên bản cũ hơn nhấn nhị phân UTF-8 Sau khi giải mã và lưu, phần bù không hợp lệ và chỉ có thể dựa vào ASCII Từ khóa
        byte[] legacyText = new String(ResponseFileFixtures.xlsx(), StandardCharsets.UTF_8)
                .getBytes(StandardCharsets.UTF_8);

        assertEquals(ResponseFileTypeDetector.detectExtension(legacyText), ".xlsx");
    }

    @Test
    public void shouldDetectBinaryControlBytesLikeWhatwgSniffing() {
        assertTrue(ResponseFileTypeDetector.looksBinary(bytes(0x08, 0x96, 0x01, 0x12, 0x03)));
        assertFalse(ResponseFileTypeDetector.looksBinary("{\"a\":1}\r\n\t".getBytes(StandardCharsets.UTF_8)));
        assertFalse(ResponseFileTypeDetector.looksBinary("Tiếng Trung Quốc GBK".getBytes(java.nio.charset.Charset.forName("GBK"))));
        assertFalse(ResponseFileTypeDetector.looksBinary("ab".getBytes(StandardCharsets.UTF_16)), "mang đến BOM của UTF-16 là văn bản");
        assertFalse(ResponseFileTypeDetector.looksBinary(new byte[0]));
        assertFalse(ResponseFileTypeDetector.looksBinary(null));
    }

    @Test
    public void shouldDetectFileOnDisk() throws IOException {
        java.nio.file.Path file = java.nio.file.Files.createTempFile("detector", ".tmp");
        try {
            java.nio.file.Files.write(file, ResponseFileFixtures.xlsx());
            assertEquals(ResponseFileTypeDetector.detectExtension(file), ".xlsx");
            assertNull(ResponseFileTypeDetector.detectExtension(file.resolveSibling("missing-" + file.getFileName())));
        } finally {
            java.nio.file.Files.deleteIfExists(file);
        }
    }

    @Test
    public void shouldOnlyLookAtHeadAndFallBackToZipBeyondIt() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.setLevel(0);
            zip.putNextEntry(new ZipEntry("padding.bin"));
            zip.write(new byte[ResponseFileTypeDetector.HEAD_BYTES]);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
            zip.closeEntry();
        }

        assertEquals(ResponseFileTypeDetector.detectExtension(out.toByteArray()), ".zip");
    }

    @Test
    public void shouldDetectOoxmlVariantWhenContentTypesIsNotFirstEntry() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write("<Relationships/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(("<Types><Override PartName=\"/xl/workbook.xml\" "
                    + "ContentType=\"application/vnd.ms-excel.sheet.macroEnabled.main+xml\"/></Types>")
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
            zip.write("<workbook/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        assertEquals(ResponseFileTypeDetector.detectExtension(out.toByteArray()), ".xlsm");
    }

    @Test
    public void shouldTrustConfirmedStandardOoxmlOverClaimedVariant() throws IOException {
        ResponseFileTypeDetector.Detection confirmed = ResponseFileTypeDetector.detect(ResponseFileFixtures.ooxml(
                "xl/workbook.xml", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"));
        ResponseFileTypeDetector.Detection guessed = ResponseFileTypeDetector.detect(ResponseFileFixtures.xlsx());

        assertEquals(confirmed, new ResponseFileTypeDetector.Detection(".xlsx", true));
        assertEquals(guessed, new ResponseFileTypeDetector.Detection(".xlsx", false), "Các loại phụ không được xác nhận khi chỉ xác định theo từ khóa");
        assertEquals(ResponseFileTypeDetector.reconcile(".xlsm", confirmed), ".xlsx", "Được xác nhận là tiêu chuẩn xlsx，Sửa lỗi .xlsm");
        assertEquals(ResponseFileTypeDetector.reconcile(".xlsm", guessed), ".xlsm");
        assertEquals(ResponseFileTypeDetector.reconcile(".jpeg",
                new ResponseFileTypeDetector.Detection(".jpg", true)), ".jpeg", "Văn bản có cùng định dạng luôn được giữ lại");
        assertEquals(ResponseFileTypeDetector.reconcile(".xlt", ResponseFileTypeDetector.detect(ole2("Workbook"))), ".xlt",
                "OLE2 Không thể phân biệt giữa các mẫu");
        assertNull(ResponseFileTypeDetector.detect("plain".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void shouldReconcileClaimedExtensionWithSignature() {
        assertEquals(ResponseFileTypeDetector.reconcile(".png", ".pdf"), ".pdf", "Khi có xung đột trong tiêu đề tệp duy nhất, nội dung sẽ chiếm ưu thế");
        assertEquals(ResponseFileTypeDetector.reconcile(".xls", ".xlsx"), ".xlsx");
        assertEquals(ResponseFileTypeDetector.reconcile(".png", ".zip"), ".zip");
        assertEquals(ResponseFileTypeDetector.reconcile(".yaml", ".zip"), ".zip", "Chỉ giữ các định dạng vùng chứa trong danh sách được phép");
        assertEquals(ResponseFileTypeDetector.reconcile(".exe", ".zip"), ".zip");
        assertEquals(ResponseFileTypeDetector.reconcile(".mp4", ".bin"), ".bin");
        assertEquals(ResponseFileTypeDetector.reconcile(".odt", ".zip"), ".odt");
        assertEquals(ResponseFileTypeDetector.reconcile(".xlsm", ".xlsx"), ".xlsm", "Retain more specific extensions when recognition results are broad");
        assertEquals(ResponseFileTypeDetector.reconcile(".xlsx", ".xlsm"), ".xlsm", "When a clear variant is identified, the identification result shall prevail");
        assertEquals(ResponseFileTypeDetector.reconcile(".docx", ".dotm"), ".dotm");
        assertEquals(ResponseFileTypeDetector.reconcile(".JPEG", ".jpg"), ".JPEG");
        assertEquals(ResponseFileTypeDetector.reconcile(".jar", ".zip"), ".jar", "Vùng chứa phổ quát không phủ nhận các định dạng dựa trên vùng chứa");
        assertEquals(ResponseFileTypeDetector.reconcile(".xlsx", ".bin"), ".xlsx", "Mã hóa Office Vâng OLE2 thùng chứa");
        assertEquals(ResponseFileTypeDetector.reconcile(".msg", ".bin"), ".msg");
        assertEquals(ResponseFileTypeDetector.reconcile(null, ".pdf"), ".pdf");
        assertEquals(ResponseFileTypeDetector.reconcile(".pdf", (String) null), ".pdf");
    }

    /** ODF/EPUB：Mục đầu tiên không được nén mimetype。 */
    private static byte[] odf(String mimeType) throws IOException {
        byte[] content = mimeType.getBytes(StandardCharsets.US_ASCII);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(content);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            ZipEntry entry = new ZipEntry("mimetype");
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(content.length);
            entry.setCrc(crc.getValue());
            zip.putNextEntry(entry);
            zip.write(content);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("content.xml"));
            zip.write("<office:document/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static byte[] zip(String name, String content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static byte[] ole2(String streamName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(bytes(0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1));
        out.writeBytes(new byte[504]);
        out.writeBytes(streamName.getBytes(StandardCharsets.UTF_16LE));
        return out.toByteArray();
    }

    private static byte[] gzip(String content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }

}
