# EasyPostman plugin Runtime Mô tả kiến ​​trúc

Tài liệu này tập trung vào `easy-postman-plugin-runtime` Cấu trúc triển khai sau khi tái cấu trúc hiện tại.

Nó không lặp lại chính nó“Cách phát triển plug-in”hoặc“Cách xuất bản plugin”，Thay vào đó hãy trả lời những câu hỏi sau:

- runtime Những khối trách nhiệm nào bây giờ được chia thành
- Liên kết thực sự từ cài đặt đến tải plugin là gì
- Cách máy chủ sử dụng đóng góp của plugin
- Chính sách xung đột hiện tại và phạm vi kiểm tra là gì?
- Khi chúng tôi tiếp tục phát triển sau này, chúng tôi sẽ ưu tiên thay đổi ở đâu

Để biết các tài liệu tổng quan có liên quan, xem:

- [PLUGINS_zh.md](./PLUGINS_zh.md)

## 1. cấu trúc hiện tại

hiện tại runtime Các lớp chính có liên quan có thể được hiểu theo trách nhiệm của chúng:

```text
easy-postman-plugin-runtime
├── PluginRuntime
├── PluginScanner
├── PluginCandidateResolver
├── PluginLoader
├── PluginRegistry
├── PluginStateStore
├── PluginPlatformSettingsStore
├── PluginRuntimePaths
├── RuntimeVersionResolver
├── PluginCompatibility
├── PluginFileInfo
└── PluginVersionComparator
```

### 1.1 Mỗi lớp chịu trách nhiệm về việc gì

- `PluginRuntime`
  - Mục nhập điều phối tổng thời gian chạy
  - Phối hợp khởi động, tắt máy, lựa chọn ứng viên và kết thúc vòng đời
  - tiếp xúc với thế giới bên ngoài `initialize()`、`shutdown()`、`getInstalledPlugins()` Chờ nhập tĩnh
- `PluginScanner`
  - Quét thư mục plugin
  - Đọc plugin jar bên trong descriptor
  - Tạo lớp đĩa `PluginFileInfo`
- `PluginCandidateResolver`
  - Chọn các plugin thực sự cần được tải từ kết quả quét
  - Xử lý bị vô hiệu hóa, đang chờ gỡ cài đặt, lọc khả năng tương thích và đồng bộ hóa `plugin.id` Chọn tốt nhất trong số nhiều phiên bản
- `PluginLoader`
  - Tạo `URLClassLoader`
  - Sự phản chiếu khởi tạo lớp mục nhập trình cắm thêm
  - gọi `onLoad()` / `onStart()` / `onStop()`
  - Bó `PluginContext` Đã nhận được hành động đăng ký `PluginRegistry`
- `PluginRegistry`
  - Lưu tập lệnh được plug-in đăng ký API、dịch vụ,Toolbox、Hoàn thành,Snippet
  - Thời gian chạy máy chủ sử dụng các khả năng của trình cắm từ đây
- `PluginStateStore`
  - Duy trì trạng thái của các plugin bị vô hiệu hóa và các plugin sẽ được gỡ cài đặt
- `PluginPlatformSettingsStore`
  - Tầng trệt JSON Kiên trì
- `PluginRuntimePaths`
  - Hợp nhất thư mục dữ liệu thời gian chạy, thư mục cài đặt và thư mục bộ đệm gói
- `RuntimeVersionResolver`
  - phân tích app version và plugin platform version
  - Trạng thái đóng gói đọc tài nguyên trước tiên và trạng thái phát triển quay lại đọc tài nguyên `pom.xml`

## 2. Liên kết tải thực sự

Liên kết tải plug-in hiện tại có thể được đơn giản hóa thành:

```text
PluginManager / Đặt tại địa phương jar
  -> plugins/installed và plugins/packages
  -> StartupCoordinator.prepareMainFrame()
  -> PluginRuntime.initialize()
  -> cleanupPendingUninstallPlugins()
  -> PluginScanner.resolvePluginDirs()
  -> PluginScanner.listPluginsFromDirectory()
  -> PluginCandidateResolver.resolveLoadCandidates()
  -> PluginLoader.loadPluginJar()
  -> plugin.onLoad(context)
  -> PluginRegistry Thu thập các khoản đóng góp
  -> PluginLoader.startPlugins()
  -> chủ nhà UI / kịch bản / plugin.host Đóng góp tiêu thụ của lớp truy cập
```

Dưới đây là một số điểm thiết kế chính: .

