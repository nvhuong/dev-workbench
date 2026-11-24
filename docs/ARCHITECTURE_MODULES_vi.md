# DevWorkbench Thông số ranh giới mô-đun

Tài liệu này xác định trách nhiệm của mô-đun và các quy tắc sắp xếp. Mục tiêu là cho phép tái cấu trúc tiếp theo và vibecoding Ưu tiên duy trì ranh giới thay vì nhồi nhét mã mới vào thư mục gần nhất.

## Sơ đồ mô-đun hiện tại

```text
dev-workbench-parent
├── dev-workbench-foundation
├── dev-workbench-request-core
├── dev-workbench-http-runtime
├── dev-workbench-collection-core
├── dev-workbench-mock-core
├── dev-workbench-plugin-api
├── dev-workbench-platform
├── dev-workbench-ui
├── dev-workbench-performance-core
├── dev-workbench-plugin-runtime
├── dev-workbench-mcp
├── dev-workbench-plugins/*
└── dev-workbench-app
```

## Trách nhiệm của mô-đun

`dev-workbench-foundation` Đúng và sai ở mức độ thấp nhất UI lớp nền. Nó có thể được chia sẻ DTO、enum、Các hằng số, đường dẫn,JSON、Công cụ hệ thống, công cụ thiết lập người dùng, cơ chế quốc tế hóa và các thông báo cơ bản key，Và có thể vượt qua các máy chủ/Các công cụ phân tích và định dạng phổ biến để tái sử dụng plugin, ví dụ: Cron、JSON Path、XML、Kích thước tệp, phần mở rộng tệp, hiển thị thời gian,HTTP header không thay đổi. Nó không thể dựa vào được Swing、Thời gian chạy plug-in, máy chủ app、Thực hiện chạy thử nghiệm căng thẳng hoặc kinh doanh UI。

`dev-workbench-request-core` Đây là lớp mô hình lõi đặc tả yêu cầu. Ví dụ: nó chứa dữ liệu cấu hình yêu cầu có thể được sử dụng lại bởi các bộ sưu tập, nhập và xuất, tập lệnh, kế hoạch kiểm tra căng thẳng, v.v `HttpRequestItem`、`SavedResponse`、`HttpHeader`、`HttpParam`、`HttpFormData`、`HttpFormUrlencoded`、`CookieInfo`、`AuthType`、`RequestAuthTypes`、`RequestBodyTypes`、`RequestItemProtocolEnum`、`TransportAuth`、`RedirectInfo`。Nó chỉ thể hiện“Yêu cầu là gì?”，Không thể dựa vào được Swing、OkHttp、app service、panel、Được gửi khi plugin được chạy hoặc cụ thể/Kết xuất thực hiện.`PreparedRequest`、`HttpResponse`、`HttpEventInfo` Là ảnh chụp nhanh trao đổi thời gian chạy, được sở hữu `dev-workbench-http-runtime` của `com.nvhuong.devworkbench.http.runtime.model`，Không thể chảy trở lại request-core hoặc app model。

`dev-workbench-http-runtime` Không có gì cả UI của HTTP Lớp thời gian chạy vận chuyển. Nó phát ra một mô hình trạng thái `PreparedRequest`、`HttpResponse`、`HttpEventInfo`，cũng như `http.runtime.transport`、`http.runtime.okhttp`、`http.runtime.file`、`http.runtime.ssl`、`http.runtime.sse`、`http.runtime.cookie`、`http.runtime.redirect`、`http.runtime.error`、`http.runtime.config`、`http.runtime.interaction`、`http.runtime.observation` Việc truyền tải được thực hiện dưới.OkHttp sự thích nghi,TLS/Giấy chứng nhận,SSE、Cookie、redirect、Ánh xạ lỗi, cài đặt thời gian chạy, cổng tương tác và cổng quan sát;`http.runtime.file` Chịu trách nhiệm nhận dạng tệp phản hồi (phát hiện tiêu đề tệp, phân tích tên tệp đã tải xuống, xác định thống nhất xem có nên xử lý tệp hay không) và được chia sẻ bởi bảng phản hồi, lưu mẫu, lịch sử và hiển thị. Mô-đun này có thể được dựa vào foundation、request-core và OkHttp，Nhưng nó không thể dựa vào được Swing/AWT、panel、app `SettingManager`、Trình truy cập máy chủ plugin hoặc IOC。Chủ nhà phải vượt qua `HttpRuntimeSettingsProvider`、`ClientCertificateProvider`、sink/dispatcher v.v. Cổng cài đặt, chứng chỉ và UI Thực hiện tương tác.

