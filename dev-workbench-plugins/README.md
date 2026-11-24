# DevWorkbench DevWorkbench Plugins

Dưới đây là các mô-đun trình cắm thêm và mô-đun quản lý trình cắm thêm chính thức:

- `plugin-manager`: catalog Phân tích, trực tuyến/Mặt tiền quản lý plug-in và cài đặt ngoại tuyến
- `plugin-client-cert`: Plugin chứng chỉ ứng dụng khách
- `plugin-capture`: Plug-in chụp gói
- `plugin-redis`: Redis plugin
- `plugin-kafka`: Kafka plugin
- `plugin-decompiler`: Java Dịch ngược plugin

Lớp nền tảng chia sẻ vẫn được đặt trong thư mục gốc:

- `dev-workbench-plugin-api`
- `dev-workbench-plugin-runtime`

Các lệnh thường dùng:

```bash
mvn -pl dev-workbench-app,dev-workbench-plugins/plugin-redis -am clean package -DskipTests
mvn clean package -DskipTests
```

Để biết thêm hướng dẫn, hãy xem:

- `docs/PLUGINS_vi.md`
- `docs/BUILD_vi.md`