- Cài đặt và tải được tách biệt
  - `easy-postman-plugin-runtime` Chỉ chịu trách nhiệm quét.descriptor Phân tích,classloader、registry、vòng đời và“Ai sẽ được tải bởi quy trình hiện tại”
  - `easy-postman-plugins/*` Thông thường các plugin chính thức JAR，Bởi runtime Nhấn descriptor trọng tải
  - `easy-postman-plugins/plugin-manager` Nó là một mô-đun phụ trợ quản lý plug-in ở phía máy chủ, máy chủ app Bạn có thể dựa vào nó trực tiếp catalog Phân tích, tải xuống, tải xuống, xác minh và cài đặt các bản ghi nguồn
  - `plugin-manager` Không bằng runtime Không nên sử dụng plugin thông thường đã tải làm mẫu thư mục mặc định cho các plugin thông thường mới hoặc thư viện quản lý plugin phía máy chủ mới; nó vẫn còn ở trong `easy-postman-plugins` Sau đây chỉ là con đường lịch sử và lựa chọn tổ chức phát hành
- Quá trình quét và lựa chọn được tách biệt
  - `PluginScanner` Chịu trách nhiệm“Xem cái gì đó”
  - `PluginCandidateResolver` Chịu trách nhiệm“Cuối cùng chọn ai”
- Đăng ký và tiêu thụ được tách ra
  - Chỉ có plugin `onLoad()` tuyên bố khả năng
  - Chủ nhà sẽ quay lại sau `PluginRegistry` Đọc thống nhất

## 3. Liên kết tiêu thụ máy chủ

chủ nhà app Các lớp không còn được kết nối trực tiếp ở mọi nơi `PluginRuntime.getRegistry()`，Thay vào đó, tất cả đều kết thúc bằng:

- [PluginAccess.java](../easy-postman-app/src/main/java/com/laker/postman/plugin/host/PluginAccess.java)

Mối quan hệ tiêu dùng hiện nay đại khái là:

```text
PluginRegistry
  -> PluginAccess
    -> PostmanApiContext.createScriptApis()
    -> ToolboxPanel.getToolboxContributions()
    -> ScriptSnippetManager.getScriptCompletionContributors()
    -> SnippetDialog.getSnippetDefinitions()
    -> ClientCertificatePluginAccess.getService(...)
```

Giá trị của việc làm này là:

- app Các lớp không cần phải phụ thuộc vào mọi thứ `PluginRuntime`
- tương lai nếu registry hoặc runtime Để đạt được sự thay đổi,app Phạm vi thay đổi sẽ nhỏ hơn nhiều

## 4. Chế độ nhập plug-in

Hiện tại chúng tôi khuyên bạn chỉ nên sử dụng lớp mục nhập trình cắm thêm“Tuyên bố về Năng lực”，Đừng làm những việc phức tạp.

Các plug-in chính thức đã bắt đầu được thống nhất:

- `PluginContributionSupport`
- `RedisI18n` / `KafkaI18n`

Tiêu thụ phía chủ nhà được thống nhất `com.laker.postman.plugin.host.PluginAccess`。

Ví dụ `KafkaPlugin` / `RedisPlugin` của `onLoad()` Bây giờ gần hơn:

```text
registerScriptApi
registerToolbox
registerScriptCompletionContributor
registerExampleSnippet
```

Việc này tốn nhiều công sức hơn so với việc viết trực tiếp vào lớp đầu vào `ToolboxContribution` / `ShorthandCompletion` / `SnippetDefinition` dễ dàng hơn review。

## 5. Chiến lược xung đột hiện tại

### 5.1 Script API alias / service type

`PluginRegistry` Hiện tại, biện pháp bảo vệ cơ bản được cung cấp cho hai loại xung đột:

- kịch bản API alias xung đột
- service type xung đột

Chiến lược hiện tại là:

- Luôn tương thích: Đăng ký sau sẽ ghi đè đăng ký trước
- Không còn im lặng: Sẽ chiến đấu `warn` Nhật ký
- Nhật ký sẽ được mang đến owner thông tin, chỉ ra“Plugin nào ghi đè plugin nào”

Đó là, bây giờ registry Đã ghi:

- Người nào đó script API alias Nó thuộc về cái nào pluginId
- Người nào đó service type Nó thuộc về cái nào pluginId

Bước này không phải là giải pháp cuối cùng nhưng cũng đủ để khiến xung đột có thể được quan sát.

### 5.2 Những phần chưa được thực hiện

