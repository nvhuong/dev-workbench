# DevWorkbench Hướng dẫn phát triển, cập nhật và xuất bản plug-in

Tài liệu này dành cho hai loại người:

- Người bảo trì muốn hiểu cơ chế plug-in hiện tại hoạt động như thế nào
- Các nhà phát triển đang phát triển, cập nhật và phát hành các phần bổ trợ

Nếu bạn chỉ quan tâm đến một câu, bạn có thể nhớ câu này trước 4 Điểm:

1. Các plugin là riêng biệt JAR，Không buộc phiên bản tóc cùng với chủ nhà
2. Nguồn thông tin chính xác duy nhất cho siêu dữ liệu plugin là của riêng mỗi plugin `pom.xml`
3. GitHub Actions Chỉ chịu trách nhiệm đọc sản phẩm, xác minh tính nhất quán, xuất bản và viết lại catalog
4. Ưu tiên xác minh địa phương“Xây dựng plugin JAR -> Cài đặt quản lý plug-in -> Khởi động lại xác minh”con đường này

Nếu bạn muốn xem nó bây giờ runtime Triển khai hạt nhân chứ không phải phát triển/Đối với quá trình phát hành, bạn có thể đọc trực tiếp: .

- [PLUGIN_RUNTIME_ARCHITECTURE_zh.md](./PLUGIN_RUNTIME_ARCHITECTURE_zh.md)

## 1. Phân lớp thành phần và nguyên tắc

Hệ thống plug-in hiện tại được chia thành 7 lớp:

```text
dev-workbench-parent
├── dev-workbench-plugin-api
├── dev-workbench-foundation
├── dev-workbench-platform
├── dev-workbench-ui
├── dev-workbench-plugin-runtime
├── dev-workbench-app
└── dev-workbench-plugins
    ├── plugin-manager
    ├── plugin-client-cert
    ├── plugin-capture
    ├── plugin-redis
    ├── plugin-kafka
    └── plugin-decompiler
```

### 1.1 Mỗi lớp chịu trách nhiệm gì?

- `dev-workbench-plugin-api`
  - Tính ổn định của plugin tiếp xúc với máy chủ SPI
  - Chứa `DevWorkbenchPlugin`、`PluginContext`、`PluginDescriptor`
  - Hợp đồng dịch vụ plugin cũng được đặt ở đây chẳng hạn `GitPluginService`、`ClientCertificatePluginService`、`RequestCollectionImportService`
  - Lớp đầu vào của plug-in mới phải triển khai giao diện ở đây
- `dev-workbench-foundation`
  - Các công cụ và thông báo cơ bản được chia sẻ bởi máy chủ và trình cắm key、Mô hình đại chúng
  - Plug-in cần được chia sẻ với máy chủ UI DTO、enum、Các hằng số, đường dẫn,JSON、Hệ thống, công cụ thiết lập hoặc phân tích cú pháp chung/Khi định dạng các công cụ, hãy đặt chúng ở đây trước tiên
- `dev-workbench-platform`
  - Khả năng của khung nền tảng máy chủ, ví dụ: IOC và cập nhật lõi khám phá
  - Không phải là một plug-in thông thường SPI；Các plugin vẫn phải ưu tiên cho các phần phụ thuộc khi chúng cần mở rộng máy chủ `dev-workbench-plugin-api`
- `dev-workbench-ui`
  - Phổ biến cho các plug-in và máy chủ UI Các thành phần cơ bản và hằng số trực quan
  - Bảng plug-in có thể được tái sử dụng và thống nhất nếu cần UI Kiểu, nút, hộp tìm kiếm, bảng hoặc điều khiển nhập cơ bản trước tiên dựa vào lớp này
- `dev-workbench-plugin-runtime`
  - Chịu trách nhiệm quét và đọc descriptor、Xác minh tính tương thích của phiên bản, tải lớp, gọi vòng đời, đăng ký điểm mở rộng và registry
  - Nó có thể được hiểu là kernel thời gian chạy plug-in
- `dev-workbench-app`
  - Bản thân ứng dụng máy chủ
  - Từ runtime và registry Khả năng đăng ký plug-in tiêu dùng
