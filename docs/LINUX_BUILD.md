# Linux Hướng dẫn đóng gói hệ thống

## Tổng quan

Dự án này cung cấp hai Linux Kịch bản đóng gói:
- `build/linux-deb.sh` - Được sử dụng cho Ubuntu/Debian Có hệ thống DEB gói
- `build/linux-rpm.sh` - Được sử dụng cho CentOS/RedHat/Fedora Có hệ thống RPM gói

## Điều kiện tiên quyết

### 1. Yêu cầu hệ thống

#### Ubuntu/Debian hệ thống (gói DEB）
```bash
# Cài đặt các công cụ cần thiết
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk maven fakeroot binutils

# Lưu ý: Đã tạo DEB Cần thiết khi cài đặt gói xdg-utils，Nhưng nó không cần thiết khi đóng gói
```

#### CentOS/RedHat hệ thống (gói RPM）
```bash
# CentOS 7/8
sudo yum install -y java-21-openjdk java-21-openjdk-devel maven rpm-build binutils

# Hoặc sử dụng dnf (CentOS 8+/Fedora)
sudo dnf install -y java-21-openjdk java-21-openjdk-devel maven rpm-build binutils
```

### 2. Xác minh môi trường
```bash
# nghiên cứu Java phiên bản (bắt buộc >= 17）
java -version

# nghiên cứu Maven
mvn -version

# nghiên cứu jlink và jpackage（JDK 21+ Mang theo của riêng bạn)
jlink --version
jpackage --version
```

## Các bước đóng gói

### Ubuntu/Debian hệ thống - Đóng gói DEB

1. **Sao chép mã dự án**
   ```bash
   git clone https://gitee.com/lakernote/easy-postman.git
   cd easy-postman
   ```

2. **Chạy tập lệnh đóng gói**
   ```bash
   ./build/linux-deb.sh
   ```

3. **tập tin được tạo ra**
   ```
   dist/<generated-package>.deb
   ```

4. **Cài đặt DEB gói**
   ```bash
   # Phương pháp đề xuất: sử dụng apt Cài đặt (tự động xử lý các phần phụ thuộc)
   sudo apt install ./dist/<generated-package>.deb

   # Hoặc sử dụng nó đầu tiên dpkg Cài đặt và sau đó sửa chữa các phần phụ thuộc
   sudo dpkg -i dist/<generated-package>.deb
   sudo apt-get install -f

   # gỡ cài đặt
   sudo dpkg -r easypostman
   ```

### CentOS/RedHat hệ thống - Đóng gói RPM

1. **Sao chép mã dự án**
   ```bash
   git clone https://gitee.com/lakernote/easy-postman.git
   cd easy-postman
   ```

2. **Chạy tập lệnh đóng gói**
   ```bash
   ./build/linux-rpm.sh
   ```

3. **tập tin được tạo ra**
   ```
   dist/<generated-package>.rpm
   ```

4. **Cài đặt RPM gói**
   ```bash
   # sử dụng rpm Cài đặt
   sudo rpm -ivh dist/<generated-package>.rpm

   # hoặc sử dụng yum/dnf（Các phần phụ thuộc sẽ được xử lý tự động)
   sudo yum install dist/<generated-package>.rpm
   # hoặc
   sudo dnf install dist/<generated-package>.rpm

   # gỡ cài đặt
   sudo rpm -e EasyPostMan
   # hoặc
   sudo yum remove EasyPostMan
   ```

## Hướng dẫn quy trình đóng gói

Cả hai tập lệnh đều thực hiện các bước sau:

1. **Kiểm tra môi trường** - Xác minh Java、Maven、jlink、jpackage Nó đã được cài đặt chưa?
2. **Phiên bản đã đọc** - Từ `pom.xml` Tự động đọc số phiên bản dự án
3. **Maven Xây dựng** - thực hiện `mvn clean package` phát ra fat JAR
4. **Tạo một thời gian chạy** - sử dụng `jlink` Tạo tinh gọn JRE thời gian chạy
5. **Các ứng dụng đóng gói** - sử dụng `jpackage` Tạo gói cài đặt hệ thống

## Tính năng đóng gói

