# Hướng dẫn sử dụng kiểm tra căng thẳng cụm

Bài viết này dành cho người dùng và giải thích cách thực hiện GUI Kế hoạch kiểm tra hiệu suất được định cấu hình sẽ được phân phối đến nhiều máy chủ để thực thi. Hành vi cơ bản là tương tự JMeter của master/worker：master Chỉ chịu trách nhiệm phân phối, bỏ phiếu, dừng và tổng hợp, áp lực yêu cầu thực sự sẽ do mỗi người xử lý worker Quá trình này được tạo ra.

## Các khái niệm cốt lõi

- `plan.json`：Từ GUI Đã xuất kế hoạch kiểm tra căng thẳng có thể thực thi được. Nó chứa các biến môi trường, biến toàn cục, cài đặt thực thi, nhóm luồng, yêu cầu, tập lệnh, xác nhận,CSV inline Tài liệu tham khảo dữ liệu và tài sản bên ngoài.
- master：GUI Chế độ từ xa hoặc `performance master run` Đặt hàng.master Không trực tiếp nén dịch vụ mục tiêu mà chỉ lập kế hoạch và phân chia nó assignment Đã ban hành worker。
- worker：trên máy chủ `performance worker` quá trình.worker Chỉ khi nhận được master được giao assignment Thực hiện sau để tránh nhiều máy chạy một kế hoạch hoàn chỉnh, dẫn đến số tiền và phân bổ lớn.
- Tổng số đồng thời:GUI trong mỗi cái Thread Group Số lượng người dùng được định cấu hình là tổng số lượng người dùng đồng thời trên toàn cầu, không phải của mỗi máy worker của sự đồng thời. Ví dụ 100 Người sử dụng,2 Đài Loan worker，Được chia theo mặc định 50/50；101 Người sử dụng,2 Đài Loan worker，Được chia theo mặc định 51/50。
- CSV Phân mảnh:CSV Data Set Sẽ theo dõi khoảng thời gian toàn cầu của người dùng ảo để tránh tất cả worker Tất cả từ đầu tiên 0 Dòng bắt đầu đọc.CSV Khi số lượng hàng ít hơn số lượng người dùng ảo, chúng sẽ được tái chế theo số lượng người dùng toàn cầu.

## Khi nào nên sử dụng kiểm tra căng thẳng cụm

Thích hợp cho:

- Độc lập CPU、Mạng, bộ mô tả tệp hoặc kết nối mục tiêu không đủ yêu cầu nhiều máy phải cùng nhau tạo áp lực.
- Cần phải chạy các bài kiểm tra căng thẳng trên các máy chủ không có máy tính để bàn trong một thời gian dài.
- GUI Chịu trách nhiệm chỉnh sửa, quan sát và dừng lại,worker Thực hiện trên máy thử áp suất chuyên dụng.

Không phù hợp với:

- Chỉ cần vài trăm lần xác minh nhanh đồng thời trên một máy GUI hoặc CLI Đơn giản hơn.
- Yêu cầu chéo nghiêm ngặt worker Bức tranh lớn P99 Các mẫu ban đầu được sáp nhập. Hiện tại master Mỗi uẩn là gì worker Kết quả báo cáo không thành công/Nhấn chậm để yêu cầu chi tiết worker Kéo trở lại sau khi duy trì giới hạn.

## Sự chuẩn bị

1. Tất cả các máy đều sử dụng cùng một phiên bản EasyPostman JAR hoặc cài đặt gói.
2. mỗi đơn vị worker Máy có thể truy cập mục tiêu API。
3. master Có thể truy cập mọi trạm worker cổng nghe.
4. Nếu bạn định tham chiếu các tệp cục bộ, vd file-source CSV、multipart Các tập tin, vui lòng đặt các tập tin trên mỗi trạm trước worker cùng một con đường.GUI Các tập tin cục bộ sẽ không được tải lên tự động.
5. nếu CSV Vâng GUI Được tạo hoặc nhập thủ công và nhúng vào kế hoạch, không cần tải thêm tệp nào lên.

## Bắt đầu worker

Bắt đầu trên mỗi máy kiểm tra sức chịu đựng worker：

```bash
java -jar easy-postman-5.5.28.jar \
  performance worker \
  --host 0.0.0.0 \
  --port 19090
```

`--progress-interval <seconds>` Chỉ cần kiểm soát worker Tần suất bảng điều khiển in tiến trình, ví dụ users、total、success、failed、qps。Nó không ảnh hưởng master Tần suất bỏ phiếu không ảnh hưởng đến việc thực hiện các yêu cầu kiểm tra căng thẳng. Mặc định là 1 Giây, vì vậy nói chung bạn không cần phải viết nó.

Tắt đầu ra tiến trình của bảng điều khiển: .

```bash
java -jar easy-postman-5.5.28.jar \
  performance worker \
  --host 0.0.0.0 \
  --port 19090 \
  --no-progress
```

Máy này xác minh cả hai worker：