`dev-workbench-collection-core` Đây là lớp cốt lõi của trường bộ sưu tập. Nó làm cho cây thu thập và phân tích cú pháp nhập có thể tái sử dụng được UI Nhập, ví dụ `RequestGroup`、`CollectionNode`、`CollectionNodeType`、`CollectionParseResult`、Phân tích xác thực bộ sưu tập helper，cũng như DevWorkbench collection parser。Nó có thể được dựa vào `dev-workbench-foundation` và `dev-workbench-request-core`，Nhưng nó không thể dựa vào được Swing/AWT、OkHttp、app service/panel/runtime、platform、plugin-runtime、IOC Hoặc gửi cụ thể/Kết xuất thực hiện.`TreeNodeBuilder`、Cây sưu tầm UI、Dịch vụ kiên trì, nhà xuất khẩu và những người khác vẫn bị ràng buộc với chủ nhà UI/Logic của dịch vụ vẫn là đầu tiên `dev-workbench-app`。

`dev-workbench-mock-core` Nó tự lưu trữ Mock Server không có gì cả UI thời gian chạy. Nó đặt Mock sự định nghĩa,Example Ảnh chụp nhanh định tuyến,method/path/query/header/body cuộc thi đấu,JDK `HttpServer`、Ghi nhật ký cuộc gọi bị giới hạn, trạng thái phiên trong bộ nhớ, khóa truy cập được chia sẻ tùy chọn và tính trung lập `MockScriptExecutor` cổng. Nó chỉ phụ thuộc foundation và JDK，Không được giới thiệu Swing/AWT、collection/request Người mẫu,app service、GraalVM、OkHttp/Netty、platform IOC hoặc plugin runtime。Bộ sưu tập để lập bản đồ tuyến đường, tính bền vững của không gian làm việc,GraalJS Chuyển thể kịch bản, không đầu CLI và Swing Trang quản trị thuộc về `dev-workbench-app`。Có thể bị ràng buộc trong thời gian chạy loopback、Card mạng cục bộ hoặc tất cả các card mạng được sử dụng để phát triển cục bộ, chia sẻ mạng LAN và tự triển khai của người dùng; không có trách nhiệm nào được đảm nhận DevWorkbench Lưu trữ đám mây, quyền của nhóm,npm gói hoặc AI Khả năng sáng tạo.

`dev-workbench-plugin-api` Đây là lớp hợp đồng plug-in. Nó đặt các plugin SPI、Ví dụ: mô tả plugin, bối cảnh plugin, cấu trúc dữ liệu điểm mở rộng và giao diện dịch vụ plugin `GitPluginService`、`ClientCertificatePluginService`、`RequestCollectionImportService`。Plug-in và máy chủ thiết lập hợp đồng thông qua các loại ở đây.

`dev-workbench-platform` Đây là lớp khung nền tảng máy chủ. Tùy chỉnh hiện được chấp nhận IOC thùng chứa `com.nvhuong.devworkbench.ioc`，và lõi khám phá cập nhật có thể tách rời khỏi các giao diện cụ thể `com.nvhuong.devworkbench.platform.update`：So sánh phiên bản, lựa chọn nguồn phát hành, khớp nội dung gói cài đặt, ghi lại nhật ký thay đổi/Định dạng và cập nhật mô hình kết quả. Khởi động tiếp theo, trang chào mừng, trợ giúp, trung tâm cài đặt, chủ đề/Các khả năng của nền tảng như bố cục ứng dụng phông chữ, nếu nó có thể được tách biệt khỏi cụ thể app Các tấm nền và dịch vụ kinh doanh cũng được ưu tiên chuyển đến đây.

