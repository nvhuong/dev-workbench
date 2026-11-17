# Script API Hướng dẫn tham khảo

Tài liệu này được cung cấp EasyPostman Chức năng tập lệnh đã hoàn tất API Tham khảo. Tất cả API Tất cả đều được biên dịch dựa trên mã thực tế để đảm bảo tính chính xác và khả năng sử dụng.

## Thư mục

- [Đối tượng toàn cầu](#Đối tượng toàn cầu)
- [pm đối tượng](#pm-đối tượng)
- [Postman Cơ sở tương thích](#postman-Cơ sở tương thích)
- [pm.environment - các biến môi trường](#pmenvironment---các biến môi trường)
- [Các biến toàn cục (Global Variables）](#Các biến toàn cầuglobal-variables)
- [pm.variables - Các biến tạm thời](#pmvariables---Các biến tạm thời)
- [pm.request - đối tượng yêu cầu](#pmrequest---đối tượng yêu cầu)
- [pm.response - đối tượng phản hồi](#pmresponse---đối tượng phản hồi)
- [pm.cookies - Cookie quản lý](#pmcookies---cookie-quản lý)
- [pm.expect - khẳng định](#pmexpect---khẳng định)
- [pm.test - Bài kiểm tra](#pmtest---Bài kiểm tra)
- [Tập lệnh nguồn dữ liệu bên ngoài API](#Tập lệnh nguồn dữ liệu bên ngoài-api)
- [console - bảng điều khiển](#console---bảng điều khiển)
- [Tích hợp sẵn JavaScript thư viện](#Tích hợp sẵn-javascript-thư viện)
- [Ví dụ đầy đủ](#Ví dụ đầy đủ)
- [Những điều cần lưu ý](#Những điều cần lưu ý)
- [Tham khảo nhanh](#Tham khảo nhanh)
- [Tài nguyên tham khảo](#Tài nguyên tham khảo)

---

## Đối tượng toàn cầu

### pm

Chủ yếu Postman API Đối tượng cung cấp quyền truy cập vào tất cả các chức năng của tập lệnh.

---

## pm đối tượng

### Danh sách các phương pháp

| phương pháp                                 | Mô tả                 | Ví dụ                                                   |
|------------------------------------|--------------------|------------------------------------------------------|
| `pm.test(name, fn)`                | Xác định một bài kiểm tra             | `pm.test("Mã trạng thái là  200", () => {})`                      |
| `pm.expect(value)`                 | Tạo các khẳng định               | `pm.expect(200).to.equal(200)`                       |
| `pm.uuid()`                        | phát ra UUID            | `pm.uuid()`                                          |
| `pm.getTimestamp()`                | Lấy dấu thời gian hiện tại (mili giây)        | `pm.getTimestamp()`                                  |
| `pm.getResponseCookie(name)`       | Nhận được phản hồi Cookie      | `pm.getResponseCookie('sessionId')`                  |
| `pm.sendRequest(options, callback)` | Gửi thêm trong kịch bản HTTP yêu cầu | `pm.sendRequest({ url: 'https://api.com' }, (err, resp) => {})` |

### Đối tượng nguồn dữ liệu bên ngoài

| đối tượng | Mô tả | Các phương pháp thường được sử dụng |
|------|------|----------|
| `pm.plugin("redis")` | Redis Đọc, viết và truy vấn | `execute(options)`、`query(options)` |
| `pm.plugin("kafka")` | Kafka Truy vấn, gửi tin nhắn và tiêu thụ tin nhắn | `listTopics(options)`、`send(options)`、`poll(options)` |
| `pm.elasticsearch` | Elasticsearch Yêu cầu và thắc mắc | `request(options)`、`query(options)` |
| `pm.influxdb` | InfluxDB Flux / InfluxQL Truy vấn và viết | `query(options)`、`write(options)`、`request(options)` |

### pm.sendRequest()

`pm.sendRequest()` Nó đã được hỗ trợ để chủ động khởi tạo các chức năng bổ sung trong tập lệnh HTTP Yêu cầu, áp dụng để làm mới trong tập lệnh trước token、Các tình huống như kéo chữ ký và làm nóng trước dữ liệu ngữ cảnh.

Chữ ký hàm gọi lại là `(err, response)`：

- `err`：Đối tượng lỗi được trả về khi yêu cầu không thành công, chứa `message`、`name`
- `response`：Khi thành công, đối tượng trình bao bọc phản hồi sẽ được trả về và có thể được sử dụng `response.code`、`response.status`、`response.headers.get()`、`response.text()`、`response.json()`

Các định dạng yêu cầu được hỗ trợ: .

- Trực tiếp đi qua URL Chuỗi, mặc định `GET`
- Đối tượng truyền:`url`、`method`、`header`、`body`
- `body.mode` Hiện đang được hỗ trợ `raw`、`formdata`、`urlencoded`

Ví dụ:

```javascript
pm.sendRequest({
    url: 'https://httpbin.org/get?source=easy-postman&from=pre-script',
    method: 'GET',
    header: {
        'X-Debug-Token': pm.environment.get('debugToken') || 'demo-token'
    }
}, function (err, response) {
    if (err) {
        console.error('Yêu cầu không thành công:', err.message);
        return;
    }

    const data = response.json();
    pm.environment.set('lastHttpbinUrl', data.url || '');
    pm.environment.set('lastHttpbinSource', data.args?source || '');
    console.log('httpbin args:', data.args);
});
```

---

## Postman Cơ sở tương thích

Trình tự thực hiện sau đây“Postman Cùng tên API Ngữ nghĩa đầu tiên,EasyPostman Các phần mở rộng không xung đột”nguyên tắc.
trong bảng dưới đây“Tương thích cốt lõi”Chỉ đề cập đến các đường dẫn được liệt kê và có các bài kiểm tra tự động chứ không phải toàn bộ đường dẫn Postman Các đối tượng được thực hiện từng mục một.

| phạm vi | trạng thái | Mô tả |
|---|---|---|
| `pm.environment`、`pm.globals`、`pm.variables`、`pm.iterationData` | Những đường dẫn thường dùng | Hỗ trợ áp dụng trong từng phạm vi `get/set/has/unset/clear/toObject/replaceIn` |
| `pm.info` | Những đường dẫn thường dùng | Request, iteration and event information;WebSocket Lĩnh vực này là EasyPostman Sự mở rộng |
| `pm.request.method` / `headers` | Tương thích cốt lõi | Sửa đổi vô hướng so với HeaderList Các sửa đổi sẽ được áp dụng cho lần gửi hiện tại |
| `pm.request.url` | Tương thích một phần | `query` PropertyList Có thể đọc và ghi được;`request.update` Hỗ trợ chuỗi và phổ biến Url definition |
| `pm.request.body` | Cấu trúc đối tượng tương thích; được viết dưới dạng phần mở rộng | `raw`、`urlencoded`、`formdata` Đọc,`toString/toJSON/isEmpty` Căn chỉnh Collection SDK；body mutation Là một phần mở rộng tương thích di chuyển |
| `pm.response`、`pm.test`、`pm.expect` | Những đường dẫn thường dùng | Đã đọc phản hồi,HeaderList và được sử dụng phổ biến Chai BDD xích |
| `pm.sendRequest(options, callback)` | Hình thức gọi lại được hỗ trợ | Promise Hình thức trả lại vẫn chưa được thực hiện |
| `pm.cookies` / `pm.cookies.jar()` | Được hỗ trợ một phần | Có sẵn các biểu mẫu đọc, viết và gọi lại thường được sử dụng;Promise và hoàn thành Cookie Store Các tùy chọn không được căn chỉnh đầy đủ |
| `pm.collectionVariables` | Để được thực hiện | bộ sưu tập hiện tại/Nhóm các biến có thể tham gia `pm.variables` và `{{...}}` được phân tích cú pháp, nhưng chưa độc lập API đối tượng |
| `pm.execution`、`pm.visualizer`、`pm.vault`、`pm.require` | Để được thực hiện | Bức tranh lớn `require()` Vâng EasyPostman Đã được mở rộng, không tương đương với `pm.require()` |
| `pm.request.auth` mutation | Để được cải thiện | Header Việc xác thực phương thức có thể được thông qua `pm.request.headers` Ôn lại |

Việc xác định khả năng tương thích được thực hiện theo mức độ ưu tiên sau:

1. hiện tại Postman chính thức Sandbox Tài liệu quyết định công khai kịch bản API hành vi tiêu chuẩn;
2. Collection SDK / Runtime Mã nguồn xác định cấu trúc đối tượng, tuần tự hóa và ngữ nghĩa thời gian chạy lịch sử;
3. Ngoài quan chức hiện tại Sandbox Các khả năng phải được đánh dấu rõ ràng“EasyPostman Sự mở rộng”hoặc“Tương thích di chuyển”，Không thể kết hợp các kết luận tương thích cốt lõi.

Tự động hoàn thành các mục và EasyPostman Bí danh lịch sử không làm gì cả“Đã tương thích rồi”chứng cớ. Phiên bản mã nguồn chính thức cho vòng so sánh này là
Postman Collection SDK `5.3.1`、Runtime `7.56.1`，Và kiểm tra hiện tại cùng một lúc Postman v12 Sandbox Tài liệu.

---

## pm.environment - các biến môi trường

Hoạt động đọc và ghi trên các biến môi trường.

### Danh sách các phương pháp

| phương pháp                | các thông số                      | Giá trị trả về     | Mô tả         | Ví dụ                                      |
|-------------------|-------------------------|---------|------------|-----------------------------------------|
| `get(key)`        | key: String             | String  | Nhận các biến môi trường     | `pm.environment.get('token')`           |
| `set(key, value)` | key: String, value: Any | void    | Đặt các biến môi trường     | `pm.environment.set('token', 'abc123')` |
| `unset(key)`      | key: String             | void    | Loại bỏ các biến môi trường     | `pm.environment.unset('token')`         |
| `has(key)`        | key: String             | Boolean | Kiểm tra xem biến môi trường có tồn tại không | `pm.environment.has('token')`           |
| `clear()`         | Không                       | void    | Xóa tất cả các biến môi trường   | `pm.environment.clear()`                |

---

## Các biến toàn cục (Global Variables）

Biến toàn cầu vượt qua `pm.globals` Quyền truy cập, vòng đời độc lập với yêu cầu hiện tại.

### Lưu ý quan trọng

- ✅ **Hỗ trợ**：`pm.globals.set()` / `pm.globals.get()`
- ✅ **Hỗ trợ**：`pm.globals.has()` / `pm.globals.unset()` / `pm.globals.clear()` / `pm.globals.toObject()`

### Danh sách các phương pháp

| phương pháp | các thông số | Mô tả | Ví dụ |
|------|------|------|------|
| `pm.globals.set(key, value)` | key: String<br>value: Any | Đặt các biến toàn cục | `pm.globals.set('baseUrl', 'https://api.com')` |
| `pm.globals.get(key)` | key: String | Nhận các biến toàn cục | `pm.globals.get('baseUrl')` |
| `pm.globals.has(key)` | key: String | Kiểm tra xem biến toàn cục có tồn tại không | `pm.globals.has('baseUrl')` |
| `pm.globals.unset(key)` | key: String | Xóa các biến toàn cục | `pm.globals.unset('baseUrl')` |
| `pm.globals.clear()` | Không | Xóa các biến toàn cục | `pm.globals.clear()` |

### Ví dụ sử dụng

```javascript
pm.globals.set('apiKey', 'abc123');
pm.globals.set('timeout', 5000);

const apiKey = pm.globals.get('apiKey');
console.log('API Key:', apiKey);
```

---

## pm.variables - Các biến tạm thời

Quản lý biến tạm thời chỉ có hiệu lực trong quá trình thực hiện yêu cầu hiện tại (sẽ không được duy trì).

### Danh sách các phương pháp

| phương pháp                | các thông số                      | Giá trị trả về     | Mô tả              | Ví dụ                                |
|-------------------|-------------------------|---------|-----------------|-----------------------------------|
| `get(key)`        | key: String             | String  | Nhận các biến tạm thời          | `pm.variables.get('userId')`      |
| `set(key, value)` | key: String, value: Any | void    | Đặt các biến tạm thời          | `pm.variables.set('userId', 123)` |
| `has(key)`        | key: String             | Boolean | Kiểm tra xem biến tạm thời có tồn tại không      | `pm.variables.has('userId')`      |
| `unset(key)`      | key: String             | void    | Xóa các biến tạm thời          | `pm.variables.unset('userId')`    |
| `clear()`         | Không                       | void    | Xóa tất cả các biến tạm thời        | `pm.variables.clear()`            |
| `toObject()`      | Không                       | Object  | Nhận tất cả các đối tượng biến tạm thời (cặp khóa-giá trị) | `pm.variables.toObject()`         |

---

## pm.request - đối tượng yêu cầu

Truy cập và vận hành hiện tại HTTP Thông tin được yêu cầu (chủ yếu ở dạng Pre-request được sử dụng trong các tập lệnh).

### tài sản

| tài sản                | loại            | Mô tả                | Ví dụ                           |
|-------------------|---------------|-------------------|------------------------------|
| `id`              | String        | Yêu cầu một mã định danh duy nhất            | `pm.request.id`              |
| `url`             | Url           | yêu cầu hiện tại URL đối tượng              | `pm.request.url`             |
| `method`          | String        | hiện đang được thực thi HTTP Phương pháp (có thể sửa đổi)      | `pm.request.method`          |
| `headers`         | HeaderList    | Danh sách các tiêu đề yêu cầu hiện tại                  | `pm.request.headers`         |
| `body`            | RequestBody   | Đối tượng nội dung yêu cầu hiện tại; trống nếu không có nội dung yêu cầu          | `pm.request.body`            |
| `formData`        | JsListWrapper | EasyPostman Các tập lệnh cũ tương thích với bí danh       | `pm.request.formData`        |
| `urlencoded`      | JsListWrapper | EasyPostman Các tập lệnh cũ tương thích với bí danh       | `pm.request.urlencoded`      |
| `params`          | JsListWrapper | EasyPostman Các tập lệnh cũ tương thích với bí danh       | `pm.request.params`          |
| `isMultipart`     | Boolean       | EasyPostman Gia hạn thời gian thực hiện          | `pm.request.isMultipart`     |
| `followRedirects` | Boolean       | EasyPostman Gia hạn thời gian thực hiện          | `pm.request.followRedirects` |

`pm.request.update(options)`、`pm.request.body`、`pm.request.url.query` và
`pm.request.headers` Theo dõi đầu tiên Postman Collection SDK cấu trúc đối tượng.EasyPostman Phần mở rộng chỉ mang tính bổ sung.
Đừng thay đổi tên Postman API ý nghĩa. Các sửa đổi tập lệnh chỉ ảnh hưởng đến việc thực thi hiện tại và sẽ không ghi đè lên định nghĩa yêu cầu được lưu trong trình chỉnh sửa.

### RequestBody

| tài sản/phương pháp               | Mô tả                                      |
|---------------------|-----------------------------------------|
| `mode`              | Các mẫu cốt lõi đã được chứng minh:`raw`、`urlencoded`、`formdata` |
| `raw`               | raw Thân hình mẫu                                |
| `urlencoded`        | URL Danh sách tham số mã hóa                              |
| `formdata`          | multipart Danh sách tham số                           |
| `update(definition)` | Nhấn Postman RequestBody Các quy tắc được cập nhật; chuỗi được chuyển sang raw người mẫu |
| `toString()`        | raw/urlencoded Biểu diễn chuỗi của văn bản;urlencoded Nhấn Postman QueryParam Dấu phân cách chuẩn hóa quy tắc |
| `toJSON()`          | Postman Collection body definition      |
| `isEmpty()`         | hiện tại body Cho dù nó trống rỗng                            |

Cần phải phân biệt“hiện tại Postman Hợp đồng sản phẩm”và“lịch sử Runtime / Collection SDK Khả năng”：

- hiện tại Postman v12 Sandbox Xử lý tài liệu `pm.request.body` Đánh dấu nó là không thể sửa đổi và nêu rõ điều này `pm` Yêu cầu không được hỗ trợ mutation；
- Collection SDK `RequestBody` Vẫn có sẵn `update()`，Postman Runtime `7.27.0`（2021-03）Tôi cũng đã nuôi nó
  pre-request trong body Những thay đổi được áp dụng cho yêu cầu thực tế;`7.26.x` và các phiên bản trước đó sẽ không được gửi sửa đổi body；
- EasyPostman Tiếp tục hỗ trợ di chuyển các bộ sưu tập cũ và tập lệnh hiện có `body.update(text)`、Ôn lại `.raw`，cũng như
  `pm.request.body = 'text'`。Các khả năng ghi này rõ ràng tương thích với tiện ích mở rộng và không được cho là hiện hành Postman v12 Sandbox tiêu chuẩn.

Vì vậy, nó có thể được thực hiện ở thời điểm hiện tại Postman Và EasyPostman Một tập lệnh mới thay thế trực tiếp giữa . và không nên dựa vào body mutation；
Nếu bạn đang di chuyển các tập lệnh lịch sử, hãy sử dụng chúng trước tiên SDK hình thái `update(text)` hoặc sửa đổi `.raw`，Việc gán một chuỗi trực tiếp chỉ hoạt động như
EasyPostman Khả năng di chuyển thoải mái được giữ lại.

Ranh giới hiện tại: .

- `file` Chế độ chỉ được bảo lưu EasyPostman đường đi cục bộ `src` Khả năng, chưa phù hợp Postman Runtime Sau khi phân tích tập tin `content`。
- GraphQL Việc nhập vẫn sử dụng cái hiện có raw JSON Chuyển đổi, không có tập lệnh nào được khai báo mutation Tương thích.
- `pm.request.update(options)` Chuỗi đã được xác minh URL、Thường được sử dụng Url definition（`protocol/auth/host/port/path/query/hash`）、method、header và body，Nó không có nghĩa là nó đã được thực hiện Request SDK tất cả các lĩnh vực.

### Sửa đổi yêu cầu hiện tại

```javascript
const xmlBody = `<data>${pm.request.body.toString()}</data>`;
pm.request.body.update(xmlBody);
pm.request.update({method: 'PUT'});
pm.request.update({
    url: {
        protocol: 'https',
        host: ['api', 'example', 'com'],
        path: ['v1', 'users'],
        query: [{key: 'page', value: '1'}]
    }
});
pm.request.followRedirects = false;
pm.request.headers.upsert({
    key: 'Content-Type',
    value: 'application/xml; charset=utf-8'
});
```

### URL Các phương thức đối tượng

| phương pháp                   | Giá trị trả về    | Mô tả           | Ví dụ                                  |
|----------------------|--------|--------------|-------------------------------------|
| `toString()`         | String | Nhận đầy đủ URL chuỗi | `pm.request.url.toString()`         |
| `getHost()`          | String | Lấy tên máy chủ        | `pm.request.url.getHost()`          |
| `getPath()`          | String | Nhận được con đường         | `pm.request.url.getPath()`          |
| `getQueryString()`   | String | Lấy chuỗi truy vấn      | `pm.request.url.getQueryString()`   |
| `getPathWithQuery()` | String | Lấy đường dẫn và chuỗi truy vấn   | `pm.request.url.getPathWithQuery()` |

### URL Query đối tượng

Truy cập các tham số truy vấn: .`pm.request.url.query`

| phương pháp             | Giá trị trả về          | Mô tả       | Ví dụ                                                        |
|----------------|--------------|----------|-----------------------------------------------------------|
| `add(item)`    | void         | Thêm tham số truy vấn   | `pm.request.url.query.add({key: 'page', value: '1'})`      |
| `upsert(item)` | Boolean/null | Cập nhật hoặc thêm tham số; trả lại những cái mới `true`，Cập nhật trả về `false` | `pm.request.url.query.upsert({key: 'page', value: '2'})`   |
| `remove(key)`  | void         | Xóa tham số     | `pm.request.url.query.remove('page')`                      |
| `get(key)`     | String       | Lấy giá trị tham số    | `pm.request.url.query.get('page')`                         |
| `has(key)`     | Boolean      | Xác định xem tham số có tồn tại hay không | `pm.request.url.query.has('page')`                         |
| `has(key, value)` | Boolean   | Khớp chặt chẽ tên và giá trị tham số | `pm.request.url.query.has('page', '1')`                    |
| `all()`        | Array        | Nhận tất cả các tham số truy vấn | `pm.request.url.query.all()`                               |
| `one(key)`     | QueryParam   | Lấy đối tượng tham số đã chỉ định | `pm.request.url.query.one('page')`                         |
| `idx(index)`   | QueryParam   | Bấm vào subscript để có được các thông số  | `pm.request.url.query.idx(0)`                              |

### Headers/FormData/Urlencoded/Params Các phương pháp thu thập

Những bộ sưu tập này là `JsListWrapper` Loại, hỗ trợ các phương thức sau: .

| phương pháp                       | các thông số                   | Giá trị trả về     | Mô tả        | Ví dụ                                                           |
|--------------------------|----------------------|---------|-----------|--------------------------------------------------------------|
| `add(item)`              | item: Object         | void    | Thêm một mục      | `pm.request.headers.add({key: 'X-Custom', value: 'test'})`   |
| `remove(key)`            | key: String          | void    | Xóa mục có cùng tên    | `pm.request.headers.remove('X-Custom')`                      |
| `upsert(item)`           | item: Object         | Boolean/null | Cập nhật hoặc chèn một mục; thêm mới `true`、đổi mới `false`、Đầu vào không hợp lệ `null` | `pm.request.headers.upsert({key: 'X-Custom', value: 'new'})` |
| `get(key)`               | key: String          | String  | Lấy giá trị của khóa được chỉ định   | `pm.request.headers.get('Content-Type')`                     |
| `has(key)`               | key: String          | Boolean | Kiểm tra xem khóa được chỉ định có tồn tại không | `pm.request.headers.has('Authorization')`                    |
| `has(key, value)`        | key/value            | Boolean | Kết hợp chặt chẽ các khóa và giá trị   | `pm.request.headers.has('Content-Type', 'application/json')` |
| `all()`                  | Không                    | Array   | Nhận tất cả các mặt hàng     | `pm.request.headers.all()`                                   |
| `one(key)`               | key: String          | Object  | Lấy mục cuối cùng có cùng tên | `pm.request.headers.one('X-Custom')`                         |
| `idx(index)`             | index: Number        | Object  | Nhấn vào đăng ký để nhận vật phẩm   | `pm.request.headers.idx(0)`                                  |
| `count()`                | Không                    | Number  | Lấy số lượng mặt hàng      | `pm.request.headers.count()`                                 |
| `clear()`                | Không                    | void    | Xóa tất cả các mục     | `pm.request.headers.clear()`                                 |
| `each(callback)`         | callback: Function   | void    | qua `(item, index, collection)` Truyền tải; ngoại lệ gọi lại làm gián đoạn tập lệnh | `pm.request.headers.each((h, i) => console.log(i, h))`        |
| `toObject()`             | Không                    | Object  | Chuyển đổi sang đối tượng khóa-giá trị    | `pm.request.headers.toObject()`                              |

### Ví dụ sử dụng

```javascript
// Thêm tiêu đề yêu cầu
pm.request.headers.add({
    key: "Authorization",
    value: "Bearer " + pm.environment.get("token")
});

// Thêm tham số truy vấn (Postman đường dẫn tiêu chuẩn)
pm.request.url.query.add({
    key: "timestamp",
    value: Date.now().toString()
});

// hiện tại body vì formdata Khi thêm các trường
pm.request.body.formdata.add({
    key: "username",
    value: "john",
    type: "text"
});

// Lấy URL thông tin
console.log("Host:", pm.request.url.getHost());
console.log("Path:", pm.request.url.getPath());
```

---

## pm.response - đối tượng phản hồi

thăm HTTP Thông tin phản hồi (chỉ ở dạng  Post-request có sẵn trong kịch bản).

### tài sản

| tài sản             | loại                | Mô tả        | Ví dụ                                           |
|----------------|-------------------|-----------|----------------------------------------------|
| `code`         | Number            | HTTP mã trạng thái  | `pm.response.code`                           |
| `status`       | String            | HTTP Văn bản trạng thái | `pm.response.status`                         |
| `headers`      | Headers           | Đối tượng tiêu đề phản hồi     | `pm.response.headers`                        |
| `responseTime` | Number            | Thời gian đáp ứng (mili giây)  | `pm.response.responseTime`                   |
| `to`           | ResponseAssertion | Hỗ trợ cú pháp xác nhận theo chuỗi  | `pm.response.to.have.status(200)`            |
| `have`         | ResponseAssertion | Hỗ trợ cú pháp xác nhận theo chuỗi  | `pm.response.to.have.header('Content-Type')` |
| `be`           | ResponseAssertion | Hỗ trợ cú pháp xác nhận theo chuỗi  | Được sử dụng cho các cuộc gọi chuỗi                                       |

### Danh sách các phương pháp

| phương pháp       | Giá trị trả về          | Mô tả            | Ví dụ                   |
|----------|--------------|---------------|----------------------|
| `text()` | String       | Nhận văn bản nội dung phản hồi       | `pm.response.text()` |
| `json()` | Object       | Nhận nội dung phản hồi JSON đối tượng | `pm.response.json()` |
| `size()` | ResponseSize | Nhận thông tin kích thước phản hồi      | `pm.response.size()` |

### Phương pháp xác nhận phản hồi

| phương pháp                     | Mô tả          | Ví dụ                                                      |
|------------------------|-------------|---------------------------------------------------------|
| `to.have.status(code)` | Khẳng định mã trạng thái       | `pm.response.to.have.status(200)`                       |
| `to.have.header(name)` | Xác nhận chứa tiêu đề phản hồi     | `pm.response.to.have.header('Content-Type')`            |
| `to.be.below(ms)`      | Khẳng định rằng thời gian phản hồi nhỏ hơn giá trị được chỉ định | `pm.expect(pm.response.responseTime).to.be.below(1000)` |

### Headers Các phương thức đối tượng

Truy cập tiêu đề phản hồi: .`pm.response.headers`

| phương pháp               | các thông số                 | Giá trị trả về     | Mô tả        | Ví dụ                                              |
|------------------|--------------------|---------|-----------|-------------------------------------------------|
| `get(name)`      | name: String       | String  | Nhận giá trị tiêu đề phản hồi    | `pm.response.headers.get('Content-Type')`       |
| `has(name)`      | name: String       | Boolean | Kiểm tra xem tiêu đề phản hồi có tồn tại không | `pm.response.headers.has('Set-Cookie')`         |
| `count()`        | Không                  | Number  | Lấy số lượng tiêu đề phản hồi   | `pm.response.headers.count()`                   |
| `all()`          | Không                  | Array   | Nhận tất cả các tiêu đề phản hồi   | `pm.response.headers.all()`                     |
| `each(callback)` | callback: Function | void    | Lặp lại qua tất cả các tiêu đề phản hồi   | `pm.response.headers.each(h => console.log(h))` |

### ResponseSize đối tượng

`pm.response.size()` Đối tượng được trả về chứa các thuộc tính sau: .

| tài sản       | loại     | Mô tả        |
|----------|--------|-----------|
| `body`   | Number | Kích thước nội dung phản hồi (byte) |
| `header` | Number | Kích thước tiêu đề phản hồi (byte) |
| `total`  | Number | Tổng kích thước (byte)   |

### Ví dụ sử dụng

```javascript
// Nhận dữ liệu phản hồi
const jsonData = pm.response.json();
console.log("Status:", pm.response.status);
console.log("Code:", pm.response.code);

// Khẳng định mã trạng thái
pm.response.to.have.status(200);

// Khẳng định tiêu đề phản hồi
pm.response.to.have.header('Content-Type');

// Nhận tiêu đề phản hồi
const contentType = pm.response.headers.get('Content-Type');

// Nhận kích thước phản hồi
const size = pm.response.size();
console.log("Kích thước cơ thể phản ứng:", size.body, "bytes");
```

---

## pm.cookies - Cookie quản lý

Quản lý và truy cập Cookie。

### Danh sách các phương pháp

| phương pháp            | các thông số                    | Giá trị trả về       | Mô tả                    | Ví dụ                                            |
|---------------|-----------------------|-----------|-----------------------|-----------------------------------------------|
| `get(name)`   | name: String          | Cookie    | Lấy tên được chỉ định Cookie        | `pm.cookies.get('sessionId')`                 |
| `set(cookie)` | cookie: Cookie/String | void      | cài đặt Cookie             | `pm.cookies.set({name: 'key', value: 'val'})` |
| `getAll()`    | Không                     | Array     | Nhận tất cả chúng Cookie           | `pm.cookies.getAll()`                         |
| `has(name)`   | name: String          | Boolean   | nghiên cứu Cookie tồn tại        | `pm.cookies.has('sessionId')`                 |
| `toObject()`  | Không                     | Object    | Nhận tất cả chúng Cookie Đối tượng (ở dạng cặp khóa-giá trị) | `pm.cookies.toObject()`                       |
| `jar()`       | Không                     | CookieJar | Lấy CookieJar đối tượng       | `pm.cookies.jar()`                            |

### CookieJar đối tượng

CookieJar Được sử dụng để quản lý tên miền chéo Cookie，vượt qua `pm.cookies.jar()` Lấy.

#### Danh sách các phương pháp

| phương pháp                           | các thông số                                                         | Mô tả                  | Ví dụ                                      |
|------------------------------|------------------------------------------------------------|---------------------|-----------------------------------------|
| `set(url, cookie, callback)` | url: String<br>cookie: String/Object<br>callback: Function | Cài đặt được chỉ định URL của Cookie   | `jar.set(url, 'key=value', callback)`   |
| `get(url, name, callback)`   | url: String<br>name: String<br>callback: Function          | Nhận được chỉ định URL của Cookie   | `jar.get(url, 'sessionId', callback)`   |
| `getAll(url, callback)`      | url: String<br>callback: Function                          | Nhận được chỉ định URL của tất cả Cookie | `jar.getAll(url, callback)`             |
| `unset(url, name, callback)` | url: String<br>name: String<br>callback: Function          | Xóa đã chỉ định URL của Cookie   | `jar.unset(url, 'sessionId', callback)` |
| `clear(url, callback)`       | url: String<br>callback: Function                          | Xóa bài tập URL của tất cả Cookie | `jar.clear(url, callback)`              |

### Cookie Thuộc tính đối tượng

| tài sản         | loại      | Mô tả            |
|------------|---------|---------------|
| `name`     | String  | Cookie tên     |
| `value`    | String  | Cookie giá trị      |
| `domain`   | String  | Cookie lãnh địa      |
| `path`     | String  | Cookie con đường     |
| `expires`  | String  | Thời gian hết hạn          |
| `maxAge`   | Number  | Thời gian tồn tại tối đa (giây)     |
| `httpOnly` | Boolean | Chỉ có vậy thôi HTTP      |
| `secure`   | Boolean | Liệu có truyền an toàn hay không (HTTPS） |
| `sameSite` | String  | SameSite tài sản   |

### Ví dụ sử dụng

```javascript
// Lấy Cookie
const sessionId = pm.cookies.get('sessionId');
if (sessionId) {
    console.log('Session ID:', sessionId.value);
}

// cài đặt Cookie
pm.cookies.set({
    name: 'myToken',
    value: 'abc123',
    domain: 'example.com',
    path: '/'
});

// nghiên cứu Cookie tồn tại
if (pm.cookies.has('sessionId')) {
    console.log('Session cookie exists');
}

// Nhận tất cả chúng Cookie
const allCookies = pm.cookies.getAll();
console.log('Total cookies:', allCookies.length);

// sử dụng CookieJar Cài đặt tên miền chéo Cookie
const jar = pm.cookies.jar();
jar.set('https://api.example.com', 'token=xyz', (error, cookie) => {
    if (error) {
        console.error('cài đặt cookie thất bại:', error);
    } else {
        console.log('Cookie Thiết lập thành công:', cookie);
    }
});

// sử dụng CookieJar Lấy Cookie
jar.get('https://api.example.com', 'token', (error, cookie) => {
    if (!error && cookie) {
        console.log('Token:', cookie.value);
    }
});
```

---

## pm.expect - khẳng định

Kiểm tra bằng cách sử dụng các xác nhận theo chuỗi (class Chai.js phong cách).

### Hỗ trợ cú pháp chuỗi

| Thuộc tính bị xích   | Mô tả    |
|--------|-------|
| `to`   | Liên kết chuỗi |
| `be`   | Liên kết chuỗi |
| `have` | Liên kết chuỗi |

### Các phương pháp xác nhận được hỗ trợ

| khẳng định                   | các thông số                           | Mô tả                   | Ví dụ                                             |
|----------------------|------------------------------|----------------------|------------------------------------------------|
| `equal(value)`       | value: Any                   | Bình đẳng nghiêm ngặt (so sánh sâu sắc)           | `pm.expect(200).to.equal(200)`                 |
| `eql(value)`         | value: Any                   | Độ sâu bằng nhau (với equal như nhau)     | `pm.expect({a: 1}).to.eql({a: 1})`             |
| `include(substring)` | substring: String            | Chứa các chuỗi con                 | `pm.expect('hello world').to.include('hello')` |
| `property(key)`      | key: String                  | Chứa các thuộc tính (chỉ được hỗ trợ Map/Object） | `pm.expect(obj).to.have.property('id')`        |
| `match(regex)`       | regex: String/Pattern/RegExp | Khớp biểu thức chính quy              | `pm.expect('hello').to.match(/^h/)`            |
| `below(number)`      | number: Number               | Giá trị nhỏ hơn giá trị được chỉ định              | `pm.expect(5).to.be.below(10)`                 |

### Ví dụ sử dụng

```javascript
// Khẳng định đẳng thức cơ bản
pm.test("Status code is 200", function () {
    pm.expect(pm.response.code).to.equal(200);
});

// Khẳng định đẳng thức sâu sắc
pm.test("Response data matches", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.eql({status: "success"});
});

// Chứa các chuỗi con
pm.test("Response contains success", function () {
    pm.expect(pm.response.text()).to.include("success");
});

// Tài sản tồn tại
pm.test("Response has userId property", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('userId');
});

// Trận đấu thường lệ
pm.test("Email format is correct", function () {
    pm.expect(email).to.match(/^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/);
});

// So sánh số
pm.test("Response time is acceptable", function () {
    pm.expect(pm.response.responseTime).to.be.below(1000);
});
```

### Những điều cần lưu ý

- Việc triển khai hiện tại hỗ trợ các phương pháp xác nhận có giới hạn, chủ yếu bao gồm:equal、eql、include、property、match、below
- Không được hỗ trợ:above、least、most、within、length、keys、members、true、false、null、undefined、ok、empty Đợi đã
- Nếu bạn cần thêm chức năng xác nhận thì nên sử dụng pm.test Kết hợp với sự đơn giản if phán xét

---

## pm.test - Bài kiểm tra

Xác định và quản lý các trường hợp thử nghiệm.

### Các phương pháp chính

#### pm.test(name, function)

Xác định một trường hợp thử nghiệm.

| các thông số         | loại       | Mô tả                                   |
|------------|----------|--------------------------------------|
| `name`     | String   | Tên thử nghiệm                                 |
| `function` | Function | Chức năng kiểm tra (có thể được sử dụng pm.expect hoặc pm.response khẳng định) |

### Ví dụ sử dụng

```javascript
// Xác định các bài kiểm tra - Kiểm tra mã trạng thái
pm.test("Mã trạng thái là  200", function () {
    pm.response.to.have.status(200);
});

// Xác định các bài kiểm tra - sử dụng pm.expect
pm.test("Thời gian đáp ứng ít hơn 500ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

// Xác định các bài kiểm tra - JSON Xác thực dữ liệu
pm.test("Phản hồi có chứa người dùng ID", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('userId');
    pm.expect(jsonData.userId).to.equal(123);
});

// Xác định các bài kiểm tra - Kiểm tra tiêu đề phản hồi
pm.test("Câu trả lời có chứa Content-Type", function () {
    pm.response.to.have.header('Content-Type');
});
```

---

## Tập lệnh nguồn dữ liệu bên ngoài API

Có thể vào Pre-request Script hoặc Test Script truy cập trực tiếp Redis、Kafka、Elasticsearch、InfluxDB，và kết hợp `pm.test()` / `pm.expect()` Hãy khẳng định.

### pm.plugin("redis")

#### Danh sách các phương pháp

| phương pháp | các thông số | Giá trị trả về | Mô tả |
|------|------|--------|------|
| `execute(options)` | `host`、`port`、`db`、`command`、`key`、`args`、`value` | Any | Thực hiện một lần Redis Đặt hàng |
| `query(options)` | Giống nhau `execute(options)` | Any | `execute()` bí danh |

#### Ví dụ: Viết và khẳng định Redis

```javascript
const redis = pm.plugin('redis');

redis.execute({
    host: 'localhost',
    port: 6379,
    db: 0,
    command: 'SET',
    key: 'user:1001',
    value: JSON.stringify({id: 1001, name: 'Alice'})
});

const redisValue = redis.query({
    host: 'localhost',
    port: 6379,
    db: 0,
    command: 'GET',
    key: 'user:1001'
});

pm.test('Redis Việc viết đã thành công', function () {
    pm.expect(redisValue).to.include('Alice');
});
```

### pm.plugin("kafka")

#### Danh sách các phương pháp

| phương pháp | các thông số | Giá trị trả về | Mô tả |
|------|------|--------|------|
| `listTopics(options)` | `bootstrapServers` Đợi đã | `Array<String>` | Lấy topic danh sách |
| `send(options)` | `bootstrapServers`、`topic`、`key`、`value`、`headers` | Object | Gửi tin nhắn |
| `poll(options)` | `bootstrapServers`、`topic`、`groupId`、`pollTimeoutMs` Đợi đã | Array | Kéo tin nhắn |

#### Ví dụ: Gửi tin nhắn và khẳng định Kafka

```javascript
const kafka = pm.plugin('kafka');

const sendResp = kafka.send({
    bootstrapServers: 'localhost:9092',
    topic: 'orders',
    key: 'order-1001',
    value: JSON.stringify({orderId: 1001, status: 'CREATED'})
});

pm.test('Kafka Đã gửi thành công', function () {
    pm.expect(sendResp.topic).to.equal('orders');
    pm.expect(sendResp.offset).to.be.least(0);
});
```

### pm.elasticsearch

#### Danh sách các phương pháp

| phương pháp | các thông số | Giá trị trả về | Mô tả |
|------|------|--------|------|
| `request(options)` | `baseUrl`、`method`、`path`、`body`、`headers` | Object | Thực hiện một lần ES HTTP yêu cầu |
| `query(options)` | Giống nhau `request(options)` | Object | `request()` bí danh |

#### Ví dụ: Viết và khẳng định Elasticsearch

```javascript
const indexResp = pm.elasticsearch.request({
    baseUrl: 'http://localhost:9200',
    method: 'POST',
    path: '/orders/_doc/order-1001',
    body: JSON.stringify({orderId: 1001, status: 'CREATED'})
});

pm.test('ES Việc viết đã thành công', function () {
    pm.expect(indexResp.code).to.be.within(200, 201);
});
```

### pm.influxdb

Hai chế độ được hỗ trợ:

- `version: 'v1'` hoặc `mode: 'influxql'`：sử dụng InfluxQL（Để lại theo mặc định `/query`、`/write`）
- `version: 'v2'` hoặc `mode: 'flux'`：sử dụng Flux / InfluxDB 2.x（Đi `/api/v2/query`、`/api/v2/write`）

#### Danh sách các phương pháp

| phương pháp | các thông số | Giá trị trả về | Mô tả |
|------|------|--------|------|
| `query(options)` | `baseUrl`、`version`、`db/org`、`query`、`token` Đợi đã | Object | Truy vấn InfluxDB |
| `write(options)` | `baseUrl`、`version`、`db/org/bucket`、`lineProtocol`、`precision` | Object | Viết line protocol dữ liệu |
| `request(options)` | `baseUrl`、`path`、`method`、`body`、`headers` | Object | Gửi bản gốc HTTP yêu cầu |

#### Các thông số thường dùng

| các thông số | Phiên bản áp dụng | Mô tả |
|------|----------|------|
| `baseUrl` | v1 / v2 | InfluxDB Địa chỉ, mặc định `http://localhost:8086` |
| `version` / `mode` | v1 / v2 | `v1` / `influxql` hoặc `v2` / `flux` |
| `db` / `database` | v1 | Tên cơ sở dữ liệu |
| `org` | v2 | Tên tổ chức |
| `bucket` | v2 Viết | bucket tên |
| `token` | v2 | `Authorization: Token xxx` |
| `username` / `password` | v1 | vượt qua `u/p` Truyền tham số |
| `query` | v1 / v2 | InfluxQL hoặc Flux câu lệnh truy vấn |
| `lineProtocol` | v1 / v2 Viết | được viết line protocol văn bản |
| `precision` | v1 / v2 Viết | Độ chính xác về thời gian, mặc định `ms` |

#### Ví dụ:InfluxDB 1.x Truy vấn và khẳng định

```javascript
const queryResp = pm.influxdb.query({
    baseUrl: 'http://localhost:8086',
    version: 'v1',
    db: 'metrics',
    username: 'root',
    password: 'root',
    query: 'SELECT * FROM cpu ORDER BY time DESC LIMIT 1'
});

pm.test('InfluxQL Truy vấn thành công', function () {
    pm.expect(queryResp.code).to.equal(200);
    pm.expect(queryResp.json).to.exist();
});
```

#### Ví dụ:InfluxDB 1.x Viết và khẳng định

```javascript
const writeResp = pm.influxdb.write({
    baseUrl: 'http://localhost:8086',
    version: 'v1',
    db: 'metrics',
    username: 'root',
    password: 'root',
    precision: 'ms',
    lineProtocol: 'api_requests,service=order count=1i ' + Date.now()
});

pm.test('Influx Việc viết đã thành công', function () {
    pm.expect(writeResp.code).to.equal(204);
});
```

#### Ví dụ:InfluxDB 2.x Flux Truy vấn

```javascript
const resp = pm.influxdb.query({
    baseUrl: 'http://localhost:8086',
    version: 'v2',
    org: 'demo-org',
    token: pm.environment.get('influxToken'),
    query: 'from(bucket: "metrics") |> range(start: -5m) |> limit(n: 1)'
});

pm.test('Influx Flux Truy vấn thành công', function () {
    pm.expect(resp.code).to.equal(200);
    pm.expect(resp.body).to.include('_value');
});
```

---

## console - bảng điều khiển

Thông tin gỡ lỗi đầu ra.

### Danh sách các phương pháp

| phương pháp                    | các thông số           | Mô tả   | Ví dụ                                |
|-----------------------|--------------|------|-----------------------------------|
| `log(message, ...)`   | message: Any | Nhật ký đầu ra | `console.log('Hello', 'World')`   |
| `info(message, ...)`  | message: Any | Thông tin đầu ra | `console.info('Info message')`    |
| `warn(message, ...)`  | message: Any | Xuất ra một cảnh báo | `console.warn('Warning message')` |
| `error(message, ...)` | message: Any | Lỗi đầu ra | `console.error('Error message')`  |

---

## Tích hợp sẵn JavaScript thư viện

EasyPostman Ba cái thường được sử dụng được tích hợp sẵn JavaScript thư viện, những thư viện này**Được tải sẵn vào phạm vi toàn cầu**，Có thể sử dụng trực tiếp, không cần thiết `require()`。

### Danh sách các thư viện được hỗ trợ

| Tên thư viện         | Tên biến toàn cầu       | Phiên bản       | Mô tả                                    | Tài liệu chính thức                                      |
|------------|-------------|----------|---------------------------------------|--------------------------------------------|
| crypto-js  | `CryptoJS`  | 4.1.1    | Thư viện tiền điện tử, được hỗ trợ AES、DES、MD5、SHA、HMAC và các thuật toán mã hóa khác | [crypto-js](https://cryptojs.gitbook.io/docs/) |
| lodash     | `_`         | 4.17.21  | JavaScript Một thư viện công cụ thực tế cung cấp nhiều phương pháp phụ trợ lập trình chức năng      | [lodash](https://lodash.com/docs/)          |
| moment     | `moment`    | 2.29.4   | Thư viện xử lý ngày và giờ để định dạng, phân tích cú pháp và thao tác ngày giờ             | [moment.js](https://momentjs.com/docs/)     |

### Cách sử dụng

```javascript
// ✅ Khuyến nghị: Sử dụng trực tiếp các biến toàn cục (không cần require）
var hash = CryptoJS.MD5('message').toString();
var randomNum = _.random(1, 100);
var now = moment().format('YYYY-MM-DD HH:mm:ss');

// ✅ Cũng được hỗ trợ: sử dụng require() trọng tải
var CryptoJS = require('crypto-js');
var _ = require('lodash');
var moment = require('moment');
```

### `require()` Tải cục bộ / Kịch bản web

Ngoài các thư viện tích hợp sẵn, các tập lệnh còn hỗ trợ chuyển `require()` Tải của riêng bạn JavaScript tập tin.

```javascript
// 1. Tải tập lệnh đường dẫn tuyệt đối cục bộ
var signer = require('/absolute/path/to/signer.js');

// 2. Tải tập lệnh đường dẫn tương đối
// trong tập lệnh cấp cao nhất ./ và ../ Theo mặc định, nó được giải quyết tương ứng với thư mục không gian làm việc hiện tại
var helper = require('./scripts/helper.js');

// 3. Các tập lệnh cục bộ tiếp tục tham chiếu các phần phụ thuộc tương đối trong nội bộ
// parent.js -> require('./child.js')
var parent = require('/absolute/path/to/parent.js');
```

Các ví dụ làm sẵn có thể được kiểm tra trực tiếp :

```javascript
// Kho lưu trữ có các ví dụ cục bộ tích hợp.local-parent.js Nó sẽ tiếp tục trong nội bộ require('./local-child.js')
// Nó có thể được thực thi trực tiếp khi không gian làm việc hiện tại trỏ đến thư mục gốc của kho
var localDemo = require('./docs/examples/js-require/local-parent.js');
console.log(localDemo.describe('easy-postman'));
console.log(localDemo.marker());
```

Tập lệnh tùy chỉnh được khuyến khích CommonJS Xuất kiểu: .

```javascript
function sign(input) {
    return input + '-signed';
}

module.exports = { sign };
```

Tập lệnh mạng mặc định**Đóng lại**，Cần phải ở đó“cài đặt -> Yêu cầu cài đặt -> Đã tải tập lệnh”Được kích hoạt rõ ràng trong “Cho phép tải tập lệnh từ mạng”Chỉ sau đó tải được cho phép.

```javascript
// Hãy đến trước“cài đặt -> Yêu cầu cài đặt -> Đã tải tập lệnh”Kích hoạt tập lệnh từ xa
var dayjs = require('https://cdn.jsdelivr.net/npm/dayjs@1.11.13/dayjs.min.js');
console.log(dayjs('2026-03-27').add(1, 'day').format('YYYY-MM-DD'));
```

Trong cùng một bộ cài đặt, bạn cũng có thể kiểm soát:

- Nó có được phép không? `http://` Tập lệnh từ xa (tắt theo mặc định, chỉ được khuyến nghị để gỡ lỗi cục bộ)
- Danh sách trắng máy chủ tập lệnh từ xa
- Đã hết thời gian kết nối / Đọc thời gian chờ
- Kích thước tối đa của một tập lệnh từ xa

Lưu ý:

- Theo mặc định, đường dẫn tương đối cấp cao nhất được giải quyết tương ứng với thư mục không gian làm việc hiện tại; nếu không gian làm việc hiện tại không khả dụng, nó sẽ quay trở lại thư mục hiện tại của quy trình.
- Sau khi mỗi lần thực thi tập lệnh kết thúc,`require()` Bộ đệm mô-đun sẽ bị xóa, vì vậy các tệp cục bộ sẽ được tải lại vào lần tiếp theo chúng được thực thi sau khi sửa đổi.
- Về cơ bản, kịch bản web vẫn vậy“Thực thi mã sau khi tải xuống”，Bạn chỉ nên tải các nguồn tập lệnh mà bạn tin cậy.

### crypto-js - Thư viện tiền điện tử

Cung cấp nhiều thuật toán mã hóa và băm khác nhau, thường được sử dụng trong các tình huống như tạo chữ ký và mã hóa dữ liệu nhạy cảm.

#### Các chức năng thường dùng

| Chức năng          | phương pháp                                  | Mô tả            | Ví dụ                                                   |
|-------------|-------------------------------------|---------------|------------------------------------------------------|
| MD5 Băm      | `CryptoJS.MD5(message)`             | phát ra MD5 Băm     | `CryptoJS.MD5('text').toString()`                    |
| SHA1 Băm     | `CryptoJS.SHA1(message)`            | phát ra SHA1 Băm    | `CryptoJS.SHA1('text').toString()`                   |
| SHA256 Băm   | `CryptoJS.SHA256(message)`          | phát ra SHA256 Băm  | `CryptoJS.SHA256('text').toString()`                 |
| HMAC-SHA256 | `CryptoJS.HmacSHA256(message, key)` | HMAC dấu hiệu       | `CryptoJS.HmacSHA256('data', 'secret').toString()`   |
| AES Mã hóa      | `CryptoJS.AES.encrypt(msg, key)`    | AES Mã hóa đối xứng      | `CryptoJS.AES.encrypt('text', 'secret').toString()`  |
| AES Giải mã      | `CryptoJS.AES.decrypt(cipher, key)` | AES Giải mã        | `CryptoJS.AES.decrypt(cipher, 'secret').toString()`  |
| Base64 Mã hóa   | `CryptoJS.enc.Base64.stringify()`   | Base64 Mã hóa     | `CryptoJS.enc.Base64.stringify(wordArray)`           |
| Byte ngẫu nhiên        | `CryptoJS.lib.WordArray.random(n)`  | phát ra n Byte dữ liệu ngẫu nhiên   | `CryptoJS.lib.WordArray.random(16).toString()`       |

#### Ví dụ sử dụng

```javascript
// Sử dụng trực tiếp các biến toàn cục CryptoJS（Không cần require）

// MD5 Băm
var md5 = CryptoJS.MD5('password123').toString();
pm.environment.set('passwordHash', md5);

// HMAC-SHA256 dấu hiệu
var data = 'userId=123&timestamp=' + Date.now();
var signature = CryptoJS.HmacSHA256(data, 'secret-key').toString();
pm.environment.set('signature', signature);

// AES Mã hóa và giải mã
var encrypted = CryptoJS.AES.encrypt('sensitive data', 'my-key').toString();
var decrypted = CryptoJS.AES.decrypt(encrypted, 'my-key').toString(CryptoJS.enc.Utf8);
```

### lodash - Thư viện công cụ

Cung cấp các phương thức hoạt động thực tế cho các kiểu dữ liệu như mảng, đối tượng và chuỗi để đơn giản hóa logic xử lý dữ liệu.

#### Các chức năng thường dùng

| phân loại   | phương pháp                          | Mô tả           | Ví dụ                                      |
|------|-----------------------------|--------------|-----------------------------------------|
| mảng   | `_.random(min, max)`        | Tạo số ngẫu nhiên        | `_.random(1, 100)`                      |
|      | `_.shuffle(array)`          | Xáo trộn mảng         | `_.shuffle([1, 2, 3])`                  |
|      | `_.sample(array)`           | Chọn ngẫu nhiên một phần tử      | `_.sample(['a', 'b', 'c'])`             |
|      | `_.uniq(array)`             | Chống trùng lặp mảng         | `_.uniq([1, 2, 2, 3])`                  |
| tập trung   | `_.map(collection, fn)`     | Chuyển đổi bản đồ         | `_.map([1,2], n => n*2)`                |
|      | `_.filter(collection, fn)`  | lọc           | `_.filter([1,2,3], n => n>1)`           |
|      | `_.find(collection, fn)`    | Tìm trận đấu đầu tiên     | `_.find(users, {role: 'admin'})`        |
|      | `_.groupBy(collection, fn)` | Nhóm           | `_.groupBy(users, 'role')`              |
|      | `_.sortBy(collection, fn)`  | Loại           | `_.sortBy(users, 'age')`                |
| đối tượng   | `_.pick(object, keys)`      | Trích xuất các thuộc tính được chỉ định       | `_.pick(user, ['id', 'name'])`          |
|      | `_.omit(object, keys)`      | Loại trừ các thuộc tính được chỉ định       | `_.omit(user, ['password'])`            |
|      | `_.merge(obj1, obj2)`       | Hợp nhất sâu các đối tượng       | `_.merge({a:1}, {b:2})`                 |
|      | `_.cloneDeep(object)`       | Nhân bản sâu         | `_.cloneDeep(complexObject)`            |
| chuỗi  | `_.camelCase(string)`       | Đặt tên bướu        | `_.camelCase('hello-world')`            |
|      | `_.snakeCase(string)`       | Được đặt tên theo hình con rắn        | `_.snakeCase('helloWorld')`             |
|      | `_.capitalize(string)`      | Viết hoa chữ cái đầu tiên        | `_.capitalize('hello')`                 |
| khác   | `_.times(n, fn)`            | thực hiện n lần       | `_.times(3, i => console.log(i))`       |
|      | `_.debounce(fn, wait)`      | Chức năng chống rung         | `_.debounce(func, 300)`                 |

#### Ví dụ sử dụng

```javascript
// Sử dụng trực tiếp các biến toàn cục _（Không cần require）

// Tạo dữ liệu thử nghiệm ngẫu nhiên
var userId = _.random(10000, 99999);
var status = _.sample(['pending', 'approved', 'rejected']);

// Hoạt động mảng
var nums = [1, 2, 3, 4, 5];
var doubled = _.map(nums, n => n * 2);
var filtered = _.filter(nums, n => n > 2);

// Hoạt động đối tượng
var user = {id: 1, name: 'John', password: 'secret', age: 30};
var safeUser = _.omit(user, ['password']);
pm.environment.set('user', JSON.stringify(safeUser));
```

### moment - Thư viện ngày và giờ

Thư viện xử lý ngày và giờ mạnh mẽ hỗ trợ định dạng, phân tích cú pháp, tính toán và xác thực ngày tháng.

#### Các chức năng thường dùng

| Chức năng       | phương pháp                                  | Mô tả             | Ví dụ                                          |
|----------|-------------------------------------|----------------|---------------------------------------------|
| Lấy thời gian hiện tại   | `moment()`                          | Tạo đối tượng thời gian hiện tại       | `moment()`                                  |
| Định dạng      | `moment().format(format)`           | Định dạng ngày          | `moment().format('YYYY-MM-DD HH:mm:ss')`    |
| ISOđịnh dạng    | `moment().toISOString()`            | thay đổi ISO 8601 định dạng  | `moment().toISOString()`                    |
| dấu thời gian      | `moment().valueOf()`                | Lấy dấu thời gian tính bằng mili giây        | `moment().valueOf()`                        |
|          | `moment().unix()`                   | Lấy dấu thời gian cấp hai        | `moment().unix()`                           |
| Phân tích ngày     | `moment(str, format)`               | Phân tích một chuỗi thành một ngày       | `moment('2024-01-01', 'YYYY-MM-DD')`        |
| Phép cộng và phép trừ ngày tháng     | `moment().add(n, unit)`             | Thêm thời gian           | `moment().add(7, 'days')`                   |
|          | `moment().subtract(n, unit)`        | Giảm thời gian           | `moment().subtract(1, 'months')`            |
| So sánh ngày     | `moment().isBefore(date)`           | Cho dù trước đây          | `moment().isBefore('2025-01-01')`           |
|          | `moment().isAfter(date)`            | Liệu sau này          | `moment().isAfter('2023-01-01')`            |
|          | `moment().isSame(date)`             | Họ có giống nhau không?           | `moment().isSame('2024-01-01', 'day')`      |
| Sự khác biệt về thời gian      | `moment().diff(date, unit)`         | Tính toán chênh lệch thời gian          | `moment().diff('2024-01-01', 'days')`       |
| bắt đầu/thời gian kết thúc | `moment().startOf(unit)`            | Lấy thời gian bắt đầu của thiết bị       | `moment().startOf('day')`                   |
|          | `moment().endOf(unit)`              | Nhận thời gian kết thúc đơn vị       | `moment().endOf('month')`                   |
| Xác minh       | `moment(str, format, true).isValid()` | Xác minh rằng ngày đó là hợp lệ       | `moment('2024-13-01', 'YYYY-MM-DD', true).isValid()` |

#### Ví dụ sử dụng

```javascript
// Sử dụng trực tiếp các biến toàn cục moment（Không cần require）

// Tạo các định dạng thời gian khác nhau
pm.environment.set('currentDate', moment().format('YYYY-MM-DD'));
pm.environment.set('currentTime', moment().format('YYYY-MM-DD HH:mm:ss'));
pm.environment.set('timestamp', moment().valueOf().toString());
pm.environment.set('isoTime', moment().toISOString());

// Tính toán ngày
var tomorrow = moment().add(1, 'days').format('YYYY-MM-DD');
var lastMonth = moment().subtract(1, 'months').format('YYYY-MM');
var startOfDay = moment().startOf('day').valueOf();
var endOfDay = moment().endOf('day').valueOf();
```

### Ví dụ về cách sử dụng kết hợp

```javascript
// Sử dụng trực tiếp các biến toàn cục (không cần require）
// CryptoJS、_（lodash）、moment Tất cả đều được tải sẵn

// Tạo một bản đã ký API Yêu cầu tham số
var params = {
    userId: pm.environment.get('userId') || '123',
    timestamp: moment().valueOf().toString(),
    nonce: CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Hex),
    action: 'getUserInfo'
};

// Sắp xếp và nối các chuỗi chữ ký theo tên khóa
var sortedKeys = _.keys(params).sort();
var signString = _.map(sortedKeys, key => key + '=' + params[key]).join('&');

// phát ra HMAC-SHA256 dấu hiệu
var secretKey = pm.environment.get('secretKey') || 'default-secret';
var signature = CryptoJS.HmacSHA256(signString, secretKey).toString();

// Lưu vào các biến môi trường
pm.environment.set('requestTimestamp', params.timestamp);
pm.environment.set('requestNonce', params.nonce);
pm.environment.set('requestSignature', signature);

console.log('Các tham số yêu cầu đã sẵn sàng');
console.log('Chuỗi chữ ký:', signString);
console.log('dấu hiệu:', signature);
```

---

## Ví dụ đầy đủ

### Pre-request Script Ví dụ

#### Ví dụ 1：Chuẩn bị yêu cầu cơ bản

```javascript
// 1. Đặt các biến môi trường
pm.environment.set('timestamp', Date.now());
pm.environment.set('requestId', pm.uuid());

// 2. Đặt các biến tạm thời
pm.variables.set('localVar', 'tempValue');

// 3. Thêm tiêu đề yêu cầu
pm.request.headers.add({
    key: 'X-Request-Time',
    value: new Date().toISOString()
});

pm.request.headers.add({
    key: 'X-Request-ID',
    value: pm.environment.get('requestId')
});

// 4. Thêm tham số truy vấn
pm.request.params.add({
    key: 'timestamp',
    value: pm.getTimestamp().toString()
});

// 5. Ôn lại URL Mã hóa dữ liệu biểu mẫu
pm.request.urlencoded.add({
    key: 'username',
    value: 'john'
});

// 6. Thêm vào multipart Dữ liệu biểu mẫu
pm.request.formData.add({
    key: 'userId',
    value: '123'
});

// 7. Thông tin gỡ lỗi đầu ra
console.log('Request URL:', pm.request.url.toString());
console.log('Request Method:', pm.request.method);
console.log('Request ID:', pm.environment.get('requestId'));
```

#### Ví dụ 2：JWT Token Chứng nhận

```javascript
// nghiên cứu token tồn tại
const token = pm.environment.get('authToken');

if (token) {
    // Thêm vào Bearer Token
    pm.request.headers.upsert({
        key: 'Authorization',
        value: 'Bearer ' + token
    });
    console.log('Đã thêm chứng nhận Token');
} else {
    console.warn('Cảnh báo: Không tìm thấy chứng nhận Token，Vui lòng đăng nhập trước');
}

// Thêm vào API Key（nếu cần)
const apiKey = pm.environment.get('apiKey');
if (apiKey) {
    pm.request.headers.upsert({
        key: 'X-API-Key',
        value: apiKey
    });
}
```

#### Ví dụ 3：Tự động tạo chữ ký (HMAC-SHA256）

```javascript
var CryptoJS = require('crypto-js');

// Nhận thông tin yêu cầu
const timestamp = Date.now().toString();
const method = pm.request.method;
const path = pm.request.url.getPath();
const secretKey = pm.environment.get('secretKey') || 'default-secret';

// Tạo chuỗi chữ ký
const signString = method + '\n' + path + '\n' + timestamp;
console.log('Chuỗi chữ ký:', signString);

// Tính toán HMAC-SHA256 dấu hiệu
const signature = CryptoJS.HmacSHA256(signString, secretKey).toString();
console.log('Chữ ký được tạo:', signature);

// Thêm tiêu đề yêu cầu liên quan đến chữ ký
pm.request.headers.upsert({
    key: 'X-Timestamp',
    value: timestamp
});

pm.request.headers.upsert({
    key: 'X-Signature',
    value: signature
});

pm.request.headers.upsert({
    key: 'X-App-Id',
    value: pm.environment.get('appId') || 'default-app'
});
```

#### Ví dụ 4：Trình tạo dữ liệu động

```javascript
var moment = require('moment');
var _ = require('lodash');

// Tạo dữ liệu người dùng ngẫu nhiên
const randomUser = {
    id: pm.uuid(),
    username: 'user_' + _.random(10000, 99999),
    email: 'test_' + Date.now() + '@example.com',
    phone: '138' + _.random(10000000, 99999999),
    createTime: moment().format('YYYY-MM-DD HH:mm:ss'),
    age: _.random(18, 60)
};

// Lưu vào các biến môi trường để sử dụng tiếp theo
pm.environment.set('testUserId', randomUser.id);
pm.environment.set('testUserEmail', randomUser.email);
pm.environment.set('testUsername', randomUser.username);

// trong trường hợp JSON Phần thân yêu cầu có thể được sửa đổi linh hoạt
console.log('Đã tạo người dùng thử nghiệm:', JSON.stringify(randomUser, null, 2));

// Tạo số thứ tự ngẫu nhiên
const orderId = 'ORD' + moment().format('YYYYMMDDHHmmss') + _.random(1000, 9999);
pm.environment.set('testOrderId', orderId);
console.log('Số thứ tự:', orderId);
```

#### Ví dụ 5：Điều kiện yêu cầu được sửa đổi

```javascript
// Xác định cấu hình yêu cầu dựa trên các biến môi trường
const env = pm.environment.get('currentEnv') || 'dev';

// Các cài đặt khác nhau tùy thuộc vào môi trường baseURL
const baseUrls = {
    'dev': 'https://dev-api.example.com',
    'test': 'https://test-api.example.com',
    'prod': 'https://api.example.com'
};

pm.environment.set('baseUrl', baseUrls[env]);
console.log('môi trường hiện nay:', env, '- API địa chỉ:', baseUrls[env]);

// Thêm các tiêu đề yêu cầu khác nhau dựa trên phương thức yêu cầu
if (pm.request.method === 'POST' || pm.request.method === 'PUT') {
    pm.request.headers.upsert({
        key: 'Content-Type',
        value: 'application/json'
    });
}

// Thêm tham số bổ sung cho các đường dẫn cụ thể
if (pm.request.url.getPath().includes('/api/v2/')) {
    pm.request.params.add({
        key: 'version',
        value: '2.0'
    });
}
```

#### Ví dụ 6：Yêu cầu xác minh dữ liệu

```javascript
// Kiểm tra các biến môi trường cần thiết
const requiredVars = ['baseUrl', 'apiKey', 'userId'];
const missingVars = [];

requiredVars.forEach(function(varName) {
    if (!pm.environment.get(varName)) {
        missingVars.push(varName);
    }
});

if (missingVars.length > 0) {
    console.error('Lỗi: Thiếu biến môi trường bắt buộc:', missingVars.join(', '));
    throw new Error('Biến môi trường bị thiếu: ' + missingVars.join(', '));
}

// Kiểm tra xem tiêu đề yêu cầu đã hoàn tất chưa
if (!pm.request.headers.has('Content-Type')) {
    console.warn('Cảnh báo: Thiếu Content-Type Tiêu đề yêu cầu');
}

console.log('✓ Đã thông qua xác minh biến môi trường');
```

#### Ví dụ 7：Hoạt động hàng loạt và xử lý dữ liệu

```javascript
var _ = require('lodash');

// Thêm tiêu đề yêu cầu tùy chỉnh theo đợt
const customHeaders = [
    { key: 'X-Client-Version', value: '1.0.0' },
    { key: 'X-Platform', value: 'web' },
    { key: 'X-Device-ID', value: pm.environment.get('deviceId') || pm.uuid() },
    { key: 'X-Session-ID', value: pm.environment.get('sessionId') || pm.uuid() }
];

customHeaders.forEach(function(header) {
    pm.request.headers.upsert(header);
});

// Thêm tham số truy vấn theo lô
const commonParams = {
    'appId': pm.environment.get('appId') || 'default',
    'lang': 'zh-CN',
    'timezone': 'Asia/Shanghai',
    'platform': 'web'
};

_.forEach(commonParams, function(value, key) {
    pm.request.params.add({ key: key, value: value });
});

console.log('Đã thêm', customHeaders.length, 'tiêu đề yêu cầu');
console.log('Đã thêm', Object.keys(commonParams).length, 'các tham số truy vấn');
```

#### Ví dụ 8：URL Mã hóa tham số

```javascript
var CryptoJS = require('crypto-js');

// Nhận các thông số nhạy cảm
const userId = pm.environment.get('userId');
const secretKey = pm.environment.get('encryptKey') || 'default-key';

if (userId) {
    // Người dùng được mã hóa ID
    const encryptedUserId = CryptoJS.AES.encrypt(userId, secretKey).toString();

    // Sử dụng giá trị được mã hóa
    pm.request.params.add({
        key: 'uid',
        value: encodeURIComponent(encryptedUserId)
    });

    console.log('nguyên bản userId:', userId);
    console.log('Sau khi mã hóa:', encryptedUserId.substring(0, 20) + '...');
}

// Base64 Mã hóa
const credentials = pm.environment.get('username') + ':' + pm.environment.get('password');
const base64Credentials = CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(credentials));

pm.request.headers.upsert({
    key: 'Authorization',
    value: 'Basic ' + base64Credentials
});
```

#### Ví dụ 9：Yêu cầu chuẩn bị cơ chế thử lại

```javascript
// Đặt bộ đếm thử lại
let retryCount = pm.environment.get('retryCount');
if (!retryCount) {
    retryCount = 0;
}
pm.environment.set('retryCount', retryCount);

// Thêm cờ thử lại
pm.request.headers.upsert({
    key: 'X-Retry-Count',
    value: retryCount.toString()
});

// Đặt thời gian chờ (tăng dần dựa trên số lần thử lại)
const baseTimeout = 5000;
const timeout = baseTimeout * (retryCount + 1);
pm.environment.set('currentTimeout', timeout);

console.log('Số lần thử lại:', retryCount, '- khoảng thời gian chờ:', timeout + 'ms');
```

#### Ví dụ 10：Dân số dữ liệu giả (để thử nghiệm)

```javascript
var _ = require('lodash');
var moment = require('moment');

// Tạo dữ liệu đơn hàng mô phỏng
const mockOrder = {
    orderId: 'TEST_' + moment().format('YYYYMMDDHHmmss') + _.random(1000, 9999),
    customerId: pm.environment.get('testUserId') || 'CUST_' + _.random(10000, 99999),
    products: _.times(_.random(1, 5), function(n) {
        return {
            productId: 'PROD_' + _.random(1000, 9999),
            quantity: _.random(1, 10),
            price: _.round(_.random(10, 1000, true), 2)
        };
    }),
    totalAmount: 0,
    orderTime: moment().toISOString(),
    status: 'pending'
};

// Tính tổng số tiền
mockOrder.totalAmount = _.sumBy(mockOrder.products, function(p) {
    return p.quantity * p.price;
});
mockOrder.totalAmount = _.round(mockOrder.totalAmount, 2);

// Lưu dữ liệu mô phỏng
pm.environment.set('mockOrderData', JSON.stringify(mockOrder));
console.log('Tạo thứ tự mô phỏng:', JSON.stringify(mockOrder, null, 2));
```

### Post-request Script Ví dụ

#### Ví dụ 1：Xác thực phản hồi cơ bản

```javascript
// 1. Kiểm tra mã trạng thái
pm.test("Mã trạng thái là  200", function () {
    pm.response.to.have.status(200);
});

// 2. Kiểm tra thời gian đáp ứng
pm.test("Thời gian đáp ứng ít hơn 500ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

// 3. Kiểm tra tiêu đề phản hồi
pm.test("Câu trả lời có chứa Content-Type", function () {
    pm.response.to.have.header('Content-Type');
});

const contentType = pm.response.headers.get('Content-Type');
console.log('Content-Type:', contentType);

// 4. JSON Kiểm tra kết cấu
pm.test("Phản hồi chứa cấu trúc dữ liệu chính xác", function () {
    const jsonData = pm.response.json();

    // Kiểm tra xem thuộc tính có tồn tại không
    pm.expect(jsonData).to.have.property('status');
    pm.expect(jsonData).to.have.property('data');

    // Kiểm tra giá trị
    pm.expect(jsonData.status).to.equal('success');
});

// 5. Chuỗi chứa bài kiểm tra
pm.test("Phần thân phản hồi chứa success", function () {
    pm.expect(pm.response.text()).to.include('success');
});

// 6. Kiểm tra kết hợp thường xuyên
pm.test("Phản hồi có chứa hợp lệ email", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.email).to.match(/^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/);
});

// 7. Lưu dữ liệu phản hồi vào các biến môi trường
const responseData = pm.response.json();
if (responseData.token) {
    pm.environment.set('authToken', responseData.token);
    console.log('Token saved:', responseData.token);
}

if (responseData.userId) {
    pm.environment.set('userId', responseData.userId);
}
```

#### Ví dụ 2：Hoàn thành kiểm tra giao diện đăng nhập

```javascript
pm.test("Yêu cầu đăng nhập thành công", function () {
    pm.response.to.have.status(200);
});

pm.test("Thời gian phản hồi đăng nhập là hợp lý", function () {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

const jsonData = pm.response.json();

pm.test("Đăng nhập trả về cấu trúc dữ liệu chính xác", function () {
    pm.expect(jsonData).to.have.property('code');
    pm.expect(jsonData).to.have.property('message');
    pm.expect(jsonData).to.have.property('data');
    pm.expect(jsonData.code).to.equal(200);
});

pm.test("Mã thông báo xác thực được trả lại", function () {
    pm.expect(jsonData.data).to.have.property('token');
    pm.expect(jsonData.data).to.have.property('refreshToken');
    pm.expect(jsonData.data.token).to.match(/^[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+\.[A-Za-z0-9\-_]+$/); // JWT định dạng
});

pm.test("Thông tin người dùng được trả về", function () {
    pm.expect(jsonData.data).to.have.property('userInfo');
    pm.expect(jsonData.data.userInfo).to.have.property('userId');
    pm.expect(jsonData.data.userInfo).to.have.property('username');
});

// Lưu thông tin xác thực
if (jsonData.code === 200 && jsonData.data) {
    pm.environment.set('authToken', jsonData.data.token);
    pm.environment.set('refreshToken', jsonData.data.refreshToken);
    pm.environment.set('currentUserId', jsonData.data.userInfo.userId);
    pm.environment.set('currentUsername', jsonData.data.userInfo.username);

    console.log('✓ Đăng nhập thành công, người dùng:', jsonData.data.userInfo.username);
    console.log('✓ Token Đã lưu');
}
```

#### Ví dụ 3：Kiểm tra giao diện danh sách dữ liệu

```javascript
const jsonData = pm.response.json();

pm.test("Lấy được danh sách đã thành công", function () {
    pm.response.to.have.status(200);
    pm.expect(jsonData.code).to.equal(200);
});

pm.test("Cấu trúc dữ liệu danh sách là chính xác", function () {
    pm.expect(jsonData.data).to.have.property('list');
    pm.expect(jsonData.data).to.have.property('total');
    pm.expect(jsonData.data).to.have.property('pageNum');
    pm.expect(jsonData.data).to.have.property('pageSize');
});

pm.test("Dữ liệu danh sách là hợp lệ", function () {
    pm.expect(Array.isArray(jsonData.data.list)).to.equal(true);
    pm.expect(jsonData.data.total).to.be.a('number');
    pm.expect(jsonData.data.list.length).to.be.below(jsonData.data.pageSize + 1);
});

// Xác thực cấu trúc mục danh sách
if (jsonData.data.list.length > 0) {
    pm.test("Các mục danh sách chứa các trường bắt buộc", function () {
        const firstItem = jsonData.data.list[0];
        pm.expect(firstItem).to.have.property('id');
        pm.expect(firstItem).to.have.property('name');
        pm.expect(firstItem).to.have.property('createTime');
    });

    // Lưu mục đầu tiên ID cho việc thử nghiệm tiếp theo
    pm.environment.set('firstItemId', jsonData.data.list[0].id);
    console.log('Tổng số hồ sơ:', jsonData.data.total);
    console.log('Số lượng bản ghi trên trang hiện tại:', jsonData.data.list.length);
}
```

#### Ví dụ 4：Xử lý lỗi và thử lại logic

```javascript
const statusCode = pm.response.code;
const retryCount = parseInt(pm.environment.get('retryCount') || '0');
const maxRetries = 3;

if (statusCode === 200) {
    // Thành công, đặt lại số lần thử lại
    pm.environment.set('retryCount', '0');

    pm.test("Yêu cầu đã thành công", function () {
        pm.response.to.have.status(200);
    });

    console.log('✓ Yêu cầu đã thành công');

} else if (statusCode === 401) {
    // Có lẽ là trái phép token Hết hạn
    pm.test("Xác thực không thành công - Token Có thể đã hết hạn", function () {
        pm.expect(statusCode).to.equal(401);
    });

    console.error('✗ Xác thực không thành công, vui lòng đăng nhập lại');
    pm.environment.unset('authToken');

} else if (statusCode === 429) {
    // Yêu cầu được thực hiện quá thường xuyên
    pm.test("Yêu cầu giới hạn hiện tại", function () {
        pm.expect(statusCode).to.equal(429);
    });

    const retryAfter = pm.response.headers.get('Retry-After') || '60';
    console.warn('⚠ Yêu cầu quá thường xuyên, nên chờ đợi', retryAfter, 'Hãy thử lại sau vài giây');

} else if (statusCode >= 500) {
    // Lỗi máy chủ, bạn có thể thử lại
    if (retryCount < maxRetries) {
        pm.environment.set('retryCount', (retryCount + 1).toString());
        console.warn('⚠ Lỗi máy chủ, sẵn sàng thử lại (' + (retryCount + 1) + '/' + maxRetries + ')');
    } else {
        console.error('✗ Đã đạt đến số lần thử lại tối đa và việc thử lại sẽ bị hủy bỏ');
        pm.environment.set('retryCount', '0');
    }
} else {
    // Các lỗi khác
    pm.test("Yêu cầu không thành công - mã trạng thái: " + statusCode, function () {
        const jsonData = pm.response.json();
        console.error('thông báo lỗi:', jsonData.message || 'Lỗi không xác định');
    });
}
```

#### Ví dụ 5：Cookie và Session quản lý

```javascript
// 8. Cookie quản lý
pm.test("nghiên cứu session cookie", function () {
    pm.expect(pm.cookies.has('sessionId')).to.equal(true);
});

const sessionCookie = pm.cookies.get('sessionId');
if (sessionCookie) {
    console.log('Session ID:', sessionCookie.value);
    pm.environment.set('sessionId', sessionCookie.value);
}

// 9. Thu được từ tiêu đề phản hồi Cookie
const responseCookie = pm.getResponseCookie('JSESSIONID');
if (responseCookie) {
    console.log('JSESSIONID:', responseCookie.value);
    pm.environment.set('jsessionId', responseCookie.value);
}

// 10. sử dụng CookieJar Cài đặt tên miền chéo Cookie
const jar = pm.cookies.jar();
jar.set('https://api.example.com', 'custom_token=xyz123', function (error, cookie) {
    if (error) {
        console.error('cài đặt cookie thất bại:', error);
    } else {
        console.log('Cookie Thiết lập thành công');
    }
});

// Kiểm tra tất cả Cookie
console.log('=== Tất cả Cookie ===');
const allCookies = pm.cookies.getAll();
allCookies.forEach(function(cookie) {
    console.log(cookie.name + ':', cookie.value);
});
```

#### Ví dụ 6：Giám sát và thống kê hiệu suất

```javascript
// 11. Nhận thông tin kích thước phản hồi
const size = pm.response.size();
console.log('Kích thước cơ thể phản ứng:', size.body, 'bytes');
console.log('Kích thước tiêu đề phản hồi:', size.header, 'bytes');
console.log('Tổng kích thước:', size.total, 'bytes');

// Thống kê hiệu suất
const responseTime = pm.response.responseTime;
pm.environment.set('lastResponseTime', responseTime.toString());

// Tính thời gian phản hồi trung bình
let totalTime = parseFloat(pm.environment.get('totalResponseTime') || '0');
let requestCount = parseInt(pm.environment.get('requestCount') || '0');

totalTime += responseTime;
requestCount += 1;

pm.environment.set('totalResponseTime', totalTime.toString());
pm.environment.set('requestCount', requestCount.toString());

const avgResponseTime = totalTime / requestCount;
console.log('Thời gian phản hồi này:', responseTime, 'ms');
console.log('Thời gian phản hồi trung bình:', avgResponseTime.toFixed(2), 'ms');
console.log('Tổng số yêu cầu:', requestCount);

// Đánh giá mức độ hiệu suất
let performanceLevel = '';
if (responseTime < 100) {
    performanceLevel = 'Tuyệt vời';
} else if (responseTime < 300) {
    performanceLevel = 'Tốt';
} else if (responseTime < 1000) {
    performanceLevel = 'Trung bình';
} else {
    performanceLevel = 'chậm hơn';
}

pm.test("Khả năng đáp ứng: " + performanceLevel + " (" + responseTime + "ms)", function () {
    console.log('Mức hiệu suất:', performanceLevel);
});
```

#### Ví dụ 7：Xác thực dữ liệu phức tạp

```javascript
const jsonData = pm.response.json();

pm.test("Xác thực cấu trúc dữ liệu lồng nhau", function () {
    // Nhiều cấp độ xác nhận lồng nhau
    pm.expect(jsonData).to.have.property('data');
    pm.expect(jsonData.data).to.have.property('user');
    pm.expect(jsonData.data.user).to.have.property('profile');
    pm.expect(jsonData.data.user.profile).to.have.property('address');
});

// Xác minh truyền tải mảng
if (jsonData.data && jsonData.data.items) {
    pm.test("Tất cả các mặt hàng đều có giá", function () {
        jsonData.data.items.forEach(function(item, index) {
            pm.expect(item).to.have.property('price');
            pm.expect(item.price).to.be.a('number');
            pm.expect(item.price).to.be.below(100000);
            console.log('hàng hóa' + (index + 1) + ':', item.name, '- giá:', item.price);
        });
    });

    // Tính tổng giá
    let totalPrice = 0;
    jsonData.data.items.forEach(function(item) {
        totalPrice += item.price * item.quantity;
    });

    pm.test("Tổng giá được tính toán chính xác", function () {
        pm.expect(totalPrice).to.equal(jsonData.data.totalAmount);
    });

    console.log('Tổng giá sản phẩm:', totalPrice);
}
```

#### Ví dụ 8：Phân tích chi tiết các tiêu đề phản hồi

```javascript
// 12. Lặp lại qua tất cả các tiêu đề phản hồi
console.log('Tất cả các tiêu đề phản hồi:');
pm.response.headers.each(function (header) {
    console.log('  ' + header.key + ': ' + header.value);
});

// Xác minh các tiêu đề phản hồi liên quan đến bảo mật
pm.test("Kiểm tra tiêu đề phản hồi bảo mật", function () {
    const securityHeaders = [
        'X-Content-Type-Options',
        'X-Frame-Options',
        'X-XSS-Protection'
    ];

    securityHeaders.forEach(function(headerName) {
        if (pm.response.headers.has(headerName)) {
            console.log('✓ Chứa các tiêu đề bảo mật:', headerName);
        } else {
            console.warn('⚠ Thiếu tiêu đề bảo mật:', headerName);
        }
    });
});

// Kiểm tra chính sách bộ nhớ đệm
if (pm.response.headers.has('Cache-Control')) {
    const cacheControl = pm.response.headers.get('Cache-Control');
    console.log('Chiến lược bộ nhớ đệm:', cacheControl);
}

// nghiên cứu CORS cài đặt
if (pm.response.headers.has('Access-Control-Allow-Origin')) {
    const cors = pm.response.headers.get('Access-Control-Allow-Origin');
    console.log('CORS cài đặt:', cors);
}
```

#### Ví dụ 10：Trích xuất và phân phối dữ liệu (API cuộc gọi bị xâu chuỗi)

```javascript
// Từ JSON Trích xuất dữ liệu lồng nhau từ phản hồi
pm.test("Trích xuất thông tin người dùng", function () {
    const jsonData = pm.response.json();

    // Cấu trúc phản hồi giả định: .{ data: { user: { id: 123, name: "John" } } }
    pm.expect(jsonData).to.have.property('data');

    const userData = jsonData.data.user;
    pm.expect(userData).to.have.property('id');
    pm.expect(userData).to.have.property('name');

    // Lưu vào các biến môi trường cho yêu cầu tiếp theo
    pm.environment.set('currentUserId', userData.id.toString());
    pm.environment.set('currentUserName', userData.name);

    // Nếu có thông tin về quyền, hãy lưu nó
    if (userData.roles) {
        pm.environment.set('userRoles', JSON.stringify(userData.roles));
    }
});

// Xử lý các phản hồi mảng
pm.test("Xử lý dữ liệu mảng", function () {
    const jsonData = pm.response.json();

    // Giả sử phản hồi là một mảng
    pm.expect(Array.isArray(jsonData.items)).to.equal(true);

    // Kiểm tra phần tử đầu tiên
    if (jsonData.items.length > 0) {
        const firstItem = jsonData.items[0];
        pm.expect(firstItem).to.have.property('id');

        // Lưu mục đầu tiên ID
        pm.environment.set('firstItemId', firstItem.id.toString());
    }

    // Lưu toàn bộ danh sách ID mảng
    const itemIds = jsonData.items.map(function(item) {
        return item.id;
    });
    pm.environment.set('allItemIds', JSON.stringify(itemIds));
    console.log('Đã trích xuất', itemIds.length, 'cá nhân ID');
});
```

#### Ví dụ 11：Giải mã và xác minh dữ liệu phản hồi

```javascript
var CryptoJS = require('crypto-js');

const jsonData = pm.response.json();

pm.test("Phản hồi chứa dữ liệu được mã hóa", function () {
    pm.expect(jsonData).to.have.property('encryptedData');
});

// Giải mã dữ liệu phản hồi
if (jsonData.encryptedData) {
    const secretKey = pm.environment.get('encryptKey') || 'default-key';

    try {
        const decryptedBytes = CryptoJS.AES.decrypt(jsonData.encryptedData, secretKey);
        const decryptedText = decryptedBytes.toString(CryptoJS.enc.Utf8);
        const decryptedData = JSON.parse(decryptedText);

        console.log('✓ Giải mã dữ liệu thành công');
        console.log('Dữ liệu được giải mã:', decryptedData);

        // Xác minh dữ liệu được giải mã
        pm.test("Dữ liệu được giải mã là hợp lệ", function () {
            pm.expect(decryptedData).to.have.property('userId');
            pm.expect(decryptedData).to.have.property('balance');
        });

        // Lưu dữ liệu được giải mã
        pm.environment.set('decryptedUserId', decryptedData.userId);
        pm.environment.set('userBalance', decryptedData.balance.toString());

    } catch (error) {
        console.error('✗ Giải mã không thành công:', error.message);
        pm.test("Giải mã dữ liệu không thành công", function () {
            throw new Error('Giải mã không thành công: ' + error.message);
        });
    }
}
```

#### Ví dụ 12：Xác minh logic kinh doanh

```javascript
var _ = require('lodash');
var moment = require('moment');

const jsonData = pm.response.json();

// Xác minh trạng thái đơn hàng
pm.test("Trạng thái đơn hàng là hợp lệ", function () {
    const validStatuses = ['pending', 'processing', 'completed', 'cancelled'];
    pm.expect(validStatuses).to.include(jsonData.order.status);
});

// Xác thực định dạng ngày
pm.test("Định dạng ngày là chính xác", function () {
    const createTime = jsonData.order.createTime;
    pm.expect(moment(createTime, moment.ISO_8601, true).isValid()).to.equal(true);
});

// Xác minh tính toán số tiền
pm.test("Số tiền đặt hàng được tính toán chính xác", function () {
    const items = jsonData.order.items;
    let calculatedTotal = _.sumBy(items, function(item) {
        return item.price * item.quantity;
    });

    // Cộng với vận chuyển
    calculatedTotal += jsonData.order.shippingFee || 0;

    // Trừ đi giảm giá
    calculatedTotal -= jsonData.order.discount || 0;

    calculatedTotal = _.round(calculatedTotal, 2);

    pm.expect(calculatedTotal).to.equal(jsonData.order.totalAmount);
    console.log('Tính số tiền:', calculatedTotal, 'Số tiền đặt hàng:', jsonData.order.totalAmount);
});

// Xác minh hàng tồn kho
pm.test("Hàng đã có đầy đủ", function () {
    jsonData.order.items.forEach(function(item) {
        pm.expect(item.quantity).to.be.below(item.stock + 1);
        if (item.quantity > item.stock * 0.8) {
            console.warn('⚠ hàng hóa', item.name, 'Hàng thiếu, thừa:', item.stock);
        }
    });
});
```

### Sử dụng các ví dụ thư viện tích hợp

#### Ví dụ 1：CryptoJS Thư viện tiền điện tử

```javascript
// Sử dụng trực tiếp các biến toàn cục CryptoJS（Không cần require）
// var CryptoJS = require('crypto-js'); // Tùy chọn, phương pháp này cũng được hỗ trợ

// 1. AES Mã hóa/Giải mã
const message = 'secret message';
const secretKey = 'my-secret-key-123';

// Mã hóa
const encrypted = CryptoJS.AES.encrypt(message, secretKey).toString();
pm.environment.set('encrypted', encrypted);
console.log('AES Mã hóa:', encrypted);

// Giải mã
const decrypted = CryptoJS.AES.decrypt(encrypted, secretKey);
const decryptedText = decrypted.toString(CryptoJS.enc.Utf8);
console.log('AES Giải mã:', decryptedText);

// 2. MD5 Băm
const password = 'myPassword123';
const md5Hash = CryptoJS.MD5(password).toString();
console.log('MD5 Băm:', md5Hash);
pm.environment.set('passwordHash', md5Hash);

// 3. SHA256 Băm
const sha256Hash = CryptoJS.SHA256(password).toString();
console.log('SHA256 Băm:', sha256Hash);

// 4. HMAC-SHA256 dấu hiệu
const timestamp = Date.now().toString();
const data = 'userId=123&timestamp=' + timestamp;
const hmacKey = 'my-hmac-key';
const signature = CryptoJS.HmacSHA256(data, hmacKey).toString();
console.log('HMAC-SHA256 dấu hiệu:', signature);

// 5. Base64 Mã hóa/giải mã
const text = 'Hello World';
const base64Encoded = CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(text));
console.log('Base64 Mã hóa:', base64Encoded);

const base64Decoded = CryptoJS.enc.Base64.parse(base64Encoded).toString(CryptoJS.enc.Utf8);
console.log('Base64 giải mã:', base64Decoded);

// 6. Tạo một chuỗi ngẫu nhiên
const randomBytes = CryptoJS.lib.WordArray.random(16);
const randomString = randomBytes.toString(CryptoJS.enc.Hex);
console.log('Chuỗi ngẫu nhiên:', randomString);
pm.environment.set('nonce', randomString);
```

#### Ví dụ 2：Lodash Thư viện xử lý dữ liệu

```javascript
// Sử dụng trực tiếp các biến toàn cục _（Không cần require）
// var _ = require('lodash'); // Tùy chọn, phương pháp này cũng được hỗ trợ

// 1. Hoạt động mảng
const numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

// lọc
const filtered = _.filter(numbers, function (n) {
    return n > 5;
});
console.log('Lọc kết quả:', filtered); // [6, 7, 8, 9, 10]

// lập bản đồ
const doubled = _.map(numbers, function(n) {
    return n * 2;
});
console.log('Kết quả bản đồ:', doubled);

// Tổng hợp
const sum = _.sum(numbers);
console.log('Tổng hợp mảng:', sum);

// Lấy mức trung bình
const avg = _.mean(numbers);
console.log('giá trị trung bình:', avg);

// 2. Hoạt động đối tượng
const user = {
    id: 123,
    name: 'John Doe',
    email: 'john@example.com',
    age: 30,
    city: 'Beijing'
};

// Trích xuất các giá trị thuộc tính
const values = _.values(user);
console.log('giá trị đối tượng:', values);

// Trích xuất tên thuộc tính
const keys = _.keys(user);
console.log('Khóa đối tượng:', keys);

// Chọn thuộc tính cụ thể
const picked = _.pick(user, ['id', 'name', 'email']);
console.log('Chọn thuộc tính:', picked);

// Bỏ qua các thuộc tính cụ thể
const omitted = _.omit(user, ['age', 'city']);
console.log('Bỏ qua các thuộc tính:', omitted);

// 3. Hoạt động thu thập
const users = [
    { id: 1, name: 'Alice', age: 25, role: 'admin' },
    { id: 2, name: 'Bob', age: 30, role: 'user' },
    { id: 3, name: 'Charlie', age: 35, role: 'user' },
    { id: 4, name: 'David', age: 28, role: 'admin' }
];

// Tìm thấy
const admin = _.find(users, { role: 'admin' });
console.log('Người quản trị đầu tiên:', admin);

// lọc
const admins = _.filter(users, { role: 'admin' });
console.log('Tất cả các quản trị viên:', admins);

// Nhóm
const grouped = _.groupBy(users, 'role');
console.log('Nhóm theo vai trò:', grouped);

// Loại
const sorted = _.sortBy(users, ['age']);
console.log('Sắp xếp theo độ tuổi:', sorted);

// Trích xuất các trường cụ thể
const names = _.map(users, 'name');
console.log('Tất cả các tên:', names);

// số liệu thống kê
const sumAges = _.sumBy(users, 'age');
console.log('Tổng số tuổi:', sumAges);

// 4. Hoạt động chuỗi
const str = 'hello world';
const capitalized = _.capitalize(str);
console.log('Viết hoa chữ cái đầu tiên:', capitalized);

const camelCase = _.camelCase('hello-world-example');
console.log('Đặt tên bướu:', camelCase); // helloWorldExample

const snakeCase = _.snakeCase('helloWorldExample');
console.log('Đặt tên cho rắn:', snakeCase); // hello_world_example

// 5. Tạo số ngẫu nhiên
const randomNum = _.random(1, 100);
console.log('Số nguyên ngẫu nhiên:', randomNum);

const randomFloat = _.random(1.5, 5.5, true);
console.log('Số dấu phẩy động ngẫu nhiên:', randomFloat);

// 6. Loại bỏ trọng lượng
const duplicates = [1, 2, 2, 3, 4, 4, 5];
const unique = _.uniq(duplicates);
console.log('Loại bỏ kết quả trùng lặp:', unique);

// 7. Hoạt động mảng
const arr1 = [1, 2, 3];
const arr2 = [3, 4, 5];

// ngã tư
const intersection = _.intersection(arr1, arr2);
console.log('ngã tư:', intersection); // [3]

// Liên minh
const union = _.union(arr1, arr2);
console.log('Liên minh:', union); // [1, 2, 3, 4, 5]

// bộ khác biệt
const difference = _.difference(arr1, arr2);
console.log('bộ khác biệt:', difference); // [1, 2]

// 8. Nhân bản sâu
const original = { a: 1, b: { c: 2 } };
const cloned = _.cloneDeep(original);
cloned.b.c = 999;
console.log('đối tượng ban đầu:', original.b.c); // 2
console.log('Nhân bản đối tượng:', cloned.b.c); // 999

// 9. Hợp nhất các đối tượng
const obj1 = { a: 1, b: 2 };
const obj2 = { b: 3, c: 4 };
const merged = _.merge({}, obj1, obj2);
console.log('Hợp nhất kết quả:', merged); // { a: 1, b: 3, c: 4 }

// 10. Chống rung và tiết lưu (dùng trong vòng lặp)
const processData = _.debounce(function(data) {
    console.log('Xử lý dữ liệu:', data);
}, 300);

// 11. Cuộc gọi chuỗi
const result = _(numbers)
    .filter(function(n) { return n % 2 === 0; })
    .map(function(n) { return n * n; })
    .sum();
console.log('Tổng các bình phương chẵn:', result);
```

#### Ví dụ 3：Moment.js Thư viện ngày và giờ

```javascript
// Sử dụng trực tiếp các biến toàn cục moment（Không cần require）
// var moment = require('moment'); // Tùy chọn, phương pháp này cũng được hỗ trợ

// 1. Lấy thời gian hiện tại
const now = moment();
console.log('thời điểm hiện tại:', now.format('YYYY-MM-DD HH:mm:ss'));

// 2. Định dạng ngày
const formatted = moment().format('YYYY-MM-DD HH:mm:ss');
pm.environment.set('currentTime', formatted);
console.log('Định dạng thời gian:', formatted);

// ISO 8601 định dạng
const iso = moment().toISOString();
console.log('ISO định dạng:', iso);

// Unix dấu thời gian
const timestamp = moment().unix();
console.log('Unix dấu thời gian:', timestamp);

// Dấu thời gian mili giây
const milliseconds = moment().valueOf();
console.log('Dấu thời gian mili giây:', milliseconds);

// 3. Phân tích ngày
const parsed1 = moment('2024-01-01', 'YYYY-MM-DD');
console.log('Phân tích ngày:', parsed1.format('YYYYNămMMmặt trăngDDNgày'));

const parsed2 = moment('01/15/2024', 'MM/DD/YYYY');
console.log('ngày Mỹ:', parsed2.format('YYYY-MM-DD'));

// Phân tích từ dấu thời gian
const fromTimestamp = moment(1704067200000);
console.log('Phân tích dấu thời gian:', fromTimestamp.format('YYYY-MM-DD HH:mm:ss'));

// 4. Tính toán ngày
const tomorrow = moment().add(1, 'days');
console.log('ngày mai:', tomorrow.format('YYYY-MM-DD'));

const nextWeek = moment().add(1, 'weeks');
console.log('tuần tới:', nextWeek.format('YYYY-MM-DD'));

const nextMonth = moment().add(1, 'months');
console.log('tháng tới:', nextMonth.format('YYYY-MM-DD'));

const yesterday = moment().subtract(1, 'days');
console.log('ngày hôm qua:', yesterday.format('YYYY-MM-DD'));

// 5. So sánh ngày
const date1 = moment('2024-01-01');
const date2 = moment('2024-12-31');

console.log('date1 Vào date2 Trước:', date1.isBefore(date2)); // true
console.log('date1 Vào date2 sau đó:', date1.isAfter(date2)); // false
console.log('Ngày tháng giống nhau:', date1.isSame(date2)); // false

// 6. Sự khác biệt ngày
const start = moment('2024-01-01');
const end = moment('2024-12-31');

const diffDays = end.diff(start, 'days');
console.log('Sự khác biệt là ngày:', diffDays);

const diffMonths = end.diff(start, 'months');
console.log('Sự khác biệt là tháng:', diffMonths);

const diffYears = end.diff(start, 'years');
console.log('Sự khác biệt trong năm:', diffYears);

// 7. Thời gian bắt đầu và kết thúc
const startOfDay = moment().startOf('day');
console.log('Hãy bắt đầu ngay hôm nay:', startOfDay.format('YYYY-MM-DD HH:mm:ss'));

const endOfDay = moment().endOf('day');
console.log('Kết thúc ngày hôm nay:', endOfDay.format('YYYY-MM-DD HH:mm:ss'));

const startOfMonth = moment().startOf('month');
console.log('Bắt đầu từ tháng này:', startOfMonth.format('YYYY-MM-DD'));

const endOfMonth = moment().endOf('month');
console.log('Kết thúc trong tháng này:', endOfMonth.format('YYYY-MM-DD'));

// 8. thời gian tương đối
const aWeekAgo = moment().subtract(7, 'days');
console.log('một tuần trước:', aWeekAgo.fromNow()); // 7 days ago

const inThreeDays = moment().add(3, 'days');
console.log('Ba ngày sau:', inThreeDays.fromNow()); // in 3 days

// 9. Ngày xác minh
const validDate = moment('2024-01-01', 'YYYY-MM-DD', true).isValid();
console.log('Ngày hợp lệ:', validDate); // true

const invalidDate = moment('2024-13-01', 'YYYY-MM-DD', true).isValid();
console.log('Ngày không hợp lệ:', invalidDate); // false

// 10. Tình huống thực tế: Tạo các định dạng thời gian khác nhau
pm.environment.set('dateYMD', moment().format('YYYY-MM-DD'));
pm.environment.set('dateYMDHMS', moment().format('YYYY-MM-DD HH:mm:ss'));
pm.environment.set('dateISO', moment().toISOString());
pm.environment.set('timestamp', moment().valueOf().toString());
pm.environment.set('dateChina', moment().format('YYYYNămMMmặt trăngDDNgày HHthời gianmmđiểmssgiây'));

// 11. Xử lý múi giờ (nếu được hỗ trợ)
const utc = moment.utc();
console.log('UTC thời gian:', utc.format('YYYY-MM-DD HH:mm:ss'));

// 12. Kịch bản kinh doanh: Phạm vi thời gian thế hệ
const today = moment().startOf('day');
const todayEnd = moment().endOf('day');

pm.environment.set('queryStartTime', today.valueOf().toString());
pm.environment.set('queryEndTime', todayEnd.valueOf().toString());

console.log('Thời gian bắt đầu truy vấn:', today.format('YYYY-MM-DD HH:mm:ss'));
console.log('Thời gian kết thúc truy vấn:', todayEnd.format('YYYY-MM-DD HH:mm:ss'));
```

#### Ví dụ 4：Sử dụng kết hợp nhiều thư viện

```javascript
var _ = require('lodash');
var moment = require('moment');
var CryptoJS = require('crypto-js');

// Kịch bản: Tạo chữ ký API yêu cầu

// 1. Chuẩn bị các thông số yêu cầu
const params = {
    userId: pm.environment.get('userId') || '123',
    timestamp: moment().valueOf().toString(),
    nonce: CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Hex),
    action: 'getUserInfo'
};

// 2. Sắp xếp các tham số theo tên khóa
const sortedKeys = _.keys(params).sort();
console.log('Các phím được sắp xếp:', sortedKeys);

// 3. Nối chuỗi chữ ký
const signString = _.map(sortedKeys, function(key) {
    return key + '=' + params[key];
}).join('&');
console.log('Chuỗi chữ ký:', signString);

// 4. Tạo chữ ký
const secretKey = pm.environment.get('secretKey') || 'default-secret';
const signature = CryptoJS.HmacSHA256(signString, secretKey).toString();
console.log('dấu hiệu:', signature);

// 5. Lưu vào các biến môi trường
pm.environment.set('requestTimestamp', params.timestamp);
pm.environment.set('requestNonce', params.nonce);
pm.environment.set('requestSignature', signature);

// 6. In các thông số yêu cầu đầy đủ
const fullParams = _.assign({}, params, { signature: signature });
console.log('Hoàn thành các tham số yêu cầu:', JSON.stringify(fullParams, null, 2));
```

#### Ví dụ 5：Trình tạo mô phỏng dữ liệu

```javascript
var _ = require('lodash');
var moment = require('moment');

// Tạo dữ liệu kiểm tra hàng loạt
const mockUsers = _.times(10, function(index) {
    return {
        id: 1000 + index,
        username: 'user_' + _.random(10000, 99999),
        email: 'test' + index + '@example.com',
        age: _.random(18, 60),
        gender: _.sample(['male', 'female']),
        city: _.sample(['Beijing', 'Shanghai', 'Guangzhou', 'Shenzhen']),
        registerTime: moment().subtract(_.random(1, 365), 'days').format('YYYY-MM-DD HH:mm:ss'),
        lastLoginTime: moment().subtract(_.random(0, 30), 'days').format('YYYY-MM-DD HH:mm:ss'),
        isActive: _.sample([true, false]),
        score: _.round(_.random(0, 100, true), 2)
    };
});

console.log('Đã tạo', mockUsers.length, 'người dùng mô phỏng');
console.log('Người dùng ví dụ:', JSON.stringify(mockUsers[0], null, 2));

// Lưu thông tin người dùng đầu tiên
pm.environment.set('testUserId', mockUsers[0].id.toString());
pm.environment.set('testUsername', mockUsers[0].username);
pm.environment.set('mockUsersData', JSON.stringify(mockUsers));

// Thống kê
const avgAge = _.meanBy(mockUsers, 'age');
const avgScore = _.meanBy(mockUsers, 'score');
const activeCount = _.filter(mockUsers, { isActive: true }).length;

console.log('Độ tuổi trung bình:', _.round(avgAge, 1));
console.log('Điểm trung bình:', _.round(avgScore, 2));
console.log('Số lượng người dùng đang hoạt động:', activeCount);
```

---

## Ví dụ về đoạn mã nâng cao

### Đoạn mã 1：OAuth 2.0 Token Tự động làm mới

```javascript
// Pre-request Script: Tự động kiểm tra và làm mới Token
const tokenExpireTime = pm.environment.get('tokenExpireTime');
const currentTime = Date.now();

// nghiên cứu token Cho dù nó hết hạn (trước5phút để làm mới)
if (!tokenExpireTime || currentTime > (parseInt(tokenExpireTime) - 300000)) {
    console.log('Token Nó sắp hết hạn hoặc đã hết hạn và cần được làm mới');
    pm.sendRequest({
        url: pm.environment.get('authUrl'),
        method: 'POST',
        header: {
            'Content-Type': 'application/json'
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                refreshToken: pm.environment.get('refreshToken')
            })
        }
    }, function (err, response) {
        if (err) {
            console.error('Làm mới token thất bại:', err.message);
            return;
        }

        const data = response.json();
        const token = data.accessToken || data.token;
        if (token) {
            pm.environment.set('authToken', token);
            pm.request.headers.upsert({
                key: 'Authorization',
                value: 'Bearer ' + token
            });
            console.log('✓ Token Việc làm mới thành công và tiêu đề xác thực đã được cập nhật');
        }
    });
} else {
    const token = pm.environment.get('authToken');
    if (token) {
        pm.request.headers.upsert({
            key: 'Authorization',
            value: 'Bearer ' + token
        });
        console.log('✓ Token Hợp lệ, tiêu đề xác thực đã được thêm');
    }
}
```

### Đoạn mã 2：Giao diện xử lý giới hạn dòng điện (đếm cục bộ)

```javascript
// Pre-request Script: Yêu cầu địa phương kiểm tra giới hạn hiện tại
const lastRequestTime = pm.environment.get('lastRequestTime');
const minInterval = 100; // Khoảng thời gian yêu cầu tối thiểu (mili giây)

if (lastRequestTime) {
    const timeSinceLastRequest = Date.now() - parseInt(lastRequestTime);
    if (timeSinceLastRequest < minInterval) {
        const waitTime = minInterval - timeSinceLastRequest;
        console.warn('⚠ Yêu cầu quá thường xuyên, nên chờ đợi', waitTime, 'ms');
    }
}

pm.environment.set('lastRequestTime', Date.now().toString());
```

### Đoạn mã 3：Chuyển đổi môi trường năng động

```javascript
// Pre-request Script: Chuyển đổi cấu hình môi trường dựa trên các biến
const env = pm.environment.get('ENV') || 'dev';

const configs = {
    dev: {
        baseUrl: 'https://dev-api.example.com',
        timeout: 10000,
        debug: true
    },
    test: {
        baseUrl: 'https://test-api.example.com',
        timeout: 8000,
        debug: true
    },
    staging: {
        baseUrl: 'https://staging-api.example.com',
        timeout: 5000,
        debug: false
    },
    prod: {
        baseUrl: 'https://api.example.com',
        timeout: 5000,
        debug: false
    }
};

const config = configs[env];
if (!config) {
    throw new Error('Môi trường không xác định: ' + env);
}

// Áp dụng cấu hình
pm.environment.set('baseUrl', config.baseUrl);
pm.environment.set('timeout', config.timeout.toString());
pm.environment.set('debug', config.debug.toString());

// Thêm tiêu đề nhận dạng môi trường
pm.request.headers.upsert({
    key: 'X-Environment',
    value: env
});

console.log('✓ Đã chuyển sang', env, 'môi trường');
console.log('  Base URL:', config.baseUrl);
console.log('  Timeout:', config.timeout);
```

### Đoạn mã 4：Dynamic template filling of request body

```javascript
// Pre-request Script: JSON Mẫu thay thế động
var _ = require('lodash');
var moment = require('moment');

// Xác định mẫu nội dung yêu cầu (có thể lấy từ các biến môi trường)
const requestTemplate = {
    "requestId": "{{requestId}}",
    "timestamp": "{{timestamp}}",
    "userId": "{{userId}}",
    "action": "{{action}}",
    "data": {
        "startDate": "{{startDate}}",
        "endDate": "{{endDate}}",
        "pageNum": "{{pageNum}}",
        "pageSize": "{{pageSize}}"
    }
};

// Chuẩn bị thay thế dữ liệu
const templateData = {
    requestId: pm.uuid(),
    timestamp: moment().valueOf(),
    userId: pm.environment.get('userId') || '123',
    action: 'queryOrders',
    startDate: moment().subtract(7, 'days').format('YYYY-MM-DD'),
    endDate: moment().format('YYYY-MM-DD'),
    pageNum: 1,
    pageSize: 20
};

// Thay thế mẫu
let requestBodyStr = JSON.stringify(requestTemplate);
_.forEach(templateData, function(value, key) {
    const placeholder = '{{' + key + '}}';
    requestBodyStr = requestBodyStr.replace(new RegExp(placeholder, 'g'), value);
});

const requestBody = JSON.parse(requestBodyStr);
console.log('Nội dung yêu cầu được tạo:', JSON.stringify(requestBody, null, 2));

// Lưu vào các biến môi trường để sử dụng theo yêu cầu
pm.environment.set('dynamicRequestBody', JSON.stringify(requestBody));
```

### Đoạn mã 5：Trình xác thực tham số hàng loạt

```javascript
// Pre-request Script: Yêu cầu xác thực tham số
const validationRules = {
    userId: {
        required: true,
        type: 'string',
        pattern: /^[0-9]+$/,
        message: 'userId Phải là một chuỗi số'
    },
    email: {
        required: false,
        type: 'string',
        pattern: /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/,
        message: 'email Định dạng không chính xác'
    },
    age: {
        required: false,
        type: 'number',
        min: 0,
        max: 150,
        message: 'age Phải ở đó 0-150 giữa'
    }
};

const params = {
    userId: pm.environment.get('userId'),
    email: pm.environment.get('email'),
    age: parseInt(pm.environment.get('age'))
};

const errors = [];

// Thực hiện xác minh
Object.keys(validationRules).forEach(function(key) {
    const rule = validationRules[key];
    const value = params[key];

    // Xác thực là cần thiết
    if (rule.required && (!value || value === '')) {
        errors.push(key + ' được yêu cầu');
        return;
    }

    if (value !== undefined && value !== null && value !== '') {
        // Kiểm tra kiểu
        if (rule.type === 'number' && typeof value !== 'number') {
            errors.push(key + ' Phải có kiểu số');
        }

        // Kiểm tra thường xuyên
        if (rule.pattern && !rule.pattern.test(String(value))) {
            errors.push(rule.message || key + ' Định dạng không chính xác');
        }

        // Kiểm tra phạm vi
        if (rule.type === 'number') {
            if (rule.min !== undefined && value < rule.min) {
                errors.push(key + ' không thể ít hơn ' + rule.min);
            }
            if (rule.max !== undefined && value > rule.max) {
                errors.push(key + ' Không thể lớn hơn ' + rule.max);
            }
        }
    }
});

if (errors.length > 0) {
    console.error('Xác thực tham số không thành công:');
    errors.forEach(function(error) {
        console.error('  ✗', error);
    });
    throw new Error('Xác thực tham số không thành công: ' + errors.join('; '));
} else {
    console.log('✓ Tất cả các tham số được xác minh đã thông qua');
}
```

### Đoạn mã 6：Chuỗi trích xuất dữ liệu phản hồi (kịch bản cuộc gọi chuỗi)

```javascript
// Post-request Script: Trích xuất dữ liệu từ các phản hồi lồng nhau phức tạp
var _ = require('lodash');

const jsonData = pm.response.json();

pm.test("Cấu trúc phản hồi là chính xác", function () {
    pm.response.to.have.status(200);
    pm.expect(jsonData.code).to.equal(0);
});

// sử dụng lodash Trích xuất dữ liệu từ các cấu trúc phức tạp
const extractedData = {
    // Trích xuất người dùng ID danh sách
    userIds: _.map(_.get(jsonData, 'data.users', []), 'id'),

    // Trích xuất thông tin chi tiết của người dùng đầu tiên
    firstUser: _.get(jsonData, 'data.users[0]', null),

    // Trích xuất tất cả các quản trị viên
    admins: _.filter(_.get(jsonData, 'data.users', []), { role: 'admin' }),

    // Tính tổng số tiền
    totalAmount: _.sumBy(_.get(jsonData, 'data.orders', []), 'amount'),

    // Nhóm đơn hàng theo trạng thái
    ordersByStatus: _.groupBy(_.get(jsonData, 'data.orders', []), 'status'),

    // Trích xuất thông tin phân trang
    pagination: _.pick(_.get(jsonData, 'data', {}), ['pageNum', 'pageSize', 'total'])
};

console.log('Dữ liệu được trích xuất:', JSON.stringify(extractedData, null, 2));

// Lưu thông tin quan trọng vào các biến môi trường
if (extractedData.firstUser) {
    pm.environment.set('firstUserId', extractedData.firstUser.id);
    pm.environment.set('firstUserName', extractedData.firstUser.name);
}

if (extractedData.userIds.length > 0) {
    pm.environment.set('userIdList', JSON.stringify(extractedData.userIds));
}

pm.environment.set('totalAmount', extractedData.totalAmount.toString());

console.log('✓ Đã trích xuất', extractedData.userIds.length, 'người dùngID');
console.log('✓ một lần:', extractedData.totalAmount);
```

### Đoạn mã 7：Điểm chuẩn hiệu suất

```javascript
// Post-request Script: Giám sát hiệu suất và điểm chuẩn
var _ = require('lodash');

const responseTime = pm.response.responseTime;
const endpoint = pm.request.url.getPath();

// Xác định đường cơ sở hiệu suất (mili giây)
const benchmarks = {
    '/api/user/login': 500,
    '/api/user/profile': 300,
    '/api/orders/list': 1000,
    '/api/products/search': 800,
    'default': 1000
};

const benchmark = benchmarks[endpoint] || benchmarks['default'];

// Đánh giá mức độ thực hiện
let performanceGrade = '';
let performanceScore = 100;

if (responseTime <= benchmark * 0.3) {
    performanceGrade = 'A+ (Tuyệt vời)';
    performanceScore = 100;
} else if (responseTime <= benchmark * 0.5) {
    performanceGrade = 'A (Tốt)';
    performanceScore = 90;
} else if (responseTime <= benchmark * 0.8) {
    performanceGrade = 'B (Trung bình)';
    performanceScore = 75;
} else if (responseTime <= benchmark) {
    performanceGrade = 'C (Đi qua)';
    performanceScore = 60;
} else if (responseTime <= benchmark * 1.5) {
    performanceGrade = 'D (chậm hơn)';
    performanceScore = 40;
} else {
    performanceGrade = 'F (Từ từ thôi)';
    performanceScore = 20;
}

// Ghi lại dữ liệu hiệu suất
const perfKey = 'perf_' + endpoint.replace(/\//g, '_');
let perfHistory = pm.environment.get(perfKey);
if (!perfHistory) {
    perfHistory = [];
} else {
    try {
        perfHistory = JSON.parse(perfHistory);
    } catch (e) {
        perfHistory = [];
    }
}

perfHistory.push(responseTime);

// Chỉ giữ lại những gì gần đây nhất10hồ sơ
if (perfHistory.length > 10) {
    perfHistory = _.takeRight(perfHistory, 10);
}

pm.environment.set(perfKey, JSON.stringify(perfHistory));

// Tính thời gian phản hồi trung bình
const avgResponseTime = _.mean(perfHistory);
const minResponseTime = _.min(perfHistory);
const maxResponseTime = _.max(perfHistory);

// Báo cáo hiệu suất đầu ra
console.log('=== Báo cáo thử nghiệm hiệu suất ===');
console.log('giao diện:', endpoint);
console.log('Thời gian phản hồi này:', responseTime, 'ms');
console.log('Mức hiệu suất:', performanceGrade, '(ghi điểm:', performanceScore + ')');
console.log('Điểm chuẩn hiệu suất:', benchmark, 'ms');
console.log('trung bình lịch sử:', _.round(avgResponseTime, 2), 'ms');
console.log('Phản hồi nhanh nhất:', minResponseTime, 'ms');
console.log('Phản hồi chậm nhất:', maxResponseTime, 'ms');

// Tạo các bài kiểm tra hiệu suất
pm.test("Kiểm tra hiệu suất: " + performanceGrade, function () {
    pm.expect(responseTime).to.be.below(benchmark);
});

// Cảnh báo nếu hiệu suất suy giảm
if (responseTime > avgResponseTime * 1.5) {
    console.warn('⚠️ Cảnh báo: Thời gian phản hồi này chậm hơn mức trung bình', _.round((responseTime / avgResponseTime - 1) * 100, 1) + '%');
}
```

### Đoạn mã 8：Kiểm tra tính nhất quán dữ liệu

```javascript
// Post-request Script: Xác thực logic kinh doanh phức tạp
var _ = require('lodash');
var moment = require('moment');

const jsonData = pm.response.json();

pm.test("Kiểm tra tính nhất quán của dữ liệu đặt hàng", function () {
    if (!jsonData.data || !jsonData.data.order) {
        throw new Error('Dữ liệu đơn hàng không tồn tại');
    }

    const order = jsonData.data.order;

    // 1. Xác minh tính toán số tiền
    const calculatedSubtotal = _.sumBy(order.items, function(item) {
        return item.price * item.quantity;
    });

    pm.expect(_.round(calculatedSubtotal, 2)).to.equal(order.subtotal);
    console.log('✓ Tổng số tiền là chính xác:', order.subtotal);

    // 2. TotalAmountValidation(Subtotal + vận chuyển hàng hóa - Giảm giá)
    const calculatedTotal = _.round(
        order.subtotal + (order.shippingFee || 0) - (order.discount || 0),
        2
    );
    pm.expect(calculatedTotal).to.equal(order.totalAmount);
    console.log('✓ Tổng số tiền là chính xác:', order.totalAmount);

    // 3. Xác minh logic chuyển trạng thái
    const validStatusFlow = {
        'pending': ['processing', 'cancelled'],
        'processing': ['shipped', 'cancelled'],
        'shipped': ['delivered', 'return_requested'],
        'delivered': ['completed', 'return_requested'],
        'return_requested': ['refunding', 'delivered'],
        'refunding': ['refunded'],
        'completed': [],
        'cancelled': [],
        'refunded': []
    };

    const currentStatus = order.status;
    console.log('✓ Tình trạng đơn hàng hiện tại:', currentStatus);

    // 4. Xác thực logic ngày
    if (order.createTime && order.updateTime) {
        const createTime = moment(order.createTime);
        const updateTime = moment(order.updateTime);

        pm.expect(updateTime.isSameOrAfter(createTime)).to.equal(true);
        console.log('✓ Thời gian cập nhật không sớm hơn thời gian tạo');
    }

    // 5. Xác minh số lượng sản phẩm
    pm.expect(order.items.length).to.be.above(0);

    order.items.forEach(function(item, index) {
        // Số lượng phải lớn hơn0
        pm.expect(item.quantity).to.be.above(0);

        // Giá phải lớn hơn hoặc bằng0
        pm.expect(item.price).to.be.below(999999);

        // SKU Không thể để trống
        pm.expect(item.sku).to.be.a('string');
        pm.expect(item.sku.length).to.be.above(0);

        console.log('✓ hàng hóa', (index + 1), 'Xác minh đã được thông qua:', item.name);
    });
});

// 6. Kiểm tra tính toàn vẹn dữ liệu
pm.test("Đã kiểm tra các trường bắt buộc", function () {
    const requiredFields = [
        'orderId',
        'userId',
        'status',
        'totalAmount',
        'createTime',
        'items'
    ];

    const order = jsonData.data.order;
    requiredFields.forEach(function(field) {
        pm.expect(order).to.have.property(field);
        pm.expect(order[field]).to.not.equal(null);
        pm.expect(order[field]).to.not.equal(undefined);
        console.log('✓ lĩnh vực', field, 'Tồn tại và có giá trị');
    });
});
```

### Đoạn mã 9：Thư viện chức năng xác nhận tùy chỉnh

```javascript
// Post-request Script: Tạo các hàm xác nhận có thể tái sử dụng

// Xác định thư viện hàm xác nhận tùy chỉnh
const customAssert = {
    // Khẳng định rằng một mảng chứa các phần tử cụ thể
    arrayContains: function(array, element, message) {
        const contains = array.indexOf(element) !== -1;
        if (!contains) {
            throw new Error(message || 'Mảng không chứa phần tử nào: ' + element);
        }
        return true;
    },

    // Khẳng định rằng độ dài chuỗi nằm trong phạm vi
    stringLengthBetween: function(str, min, max, message) {
        if (str.length < min || str.length > max) {
            throw new Error(message || 'Độ dài chuỗi phải ở mức  ' + min + '-' + max + ' giữa');
        }
        return true;
    },

    // Khẳng định rằng ngày nằm trong phạm vi
    dateInRange: function(dateStr, startDate, endDate, message) {
        const date = new Date(dateStr);
        const start = new Date(startDate);
        const end = new Date(endDate);

        if (date < start || date > end) {
            throw new Error(message || 'Ngày nằm ngoài phạm vi được chỉ định');
        }
        return true;
    },

    // Khẳng định rằng cấu trúc đối tượng phù hợp
    objectMatchesSchema: function(obj, schema, message) {
        for (let key in schema) {
            if (!obj.hasOwnProperty(key)) {
                throw new Error(message || 'Đối tượng bị thiếu trường: ' + key);
            }

            const expectedType = schema[key];
            const actualType = typeof obj[key];

            if (actualType !== expectedType && !(expectedType === 'array' && Array.isArray(obj[key]))) {
                throw new Error(message || 'lĩnh vực ' + key + ' Lỗi gõ, dự kiến: ' + expectedType + '，thật sự: ' + actualType);
            }
        }
        return true;
    },

    // Khẳng định giá trị liệt kê
    isOneOf: function(value, allowedValues, message) {
        if (allowedValues.indexOf(value) === -1) {
            throw new Error(message || 'Giá trị phải là một trong những giá trị sau: ' + allowedValues.join(', '));
        }
        return true;
    },

    // Khẳng định phạm vi số
    numberInRange: function(num, min, max, message) {
        if (num < min || num > max) {
            throw new Error(message || 'Giá trị phải ở  ' + min + '-' + max + ' giữa');
        }
        return true;
    }
};

// Sử dụng xác nhận tùy chỉnh
const jsonData = pm.response.json();

pm.test("Kiểm tra xác nhận tùy chỉnh", function () {
    const user = jsonData.data.user;

    // Cấu trúc đối tượng thử nghiệm
    customAssert.objectMatchesSchema(user, {
        id: 'string',
        name: 'string',
        age: 'number',
        email: 'string',
        roles: 'array'
    });
    console.log('✓ So khớp cấu trúc đối tượng');

    // Kiểm tra các giá trị liệt kê
    customAssert.isOneOf(user.status, ['active', 'inactive', 'suspended']);
    console.log('✓ Giá trị trạng thái là hợp lệ');

    // Phạm vi giá trị kiểm tra
    customAssert.numberInRange(user.age, 0, 150);
    console.log('✓ Độ tuổi nằm trong phạm vi hợp lệ');

    // Kiểm tra độ dài chuỗi
    customAssert.stringLengthBetween(user.name, 1, 50);
    console.log('✓ Độ dài tên là hợp lệ');

    // Mảng thử nghiệm chứa
    customAssert.arrayContains(user.roles, 'user');
    console.log('✓ Danh sách ký tự chứa các ký tự cơ sở');
});
```

### Đoạn mã 10：Kiểm tra quốc tế hóa đa ngôn ngữ

```javascript
// Pre-request Script: Kiểm tra đa ngôn ngữ quốc tế
const languages = ['zh-CN', 'en-US', 'ja-JP', 'ko-KR', 'fr-FR'];
const currentLang = pm.environment.get('testLanguage') || 'zh-CN';

// Đặt tiêu đề yêu cầu ngôn ngữ
pm.request.headers.upsert({
    key: 'Accept-Language',
    value: currentLang
});

pm.request.headers.upsert({
    key: 'X-Locale',
    value: currentLang
});

console.log('Ngôn ngữ kiểm tra hiện tại:', currentLang);

// Post-request Script: Xác thực phản hồi đa ngôn ngữ
pm.test("Xác thực phản hồi đa ngôn ngữ", function () {
    const jsonData = pm.response.json();
    const lang = pm.request.headers.get('Accept-Language') || 'zh-CN';

    // Kiểm tra trường ngôn ngữ trong phản hồi
    if (jsonData.message) {
        console.log('Trả lời tin nhắn (' + lang + '):', jsonData.message);

        // Xác minh rằng phản hồi bằng tiếng Trung có chứa các ký tự tiếng Trung
        if (lang === 'zh-CN') {
            pm.expect(jsonData.message).to.match(/[\u4e00-\u9fa5]/);
        }

        // Xác minh rằng các câu trả lời bằng tiếng Anh chỉ chứaASCIInhân vật
        if (lang === 'en-US') {
            pm.expect(jsonData.message).to.match(/^[\x00-\x7F]*$/);
        }
    }
});
```

### Đoạn mã 11：Kiểm tra sự bất lực

```javascript
// Post-request Script: Xác minh bình thường (yêu cầu thực hiện cùng một yêu cầu nhiều lần)
const requestId = pm.environment.get('idempotencyTestId');
const responseBody = pm.response.text();
const responseHash = CryptoJS.MD5(responseBody).toString();

if (!requestId) {
    // Yêu cầu đầu tiên, lưu yêu cầuIDvà hàm băm phản hồi
    pm.environment.set('idempotencyTestId', pm.uuid());
    pm.environment.set('idempotencyResponseHash', responseHash);
    pm.environment.set('idempotencyCount', '1');
    console.log('Kiểm tra bất lực: Chương1yêu cầu, đường cơ sở phản hồi đã được lưu');
} else {
    // Đối với các yêu cầu tiếp theo, hãy xác minh xem các phản hồi có nhất quán hay không
    const savedHash = pm.environment.get('idempotencyResponseHash');
    let count = parseInt(pm.environment.get('idempotencyCount') || '1');
    count++;
    pm.environment.set('idempotencyCount', count.toString());

    pm.test("Xác minh bất lực - KHÔNG. " + count + "yêu cầu", function () {
        pm.expect(responseHash).to.equal(savedHash);
        console.log('✓ Trả lời bằng không1Các yêu cầu là nhất quán');
    });

    console.log('Kiểm tra Idempotence: thực hiện', count, 'yêu cầu');

    // Hoàn thành5Dọn dẹp sau mỗi lần kiểm tra
    if (count >= 5) {
        pm.environment.unset('idempotencyTestId');
        pm.environment.unset('idempotencyResponseHash');
        pm.environment.unset('idempotencyCount');
        console.log('✓ Kiểm tra idempotence đã hoàn thành và dữ liệu kiểm tra đã được làm sạch');
    }
}
```

### Đoạn mã 12：Tạo dữ liệu biểu đồ thống kê thời gian đáp ứng

```javascript
// Post-request Script: Thu thập dữ liệu hiệu suất để trình bày biểu đồ
var moment = require('moment');
var _ = require('lodash');

const endpoint = pm.request.url.getPath();
const responseTime = pm.response.responseTime;
const timestamp = moment().format('YYYY-MM-DD HH:mm:ss');

// Lấy dữ liệu lịch sử
let perfData = pm.environment.get('performanceChartData');
if (!perfData) {
    perfData = [];
} else {
    try {
        perfData = JSON.parse(perfData);
    } catch (e) {
        perfData = [];
    }
}

// Thêm điểm dữ liệu mới
perfData.push({
    endpoint: endpoint,
    responseTime: responseTime,
    timestamp: timestamp,
    statusCode: pm.response.code,
    success: pm.response.code === 200
});

// Giữ gần đây100hồ sơ
if (perfData.length > 100) {
    perfData = _.takeRight(perfData, 100);
}

pm.environment.set('performanceChartData', JSON.stringify(perfData));

// Thống kê theo nhóm giao diện
const groupedByEndpoint = _.groupBy(perfData, 'endpoint');
const stats = {};

_.forEach(groupedByEndpoint, function(records, endpoint) {
    const times = _.map(records, 'responseTime');
    stats[endpoint] = {
        count: records.length,
        avg: _.round(_.mean(times), 2),
        min: _.min(times),
        max: _.max(times),
        p50: _.round(times.sort()[Math.floor(times.length * 0.5)], 2),
        p95: _.round(times.sort()[Math.floor(times.length * 0.95)], 2),
        successRate: _.round(_.filter(records, { success: true }).length / records.length * 100, 2)
    };
});

console.log('=== Thống kê hiệu suất ===');
console.log(JSON.stringify(stats, null, 2));

// Lưu số liệu thống kê
pm.environment.set('performanceStats', JSON.stringify(stats));
```

---

## Những điều cần lưu ý

1. **Hạn chế phạm vi**
    - Postman ở giữa `pm.response` Chủ yếu được sử dụng cho Post-response kịch bản
    - EasyPostman Vào Pre-request Các đối tượng phản hồi trống được cung cấp dưới dạng tiện ích mở rộng tương thích ngược và không nên dựa vào dữ liệu bên trong chúng

2. **Loại biến**
    - `pm.environment` Được sử dụng để lưu trữ biến liên tục
    - `pm.variables` là việc thực hiện hiện tại/Các biến tạm thời trong phạm vi đang chạy sẽ không được duy trì
    - `pm.globals` Được sử dụng cho các biến toàn cục cấp ứng dụng
    - Các biến môi trường sẽ được lưu vào các tệp, các biến tạm thời thì không

3. **Phạm vi khẳng định**
    - Những cái thường được sử dụng được hỗ trợ Chai BDD Chuỗi, bao gồm đẳng thức, bao gồm, thuộc tính, loại, tính đều đặn, độ dài, phạm vi số, khóa và thành viên, v.v
    - Chưa cam kết và đầy đủ Chai Hệ sinh thái plug-in nhất quán theo từng mục; nên chạy thử nghiệm khả năng tương thích khi di chuyển các xác nhận phức tạp

4. **Cookie quản lý**
    - `pm.cookies` Cung cấp miền yêu cầu hiện tại Cookie thăm
    - `pm.cookies.jar()` Có thể được quản lý trên các miền Cookie，Cần phải đầy đủ URL
    - Cookie Hoạt động không đồng bộ, sử dụng các hàm gọi lại để xử lý kết quả

5. **Chuyển đổi kiểu**
    - sử dụng `pm.response.json()` Trước khi đảm bảo phản hồi là hợp pháp JSON định dạng
    - Các biến môi trường được tự động chuyển đổi thành chuỗi khi được lưu trữ
    - sử dụng `.toString()` Đảm bảo các loại số được chuyển đổi chính xác

6. **Hoạt động thu thập**
    - Đường dẫn tiêu chuẩn là `pm.request.headers`、`pm.request.url.query`、`pm.request.body.formdata` và `pm.request.body.urlencoded`
    - Các sửa đổi đối với các bộ sưu tập này ảnh hưởng trực tiếp đến các yêu cầu thực tế được gửi (chỉ trong  Pre-request hợp lệ)
    - sử dụng `add()`、`remove()`、`upsert()` Thực hiện các hoạt động thu thập

7. **Thư viện tích hợp**
    - Hỗ trợ `crypto-js`、`lodash`、`moment` Ba thư viện tích hợp
    - ✅ **gợi ý**：Sử dụng trực tiếp các biến toàn cục `CryptoJS`、`_`、`moment`（được tải trước)
    - ✅ **Cũng được hỗ trợ**：sử dụng `require('library-name')` trọng tải
    - Mã thư viện sẽ được lưu vào bộ nhớ đệm và việc tải lặp lại sẽ không ảnh hưởng đến hiệu suất

8. **Các tính năng chưa tương thích hoàn toàn**
    - `pm.collectionVariables`
    - `pm.execution`、`pm.visualizer`、`pm.vault`、`pm.require`
    - `pm.sendRequest()` và Cookie Jar của Promise hình thức

---

## Tham khảo nhanh

### Thường được sử dụng API Kiểm tra nhanh

```javascript
// ===== các biến môi trường =====
pm.environment.set('key', 'value')        // cài đặt
pm.environment.get('key')                 // Lấy
pm.environment.has('key')                 // nghiên cứu
pm.environment.unset('key')               // xóa bỏ
pm.environment.clear()                    // Trống

// ===== Các biến toàn cầu =====
pm.globals.set('key', 'value')            // Đặt các biến toàn cục
pm.globals.get('key')                     // Nhận các biến toàn cục

// ===== Các biến tạm thời =====
pm.variables.set('key', 'value')          // cài đặt
pm.variables.get('key')                   // Lấy
pm.variables.has('key')                   // nghiên cứu
pm.variables.unset('key')                 // xóa bỏ

// ===== Yêu cầu hành động (Pre-request) =====
pm.request.headers.add({key, value})      // Thêm tiêu đề yêu cầu
pm.request.url.query.add({key, value})    // Thêm tham số truy vấn
pm.request.body.formdata.add({key, value, type: 'text'}) // Thêm dữ liệu biểu mẫu
pm.request.body.update('new raw body')    // đổi mới raw body
pm.request.url.toString()                 // Lấy URL

// ===== Trả lời quyền truy cập (Post-request) =====
pm.response.code                          // mã trạng thái
pm.response.status                        // Văn bản trạng thái
pm.response.responseTime                  // thời gian đáp ứng
pm.response.text()                        // Văn bản phản hồi
pm.response.json()                        // phản hồi JSON
pm.response.headers.get('name')           // Nhận tiêu đề phản hồi
pm.response.size()                        // Kích thước phản hồi

// ===== Cookie =====
pm.cookies.get('name')                    // Lấy Cookie
pm.cookies.set({name, value})             // cài đặt Cookie
pm.cookies.has('name')                    // nghiên cứu Cookie
pm.getResponseCookie('name')              // Nhận được từ phản hồi

// ===== Kiểm tra khẳng định =====
pm.test("Tên thử nghiệm", function () {            // Xác định các bài kiểm tra
    pm.response.to.have.status(200)       // Khẳng định mã trạng thái
    pm.expect(value).to.equal(expected)   // Khẳng định bình đẳng
    pm.expect(str).to.include(substr)     // Chứa các khẳng định
    pm.expect(obj).to.have.property('k')  // Khẳng định tài sản
    pm.expect(str).to.match(/regex/)      // Khẳng định thường xuyên
    pm.expect(num).to.be.below(max)       // Khẳng định bằng số
})

// ===== Phương pháp công cụ =====
pm.uuid()                                 // phát ra UUID
pm.getTimestamp()                         // Nhận dấu thời gian
console.log(message)                      // Nhật ký đầu ra

// ===== Thư viện tích hợp (biến toàn cục, được sử dụng trực tiếp)=====
CryptoJS.MD5('text').toString()           // CryptoLibrary (biến toàn cục CryptoJS）
_.random(1, 100)                          // Thư viện công cụ (biến toàn cục _）
moment().format('YYYY-MM-DD')             // Thư viện ngày (biến toàn cục moment）

// Cũng được hỗ trợ require()
var CryptoJS = require('crypto-js')
var _ = require('lodash')
var moment = require('moment')
```

---

## Tài nguyên tham khảo

- [Postman Tài liệu chính thức](https://learning.postman.com/docs/writing-scripts/intro-to-scripts/)
- [Postman Runtime yêu cầu mutation Kiểm tra tích hợp](https://github.com/postmanlabs/postman-runtime/blob/develop/test/integration/mutations/request.test.js)
- [Postman Runtime 7.27.0 body mutation thay đổi](https://github.com/postmanlabs/postman-runtime/commit/3b998e4471919df458e09651de00d4a45e6f3bb2)
- [Postman Collection SDK RequestBody hoàn thành](https://github.com/postmanlabs/postman-collection/blob/develop/lib/collection/request-body.js)
- [ChaiJS Thư viện khẳng định](https://www.chaijs.com/api/bdd/)
