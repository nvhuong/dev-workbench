# Báo cáo rà soát thư viện Java

Ngày rà soát: 17-11-2026  
Phạm vi: source và artifact Maven/JAR hiện có trong workspace EasyPostman.

## Kết luận

Rà soát tĩnh hiện tại **chưa phát hiện dấu hiệu rõ ràng** về mã độc hoặc hành vi âm thầm gửi dữ liệu người dùng tới máy chủ không liên quan. Đây không phải chứng nhận an toàn tuyệt đối: rà soát tĩnh không thể chứng minh một chương trình không có mã độc, và một số class thư viện chưa được decompile thủ công từng file.

## Phạm vi đã kiểm tra

- Source Java của app và plugin: khoảng 863 file (773 trong app và 90 trong plugin source), rà các điểm dùng mạng, socket, tiến trình, class loading, file và cập nhật.
- Artifact JAR hiện diện: 18 file JAR, tổng cộng khoảng 51.622 class; gồm app uber-JAR, các module, plugin manager và 5 plugin đóng gói.
- App uber-JAR có 27.098 class, trong đó 2.273 class của EasyPostman và khoảng 24.825 class thư viện bên thứ ba.
- Đã tạo inventory dependency Maven cho host app, dò URL/hằng chuỗi trong class, và decompile bằng CFR 0.152. Tại thời điểm ghi báo cáo, CFR đã xuất 1.913 class thư viện từ app uber-JAR; decompile toàn bộ 24.825 class thư viện app chưa hoàn tất. Source của EasyPostman đã được rà bằng API/hành vi nhạy cảm; class thư viện có API mạng/file/process được tính là khả năng chức năng, không coi marker đơn lẻ là kết luận malware.
- Artifact JAR SHA-256 được ghi ở phần cuối để đối chiếu đúng file đã rà.

## Hành vi mạng hợp lệ quan sát được

- App thực hiện request do người dùng cấu hình qua OkHttp, WebSocket/SSE và hỗ trợ performance test. Dữ liệu gửi tới endpoint trong request là chức năng chính của Postman client.
- Có request kiểm tra/phát hành cập nhật tới GitHub hoặc Gitee (`lakernote/easy-postman`), cùng tải installer theo luồng cập nhật.
- Plugin manager đọc catalog và tải plugin từ GitHub/Gitee.
- Git workspace plugin có thể đồng bộ repository được người dùng cấu hình.
- Plugin Kafka/Redis/Capture và client certificate kết nối tới broker, cache, proxy hoặc endpoint cấu hình bởi người dùng.
- Thư viện MCP/Reactor/Jackson có URL tài liệu/specification trong constant pool; đây không phải bằng chứng về kết nối mạng.

Trong source tìm thấy `ProcessBuilder` trong `UpdateDownloader`, dùng để khởi chạy installer sau thao tác cập nhật. Những API mạng/socket khác thuộc HTTP runtime, kiểm tra update, plugin, Git, proxy và protocol mà ứng dụng công bố.

## Dependency/artifact chính

Host app dùng các thư viện như PDFBox, FlatLaf, Jackson, GraalJS, OkHttp, JGit, MCP SDK, JFreeChart và logging. Plugin đóng gói thêm Kafka client, Netty/BouncyCastle, Ayza/BouncyCastle, CFR và Jedis. Inventory đầy đủ theo Maven nằm tại `/tmp/easy-postman-dependency-tree.txt` trong môi trường kiểm tra.

Rà soát này tập trung vào hành vi đánh cắp dữ liệu/mã độc. Nó không thay cho quét CVE/SCA phiên bản dependency, kiểm thử hành vi runtime trong sandbox, kiểm tra nguồn upstream/chữ ký nhà phát hành, hay review thủ công toàn bộ bytecode thư viện.

## SHA-256 JAR

```text
7f302e509698769853d980bf12308b91d7cfa1cb9be3d860337c789667b7b1e7  easy-postman-app/target/easy-postman-6.3.3.jar
9f4c4125233442e51fe1e3b6427b9aa538d48ec6fa788e7ca4abc4071dc25912  easy-postman-app/target/original-easy-postman-6.3.3.jar
f997f5d5cdfc67f769edf97bd4733b242c65b40fb1a12762a14ecf7fc72f7ab3  easy-postman-plugins/plugin-capture/target/easy-postman-6.0.15-plugin-capture.jar
49be3c9e61d9a6908a69ecbe8380164e7b07509c69f0966e48a46c7cf977c1e9  easy-postman-plugins/plugin-capture/target/original-easy-postman-6.0.15-plugin-capture.jar
ec84f71151a0393f8d597fbfb622f0cce0e1cff64902e1ab5e73244665c82a63  easy-postman-plugins/plugin-client-cert/target/easy-postman-6.0.8-plugin-client-cert.jar
abf0774127a97e02fd7b0bd346ac23311608bdde0be411a1adfa3414e65264f8  easy-postman-plugins/plugin-client-cert/target/original-easy-postman-6.0.8-plugin-client-cert.jar
f8ef99ab6ebae4df9eeaf51ab2ca8e559148414bb4452e62e5d62608b9724173  easy-postman-plugins/plugin-decompiler/target/easy-postman-6.0.9-plugin-decompiler.jar
d94770d43e73937f7a04f4dd63903faea4bd1f97910d568910e4729b4119e323  easy-postman-plugins/plugin-decompiler/target/original-easy-postman-6.0.9-plugin-decompiler.jar
a11211ce0cd6f4ba34ff04fa7f0eea02d4b9e4ec9b4b09e7f560b2b584af0e66  easy-postman-plugins/plugin-kafka/target/easy-postman-6.0.11-plugin-kafka.jar
66b9ccef752b07f5ebd3ae9336fca59beab6719bfe8450def8824f0a09bbdf08  easy-postman-plugins/plugin-kafka/target/original-easy-postman-6.0.11-plugin-kafka.jar
4fa0be6a472b14a001aafd8d9c54c740e5e765f649f041acab0605abd7bd7d8e  easy-postman-plugins/plugin-manager/target/easy-postman-plugin-manager-6.3.3.jar
36044c7f8c3ed4301597709206d2d6a145d2ca0b9177adc99a19c5404b8a90da23  easy-postman-plugins/plugin-redis/target/easy-postman-6.0.11-plugin-redis.jar
a0b2fd7e4b680c98abc799d8c2d6a145d2ca0b9177adc99a19c5404b8a90da23  easy-postman-plugins/plugin-redis/target/original-easy-postman-6.0.11-plugin-redis.jar
```