`dev-workbench-ui` Nó được chia sẻ Swing Hệ thống thiết kế. Nó đặt `FontsUtil`、`IconUtil`、`NotificationCenter`、`EditorThemeUtil`、`ModernColors`、`UiSingletonFactory`/`UiSingletonPanel`/`UiSingletonMenuBar`、`DebouncedSaveSupport`、`IRefreshable`、Nút công khai/tìm kiếm/cái bàn/Các thành phần như điều khiển đầu vào chẳng hạn `DevWorkbenchComboBox`、`DevWorkbenchJSpinner`、`DevWorkbenchPasswordField`、`EditButton`、`SaveButton`、`WrapToggleButton`，và được tham chiếu trực tiếp bởi các thành phần này icons/theme tài nguyên. Màu chính button TRÊN icon nên được sử dụng on-primary Chiến lược màu sắc, giữ màu trắng và không theo sự chuyển đổi chủ đề sáng và tối.

Quyền sở hữu tài nguyên là duy nhất owner Quản lý:`dev-workbench-ui/src/main/resources/icons` Đặt các điều khiển chung/hành động/Biểu tượng trạng thái chẳng hạn save、copy、paste、search、clear、cancel、close、delete、duplicate、eye、info、warning、arrow、chevron、wrap、start、stop、send、connect、collapse、expand、more、detail、import、export；`dev-workbench-app/src/main/resources/icons` Chỉ bao gồm doanh nghiệp lưu trữ, thương hiệu, giao thức, không gian làm việc, bài kiểm tra sức chịu đựng, cây bộ sưu tập, v.v app Biểu tượng độc quyền; biểu tượng lối vào trình cắm là của riêng trình cắm `src/main/resources/icons`。Không giữ các tài nguyên có cùng tên và đường dẫn trong hai mô-đun, hãy tránh điều này classpath Đổ bóng tài nguyên và trôi dạt phiên bản biểu tượng. Nếu plugin chính thức được trích dẫn `icons/*.svg`，The icon must come from the plugin itself or `dev-workbench-ui`，Không thể dựa vào được app resources。

Các tài nguyên khác cũng nhấp vào owner Quản lý:`dev-workbench-app/src/main/resources/js-libs` Nó là một thư viện tích hợp khi tập lệnh máy chủ được chạy.`logback.xml` Máy chủ có đang chạy cấu hình không.`dev-workbench-plugins/plugin-manager/src/main/resources/plugin-catalog` Nó là thư mục từ dưới lên của thị trường plug-in ở phía máy chủ và là một plug-in thông thường descriptor và plugin độc quyền icon/message bundle Thực hiện theo các mô-đun plug-in tương ứng.

`dev-workbench-plugin-runtime` Chỉ chịu trách nhiệm quét plug-in.descriptor Phân tích,classloader、registry、Sự kiên trì của vòng đời và trạng thái. Nó không phát hành các khả năng bổ trợ kinh doanh cụ thể và không chịu trách nhiệm catalog Phân tích, tải xuống hoặc cài đặt UI。

`dev-workbench-mcp` là độc lập MCP Lớp thích ứng giao thức chỉ chịu trách nhiệm cho chính thức SDK、stdio vòng đời,tool schema、annotations、Kết quả có cấu trúc và tối thiểu `DevWorkbenchMcpBackend` hợp đồng. Nó có thể được dựa vào foundation và MCP SDK，Nhưng nó không thể dựa vào được app、Swing、Không gian làm việc kiên trì hoặc cụ thể HTTP/Thực thi tập lệnh; chủ nhà có mặt app được nhận ra bên trong backend。

`dev-workbench-performance-core` Đừng để bất cứ điều gì đi UI、Cốt lõi của trường kiểm tra sức chịu đựng thực hiện liên kết mà không cần truyền: chỉnh sửa dữ liệu nút lập kế hoạch trạng thái và trạng thái đang chạy `plan.json` Mô hình, hợp đồng thời gian chạy, lập kế hoạch nhóm luồng, thống kê, xu hướng, ảnh chụp nhanh báo cáo,worker assignment/asset reference Loại điều này GUI、CLI、worker Tất cả các hợp đồng phải được sử dụng lại. Nó không phụ thuộc trực tiếp OkHttp、Swing、workspace dịch vụ hoặc app chuỗi thực thi.

