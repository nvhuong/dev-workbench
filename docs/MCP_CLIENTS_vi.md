# Kết nối DevWorkbench MCP với các AI client

DevWorkbench có sẵn một MCP server chạy kiểu **STDIO**: AI client tự khởi động chương trình `DevWorkbenchMCP` và nói chuyện với nó qua stdin/stdout. Bạn không cần mở cổng mạng hay chạy server riêng; chỉ cần khai báo **lệnh khởi động** trong AI client.

Tài liệu này hướng dẫn cho: [Claude Code](#claude-code), [Claude Desktop](#claude-desktop), [Codex](#codex-openai), [Gemini CLI](#gemini-cli), [Kilo Code](#kilo-code), [Cline / Roo Code](#cline--roo-code), [Cursor](#cursor), [Windsurf](#windsurf), [VS Code (GitHub Copilot)](#vs-code-github-copilot).

Chi tiết về tool, kiến trúc và bảo mật: [MCP_SERVER_DESIGN_vi.md](MCP_SERVER_DESIGN_vi.md).

---

## 1. Chuẩn bị: lệnh khởi động

Cài DevWorkbench từ [GitHub Releases](https://github.com/nvhuong/dev-workbench/releases). Bộ cài đã kèm Java và trình khởi chạy `DevWorkbenchMCP`:

| Nền tảng | Lệnh (`command`) |
|---|---|
| macOS | `/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP` |
| Windows (bộ cài) | `C:\Program Files\DevWorkbench\DevWorkbenchMCP.exe` |
| Windows (portable) | `<thư mục giải nén>\DevWorkbenchMCP.exe` |
| Linux (DEB/RPM) | `/opt/devworkbench/bin/DevWorkbenchMCP` |
| Chỉ có file JAR (cần Java 21+) | `java` với args `-jar /đường/dẫn/dev-workbench-<phiên bản>.jar mcp serve` |

Kiểm tra nhanh trong terminal: chạy lệnh trên kèm `--help`, phải in ra hướng dẫn sử dụng.

### Tham số (`args`) thường dùng

| Mục đích | `args` |
|---|---|
| Mặc định: mọi workspace đã đăng ký, chỉ tool đọc và chạy request | `[]` |
| Cho phép tool ghi dữ liệu / tạo tải (`run_performance`, `sql_execute`, `redis_write`, `kafka_send`, `elasticsearch_write`, `influxdb_write`, `remote_exec`, `minio_presigned_url`) | `["--allow-writes"]` |
| Chỉ cho phép một workspace | `["/đường/dẫn/tuyệt/đối/tới/workspace"]` |
| Tăng giới hạn kích thước kết quả (mặc định 64 KiB) | `["--max-response-bytes", "262144"]` |

Có thể kết hợp, ví dụ `["/path/workspace", "--allow-writes"]`. Với JAR, đặt các tham số này **sau** `"serve"`.

> **Lưu ý an toàn:** chỉ thêm `--allow-writes` cho AI client bạn tin tưởng. Kết nối tới SQL, Redis, Kafka, Elasticsearch, InfluxDB, MinIO, SSH luôn dùng **profile đã lưu trong DevWorkbench**, nên mật khẩu/token không đi qua AI. Hãy tạo profile trong app trước.

Trong các ví dụ dưới đây dùng đường dẫn macOS; trên Windows/Linux chỉ cần thay `command`. Trong JSON trên Windows, nhớ viết `\\` thay cho `\`.

---

## Claude Code

Thêm bằng lệnh (mọi cờ phải đứng **trước** tên server, lệnh khởi động đứng sau `--`):

```bash
# Chỉ dùng trong dự án hiện tại
claude mcp add dev-workbench -- /Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP

# Dùng cho mọi dự án
claude mcp add --scope user dev-workbench -- /Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP

# Bật tool ghi dữ liệu
claude mcp add --scope user dev-workbench -- /Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP --allow-writes
```

Chia sẻ cho cả nhóm: tạo `.mcp.json` ở gốc repo:

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

Kiểm tra: `claude mcp list`, hoặc gõ `/mcp` trong phiên Claude Code.

## Claude Desktop

Mở **Settings → Developer → Edit Config** (file `claude_desktop_config.json`; macOS: `~/Library/Application Support/Claude/`, Windows: `%APPDATA%\Claude\`) và thêm:

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

Thoát hẳn và mở lại Claude Desktop. Biểu tượng công cụ trong ô chat sẽ hiện các tool của DevWorkbench.

## Codex (OpenAI)

Codex CLI và extension Codex trong IDE dùng chung file `~/.codex/config.toml`.

```bash
codex mcp add dev-workbench -- /Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP
```

Hoặc sửa trực tiếp `~/.codex/config.toml`:

```toml
[mcp_servers.dev-workbench]
command = "/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP"
args = []
# Bật tool ghi dữ liệu:
# args = ["--allow-writes"]
```

Kiểm tra: `codex mcp list` hoặc `codex mcp get dev-workbench --json`.

## Gemini CLI

```bash
# Mặc định lưu vào .gemini/settings.json của dự án; -s user để dùng cho mọi dự án
gemini mcp add -s user dev-workbench /Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP
```

Hoặc sửa `~/.gemini/settings.json`:

```json
{
  "mcpServers": {
    "dev-workbench": {
      "command": "/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP",
      "args": [],
      "timeout": 600000
    }
  }
}
```

`timeout` (mili giây) nên đặt lớn vì `run_collection`, `run_functional`, `run_performance` có thể chạy lâu. Không nên bật `"trust": true` (bỏ qua mọi xác nhận) khi đã dùng `--allow-writes`. Kiểm tra: `gemini mcp list`, hoặc gõ `/mcp` trong phiên Gemini.

## Kilo Code

Kilo Code (extension VS Code và Kilo CLI) đọc file `kilo.jsonc`:
- Toàn cục: `~/.config/kilo/kilo.jsonc`
- Theo dự án: `kilo.jsonc` hoặc `.kilo/kilo.jsonc` ở gốc dự án

Trong extension có thể vào **Settings → MCP → Add Server → Local (stdio)** rồi nhập lệnh, hoặc sửa file:

```jsonc
{
  "mcp": {
    "dev-workbench": {
      "type": "local",
      // Lệnh và tham số nằm chung trong một mảng
      "command": ["/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP"],
      "enabled": true,
      "timeout": 600000
    }
  }
}
```

Bật tool ghi: `"command": ["/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP", "--allow-writes"]`.

Kilo Code bản cũ dùng `mcp_settings.json` / `.kilocode/mcp.json` với định dạng `mcpServers` giống [Cline](#cline--roo-code).

## Cline / Roo Code

Mở bảng **MCP Servers → Configure** (file `cline_mcp_settings.json`; Roo Code: `mcp_settings.json` hoặc `.roo/mcp.json`):

```json
{
  "mcpServers": {
    "dev-workbench": {
      "command": "/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP",
      "args": [],
      "disabled": false,
      "alwaysAllow": ["list_workspaces", "list_collections", "list_requests"],
      "timeout": 600
    }
  }
}
```

`alwaysAllow` chỉ nên chứa các tool đọc; để AI hỏi lại trước khi chạy request hoặc ghi dữ liệu.

## Cursor

Toàn cục: `~/.cursor/mcp.json`; theo dự án: `.cursor/mcp.json`. Hoặc **Cursor Settings → MCP → Add new MCP server**.

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

## Windsurf

**Windsurf Settings → Cascade → MCP Servers → View raw config** (file `~/.codeium/windsurf/mcp_config.json`):

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

## VS Code (GitHub Copilot)

Theo dự án: `.vscode/mcp.json`; cho người dùng: lệnh **MCP: Open User Configuration**. Khóa gốc là `servers` (không phải `mcpServers`):

```json
{
  "servers": {
    "dev-workbench": {
      "type": "stdio",
      "command": "/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP",
      "args": []
    }
  }
}
```

Bản Copilot mới còn đọc `~/.copilot/mcp-config.json` với khóa gốc `mcpServers`, cùng định dạng như Cursor.

## Client khác

Hầu hết client MCP khác (Continue, Zed, LM Studio, Goose, JetBrains AI Assistant…) đều nhận một server STDIO với hai thông tin: **command** = trình khởi chạy ở [mục 1](#1-chuẩn-bị-lệnh-khởi-động) và **args** = tham số cần thiết.

---

## 2. Thử ngay sau khi cài

Khởi động lại AI client, mở cuộc trò chuyện mới và thử lần lượt:

1. `Dùng DevWorkbench MCP liệt kê các workspace. Không chạy request nào.`
2. `Liệt kê collection và request trong workspace <tên>.`
3. `Chạy request "GET Example" với môi trường Dev Env và tóm tắt kết quả.`
4. `Chạy functional test của workspace và cho biết request nào lỗi.`
5. `Liệt kê profile SQL và cho tôi xem cấu trúc bảng CUSTOMERS.` (cần plugin SQL)

## 3. Danh sách tool

| Nhóm | Tool đọc (luôn bật) | Tool hành động |
|---|---|---|
| Workspace, collection, môi trường | `list_workspaces`, `list_collections`, `list_environments`, `list_requests`, `list_folders` | `run_request`, `run_collection`, `create_collection`, `create_folder`, `create_request`, `update_request`, `delete_request` ✱ |
| Functional test | — | `run_functional` |
| Kiểm thử hiệu năng | `list_performance_plans` | `run_performance` ✱ |
| Lịch sử, mock server | `list_request_history`, `list_mock_servers` | — |
| Elasticsearch | `elasticsearch_list_profiles`, `elasticsearch_request` | `elasticsearch_write` ✱ |
| InfluxDB | `influxdb_list_profiles`, `influxdb_query` | `influxdb_write` ✱ |
| Plugin SQL | `sql_list_profiles`, `sql_list_tables`, `sql_describe_table`, `sql_query` | `sql_execute` ✱ |
| Plugin Redis | `redis_list_profiles`, `redis_scan_keys`, `redis_read` | `redis_write` ✱ |
| Plugin Kafka | `kafka_list_profiles`, `kafka_list_topics`, `kafka_poll` | `kafka_send` ✱ |
| Plugin MinIO | `minio_list_profiles`, `minio_list_buckets`, `minio_list_objects`, `minio_read_object` | `minio_presigned_url` ✱ |
| Plugin Decompiler | `decompiler_list_classes`, `decompiler_decompile_class` | — |
| Plugin Remote (SSH) | `remote_list_profiles` | `remote_exec` ✱ |

✱ chỉ xuất hiện khi chạy với `--allow-writes`. Tool của plugin chỉ có khi plugin đã được cài trong DevWorkbench (Plugin → Trung tâm plugin).

## 4. Xử lý sự cố

| Triệu chứng | Cách xử lý |
|---|---|
| Client báo không khởi động được server | Dùng **đường dẫn tuyệt đối** cho `command`; chạy thử lệnh đó kèm `--help` trong terminal. |
| `No runnable Dev Workbench workspace found` | Mở DevWorkbench ít nhất một lần để tạo workspace, hoặc truyền đường dẫn một workspace có `collections.json`. |
| Không thấy tool của plugin (`sql_*`, `redis_*`…) | Cài plugin trong Trung tâm plugin, rồi khởi động lại AI client. |
| Không thấy `run_performance`, `sql_execute`… | Các tool này cần `--allow-writes` trong `args`. |
| Tool chạy lâu bị ngắt | Tăng `timeout` của client (Gemini, Kilo, Cline) như ví dụ ở trên. |
| Kết quả bị cắt (`"truncated": true`) | Thu hẹp truy vấn (limit, filter) hoặc thêm `--max-response-bytes`. |
| `remote_exec` báo host chưa tin cậy | Kết nối server đó một lần trong công cụ Remote của DevWorkbench và chấp nhận host key. |
| Thêm/xóa workspace trong app nhưng AI không thấy | Khởi động lại AI client (MCP đọc danh sách workspace khi khởi động). |
| Windows: lỗi đường dẫn trong JSON | Viết `\\`, ví dụ `"C:\\Program Files\\DevWorkbench\\DevWorkbenchMCP.exe"`. |
