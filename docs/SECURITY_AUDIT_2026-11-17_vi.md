# Báo cáo rà soát thư viện Java

Ngày rà soát: 17-11-2026  
Phạm vi: source và artifact Maven/JAR hiện có trong workspace DevWorkbench.

## Kết luận

Sau khi decompile toàn bộ class trong 7 artifact đóng gói và rà thủ công các đường thực thi nhạy cảm, **chưa thấy bằng chứng đánh cắp dữ liệu hoặc kết nối bí mật tới máy chủ không liên quan**. Đây là kết luận dựa trên rà soát tĩnh; không thể chứng minh tuyệt đối rằng phần mềm không có mã độc, và báo cáo nêu rõ phần nào đã đọc theo luồng hành vi thay vì đọc từng dòng của hàng chục nghìn file thư viện.

## Phạm vi đã kiểm tra

- Source Java của app và plugin: khoảng 863 file (773 trong app và 90 trong plugin source), rà các điểm dùng mạng, socket, tiến trình, class loading, file và cập nhật.
- Decompile Vineflower toàn bộ 7 JAR đóng gói (app uber-JAR và 6 plugin): 50.921 class đầu vào, 25.848 file Java đầu ra trong 33 archive theo shard. Tất cả 33 archive kiểm tra được và không có lỗi ZIP. Các 701 class từ module JAR được so SHA-256 với class tương ứng trong app uber-JAR và trùng byte-for-byte; do đó đã được bao phủ qua app uber-JAR.
- App uber-JAR có 27.098 class. Các class thư viện đã decompile được ra source; class lồng nhau thường được hợp nhất vào file Java của class ngoài nên số file source ít hơn số class đầu vào.
- Vineflower ghi nhận 29 cảnh báo method: 26 trong OkHttp/Okio, 2 trong Netty/Bouncy Castle, và 1 mục `Statement cannot be decomposed` gắn với Netty executor. Đã chạy CFR 0.152 bổ sung cho các cụm OkHttp/Okio, Netty và toàn package HAETAE; đọc thủ công các luồng kết nối, TLS/hostname verification, interceptor request, WebSocket, HTTP/2, DiskLruCache, thao tác file/ZIP và Netty executor. 6 cảnh báo `could not be written` nằm ở table model/source app hoặc plugin client-cert, không phải thư viện runtime; mã nguồn gốc tương ứng hiện diện trong repository.
- Module JAR trùng class nên không đếm trùng trong tổng 50.921 class. Artifact `original-*.jar` là bản lặp theo cấu hình build và không bổ sung class khác; không được tính là binary khác trong corpus decompile.
- Artifact JAR SHA-256 được ghi ở phần cuối để đối chiếu đúng file đã rà. Bản `plugin-redis` shaded JAR được dùng trong shard là `36044c7f8c3ed43016a52b12aa909e93c87f7ddcd82fecb588b4b5f7d11791ab`; file `original-*.jar` có hash cũ do khác lần build và không được tính là cùng binary.

## Hành vi mạng hợp lệ quan sát được

- App thực hiện request do người dùng cấu hình qua OkHttp, WebSocket/SSE và hỗ trợ performance test. Dữ liệu gửi tới endpoint trong request là chức năng chính của DevWorkbench client.
- Git workspace plugin có thể đồng bộ repository được người dùng cấu hình.
- Plugin Kafka/Redis/Capture và client certificate kết nối tới broker, cache, proxy hoặc endpoint cấu hình bởi người dùng.
- Thư viện MCP/Reactor/Jackson có URL tài liệu/specification trong constant pool; đây không phải bằng chứng về kết nối mạng.

Trong source app có `ProcessBuilder` trong `UpdateDownloader`, dùng để khởi chạy installer sau thao tác cập nhật. MCP SDK có khả năng client transport khởi chạy process, nhưng host app khởi tạo MCP server qua stdin/stdout; không thấy luồng gọi MCP client đó trong app composition. Các API mạng/socket khác thuộc HTTP runtime, kiểm tra update, plugin, Git, proxy và protocol mà ứng dụng công bố.

## Lưu ý an ninh tìm thấy

- Có dữ liệu nhạy cảm có thể xuất hiện trong log cục bộ: `SseStreamEventListener` log toàn bộ response headers khi lỗi/hủy (có thể chứa `Set-Cookie`); `HttpRequestExecutor` log URL đầy đủ trong một số nhánh hủy/gián đoạn (có thể chứa token trong query string). Đây là rủi ro lộ dữ liệu trên máy nếu người khác đọc được log, **không phải bằng chứng gửi dữ liệu ra ngoài**. Cần tránh chia sẻ file log thô và nên redaction các trường này trong bản vá riêng.
- `logback.xml` cấu hình appender console/file tại máy, không thấy network appender. Thư mục log có giới hạn luân chuyển/kích thước. `DevWorkbenchApiContext` có log cookie ở DEBUG; không phải mức mặc định INFO.

