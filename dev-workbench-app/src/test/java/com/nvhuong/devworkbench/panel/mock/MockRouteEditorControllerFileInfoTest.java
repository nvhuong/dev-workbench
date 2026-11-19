package com.nvhuong.devworkbench.panel.mock;

import com.nvhuong.devworkbench.request.model.HttpHeader;
import com.nvhuong.devworkbench.request.model.SavedResponse;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class MockRouteEditorControllerFileInfoTest {

    private static final List<HttpHeader> ATTACHMENT = List.of(
            new HttpHeader(true, "Content-Disposition", "attachment; filename=orders.csv"));

    @Test
    public void shouldKeepFileInfoWhenBodyIsUnchanged() {
        SavedResponse example = fileExample(false, "");

        assertEquals(MockRouteEditorController.fileInfoAfterEdit(example, "", List.of()).getFileName(), "orders.xlsx",
                "Chỉ thay đổi độ trễ/Kịch bản vẫn cần được trả lại 501，Thay vào đó 0 tập tin byte");
        assertEquals(MockRouteEditorController.fileInfoAfterEdit(example, null, null).getFileName(), "orders.xlsx");
    }

    @Test
    public void shouldDropOmittedFileInfoWhenUserWritesBody() {
        SavedResponse example = fileExample(false, "");

        assertNull(MockRouteEditorController.fileInfoAfterEdit(example, "{\"ok\":true}", List.of()));
    }

    @Test
    public void shouldKeepTextAttachmentFileInfoOnlyWhileAttachmentHeaderRemains() {
        SavedResponse example = fileExample(true, "a,b");

        assertEquals(MockRouteEditorController.fileInfoAfterEdit(example, "a,b,c", ATTACHMENT).getFileName(), "orders.csv");
        assertNull(MockRouteEditorController.fileInfoAfterEdit(example, "a,b", List.of()),
                "xóa bỏ attachment Sau đó tiến hành như một ví dụ văn bản bình thường");
        assertNull(MockRouteEditorController.fileInfoAfterEdit(example, "a,b", List.of(
                new HttpHeader(false, "Content-Disposition", "attachment"))), "Tiêu đề phản hồi bị tắt không có hiệu lực");
    }

    @Test
    public void shouldDropOmittedFileInfoWhenHeadersNoLongerDescribeAFile() {
        SavedResponse json = fileExample(false, "");
        json.setHeaders(List.of(new HttpHeader(true, "Content-Type", "application/json")));
        json.setBodySize(4096);
        List<HttpHeader> plainText = List.of(new HttpHeader(true, "Content-Type", "text/plain"));

        assertNull(MockRouteEditorController.fileInfoAfterEdit(json, "", plainText), "Nó sẽ không trở lại sau khi thay đổi thành văn bản bình thường 501");
        assertEquals(MockRouteEditorController.bodySizeAfterEdit(json, "", plainText, null), 0L);
        assertEquals(MockRouteEditorController.fileInfoAfterEdit(json, "", json.getHeaders()).getFileName(),
                "orders.xlsx", "Tiêu đề phản hồi được giữ lại khi nó không thay đổi (được khai báo bởi máy chủ JSON、lợi nhuận thực tế xlsx）");
        assertEquals(MockRouteEditorController.fileInfoAfterEdit(json, "", List.of(
                new HttpHeader(true, "Content-Type", "application/octet-stream"))).getFileName(), "orders.xlsx");
    }

    @Test
    public void shouldUpdateFileNameFromEditedHeaders() {
        SavedResponse csv = fileExample(true, "a,b");
        csv.getFile().setFileName("orders.csv");

        assertEquals(MockRouteEditorController.fileInfoAfterEdit(csv, "a,b", List.of(
                new HttpHeader(true, "Content-Disposition", "attachment; filename=customers.csv"))).getFileName(),
                "customers.csv");
        assertEquals(MockRouteEditorController.fileInfoAfterEdit(csv, "a,b", List.of(
                new HttpHeader(true, "Content-Disposition", "attachment"),
                new HttpHeader(true, "Content-Type", "text/plain"))).getFileName(),
                "orders.txt", "Nhấn khi không có tên tập tin nào được khai báo Content-Type Cập nhật phần mở rộng");

        SavedResponse omitted = fileExample(false, "");
        assertEquals(MockRouteEditorController.fileInfoAfterEdit(omitted, "", List.of(
                new HttpHeader(true, "Content-Disposition", "attachment; filename=\"q3.xlsx\""))).getFileName(), "q3.xlsx");
    }

    @Test
    public void shouldUpdateBodySizeWhenBodyOrCharsetChanges() {
        SavedResponse omitted = fileExample(false, "");
        omitted.setBodySize(4096);
        assertEquals(MockRouteEditorController.bodySizeAfterEdit(omitted, "", List.of(
                new HttpHeader(true, "Content-Type", "application/pdf")), omitted.getFile()), 4096L, "Các tệp có nội dung chưa được lưu vẫn giữ nguyên kích thước ban đầu");
        assertEquals(MockRouteEditorController.bodySizeAfterEdit(omitted, "名称", List.of(), null), 6L);

        SavedResponse csv = fileExample(true, "名称");
        csv.setHeaders(List.of(new HttpHeader(true, "Content-Type", "text/csv; charset=UTF-8")));
        csv.setBodySize(6);
        assertEquals(MockRouteEditorController.bodySizeAfterEdit(csv, "名称", csv.getHeaders(), csv.getFile()), 6L);
        assertEquals(MockRouteEditorController.bodySizeAfterEdit(csv, "名称", List.of(
                new HttpHeader(true, "Content-Type", "text/csv; charset=GBK")), csv.getFile()), 4L, "Thay đổi charset Sau đó tính toán theo bộ ký tự mới");
    }

    private static SavedResponse fileExample(boolean contentInBody, String body) {
        SavedResponse.FileInfo file = new SavedResponse.FileInfo();
        file.setFileName("orders.xlsx");
        file.setContentInBody(contentInBody);
        SavedResponse example = new SavedResponse();
        example.setBody(body);
        example.setFile(file);
        return example;
    }
}
