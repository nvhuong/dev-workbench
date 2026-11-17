# SSL Hướng dẫn cấu hình chứng chỉ ứng dụng khách

## Tổng quan về chức năng

EasyPostman Bây giờ được hỗ trợ SSL Chứng chỉ ứng dụng khách (mTLS - mutual TLS）Chứng nhận. Cần phải xác minh chứng chỉ ứng dụng khách khi truy cập API có thể dựa trên các tên máy chủ khác nhau hoặc  URL Cấu hình chứng chỉ ứng dụng khách thích hợp.

## Các định dạng chứng chỉ được hỗ trợ

### 1. PFX/P12 định dạng (được khuyến nghị)
- Một tệp duy nhất chứa chứng chỉ và khóa riêng
- Thường được bảo vệ bằng mật khẩu
- Phần mở rộng tệp: .`.pfx`, `.p12`
- **Ưu điểm**：Quản lý tập tin rất đơn giản, một tập tin chứa mọi thứ

### 2. PEM định dạng
- Certificate files and private key files are separate
- Certificate file extension: .`.pem`, `.crt`
- Phần mở rộng tệp khóa riêng: .`.key`, `.pem`
- **Chú ý**：Hiện tại chỉ hỗ trợ các tệp khóa riêng không được mã hóa (không được hỗ trợ ENCRYPTED PRIVATE KEY）

## Cách sử dụng

### 1. Mở cài đặt chứng chỉ ứng dụng khách

Chọn: từ menu chính.**cài đặt (Settings)** → **Giấy chứng nhận của khách hàng (Client Certificates)**

### 2. Thêm cấu hình chứng chỉ

Bấm vào **Thêm chứng chỉ** và điền các thông tin sau:

- **tên**（Tùy chọn): Đặt tên cho cấu hình chứng chỉ này để dễ nhận biết
- **tên máy chủ**（Bắt buộc): Tên máy chủ của máy chủ mục tiêu
  - Kết quả chính xác: .`api.example.com`
  - Trận đấu ký tự đại diện: .`*.example.com`（cuộc thi đấu api.example.com、test.example.com Tất cả các tên miền phụ cũng khớp example.com chính nó)
- **cổng**：Số cổng mục tiêu
  - `0` Hoặc để trống: khớp tất cả các cổng
  - Chỉ định số cổng (ví dụ `8443`）：Chỉ khớp với cổng này
- **Certificate type**：Chọn PFX/P12 hoặc PEM
- **Certificate file**（Bắt buộc): Chọn đường dẫn tệp chứng chỉ
- **Tệp khóa riêng**（PEM (Bắt buộc khi định dạng): Chọn đường dẫn tệp khóa riêng
- **mật khẩu**（Tùy chọn): Mật khẩu cho chứng chỉ hoặc khóa riêng
- **Kích hoạt**：Kiểm tra để kích hoạt cấu hình chứng chỉ này

### 3. Quy tắc phù hợp với chứng chỉ

Khi được gửi HTTPS Khi được yêu cầu, hệ thống sẽ tự động tìm kiếm chứng chỉ ứng dụng khách phù hợp: .

1. Mỗi chứng chỉ được kiểm tra theo thứ tự được cấu hình
2. Cái đầu tiên phù hợp**Đã bật**Certificate will be used
3. Quy tắc tương ứng:
   - Tên máy chủ phải khớp (hỗ trợ ký tự đại diện)
   - Số cổng phải khớp (hoặc chứng chỉ được định cấu hình để khớp với tất cả các cổng)

#### Ví dụ về khớp tên máy chủ

| Certificate configuration | Máy chủ phù hợp | Máy chủ không khớp |
|---------|-----------|------------|
| `api.example.com` | `api.example.com` | `test.example.com`, `example.com` |
| `*.example.com` | `api.example.com`, `test.example.com`, `example.com` | `api.test.example.com` |
| `example.com` | `example.com` | `api.example.com`, `www.example.com` |

## Ví dụ về cấu hình

### Ví dụ 1：đơn API máy chủ(PFX định dạng)

```
tên:Production API Client Cert
Tên máy chủ: .api.production.com
Cảng:443
Certificate type:PFX
Certificate file:/path/to/client-cert.pfx
Mật khẩu:your-password
Kích hoạt:✓
```

### Ví dụ 2：Nhiều tên miền phụ (ký tự đại diện)

```
tên:Development Environment
Tên máy chủ: .*.dev.example.com
Cảng:0 (Tất cả các cổng)
Certificate type:PEM
Certificate file:/path/to/cert.pem
Tệp khóa riêng:/path/to/key.pem
Mật khẩu:(Để trống)
Kích hoạt:✓
```

### Ví dụ 3：Dịch vụ trên các cổng cụ thể

```
tên:Internal Service
Tên máy chủ: .internal.company.com
Cảng:8443
Certificate type:PFX
Certificate file:/path/to/internal.p12
Mật khẩu:secret123
Kích hoạt:✓
```

## Certificate file preparation

### Từ PEM Chuyển đổi sang  PFX

