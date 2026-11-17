## OkHttpClient Nhóm kết nối và mô tả tham số đồng thời

Vào Easy Postman của OkHttpClientManager , hãy sử dụng phương pháp sau cho mỗi trường hợp baseUri Định cấu hình kết nối tổng hợp và Dispatcher：

```java
// Cấu hình nhóm kết nối
.connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDuration, TimeUnit.SECONDS))

// Dispatcher Cấu hình đồng thời
Dispatcher dispatcher = new Dispatcher();
dispatcher.setMaxRequests(maxRequests);
dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
.dispatcher(dispatcher)
```

### Giải thích tham số

#### Thông số nhóm kết nối
- **maxIdleConnections**：Số lượng kết nối nhàn rỗi tối đa. Cho biết số lượng kết nối tối đa được phép ở trạng thái rảnh trong nhóm kết nối.
- **keepAliveDuration**：Thời gian tồn tại tối đa của kết nối không hoạt động (đơn vị được chỉ định bởi tham số thứ ba). Các kết nối không hoạt động không được sử dụng quá thời gian này sẽ bị đóng.
- **TimeUnit.SECONDS**：được chỉ định keepAliveDuration Đơn vị thời gian là giây.

#### Dispatcher Các tham số đồng thời (⚠️ Các thông số chính của kiểm tra áp suất)
- **maxRequests**：Số lượng yêu cầu đồng thời tối đa (tổng của tất cả các máy chủ). Mặc định64，Bạn nên đặt nó theo số lượng người dùng đồng thời trong quá trình kiểm tra căng thẳng2-4lần.
- **maxRequestsPerHost**：Số lượng yêu cầu đồng thời tối đa cho một máy chủ. mặc định5，**Đây là lý do chính cho hàng đợi kiểm tra căng thẳng**！Bạn nên đặt nó theo số lượng người dùng đồng thời trong quá trình kiểm tra căng thẳng.

### ⚠️ Khắc phục sự cố về hiệu suất của stress test

Nếu bạn phát hiện ra trong quá trình kiểm tra căng thẳng **Stalled** Nguyên nhân là do thời gian dài bất thường (ví dụ hàng chục giây), cho thấy yêu cầu đang chờ trong hàng đợi **maxRequestsPerHost** Số lượng đồng thời bị hạn chế.

#### Ví dụ vấn đề
```
DnsStart 14:21:35.808
DnsEnd 14:21:36.424 (DNSTruy vấn là bình thường.616ms)
ConnectStart 14:22:51.429 ⚠️ 75Trì hoãn lần thứ hai! .

Stalled: 75621 ms ← Đây là thời gian xếp hàng yêu cầu
```

**lý do**：OkHttp Mặc định `maxRequestsPerHost=5`，Nếu số lượng đồng thời vượt quá5，Yêu cầu bổ sung sẽ chờ trong hàng đợi.

