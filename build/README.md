# Build Scripts

`build/` Thư mục này chỉ chịu trách nhiệm phân phối và đóng gói, đồng thời không chứa các tập lệnh xác minh và phát triển trình cắm thêm.

Tập lệnh hiện tại: .

- `mac.sh`: macOS DMG
- `win-exe.bat`: Windows EXE / portable
- `linux-deb.sh`: Debian / Ubuntu DEB（Kiến trúc sản phẩm tuân theo máy đóng gói hiện tại, vd `amd64` / `arm64`）
- `linux-rpm.sh`: RPM（Kiến trúc sản phẩm tuân theo máy đóng gói hiện tại)

Thỏa thuận thống nhất:

- Maven Đầu tiên hãy sản xuất cái có số phiên bản jar：
  `dev-workbench-app/target/dev-workbench-${version}.jar`
- Tập lệnh đóng gói sau đó được sao chép sang một tên cố định: .
  `dev-workbench.jar`
- `jpackage` Và tên tệp cố định luôn được tham chiếu bên trong gói cài đặt
- `jpackage` vượt qua `dev-workbench-mcp-launcher.properties` Thế hệ bổ sung `DevWorkbenchMCP`；Nó chia sẻ ngôi nhà với trình khởi chạy máy tính để bàn JAR và tích hợp sẵn JRE，Windows phiên bản được sử dụng console launcher

Windows `app-image` và phiên bản di động ZIP Cấu trúc cốt lõi là: .

```text
DevWorkbench/
├── DevWorkbench.exe
├── DevWorkbenchMCP.exe
├── app/
├── runtime/
└── .portable        # Chỉ có một phiên bản di động tồn tại
```

Phiên bản di động đã hoàn tất `app-image` Thêm vào `.portable` Đã đánh dấu, không cần cài đặt thêm Java。`DevWorkbenchMCP.exe` Phụ thuộc vào cùng một thư mục `app/` và `runtime/`，Không được phân phối hoặc sao chép riêng lẻ.

Nó có thể được thực hiện đầu tiên sau khi đóng gói `DevWorkbenchMCP.exe --help` Xác minh MCP Trình khởi chạy và tích hợp sẵn JRE，Mở nó lại `DevWorkbench.exe` Xác minh máy tính để bàn.

Lợi ích của việc làm này:

- Tập lệnh đóng gói không cần thay đổi số phiên bản
- Bên trong gói cài đặt classpath đã sửa
- Tự động cập nhật và thay thế master jar Đơn giản hơn

Nếu điều bạn muốn làm là:

- Xây dựng gói phát hành: xem `build/`
- Plug-in được phát triển cục bộ/Xác minh cài đặt: Xem `scripts/plugin-dev.sh`
- Kiến trúc plug-in và hướng dẫn sử dụng: xem `docs/PLUGINS_zh.md`