- `dev-workbench-plugins`
  - Thông thường các plugin chính thức JAR tập trung
  - `plugin-client-cert`、`plugin-capture`、`plugin-redis`、`plugin-kafka`、`plugin-decompiler` Loại điều này `plugin-*` Nó là một plug-in thời gian chạy phổ biến
  - `plugin-manager` Ngoại lệ là: đó là máy chủ app Chịu trách nhiệm về mô-đun phụ trợ quản lý plug-in phía máy chủ phụ thuộc trực tiếp catalog Phân tích, trực tuyến/Không có mặt tiền cài đặt ngoại tuyến và bản ghi nguồn cài đặt runtime Quét các plugin phổ biến đã tải

### 1.2 Cách máy chủ tải trình cắm

Liên kết chạy thực tế là khoảng:

```text
địa phương JAR / Điều khiển từ xa catalog
  -> plugin-manager Chịu trách nhiệm cài đặt và tải xuống
  -> Phiên bản đã cài đặt của plug-in JAR Nhập thư mục dữ liệu plugins/installed và plugins/packages
  -> Phiên bản di động được ghi vào thư mục gốc của chương trình theo mặc định plugins và plugins/packages，Thuận tiện cho việc sao chép ngoại tuyến
  -> runtime Quét thư mục plugin
  -> Đọc META-INF/dev-workbench/*.properties
  -> phân tích PluginDescriptor
  -> Kiểm tra app Phiên bản + platform Khả năng tương thích của phiên bản
  -> Tạo các plugin riêng biệt cho từng plugin URLClassLoader
  -> Sự tạo phản xạ entryClass
  -> gọi onLoad(context)
  -> Hướng cắm registry Đăng ký kịch bản API / Toolbox / Hoàn thành / Snippet / Service
  -> Chủ nhà tiêu thụ những khả năng này
```

Những điểm chính:

- Các plug-in không phụ thuộc trực tiếp vào việc triển khai nội bộ của máy chủ mà thông qua `PluginContext` Khai báo khả năng
- Các trình nạp lớp không được chia sẻ giữa các plug-in để tránh xung đột phụ thuộc làm ảnh hưởng đến máy chủ
- `dev-workbench-plugin-runtime` Chịu trách nhiệm quét, tải plugin, vòng đời và registry；`plugin-manager` Chỉ chịu trách nhiệm cài đặt/catalog Khả năng phụ trợ của chủ nhà bên
- Không được dựa vào các plug-in thông thường `dev-workbench-app`；chủ nhà app Có thể dựa vào `dev-workbench-plugins/plugin-manager` Thư mục này là một trường hợp đặc biệt
- Giống nhau `plugin.id` Khi có nhiều phiên bản, chỉ phiên bản cao nhất mới được chọn để tải khi chạy
- Việc chặn khả năng tương thích sẽ được thực hiện trong giai đoạn cài đặt để tránh điều đó“Cài đặt thành công nhưng thời gian chạy bị bỏ qua”trạng thái thành công sai

### 1.3 Plug-in có thể mở rộng những khả năng nào?

hiện tại `PluginContext` Hỗ trợ các loại điểm mở rộng này:

- `registerScriptApi`
  - Tiếp xúc với môi trường kịch bản `pm.plugin("xxx")`
- `registerService`
  - Nhận dịch vụ theo loại từ lớp truy cập phía máy chủ
- `registerToolboxContribution`
  - Đi Toolbox Thêm bảng
- `registerScriptCompletionContributor`
  - Thêm tính năng tự động hoàn thành vào trình chỉnh sửa tập lệnh
- `registerSnippet`
  - Thêm đoạn mã script

qua `plugin-kafka` Ví dụ,`KafkaPlugin` Vào `onLoad` Đăng ký một lần:

- `pm.plugin("kafka")`
- Kafka Toolbox bảng điều khiển
- Kafka Hoàn thành tập lệnh liên quan
- Kafka Đoạn mẫu

Nói cách khác, đó không phải là trách nhiệm của lớp đầu vào plug-in“Tự mình chạy logic kinh doanh”，Đúng hơn“Đính kèm khả năng vào máy chủ khi tải”。

## 2. Mô hình phiên bản và khả năng tương thích

### 2.1 Tại sao chúng ta phải phân biệt app và platform

Có hai ranh giới phiên bản trong hệ thống hiện tại: .

- `app version`
  - chủ nhà DevWorkbench phiên bản phát hành
  - Ví dụ `5.3.16`
- `plugin platform version`
  - plugin SPI / Các phiên bản ranh giới được tập hợp trong thời gian chạy
  - Hiện tại bằng root `pom.xml` bên trong `plugin.platform.version` quản lý