**giải pháp**：Tăng các thông số sau trong cài đặt kiểm tra sức chịu đựng:
- **Số lượng yêu cầu đồng thời tối đa**：Nên đặt nó theo số lượng người dùng đồng thời2-4lần (ví dụ:100Người dùng đồng thời → Đặt thành  200-400）
- **Số lượng đồng thời tối đa trên một máy chủ**：Nên đặt nó theo số lượng người dùng đồng thời (ví dụ:100Người dùng đồng thời → Đặt thành  100-200）

Vào Easy Postman Vào, vào **cài đặt → Kiểm tra căng thẳng**，Điều chỉnh:
- Số lượng yêu cầu đồng thời tối đa (Max Concurrent Requests)
- Số lượng đồng thời tối đa trên một máy chủ (Max Concurrent Requests Per Host)

### maxIdleConnections Detailed explanation
maxIdleConnections Kiểm soát cùng một máy chủ (baseUri）Bên dưới là số lượng kết nối nhàn rỗi tối đa có thể được sử dụng lại trong nhóm kết nối.

#### Đưa ra ví dụ
Giả định maxIdleConnections Đặt thành  6：
- Nếu bạn đang nhắm mục tiêu cùng một máy chủ (ví dụ: https://api.example.com）Đồng loạt khởi xướng 10 yêu cầu, nhóm kết nối sẽ chỉ giữ lại nhiều nhất 6 kết nối nhàn rỗi.
- Các kết nối còn lại sẽ bị đóng sau khi không hoạt động để tránh lãng phí tài nguyên.
- Điều này có thể cải thiện hiệu suất trong điều kiện đồng thời cao và ngăn chặn quá nhiều kết nối vô dụng chiếm tài nguyên hệ thống.

#### Các tình huống áp dụng
- **Gỡ lỗi giao diện/môi trường phát triển**：nói chung là 6 đủ.
- **Kiểm tra căng thẳng hiệu suất/Kịch bản đồng thời cao**：Trạng thái chạy stress test sử dụng độc lập `HttpClientRuntimeConfig` Kiểm soát nhóm kết nối và các tham số đồng thời để tránh sửa đổi liên kết gỡ lỗi chung trên máy tính để bàn OkHttpClient。

### maxIdleConnections Giải thích chi tiết (mô tả quy trình chi tiết hơn)

Giả định maxIdleConnections Đặt thành  6，keepAliveDuration Đặt thành  90 Giây tiếp theo, khung cảnh như sau:

#### Đồng loạt khởi xướng 10 toàn bộ quá trình của một yêu cầu

1. **Yêu cầu giai đoạn bắt đầu**
   khách hàng (ví dụ: Easy Postman）Đồng thời đến cùng một máy chủ (ví dụ https://api.example.com）Đồng loạt khởi xướng 10 cá nhân HTTP yêu cầu.
   OkHttp Một kết nối sẽ được phân bổ cho mỗi yêu cầu (nếu có một kết nối nhàn rỗi trong nhóm kết nối, nó sẽ được sử dụng lại, nếu không một kết nối mới sẽ được tạo).

2. **Yêu cầu xử lý và trả lại kết nối**
   10 Sau khi yêu cầu được xử lý, điều này 10 Kết nối sẽ được trả về nhóm kết nối.
   Nhóm kết nối sẽ kiểm tra xem số lượng kết nối nhàn rỗi hiện tại có bị vượt quá hay không maxIdleConnections（Trong trường hợp này 6）。

3. **Quản lý kết nối nhàn rỗi**
   Nếu số lượng kết nối nhàn rỗi vượt quá sau khi quay trở lại 6，Nhóm kết nối sẽ ngay lập tức đóng các kết nối nhàn rỗi dư thừa và chỉ giữ lại chúng 6 cá nhân.
   Những kết nối nào được giữ lại và những kết nối nào bị đóng OkHttp Quyết định chính sách nội bộ (thường kết nối hoạt động gần đây nhất được giữ lại).

4. **keepAliveDuration vai trò**
   dành riêng 6 Các kết nối không hoạt động sẽ chờ để sử dụng lại trong nhóm kết nối.
   Nếu vào keepAliveDuration（Chẳng hạn như 90 giây), các kết nối không hoạt động này sẽ tự động bị đóng để giải phóng tài nguyên.
   Tức là,**maxIdleConnections** Nó đã được quyết định“Số lượng kết nối nhàn rỗi tối đa có thể được giữ lại”，**keepAliveDuration** Nó đã được quyết định“Khoảng thời gian tối đa mà một kết nối không hoạt động có thể được duy trì”。

#### Vai trò của các thông số chính

- **maxIdleConnections**：Xác định số lượng kết nối nhàn rỗi tối đa có thể được giữ lại trong nhóm kết nối. Ngoài con số này, các kết nối nhàn rỗi dư thừa sẽ bị đóng ngay lập tức.
- **keepAliveDuration**：Xác định khoảng thời gian tối đa mà một kết nối không hoạt động có thể duy trì ở trạng thái không hoạt động. Các kết nối không hoạt động không được sử dụng lại quá thời gian này sẽ tự động bị đóng.

#### Chỉ báo quá trình

1. đồng thời 10 yêu cầu, tạo ra 10 kết nối.
2. Yêu cầu kết thúc.10 các kết nối được trả về nhóm kết nối.
3. Kết nối tổng hợp chỉ được bảo lưu 6 những kết nối không hoạt động, hãy đóng những kết nối dư thừa ngay lập tức 4 cá nhân.
4. Cái này 6 Có kết nối miễn phí 90 Nếu nó không được sử dụng lại bởi các yêu cầu mới trong vòng vài giây, nó sẽ tự động bị đóng.

#### Lợi ích của việc làm này

- Tránh tình trạng số lượng lớn các kết nối vô dụng chiếm tài nguyên hệ thống trong thời gian dài.
- Đảm bảo hiệu suất và sử dụng tài nguyên ở mức độ đồng thời cao.
- Các thông số của nhóm kết nối có thể được điều chỉnh linh hoạt theo sự đồng thời thực tế.

### Tham khảo
- OkHttp Tài liệu chính thức: .[ConnectionPool](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-connection-pool/)
- Chrome Mặc định của trình duyệt cho mỗi host 6 kết nối,Easy Postman Mặc định là chính sách tương tự.