## Dependency/artifact chính

Host app dùng các thư viện như PDFBox, FlatLaf, Jackson, GraalJS, OkHttp, JGit, MCP SDK, JFreeChart và logging. Plugin đóng gói thêm Kafka client, Netty/BouncyCastle, Ayza/BouncyCastle, CFR và Jedis. Inventory đầy đủ theo Maven nằm tại `/tmp/dev-workbench-dependency-tree.txt` trong môi trường kiểm tra.

Mức độ review thủ công: đọc các điểm kích hoạt nhạy cảm trong source ứng dụng; quét source decompile của toàn bộ corpus để định vị process launch, nạp mã, truy cập mạng và file; đọc thủ công các đường thư viện có khả năng gửi/nhận dữ liệu hoặc ghi cache, cùng các method Vineflower báo lỗi bằng source CFR. Không đọc tuần tự từng dòng của toàn bộ 25.848 file vendor — điều đó sẽ không khả thi và cũng không phải phương pháp tốt để tìm hành vi; vì vậy không gọi đây là kiểm toán chứng minh an toàn tuyệt đối. Báo cáo không thay cho quét CVE/SCA theo phiên bản, kiểm thử runtime trong sandbox, xác minh nguồn upstream/chữ ký, hoặc rà soát release artifact được tải từ nguồn độc lập.

## SHA-256 JAR

```text
7f302e509698769853d980bf12308b91d7cfa1cb9be3d860337c789667b7b1e7  dev-workbench-app/target/dev-workbench-6.3.3.jar
9f4c4125233442e51fe1e3b6427b9aa538d48ec6fa788e7ca4abc4071dc25912  dev-workbench-app/target/original-dev-workbench-6.3.3.jar
f997f5d5cdfc67f769edf97bd4733b242c65b40fb1a12762a14ecf7fc72f7ab3  dev-workbench-plugins/plugin-capture/target/dev-workbench-6.0.15-plugin-capture.jar
49be3c9e61d9a6908a69ecbe8380164e7b07509c69f0966e48a46c7cf977c1e9  dev-workbench-plugins/plugin-capture/target/original-dev-workbench-6.0.15-plugin-capture.jar
ec84f71151a0393f8d597fbfb622f0cce0e1cff64902e1ab5e73244665c82a63  dev-workbench-plugins/plugin-client-cert/target/dev-workbench-6.0.8-plugin-client-cert.jar
abf0774127a97e02fd7b0bd346ac23311608bdde0be411a1adfa3414e65264f8  dev-workbench-plugins/plugin-client-cert/target/original-dev-workbench-6.0.8-plugin-client-cert.jar
f8ef99ab6ebae4df9eeaf51ab2ca8e559148414bb4452e62e5d62608b9724173  dev-workbench-plugins/plugin-decompiler/target/dev-workbench-6.0.9-plugin-decompiler.jar
d94770d43e73937f7a04f4dd63903faea4bd1f97910d568910e4729b4119e323  dev-workbench-plugins/plugin-decompiler/target/original-dev-workbench-6.0.9-plugin-decompiler.jar
a11211ce0cd6f4ba34ff04fa7f0eea02d4b9e4ec9b4b09e7f560b2b584af0e66  dev-workbench-plugins/plugin-kafka/target/dev-workbench-6.0.11-plugin-kafka.jar
66b9ccef752b07f5ebd3ae9336fca59beab6719bfe8450def8824f0a09bbdf08  dev-workbench-plugins/plugin-kafka/target/original-dev-workbench-6.0.11-plugin-kafka.jar
4fa0be6a472b14a001aafd8d9c54c740e5e765f649f041acab0605abd7bd7d8e  dev-workbench-plugins/plugin-manager/target/dev-workbench-plugin-manager-6.3.3.jar
36044c7f8c3ed4301597709206d2d6a145d2ca0b9177adc99a19c5404b8a90da23  dev-workbench-plugins/plugin-redis/target/dev-workbench-6.0.11-plugin-redis.jar
a0b2fd7e4b680c98abc799d8c2d6a145d2ca0b9177adc99a19c5404b8a90da23  dev-workbench-plugins/plugin-redis/target/original-dev-workbench-6.0.11-plugin-redis.jar
```
