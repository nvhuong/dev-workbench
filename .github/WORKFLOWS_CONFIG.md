# GitHub Tóm tắt cấu hình

Tài liệu này giải thích EasyPostman Đã thêm dự án GitHub tập tin cấu hình.

---

## 📁 Cấu trúc tập tin

```
.github/
├── workflows/                          # GitHub Actions Quy trình làm việc
│   ├── pr-check.yml                   # PR Kiểm tra tự động
│   ├── welcome.yml                    # Những người đóng góp mới được chào đón
│   ├── auto-label.yml                 # Ghi nhãn tự động
│   ├── sync-labels.yml                # Đồng bộ hóa các định nghĩa nhãn
│   ├── release.yml                    # Bản phát hành (đa nền tảng)
│   ├── winget.yml                     # WinGet Nộp danh sách
│   └── codeql-analysis.yml            # CodeQL Phân tích bảo mật mã
├── ISSUE_TEMPLATE/                    # Issue bản mẫu
│   ├── bug_report.yml                 # Bug Mẫu báo cáo
│   ├── feature_request.yml            # Mẫu yêu cầu tính năng
│   ├── question.yml                   # Mẫu câu hỏi tư vấn
│   └── config.yml                     # Issue Cấu hình mẫu
├── PULL_REQUEST_TEMPLATE.md           # PR bản mẫu
├── CONTRIBUTING.md                    # Nguyên tắc đóng góp
├── labels.yml                         # Định nghĩa thẻ
├── dependabot.yml                     # Dependabot Phụ thuộc vào cấu hình cập nhật
└── WORKFLOWS_CONFIG.md                # Tài liệu này (hướng dẫn cấu hình)
```

---

## ⚙️ GitHub Actions Quy trình làm việc được giải thích chi tiết

### 1. PR Kiểm tra tự động (`pr-check.yml`)

**Thời điểm kích hoạt**: Khi có PR Gửi tới main/master/develop Khi phân nhánh

**Kiểm tra nội dung**:
- ✅ **Xây dựng các bài kiểm tra**
  - Maven Biên dịch
  - Chạy thử nghiệm đơn vị
  - Tải lên sản phẩm xây dựng
- ✅ **Chất lượng mã**
  - Kiểm tra định dạng mã
  - Xác minh biên soạn
- ✅ **PR Xác minh**
  - nghiên cứu PR Tiêu đề có trống không
  - nghiên cứu PR Mô tả có đầy đủ không?
  - Kiểm tra xung đột hợp nhất
- ✅ **Kiểm tra tóm tắt**
  - Tóm tắt tất cả các kết quả kiểm tra
  - Vào PR Trạng thái kiểm tra được hiển thị trong

**tác dụng**: Đảm bảo mọi thứ PR Tất cả đều đáp ứng tiêu chuẩn chất lượng và có quy trình đánh giá tự động

---

### 2. Những người đóng góp mới được chào đón (`welcome.yml`)

**Thời điểm kích hoạt**: Khi có cái mới Issue hoặc PR khi được tạo

**Chức năng**:
- 🎉 Tự động chào đón những người đóng góp lần đầu
- 📖 Cung cấp liên kết đến các diễn đàn tài liệu và thảo luận
- 💡 Đưa ra gợi ý cho các bước tiếp theo

**tác dụng**: Làm cho những người đóng góp mới cảm thấy được chào đón và tăng tính thân thiện với cộng đồng

---

### 3. Ghi nhãn tự động (`auto-label.yml`)

**Thời điểm kích hoạt**: Khi nào Issue hoặc PR Khi được tạo hoặc chỉnh sửa

