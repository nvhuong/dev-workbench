# Script Bảng cheat đoạn mã

Tài liệu này cung cấp chỉ mục nhanh về các đoạn mã tập lệnh thường được sử dụng để tạo điều kiện cho việc tìm kiếm và sao chép nhanh chóng.

## 📑 Thư mục

### Các thao tác cơ bản
- [Đặt các biến môi trường](#Đặt các biến môi trường)
- [Nhận các biến môi trường](#Nhận các biến môi trường)
- [phát raUUID](#phát rauuid)
- [Nhận dấu thời gian](#Nhận dấu thời gian)
- [Thêm tiêu đề yêu cầu](#Thêm tiêu đề yêu cầu)
- [Thêm tham số truy vấn](#Thêm tham số truy vấn)
- [Kịch bản trước được gửi đi HTTP yêu cầu](#Kịch bản trước được gửi đi-http-yêu cầu)

### Chứng nhận liên quan
- [Bearer Token Chứng nhận](#bearer-token-Chứng nhận)
- [Basic Chứng nhận](#basic-Chứng nhận)
- [API Key Chứng nhận](#api-key-Chứng nhận)
- [HMAC dấu hiệu](#hmac-dấu hiệu)
- [OAuth 2.0 Token Làm mới](#oauth-20-token-Làm mới)

### Xử lý dữ liệu
- [phân tích JSON phản hồi](#phân tích-json-phản hồi)
- [Trích xuất dữ liệu lồng nhau](#Trích xuất dữ liệu lồng nhau)
- [Truyền tải mảng và lọc](#Truyền tải mảng và lọc)
- [Chuyển đổi dữ liệu](#Chuyển đổi dữ liệu)
- [JSON Điền mẫu](#json-Điền mẫu)

### Kiểm tra khẳng định
- [Xác nhận mã trạng thái](#Xác nhận mã trạng thái)
- [Khẳng định thời gian đáp ứng](#Khẳng định thời gian đáp ứng)
- [Cơ quan phản hồi khẳng định](#Cơ quan phản hồi khẳng định)
- [Xác nhận tiêu đề phản hồi](#Xác nhận tiêu đề phản hồi)
- [JSON Khẳng định mang tính cấu trúc](#json-Khẳng định mang tính cấu trúc)

### Mã hóa và mã hóa
- [MD5 Băm](#md5-Băm)
- [SHA256 Băm](#sha256-Băm)
- [HMAC-SHA256 dấu hiệu](#hmac-sha256-dấu hiệu)
- [AES Mã hóa/Giải mã](#aes-Mã hóa và giải mã)
- [Base64 Mã hóa/giải mã](#base64-Mã hóa và giải mã)

### Ngày giờ
- [Định dạng thời gian hiện tại](#Định dạng thời gian hiện tại)
- [Tính toán ngày](#Tính toán ngày)
- [So sánh ngày](#So sánh ngày)
- [Phân tích chuỗi ngày](#Phân tích chuỗi ngày)

### Cookie quản lý
- [Lấy Cookie](#Lấy-cookie)
- [cài đặt Cookie](#cài đặt-cookie)
- [Tên miền chéo Cookie quản lý](#Tên miền chéo-cookie-quản lý)

### Nguồn dữ liệu bên ngoài
- [Redis Đọc và viết các khẳng định](#redis-Đọc và viết các khẳng định)
- [Kafka Gửi tin nhắn khẳng định](#kafka-Gửi tin nhắn khẳng định)
- [Elasticsearch Viết các khẳng định](#elasticsearch-Viết các khẳng định)
- [InfluxDB Truy vấn và viết các khẳng định](#influxdb-Truy vấn và viết các khẳng định)

### Công cụ thực tế
- [Tạo dữ liệu ngẫu nhiên](#Tạo dữ liệu ngẫu nhiên)
- [Xác thực dữ liệu](#Xác thực dữ liệu)
- [Giám sát hiệu suất](#Giám sát hiệu suất)
- [Nhật ký gỡ lỗi](#Nhật ký gỡ lỗi)

---

## Các thao tác cơ bản

### Đặt các biến môi trường
```javascript
pm.environment.set('key', 'value');
pm.environment.set('userId', '123');
pm.environment.set('token', 'abc123xyz');
```

### Nhận các biến môi trường
```javascript
const value = pm.environment.get('key');
const userId = pm.environment.get('userId');
const token = pm.environment.get('token');
```

### phát raUUID
```javascript
const uuid = pm.uuid();
pm.environment.set('requestId', uuid);
```

### Nhận dấu thời gian
```javascript
// Dấu thời gian mili giây
const timestamp = pm.getTimestamp();
pm.environment.set('timestamp', timestamp);

// Dấu thời gian cấp hai
const unixTime = Math.floor(Date.now() / 1000);
```

### Thêm tiêu đề yêu cầu
```javascript
// Pre-request Script
pm.request.headers.add({
    key: 'X-Custom-Header',
    value: 'custom-value'
});

// Cập nhật hoặc chèn tiêu đề yêu cầu
pm.request.headers.upsert({
    key: 'Authorization',
    value: 'Bearer ' + pm.environment.get('token')
});
```

### Thêm tham số truy vấn
```javascript
// Pre-request Script
pm.request.params.add({
    key: 'timestamp',
    value: Date.now().toString()
});

pm.request.params.add({
    key: 'userId',
    value: pm.environment.get('userId')
});
```

### Kịch bản trước được gửi đi HTTP yêu cầu
```javascript
// Pre-request Script
pm.sendRequest({
    url: 'https://httpbin.org/get?source=dev-workbench&from=pre-script',
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
    console.log('Trạng thái phản hồi:', response.code, response.status);
    console.log('httpbin args:', data.args);
});
```

---

## Chứng nhận liên quan

### Bearer Token Chứng nhận
```javascript
// Pre-request Script
const token = pm.environment.get('authToken');
pm.request.headers.upsert({
    key: 'Authorization',
    value: 'Bearer ' + token
});
```

### Basic Chứng nhận
```javascript
// Pre-request Script
const username = pm.environment.get('username');
const password = pm.environment.get('password');
const credentials = CryptoJS.enc.Base64.stringify(
    CryptoJS.enc.Utf8.parse(username + ':' + password)
);

pm.request.headers.upsert({
    key: 'Authorization',
    value: 'Basic ' + credentials
});
```

### API Key Chứng nhận
```javascript
// Pre-request Script
const apiKey = pm.environment.get('apiKey');

// cách1: Tiêu đề yêu cầu
pm.request.headers.upsert({
    key: 'X-API-Key',
    value: apiKey
});

// cách2: Tham số truy vấn
pm.request.params.add({
    key: 'apiKey',
    value: apiKey
});
```

### HMAC dấu hiệu
```javascript
// Pre-request Script
const timestamp = Date.now().toString();
const method = pm.request.method;
const path = pm.request.url.getPath();
const secretKey = pm.environment.get('secretKey');

// Tạo chuỗi chữ ký
const signString = method + '\n' + path + '\n' + timestamp;

// Tính toán HMAC-SHA256
const signature = CryptoJS.HmacSHA256(signString, secretKey).toString();

pm.request.headers.upsert({
    key: 'X-Timestamp',
    value: timestamp
});

pm.request.headers.upsert({
    key: 'X-Signature',
    value: signature
});
```

### OAuth 2.0 Token Làm mới
```javascript
// Pre-request Script
const tokenExpireTime = pm.environment.get('tokenExpireTime');
const currentTime = Date.now();

if (!tokenExpireTime || currentTime > (parseInt(tokenExpireTime) - 300000)) {
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
            console.error('Làm mới không thành công:', err.message);
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
        }
    });
} else {
    const token = pm.environment.get('authToken');
    pm.request.headers.upsert({
        key: 'Authorization',
        value: 'Bearer ' + token
    });
}
```

---

## Xử lý dữ liệu

### phân tích JSON phản hồi
```javascript
// Post-request Script
const jsonData = pm.response.json();
console.log('dữ liệu phản hồi:', jsonData);

// Lưu dữ liệu vào các biến môi trường
pm.environment.set('userId', jsonData.data.userId);
pm.environment.set('username', jsonData.data.username);
```

### Trích xuất dữ liệu lồng nhau
```javascript
// Post-request Script
const jsonData = pm.response.json();

// sử dụng lodash Khai thác an toàn
const userId = _.get(jsonData, 'data.user.id');
const userName = _.get(jsonData, 'data.user.profile.name');
const firstEmail = _.get(jsonData, 'data.user.emails[0]');

pm.environment.set('userId', userId);
console.log('Tên người dùng:', userName);
```

### Truyền tải mảng và lọc
```javascript
// Post-request Script
const jsonData = pm.response.json();
const users = jsonData.data.users;

// Lọc người dùng đang hoạt động
const activeUsers = _.filter(users, { status: 'active' });
console.log('Số lượng người dùng đang hoạt động:', activeUsers.length);

// Trích xuất tất cả người dùngID
const userIds = _.map(users, 'id');
pm.environment.set('userIds', JSON.stringify(userIds));

// Nhóm theo vai trò
const groupedByRole = _.groupBy(users, 'role');
console.log('Nhóm theo vai trò:', groupedByRole);
```

### Chuyển đổi dữ liệu
```javascript
// Post-request Script
const jsonData = pm.response.json();

// Chuyển đổi sang định dạng được chỉ định
const transformed = _.map(jsonData.data.items, function(item) {
    return {
        id: item.id,
        name: item.name,
        price: _.round(item.price, 2),
        category: _.upperFirst(item.category)
    };
});

console.log('Dữ liệu được chuyển đổi:', transformed);
```

### JSON Điền mẫu
```javascript
// Pre-request Script
const template = {
    "userId": "{{userId}}",
    "timestamp": "{{timestamp}}",
    "action": "{{action}}"
};

const data = {
    userId: pm.environment.get('userId'),
    timestamp: Date.now(),
    action: 'getUserInfo'
};

let json = JSON.stringify(template);
_.forEach(data, function(value, key) {
    json = json.replace('{{' + key + '}}', value);
});

console.log('Sau khi điềnJSON:', json);
```

---

## Kiểm tra khẳng định

### Xác nhận mã trạng thái
```javascript
// Post-request Script
pm.test("Mã trạng thái là  200", function () {
    pm.response.to.have.status(200);
});

pm.test("Mã trạng thái là  2xx", function () {
    pm.expect(pm.response.code).to.be.below(300);
    pm.expect(pm.response.code).to.be.above(199);
});
```

### Khẳng định thời gian đáp ứng
```javascript
// Post-request Script
pm.test("Thời gian đáp ứng ít hơn 500ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(500);
});

pm.test("Thời gian đáp ứng ít hơn 1 giây", function () {
    pm.expect(pm.response.responseTime).to.be.below(1000);
});
```

### Cơ quan phản hồi khẳng định
```javascript
// Post-request Script
pm.test("Phản hồi có chứa một chỉ báo thành công", function () {
    pm.expect(pm.response.text()).to.include('success');
});

pm.test("Nội dung phản hồi không trống", function () {
    pm.expect(pm.response.text().length).to.be.above(0);
});
```

### Xác nhận tiêu đề phản hồi
```javascript
// Post-request Script
pm.test("Câu trả lời có chứa Content-Type", function () {
    pm.response.to.have.header('Content-Type');
});

pm.test("Content-Type Vâng JSON", function () {
    const contentType = pm.response.headers.get('Content-Type');
    pm.expect(contentType).to.include('application/json');
});
```

### JSON Khẳng định mang tính cấu trúc
```javascript
// Post-request Script
pm.test("Phản hồi chứa các trường bắt buộc", function () {
    const jsonData = pm.response.json();

    pm.expect(jsonData).to.have.property('code');
    pm.expect(jsonData).to.have.property('message');
    pm.expect(jsonData).to.have.property('data');

    pm.expect(jsonData.code).to.equal(200);
    pm.expect(jsonData.data).to.have.property('userId');
});
```

---

## Mã hóa và mã hóa

### MD5 Băm
```javascript
// Pre-request Script
const password = 'myPassword123';
const md5Hash = CryptoJS.MD5(password).toString();

pm.environment.set('passwordHash', md5Hash);
console.log('MD5:', md5Hash);
```

### SHA256 Băm
```javascript
// Pre-request Script
const data = 'sensitive data';
const sha256Hash = CryptoJS.SHA256(data).toString();

pm.environment.set('dataHash', sha256Hash);
console.log('SHA256:', sha256Hash);
```

### HMAC-SHA256 dấu hiệu
```javascript
// Pre-request Script
const data = 'userId=123&timestamp=' + Date.now();
const secretKey = pm.environment.get('secretKey');
const signature = CryptoJS.HmacSHA256(data, secretKey).toString();

pm.environment.set('signature', signature);
console.log('dấu hiệu:', signature);
```

### AES Mã hóa/Giải mã
```javascript
// Mã hóa
const message = 'secret message';
const key = 'my-secret-key';
const encrypted = CryptoJS.AES.encrypt(message, key).toString();
pm.environment.set('encrypted', encrypted);

// Giải mã
const encryptedData = pm.environment.get('encrypted');
const decrypted = CryptoJS.AES.decrypt(encryptedData, key);
const decryptedText = decrypted.toString(CryptoJS.enc.Utf8);
console.log('Giải mã kết quả:', decryptedText);
```

### Base64 Mã hóa/giải mã
```javascript
// Mã hóa
const text = 'Hello World';
const base64 = CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(text));
console.log('Base64:', base64);

// giải mã
const decoded = CryptoJS.enc.Base64.parse(base64).toString(CryptoJS.enc.Utf8);
console.log('giải mã:', decoded);
```

---

## Ngày giờ

### Định dạng thời gian hiện tại
```javascript
// Các định dạng khác nhau
pm.environment.set('dateYMD', moment().format('YYYY-MM-DD'));
pm.environment.set('dateTime', moment().format('YYYY-MM-DD HH:mm:ss'));
pm.environment.set('isoTime', moment().toISOString());
pm.environment.set('timestamp', moment().valueOf().toString());
pm.environment.set('unixTime', moment().unix().toString());

console.log('thời điểm hiện tại:', moment().format('YYYY-MM-DD HH:mm:ss'));
```

### Tính toán ngày
```javascript
// Cộng và trừ ngày tháng
const tomorrow = moment().add(1, 'days').format('YYYY-MM-DD');
const nextWeek = moment().add(7, 'days').format('YYYY-MM-DD');
const nextMonth = moment().add(1, 'months').format('YYYY-MM-DD');

const yesterday = moment().subtract(1, 'days').format('YYYY-MM-DD');
const lastWeek = moment().subtract(7, 'days').format('YYYY-MM-DD');

pm.environment.set('startDate', lastWeek);
pm.environment.set('endDate', tomorrow);
```

### So sánh ngày
```javascript
const date1 = moment('2024-01-01');
const date2 = moment('2024-12-31');

console.log('date1 < date2:', date1.isBefore(date2));
console.log('date1 > date2:', date1.isAfter(date2));
console.log('date1 == date2:', date1.isSame(date2));

// Tính sự khác biệt trong ngày
const diffDays = date2.diff(date1, 'days');
console.log('Sự khác biệt là ngày:', diffDays);
```

### Phân tích chuỗi ngày
```javascript
// Phân tích các định dạng khác nhau
const date1 = moment('2024-01-01', 'YYYY-MM-DD');
const date2 = moment('01/15/2024', 'MM/DD/YYYY');
const date3 = moment('2024Năm1mặt trăng1Ngày', 'YYYYNămMmặt trăngDNgày');

// Ngày xác minh
const isValid = moment('2024-13-01', 'YYYY-MM-DD', true).isValid();
console.log('Ngày hợp lệ:', isValid); // false

// Lấy phạm vi thời gian
const startOfDay = moment().startOf('day').valueOf();
const endOfDay = moment().endOf('day').valueOf();

pm.environment.set('startTimestamp', startOfDay.toString());
pm.environment.set('endTimestamp', endOfDay.toString());
```

---

## Cookie quản lý

### Lấy Cookie
```javascript
// Post-request Script
const sessionCookie = pm.cookies.get('sessionId');
if (sessionCookie) {
    console.log('Session ID:', sessionCookie.value);
    pm.environment.set('sessionId', sessionCookie.value);
}

// nghiên cứu Cookie tồn tại
if (pm.cookies.has('authToken')) {
    console.log('Chứng nhận Cookie tồn tại');
}

// Nhận tất cả chúng Cookie
const allCookies = pm.cookies.getAll();
console.log('Cookie tổng cộng:', allCookies.length);
```

### cài đặt Cookie
```javascript
// Post-request Script
pm.cookies.set({
    name: 'customToken',
    value: 'abc123',
    domain: 'example.com',
    path: '/',
    httpOnly: true,
    secure: true
});
```

### Tên miền chéo Cookie quản lý
```javascript
// Post-request Script
const jar = pm.cookies.jar();

// cài đặt Cookie
jar.set('https://api.example.com', 'token=xyz123', function(error, cookie) {
    if (error) {
        console.error('Thiết lập không thành công:', error);
    } else {
        console.log('Cookie Đã đặt rồi');
    }
});

// Lấy Cookie
jar.get('https://api.example.com', 'token', function(error, cookie) {
    if (!error && cookie) {
        console.log('Token:', cookie.value);
        pm.environment.set('apiToken', cookie.value);
    }
});
```

---

## Nguồn dữ liệu bên ngoài

### Redis Đọc và viết các khẳng định
```javascript
const redis = pm.plugin('redis');

redis.execute({
    host: 'localhost',
    port: 6379,
    db: 0,
    command: 'SET',
    key: 'order:1001',
    value: '{"id":1001,"status":"CREATED"}'
});

const value = redis.query({
    host: 'localhost',
    port: 6379,
    db: 0,
    command: 'GET',
    key: 'order:1001'
});

pm.test('Redis value exists', function () {
    pm.expect(value).to.include('CREATED');
});
```

### Kafka Gửi tin nhắn khẳng định
```javascript
const kafka = pm.plugin('kafka');

const resp = kafka.send({
    bootstrapServers: 'localhost:9092',
    topic: 'orders',
    key: 'order-1001',
    value: '{"id":1001,"status":"CREATED"}'
});

pm.test('Kafka send success', function () {
    pm.expect(resp.topic).to.equal('orders');
    pm.expect(resp.offset).to.be.least(0);
});
```

### Elasticsearch Viết các khẳng định
```javascript
const resp = pm.elasticsearch.request({
    baseUrl: 'http://localhost:9200',
    method: 'POST',
    path: '/orders/_doc/order-1001',
    body: JSON.stringify({id: 1001, status: 'CREATED'})
});

pm.test('ES index success', function () {
    pm.expect(resp.code).to.be.within(200, 201);
});
```

### InfluxDB Truy vấn và viết các khẳng định
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

pm.test('Influx write success', function () {
    pm.expect(writeResp.code).to.equal(204);
});

const queryResp = pm.influxdb.query({
    baseUrl: 'http://localhost:8086',
    version: 'v1',
    db: 'metrics',
    username: 'root',
    password: 'root',
    query: 'SELECT * FROM cpu ORDER BY time DESC LIMIT 1'
});

pm.test('Influx query success', function () {
    pm.expect(queryResp.code).to.equal(200);
    pm.expect(queryResp.json).to.exist();
});
```

---

## Công cụ thực tế

### Tạo dữ liệu ngẫu nhiên
```javascript
// Pre-request Script

// Số nguyên ngẫu nhiên
const randomId = _.random(10000, 99999);
const randomAge = _.random(18, 60);

// Chọn ngẫu nhiên
const randomStatus = _.sample(['pending', 'approved', 'rejected']);
const randomCity = _.sample(['Beijing', 'Shanghai', 'Guangzhou', 'Shenzhen']);

// ngẫu nhiênUUID
const uuid = pm.uuid();

// Chuỗi ngẫu nhiên
const randomStr = CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Hex);

// Email ngẫu nhiên
const randomEmail = 'test_' + Date.now() + '@example.com';

// Số điện thoại di động ngẫu nhiên
const randomPhone = '138' + _.random(10000000, 99999999);

pm.environment.set('testUserId', randomId.toString());
pm.environment.set('testEmail', randomEmail);
pm.environment.set('testPhone', randomPhone);

console.log('Dữ liệu ngẫu nhiên được tạo ra:');
console.log('  ID:', randomId);
console.log('  Email:', randomEmail);
console.log('  Phone:', randomPhone);
```

### Xác thực dữ liệu
```javascript
// Pre-request Script
const userId = pm.environment.get('userId');
const email = pm.environment.get('email');

// Xác thực các tham số cần thiết
if (!userId) {
    throw new Error('userId Không thể để trống');
}

// Xác minh định dạng email
const emailPattern = /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/;
if (email && !emailPattern.test(email)) {
    throw new Error('Định dạng email không chính xác');
}

// Xác minh phạm vi số
const age = parseInt(pm.environment.get('age'));
if (age && (age < 0 || age > 150)) {
    throw new Error('Phải có tuổi 0-150 giữa');
}

console.log('✓ Xác minh tham số đã được thông qua');
```

### Giám sát hiệu suất
```javascript
// Post-request Script
const responseTime = pm.response.responseTime;

// Ghi lại thời gian phản hồi
console.log('thời gian đáp ứng:', responseTime, 'ms');

// Mức hiệu suất
if (responseTime < 100) {
    console.log('hiệu suất: Tuyệt vời ⭐⭐⭐⭐⭐');
} else if (responseTime < 300) {
    console.log('hiệu suất: Tốt ⭐⭐⭐⭐');
} else if (responseTime < 1000) {
    console.log('hiệu suất: Trung bình ⭐⭐⭐');
} else {
    console.log('hiệu suất: chậm hơn ⭐⭐');
}

// Thống kê tích lũy
let totalTime = parseFloat(pm.environment.get('totalResponseTime') || '0');
let count = parseInt(pm.environment.get('requestCount') || '0');

totalTime += responseTime;
count += 1;

pm.environment.set('totalResponseTime', totalTime.toString());
pm.environment.set('requestCount', count.toString());

console.log('Thời gian phản hồi trung bình:', (totalTime / count).toFixed(2), 'ms');
console.log('Tổng số yêu cầu:', count);
```

### Nhật ký gỡ lỗi
```javascript
// Detailed debugging information
console.log('=== Yêu cầu thông tin ===');
console.log('URL:', pm.request.url.toString());
console.log('Method:', pm.request.method);
console.log('Headers:', pm.request.headers.all());

// Post-request Script
console.log('=== Thông tin phản hồi ===');
console.log('Status:', pm.response.code, pm.response.status);
console.log('Time:', pm.response.responseTime, 'ms');
console.log('Size:', pm.response.size().total, 'bytes');

// Nhật ký tình trạng
const debug = pm.environment.get('debug') === 'true';
if (debug) {
    console.log('Debug người mẫu: Bật lên');
    console.log('cơ quan phản ứng:', pm.response.text());
}

// Nhật ký lỗi
if (pm.response.code !== 200) {
    console.error('✗ Yêu cầu không thành công');
    console.error('mã trạng thái:', pm.response.code);
    console.error('phản hồi:', pm.response.text());
}
```

---

## Các chế độ kết hợp thường được sử dụng

### Hoàn tất quá trình chuẩn bị yêu cầu
```javascript
// Pre-request Script - Ví dụ toàn diện

// 1. Tạo yêu cầuID
const requestId = pm.uuid();
pm.environment.set('requestId', requestId);

// 2. Tạo dấu thời gian
const timestamp = Date.now();
pm.environment.set('timestamp', timestamp.toString());

// 3. Chuẩn bị cho việc chứng nhận
const token = pm.environment.get('authToken');
if (token) {
    pm.request.headers.upsert({
        key: 'Authorization',
        value: 'Bearer ' + token
    });
}

// 4. Thêm tiêu đề yêu cầu chung
pm.request.headers.upsert({
    key: 'X-Request-ID',
    value: requestId
});

pm.request.headers.upsert({
    key: 'X-Timestamp',
    value: timestamp.toString()
});

// 5. Thêm tham số truy vấn
pm.request.params.add({
    key: 'timestamp',
    value: timestamp.toString()
});

// 6. Tạo chữ ký
const secretKey = pm.environment.get('secretKey');
const signData = requestId + timestamp + secretKey;
const signature = CryptoJS.HmacSHA256(signData, secretKey).toString();

pm.request.headers.upsert({
    key: 'X-Signature',
    value: signature
});

console.log('✓ Yêu cầu đã sẵn sàng');
console.log('  Request ID:', requestId);
console.log('  Timestamp:', timestamp);
console.log('  Signature:', signature.substring(0, 16) + '...');
```

### Hoàn tất quá trình xác minh phản hồi
```javascript
// Post-request Script - Ví dụ toàn diện

// 1. Xác minh cơ bản
pm.test("Yêu cầu đã thành công", function () {
    pm.response.to.have.status(200);
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

// 2. Phân tích phản hồi
const jsonData = pm.response.json();

// 3. Xác minh doanh nghiệp
pm.test("Phản hồi của doanh nghiệp là chính xác", function () {
    pm.expect(jsonData).to.have.property('code');
    pm.expect(jsonData.code).to.equal(200);
    pm.expect(jsonData).to.have.property('data');
});

// 4. Trích xuất dữ liệu
if (jsonData.code === 200 && jsonData.data) {
    const userId = _.get(jsonData, 'data.userId');
    const userName = _.get(jsonData, 'data.userName');
    const token = _.get(jsonData, 'data.token');

    if (userId) pm.environment.set('userId', userId);
    if (userName) pm.environment.set('userName', userName);
    if (token) pm.environment.set('authToken', token);

    console.log('✓ Dữ liệu được trích xuất và lưu');
}

// 5. Kỷ lục hiệu suất
const responseTime = pm.response.responseTime;
console.log('thời gian đáp ứng:', responseTime, 'ms');

// 6. Ghi lại các chỉ số chính; kết quả kiểm tra được người chạy tóm tắt
console.log('Tình trạng kinh doanh:', jsonData.code);
```

---

## Sao chép mẫu nhanh chóng

### Pre-request Mẫu cơ bản
```javascript
// Pre-request Script

// các biến môi trường
pm.environment.set('timestamp', Date.now().toString());
pm.environment.set('requestId', pm.uuid());

// Chứng nhận
const token = pm.environment.get('authToken');
if (token) {
    pm.request.headers.upsert({
        key: 'Authorization',
        value: 'Bearer ' + token
    });
}

// Tiêu đề yêu cầu chung
pm.request.headers.upsert({
    key: 'X-Request-ID',
    value: pm.environment.get('requestId')
});

console.log('✓ Pre-request Hoàn thành');
```

### Post-request Mẫu cơ bản
```javascript
// Post-request Script

// Kiểm tra mã trạng thái
pm.test("Mã trạng thái là chính xác", function () {
    pm.response.to.have.status(200);
});

// Kiểm tra thời gian đáp ứng
pm.test("Thời gian đáp ứng là hợp lý", function () {
    pm.expect(pm.response.responseTime).to.be.below(1000);
});

// Phân tích phản hồi
const jsonData = pm.response.json();

// Xác minh doanh nghiệp
pm.test("Phản hồi của doanh nghiệp là chính xác", function () {
    pm.expect(jsonData.code).to.equal(200);
});

// Trích xuất dữ liệu
if (jsonData.data) {
    // Trích xuất dữ liệu khi cần thiết
    console.log('dữ liệu phản hồi:', jsonData.data);
}

console.log('✓ Post-request Hoàn thành');
```

---

## gợi ý

1. **Sử dụng trực tiếp các biến toàn cục**：`CryptoJS`、`_`（lodash）、`moment` Đã tải sẵn, không cần `require()`
2. **Cũng được hỗ trợ require()**：`var CryptoJS = require('crypto-js')` Cũng có sẵn
3. **Hãy tận dụng nó thật tốt console.log**：Thông tin khóa đầu ra trong quá trình gỡ lỗi
4. **Đặt tên biến môi trường**：Sử dụng những tên có ý nghĩa như  `authToken`、`userId`
5. **Xử lý lỗi**：Được sử dụng cho các hoạt động quan trọng try-catch đóng gói
6. **Tối ưu hóa hiệu suất**：Tránh thực hiện các phép tính lặp đi lặp lại trong vòng lặp

---

**Để biết thêm chi tiết vui lòng tham khảo:** [SCRIPT_API_REFERENCE_vi.md](./SCRIPT_API_REFERENCE_vi.md)
