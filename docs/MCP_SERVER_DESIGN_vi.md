# DevWorkbench MCP Server

DevWorkbench Tiêu chuẩn tích hợp MCP stdio Server，cho phép Codex、Claude Desktop、Cursor Đợi khách hàng khám phá không gian làm việc, chọn không gian làm việc và môi trường, đồng thời thực hiện lệnh đã lưu trên mỗi lệnh gọi công cụ HTTP yêu cầu.MCP Trực tiếp tái sử dụng chuỗi thực thi không đầu mà không cần thao tác từ xa Swing trang, nó cũng không sửa đổi lựa chọn hiện tại trên màn hình nền. Thực hiện trao đổi thư từ [Issue #169](https://github.com/nvhuong/dev-workbench/issues/169)。

## Cài đặt và cấu hình

### 1. Lấy DevWorkbench

MCP Đã được đưa vào chương trình chính và không được phát hành riêng MCP Gói cài đặt:

| Phương pháp tải xuống | MCP Cách sử dụng | Bạn có cần phải cài đặt nó một cách riêng biệt? Java |
|---|---|---|
| macOS / Windows / Linux Gói cài đặt gốc | Sử dụng những cái có trong gói `DevWorkbenchMCP` launcher | Không, bao gồm JRE |
| Windows Phiên bản di động ZIP | Sử dụng sau khi giải nén `DevWorkbenchMCP.exe` | Không, bao gồm JRE |
| Đa nền tảng JAR | `java -jar ... mcp serve` | Có, bắt buộc Java 21+ |

Được đề xuất từ [GitHub Releases](https://github.com/nvhuong/dev-workbench/releases) Tải xuống gói gốc.Windows Phiên bản di động không cần cài đặt và bạn có thể sử dụng phiên bản máy tính để bàn sau khi giải nén MCP。

### 2. Tìm trình khởi chạy gốc

| Phương pháp cài đặt | `command` |
|---|---|
| macOS | `/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP` |
| Windows Phiên bản cài đặt | `C:\Program Files\DevWorkbench\DevWorkbenchMCP.exe` |
| Windows Phiên bản di động | Giải nén thư mục `DevWorkbenchMCP.exe`，Ví dụ `D:\Tools\DevWorkbench\DevWorkbenchMCP.exe` |
| Linux DEB / RPM | `/opt/devworkbench/bin/DevWorkbenchMCP` |

Windows Phiên bản di động đại khái như sau sau khi giải nén. hai EXE chia sẻ cùng một JAR và thời gian chạy, vì vậy đừng chỉ đặt  `DevWorkbenchMCP.exe` Sao chép nó một cách riêng biệt:

```text
DevWorkbench/
├── DevWorkbench.exe          # Ứng dụng máy tính để bàn
├── DevWorkbenchMCP.exe       # MCP stdio launcher
├── app/
│   ├── dev-workbench.jar
│   ├── DevWorkbench.cfg
│   └── DevWorkbenchMCP.cfg
├── runtime/                 # Tích hợp sẵn JRE
└── .portable                # Chỉ có một phiên bản di động tồn tại
```

macOS Gói cài đặt sử dụng cùng cấu trúc chia sẻ: .

```text
DevWorkbench.app/Contents/
├── MacOS/DevWorkbench
├── MacOS/DevWorkbenchMCP
├── app/dev-workbench.jar
├── app/DevWorkbenchMCP.cfg
└── runtime/Contents/Home/
```

### 3. cấu hình MCP khách hàng

Trình khởi chạy gốc sẽ tự động điền nó vào `mcp serve`，Vì vậy, nó được cấu hình theo mặc định `args` Chắc trống rồi, không viết nữa `"mcp", "serve"`。

Codex CLI：

```bash
codex mcp add dev-workbench -- /Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP
```

Cũng có thể được viết Codex của `config.toml`：

```toml
[mcp_servers.dev-workbench]
command = "/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP"
args = []
```

Claude Desktop、Claude Code、Cursor Chờ sử dụng JSON Máy khách có thể được cấu hình hoàn toàn `mcpServers` Sự vật:

```json
{
  "mcpServers": {
    "dev-workbench": {
      "command": "/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP",
      "args": []
    }
  }
}
```

Windows JSON Dấu gạch chéo ngược trong đường dẫn cần phải được viết dưới dạng dấu gạch chéo ngược kép, ví dụ: `"D:\\Tools\\DevWorkbench\\DevWorkbenchMCP.exe"`。

Windows Phiên bản di động hoàn chỉnh JSON Ví dụ:

```json
{
  "mcpServers": {
    "dev-workbench": {
      "command": "D:\\Tools\\DevWorkbench\\DevWorkbenchMCP.exe",
      "args": []
    }
  }
}
```

Sau khi lưu, hãy khởi động lại máy khách và tạo cuộc trò chuyện mới. Trước tiên, bạn nên thực hiện xác minh chỉ đọc:

```text
Hãy sử dụng DevWorkbench MCP Liệt kê tất cả các không gian làm việc mà không thực hiện bất kỳ giao diện nào.
```

Codex、Claude và Cursor Mục cấu hình cụ thể có thể thay đổi theo phiên bản máy khách, vui lòng tham khảo các tài liệu chính thức tương ứng:[Codex MCP](https://developers.openai.com/codex/mcp/)、[Claude MCP](https://docs.anthropic.com/en/docs/claude-code/mcp)、[Cursor MCP](https://docs.cursor.com/context/model-context-protocol)。

### 4. Mô hình quy trình và không gian làm việc mặc định

Theo mặc định, không cần cấu hình thư mục không gian làm việc.MCP Quá trình này sẽ đọc DevWorkbench Đối với không gian làm việc đã đăng ký, khách hàng sẽ gọi nó trước `list_workspaces`，Để theo dõi Tool đang đến `workspaceId` Bạn có thể chọn một không gian làm việc.

MCP Máy khách bắt đầu một cho mỗi kết nối `DevWorkbenchMCP` quá trình và nhiều lần Tool Tái sử dụng giữa các cuộc gọi; không khởi động lại mỗi khi yêu cầu được thực hiện. Sau khi kết nối hoàn tất stdin đóng cửa,MCP Quá trình kết thúc và không có người dùng nào được yêu cầu duy trì dịch vụ thường trú.

ứng dụng máy tính để bàn và MCP là hai quy trình độc lập có thể chạy đồng thời và đọc cùng một loạt không gian làm việc.MCP Các lựa chọn không gian làm việc và môi trường chỉ ảnh hưởng đến thời điểm này Tool Việc gọi điện sẽ không mô phỏng việc nhấp vào trang cũng như không thay đổi lựa chọn hiện tại trên màn hình. Sau khi thêm hoặc xóa không gian làm việc trên màn hình nền, bạn cần khởi động lại MCP Máy khách kết nối để làm mới thư mục.

### 5. Tùy chọn: Chỉ ủy quyền cho một không gian làm việc

Nếu bạn cần thu hẹp phạm vi ủy quyền, hãy chuyển đến trình khởi chạy gốc `args` Truyền vào đường dẫn tuyệt đối của không gian làm việc: .

```json
{
  "command": "/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP",
  "args": ["/absolute/path/to/my-api-workspace"]
}
```

Thư mục này nên chứa `collections.json`，`environments.json` Không bắt buộc.

### 6. Đa nền tảng JAR Cách thay thế

Chỉ tải xuống JAR Cấu hình bên dưới vẫn có thể được sử dụng nhưng bắt buộc Java 21+：

```json
{
  "command": "java",
  "args": ["-jar", "/absolute/path/to/dev-workbench-{version}.jar", "mcp", "serve"]
}
```

JAR Để chỉ cho phép một không gian làm việc, trong `"serve"` Thêm thư mục sau. Đường dẫn trình khởi chạy gốc thường không thay đổi sau khi nâng cấp ứng dụng; đã được phiên bản JAR Sau khi tên tệp thay đổi, cấu hình máy khách cần được sửa đổi đồng thời.

Các thông số tùy chọn `--max-response-bytes <n>` Hãy kiểm soát một lần Tool Tổng số lượng nội dung phản hồi được cuộc gọi trả về, mặc định là 64 KiB，Có thể được đặt thành 1 KiB～10 MiB。Trình khởi chạy gốc sẽ đưa nó vào ngay `args`；JAR cách để đặt `"serve"` sau đó.

Tham số `--allow-writes` bật các tool ghi của plugin (`sql_execute`, `redis_write`, `kafka_send`, `minio_presigned_url`). Mặc định chỉ có các tool đọc; không có cờ này thì tool ghi không xuất hiện trong `tools/list` và gọi trực tiếp sẽ báo không tìm thấy.

### 7. Khắc phục sự cố cài đặt

- Chạy trực tiếp trước `DevWorkbenchMCP --help`；Có thể hiển thị thông tin trợ giúp cho trình khởi chạy và phần mềm tích hợp JRE trọn.
- trong cấu hình `command` Nên sử dụng đường dẫn tuyệt đối.Windows JSON Các đường dẫn cần thoát khỏi dấu gạch chéo ngược và các đường dẫn có dấu cách trong dòng lệnh cần được trích dẫn.
- Windows Phiên bản di động phải được giải nén hoàn toàn chứ không được chỉ sao chép EXE；Sau khi nâng cấp hoặc di chuyển thư mục giải nén phải cập nhật đồng thời MCP cấu hình.
- Codex Có thể thực thi được `codex mcp get dev-workbench --json` Kiểm tra lệnh đã lưu cuối cùng; các khách hàng khác sử dụng của riêng họ MCP Server Kiểm tra trang trạng thái.
- Sau khi sửa đổi cấu hình hoặc danh sách không gian làm việc trên màn hình, hãy khởi động lại MCP khách hàng hoặc xây dựng lại cuộc trò chuyện. trong quá trình hoạt động bình thường stdout Chỉ JSON-RPC，Nhật ký thông thường bị tắt theo mặc định; các lỗi cần thiết sẽ được giải mẫn cảm và ghi lại stderr。

## dụng cụ

| Tool | Đầu vào chính | Mục đích |
|---|---|---|
| `list_workspaces` | Không | Liệt kê các không gian làm việc được ủy quyền và tính ổn định của quá trình này ID |
| `list_collections` | `workspaceId?` | Liệt kê bộ sưu tập, mô tả và số lượng được yêu cầu |
| `list_environments` | `workspaceId?` | Liệt kê tên môi trường và biến, không trả về giá trị biến |
| `list_requests` | `workspaceId?`、`collectionId?`、`query?`、`limit?` | Tìm các yêu cầu đã lưu |
| `run_request` | `workspaceId?`、`requestId`、`environmentId?`、`environmentOverrides?` | Thực hiện một HTTP yêu cầu |
| `run_collection` | `workspaceId?`、`collectionId`、`environmentId?`、`environmentOverrides?`、`iterations?`、`bail?` | Thực hiện toàn bộ bộ sưu tập |

### Tool của plugin

Plugin đã cài (trong `<thư mục dữ liệu>/plugins/installed`) có thể đăng ký thêm tool qua `PluginContext.registerMcpTool(McpToolContribution)`. Tool của plugin không bao giờ ghi đè tên tool có sẵn. Mọi kết nối đều dùng **profile đã lưu trong plugin** theo tên, nên mật khẩu/khóa không đi qua AI client và không bao giờ được trả về.

| Plugin | Tool đọc (luôn bật) | Tool ghi (cần `--allow-writes`) |
|---|---|---|
| SQL | `sql_list_profiles`, `sql_list_tables`, `sql_describe_table`, `sql_query` (chỉ một câu SELECT/WITH/SHOW/DESCRIBE/EXPLAIN, kết nối read-only, tối đa 1000 dòng, timeout 60 s) | `sql_execute` (một câu lệnh, commit khi thành công, rollback khi lỗi) |
| Redis | `redis_list_profiles`, `redis_scan_keys` (SCAN, không chặn), `redis_read` (GET, EXISTS, TYPE, TTL, HGET, HGETALL, LRANGE, SMEMBERS, ZRANGE) | `redis_write` (SET với `ttlSeconds` tùy chọn, DEL) |
| Kafka | `kafka_list_profiles`, `kafka_list_topics`, `kafka_poll` (consumer group tạm, không commit offset) | `kafka_send` |
| MinIO | `minio_list_profiles`, `minio_list_buckets`, `minio_list_objects`, `minio_read_object` (tối đa 1 MB, text nếu là UTF-8) | `minio_presigned_url` (link ai có cũng tải được) |
| Decompiler | `decompiler_list_classes`, `decompiler_decompile_class` (file .jar/.war/.ear/.zip/.class trên máy) | — |

Kết quả mọi tool bị giới hạn bởi `--max-response-bytes`; khi vượt, trả về `truncated: true` kèm `partialJson`.

### Ngữ nghĩa lựa chọn không gian làm việc và môi trường

ở đây“Chọn”Đây là định tuyến cấp độ cuộc gọi, không phải mô phỏng các thao tác nhấp chuột thủ công trên trang dành cho máy tính để bàn:

- Khi bạn chuyển vào thư mục không gian làm việc khi khởi động, chỉ thư mục đó mới được ủy quyền, tất cả chúng Tool Tất cả đều có thể được bỏ qua `workspaceId`。
- Khi thư mục khởi động bị bỏ qua, .MCP Quá trình này được đọc khi khởi động DevWorkbench Không gian làm việc đã đăng ký. Khách hàng gọi trước `list_workspaces`，Sau đó trả lại nó `workspaceId` Đã chuyển sang theo dõi Tool，Bạn có thể thực hiện các yêu cầu giữa các không gian làm việc khác nhau.
- Bị bỏ qua trong chế độ nhiều không gian làm việc `workspaceId`，Sẽ sử dụng MCP Được ghi lại khi quá trình bắt đầu DevWorkbench Không gian làm việc hiện tại; nếu nó không thể được xác định duy nhất thì cần phải chuyển rõ ràng `workspaceId`。
- Chọn `workspaceId` Phiên bản dành cho máy tính để bàn sẽ không được gọi“Chuyển đổi không gian làm việc”，Không gian làm việc hiện tại mới sẽ không được lưu và những không gian làm việc đã mở cũng sẽ không được lưu Swing Trang chuyển đổi tương ứng. Sau khi thêm, bớt hoặc chuyển đổi không gian làm việc trên desktop, bạn nên khởi động lại MCP Quá trình khách hàng làm mới thư mục; luôn luôn được thông qua một cách rõ ràng `workspaceId` Ổn định nhất.
- `list_environments` Được sử dụng để khám phá môi trường;`run_request` / `run_collection` của `environmentId` Bạn có thể vượt qua môi trường ID hoặc tên chính xác. Khi bị bỏ qua, không gian làm việc sẽ được đọc `environments.json` Trong môi trường kích hoạt, vật phẩm đầu tiên được sử dụng khi không có vật phẩm kích hoạt.
- `environmentId` Nó chỉ xác định môi trường nào sẽ sử dụng cho việc thực thi này và không sửa đổi trạng thái kích hoạt hoặc lưu nó `environments.json`。

Tool Tham số không chấp nhận bất kỳ thư mục cục bộ nào làm tham số `workspaceId`，Chỉ chấp nhận điều này MCP Được ủy quyền khi quá trình bắt đầu ID。

`environmentId` Chọn môi trường đã lưu. Nếu bỏ qua, môi trường hoạt động sẽ được sử dụng.`environmentOverrides` Nhận giá trị khóa chuỗi, ví dụ: .

```json
{
  "environmentId": "development",
  "environmentOverrides": {
    "baseUrl": "http://127.0.0.1:8080",
    "apiToken": "temporary-value"
  }
}
```

Giá trị bị ghi đè chỉ có hiệu lực trong môi trường bộ nhớ của lần chạy này và sẽ không bị sửa đổi `environments.json`，Kết quả chỉ trả về tên biến bị ghi đè và không lặp lại giá trị đầu vào.

### Những cái nào có thể được gọi API

- `list_requests` Có thể nhấn `collectionId` và tìm kiếm từ khóa cho các yêu cầu đã lưu trong không gian làm việc; kết quả bao gồm ổn định `requestId`、method、Giải mẫn cảm URL và protocol。
- `run_request` Chỉ thực hiện những gì được chỉ định `requestId` Cái tương ứng đã được lưu HTTP Yêu cầu; không thể được chuyển trực tiếp URL、method、header hoặc body Yêu cầu lắp ráp tạm thời.
- `run_collection` Thực hiện tất cả các yêu cầu đã lưu trong bộ sưu tập cấp cao nhất được chỉ định.MCP v1 Chỉ được hỗ trợ HTTP；Chứa trong một yêu cầu hoặc bộ sưu tập duy nhất SSE/WebSocket Sẽ từ chối thực hiện trong khi chờ thỏa thuận.
- Mức độ chi tiết của ủy quyền hiện tại là“khu vực làm việc”，KHÔNG collection/request Danh sách trắng. Miễn là một không gian làm việc được cấp phép, khách hàng có thể khám phá và thực thi bất kỳ không gian làm việc đã lưu nào HTTP Yêu cầu; khi cần có các hạn chế nghiêm ngặt, các tham số khởi chạy chỉ nên được sử dụng để chỉ cấp phép cho một không gian làm việc chuyên dụng và chỉ giữ lại những không gian làm việc được phép gọi đến đó API。
- `environmentOverrides` Có thể thay đổi `{{baseUrl}}` Đợi đã URL biến, vì vậy nó không phải là danh sách trắng tên miền. Không gian làm việc sản xuất chỉ nên được ủy quyền cho những người đáng tin cậy MCP khách hàng.
- binary / multipart Việc tải lên chỉ cho phép đọc các tệp thực trong không gian làm việc được ủy quyền hiện tại; những đường dẫn tuyệt đối.`..` Cả liên kết tượng trưng lẫn liên kết tượng trưng đều không thể thoát ra ngoài vùng làm việc.

Chuỗi cuộc gọi điển hình:

```text
list_workspaces
  → list_collections / list_environments
  → list_requests
  → run_request / run_collection
```

## ngành kiến ​​​​trúc

```text
MCP Client
  │ stdio JSON-RPC
  ▼
dev-workbench-mcp
  ├─ official MCP Java SDK
  ├─ stdio lifecycle
  ├─ Sáu tool schema + annotations + tool của plugin
  └─ DevWorkbenchMcpBackend
                │
                ▼
dev-workbench-app
  ├─ mcp serve CLI
  ├─ authorized workspace catalog
  ├─ collection/environment adapter
  ├─ response redaction and size limit
  └─ WorkspaceRunExecutor
       ├─ inheritance and variables
       ├─ pre/post scripts and tests
       └─ HTTP runtime
```

`dev-workbench-mcp` Chỉ có lớp giao thức, không có lớp phụ thuộc app、Swing、Lưu trữ không gian làm việc hoặc thực hiện các yêu cầu cụ thể.app hoàn thành backend Và tái sử dụng chuỗi thực thi không đầu hiện có nên không có bản sao nào được tạo ra DevWorkbench các biến, tập lệnh,Cookie、TLS và HTTP Ngữ nghĩa. SPI plugin chỉ được thêm phương thức mặc định `PluginContext.registerMcpTool`, tương thích ngược nên không cần tăng `plugin.platform.version`; plugin gọi nó trong `try/catch (LinkageError)` để vẫn chạy trên host cũ.

## Ranh giới an ninh

- Chế độ không gian làm việc đơn chỉ cho phép các thư mục trong tham số khởi động; chế độ đa không gian làm việc chỉ cho phép DevWorkbench Không gian làm việc trong sổ đăng ký.
- MCP v1 Không có tệp bộ sưu tập hoặc môi trường nào được ghi và không có công cụ nhập, lưu và xóa nào được cung cấp.
- Tool ghi của plugin chỉ được bật khi khởi động với `--allow-writes`; tool đọc của SQL dùng kết nối read-only, tự động commit tắt và chặn script nhiều câu lệnh.
- Giá trị môi trường không được trả về thông qua công cụ liệt kê; ghi đè một lần sẽ không được ghi vào đĩa. Nhưng mục tiêu API Các giá trị đầu vào có thể được lặp lại trong nội dung phản hồi, nội dung này có thể chứa dữ liệu cá nhân hoặc doanh nghiệp, do đó chỉ có thể có các kết nối đáng tin cậy MCP khách hàng.
- Tiêu đề phản hồi xác thực phổ biến, tùy chỉnh token/key/password/secret cái đầu,URL user-info、Các tham số truy vấn nhạy cảm và các giá trị khóa nhạy cảm phổ biến trong nội dung phản hồi sẽ được giải mẫn cảm; giải mẫn cảm chung không thể xác định tất cả các lĩnh vực kinh doanh và không thể thay thế việc phân loại dữ liệu và các quyền tối thiểu ở phía máy chủ.
- Nội dung phản hồi có giới hạn byte trên và liệu nó có bị cắt bớt và số byte ban đầu được trả về hay không; số lượng tối đa được trả về cho mỗi cuộc gọi là 100 lời yêu cầu/Chi tiết phản hồi, số lượng bị bỏ qua được trả về khi vượt quá, số lượng tóm tắt vẫn bao gồm toàn bộ lần chạy.
- Các cuộc gọi thực thi trong cùng một quy trình được tuần tự hóa; mỗi lần Tool Rõ ràng trước và sau khi thực hiện MCP quá trình Cookie，Bộ sưu tập vẫn có thể được chia sẻ trong bộ sưu tập Cookie，Tuy nhiên, các phiên không được kế thừa giữa các cuộc gọi và không gian làm việc khác nhau.
- stdio Nhập gặp phải EOF、bất hợp pháp JSON、Tin nhắn quá khổ hoặc I/O Nó sẽ kết thúc khi có lỗi MCP quá trình và sẽ không để lại một quá trình treo đã mất khả năng giao tiếp.
- stdout Chỉ đầu ra MCP JSON-RPC。MCP Chế độ đóng ứng dụng, plug-in và  HTTP Logback Chỉ đăng nhập những gì cần thiết CLI/Đã viết lỗi giao thức stderr；Có sẵn để khắc phục sự cố JVM Thuộc tính hệ thống khôi phục rõ ràng mức độ nhật ký.

`run_request` và `run_collection` Sẽ truy cập bên ngoài API，Do đó, có thể có những tác dụng phụ thực sự trong kinh doanh MCP annotations Được đánh dấu là hành động thế giới mở không chỉ đọc, có khả năng phá hoại. Không gian làm việc sản xuất chỉ nên được ủy quyền cho các khách hàng đáng tin cậy.

## Xây dựng và xác minh

```bash
mvn -pl dev-workbench-app -am -DskipTests clean package
```

sản phẩm:`dev-workbench-app/target/dev-workbench-{version}.jar`。

Các mô-đun giao thức có thể được kiểm tra độc lập:

```bash
mvn -pl dev-workbench-mcp -am test
```

tham khảo:[MCP đặc điểm kỹ thuật](https://modelcontextprotocol.io/specification/2026-07-28)、[chính thức Java SDK](https://github.com/modelcontextprotocol/java-sdk)。