```bash
java -jar easy-postman-5.5.28.jar performance worker --host 127.0.0.1 --port 19090
java -jar easy-postman-5.5.28.jar performance worker --host 127.0.0.1 --port 19091
```

## GUI Thực hiện từ xa

1. Định cấu hình một hoặc nhiều trên trang kiểm tra hiệu suất Thread Group。
2. Kiểm tra phần trên cùng `Điều khiển từ xa`。
3. Vào `Workers` Điền vào hộp đầu vào worker Danh sách, hỗ trợ phân tách bằng dấu phẩy hoặc khoảng trắng:

```text
10.0.0.11:19090,10.0.0.12:19090
```

4. Nhấp vào Bắt đầu.GUI Sẽ hành động master Tạo các mảnh vỡ assignment，Và gửi kế hoạch hiện tại cho từng người worker。
5. Khi bấm vào để dừng lại,GUI Sẽ trao cho tất cả worker gửi stop。
6. Sau khi kết thúc,GUI Sẽ kéo từng cái một worker Sự kết thúc report và tổng hợp thành“Báo cáo”；thất bại/Chi tiết yêu cầu chậm sẽ được kéo trở lại ở giai đoạn đóng thích hợp“Bảng kết quả”。

## CLI master thực hiện

Bắt đầu với GUI Xuất khẩu `plan.json`，Chạy lại: .

```bash
java -jar easy-postman-5.5.28.jar \
  performance master run \
  --plan /path/to/plan.json \
  --workers 10.0.0.11:19090,10.0.0.12:19090 \
  --out /tmp/easy-postman-master-result.json
```

Không có máy đơn lẻ GUI Thực hiện cùng một kế hoạch:

```bash
java -jar easy-postman-5.5.28.jar \
  performance run \
  --plan /path/to/plan.json \
  --out /tmp/easy-postman-result.json
```

## Cách hoạt động của tính năng làm mới theo thời gian thực

Độc lập `performance run` và `performance master run` Tiến trình sẽ được in trên bảng điều khiển. được chỉ định `--out` Sau đó, tập tin đầu ra đầu tiên sẽ trở thành  `PENDING`，Trong quá trình hoạt động, nó được cập nhật liên tục thông qua các tập tin tạm thời trong cùng thư mục và thay thế nguyên tử `RUNNING` Ảnh chụp nhanh được thay thế bằng báo cáo cuối cùng hoàn chỉnh sau khi hoàn thành; nó cũng sẽ được viết lần này trong trường hợp thất bại `FAILED` Báo cáo để tránh giữ lại kết quả cuối cùng. Sự kết thúc của cả hai đơn đặt hàng JSON Tất cả đều ở mức cao nhất `PerformanceJsonReport`，Có thể sử dụng thống nhất `.summary.totalRequests` Đọc đường dẫn bằng nhau.

GUI Khi chạy ở chế độ từ xa, mỗi 1 giây cho mỗi worker Truy vấn trạng thái một lần:

- Đóng lại“Làm mới trong thời gian thực”và đóng cửa“Kích hoạt xu hướng”Thời gian: Yêu cầu `/api/performance/v1/runs/{runId}?report=false`，Chỉ kéo trạng thái nhẹ, bao gồm số lượng người dùng đang hoạt động, tổng số người dùng, số lượng yêu cầu và số lần thất bại.QPS và trạng thái hoạt động.
- Bật lên“Làm mới trong thời gian thực”hoặc“Kích hoạt xu hướng”Thời gian: Yêu cầu `/api/performance/v1/runs/{runId}`，worker Tổng hợp hiện tại sẽ được xây dựng report Ảnh chụp nhanh quay trở lại. Báo cáo thời gian thực được làm mới bằng cách sử dụng nó“Báo cáo”trang; sử dụng nó để đọc biểu đồ xu hướng HTTP/WS/SSE Số lượng tích lũy cấp độ giao thức.
- “Kích hoạt xu hướng”Sử dụng cùng một vòng dữ liệu trạng thái để lấy mẫu xu hướng mà không cần bổ sung thêm worker The number of requests; khoảng thời gian lấy mẫu xu hướng là cài đặt của người dùng, là mức thấp nhất 1 giây.WS/SSE của việc gửi tin nhắn/giây để nhận được tin nhắn/Giây phải phụ thuộc vào tổng hợp report，Nếu không thì nhẹ status Chỉ có thể lấy được tổng số yêu cầu và không thể phân biệt được số lượng thông báo giao thức.
- Sau khi chạy:master Sự lôi kéo `/result` Nhận báo cáo cuối cùng và kéo nó `/details` Không thể lấy được/Chi tiết yêu cầu chậm.

Vì vậy,worker Số lượng càng nhiều,master Số lượng yêu cầu mặt phẳng điều khiển mỗi giây là khoảng `worker Số lượng`。Khi bật báo cáo hoặc xu hướng theo thời gian thực, mỗi nội dung phản hồi sẽ lớn hơn.worker Cần xây dựng sự tổng hợp report Ảnh chụp nhanh, nhưng sẽ không trả về chi tiết lỗi hoặc danh sách nội dung phản hồi; bạn nên tắt báo cáo thời gian thực nếu cần đối với các bài kiểm tra sức chịu đựng ngắn hoặc các bài kiểm tra sức chịu đựng thông lượng cực cao và chỉ xem các xu hướng và báo cáo cuối cùng.