### DEB Đặc điểm gói
- ✅ Tự động tạo lối tắt trên màn hình
- ✅ Đã thêm vào Menu Ứng dụng (danh mục Công cụ phát triển)
- ✅ Chứa hợp lý JRE Thời gian chạy (không cần cài đặt hệ thống Java）
- ✅ Tự động cấu hình các tham số bộ nhớ (256MB-512MB）
- ✅ UTF-8 Hỗ trợ mã hóa

### RPM Đặc điểm gói
- ✅ Tự động tạo lối tắt trên màn hình
- ✅ Đã thêm vào Menu Ứng dụng (danh mục Công cụ phát triển)
- ✅ Chứa hợp lý JRE Thời gian chạy (không cần cài đặt hệ thống Java）
- ✅ Tự động cấu hình các tham số bộ nhớ (256MB-512MB）
- ✅ UTF-8 Hỗ trợ mã hóa
- ✅ MIT Tuyên bố giấy phép

## Sử dụng sau khi cài đặt

### Bắt đầu ứng dụng

1. **Thông qua menu ứng dụng**
   - Tìm thấy trong menu hệ thống "Development" hoặc "công cụ phát triển" phân loại
   - Bấm vào "EasyPostman" biểu tượng để bắt đầu

2. **Thông qua dòng lệnh**
   ```bash
   easypostman
   ```

3. **Kiểm tra vị trí cài đặt**
   ```bash
   # DEB gói
   dpkg -L easypostman

   # RPM gói
   rpm -ql easypostman
   ```

## Câu hỏi thường gặp

### 1. Đóng gói không thành công: không tìm thấy jpackage

**câu hỏi**: `jpackage: command not found`

**giải pháp**:
```bash
# Hãy chắc chắn rằng bạn đã cài đặt hoàn chỉnh JDK 21+（KHÔNG JRE）
# Ubuntu/Debian
sudo apt-get install openjdk-21-jdk

# CentOS/RedHat
sudo yum install java-21-openjdk-devel
```

### 2. Không thể khởi động sau khi cài đặt

**câu hỏi**: Không có phản hồi khi nhấp đúp vào biểu tượng

**giải pháp**:
```bash
# Kiểm tra nhật ký lỗi
journalctl --user -f

# Hoặc chạy trực tiếp từ terminal để xem lỗi
/opt/easypostman/bin/EasyPostMan
```

### 3. Vấn đề về quyền

**câu hỏi**: `Permission denied`

**giải pháp**:
```bash
# Đảm bảo tập lệnh có quyền thực thi
chmod +x build/linux-deb.sh build/linux-rpm.sh
```

### 4. Maven Xây dựng không thành công

**câu hỏi**: Tải xuống phần phụ thuộc không thành công

**giải pháp**:
```bash
# Dọn dẹp Maven Thử lại bộ nhớ đệm
rm -rf ~/.m2/repository
./build/linux-deb.sh  # hoặc ./build/linux-rpm.sh
```

### 5. jlink Thất bại: Không tìm thấy objcopy

**câu hỏi**: `Error: java.io.IOException: Cannot run program "objcopy": error=2, No such file or directory`

**lý do**: jlink Cần thiết khi tạo hình ảnh thời gian chạy `objcopy` Công cụ tối ưu hóa và nén các mô-đun

**giải pháp**:
```bash
# Ubuntu/Debian hệ thống
sudo apt-get install binutils

# CentOS/RedHat hệ thống
sudo yum install binutils
# hoặc
sudo dnf install binutils

# Xác minh cài đặt
objcopy --version
```

### 6. DEB Cài đặt gói không thành công: thiếu xdg-utils

**câu hỏi**: `easypostman depends on xdg-utils; however: Package xdg-utils is not installed.`

**lý do**: DEB Phụ thuộc gói `xdg-utils` để xử lý việc tích hợp máy tính để bàn (phím tắt, liên kết tệp, v.v.)

**giải pháp**:
```bash
# cách 1：Cài đặt phụ thuộc trước, sau đó cài đặt DEB gói
sudo apt-get install xdg-utils
sudo dpkg -i dist/<generated-package>.deb

# cách 2：sử dụng apt-get Tự động xử lý các phần phụ thuộc (được khuyến nghị)
sudo apt-get install -f
# Điều này sẽ tự động cài đặt các phần phụ thuộc còn thiếu và hoàn tất easypostman cấu hình

# cách 3：sử dụng apt Cài đặt trực tiếp (các phần phụ thuộc sẽ được xử lý tự động)
sudo apt install ./dist/<generated-package>.deb
```
