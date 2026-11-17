# 🏗️ Kiến trúc hệ thống

## Tổng quan về kiến ​​trúc

```
EasyPostman
├── 🎨 Lớp giao diện người dùng (UI Layer)
│   ├── Workspace Quản lý không gian làm việc
│   ├── Collections Quản lý bộ sưu tập giao diện
│   ├── Environments Cấu hình biến môi trường
│   ├── History Yêu cầu lịch sử
│   ├── Performance Mô-đun kiểm tra hiệu suất
│   └── NetworkLog Giám sát yêu cầu mạng
├── 🔧 Lớp logic nghiệp vụ (Business Layer)
│   ├── HTTP Yêu cầu công cụ xử lý
│   ├── Công cụ chuyển mạch và cách ly không gian làm việc
│   ├── Git Công cụ kiểm soát phiên bản
│   ├── Trình phân tích cú pháp biến môi trường
│   ├── Công cụ thực thi tập lệnh
│   ├── Mô-đun nhập và xuất dữ liệu
│   └── Người thực hiện kiểm tra hiệu suất
├── 💾 Lớp truy cập dữ liệu (Data Layer)
│   ├── Quản lý lưu trữ không gian làm việc
│   ├── Lưu trữ tập tin cục bộ
│   ├── Git Quản lý kho hàng
│   ├── Quản lý cấu hình
│   └── Quản lý lịch sử
└── 🌐 Lớp truyền thông mạng (Network Layer)
    ├── HTTP/HTTPS khách hàng
    ├── WebSocket khách hàng
    ├── SSE khách hàng
    └── Git Giao tiếp kho từ xa
```

---

## 🛠️ Hướng dẫn lựa chọn kỹ thuật

### Ngăn xếp công nghệ cốt lõi

- **Java 21**: sử dụng Java 21 LTS，Tận hưởng sự hiện đại Java Đặc điểm
  - Records、Sealed Classes、Pattern Matching
  - Tăng cường NullPointerException tin nhắn
  - Text Blocks Xử lý chuỗi tốt hơn

