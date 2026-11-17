# Báo cáo performance và nâng cấp Java 21

## Kết quả kiểm tra chức năng cũ

- Tab Report tổng hợp chỉ số HTTP, WebSocket và SSE. Nút Copy Data sao chép bảng Markdown.
- Export trên thanh công cụ performance xuất `plan.json` để chạy lại kế hoạch; không xuất kết quả kiểm thử.
- CLI ghi kết quả có cấu trúc dạng JSON thông qua `PerformanceCommandReportOutput`.
- Chưa có luồng xuất PDF tổng hợp từ tab Report.

## Chức năng PDF bổ sung

Chạy bài kiểm thử, mở tab Report và chọn **Export PDF**. Chọn đường dẫn lưu; ứng dụng xác nhận nếu file đã tồn tại. Có thể xuất khi đang chạy: dữ liệu là bản chụp tại lúc nhấn nút. Nên đợi bài chạy kết thúc để có kết quả cuối cùng.

Mỗi giao thức có trang tổng quan riêng, gồm thẻ tổng số mẫu, số thành công, số thất bại và tỷ lệ thành công. HTTP có thêm QPS, thời gian trung bình, P95 và P99. WebSocket/SSE hiển thị các chỉ số phù hợp với giao thức.

Biểu đồ thanh thể hiện tỷ lệ thành công/thất bại của 10 endpoint đầu tiên theo thứ tự bảng. Tên dài được rút gọn trong biểu đồ; bảng chi tiết giữ đầy đủ tên và dữ liệu của mọi endpoint. Bảng chia thành các nhóm tối đa 5 chỉ số, lặp lại cột endpoint và tiêu đề khi sang trang. Tài liệu dùng khổ A4, màu nền sáng, khoảng trắng, số trang và thời điểm xuất có múi giờ. Chữ trong PDF có thể tìm kiếm và sao chép. Font nhúng hỗ trợ tiếng Việt và tiếng Trung; ký tự ngoài bộ font được thay bằng dấu `?`.

Xuất PDF chạy nền, không truy cập Swing từ luồng xuất. File được tạo tạm trước khi thay thế file đích để tránh mất báo cáo cũ khi tạo PDF thất bại.

## Phạm vi dữ liệu

- Báo cáo dùng cùng số liệu đã tổng hợp trong tab Report; không tính lại percentile từ giá trị trung bình.
- Thời điểm trên trang là thời điểm xuất, không phải thời điểm bắt đầu bài chạy.
- Phiên bản này chưa đưa cấu hình tải, môi trường chạy, chuỗi thời gian hoặc danh sách lỗi chi tiết vào PDF.
- Chưa có ngưỡng SLA nên không tự đưa ra kết luận đạt/không đạt.
- File mẫu từ test dùng dữ liệu giả lập để kiểm tra bố cục, không phải kết quả benchmark của ứng dụng.

## Java 21

Compiler của tất cả module và plugin kế thừa `maven.compiler.release=21`. CI, script đóng gói và tài liệu yêu cầu JDK 21+. JAR mới yêu cầu Java 21 trở lên; installer vẫn dùng runtime được jlink/jpackage đóng gói.

Build toàn bộ:

```sh
mvn -B -DskipTests clean package
```

Kiểm tra báo cáo ở chế độ headless:

```sh
mvn -pl easy-postman-app -am -Dtest=PerformanceReportPdfExporterTest,PerformanceReportMarkdownBuilderTest,PerformanceProtocolReportDataTest,PerformanceReportRowMapperTest,PerformanceReportTableSchemaTest -Dsurefire.failIfNoSpecifiedTests=false -Djava.awt.headless=true test
```

Test PDF tạo `easy-postman-app/target/performance-report-demo.pdf` và ảnh trang đầu `performance-report-preview.png` để kiểm tra trực quan.

PDF được tạo bằng [Apache PDFBox](https://pdfbox.apache.org/3.0/getting-started.html); font và giấy phép đi kèm trong thư mục resources/fonts.
