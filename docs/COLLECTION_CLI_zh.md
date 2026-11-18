# DevWorkbench Collection CLI

`collection run` Được sử dụng trên các máy phát triển.CI Runner Hoặc trên một máy chủ độc lập, chạy không có giao diện theo bộ sưu tập và thư mục DevWorkbench bản địa workspace。

Nó bắt đầu từ workspace của `collections.json` Đọc yêu cầu và hỗ trợ lựa chọn phạm vi chạy theo bộ sưu tập và thư mục.

CLI để hoàn thành workspace Thư mục là đơn vị đang chạy. Bình thường workspace Toàn bộ thư mục phải được sao chép vào  CI；Git workspace Đáng lẽ phải ở đó CI checkout Sau đó chạy trực tiếp thư mục kho, không sao chép chỉ một trong số chúng JSON tập tin.

```bash
java -jar dev-workbench.jar collection run <workspace-directory> [options]
```

`<workspace-directory>` Chỉ định trực tiếp workspace thư mục, ví dụ: .

```bash
java -jar dev-workbench.jar collection run /srv/api-workspace
```

## Tải về mới nhất JAR

Mở bất kỳ trang phát hành nào, mới nhất `v*` phiên bản Assets Tải về `dev-workbench-{Số phiên bản}.jar`：