## worker Ranh giới bộ nhớ và hiệu suất

worker Không lưu tất cả các chi tiết yêu cầu để tránh cao QPS Ký ức thời gian tăng trưởng vô tận:

- Thống kê tổng hợp sử dụng báo chí API / Đối tượng thống kê của kích thước giao thức không lưu tuyến tính phản hồi hoàn chỉnh theo số lượng yêu cầu.
- efficient mode Khi được bật, chi tiết kết quả chỉ giữ lại các yêu cầu không thành công và yêu cầu chậm, ở mức tối đa 1000 dải.
- efficient mode Khi đóng lại, kết quả được chi tiết bằng cách nhấn“Giới hạn số lượng hàng trong bảng kết quả hiệu suất”Giữ lại, bỏ đi những chi tiết cũ nhất sau khi vượt quá.
- Nội dung phản hồi sẽ được cắt theo chính sách xem trước và các trường không bắt buộc để hiển thị bảng kết quả sẽ được đơn giản hóa.
- worker Sau khi hoàn thành run Trạng thái được giữ lại theo mặc định 30 phút, nó sẽ tự động được làm sạch sau khi hết hạn để tránh điều đó `/result` và `/details` Sử dụng bộ nhớ dài hạn.

Khuyến nghị:

- Kiểm tra lưu lượng lớn hoặc áp suất dài hạn được kích hoạt efficient mode。
- Đóng khi không cần chạy báo cáo“Làm mới trong thời gian thực”。
- Không đặt số lượng hàng trong bảng kết quả quá lớn. Khắc phục sự cố lỗi thường chỉ yêu cầu các lỗi gần đây và yêu cầu chậm.
- mỗi đơn vị worker Sử dụng máy kiểm tra áp suất độc lập để tránh GUI、dịch vụ mục tiêu và worker Lấy tài nguyên của cùng một máy.

## Giao diện mặt phẳng điều khiển

| Method | Path | Mô tả |
|---|---|---|
| `GET` | `/api/performance/v1/health` | worker Khám phá. |
| `POST` | `/api/performance/v1/runs` | Gửi một cuộc chạy,body Chứa đầy đủ `plan` và worker của `assignment`。 |
| `GET` | `/api/performance/v1/runs/{runId}?report=false` | Thăm dò trạng thái nhẹ, được sử dụng khi tắt các báo cáo và xu hướng theo thời gian thực. |
| `GET` | `/api/performance/v1/runs/{runId}` | Với sự tổng hợp nhanh chóng report Thăm dò trạng thái, được sử dụng khi bật báo cáo hoặc xu hướng theo thời gian thực. |
| `POST` | `/api/performance/v1/runs/{runId}/stop` | Dừng hoạt động được chỉ định. |
| `GET` | `/api/performance/v1/runs/{runId}/result` | Kéo cuối cùng JSON report。 |
| `GET` | `/api/performance/v1/runs/{runId}/details` | Kéo không thành công/Chi tiết yêu cầu chậm cho GUI Bảng kết quả. |

Các yêu cầu của mặt phẳng điều khiển không được bao gồm trong bài kiểm tra sức chịu đựng report Số lượng yêu cầu và QPS。

## Câu hỏi thường gặp

### Tại sao lại là hai worker Không nhất thiết phải tốt hơn một worker nhanh? .

Kiểm tra sức chịu đựng phân tán chỉ giải quyết được vấn đề này“Nút thắt cuối áp lực”。Nếu nút thắt cổ chai nằm ở dịch vụ đích, vòng lặp mạng,master/worker Cạnh tranh với cùng một máy CPU、Chi phí khởi tạo kiểm tra căng thẳng ngắn, thực thi tập lệnh hoặc HTTP Trên cài đặt kết nối, có rất nhiều worker Không nhất thiết phải nhanh hơn. Ít nhất nên chạy 30 giây trở lên và đặt worker Đặt nó trên một máy riêng để kiểm tra.

### `plan.json` Thời gian tải lên có được tính là thời gian kiểm tra căng thẳng không? .

Không được tính.master gửi `plan + assignment` Nó thuộc về phân bố mặt phẳng điều khiển và không được đưa vào tính toán cuối cùng report của `elapsedTimeMs` và QPS。worker Bảng điều khiển chạy từ  worker Việc thực thi không đồng bộ sẽ bắt đầu quá trình tính toán và có thể chứa worker nội bộ plan Biên dịch, khởi tạo nhóm tập lệnh và ứng dụng phân đoạn.

### worker Có cần phải mở nó ra không? GUI？

Không cần.`performance worker`、`performance run` và `performance master run` Tất cả sẽ đi headless Đường dẫn khởi động, chưa được tạo Swing Cửa sổ chính.