Việc điều chỉnh thực thi cụ thể của bài kiểm tra căng thẳng hiện vẫn được giữ nguyên `dev-workbench-app`：GUI chạy,headless CLI、worker server Tái sử dụng đầu tiên app Một chuỗi thực thi hoàn chỉnh được bao gồm, bao gồm phân tích biến và môi trường/Biến toàn cục, tập lệnh, xác nhận, trình trích xuất,CSV inline/file asset、multipart các giấy tờ, chứng chỉ và HTTP/SSE/WebSocket quá trình lây truyền. Việc theo dõi chỉ có trong những không UI Semantics can follow app Chỉ sau khi tách biệt rõ ràng, chúng ta mới có thể xem xét bổ sung thêm sự độc lập mới headless/runner mô-đun.

Các cuộc chạy không đầu tuân theo các ranh giới giống nhau: .`dev-workbench-collection-core` Không có sẵn UI Mô hình miền bộ sưu tập gốc;`com.nvhuong.devworkbench.collection.cli` Chỉ cần sở hữu `collection run` các tham số và bộ sưu tập/Lựa chọn thư mục;`com.nvhuong.devworkbench.functional.cli` Chỉ cần sở hữu `functional run` thông số,`functional_config.json` Phân tích cú pháp và lựa chọn yêu cầu;`com.nvhuong.devworkbench.workspace.cli` Cung cấp khả năng phân tích cú pháp không gian làm việc và tính năng gốc được chia sẻ bởi cả hai `collections.json` / `environments.json` Đang tải, biến toàn cục, dữ liệu lặp bên ngoài,HTTP、Công cụ thực thi tập lệnh và báo cáo. Cả hai mệnh lệnh đều được thông qua `WorkspaceRunPlanner` Lập kế hoạch kết nối giao diện mà không phụ thuộc vào nhau CLI gói.DevWorkbench Trình phân tích cú pháp chỉ phục vụ khả năng nhập chứ không phục vụ mục nhập CLI chuỗi thực thi. Đừng để nó collection-core Sự phụ thuộc ngược lại app dịch vụ thực thi.IOC、Thời gian chạy plugin hoặc Swing。

MCP Server Sử dụng cổng/Bộ chia bộ chuyển đổi:`dev-workbench-mcp/com.nvhuong.devworkbench.mcp.server` Có tiêu chuẩn transport、có thể quan sát được stdio Vòng đời và sáu lõi tool schema；`dev-workbench-app/com.nvhuong.devworkbench.mcp` Chỉ cần sở hữu `mcp serve` CLI、Ủy quyền không gian làm việc, đọc môi trường, hạn chế đường dẫn tải lên,Cookie Kiểm dịch cuộc gọi, giải mẫn cảm, giới hạn chi tiết kết quả và `WorkspaceRunExecutor` backend。Các mô-đun giao thức không thể phụ thuộc ngược lại app，Các biến, tập lệnh hoặc ngữ nghĩa thực thi yêu cầu cũng không được sao chép. Lớp thực thi được chia sẻ đã được thêm tùy chọn để trả về ảnh chụp nhanh phản hồi cho từng yêu cầu và hỗ trợ phạm vi môi trường một lần chụp `WorkspaceRunObserver`、`environmentOverrides`、Tải lên thư mục gốc của tập tin và báo cáo chi tiết giới hạn trên; mặc định là tương ứng no-op、vô giá trị Map、Không hạn chế và không hạn chế, tồn tại `collection run` / `functional run` Các thông số, đầu ra và hành vi không thay đổi. Xem cách sử dụng chi tiết `docs/MCP_SERVER_DESIGN_vi.md`。

