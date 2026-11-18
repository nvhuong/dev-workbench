package com.nvhuong.devworkbench.http.runtime.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Xây dựng một nhận dạng nhỏ nhất Office Open XML Tệp, được sử dụng để đáp ứng các bài kiểm tra liên quan đến nhận dạng tệp.
 */
public final class ResponseFileFixtures {

    private ResponseFileFixtures() {
    }

    public static byte[] xlsx() throws IOException {
        return ooxml("xl/workbook.xml");
    }

    public static byte[] ooxml(String mainPartName) throws IOException {
        return ooxml(mainPartName, null);
    }

    /**
     * @param mainContentType Loại nội dung của tiện ích chính (chẳng hạn như macro/Biến thể mẫu), không được khai báo khi trống
     */
    public static byte[] ooxml(String mainPartName, String mainContentType) throws IOException {
        String types = mainContentType == null ? "<Types/>"
                : "<Types><Override PartName=\"/" + mainPartName + "\" ContentType=\"" + mainContentType + "\"/></Types>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(types.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(mainPartName));
            // Chứa không thể nhấn được UTF-8 Các byte được giải mã, mô phỏng nội dung nhị phân thực
            zip.write(new byte[]{(byte) 0xC3, (byte) 0x28, 0x00, (byte) 0xFF});
            zip.closeEntry();
        }
        return out.toByteArray();
    }
}