Nếu bạn có PEM Chứng chỉ định dạng và khóa riêng có thể được sử dụng OpenSSL Chuyển đổi sang  PFX：

```bash
openssl pkcs12 -export -out client-cert.pfx \
  -inkey private-key.pem \
  -in certificate.pem \
  -certfile ca-cert.pem
```

### Di dời PEM Bảo vệ mật khẩu của khóa riêng

Nếu của bạn PEM Khóa riêng được mã hóa và trước tiên cần phải xóa mật khẩu:

```bash
openssl rsa -in encrypted-key.pem -out unencrypted-key.pem
```

**Cảnh báo**：Các tệp khóa riêng không được mã hóa phải được lưu giữ đúng cách và được khuyến nghị sử dụng PFX định dạng và đặt mật khẩu.

## Biện pháp phòng ngừa an toàn

### 1. Certificate files are secure
- Chứng chỉ ứng dụng khách và khóa riêng rất nhạy cảm, vui lòng đảm bảo quyền truy cập tệp được đặt chính xác
- Nên sử dụng bảo vệ bằng mật khẩu PFX/P12 định dạng
- Không gửi tệp chứng chỉ tới hệ thống kiểm soát phiên bản

### 2. Lưu trữ mật khẩu
- Mật khẩu chứng chỉ được lưu dưới dạng văn bản rõ ràng trong tệp cấu hình: .`~/.easypostman/client_certificates.json`
- Access to this file should be restricted to the current user
- Hãy thận trọng khi sử dụng trên máy tính dùng chung

### 3. Certificate validity period
- Kiểm tra thời hạn hiệu lực của chứng chỉ thường xuyên
- After the certificate expires, the configuration needs to be updated

## Khắc phục sự cố

### The certificate is not valid

1. **Kiểm tra xem cấu hình chứng chỉ có được bật hay không**
   - Xác nhận cấu hình chứng chỉ"Kích hoạt"Tùy chọn được chọn

2. **Kiểm tra sự trùng khớp của tên máy chủ**
   - Kiểm tra đầu ra nhật ký để xác nhận rằng tên máy chủ khớp
   - Chú ý phân biệt chữ hoa chữ thường khi sử dụng ký tự đại diện (được chuyển thành chữ thường khi khớp)

3. **Kiểm tra đường dẫn tệp chứng chỉ**
   - Xác minh rằng tệp chứng chỉ tồn tại và có thể đọc được
   - PEM Định dạng này yêu cầu cả tệp chứng chỉ và tệp khóa riêng

4. **Kiểm tra mật khẩu**
   - PFX/P12 Chứng chỉ định dạng thường yêu cầu mật khẩu
   - Mật khẩu không chính xác sẽ khiến việc tải chứng chỉ không thành công

5. **Xem nhật ký**
   - Chi tiết về việc tải và khớp chứng chỉ được hiển thị trong nhật ký
   - Thông báo lỗi sẽ giúp xác định vấn đề

### Những sai lầm phổ biến

#### "Certificate file not found or not readable"
- The certificate file path is wrong or the file does not exist
- Tệp không có đủ quyền và không thể đọc được

