# 🚀 Hướng dẫn xây dựng

Tài liệu này chỉ nói về hai điều:

- Xây dựng từ nguồn DevWorkbench
- Tạo các gói phân phối cho từng nền tảng

Kiến trúc plug-in, phát triển plug-in cục bộ, trực tuyến/Để biết hướng dẫn cài đặt ngoại tuyến, hãy xem:

- `docs/PLUGINS_zh.md`

## Điều kiện tiên quyết

### Yêu cầu

- **Java 21** hoặc cao hơn
- **Maven 3.6+**
- **Git**

### gợi ý

- **[JetBrains Runtime (JBR)](https://github.com/JetBrains/JetBrainsRuntime)** - vì Swing Ứng dụng được tối ưu hóa JDK

> 💡 **Tại sao chọn JetBrains Runtime？**
>
> JetBrains Runtime được thiết kế đặc biệt cho Swing/AWT Ứng dụng được tối ưu hóa JDK Các bản phân phối, có sẵn ở:
> - ✨ Tốt hơn Swing/AWT Hiệu suất kết xuất
> - 🎨 Cải thiện hiển thị phông chữ và HiDPI Hỗ trợ
> - 🐛 Tiêu chí cố định JDK trong Swing Có liên quan bug
> - 🚀 Tối ưu hóa cho các ứng dụng máy tính để bàn GC và điều chỉnh hiệu suất
>
> **Địa chỉ tải về**: [JetBrains Runtime Releases](https://github.com/JetBrains/JetBrainsRuntime/releases)

---

## 🔧 Xây dựng từ nguồn

### 1. Sao chép kho lưu trữ

```bash
# GitHub
git clone https://github.com/nvhuong/dev-workbench.git
cd dev-workbench

cd dev-workbench
```

### 2. Xây dựng dự án

```bash
# Làm sạch và đóng gói
mvn clean package

# Bỏ qua kiểm tra (nhanh hơn)
mvn clean package -DskipTests
```

Điều này sẽ tạo ra: .
- `dev-workbench-app/target/dev-workbench-{Số phiên bản}.jar` - Chương trình chính có thể thực thi được JAR
- `dev-workbench-mcp/target/dev-workbench-mcp-{Số phiên bản}.jar` - MCP Mô-đun giao thức; sẽ được sáp nhập vào chương trình chính khi phát hành JAR，Không cần phân phối riêng biệt
- `dev-workbench-plugins/plugin-*/target/dev-workbench-{Số phiên bản}-plugin-*.jar` - plugin JAR

### 3. Chạy ứng dụng

```bash
# Chạy trực tiếp
java -jar dev-workbench-app/target/dev-workbench-*.jar

# Hoặc sử dụng tùy chỉnh JVM tùy chọn
java -Xms256m -Xmx2g -jar dev-workbench-app/target/dev-workbench-*.jar

# Xác minh chủ JAR Tích hợp sẵn MCP Server
java -jar dev-workbench-app/target/dev-workbench-*.jar mcp serve --help
```

### Các plugin được xây dựng cục bộ

Nếu bạn muốn xây dựng một trình cắm cục bộ, hãy sử dụng nó trực tiếp Maven Thế thôi:

```bash
mvn -pl dev-workbench-app,dev-workbench-plugins/plugin-redis -am clean package -DskipTests
```

Nếu bạn muốn xây dựng tất cả các plugin cùng một lúc: .

```bash
mvn clean package -DskipTests
```

Thêm cài đặt plug-in, gói ngoại tuyến,catalog và ghi chú phân phối độc lập, xem `docs/PLUGINS_zh.md`。

---

## 📦 Tạo gói cài đặt gốc

### macOS

#### Yêu cầu
- macOS 10.13+（High Sierra hoặc muộn hơn)
- Xcode Công cụ dòng lệnh: .`xcode-select --install`
- Hỗ trợ jpackage của JDK 21+

#### Xây dựng DMG

```bash
# Cấp quyền thực thi cho tập lệnh
chmod +x build/mac.sh

# Chạy tập lệnh xây dựng
./build/mac.sh
```

**đầu ra**: `dist/DevWorkbench-{Số phiên bản}-macos-{ngành kiến ​​​​trúc}.dmg`
- Apple Silicon (M1/M2/M3/M4): `macos-arm64.dmg`
- Intel: `macos-x86_64.dmg`
- sau khi cài đặt `.app` Chứa cả hai `Contents/MacOS/DevWorkbench` và `Contents/MacOS/DevWorkbenchMCP`，Cả hai đều chung một chủ JAR với tích hợp sẵn JRE

#### Xây dựng bằng tay

```bash
# Chuẩn bị jpackage Đầu vào; đã sửa trong gói cài đặt JAR tên
mkdir -p target/dist-input
cp dev-workbench-app/target/dev-workbench-{Số phiên bản}.jar target/dist-input/dev-workbench.jar

# Apple Silicon
jpackage \
  --input target/dist-input \
  --name DevWorkbench \
  --main-jar dev-workbench.jar \
  --main-class com.nvhuong.devworkbench.App \
  --add-launcher DevWorkbenchMCP=build/dev-workbench-mcp-launcher.properties \
  --type dmg \
  --icon assets/mac/DevWorkbench.icns \
  --app-version {Số phiên bản} \
  --vendor "nvhuong" \
  --mac-package-name "DevWorkbench"

# Intel
jpackage \
  --input target/dist-input \
  --name DevWorkbench \
  --main-jar dev-workbench.jar \
  --main-class com.nvhuong.devworkbench.App \
  --add-launcher DevWorkbenchMCP=build/dev-workbench-mcp-launcher.properties \
  --type dmg \
  --icon assets/mac/DevWorkbench.icns \
  --app-version {Số phiên bản} \
  --vendor "nvhuong" \
  --mac-package-name "DevWorkbench" \
  --java-options "--add-opens java.base/java.lang=ALL-UNNAMED"
```

---

### Windows

#### Yêu cầu
- Windows 10/11
- Hỗ trợ jpackage của JDK 21+
- [WiX Toolset 3.11+](https://wixtoolset.org/)（Được sử dụng cho MSI）
- [Inno Setup 6+](https://jrsoftware.org/isinfo.php)（Được sử dụng cho EXE trình cài đặt)

#### Xây dựng EXE Trình cài đặt

```batch
# Chạy tập lệnh xây dựng
build\win-exe.bat
```

**đầu ra**:
- `dist/DevWorkbench-{Số phiên bản}-windows-x64.exe` - Trình cài đặt
- `dist/DevWorkbench-{Số phiên bản}-windows-x64-portable.zip` - Phiên bản di động
- Thư mục cài đặt và phiên bản di động ZIP Tất cả bao gồm `DevWorkbench.exe`、`DevWorkbenchMCP.exe`、`app/` Và `runtime/`；Phiên bản di động có thể được sử dụng sau khi giải nén hoàn toàn MCP，Không cần Java

#### Xây dựng bằng tay

```batch
rem Chuẩn bị jpackage Nhập
if not exist target\dist-input mkdir target\dist-input
copy dev-workbench-app\target\dev-workbench-{Số phiên bản}.jar target\dist-input\dev-workbench.jar

rem Được tạo để chứa cả launcher và tích hợp sẵn JRE thư mục ứng dụng
jpackage ^
  --type app-image ^
  --input target\dist-input ^
  --name DevWorkbench ^
  --main-jar dev-workbench.jar ^
  --main-class com.nvhuong.devworkbench.App ^
  --add-launcher DevWorkbenchMCP=build\dev-workbench-mcp-launcher.properties ^
  --dest target ^
  --icon assets\win\DevWorkbench.ico ^
  --app-version {Số phiên bản} ^
  --vendor "nvhuong"

rem Tạo một phiên bản di động ZIP；Chính thức EXE Hãy chạy trình cài đặt trực tiếp build\win-exe.bat
echo This is a portable version>target\DevWorkbench\.portable
mkdir target\portable
xcopy target\DevWorkbench target\portable\DevWorkbench\ /E /I
cd target\portable
powershell Compress-Archive -Path DevWorkbench -DestinationPath ..\DevWorkbench-{Số phiên bản}-windows-x64-portable.zip
cd ..\..
```

---

### Linux (Ubuntu/Debian)

#### Yêu cầu
- Ubuntu 18.04+ hoặc Debian 10+
- Hỗ trợ jpackage của JDK 21+
- dpkg-dev: `sudo apt install dpkg-dev`

#### Xây dựng DEB gói

```bash
# Cấp quyền thực thi cho tập lệnh
chmod +x build/linux-deb.sh

# Chạy tập lệnh xây dựng
./build/linux-deb.sh
```

**đầu ra**: `dist/` Thư mục tương ứng với kiến trúc máy đóng gói hiện tại được tạo ra DEB các tập tin chẳng hạn `dist/*.deb`

Sau khi cài đặt trình khởi chạy máy tính để bàn và MCP Các bệ phóng được đặt tương ứng `/opt/devworkbench/bin/DevWorkbench` Và `/opt/devworkbench/bin/DevWorkbenchMCP`，Tích hợp sẵn JRE。

#### Xây dựng bằng tay

```bash
mkdir -p target/dist-input
cp dev-workbench-app/target/dev-workbench-{Số phiên bản}.jar target/dist-input/dev-workbench.jar

jpackage \
  --input target/dist-input \
  --name devworkbench \
  --main-jar dev-workbench.jar \
  --main-class com.nvhuong.devworkbench.App \
  --add-launcher DevWorkbenchMCP=build/dev-workbench-mcp-launcher.properties \
  --type deb \
  --icon assets/linux/DevWorkbench.png \
  --app-version {Số phiên bản} \
  --vendor "nvhuong" \
  --linux-package-name "devworkbench" \
  --linux-menu-group "Development" \
  --linux-shortcut
```

#### Cài đặt DEB gói

```bash
sudo dpkg -i dist/<generated-package>.deb

# Nếu thiếu phụ thuộc
sudo apt-get install -f
```

---

### Linux (dựa trên RPM)

#### Yêu cầu
- Fedora/RHEL/CentOS
- Hỗ trợ jpackage của JDK 21+
- rpm-build: `sudo dnf install rpm-build`

#### Xây dựng RPM gói

```bash
# Cấp quyền thực thi cho tập lệnh
chmod +x build/linux-rpm.sh

# Chạy tập lệnh xây dựng
./build/linux-rpm.sh
```

**đầu ra**: `dist/` Thư mục tương ứng với kiến trúc máy đóng gói hiện tại được tạo ra RPM các tập tin chẳng hạn `dist/*.rpm`

---

## 🔍 Tùy chọn xây dựng nâng cao

### Tùy chỉnh JVM tùy chọn

Chỉnh sửa tập lệnh khởi động hoặc sử dụng  jpackage `--java-options`：

```bash
jpackage \
  --java-options "-Xms256m" \
  --java-options "-Xmx2g" \
  --java-options "-Dfile.encoding=UTF-8" \
  # ... Các lựa chọn khác
```

### sử dụng jlink Tạo thời gian chạy tùy chỉnh

Tạo một cái tối thiểu chỉ chứa các mô-đun cần thiết JRE：

```bash
# Liệt kê các module cần thiết
jdeps --print-module-deps target/dev-workbench-{Số phiên bản}.jar

# Tạo thời gian chạy tùy chỉnh
jlink \
  --add-modules java.base,java.desktop,java.logging,java.naming,java.net.http,jdk.httpserver,java.sql,java.xml \
  --output target/custom-runtime \
  --strip-debug \
  --compress 2 \
  --no-header-files \
  --no-man-pages

# Sử dụng bao bì thời gian chạy tùy chỉnh
jpackage \
  --runtime-image target/custom-runtime \
  # ... Các lựa chọn khác
```

---

## 🐛 khắc phục sự cố

### macOS: "Ứng dụng bị hỏng và không thể mở được"

```bash
# Xóa thuộc tính cách ly
sudo xattr -rd com.apple.quarantine /Applications/DevWorkbench.app
```

### Windows: "Windows Máy tính của bạn đã được bảo vệ"

Bấm vào"Thêm thông tin" → "Vẫn phải chạy"。Điều này được áp dụng không dấu SmartScreen Cảnh báo.

### Linux: Thiếu phụ thuộc

```bash
# Ubuntu/Debian
sudo apt-get install -f

# Fedora/RHEL
sudo dnf install <Thiếu gói>
```

### Xây dựng không thành công, nhắc nhở "jpackage: command not found"

Hãy chắc chắn rằng bạn đang sử dụng  JDK 21+（KHÔNG JRE）Và jpackage Vào PATH Trung bình:

```bash
# nghiên cứu Java Phiên bản
java -version

# nghiên cứu jpackage
jpackage --version

# Nếu không tìm thấy, đường dẫn đầy đủ sẽ được sử dụng
$JAVA_HOME/bin/jpackage --version
```

---

## 📚 Các tài nguyên khác

- [jpackage Tài liệu](https://docs.oracle.com/en/java/javase/17/jpackage/)
- [Maven Tài liệu](https://maven.apache.org/guides/)
- [JetBrains Runtime](https://github.com/JetBrains/JetBrainsRuntime)

---

## 💬 Cần giúp đỡ? .

- 💬 GitHub Bảng thảo luận: [https://github.com/nvhuong/dev-workbench/discussions](https://github.com/nvhuong/dev-workbench/discussions)
- 🐛 Báo cáo một vấn đề: [https://github.com/nvhuong/dev-workbench/issues](https://github.com/nvhuong/dev-workbench/issues)

Contact: phone 0964210295 · email [huong.nguyenvan.hd@gmail.com](mailto:huong.nguyenvan.hd@gmail.com).