HTTP Gửi nút chuỗi thực thi“Sẵn sàng vào app，Truyền vào runtime，UI Vào adapter”Phân chia: việc chuẩn bị yêu cầu, tổng kiểm tra và các nhà máy yêu cầu mặc định vẫn còn đó `dev-workbench-app` của `http.request`，Bởi vì họ cũng dựa vào tính kế thừa của bộ sưu tập, độ phân giải của biến, tập lệnh và app dịch vụ;URL/query Các công cụ chung có sẵn `dev-workbench-request-core` của `request.util`；Phân tích cài đặt chạy ở cấp độ yêu cầu, .`HttpTransport` cảng,`DefaultHttpTransport`、`HttpExchangeExecutor`、`RealtimeConnectionFactory`、Phạm vi client provider、OkHttp sự thích nghi,TLS/Certificate configuration,SSE runtime、Cookie store、redirect、Bản đồ lỗi, cổng tương tác và cổng quan sát đã được chỉ định `dev-workbench-http-runtime`。Swing Việc thực hiện chỉ được đặt `panel/http/runtime` hoặc `http.runtime.app` Loại điều này app adapter ở giữa. bên trên runtime Mã không thể phụ thuộc trực tiếp vào nó Swing/panel，Nó không thể được đọc trực tiếp app của `SettingManager`；tương lai JavaFX host chỉ nên được cung cấp JavaFX adapter，mà không thay đổi HTTP runtime。

`dev-workbench-app` Là lớp lắp ráp máy chủ. Nó có thể được đặt ở lối vào chính.MainFrame、Thực đơn, cụ thể app Bảng điều khiển, khởi động cụ thể wiring、Trang cài đặt, trang cập nhật, trang chào mừng, trang trợ giúp,app-only dịch vụ và khả năng thích ứng truy cập plug-in phía máy chủ `com.nvhuong.devworkbench.plugin.host`。Khi di chuyển các khả năng của nền tảng sau này, chúng tôi sẽ di chuyển dần dần từ đây `dev-workbench-platform`。

`dev-workbench-app` Cũng cần tránh việc hình thành những khái quát mới trong nội bộ model gói.app Không còn sở hữu nữa `com.nvhuong.devworkbench.model` Xuống HTTP Mô hình trao đổi thời gian chạy; thử nghiệm chức năng runner Đặt dữ liệu `com.nvhuong.devworkbench.functional.model`，Kịch bản xác nhận kết quả `com.nvhuong.devworkbench.script.model`，SSE/WebSocket Loại tin nhắn đặt `com.nvhuong.devworkbench.stream`，Thư mục đoạn mã `com.nvhuong.devworkbench.snippet`，lịch sử, chứng chỉ, biến, môi trường,cURL Nhập khẩu vv app Mô hình bên trong theo sau mỗi owner gói. Đừng đặt nó UI view-state、Nhập khẩu tạm thời DTO Hoặc các mô hình dành riêng cho miền được cắm lại `com.nvhuong.devworkbench.model`。

`dev-workbench-plugins/*` Thông thường các plugin chính thức JAR。Các plug-in thông thường không được dựa vào máy chủ ngược lại app Thực hiện nội bộ; được thông qua khi máy chủ cần được mở rộng `dev-workbench-plugin-api` Khả năng đăng ký yêu cầu một nền tảng chung DTO/Sự phụ thuộc của công cụ `dev-workbench-foundation`，Cần phải thống nhất Swing Phụ thuộc vào phong cách `dev-workbench-ui`。

Ranh giới tương thích của trình cắm không chỉ bao gồm `dev-workbench-plugin-api` của SPI。Bởi vì plug-in sẽ `provided` phụ thuộc vào phương pháp `dev-workbench-plugin-api`、`dev-workbench-foundation`、`dev-workbench-ui` và các mô-đun nền tảng khác, hãy xóa, đổi tên và di chuyển các lớp công khai trong các mô-đun này/phương pháp/liệt kê/Đường dẫn tài nguyên hoặc thay đổi chữ ký và ngữ nghĩa điểm mở rộng có thể khiến các plugin cũ xuất hiện trước hoặc sau khi chúng được tải `NoClassDefFoundError`、`NoSuchMethodError` hoặc khác linkage sai lầm. Nếu các thay đổi có chủ ý không duy trì khả năng tương thích nhị phân, thì nên tăng cường quyền root `pom.xml` của `plugin.platform.version`，Đồng bộ hóa các plug-in chính thức `plugin.min/maxPlatformVersion`，Và bù đắp cho thời gian chạy/Đã cập nhật các bài kiểm tra kiểm tra để các plugin cũ trên toàn nền tảng được hiển thị rõ ràng là không tương thích trước khi tải. Đã xuất bản catalog Mục nhập tương ứng với mục hiện có jar và `sha256`，Không giả mạo các mục cũ để tương thích với nền tảng mới platform range；Phạm vi mới nên được xây dựng bằng plugin mới artifact giải phóng.