#### "Failed to load client certificate"
- The certificate file format is incorrect
- Mật khẩu không chính xác (PFX/P12）
- PEM Tệp khóa riêng được mã hóa

#### "Encrypted PEM private keys are not supported"
- Của bạn PEM Tệp khóa riêng được mã hóa
- Vui lòng xóa mật khẩu bảo vệ hoặc chuyển đổi sang PFX định dạng

## Chi tiết kỹ thuật

### Vị trí tập tin cấu hình

- **macOS**: `~/.easypostman/client_certificates.json`
- **Windows**: `%USERPROFILE%\.easypostman\client_certificates.json`
- **Linux**: `~/.easypostman/client_certificates.json`

### Các thuật toán được hỗ trợ

- **Certificate type**: X.509
- **Thuật toán khóa riêng**: RSA, EC (Elliptic Curve)
- **Lưu trữ chìa khóa**: PKCS#12 (PFX), PKCS#8 (PEM)

### mTLS Quá trình bắt tay

1. Máy khách khởi tạo máy chủ HTTPS kết nối
2. Máy chủ gửi chứng chỉ và yêu cầu chứng chỉ ứng dụng khách
3. EasyPostman Tìm chứng chỉ ứng dụng khách phù hợp dựa trên tên máy chủ và cổng
4. Gửi chứng chỉ ứng dụng khách và khóa riêng đến máy chủ
5. Máy chủ xác minh chứng chỉ ứng dụng khách và hoàn tất xác thực hai chiều

## Các chức năng liên quan

- **SSL Chế độ xác thực**: Có thể chọn chế độ nghiêm ngặt hoặc chế độ thoải mái trong cài đặt yêu cầu
- **Xem thông tin chứng chỉ**: Thông tin chứng chỉ máy chủ có thể được xem trong phản hồi yêu cầu
- **Nhật ký web**: Có thể được xem SSL Chi tiết bắt tay

## Nhật ký thay đổi

- **v1.0.0**: Phiên bản đầu tiên, được hỗ trợ PFX/P12 và PEM định dạng
- **v1.0.1**: Đã sửa lỗi logic khớp tên miền ký tự đại diện và cải thiện khả năng xử lý lỗi
- **v1.1.0** (Kế hoạch): Mã hóa được hỗ trợ PEM Khóa riêng và mật khẩu được mã hóa và lưu trữ

## Phản hồi và hỗ trợ

Nếu bạn gặp sự cố khi sử dụng tính năng chứng chỉ ứng dụng khách: .

1. Kiểm tra đầu ra nhật ký để biết thông tin lỗi chi tiết
2. Tham khảo phần khắc phục sự cố của tài liệu này
3. Vào GitHub nộp Issue và cung cấp thông tin chi tiết (cẩn thận để không tiết lộ thông tin nhạy cảm)

---

**gợi ý**：Chứng chỉ khách hàng là cấp doanh nghiệp API Một phương thức xác thực quan trọng, cấu hình chính xác có thể mang lại tính bảo mật cao hơn. Nếu tổ chức của bạn sử dụng xác thực chứng chỉ ứng dụng khách, hãy tham khảo ý kiến quản trị viên hệ thống của bạn để biết thông tin cấu hình và tệp chứng chỉ chính xác.

### 4. Certificate management

- **Chỉnh sửa**：Sau khi chọn chứng chỉ, nhấp vào nút Chỉnh sửa để sửa đổi cấu hình
- **xóa bỏ**：Sau khi chọn chứng chỉ, nhấp vào nút Xóa để xóa cấu hình
- **Kích hoạt/Tàn tật**：Chọn hoặc bỏ chọn trực tiếp trong biểu mẫu Enabled cột

## Ví dụ về cấu hình

### Ví dụ 1：cho cụ thể API Cấu hình chứng chỉ

```
tên:Production API Certificate
Tên máy chủ: .api.prod.example.com
Cảng:443
Certificate type:PFX
Certificate file:/path/to/client-cert.pfx
Mật khẩu:your-certificate-password
Kích hoạt:✓
```

### Ví dụ 2：Định cấu hình chứng chỉ cho tất cả các tên miền phụ

```
tên:Development Environment
Tên máy chủ: .*.dev.example.com
Cảng:0 (Phù hợp với tất cả các cổng)
Certificate type:PEM
Certificate file:/path/to/cert.pem
Tệp khóa riêng:/path/to/key.pem
Kích hoạt:✓
```

## Certificate file storage location

Cấu hình chứng chỉ ứng dụng khách được lưu trong: .
- **Windows**: `C:\Users\{Tên người dùng}\.easy_postman\client_certificates.json`
- **macOS/Linux**: `~/.easy_postman/client_certificates.json`

> ⚠️ Lưu ý: Tệp cấu hình chứa đường dẫn chứng chỉ và mật khẩu (văn bản rõ ràng), vui lòng chú ý bảo vệ tính bảo mật của tệp này.

## Câu hỏi thường gặp

### Q1: Những định dạng chứng chỉ nào được hỗ trợ? .
A: Hiện đang được hỗ trợ PFX/P12（Khuyến nghị) và PEM định dạng. Mã hóa chưa được hỗ trợ PEM khóa riêng.

### Q2: Làm thế nào để tạo chứng chỉ kiểm tra? .
A: Có thể được sử dụng OpenSSL Tạo công cụ: .
```bash
# phát ra PFX định dạng
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365
openssl pkcs12 -export -out client-cert.pfx -inkey key.pem -in cert.pem

# PEM Định dạng được sử dụng trực tiếp key.pem và cert.pem
```

### Q3: Tôi nên làm gì nếu chứng chỉ không hợp lệ? .
A: Vui lòng kiểm tra:
1. Whether certificate configuration is enabled
2. Tên máy chủ và cổng có khớp nhau không?
3. Is the certificate file path correct?
4. Mật khẩu chứng chỉ có đúng không?
5. Kiểm tra tệp nhật ký để biết thông tin lỗi chi tiết

### Q4: Làm cách nào để xác minh xem chứng chỉ có được sử dụng không? .
A: Sau khi gửi yêu cầu, thông tin tương tự như sau sẽ được hiển thị trong nhật ký:
```
[INFO] Using client certificate for host: api.example.com (Production API Certificate)
```

## Triển khai kỹ thuật

- sử dụng Java KeyStore API Tải và quản lý chứng chỉ
- vượt qua OkHttp SSLSocketFactory Định cấu hình chứng chỉ ứng dụng khách
- Hỗ trợ TLS 1.2 và TLS 1.3 thỏa thuận
- Tự động khớp với cấu hình chứng chỉ phù hợp nhất

## Tài liệu tham khảo

- [Postman Tài liệu chứng nhận của khách hàng](https://learning.postman.com/docs/sending-requests/authorization/certificates/)
- [mTLS Giới thiệu](https://en.wikipedia.org/wiki/Mutual_authentication)
- [OpenSSL Certificate management](https://www.openssl.org/docs/man1.1.1/man1/openssl.html)