- **JavaSwing**: Máy tính để bàn gốc GUI khung
  - Khả năng tương thích đa nền tảng (Windows、macOS、Linux）
  - Cái nhìn và cảm nhận bản địa
  - Không có chi phí trình duyệt

- **jlink & jpackage**: Công cụ đóng gói chính thức
  - Tạo một hình ảnh thời gian chạy tùy chỉnh
  - Tạo gói cài đặt gốc (DMG、EXE、DEB）
  - Giảm kích thước phân phối

### UI Thư viện thành phần

- **[FlatLaf](https://github.com/JFormDesigner/FlatLaf)**: hiện đại hóa UI chủ đề
  - Hỗ trợ chế độ tối và sáng
  - Hỗ trợ HiDPI/Retina trình diễn
  - bản địa macOS phong cách
  - Phối màu tùy chỉnh

- **[RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea)**: Syntax highlighting text editor
  - Hỗ trợ JSON、XML、JavaScript、HTML Đợi đã
  - Gấp mã
  - Hoàn thành tự động
  - Tìm kiếm và thay thế

- **SwingX**: Sự mở rộng Swing Thư viện thành phần
  - Bảng và cây nâng cao
  - Bộ chọn ngày
  - Bảng tìm kiếm

- **MigLayout**: Trình quản lý bố cục mạnh mẽ
  - Linh hoạt và trực quan
  - Hỗ trợ thiết kế đáp ứng
  - Tính nhất quán đa nền tảng

### Mạng và Truyền thông

- **[OkHttp](https://github.com/square/okhttp)**: Hiệu suất cao HTTP khách hàng
  - HTTP/2 Hỗ trợ
  - Bể kết nối
  - Minh bạch GZIP Nén
  - Bộ nhớ đệm phản hồi
  - Hỗ trợ đánh chặn

- **WebSocket Hỗ trợ**: Giao tiếp thời gian thực
  - Giao tiếp song công hoàn toàn
  - Đóng khung tin nhắn
  - Ping/Pong Nhịp tim

- **SSE (Server-Sent Events)**: Hỗ trợ đẩy máy chủ
  - Phân tích luồng sự kiện
  - Tự động kết nối lại
  - sự kiện ID theo dõi

### Công cụ kịch bản

- **GraalVM JavaScript**：Được sử dụng ở phía trước/Thời gian chạy tập lệnh nhúng cho tập lệnh phía sau
  - ECMAScript 5.1 Hỗ trợ
  - Java khả năng tương tác

- **GraalVM JavaScript** (Java 21+): Hiện đại JavaScript động cơ
  - ECMAScript 2021+ Hỗ trợ
  - Hiệu suất tốt hơn
  - Hỗ trợ đa ngôn ngữ

### Dữ liệu và lưu trữ

- **JSON xử lý**:
  - Gson: JSON tuần tự hóa/Giải tuần tự hóa
  - Jackson: Thay thế JSON bộ xử lý

- **XML xử lý**:
  - Tích hợp sẵn Java XML API
  - XPath Hỗ trợ

- **Lưu trữ tập tin**:
  - Địa phương cho bộ sưu tập giao diện JSON tập tin
  - Tệp biến môi trường
  - Git Tích hợp kho hàng

### Kiểm soát phiên bản

- **JGit**: Tinh khiết Java Git hoàn thành
  - Clone、commit、push、pull hoạt động
  - Quản lý chi nhánh
  - Phát hiện xung đột
  - SSH và HTTPS Chứng nhận

### Ghi nhật ký

- **SLF4J + Logback**: Khung ghi nhật ký
  - Cấu hình linh hoạt
  - Nhiều phương thức đầu ra (bảng điều khiển, tệp, cuộn)
  - Tối ưu hóa hiệu suất
  - Hỗ trợ ghi nhật ký không đồng bộ

---

## 📦 Xây dựng và đóng gói

### Xây dựng công cụ

- **Maven**: Quản lý dự án và tự động hóa tòa nhà
  - Quản lý sự phụ thuộc
  - Hệ sinh thái plugin
  - Hỗ trợ đa mô-đun

### Chiến lược đóng gói

1. **Đa nền tảng JAR**:
   ```
   mvn clean package
   ```
   - phát ra `easy-postman-{Số phiên bản}.jar`
   - Cần thiết Java 21+ thời gian chạy
   - Kích thước phân phối tối thiểu

2. **Gói cài đặt gốc**:
   - **macOS DMG**: sử dụng `jpackage` Tùy chỉnh biểu tượng và hình nền
   - **Windows EXE**: Inno Setup Trình cài đặt đi kèm với các phím tắt
   - **Windows Phiên bản di động ZIP**: Sẵn sàng giải nén, không cần cài đặt
   - **Linux DEB**: Debian Gói phần mềm đi kèm với tích hợp máy tính để bàn

3. **Thời gian chạy tùy chỉnh**:
   - sử dụng `jlink` Tạo sự tối thiểu hóa JRE
   - Chỉ có các mô-đun cần thiết được bao gồm
   - Giảm khoảng 50% kích thước phân phối

### Khuyến nghị phát triển JDK

> 💡 **Khuyến khích JetBrains Runtime (JBR)** Nhận được điều tốt nhất Swing hiệu suất:
>
> - Hiển thị phông chữ tốt hơn trên tất cả các nền tảng
> - Được cải thiện HiDPI Hỗ trợ
> - Swing cụ thể bug Sửa chữa
> - Bộ sưu tập rác được tối ưu hóa cho các ứng dụng máy tính để bàn
>
> **Địa chỉ tải về**: [JetBrains Runtime Releases](https://github.com/JetBrains/JetBrainsRuntime/releases)

---

## 🔧 Quy trình phát triển

### Cấu trúc dự án

```
easy-postman/
├── src/main/java/          # Java mã nguồn
│   └── com/laker/postman/
│       ├── ui/             # UI thành phần
│       ├── service/        # logic kinh doanh
│       ├── model/          # Mô hình dữ liệu
│       ├── network/        # lớp mạng
│       └── utils/          # Công cụ
├── src/main/resources/     # Các tập tin tài nguyên
│   ├── icons/              # Biểu tượng ứng dụng
│   ├── themes/             # FlatLaf chủ đề
│   ├── js-libs/            # JavaScript thư viện
│   └── messages*.properties # Các tập tin quốc tế hóa
├── build/                  # Xây dựng kịch bản
│   ├── mac.sh             # macOS Đóng gói
│   ├── win-exe.bat        # Windows Trình cài đặt
│   └── linux-deb.sh       # Linux DEB Đóng gói
└── docs/                   # Tài liệu
```

### Bài kiểm tra

- **Kiểm tra đơn vị**: JUnit 5
- **Kiểm tra tích hợp**: TestNG
- **UI Bài kiểm tra**: Kiểm tra thủ công trên các chủ đề và độ phân giải khác nhau

---

## 🚀 Tối ưu hóa hiệu suất

### UI kết xuất
- Tải các bộ sưu tập lớn một cách lười biếng
- Cuộn ảo của lịch sử
- Tìm kiếm và lọc chống rung
- Hiển thị ngoài màn hình của các thành phần phức tạp

### mạng
- sử dụng OkHttp hồ kết nối
- Yêu cầu loại bỏ hỗ trợ
- Xử lý phản hồi trực tuyến
- Gzip Nén

### Quản lý bộ nhớ
- Bộ nhớ đệm sử dụng các tham chiếu yếu
- Hạn chế lưu giữ lịch sử
- Giới hạn kích thước phản hồi
- Điều chỉnh thu gom rác

### Thời gian bắt đầu
- Khởi tạo lười biếng các thành phần
- Tải tài nguyên song song
- Biểu đồ phụ thuộc được tối ưu hóa
- GraalVM AOT Biên dịch (tương lai)

---

## 🔒 Cân nhắc về an toàn

- **lưu trữ cục bộ**: Tất cả dữ liệu được lưu trữ cục bộ, không đồng bộ hóa trên đám mây
- **Không có đo từ xa**: Không theo dõi hoặc phân tích
- **Certificate verification**: Đúng SSL/TLS Xác minh
- **Lưu trữ thông tin xác thực**: Git Lưu trữ an toàn thông tin xác thực
- **Hộp cát tập lệnh**: JavaScript Thực hiện trong môi trường hộp cát
- **Nhập xác nhận**: Ngăn chặn các cuộc tấn công tiêm

---

## 🌐 Khả năng tương thích đa nền tảng

### Windows
- Cái nhìn và cảm nhận bản địa
- Liên kết tập tin
- Tích hợp đăng ký
- Hỗ trợ cập nhật tự động

### macOS
- Tích hợp thanh menu gốc
- Touch Bar Hỗ trợ (tương lai)
- Tương thích với hộp cát
- Được công chứng để bảo mật

### Linux
- Tích hợp tập tin máy tính để bàn
- Hỗ trợ khay hệ thống
- Nhiều môi trường máy tính để bàn (GNOME、KDE、XFCE）
- AppImage Hỗ trợ (tương lai)

---

## 📈 Những cải tiến trong tương lai

- 🚧 Hệ thống plug-in cho khả năng mở rộng
- 🚧 GraphQL Hỗ trợ
- 🚧 gRPC Hỗ trợ giao thức
- ✅ Tự lưu trữ Mock Server（tập trung Example、Kịch bản động, LAN/CLI、khóa truy cập, trạng thái phiên, nhật ký cuộc gọi)
- 🚧 API Tạo tài liệu
- 🚧 Đồng bộ hóa không gian làm việc trên đám mây (tùy chọn)
- 🚧 Ứng dụng đồng hành trên thiết bị di động
