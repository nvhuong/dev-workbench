# 🎯 Mô tả chi tiết các chức năng

## 🏢 Quản lý không gian làm việc

### Không gian làm việc cục bộ
- ✅ Thích hợp cho các dự án cá nhân
- ✅ Dữ liệu được lưu trữ hoàn toàn cục bộ và quyền riêng tư được bảo mật
- ✅ Không cần kết nối internet
- ✅ Kiểm soát hoàn toàn dữ liệu của bạn

### Git khu vực làm việc
- ✅ Kiểm soát phiên bản được hỗ trợ
- ✅ Hỗ trợ hợp tác nhóm
- ✅ Sao chép từ kho lưu trữ từ xa (GitHub/Gitee/GitLab）
- ✅ Khởi tạo cục bộ Git nhà kho
- ✅ Cách ly dữ liệu cấp dự án
- ✅ Chuyển đổi không gian làm việc nhanh chóng
- ✅ Nhiều phương thức xác thực:
  - Tên người dùng/mật khẩu
  - Personal Access Token
  - SSH Key

### Git hoạt động
- **Gửi(Commit）**：Lưu các thay đổi cục bộ vào kiểm soát phiên bản
- **đẩy(Push）**：Đẩy các cam kết cục bộ vào kho lưu trữ từ xa
- **kéo(Pull）**：Nhận những thay đổi mới nhất từ kho lưu trữ từ xa
- **Phát hiện xung đột**：Xử lý và giải quyết xung đột thông minh

### Quá trình hợp tác nhóm
1. **Đội trưởng**：
   - Tạo Git Không gian làm việc (sao chép từ xa hoặc khởi tạo cục bộ)
   - Cấu hình bộ sưu tập giao diện và các biến môi trường
   - Gửi và đẩy đến kho lưu trữ từ xa
2. **thành viên trong nhóm**：
   - Tạo Git Không gian làm việc (sao chép từ xa)
   - Nhận dữ liệu giao diện và cấu hình môi trường mới nhất
   - Gửi và đẩy bản cập nhật sau khi sửa đổi cục bộ
3. **Hợp tác hàng ngày**：
   - Trước khi bắt đầu làm việc: Thực hiện **Pull** Kéo những thay đổi mới nhất
   - Sau khi hoàn thành việc sửa đổi: thực thi **Commit** Cam kết thay đổi cục bộ
   - Chia sẻ thông tin cập nhật: Đã thực hiện **Push** Đẩy vào kho lưu trữ từ xa

---

## 🔌 Gỡ lỗi giao diện

### Hỗ trợ giao thức
- ✅ HTTP/1.1 và HTTP/2
- ✅ Hoàn thành REST API Phương pháp:GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS
- ✅ SSE（Server-Sent Events）
- ✅ WebSocket

### Yêu cầu định dạng cơ thể
- Form Data
- x-www-form-urlencoded
- JSON
- XML
- Binary

### Các tính năng chức năng
- ✅ Tải lên và tải xuống tệp (hỗ trợ kéo và thả)
- ✅ Cookie Quản lý tự động và chỉnh sửa thủ công
- ✅ Chỉnh sửa trực quan các tiêu đề yêu cầu và tham số truy vấn
- ✅ Hiển thị định dạng dữ liệu phản hồi (JSON、XML、HTML）
- ✅ Thời gian phản hồi, mã trạng thái, thống kê kích thước

### Tự lưu trữ Mock Server
- ✅ Menu cấp độ đầu tiên từ bên trái“Mock Server”Tạo một dịch vụ cho không gian làm việc hiện tại
- ✅ Bộ sưu tập là không cần thiết; một dịch vụ có thể được liên kết 0～Nhiều bộ sưu tập và liệt kê tất cả HTTP Yêu cầu (bao gồm cả các yêu cầu chưa được định cấu hình phản hồi)
- ✅ Bạn có thể trực tiếp thêm các tuyến độc lập và chỉnh sửa mã trạng thái cũng như tiêu đề phản hồi mà không cần gọi giao diện thực hoặc tạo bộ sưu tập trước.Body và trì hoãn
- ✅ Bộ sưu tập có thể được tạo bằng cách nhấp chuột phải vào chúng Mock Server，Nhấp chuột phải vào yêu cầu để thêm nó trực tiếp Mock phản hồi
- ✅ Phản hồi cho yêu cầu thu thập vẫn được lưu dưới dạng bộ sưu tập Example；Các tuyến đường độc lập được lưu trực tiếp vào `mock_servers.json`
- ✅ cuộc thi đấu method/path/query，Đặc điểm kỹ thuật phù hợp tùy chọn header và JSON/văn bản body
- ✅ Hỗ trợ `x-mock-response-id`、`x-mock-response-name`、`x-mock-response-code` Chọn Example
- ✅ Hỗ trợ `x-mock-match-request-body`、`x-mock-match-request-headers` Cho phép kết hợp chính xác trên cơ sở mỗi yêu cầu
- ✅ Hỗ trợ cấp độ định tuyến **Code Mock** và các tập lệnh toàn cầu tùy chọn, thông qua  `pm.request`、`pm.response`、`pm.state` Phản ứng năng động
- ✅ Code Mock Hỗ trợ biên tập `pm.*` Tự động hoàn thành, trạng thái đã bật,API Kiểm tra nhanh là tốt 8 Tuyển tập các ví dụ thường được sử dụng có thể được xem trước, chèn hoặc thay thế
- ✅ Hỗ trợ CORS、đã sửa/Độ trễ định tuyến,`x-mock-response-delay` Tạm thời bị trì hoãn, bị giới hạn/Tắt tính năng ghi nhật ký cuộc gọi và `x-mock-session-id` trạng thái phiên
- ✅ Không sử dụng thêm phụ thuộc phía máy chủ JDK `HttpServer`，Nhấn CPU Mở rộng chuỗi công nhân và nhấn HTTP Method Định tuyến được lập chỉ mục trước, thích hợp cho việc gỡ lỗi chung đồng thời và kiểm tra mức độ căng thẳng nhẹ
- ✅ Theo mặc định, tất cả các card mạng đều được giám sát và giao diện hiển thị quyền truy cập cục bộ URL `127.0.0.1`；Các thiết bị LAN có thể được truy cập cục bộ IP và có thể được truy cập thông qua cùng một cổng `x-api-key` Đặt khóa truy cập được chia sẻ
- ✅ `mock_servers.json` Và `collections.json` Lưu cùng nhau trong không gian làm việc hiện tại.Git Không gian làm việc sẽ được phiên bản cùng nhau
- ✅ Hỗ trợ `mock run` Chạy không có giao diện, không gian làm việc có thể được sao chép vào máy chủ hoặc CI Tự lưu trữ
- ✅ Trang quản lý có thể sao chép trực tiếp các lệnh khởi động tự lưu trữ cho các không gian làm việc và dịch vụ tương ứng
- ✅ Không bao gồm EasyPostman Lưu trữ đám mây, quyền nhóm, bên ngoài npm gói hoặc AI phát ra

Ví dụ khởi động máy chủ: .

```bash
EASY_POSTMAN_MOCK_API_KEY=change-me \
java -jar easy-postman.jar mock run /path/to/workspace \
  --server "Payments Mock" --host 0.0.0.0 --port 3001
```

Nên sử dụng mạng công cộng Nginx、Caddy Hoặc việc cân bằng tải trên đám mây sẽ bị chấm dứt sau đó HTTPS，Và hạn chế các cổng vào của tường lửa.

Ví dụ về tập lệnh: .

```javascript
const input = JSON.parse(pm.request.body || "{}");
if (Number(input.amount) === 0.5) {
  pm.response.setStatusCode(402);
  pm.response.setBody(JSON.stringify({ status: "deny" }));
} else if (Number(input.amount) === 1.1) {
  pm.response.setStatusCode(200);
  pm.response.setBody(JSON.stringify({ status: "partial_approval" }));
}
```

`pm.request` cung cấp `method`、`path`、`body`、`header(name)`、`query(name)` và `pathVariable(name)`；`pm.response` Mã trạng thái có thể được đặt.header、body và `delayMs`。Trạng thái chỉ tồn tại trong bộ nhớ và không được giữ lại khi xóa trạng thái, làm mới thời gian chạy dịch vụ hoặc thoát khỏi ứng dụng.

---

## 🤖 MCP Và AI khách hàng

- ✅ Tiêu chuẩn tích hợp MCP stdio Server，Có thể kết nối Codex、Claude Desktop、Claude Code、Cursor Đợi khách hàng địa phương
- ✅ Gói cài đặt gốc và Windows Phiên bản di động tích hợp `EasyPostmanMCP` Và JRE，Không cần cài đặt thêm Java Hoặc tải xuống riêng biệt MCP
- ✅ Liệt kê các không gian làm việc, bộ sưu tập, môi trường và yêu cầu đã lưu được ủy quyền; danh sách môi trường không trả về giá trị biến
- ✅ Nhấn mỗi lần Tool Lựa chọn cuộc gọi `workspaceId` và `environmentId`，Không sửa đổi lựa chọn hiện tại trên màn hình nền
- ✅ Thực hiện một lưu duy nhất HTTP Yêu cầu hoặc toàn bộ bộ sưu tập cấp cao nhất, sử dụng lại tính kế thừa biến, tập lệnh trước và sau cũng như xác nhận.Cookie、TLS và HTTP chuỗi thực thi
- ✅ Hỗ trợ sử dụng một lần `environmentOverrides`，Nó chỉ có hiệu lực trong bộ nhớ và không được ghi lại vào vùng làm việc
- ✅ MCP Các quy trình và ứng dụng máy tính để bàn có thể chạy đồng thời; quy trình tương tự được sử dụng lại trong một kết nối chứ không phải cho mỗi kết nối API Bắt đầu lặp lại cuộc gọi
- ✅ Có giới hạn trên đối với nội dung và thông tin chi tiết của phản hồi, đồng thời, dữ liệu xác thực chung và đường dẫn lỗi sẽ bị loại bỏ.stdout Chỉ đầu ra MCP JSON-RPC
- 🔒 MCP Một yêu cầu thực sự bên ngoài có thể được thực hiện và phản hồi được trả về cho khách hàng. Chỉ những khách hàng đáng tin cậy và không gian làm việc cần thiết mới được ủy quyền

Xem cài đặt với đường viền hoàn chỉnh [MCP Server Tài liệu](MCP_SERVER_DESIGN_zh.md)。

---

## 🌍 Quản lý môi trường

- ✅ Chuyển đổi nhanh chóng giữa nhiều môi trường ( Development/Bài kiểm tra/Sản xuất)
- ✅ Hỗ trợ biến toàn cục và biến môi trường
- ✅ Tham chiếu lồng nhau biến:`{{baseUrl}}/api/{{version}}`
- ✅ Biến động:
  - `{{$timestamp}}` - Dấu thời gian hiện tại
  - `{{$randomInt}}` - Số nguyên ngẫu nhiên
  - `{{$uuid}}` - UUID máy phát điện
- ✅ Nhập và xuất các biến môi trường

---

## 📝 Hỗ trợ viết kịch bản

### Pre-request Script（tập lệnh yêu cầu trước)
- Thực hiện trước khi gửi yêu cầu JavaScript
- Đặt biến động
- Chuẩn bị dữ liệu thử nghiệm
- Sửa đổi các tham số yêu cầu

### Tests Script（kịch bản thử nghiệm)
- Thực hiện sau khi nhận được phản hồi JavaScript
- Phân tích dữ liệu phản hồi
- Đặt các biến từ phản hồi
- Thực hiện liên kết yêu cầu
- Hỗ trợ khẳng định

### Chức năng tích hợp
- ✅ Thư viện đoạn mã
- ✅ JavaScript Môi trường thời gian chạy
- ✅ Kiểm tra khẳng định
- ✅ Yêu cầu liên kết

---

## ⚡ Kiểm tra hiệu suất

### Chế độ nhóm chủ đề
1. **Số lượng chủ đề cố định**：Kiểm tra tải ổn định
   - Số lượng chủ đề không đổi
   - Thích hợp để kiểm tra hiệu suất điểm chuẩn

2. **Tăng dần**：Tăng dần tải
   - Dần dần thêm chủ đề
   - Kiểm tra hành vi của hệ thống khi tải tăng dần

3. **Cầu thang**：Kiểm tra tải theo giai đoạn
   - Tải được tăng lên theo từng giai đoạn
   - Identify different levels of performance bottlenecks

4. **Tăng vọt**：Kiểm tra giao thông bùng nổ
   - Lưu lượng truy cập tăng đột ngột
   - Kiểm tra khả năng phục hồi của hệ thống

### Giám sát và báo cáo
- ✅ Giám sát hiệu suất thời gian thực
- ✅ Detailed test report:
  - Phân phối thời gian đáp ứng
  - TPS（giao dịch mỗi giây)
  - Phân tích tỷ lệ lỗi
  - thành công/Thống kê thất bại
- ✅ Phân tích cây kết quả
- ✅ Biểu đồ xu hướng hiệu suất
- ✅ Xuất kết quả kiểm tra

---

## 📊 Phân tích dữ liệu

### Yêu cầu lịch sử
- ✅ Chế độ xem dòng thời gian của tất cả các yêu cầu
- ✅ Nhanh chóng phát lại các yêu cầu trước đó
- ✅ Bằng mã trạng thái, phương thức,URL lọc
- ✅ Tự động lưu và lưu trữ liên tục

### Nhật ký sự kiện mạng
- ✅ Detailed network event monitoring
- ✅ yêu cầu/tiêu đề phản hồi
- ✅ Sự cố thời gian
- ✅ Chẩn đoán lỗi

### Thống kê phản hồi
- ✅ Phân tích thời gian đáp ứng
- ✅ Theo dõi kích thước dữ liệu
- ✅ Tự động phân loại các yêu cầu xấu
- ✅ Thống kê xuất khẩu

---

## 🔄 Quản lý dữ liệu

### Hỗ trợ nhập khẩu
- ✅ **Postman Collection v2.1**：Hỗ trợ xuất nhập khẩu
- ✅ **cURL Đặt hàng**：Chuyển đổi thành yêu cầu
- 🚧 **HAR tập tin**：Đang được phát triển
- 🚧 **OpenAPI/Swagger**：Đang được phát triển

### Hỗ trợ xuất khẩu
- ✅ Postman Collection định dạng
- ✅ cURL Đặt hàng
- ✅ các biến môi trường
- ✅ Kết quả kiểm tra

---

## 🎨 Giao diện người dùng

### chủ đề
- ✅ Chế độ màu sắc tươi sáng
- ✅ Chế độ tối
- ✅ Tự động chuyển đổi dựa trên tùy chọn hệ thống

### Chức năng soạn thảo
- ✅ Syntax Highlighting:
  - JSON
  - XML
  - JavaScript
  - HTML
- ✅ Định dạng tự động
- ✅ Gấp mã
- ✅ Tìm kiếm và thay thế

### quốc tế hóa
- ✅ Tiếng Trung giản thể
- ✅ English
- 🚧 Sắp có thêm nhiều ngôn ngữ

---

## ☕ Công cụ bổ sung

### Java Trình giải mã
- ✅ Trình dịch ngược tích hợp để phân tích Java lớp học
- ✅ Từ JAR Mã nguồn xem tập tin
- ✅ Thích hợp cho việc gỡ lỗi và kỹ thuật đảo ngược

### Giấy chứng nhận của khách hàng
- ✅ Hỗ trợ mTLS（Cả hai hướng TLS）
- ✅ Nhập chứng chỉ ứng dụng khách
- ✅ An toàn API Bài kiểm tra

---

## 🔒 Quyền riêng tư và bảo mật

- ✅ **Bộ nhớ cục bộ mặc định**：Chưa được kích hoạt Git đồng bộ hóa hoặc MCP , dữ liệu không gian làm việc sẽ không được tự động tải lên
- 🔒 **Các kết nối được kiểm soát bởi người dùng**：Git Từ xa, đo lường API và MCP Khách hàng có thể nhận được dữ liệu được gửi rõ ràng bởi người dùng
- ✅ **Không có đo từ xa**：Không theo dõi, không phân tích
- ✅ **Ưu tiên ngoại tuyến**：Hoạt động hoàn toàn ngoại tuyến
- ✅ **Nguồn mở**：Mã là minh bạch và có thể xem xét
- ✅ **Git Mã hóa**：Mã hóa được hỗ trợ Git nhà kho