- [GitHub cập nhật Release](https://github.com/nvhuong/dev-workbench/releases/latest)
- [Gitee Releases（Phản ánh trong nước)](https://gitee.com/nvhuong/dev-workbench/releases)

Gitee Đồng bộ hóa tệp đính kèm có thể bị trì hoãn; nếu bản gốc tạm thời không có sẵn trong phiên bản mới nhất JAR，Hãy sử dụng GitHub Địa chỉ tải về. chạy CLI Cần thiết Java 21 hoặc cao hơn.

## 1. Workspace tập tin

tiêu chuẩn workspace Thư mục chứa 4 cá nhân JSON Tập tin:

```text
api-workspace/
├── collections.json          # Bắt buộc: Bộ sưu tập, Thư mục, Yêu cầu, Tập lệnh và Xác nhận
├── environments.json         # Danh sách môi trường và môi trường hoạt động
├── functional_config.json    # Cấu hình kiểm tra chức năng,collection run Không đọc
└── performance_config.json   # Cấu hình kiểm tra hiệu năng,collection run Không đọc
```

CLI Hành vi:

- Chỉ yêu cầu cấu hình từ workspace của thư mục gốc `collections.json`。
- Tự động đọc cùng một thư mục `environments.json`。
- `functional_config.json` và `performance_config.json` Thuộc tiêu chuẩn workspace，Nhưng đừng tham gia `collection run`。
- Cấp độ ứng dụng `global_variables.json` Từ DevWorkbench Đọc thư mục dữ liệu.
- `-d` được chỉ định CSV / JSON Nhiều vòng dữ liệu, đường dẫn tương đối từ workspace Độ phân giải thư mục gốc .
- Đường dẫn tải lên tương đối mặc định từ  workspace Độ phân giải thư mục gốc .
- CLI Chỉ đọc workspace Cấu hình, bộ sưu tập và môi trường không được ghi lại.

## 2. Thời gian sử dụng ngắn nhất

chạy workspace Tất cả các bộ sưu tập trong:

```bash
java -jar dev-workbench.jar collection run /srv/api-workspace
```

Chọn Bộ sưu tập, Thư mục và Môi trường:

```bash
java -jar dev-workbench.jar collection run /srv/api-workspace \
  -c "Order API" \
  --folder "Smoke" \
  -e "CI"
```

Vào Git workspace Thư mục gốc: .

```bash
java -jar /opt/dev-workbench/dev-workbench.jar collection run .
```

## 3. Lựa chọn bộ sưu tập và thư mục

Không được truyền lại `-c` Khi chạy tất cả các bộ sưu tập gốc:

```bash
java -jar dev-workbench.jar collection run "$WORKSPACE_DIR"
```

`-c` Có sẵn theo tên bộ sưu tập hoặc ID Chọn và lặp lại:

```bash
java -jar dev-workbench.jar collection run "$WORKSPACE_DIR" \
  -c "User API" \
  -c "Order API"
```

`--folder` Chọn thư mục và các thư mục con của nó theo tên thư mục, có thể lặp lại:

```bash
java -jar dev-workbench.jar collection run "$WORKSPACE_DIR" \
  -c "Order API" \
  --folder "Smoke" \
  --folder "Regression"
```

Mã thoát là khi bộ sưu tập hoặc thư mục không tồn tại `2`，và liệt kê các bộ sưu tập có sẵn hoặc các thư mục chưa từng có.

## 4. CSV / JSON Dẫn động nhiều bánh

### 4.1 CSV

Ví dụ CI trên máy `/opt/dev-workbench-data/users.csv`：

```csv
user,role
alice,admin
bob,user
```

chạy:

```bash
java -jar dev-workbench.jar collection run "$WORKSPACE_DIR" \
  -d /opt/dev-workbench-data/users.csv
```

Theo mặc định, hai vòng được thực hiện. được sử dụng trong các yêu cầu `{{user}}`，Được sử dụng trong tập lệnh:

```javascript
pm.iterationData.get('user');
pm.variables.get('user');
pm.info.iteration;
pm.info.iterationCount;
```

### 4.2 JSON

JSON Phải là một mảng các đối tượng: .

```json
[
  {"user": "alice", "role": "admin"},
  {"user": "bob", "role": "user"}
]
```

```bash
java -jar dev-workbench.jar collection run "$WORKSPACE_DIR" \
  -d /opt/dev-workbench-data/users.json
```

### 4.3 `-n` luật lệ

| dữ liệu | các thông số | hành vi thực tế |
|---|---|---|
| Không có dữ liệu | Không được truyền lại `-n` | 1 bánh xe |
| 2 Được rồi | Không được truyền lại `-n` | 2 bánh xe |
| 2 Được rồi | `-n 1` | Chỉ sử dụng  1 Được rồi |
| 2 Được rồi | `-n 5` | Sử dụng  1、2、1、2、1 Được rồi |
| Không có dữ liệu | `-n 3` | 3 Tạm biệt dữ liệu lặp lại |

## 5. bình thường workspace Sao chép vào CI

bình thường workspace Không bắt buộc phải có Git nhà kho.GUI Sau khi máy được lưu, toàn bộ thư mục sẽ được chuyển thay vì chỉ sao chép `collections.json`。

Phát triển máy đóng gói:

```bash
tar -C /Users/me/DevWorkbench/workspaces \
  -czf order-api-workspace.tar.gz \
  order-api
```

CI Giải nén và chạy: .

```bash
mkdir -p "$CI_PROJECT_DIR/api-workspace"
tar -xzf order-api-workspace.tar.gz \
  -C "$CI_PROJECT_DIR/api-workspace" \
  --strip-components=1

java -jar "$CI_PROJECT_DIR/tools/dev-workbench.jar" \
  collection run "$CI_PROJECT_DIR/api-workspace" \
  -e "CI" \
  -c "Order API" \
  --folder "Smoke" \
  --bail \
  --out "$CI_PROJECT_DIR/target/collection-result.json"
```

Nó cũng có thể được thông qua CI Artifact、SCP、rsync、Docker volume hoặc Kubernetes volume giao workspace。

## 6. Git workspace Vào CI đang hoạt động

Đề nghị cho phép GUI workspace Bản thân danh mục là như vậy Git Kho:

1. Máy phát triển clone API workspace nhà kho.
2. DevWorkbench Tay cầm giữa Git workspace Đường dẫn trỏ đến thư mục này.
3. sử dụng GUI duy trì workspace。
4. Xem xét và gửi workspace của 4 cá nhân JSON tập tin.
5. push Sau đó bởi CI Sử dụng sạch sẽ checkout。
6. CI Thực thi tại thư mục gốc của kho `collection run .`。

Đừng gửi sự thật Token、Cookie、Khóa riêng, dữ liệu sản xuất hoặc báo cáo chạy.

### GitHub Actions

```yaml
name: DevWorkbench Collection Test

on:
  push:
  pull_request:

jobs:
  collection-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Run collection
        run: |
          mkdir -p target
          java -jar tools/dev-workbench.jar \
            collection run . \
            -e "CI" \
            -c "Order API" \
            --folder "Smoke" \
            --bail \
            --out target/collection-result.json

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: collection-result
          path: target/collection-result.json
```

mã doanh nghiệp và workspace Khi chia kho,checkout Sau khi đến thư mục, chuyển trực tiếp thư mục:

```bash
git clone "$API_WORKSPACE_REPOSITORY" "$CI_PROJECT_DIR/api-workspace"

java -jar "$CI_PROJECT_DIR/tools/dev-workbench.jar" \
  collection run "$CI_PROJECT_DIR/api-workspace"
```

CI Không cần đăng ký workspace，Không cần GUI Máy vẫn trực tuyến.

## 7. Môi trường, các biến toàn cục và Secret

Không được truyền lại `-e` ưu tiên khi sử dụng `active: true` môi trường; nếu không thì môi trường đầu tiên sẽ được sử dụng.CI Đề nghị khắc phục `-e "CI"`。

`global_variables.json` Nó là một tập tin cấp độ ứng dụng.CI Bạn có thể chỉ định thư mục dữ liệu cách ly:

```bash
java -DdevWorkbench.data.dir="$RUNNER_TEMP/dev-workbench-data" \
  -jar tools/dev-workbench.jar \
  collection run . \
  -e "CI"
```

Secret Không nên nộp cho workspace。Có thể vào CI tạm thời checkout Được sử dụng ở Trung Quốc `jq` Tiêm một bản sao của môi trường: .

```bash
jq --arg token "$API_TOKEN" '
  map(
    if .name == "CI" then
      .variableList |= map(
        if .key == "apiToken" then .value = $token else . end
      )
    else . end
  )
' environments.json > environments.json.tmp

mv environments.json.tmp environments.json
```

CLI Sẽ không tự động đặt tùy ý shell Các biến môi trường được ánh xạ tới  DevWorkbench các biến.

## 8. Tải lên tập tin

Tải lên tệp đính kèm không chuẩn workspace tập tin. Khi yêu cầu cần được tải lên, bạn có thể sử dụng đường dẫn tuyệt đối:

```text
/opt/dev-workbench-test-data/avatar.png
```

Mặc định là  workspace Độ phân giải thư mục gốc Khi tệp đính kèm nằm trong thư mục gắn kết khác:

```bash
java -jar dev-workbench.jar collection run "$WORKSPACE_DIR" \
  --working-dir "$CI_PROJECT_DIR/test-assets"
```

Đường dẫn trống và vẫn chứa `{{...}}`、Lỗi trước khi gửi nếu tệp không tồn tại hoặc không thể đọc được.

## 9. Các thông số và mã thoát

| các thông số | Mô tả |
|---|---|
| `[workspace-directory]` | workspace thư mục;Git workspace Thư mục gốc có thể ghi được `.` |
| `-w, --workspace` | workspace Việc viết thư mục rõ ràng không thể được sử dụng cùng lúc với các tham số vị trí |
| `-c, --collection` | Chọn một bộ sưu tập, có thể lặp lại |
| `--folder` | Chọn một thư mục, có thể lặp lại |
| `-e, --environment` | tên môi trường hoặc  ID |
| `-d, --iteration-data` | workspace Tương đối hoặc tuyệt đối CSV / JSON con đường |
| `-n, --iteration-count` | Tổng số vòng là số nguyên dương |
| `--working-dir` | Tải lên thư mục gốc của tập tin |
| `--out` | JSON Đường dẫn báo cáo |
| `--bail` | Dừng sau khi yêu cầu, tập lệnh hoặc xác nhận đầu tiên không thành công |
| `-h, --help` | giúp đỡ |

Mã thoát: .`0` Tất cả đều thành công;`1` Request, script, or assertion failed;`2` Tham số hoặc workspace Dữ liệu không hợp lệ.

Báo cáo schema vì `2.1`，`selectionMode` Đã sửa như `COLLECTIONS`。

## 10. Ví dụ về kho

Ví dụ workspace：[`docs/examples/collection-cli`](examples/collection-cli/)

```bash
java -jar dev-workbench-app/target/dev-workbench-*.jar \
  collection run docs/examples/collection-cli \
  -c "DevWorkbench CLI Example" \
  --folder "Smoke" \
  --bail \
  --out target/collection-run-result.json
```

This command sends a request and completes two assertions.

Kiểm tra chức năng CLI Xem [`FUNCTIONAL_CLI_zh.md`](FUNCTIONAL_CLI_zh.md)。