`dev-workbench-plugins/plugin-manager` là trường hợp đặc biệt về quyền sở hữu trong thư mục này: nó là máy chủ app Chịu trách nhiệm về mô-đun phụ trợ quản lý plug-in phía máy chủ phụ thuộc trực tiếp catalog Phân tích, trực tuyến/Mặt tiền cài đặt ngoại tuyến và bản ghi nguồn cài đặt; nó không được cung cấp bởi  `dev-workbench-plugin-runtime` Không thể sử dụng các phần bổ trợ thời gian chạy thông thường được tải bằng cách quét làm các mẫu phần bổ trợ thông thường. Nó tiếp tục được đặt `dev-workbench-plugins` Trong thư mục tổng hợp có các đường dẫn lịch sử và các lựa chọn tổ chức phát hành; thư viện quản lý plug-in phía máy chủ mới không được đặt theo mặc định `dev-workbench-plugins/*`，Bạn có thể chuyển sang tên mô-đun hoặc thư mục rõ ràng hơn sau này. Về mặt phụ thuộc, các plug-in thông thường vẫn không được phép phụ thuộc vào nó `dev-workbench-app`，Nhưng chủ nhà app Có thể dựa vào `plugin-manager` Đây là một trường hợp đặc biệt.

## Quốc tế hóa, phông chữ, vị trí chủ đề

Cơ chế quốc tế hóa và sắp xếp từ vựng cơ bản đa mô-đun `dev-workbench-foundation`。`I18nUtil`、`CommonI18n`、`CommonMessageKeys`、`common-messages.properties` / `common-messages_vi.properties`、locale/settings key Thuộc về khả năng cơ bản. Từ vựng phổ biến bao gồm OK、Cancel、Save、Copy、Close、Search、Success、Error、Warning、Tip Loại này không có kinh doanh cụ thể owner nhãn ngắn.

Tài nguyên quốc tế theo mô-đun tương ứng. công cộng UI Thành phần copywriting độc quyền được đặt `dev-workbench-ui`，Ví dụ `ui-messages.properties` / `ui-messages_vi.properties`；Trang chủ và bản sao doanh nghiệp lưu trữ được đặt `dev-workbench-app` của `messages_en.properties` / `messages_vi.properties`；Ví dụ: bản sao trình cắm thêm được đặt trong mỗi mô-đun trình cắm thêm `redis-messages.properties`、`kafka-messages.properties`。Đừng công khai UI Phụ thuộc vào thành phần hoặc plugin app resources để hiển thị bản sao; nếu nhiều mô-đun yêu cầu cùng một nhãn ngắn, hãy đặt nó lên đầu foundation common bundle，được sử dụng cùng nhau `CommonI18n.get(CommonMessageKeys...)`，Đừng ở đó `ui-messages*`、app hoặc plugin bundle Tên tương tự được xác định nhiều lần ở đây common key。

Công cụ phông chữ và phông chữ token đặt `dev-workbench-ui`。Đọc cài đặt khi khởi động chung và áp dụng bố cục phông chữ hiện đang được sử dụng app，Thuộc về khi di cư `dev-workbench-platform`。

chủ đề token、Semantic color, icon theme adaptation,RSyntaxTextArea Chủ đề biên tập XML và công cộng UI Giải phóng tài nguyên `dev-workbench-ui`。FlatLaf Quá trình cài đặt, hoạt ảnh chuyển đổi chủ đề và điều phối ứng dụng khởi động hiện đang được thực hiện app，Thuộc về khi di cư `dev-workbench-platform`。FlatLaf token Ghi đè tập tin nếu phụ thuộc app cụ thể bên trong `DevWorkbenchLightLaf` / `DevWorkbenchDarkLaf` Đường dẫn lớp, hãy theo dõi trước app。