Các xung đột sau đây hiện không có sự quản lý thống nhất:

- `ToolboxContribution` của sự lặp lại `id`
- `SnippetDefinition` Tiêu đề trùng lặp hoặc xung đột ngữ nghĩa
- `ScriptCompletionContributor` Hoàn thành trùng lặp cho

Những việc này có thể được thực hiện sau.

## 6. Phạm vi kiểm tra hiện tại

runtime Các bài kiểm tra hiện nay chủ yếu ở:

- [PluginRuntimeTest.java](../easy-postman-plugin-runtime/src/test/java/com/laker/postman/plugin/runtime/PluginRuntimeTest.java)
- [PluginRegistryTest.java](../easy-postman-plugin-runtime/src/test/java/com/laker/postman/plugin/runtime/PluginRegistryTest.java)

Các hành vi chính đã được đề cập bao gồm:

- Nó sẽ được dọn sạch khi kích hoạt lại plugin pending uninstall
- Thư mục dữ liệu tùy chỉnh sẽ có hiệu lực
- plugin platform version độc lập với app version
- Các phiên bản nền tảng không tương thích sẽ bị từ chối
- pending uninstall Làm sạch tập tin
- Giống nhau `plugin.id` Khi có nhiều phiên bản thì chỉ tải phiên bản cao nhất
- Khi phiên bản mới nhất không tương thích, hãy quay lại phiên bản tương thích nhất tiếp theo
- Khi một trình cắm không tải được, các trình cắm khác sẽ tiếp tục tải
- `shutdown()` Plug-in sẽ được gọi `onStop()`
- registry Vâng alias/type Lặp lại đăng ký theo sau“Đăng ký sau ghi đè đăng ký trước”
- registry Những người đăng ký mới nhất sẽ được giữ lại owner thông tin

## 7. Hiện tại vẫn còn những hạn chế

Sau vòng tái cấu trúc này, cấu trúc đã trở nên rõ ràng hơn nhiều runtime Đây chưa phải là phiên bản cuối cùng.

Những hạn chế này vẫn còn tồn tại:

- `PluginRuntime` Vẫn là một mục toàn cầu tĩnh
  - Mặc dù việc kiểm thử có thể kiểm soát được nhưng nó vẫn chưa phải là một sự khởi tạo thực sự runtime
- registry Vâng UI Số đóng góp owner Theo dõi cấp độ
  - Hiện tại chỉ theo dõi thôi script API và service
- Việc gỡ cài đặt thực sự nóng không được hỗ trợ
  - Hiện đã được kích hoạt/Tàn tật/Gỡ cài đặt vẫn hoạt động“Nó sẽ có hiệu lực vào lần tiếp theo bạn khởi động nó”Ý tưởng chiếm ưu thế
- `PluginPlatformSettingsStore` Nó vẫn là một nền tảng plug-in key-value JSON Bưu kiện
  - Chưa typed state model

## 8. Đề xuất cho sự phát triển tiếp theo

Nếu quá trình phát triển tiếp tục, các ưu tiên được đề xuất như sau:

1. Đưa cho `ToolboxContribution` / `SnippetDefinition` / completion Cũng trang điểm owner Theo dõi
2. Bó `PluginRuntime` Bao bọc một lớp khởi tạo khác từ mục nhập tĩnh facade
3. Đưa cho `PluginStateStore` Giới thiệu typed state model，thay vì chỉ là một tập hợp các chuỗi
4. Evaluate whether to support hot start and stop without restarting
5. Sắp xếp chính sách xung đột hiện tại thành tài liệu hợp đồng hiển thị cho các nhà phát triển phần bổ trợ

## 9. Đọc gợi ý đặt hàng

Nếu bạn muốn làm theo mã để hiểu bộ hiện tại runtime，Thứ tự được đề xuất là:

1. [PluginRuntime.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginRuntime.java)
2. [PluginScanner.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginScanner.java)
3. [PluginCandidateResolver.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginCandidateResolver.java)
4. [PluginLoader.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginLoader.java)
5. [PluginRegistry.java](../easy-postman-plugin-runtime/src/main/java/com/laker/postman/plugin/runtime/PluginRegistry.java)
6. [PluginRuntimeTest.java](../easy-postman-plugin-runtime/src/test/java/com/laker/postman/plugin/runtime/PluginRuntimeTest.java)
7. [PluginRegistryTest.java](../easy-postman-plugin-runtime/src/test/java/com/laker/postman/plugin/runtime/PluginRegistryTest.java)
