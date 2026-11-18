# DevWorkbench Functional CLI

`functional run` Được sử dụng trên các máy phát triển.CI Runner Hoặc trên một máy chủ độc lập, không có giao diện nào có thể được sao chép DevWorkbench GUI“Kiểm tra chức năng”Bảng lưu lựa chọn yêu cầu, thứ tự thực hiện và CSV Nhiều vòng dữ liệu.

CLI để hoàn thành workspace Thư mục là đơn vị đang chạy. Bình thường workspace Toàn bộ thư mục phải được sao chép vào  CI；Git workspace Đáng lẽ phải ở đó CI checkout Sau đó chạy trực tiếp thư mục kho, đừng chỉ sao chép nó `functional_config.json`。

```bash
java -jar dev-workbench.jar functional run <workspace-directory> [options]
```

`<workspace-directory>` Chỉ định trực tiếp workspace thư mục, ví dụ: .

```bash
java -jar dev-workbench.jar functional run /srv/api-workspace
```

## Tải về mới nhất JAR

Mở bất kỳ trang phát hành nào, mới nhất `v*` phiên bản Assets Tải về `dev-workbench-{Số phiên bản}.jar`：

- [GitHub cập nhật Release](https://github.com/nvhuong/dev-workbench/releases/latest)
- [Gitee Releases（Phản ánh trong nước)](https://gitee.com/nvhuong/dev-workbench/releases)

Gitee Đồng bộ hóa tệp đính kèm có thể bị trì hoãn; nếu bản gốc tạm thời không có sẵn trong phiên bản mới nhất JAR，Hãy sử dụng GitHub Địa chỉ tải về. chạy CLI Cần thiết Java 21 hoặc cao hơn.

Nó không cung cấp các tham số lọc bộ sưu tập hoặc thư mục; phạm vi thực thi được giới hạn ở  `functional_config.json` quyết định.

## 1. Workspace tập tin

tiêu chuẩn workspace Thư mục chứa 4 cá nhân JSON Tập tin:

```text
api-workspace/
├── collections.json          # Bắt buộc: yêu cầu nội dung, tập lệnh và xác nhận
├── environments.json         # Danh sách môi trường và môi trường hoạt động
├── functional_config.json    # Bắt buộc: Yêu cầu đã chọn ID、Nội tuyến tuần tự và tùy chọn CSV
└── performance_config.json   # Cấu hình kiểm tra hiệu năng,functional run Không đọc
```

Trách nhiệm về hồ sơ:

- `functional_config.json` Quyết định những yêu cầu nào sẽ chạy, thứ tự thực hiện và dữ liệu lặp lại mặc định.
- `collections.json` Hãy là nguồn sự thật duy nhất cho nội dung được yêu cầu.
- `requestItemId` Đã sử dụng từ mới nhất `collections.json` Tìm yêu cầu.
- `environments.json` Cung cấp các biến môi trường.
- `performance_config.json` Thuộc tiêu chuẩn workspace，Nhưng đừng tham gia `functional run`。
- Cấp độ ứng dụng `global_variables.json` Từ DevWorkbench Đọc thư mục dữ liệu.
- CLI Chỉ đọc workspace，Sẽ không viết lại GUI cấu hình.

## 2. Thời gian sử dụng ngắn nhất

```bash
java -jar dev-workbench.jar functional run /srv/api-workspace
```

Chọn CI môi trường và tạo báo cáo:

```bash
java -jar dev-workbench.jar functional run /srv/api-workspace \
  -e "CI" \
  --bail \
  --out target/functional-result.json
```

Vào Git workspace Thư mục gốc: .

```bash
java -jar /opt/dev-workbench/dev-workbench.jar functional run .
```

## 3. `functional_config.json`

GUI Ví dụ về cấu trúc đã lưu cho bảng kiểm tra chức năng: .

```json
{
  "version": "1.0",
  "rows": [
    {"selected": true, "requestItemId": "request-login"},
    {"selected": false, "requestItemId": "request-debug"}
  ],
  "csvState": {
    "sourceName": "users.csv",
    "headers": ["user", "role"],
    "rows": [
      {"user": "alice", "role": "admin"},
      {"user": "bob", "role": "user"}
    ]
  }
}
```

Quy tắc chạy:

- Chỉ cần thực hiện `selected: true` ĐƯỢC RỒI.
- Nhấn `rows` Trình tự lưu được thực thi.
- Mỗi `requestItemId` Phải có khả năng ở đó `collections.json` được tìm thấy trong.
- Không có yêu cầu hoặc yêu cầu nào được chọn ID Mã thoát là khi nó đã hết hạn `2`，tránh CI Rò rỉ im lặng.
- `csvState.sourceName` chỉ một GUI Hiển thị nguồn và không đọc lại tệp khi chạy.
- Dữ liệu nhiều vòng mặc định thực sự là `csvState.rows` nội dung được nhúng trong .

GUI Làm mới bảng kiểm tra chức năng và lưu nó để đồng bộ hóa các yêu cầu đã xóa hoặc di chuyển ID。

## 4. CSV / JSON Dẫn động nhiều bánh

### 4.1 Sử dụng nội tuyến CSV

nếu `csvState.rows` Có hai dòng: .

```bash
java -jar dev-workbench.jar functional run "$WORKSPACE_DIR"
```

Theo mặc định, hai vòng được thực hiện và không cần truyền thêm `-d`。

được sử dụng trong các yêu cầu `{{user}}`，Được sử dụng trong tập lệnh:

```javascript
pm.iterationData.get('user');
pm.variables.get('user');
pm.info.iteration;
pm.info.iterationCount;
```

Sẽ `functional_config.json` Gửi tới Git Nó tương đương với việc nộp một trong số họ CSV Được rồi, đừng nhúng mật khẩu thật.Token hoặc dữ liệu sản xuất.

### 4.2 Dữ liệu bên ngoài ghi đè lên dữ liệu nhúng

CI Đã đặt trước GUI Yêu cầu lựa chọn, nhưng sử dụng CI Dữ liệu riêng tư:

```bash
java -jar dev-workbench.jar functional run "$WORKSPACE_DIR" \
  -d /opt/dev-workbench-data/ci-users.csv
```

Tại thời điểm này danh sách yêu cầu vẫn đến từ `functional_config.json`，Dữ liệu lặp lại hoàn toàn đến từ các tệp bên ngoài. Tương đối `-d` đường dẫn từ workspace Độ phân giải thư mục gốc .

JSON Phải là một mảng các đối tượng: .

```json
[
  {"user": "alice", "role": "admin"},
  {"user": "bob", "role": "user"}
]
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

GUI Sau khi máy được lưu, nó sẽ được sao chép hoàn toàn workspace，bao gồm `collections.json` và `functional_config.json`。

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
  functional run "$CI_PROJECT_DIR/api-workspace" \
  -e "CI" \
  --bail \
  --out "$CI_PROJECT_DIR/target/functional-result.json"
```

nếu CI Để sử dụng dữ liệu riêng tư, hãy thay đổi lệnh chạy thành:

```bash
java -jar "$CI_PROJECT_DIR/tools/dev-workbench.jar" \
  functional run "$CI_PROJECT_DIR/api-workspace" \
  -e "CI" \
  -d "$CI_PROJECT_DIR/test-data/ci-users.csv" \
  --bail \
  --out "$CI_PROJECT_DIR/target/functional-result.json"
```

workspace Nó cũng có thể được thông qua CI Artifact、SCP、rsync、Docker volume hoặc Kubernetes volume giao.

## 6. Git workspace Vào CI đang hoạt động

Quy trình bảo trì đề xuất:

1. Máy phát triển clone API workspace nhà kho.
2. DevWorkbench Tay cầm giữa Git workspace Đường dẫn trỏ đến thư mục này.
3. GUI Duy trì Bộ sưu tập, Môi trường, Kiểm tra chức năng và Dấu kiểm trong Bảo trì CSV。
4. Lưu để xem xét và gửi workspace của 4 cá nhân JSON tập tin.
5. commit Và push。
6. CI Mỗi Job Sử dụng sạch sẽ checkout。
7. Chạy trong thư mục gốc của kho lưu trữ `functional run .`。

Vì thế CI Không phụ thuộc GUI Máy đang trực tuyến và việc lựa chọn kiểm tra chức năng cũng như nội dung yêu cầu có thể được thực hiện bất cứ lúc nào Git phiên bản được xem xét cùng nhau.

Đề xuất gửi:

```text
collections.json
functional_config.json
environments.json
performance_config.json
```

Đừng gửi sự thật Token、Cookie、Khóa riêng, dữ liệu sản xuất hoặc báo cáo chạy.

### GitHub Actions

```yaml
name: DevWorkbench Functional Test

on:
  push:
  pull_request:

jobs:
  functional-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "21"

      - name: Run functional tests
        run: |
          mkdir -p target
          java -jar tools/dev-workbench.jar \
            functional run . \
            -e "CI" \
            --bail \
            --out target/functional-result.json

      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: functional-result
          path: target/functional-result.json
```

mã doanh nghiệp và API workspace Kho phụ:

```bash
git clone "$API_WORKSPACE_REPOSITORY" "$CI_PROJECT_DIR/api-workspace"

java -jar "$CI_PROJECT_DIR/tools/dev-workbench.jar" \
  functional run "$CI_PROJECT_DIR/api-workspace" \
  -e "CI"
```

## 7. Môi trường, các biến toàn cục và Secret

Không được truyền lại `-e` ưu tiên khi sử dụng `active: true` môi trường; nếu không thì môi trường đầu tiên sẽ được sử dụng.CI Đề nghị khắc phục `-e "CI"`。

Cô lập thư mục biến toàn cục cấp ứng dụng: .

```bash
java -DdevWorkbench.data.dir="$RUNNER_TEMP/dev-workbench-data" \
  -jar tools/dev-workbench.jar \
  functional run . \
  -e "CI"
```

Secret Không nên viết Git workspace。Có thể vào CI tạm thời checkout Được sử dụng ở Trung Quốc `jq` tiêm: .

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

Tải lên tệp đính kèm không chuẩn workspace tập tin. Đính kèm bởi CI Khi gắn một mình:

```bash
java -jar dev-workbench.jar functional run "$WORKSPACE_DIR" \
  --working-dir "$CI_PROJECT_DIR/test-assets"
```

Đường dẫn trống và vẫn chứa `{{...}}`、Lỗi trước khi gửi nếu tệp không tồn tại hoặc không thể đọc được.

## 9. Các thông số, báo cáo và mã thoát

| các thông số | Mô tả |
|---|---|
| `[workspace-directory]` | workspace thư mục;Git workspace Thư mục gốc có thể ghi được `.` |
| `-w, --workspace` | workspace Việc viết thư mục rõ ràng không thể được sử dụng cùng lúc với các tham số vị trí |
| `-e, --environment` | tên môi trường hoặc  ID |
| `-d, --iteration-data` | Ghi đè dữ liệu nhúng CSV / JSON |
| `-n, --iteration-count` | Tổng số vòng là số nguyên dương |
| `--working-dir` | Tải lên thư mục gốc của tập tin |
| `--out` | JSON Đường dẫn báo cáo |
| `--bail` | Dừng sau khi yêu cầu, tập lệnh hoặc xác nhận đầu tiên không thành công |
| `-h, --help` | giúp đỡ |

`functional run` Không được chấp nhận `-c` hoặc `--folder`。

Mã thoát: .`0` Tất cả đều thành công;`1` Request, script, or assertion failed;`2` thông số,workspace Hoặc cấu hình chức năng không hợp lệ.

Báo cáo schema vì `2.1`，TRONG:

- `selectionMode` Đã sửa như `FUNCTIONAL_CONFIG`。
- `iterationDataSource` Cho biết được nhúng CSV Nguồn, đường dẫn tuyệt đối của dữ liệu ngoài hoặc `<none>`。
- Báo cáo cũng có chứa workspace、Môi trường, số vòng, kết quả yêu cầu và chi tiết xác nhận.

## 10. Ví dụ về kho

Ví dụ workspace：[`docs/examples/functional-cli`](examples/functional-cli/)

```bash
java -jar dev-workbench-app/target/dev-workbench-*.jar \
  functional run docs/examples/functional-cli \
  --bail \
  --out target/functional-run-result.json
```

This command reads `functional_config.json` yêu cầu đã chọn và hai dòng nội tuyến CSV，Thực hiện hai vòng, gửi hai yêu cầu và hoàn thành bốn xác nhận.

tập trung CLI Xem [`COLLECTION_CLI_zh.md`](COLLECTION_CLI_zh.md)。