## Xác định nơi một lớp nên được đặt

Hãy hỏi trước 5 Câu hỏi:

1. Cho dù nó hoàn toàn không có gì UI Và cần sử dụng nhiều mô-đun? Có được ưu tiên `foundation`。
2. Cho dù đó là một cấu hình yêu cầu/Lưu phản hồi/Mô hình đặc tả yêu cầu có thể tái sử dụng như giao thức yêu cầu? Đúng `request-core`。
3. Cho dù đó là một cây sưu tập/Nhóm bộ sưu tập/Phân tích cú pháp nhập có thể được sử dụng lại và không có tệp  UI Mô hình miền bộ sưu tập? Đúng `collection-core`。
4. Đây có phải là hợp đồng để plugin khai báo khả năng của máy chủ không? Vâng `plugin-api`。
5. Cho dù đó là Không có UI của địa phương Mock khớp tuyến đường, trạng thái phiên hoặc JDK HTTP Thời gian chạy? Đúng `mock-core`；tập trung/GraalJS/Swing Sự thích ứng vẫn còn `app`。
6. Liệu nó có thể tái sử dụng được hay không Swing thành phần, màu sắc, phông chữ, biểu tượng hoặc UI dụng cụ? Đúng `ui`。
7. Cho dù đó là tải plugin, quét, vòng đời hay registry？Vâng `plugin-runtime`。
8. Cho dù đó là khả năng của khung nền tảng hay không, vd IOC、Bắt đầu điều phối, nâng cấp, trang chào mừng, trợ giúp, trung tâm cài đặt, chủ đề/Bố cục ứng dụng phông chữ? Có khả năng tách ra khỏi bê tông app UI Thời gian để phát hành `platform`，Nếu không thì ở lại đầu tiên `app`。
9. Đây có phải là một trang chủ, một menu hoặc một tổ hợp kinh doanh cụ thể không? Nếu vậy thì hãy để nó đi `app`。

The update capabilities are divided according to this boundary: version checking, update source, asset analysis, change log and `UpdateInfo`/`UpdateCheckFrequency` Loại dữ liệu nền tảng này được đặt `dev-workbench-platform`；`AppUpdateCheckCoordinator`、`UpdateUiController`、`UpdateDownloader`、Cập nhật cửa sổ bật lên và cài đặt/Quá trình thoát, cập nhật thị trường plug-in UI Ở lại `dev-workbench-app`。platform Không thể dựa vào trực tiếp app của `SettingManager`，Cần phải vượt qua `UpdateSettingsProvider` Loại giao diện tối thiểu này bao gồm app sự thích nghi.

Các mô-đun mới chỉ được tháo dỡ khi đáp ứng ít nhất hai điều kiện: tái sử dụng ở nhiều khu vực, hướng phụ thuộc rõ ràng và có thể được thử nghiệm và phát hành độc lập/Tốc độ rủi ro là khác nhau và các gói hiện tại đã có nhiều trách nhiệm. Nếu không thì sử dụng trước package Tổ chức, đừng xé rời các mô-đun chỉ để làm cho tên trông đẹp mắt.

## Những vấn đề bị cấm