**Nhận dạng thông minh**:
- 🏷️ Xác định loại câu hỏi từ tiêu đề và nội dung (bug、feature、question）
- 🧩 Identify relevant components (ui、api、performance、workspace Chờ đợi)
- 💻 Identify the operating system (Windows、macOS、Linux）
- 🌐 Recognize language (Chinese, English)

**tác dụng**: Tự động phân loại các vấn đề để thuận tiện cho việc quản lý và tìm kiếm

---

### 4. Đồng bộ hóa thẻ (`sync-labels.yml`)

**Thời điểm kích hoạt**: Khi nào `.github/labels.json` Khi tệp được cập nhật hoặc được kích hoạt theo cách thủ công

**Chức năng**:
- 📋 theo `labels.yml` Xác định thẻ kho đồng bộ
- 🔄 Tự động tạo các thẻ bị thiếu
- 🎨 Thống nhất màu sắc và mô tả nhãn

**tác dụng**: Giữ hệ thống ghi nhãn nhất quán và tiêu chuẩn hóa

---

### 5. Phát hành bản dựng (`release.yml`)

**Thời điểm kích hoạt**: Khi tạo cái mới Release thời gian, hoặc kích hoạt bằng tay

**Xây dựng nội dung**:
- 📦 **Đa nền tảng Fat JAR** - sử dụng Maven Được xây dựng để hoạt động trên tất cả các nền tảng
- 🪟 **Windows EXE** - sử dụng jpackage và Inno Setup Tạo gói cài đặt
- 📦 **Windows Phiên bản di động** - Phiên bản không cần cài đặt màu xanh lá cây, sẵn sàng sử dụng sau khi giải nén
- 🍎 **macOS DMG** - vì Intel và Apple Silicon (M1、M2、M3、M4) Tạo gói cài đặt
- 🐧 **Linux DEB** - Tạo Debian/Ubuntu Gói cài đặt phân phối sê-ri (`amd64` và `arm64`）
- 🐧 **Linux RPM** - Tạo Red Hat / Rocky / CentOS / Fedora Gói cài đặt hàng loạt (`x86_64` và `aarch64`）

**Đặc điểm**:
- ✅ sử dụng jlink Tạo tinh gọn JRE，Giảm kích thước của gói cài đặt
- ✅ Tự động trích xuất số phiên bản
- ✅ Tất cả các nền tảng đều có chung một nền tảng Fat JAR
- ✅ Tự động tải lên Release trang

**tác dụng**: Tự động hóa quy trình phát hành đa nền tảng để đảm bảo chất lượng phát hành và tính nhất quán

---

### 6. Phân tích bảo mật mã (`codeql-analysis.yml`)

**Thời điểm kích hoạt**:
- Đẩy tới master Khi phân nhánh
- Nhắm mục tiêu master Phân nhánh PR
- Chủ nhật hàng tuần UTC 02:00 Chạy thường xuyên

**Chức năng**:
- 🔍 sử dụng GitHub CodeQL Thực hiện phân tích mã tĩnh
- 🛡️ Phát hiện các lỗ hổng bảo mật tiềm ẩn
- 📊 Identify code quality issues
- ⚠️ Trong nhà kho Security Cảnh báo được hiển thị trong nhãn

**tác dụng**: Liên tục theo dõi bảo mật mã để phát hiện và khắc phục sớm các vấn đề bảo mật

---

### 7. WinGet Nộp danh sách (`winget.yml`)

**Thời điểm kích hoạt**:
- Chỉ hỗ trợ kích hoạt thủ công, phù hợp để đồng bộ phiên bản ổn định theo yêu cầu WinGet
- `mode=initial` Đối với lần đầu tiên nhập cảnh WinGet nhà kho
- `mode=update` Được sử dụng để cập nhật phiên bản tiếp theo sau khi phiên bản đầu tiên được hợp nhất
- Có thể tắt được `submit` Chỉ có bản xem trước của danh sách được tạo chứ không được gửi PR

**Chức năng**:
- Đợi đã GitHub Release trong `EasyPostman-{version}-windows-x64.exe` Tải lên đã hoàn tất
- `initial` Chế độ sẽ tải gói cài đặt về và tính toán SHA256、Tạo phiên bản đầu tiên của ba tệp manifest
- `initial` Các mô hình sẽ manifest Đẩy tới `fork_owner/winget-pkgs` chi nhánh, và tới `microsoft/winget-pkgs` Tạo PR
- `update` Chế độ sẽ tải về chính thức `wingetcreate`，gọi `wingetcreate update Laker.EasyPostman`，và đồng bộ hóa một cách rõ ràng package version Và Apps & Features display version

**Điều kiện tiên quyết**:
- nhà kho Secrets cấu hình `WINGET_CREATE_GITHUB_TOKEN`
- Cái token sử dụng GitHub classic PAT，Ít nhất là cần thiết `public_repo` Quyền, được sử dụng để `fork_owner/winget-pkgs` Đẩy cành về phía  `microsoft/winget-pkgs` Mở PR
- `initial` Yêu cầu mẫu rồi fork `microsoft/winget-pkgs`，Mặc định fork owner vì `lakernote`
- `update` Yêu cầu về chế độ `Laker.EasyPostman` Phiên bản đầu tiên của danh sách đã được xuất bản `microsoft/winget-pkgs` hợp nhất

**Ví dụ gửi đầu tiên**:
Vào Actions Trang chạy `Publish WinGet Manifest`，Chọn `mode=initial`，Điền vào `version=6.0.11`，`fork_owner=lakernote`，`submit=true`。

**Ví dụ sẽ được cập nhật sau**:
Vào Actions Trang chạy `Publish WinGet Manifest`，Chọn `mode=update`，Điền số phiên bản ổn định để đồng bộ.`submit=true`。

**tác dụng**: Khi cần thiết Windows Phiên bản ổn định được đồng bộ hóa với WinGet Kho, người dùng có thể đi qua `winget install --id Laker.EasyPostman -e` Cài đặt hoặc nâng cấp.

---

## 🔧 Giải thích chi tiết về các tập tin cấu hình khác

### 1. Dependabot cấu hình (`dependabot.yml`)

**Chức năng**:
- 📦 Tự động phát hiện Maven Đã cập nhật các phần phụ thuộc
- 🔒 Tự động tạo các bản cập nhật bảo mật PR
- 📅 Kiểm tra cập nhật phụ thuộc hàng tuần
- 🎯 Hạn chế mở cùng một lúc PR Số lượng (tối đa 5 cá nhân)

**tác dụng**:
- Luôn cập nhật các thư viện phụ thuộc và khắc phục các lỗ hổng bảo mật một cách kịp thời
- Giảm khối lượng công việc bảo trì thủ công các phụ thuộc
- Hợp tác GitHub Security Alerts Cung cấp bảo mật toàn diện

---

## 📝 Issue Giải thích chi tiết về các mẫu

### 1. Bug Mẫu báo cáo (`bug_report.yml`)

**Thông tin cần thiết**:
- ✅ Số phiên bản
- ✅ hệ điều hành
- ✅ Mô tả vấn đề
- ✅ Các bước để tái tạo
- ✅ Hành vi dự kiến
- ✅ hành vi thực tế
- ✅ Mức độ nghiêm trọng

**Thông tin tùy chọn**:
- ảnh chụp màn hình
- Nhật ký
- Java Phiên bản
- Thông tin bổ sung

**Ưu điểm**: Thu thập thông tin có cấu trúc cải thiện hiệu quả vị trí vấn đề

---

### 2. Mẫu yêu cầu tính năng (`feature_request.yml`)

**Thông tin cần thiết**:
- ✅ Loại chức năng (API gỡ lỗi/Kiểm tra hiệu suất/không gian làm việc, v.v.)
- ✅ Mô tả vấn đề (điểm đau)
- ✅ giải pháp mong muốn
- ✅ Kịch bản sử dụng
- ✅ ưu tiên
- ✅ Phạm vi người dùng có lợi

**Thông tin tùy chọn**:
- Các lựa chọn thay thế
- Thiết kế giao diện
- Thực hiện tham khảo
- Sẵn sàng đóng góp

**Ưu điểm**: Đạt được sự hiểu biết toàn diện về các yêu cầu tính năng và tạo điều kiện cho các cuộc thảo luận của cộng đồng

---

### 3. Mẫu câu hỏi tư vấn (`question.yml`)

**Thông tin cần thiết**:
- ✅ Hạng mục câu hỏi
- ✅ Mô tả vấn đề

**Thông tin tùy chọn**:
- Thông tin liên quan (phiên bản, hệ điều hành)
- Đã thử các phương pháp
- Thông tin bổ sung

**Ưu điểm**: Trả lời nhanh các câu hỏi của người dùng và xây dựng nền tảng kiến ​​thức

---

## 📋 PR Giải thích chi tiết về các mẫu

**PR bản mẫu** (`PULL_REQUEST_TEMPLATE.md`)

**Chứa nội dung**:
- 📝 PR Mô tả
- 🔗 Có liên quan Issue
- 🎯 Thay đổi loại (Bug Sửa chữa/Các tính năng mới/Cập nhật tài liệu, v.v.)
- 📋 Danh sách các thay đổi
- 🧪 Hướng dẫn kiểm tra
- 📸 Ảnh chụp màn hình (UI những thay đổi liên quan)
- ✅ Danh sách kiểm tra (mã thông số kỹ thuật/Bài kiểm tra/tài liệu)
- 💡 Hướng dẫn bổ sung

**Ưu điểm**:
- tiêu chuẩn hóa PR định dạng
- Hãy chắc chắn rằng các thông tin cần thiết là đầy đủ
- Cải thiện hiệu quả xem xét mã

---

## 📖 Hướng dẫn đóng góp được giải thích chi tiết

**Nguyên tắc đóng góp** (`CONTRIBUTING.md`)

**Chứa nội dung**:
- 🚀 Chuẩn bị trước khi bạn bắt đầu
- 📋 Phương thức đóng góp (báo cáo Bug/Các tính năng được đề xuất/Gửi mã/Cải thiện tài liệu)
- 💻 Cấu hình môi trường phát triển
- 🔄 quá trình phát triển
- 📏 Thông số mã
- ✅ PR Danh sách kiểm tra
- 🎯 Đề xuất phát triển (Cấu trúc dự án/Ngăn xếp công nghệ/kỹ năng sửa lỗi)
- 🤝 Quy tắc ứng xử
- 📞 Chi tiết liên lạc

**Ưu điểm**:
- Hướng dẫn đóng góp đầy đủ
- Hạ ngưỡng đóng góp cho người mới
- Thống nhất quá trình phát triển

---

## 🏷️ Hệ thống thẻ được giải thích chi tiết

**Định nghĩa thẻ** (`labels.yml`)

**Phân loại nhãn**:

1. **Loại câu hỏi**
   - `bug` - Bug Báo cáo
   - `enhancement` - Cải tiến tính năng
   - `question` - Tư vấn thắc mắc
   - `documentation` - Tài liệu liên quan

2. **ưu tiên**
   - `priority-high` - Ưu tiên cao
   - `priority-medium` - Ưu tiên trung bình
   - `priority-low` - Mức độ ưu tiên thấp

3. **trạng thái**
   - `Yêu cầu xác nhận  need-confirm` - Cần thêm thông tin
   - `Cần thảo luận  need-discussion` - Cần có sự thảo luận của cộng đồng
   - `Đang tiến hành  in-progress` - Đang xử lý
   - `Đã giải quyết  resolved` - Đã giải quyết
   - `Sẽ không được sửa chữa  wontfix` - Sẽ không xử lý nó
   - `duplicate` - Lặp lại câu hỏi
   - `invalid` - Câu hỏi không hợp lệ

4. **Nhãn thành phần**
   - `ui` - UI Có liên quan
   - `api` - API Gỡ lỗi liên quan
   - `performance` - Kiểm tra hiệu suất liên quan
   - `workspace` - Quản lý không gian làm việc
   - `environment` - các biến môi trường
   - `script` - Chức năng kịch bản
   - `network` - Các giao thức mạng
   - `import-export` - Nhập khẩu và xuất khẩu
   - `git` - Git tích hợp

5. **Thẻ nền tảng**
   - `windows` - Windows nền tảng
   - `macos` - macOS nền tảng
   - `linux` - Linux nền tảng

6. **Language tag**
   - `chinese` - Tiếng Trung Quốc
   - `english` - Tiếng Anh

7. **Thẻ đặc biệt**
   - `good first issue` - Thích hợp cho người mới bắt đầu
   - `help wanted` - Cần giúp đỡ
   - `security` - Liên quan đến an ninh
   - `dependencies` - Đã cập nhật các phần phụ thuộc

**Ưu điểm**:
- Hệ thống phân loại rõ ràng
- Hỗ trợ nhãn song ngữ
- Tạo điều kiện quản lý vấn đề và lọc

---

## 🎯 Hướng dẫn sử dụng

### Dành cho những người đóng góp

1. **Báo cáo Bug**:
   - thăm [Issues](https://github.com/lakernote/easy-postman/issues/new/choose)
   - Chọn "🐛 Bug Báo cáo" bản mẫu
   - Điền đầy đủ thông tin và gửi

2. **Các tính năng được đề xuất**:
   - thăm [Issues](https://github.com/lakernote/easy-postman/issues/new/choose)
   - Chọn "✨ Yêu cầu tính năng" bản mẫu
   - Mô tả các kịch bản sử dụng và các kịch bản dự kiến

3. **nộp PR**:
   - Fork dự án và tạo ra một chi nhánh
   - Phát triển và thử nghiệm
   - nộp PR（Mẫu sẽ được tự động điền)
   - Đợi quá trình kiểm tra tự động hoàn tất
   - Thực hiện thay đổi dựa trên phản hồi

### Dành cho người bảo trì

1. **quản lý Issues**:
   - Thẻ tự động hỗ trợ phân loại
   - Sử dụng thẻ để lọc và sắp xếp
   - Trả lời và xử lý nhanh chóng

2. **ôn tập PR**:
   - Xem kết quả kiểm tra tự động
   - Tiến hành đánh giá mã
   - Cung cấp phản hồi mang tính xây dựng
   - Hợp nhất hoặc yêu cầu sửa đổi

3. **Duy trì nhãn**:
   - Chỉnh sửa `.github/labels.json`
   - Tự động đồng bộ sau khi đẩy
   - Giữ hệ thống ghi nhãn của bạn gọn gàng

---

## 📊 hiệu quả mong đợi

### Tăng cường tự động hóa
- ✅ PR Tự động xây dựng và thử nghiệm
- ✅ Phân loại thẻ tự động
- ✅ Tự động chào đón những người đóng góp mới

### Góp phần cải thiện trải nghiệm
- ✅ Hướng dẫn đóng góp rõ ràng
- ✅ Có cấu trúc Issue bản mẫu
- ✅ được tiêu chuẩn hóa PR quá trình

### Tối ưu hóa quản lý dự án
- ✅ Các vấn đề được phân loại rõ ràng
- ✅ Các ưu tiên rất rõ ràng
- ✅ Tiến độ có thể được theo dõi

### Xây dựng cộng đồng
- ✅ Hạ ngưỡng đóng góp
- ✅ Nâng cao hiệu quả phản ứng
- ✅ Tăng cường hoạt động cộng đồng

---

## 📞 Cần giúp đỡ? .

Mọi thắc mắc vui lòng:
- 📖 Xem [Nguyên tắc đóng góp](.github/CONTRIBUTING.md)
- 💬 thăm [Bảng thảo luận](https://github.com/lakernote/easy-postman/discussions)
- 📧 Liên hệ với người bảo trì

---

**Cảm ơn bạn vì điều đó EasyPostman Hãy đóng góp! .** 🎉