Hiểu đơn giản:

- `platform` Ống“Giao diện plug-in này vẫn có thể được kết nối chứ?”
- `app` Ống“Máy chủ hiện tại có các chức năng cụ thể mà plug-in phụ thuộc vào không?”

Để giải thích thêm một chút:

- gốc `pom.xml` bên trong `revision`
  - Là phiên bản phân phối máy chủ
  - Nó sẽ tiếp tục tăng với các bản phát hành, bản vá lỗi thông thường và các thay đổi không phải plugin của máy chủ
- plugin `pom.xml` bên trong `host.version`
  - Theo mặc định `${revision}`
  - Hiệu quả là duy nhất“Phiên bản nào của gói nền tảng máy chủ phụ thuộc vào thời điểm biên dịch”
- gốc `pom.xml` bên trong `plugin.platform.version`
  - Là phiên bản nền tảng plug-in thời gian chạy
  - Chức năng là biểu thức“hiện tại runtime / SPI Có bất kỳ thay đổi không tương thích nào đối với ranh giới lắp ráp không?”

Tại sao `plugin.min/maxPlatformVersion` Nó không thể được viết trực tiếp `${revision}`：

- `revision` Thay đổi quá thường xuyên, nhiều biến thể và plugin SPI Không có gì để làm
- Nếu đặt platform Phạm vi tương thích được gắn trực tiếp với `revision`，Nhiều plug-in cũ thực sự có thể tải được sẽ bị đánh giá sai“Không tương thích”
- Vì vậy nó phải được đặt“Nhịp điệu giải phóng máy chủ”và“Ranh giới tương thích nền tảng plug-in”Khái niệm này được chia thành hai phiên bản và được duy trì riêng biệt

### 2.2 Khi nào cần thay đổi platform version

Chỉ có các ranh giới tập hợp trình cắm thêm hoặc các nền tảng chung mà các trình cắm thêm có thể phụ thuộc vào API Khuyến mãi chỉ nên diễn ra khi xảy ra những thay đổi không tương thích `plugin.platform.version`，Ví dụ:

- `PluginContext` Có những sửa đổi không tương thích
- `PluginDescriptor` Semantics undergo disruptive changes
- runtime Phương pháp lắp ráp đã thay đổi và trình cắm cũ sẽ không hợp lệ
- Một số loại hợp đồng điểm gia hạn được thay thế
- Trình cắm bắt đầu bằng  `provided` Các mô-đun nền tảng phụ thuộc vào chế độ (ví dụ: `dev-workbench-plugin-api`、`dev-workbench-foundation`、`dev-workbench-ui`）Xóa hoặc đổi tên lớp công khai/phương pháp này, plugin cũ sẽ xuất hiện `NoClassDefFoundError`、`NoSuchMethodError` hoặc khác linkage sai lầm

Nếu chỉ:

- Chủ nhà trồng một cây bình thường bug
- Máy chủ đã phát hành một phiên bản vá lỗi
- Một plug-in thêm chức năng mới của riêng nó

Thông thường thì không nên bump `plugin.platform.version`。

### 2.3 Khi nào cần thêm min/maxAppVersion

Chỉ khi plug-in thực sự phụ thuộc vào khả năng nhất định của máy chủ thì mới nên thêm:

- Máy chủ đã thêm một plug-in mới để gọi API/service
- Tiếp đón ai đó UI Vùng chứa, cổng hoặc sự kiện chỉ tồn tại trong các phiên bản mới
- Plugin rõ ràng không hỗ trợ phiên bản máy chủ cũ hơn

Điều này thường chỉ được viết nếu plugin chỉ là một tiện ích mở rộng thông thường `min/maxPlatformVersion` Thế là đủ rồi.

### 2.4 hiện tại descriptor và catalog mối quan hệ

Các quy định hiện hành là:

- Hãy tự mình cắm vào `pom.xml` Duy trì siêu dữ liệu
- Maven Tạo bộ lọc `META-INF/dev-workbench/*.properties`
- GitHub Actions Từ xây dựng đến tốt JAR Đọc bên trong descriptor
- workflow Lại descriptor Nội dung được đồng bộ trở lại `catalog.json`

Vậy thứ tự thực sự là:

```text
pom.xml -> descriptor -> release asset -> catalog
```

KHÔNG:

```text
workflow Lĩnh vực chữ viết tay -> catalog -> Sau đó đẩy plug-in trở lại
```

Điều này tránh bị trôi do trám nhiều lần bằng tay.

## 3. Cách phát triển một plug-in mới

Con đường ngắn nhất được đưa ra dưới đây dựa trên thực tiễn thực tế của kho hiện tại.

### 3.1 Tạo một mô-đun mới

Các plug-in chính thức chung được khuyến nghị `dev-workbench-plugins/` Tạo mới dưới:

```text
dev-workbench-plugins/plugin-xxx
├── pom.xml
├── src/main/java
└── src/main/resources/META-INF/dev-workbench/plugin-xxx.properties
```

Sau đó thêm mô-đun vào bản dựng tổng hợp.

Đừng đặt nó `dev-workbench-plugins/plugin-manager` Hãy coi nó như một mẫu plug-in bình thường. Nó ở lại `dev-workbench-plugins` Sau đây là lộ trình lịch sử và lựa chọn tổ chức phát hành; thư viện quản lý plug-in phía máy chủ mới không nên tiếp tục được đặt theo mặc định `dev-workbench-plugins/*`，Bạn có thể chuyển sang tên mô-đun hoặc thư mục rõ ràng hơn sau này.

### 3.2 Viết một trình cắm thêm `pom.xml`

Bạn nên tham khảo trực tiếp các plugin chính thức hiện có, chẳng hạn như: .

- `dev-workbench-plugins/plugin-capture/pom.xml`
- `dev-workbench-plugins/plugin-kafka/pom.xml`
- `dev-workbench-plugins/plugin-redis/pom.xml`

Ít nhất các trường này là bắt buộc: .

```xml
<version>5.3.16</version>
<artifactId>dev-workbench-plugin-xxx</artifactId>

<properties>
    <host.version>${revision}</host.version>
    <plugin.id>plugin-xxx</plugin.id>
    <plugin.name>XXX Plugin</plugin.name>
    <plugin.entryClass>com.nvhuong.devworkbench.plugin.xxx.XxxPlugin</plugin.entryClass>
    <plugin.description>...</plugin.description>
    <plugin.minPlatformVersion>${plugin.platform.version}</plugin.minPlatformVersion>
    <plugin.maxPlatformVersion>${plugin.platform.version}</plugin.maxPlatformVersion>
</properties>
```

Mô tả:

- `host.version`
  - Phiên bản nào của gói nền tảng máy chủ phụ thuộc vào thời điểm biên dịch
  - Plug-in chính thức tuân theo mặc định `${revision}`，Điều này có thể trực tiếp căn chỉnh dòng điện `api / foundation / ui` đầu vào biên dịch
  - Nếu chỉ có chủ nhà phát hành phiên bản bình thường thì hãy theo dõi tại đây; nó không chịu trách nhiệm thể hiện phạm vi tương thích hoạt động
- `plugin.id`
  - Chỉ có trình cắm ID
  - Xuất bản, cài đặt và nâng cấp đều dựa vào nó để nhận dạng
- `plugin.entryClass`
  - Lớp đầu vào plug-in
- `plugin.min/maxPlatformVersion`
  - Hiện tại, các plug-in chính thức đều có quyền root theo mặc định `pom.xml` của `plugin.platform.version`
  - Nó thể hiện phạm vi tương thích nền tảng plug-in thời gian chạy, không phải số phát hành máy chủ
  - Chỉ nên thực hiện điều chỉnh nếu có những thay đổi đột phá đối với chính cơ chế plugin

Nếu bạn tìm ra“Phiên bản không phù hợp”，Xét xử theo nội dung sau:

- Nó không biên dịch được vì plug-in phụ thuộc vào máy chủ mới `api / foundation / ui`
  - Điều chỉnh hoặc làm theo `host.version`
- Trong thời gian chạy, máy chủ cũ thiếu các tính năng mới mà các plug-in phụ thuộc vào
  - Thêm hoặc điều chỉnh `plugin.minAppVersion`
- runtime / SPI Nếu hợp đồng thay đổi, plug-in cũ sẽ không cài đặt được
  - Nhấc rễ trước `pom.xml` của `plugin.platform.version`
  - Điều chỉnh lại plug-in `plugin.min/maxPlatformVersion`
- Chỉ là plug-in đó đã thay đổi chức năng hoặc đã được sửa chữa bug
  - Thông thường chỉ có bản thân plug-in được sửa đổi `<version>`

Nếu trình cắm phụ thuộc vào chức năng dành riêng cho máy chủ, bạn có thể thêm bổ sung:

```xml
<plugin.minAppVersion>5.3.16</plugin.minAppVersion>
<plugin.maxAppVersion>5.4.0</plugin.maxAppVersion>
```

Nhưng hãy lưu ý rằng nó hiện là chính thức descriptor Theo mặc định, mẫu không bao gồm hai trường này; bạn cần cập nhật chúng cùng một lúc descriptor bản mẫu.

### 3.3 Viết descriptor bản mẫu

Ví dụ:

```properties
plugin.id=${plugin.id}
plugin.name=${plugin.name}
plugin.version=${project.version}
plugin.entryClass=${plugin.entryClass}
plugin.description=${plugin.description}
plugin.homepage=${plugin.homepage}
plugin.minPlatformVersion=${plugin.minPlatformVersion}
plugin.maxPlatformVersion=${plugin.maxPlatformVersion}
```

Nếu bạn muốn sử dụng `minAppVersion` / `maxAppVersion`，Hãy nhớ thêm chúng vào mẫu.

Nguyên tắc thiết kế hiện nay là:

- descriptor là một phần của sản phẩm xây dựng
- descriptor Giá trị thực sự của đến từ  `pom.xml`
- Đừng ở đó descriptor Phiên bản và phạm vi tương thích được lập trình thủ công

### 3.4 Viết lớp đầu vào

Lớp đầu vào phải được thực hiện `DevWorkbenchPlugin`：

```java
public class XxxPlugin implements DevWorkbenchPlugin {

    @Override
    public void onLoad(PluginContext context) {
        // Đăng ký điểm mở rộng
    }
}
```

Các mẫu phổ biến:

```java
context.registerScriptApi("xxx", ScriptXxxApi::new);
context.registerToolboxContribution(...);
context.registerScriptCompletionContributor(...);
context.registerSnippet(...);
```

Khuyến nghị:

- `onLoad`
  - Chỉ đăng ký được thực hiện, không có việc khởi tạo nặng nào được thực hiện
- `onStart`
  - Đặt logic thực sự cần được thực thi khi khởi động
- `onStop`
  - Thực hiện giải phóng tài nguyên

### 3.5 Cách chọn phụ thuộc

Quy định ưu tiên:

- Phụ thuộc hợp đồng plug-in `dev-workbench-plugin-api`
- Chia sẻ không UI Các loại cơ sở và phụ thuộc công cụ `dev-workbench-foundation`
- Chia sẻ Swing Thành phần, phông chữ, biểu tượng, màu sắc và thông báo UI phụ thuộc `dev-workbench-ui`
- Đừng dựa trực tiếp vào sự phụ thuộc ngược lại `dev-workbench-app` Lớp bên trong
- Nếu chính plugin đó cần thư viện của bên thứ ba thì thư viện đó sẽ được bao gồm trong plugin cùng với plugin JAR
- Các phần phụ thuộc đã được máy chủ cung cấp và phần bổ trợ chỉ cần được tham chiếu trong quá trình biên dịch sẽ được sử dụng trước tiên `provided`

### 3.6 Phát triển plug-in mới checklist

Nên làm theo danh sách này:

1. Mới `plugin-xxx` mô-đun
2. Kết hợp tốt `pom.xml`
3. Viết descriptor bản mẫu
4. Viết lớp đầu vào và thực hiện nó `DevWorkbenchPlugin`
5. Đăng ký các điểm mở rộng cần thiết
6. Được đóng gói và tạo ra tại địa phương JAR
7. Xác minh cài đặt cục bộ
8. Thử nghiệm bổ sung
9. Đi lần nữa GitHub giải phóng

## 4. Cách cập nhật plugin hiện có

Cập nhật các plug-in hiện có, hãy trả lời phần này trước 3 Câu hỏi:

1. Lần này là bản cập nhật chức năng của chính trình cắm hoặc thay đổi về ranh giới tương thích nền tảng
2. Không cần phải nâng cấp phiên bản plug-in
3. Có cần phải điều chỉnh không? app/platform Phạm vi tương thích

### 4.1 Các bản cập nhật phổ biến nhất: Cập nhật tính năng plug-in

Nếu chỉ:

- Thêm một kịch bản mới API phương pháp
- Thêm một cái mới Toolbox Chức năng
- Tự sửa chữa plug-in bug
- Tự mình tinh chỉnh plugin UI

Thông thường chỉ:

1. Ôn lại `plugin-xxx` mã
2. Quảng cáo chính plugin đó `<version>`
3. Xây dựng lại và xác nhận

Nói chung là không cần thay đổi:

- chủ nhà `revision`
- `plugin.platform.version`

### 4.2 Các bản cập nhật yêu cầu điều chỉnh phạm vi tương thích

Nếu plugin dựa vào khả năng lưu trữ mới: .

1. trong phần bổ trợ `pom.xml` Thêm hoặc điều chỉnh `plugin.minAppVersion`
2. Vào descriptor Đã thêm đồng bộ vào mẫu `plugin.minAppVersion`
3. Xác minh cục bộ rằng phiên bản cũ của máy chủ thực sự không còn phù hợp

Nếu ranh giới nền tảng plug-in đã thay đổi (SPI、runtime Các phương pháp lắp ráp hoặc phần bổ trợ có thể dựa vào khả năng hiển thị của các mô-đun nền tảng API bị phá hủy):

1. Nhấc rễ trước `pom.xml` bên trong `plugin.platform.version`
2. Sau đó cập nhật các plugin bị ảnh hưởng `plugin.min/maxPlatformVersion`
3. Xác minh rằng các plugin cũ được đánh giá chính xác là không tương thích

Đừng thay đổi nó vì những tình huống sau đây `plugin.platform.version`：

- chủ nhà `revision` Từ `5.3.16` tăng lên `5.3.17`
- Ký chủ đã đạt được trạng thái tu luyện bình thường bug，Nhưng plugin SPI Không có gì thay đổi
- Một trình cắm thêm sẽ thêm bảng công cụ hoặc tập lệnh mới của riêng nó API

### 4.3 Thứ tự đề xuất để cập nhật các plugin hiện có

```text
Thay đổi mã
  -> Quyết định xem bump Phiên bản plugin
  -> Quyết định có nên sửa đổi hay không min/maxAppVersion
  -> Quyết định có nên sửa đổi hay không min/maxPlatformVersion
  -> Xây dựng tại địa phương
  -> Xác minh cài đặt cục bộ
  -> dry run giải phóng
  -> Chính thức phát hành
```

## 5. Vào GitHub Phải làm gì khi xuất bản một plug-in

Plug-in chính thức hiện tại được phát hành độc lập tại:

- `.github/workflows/plugin-release.yml`

### 5.1 Bạn cần thay đổi điều gì trước khi phát hành chính thức?

Đảm bảo ít nhất những điều sau:

1. plugin mục tiêu `pom.xml` Phiên bản đã được nâng cấp
2. descriptor Các trường mẫu đã hoàn tất
3. Thẻ xây dựng địa phương
4. Xác minh địa phương đã được thông qua
5. Nếu có liên quan đến những thay đổi về khả năng tương thích, .`min/maxAppVersion` hoặc `min/maxPlatformVersion` Đã cập nhật chính xác

### 5.2 workflow Nó sẽ làm gì

Cái này workflow Hiện đang làm:

1. Chỉ xây dựng các mô-đun plugin bạn đã chọn
2. Đầu ra plugin duy nhất JAR
3. Tính toán JAR của `sha256`
4. Từ JAR bên trong descriptor Đọc siêu dữ liệu plugin
5. Kiểm tra `plugin id / version / tag` tính nhất quán
6. giải phóng GitHub Release
7. Tùy chọn xuất bản Gitee Release
8. Cập nhật tùy chọn GitHub / Gitee catalog

### 5.3 workflow những ràng buộc chính

Phiên bản chính thức hiện tại có các quy tắc sau:

- Bản phát hành chính thức chỉ có thể được kích hoạt từ nhánh mặc định
- Không `dry_run` trường hợp, chính thức GitHub catalog phải được cập nhật cùng nhau
- workflow Không còn tự mình duy trì các trường tương thích nữa mà từ  JAR descriptor Đọc

### 5.4 Các bước xuất bản được đề xuất

Nên làm theo thứ tự này mọi lúc:

1. Mã và phiên bản đã được thay đổi cục bộ
2. Xây dựng và xác minh tại địa phương
3. Vào GitHub Actions Kích hoạt thủ công `plugin-release.yml`
4. Chạy một lần đầu tiên `dry_run=true`
5. Xác nhận sản phẩm.descriptor、Tất cả các phiên bản đều đúng
6. Chạy lại để phát hành chính thức

### 5.5 workflow_dispatch Cách chọn tham số

Bạn thường chỉ cần quan tâm đến những đầu vào này: .

- `plugin`
  - Chọn plugin để xuất bản, ví dụ `capture`、`kafka`
- `dry_run`
  - Đề nghị đầu tiên `true`
- `publish_gitee_release`
  - Có đồng bộ hóa hay không Gitee Release
- `update_github_catalog`
  - Có nên viết lại không GitHub catalog
- `update_gitee_catalog`
  - Có nên viết lại không Gitee catalog
- `release_notes`
  - Không bắt buộc

### 5.6 Những gì bạn nhận được khi xuất bản

Sau khi phát hành chính thức, bạn thường nhận được:

- Một plugin duy nhất JAR Tệp đính kèm
- một `.sha256.txt` tập tin
- Một miếng GitHub Release
- Đã cập nhật `plugin-catalog/catalog-github.json`
- Nếu bật Gitee，Đồng bộ hóa là tương ứng Gitee Release và catalog đổi mới

## 6. Cách xác minh cục bộ

Xác minh địa phương được khuyến khích 4 Lớp, không chỉ biên dịch và chuyển.

### 6.1 Cấp độ 1: Xây dựng đã thông qua

Xây dựng một plugin duy nhất: .

```bash
mvn -pl dev-workbench-plugins/plugin-kafka -am clean package -DskipTests
```

Xây dựng nhiều plugin:

```bash
mvn -pl dev-workbench-plugins/plugin-kafka,dev-workbench-plugins/plugin-redis -am clean package -DskipTests
```

### 6.2 Cấp độ 2: Kiểm tra descriptor Có đúng không?

Kiểm tra ít nhất hai nơi:

1. Xây dựng thư mục đầu ra

```text
dev-workbench-plugins/plugin-kafka/target/classes/META-INF/dev-workbench/plugin-kafka.properties
```

2. cuối cùng JAR bên trong descriptor

Bạn cần xác nhận:

- `plugin.id`
- `plugin.version`
- `plugin.entryClass`
- `plugin.description`
- `plugin.min/maxPlatformVersion`
- Nếu cần thiết,`plugin.min/maxAppVersion`

Mục tiêu của lớp này là xác định:

- `pom.xml` -> descriptor bản mẫu -> Xây dựng sản phẩm
  Không trôi dạt

### 6.3 Cấp độ thứ ba: xác minh cài đặt

Phương thức xác thực cục bộ được khuyên dùng nhất:

1. Mở DevWorkbench
2. Nhập quản lý plug-in
3. Chọn“Cài đặt cục bộ JAR”
4. Chọn plug-in bạn vừa gõ JAR
5. Khởi động lại ứng dụng
6. Xác minh rằng khả năng của trình cắm thêm thực sự xuất hiện

Theo loại plugin, hãy kiểm tra ít nhất: .

- Toolbox Liệu bảng điều khiển có xuất hiện hay không
- `pm.xxx` có sẵn
- Việc hoàn thành tập lệnh có xuất hiện hay không
- Snippet xuất hiện
- Tàn tật / Kích hoạt / Trạng thái có đúng sau khi gỡ cài đặt không?

### 6.4 Cấp độ 4: Xác minh tính tương thích

Nếu thay đổi này liên quan đến phạm vi tương thích, bạn nên xác minh cụ thể:

- Liệu các plug-in không tương thích có bị chặn trong giai đoạn cài đặt hay không
- Liệu các plug-in tương thích có thể được cài đặt và tải bình thường hay không
- Liệu bản sao tương thích của giao diện quản lý plug-in có đáp ứng được mong đợi hay không
- Nút cài đặt trong danh sách thị trường đã được tắt hoặc cho phép đúng cách

### 6.5 Tùy chọn: Quét thư mục trong quá trình phát triển

Một thư mục quét giai đoạn phát triển bổ sung hiện được hỗ trợ: .

- `-DdevWorkbench.plugins.dir=/your/path`

Nó phù hợp cho:

- Gỡ lỗi thư mục plugin bổ sung
- Xác minh thư mục plug-in cài sẵn của doanh nghiệp

Nhưng đối với việc phát triển một plug-in duy nhất, vẫn là con đường xác minh ổn định nhất“Xây dựng JAR Sau đó cài đặt nó thông qua quản lý plug-in”。

## 7. Phương pháp cài đặt và phân phối của người dùng

Mặc dù tài liệu này thiên về nhà phát triển hơn nhưng tốt nhất người dùng nên có hiểu biết thống nhất về phương pháp cài đặt.

Hiện chỉ dành riêng cho người dùng 2 đường dẫn:

1. Cài đặt cục bộ
   - Cài đặt trực tiếp các plugin riêng lẻ JAR
2. Cài đặt trực tuyến
   - thông qua điều khiển từ xa `catalog.json` Tải và cài đặt

Hiện tại chính thức catalog：

- GitHub
  - `https://raw.githubusercontent.com/nvhuong/dev-workbench/main/plugin-catalog/catalog-github.json`

### 7.1 Tại sao cả địa phương và trực tuyến vẫn còn

Bởi vì cả hai giải quyết các vấn đề khác nhau:

- địa phương JAR
  - Thích hợp cho việc gỡ lỗi chung trong quá trình phát triển, phân phối thủ công và cài đặt với chi phí tinh thần thấp nhất
- trực tuyến catalog
  - Thích hợp để bảo trì dài hạn, phân phối chính thức và nguồn plug-in mạng nội bộ của nhóm

### 7.2 Ý nghĩa của cấu trúc thư mục hiện tại

Theo mặc định, bạn sẽ thấy hai thư mục trên máy của người dùng: .

- `DevWorkbench/plugins/installed/`
- `DevWorkbench/plugins/packages/`

Ý nghĩa:

- `installed/`
  - Bản sao hiện đang được sử dụng để tải thực tế
- `packages/`
  - Giữ một bản sao của gói
  - Bộ đệm để cài đặt lại, nâng cấp và các tình huống khóa tệp nhất định

## 8. Câu hỏi thường gặp

### Q1：Các tệp tối thiểu cần ghi khi phát triển một trình cắm thêm là gì?

Ít nhất là những điều này:

1. `plugin-xxx/pom.xml`
2. `src/main/java/.../XxxPlugin.java`
3. `src/main/resources/META-INF/dev-workbench/plugin-xxx.properties`

Nếu plugin có nó UI hoặc kịch bản API，Thêm các danh mục tương ứng.

### Q2：Tôi có cần thay đổi phiên bản máy chủ để cập nhật plug-in không?

Không nhất thiết, thường không được sử dụng.

Trong hầu hết các trường hợp:

- Chỉ thay đổi mã plug-in
- Chỉ cải thiện plugin của riêng mình `<version>`

Những thay đổi chỉ cần thiết khi bản thân cơ chế plug-in trải qua những thay đổi không tương thích `plugin.platform.version`。

### Q3：Tại sao GitHub Actions Các trường tương thích không được duy trì trực tiếp

Bởi vì định nghĩa tương thích phải thuộc về chính plugin chứ không phải  workflow。

Luồng dữ liệu chính xác hiện tại là: .

- plugin `pom.xml` Xác định siêu dữ liệu
- Maven phát ra descriptor
- workflow Từ JAR descriptor Đọc và xác minh
- workflow đổi mới catalog

Điều này làm cho nó ít có khả năng xảy ra nhiều hiện tượng trôi dạt bằng tay.

### Q4：Tại sao không nên sao chép trực tiếp các tệp để xác minh cục bộ?

Vì đường dẫn quản lý plug-in sẽ được xác minh bằng cách:

- JAR Nó có hợp lệ không?
- descriptor Nó có thể đọc được không
- Liệu khả năng tương thích có được đáp ứng hay không
- Nguồn cài đặt có được ghi lại hay không

Điều này gần với đường dẫn người dùng thực hơn là sao chép thủ công.

### Q5：Tôi nên mô tả hệ thống plug-in này như thế nào

Lời đề nghị được thống nhất thành một câu:

> DevWorkbench Plug-in này là duy nhất JAR Đối với các đơn vị giao hàng, thông qua các plugin ổn định API、Thời gian chạy plugin độc lập và từ xa catalog cơ chế để đạt được sự phát triển độc lập, nâng cấp độc lập và phát hành độc lập.