- Đừng đặt nó Swing Đặt thành phần vào `foundation`。
- Đừng đặt lại mô hình đặc tả yêu cầu `dev-workbench-app` của `com.nvhuong.devworkbench.model`。
- Đừng để nó `dev-workbench-request-core` phụ thuộc Swing、OkHttp、app service、panel hoặc thời gian chạy plugin.
- Không đặt các bộ sưu tập trong các mô hình cốt lõi hoặc  DevWorkbench collection parser Đặt nó lại `dev-workbench-app`，Cũng đừng để nó `dev-workbench-collection-core` phụ thuộc Swing、OkHttp、app service/panel/runtime、platform、plugin-runtime hoặc IOC。
- Đừng để nó `dev-workbench-mock-core` phụ thuộc Swing/AWT、collection/request Người mẫu,app service、GraalVM、OkHttp/Netty、platform IOC hoặc plugin runtime，Cũng không mở rộng triển khai đám mây, quyền và npm sinh thái.
- Đừng để nó HTTP gửi,OkHttp sự thích nghi,SSL、Cookie Thông báo loại này runtime/service Mã trực tiếp phụ thuộc vào nó Swing/panel；Sử dụng tính trung lập sink/dispatcher，Bởi Swing/JavaFX adapter hoàn thành.
- Không đặt giao diện dịch vụ plug-in `foundation`。
- Đừng phụ thuộc vào các plugin chính thức thông thường `dev-workbench-app`；`dev-workbench-plugins/plugin-manager` Đó là chủ nhà app Các trường hợp đặc biệt về quản lý trình cắm có thể dựa trực tiếp vào và không thể sử dụng làm mẫu trình cắm mới.
- Đừng công khai UI Sự phụ thuộc thành phần app resources để tự tải icons。
- Đừng ở đó app và ui Lặp lại cùng một tên `icons/*.svg`；Biểu tượng phổ quát được trả lại `dev-workbench-ui`，Biểu tượng doanh nghiệp thuộc về mô-đun người tiêu dùng.
- Đừng ở đó app Một bộ nút riêng tư mới đã được thêm vào bảng điều khiển/màu sắc/Quy tắc phông chữ nên được giải quyết trước tiên `dev-workbench-ui`。
- Đừng đặt nó“Tôi vẫn chưa biết đặt nó ở đâu”Mã được đưa vào một khái quát common gói.

## Sau đó đơn giản hóa việc phân chia mục tiêu

Việc rút tiền tiếp theo có thể được thực hiện dần dần tùy theo thu nhập và không cần phải hoàn thành tất cả cùng một lúc:

```text
dev-workbench-platform
  Đã có:IOC、Cập nhật lõi khám phá
  Theo dõi: khởi động, trang chào mừng, trợ giúp, trung tâm cài đặt, chủ đề/Bố cục ứng dụng phông chữ

dev-workbench-request-core
  Hiện có: mô hình cấu hình yêu cầu, lưu phản hồi,header/body/auth/protocol DTO
  Theo dõi: Tiếp tục hội tụ mô hình đặc tả yêu cầu và không duy trì sự phụ thuộc vào việc triển khai truyền dẫn

dev-workbench-collection-core
  Hiện có: thiết lập mô hình cây thuần túy, mô hình nhóm, kết quả phân tích cú pháp,DevWorkbench collection parser
  Tiếp theo: Xuất khẩu tính trung lập DTO、Hợp đồng đọc-ghi bộ sưu tập, không UI Truy vấn/Sao chép/Logic kế thừa

dev-workbench-mock-core
  Đã: địa phương JDK HTTP dịch vụ,Example Khớp tuyến, trạng thái phiên, cổng ghi nhật ký và tập lệnh
  Ranh giới: Hỗ trợ hoạt động cục bộ, LAN và tự lưu trữ, không tăng DevWorkbench Đám mây, quyền nhóm,npm hoặc AI Tạo sự phụ thuộc

dev-workbench-runtime-core
  Theo dõi: phân tích biến, hợp đồng đường dẫn tập lệnh, xác nhận/Mô hình chạy trung tính vắt; cụ thể Rhino/Graal/HTTP Sự thích ứng vẫn bị chia cắt bởi sự phụ thuộc

dev-workbench-http-runtime
  Đã có:`PreparedRequest`、`HttpResponse`、`HttpEventInfo`、transport、OkHttp、SSL、Cookie store、SSE、redirect、runtime settings、Sự tương tác/Cảng quan sát
  Theo dõi: Tiếp tục đưa vào mô hình thời gian chạy OkHttp-specific Loại hội tụ về trung tính DTO，và giữ UI Chỉ vượt qua sink/dispatcher truy cập
```

Giữ trong khi giải nén app Chỉ việc lắp ráp được thực hiện, các mô-đun lõi không có sự phụ thuộc ngược lại app UI。
