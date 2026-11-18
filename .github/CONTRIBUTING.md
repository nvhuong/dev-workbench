# Nguyên tắc đóng góp | Contributing Guide

[English](#english) | [Tiếng Trung Quốc](#Tiếng Trung Quốc)

---

## Tiếng Trung Quốc

Cảm ơn bạn vì điều đó DevWorkbench sự chú ý! Chúng tôi hoan nghênh sự đóng góp dưới mọi hình thức.

### 🚀 trước khi bắt đầu

Trước khi gửi đóng góp của bạn, vui lòng đảm bảo:

1. Đọc [README](../README_zh.md) Tìm hiểu về dự án
2. Đã tìm kiếm [Issues](https://github.com/nvhuong/dev-workbench/issues) Xác nhận rằng vấn đề chưa được báo cáo
3. Đã đọc hướng dẫn đóng góp này

### 📋 Những cách đóng góp

#### 1. Báo cáo Bug

Nếu bạn tìm ra Bug，Xin vui lòng:

1. Vào [Issues](https://github.com/nvhuong/dev-workbench/issues) Hãy tìm kiếm xem bạn đã gặp vấn đề tương tự chưa
2. Nếu không, hãy tạo một cái mới Issue，Chọn "🐛 Bug Báo cáo" bản mẫu
3. Điền đầy đủ thông tin bao gồm:
   - Các bước để tái tạo
   - Hành vi mong đợi và hành vi thực tế
   - Thông tin môi trường (hệ điều hành, số phiên bản, v.v.)
   - Nhật ký và ảnh chụp màn hình có liên quan

#### 2. Tính năng gợi ý

Nếu bạn có ý tưởng về các tính năng mới: .

1. Đầu tiên vào [Discussions](https://github.com/nvhuong/dev-workbench/discussions) được thảo luận ở
2. Nếu được chấp thuận, hãy tạo Issue，Chọn "✨ Yêu cầu tính năng" bản mẫu
3. Mô tả chi tiết:
   - Kịch bản sử dụng
   - giải pháp mong muốn
   - Các lựa chọn thay thế
   - Thiết kế giao diện (nếu có liên quan UI）

#### 3. Gửi mã

Đóng góp mã rất được hoan nghênh! .

##### Chuẩn bị môi trường

```bash
# 1. Fork dự án của bạn GitHub Số tài khoản

# 2. Nhân bản của bạn Fork
git clone https://github.com/YOUR_USERNAME/dev-workbench.git
cd dev-workbench

# 3. Thêm kho lưu trữ ngược dòng
git remote add upstream https://github.com/nvhuong/dev-workbench.git

# 4. Cài đặt phụ thuộc
# Hãy chắc chắn rằng nó đã được cài đặt Java 21+
mvn clean install
```

##### quá trình phát triển

```bash
# 1. Đồng bộ hóa mã mới nhất
git checkout master
git pull upstream master

# 2. Tạo các nhánh tính năng
git checkout -b feature/your-feature-name
# Hoặc sửa chữa chi nhánh
git checkout -b fix/your-bug-fix

# 3. cho sự phát triển
# ... Mã hóa ...

# 4. Biên dịch bài kiểm tra (package Các bài kiểm tra được bao gồm; nhật ký kiểm tra được tắt theo mặc định và có thể được thêm vào trong quá trình khắc phục sự cố -Dtest.log.level=DEBUG）
mvn clean package

# 5. Gửi mã
git add .
git commit -m "feat: Thêm mô tả tính năng mới"
# hoặc
git commit -m "fix: Khắc phục sự cố"

# 6. Đã đẩy tới bạn Fork
git push origin feature/your-feature-name

# 7. Tạo Pull Request
# Vào GitHub Được tạo trên PR，Từ chi nhánh của bạn đến upstream/master
```

##### Thông số mã

- **Phong cách mã hóa**：Theo dõi Java Tiêu chuẩn mã hóa
- **Gửi thông tin**：Sử dụng thông tin gửi ngữ nghĩa
  - `feat:` Các tính năng mới
  - `fix:` Bug Sửa chữa
  - `docs:` Tài liệu được cập nhật
  - `style:` Điều chỉnh định dạng mã
  - `refactor:` Tái cấu trúc mã
  - `perf:` Tối ưu hóa hiệu suất
  - `test:` Kiểm tra liên quan
  - `chore:` Xây dựng/Công cụ liên quan
- **Lưu ý**：Thêm nhận xét rõ ràng cho logic phức tạp
- **Bài kiểm tra**：Thêm trường hợp thử nghiệm cho các tính năng mới

##### Pull Request Danh sách kiểm tra

Đang gửi PR Trước khi tiếp tục, hãy đảm bảo:

- [ ] Mã biên dịch thành công (`mvn clean package`）
- [ ] Tất cả các bài kiểm tra đã vượt qua (`mvn test`）
- [ ] Mã tuân theo các tiêu chuẩn mã hóa của dự án
- [ ] Đã thêm ý kiến ​​cần thiết
- [ ] Cập nhật tài liệu liên quan (nếu cần)
- [ ] PR Mô tả rõ ràng, giải thích những thay đổi là gì và tại sao
- [ ] Những cái có liên quan được liên kết Issue（nếu có)

#### 4. Cải thiện tài liệu

Giấy tờ cũng quan trọng không kém! Bạn có thể:

- Sửa lỗi chính tả hoặc mô tả không chính xác
- Ví dụ sử dụng bổ sung
- Dịch tài liệu
- Thêm câu hỏi thường gặp

### 🎯 Đề xuất phát triển

#### Cấu trúc dự án

```
src/main/java/com/nvhuong/
├── devworkbench/           # ứng dụng chính
│   ├── ui/           # UI thành phần
│   ├── service/      # logic kinh doanh
│   ├── model/        # Mô hình dữ liệu
│   ├── utils/        # Công cụ
│   └── network/      # Yêu cầu mạng
└── ...
```

#### Ngăn xếp công nghệ

- **UI khung**: JavaFX / Swing + FlatLaf
- **HTTP khách hàng**: OkHttp
- **JSON xử lý**: Jackson / Gson
- **Git tích hợp**: JGit
- **Công cụ kịch bản**: GraalVM Polyglot

#### Mẹo gỡ lỗi

```bash
# Chạy ở chế độ phát triển
mvn clean compile exec:java -Dexec.mainClass="com.nvhuong.devworkbench.App"

# Bật tính năng ghi nhật ký gỡ lỗi
# Ôn lại src/main/resources/logback.xml Cấp độ nhật ký trong
```

### 🤝 Quy tắc ứng xử

- Hãy đối xử tôn trọng với người khác và giao tiếp một cách thân thiện
- Chấp nhận những lời chỉ trích mang tính xây dựng
- Tập trung vào những gì tốt nhất cho dự án
- Hãy kiên nhẫn và bao dung với người mới

### 📞 Chi tiết liên lạc

- **GitHub Issues**: [nộp Issue](https://github.com/nvhuong/dev-workbench/issues)
- **Discussions**: [Tham gia vào các cuộc thảo luận](https://github.com/nvhuong/dev-workbench/discussions)

### 🙏 tri ân

Cảm ơn vì tất cả DevWorkbench Đóng góp của các nhà phát triển! .

---

## English

Thank you for your interest in DevWorkbench! We welcome all forms of contributions.

### 🚀 Before You Start

Before contributing, please ensure:

1. You've read the [README](../README.md) to understand the project
2. You've searched [Issues](https://github.com/nvhuong/dev-workbench/issues) to confirm the issue hasn't been reported
3. You've read this contributing guide

### 📋 Ways to Contribute

#### 1. Report Bugs

If you find a bug:

1. Search [Issues](https://github.com/nvhuong/dev-workbench/issues) for existing reports
2. If none exist, create a new Issue using the "🐛 Bug Report" template
3. Fill in complete information including:
   - Reproduction steps
   - Expected vs actual behavior
   - Environment info (OS, version, etc.)
   - Relevant logs and screenshots

#### 2. Suggest Features

If you have a feature idea:

1. Discuss it in [Discussions](https://github.com/nvhuong/dev-workbench/discussions) first
2. If approved, create an Issue using the "✨ Feature Request" template
3. Describe in detail:
   - Use cases
   - Desired solution
   - Alternative solutions
   - UI mockups (if applicable)

#### 3. Submit Code

We welcome code contributions!

##### Setup Environment

```bash
# 1. Fork the project to your GitHub account

# 2. Clone your fork
git clone https://github.com/YOUR_USERNAME/dev-workbench.git
cd dev-workbench

# 3. Add upstream remote
git remote add upstream https://github.com/nvhuong/dev-workbench.git

# 4. Install dependencies
# Ensure Java 21+ is installed
mvn clean install
```

##### Development Workflow

```bash
# 1. Sync latest code
git checkout master
git pull upstream master

# 2. Create feature branch
git checkout -b feature/your-feature-name
# or fix branch
git checkout -b fix/your-bug-fix

# 3. Develop
# ... code ...

# 4. Build and test
mvn clean package
mvn test

# 5. Commit changes
git add .
git commit -m "feat: add new feature description"
# or
git commit -m "fix: fix some issue"

# 6. Push to your fork
git push origin feature/your-feature-name

# 7. Create Pull Request
# Create PR on GitHub from your branch to upstream/master
```

##### Code Standards

- **Code Style**: Follow Java coding standards
- **Commit Messages**: Use semantic commit messages
  - `feat:` New feature
  - `fix:` Bug fix
  - `docs:` Documentation update
  - `style:` Code formatting
  - `refactor:` Code refactoring
  - `perf:` Performance improvement
  - `test:` Test related
  - `chore:` Build/tools related
- **Comments**: Add clear comments for complex logic
- **Tests**: Add test cases for new features

##### Pull Request Checklist

Before submitting a PR, ensure:

- [ ] Code compiles successfully (`mvn clean package`)
- [ ] All tests pass (`mvn test`)
- [ ] Code follows project coding standards
- [ ] Added necessary comments
- [ ] Updated relevant documentation (if needed)
- [ ] PR description is clear, explaining changes and reasons
- [ ] Linked related Issues (if any)

#### 4. Improve Documentation

Documentation is equally important! You can:

- Fix typos or inaccurate descriptions
- Add usage examples
- Translate documentation
- Add FAQ entries

### 🎯 Development Tips

#### Project Structure

```
src/main/java/com/nvhuong/
├── devworkbench/           # Main application
│   ├── ui/           # UI components
│   ├── service/      # Business logic
│   ├── model/        # Data models
│   ├── utils/        # Utilities
│   └── network/      # Network requests
└── ...
```

#### Tech Stack

- **UI Framework**: JavaFX / Swing + FlatLaf
- **HTTP Client**: OkHttp
- **JSON Processing**: Jackson / Gson
- **Git Integration**: JGit
- **Script Engine**: GraalVM Polyglot

#### Debug Tips

```bash
# Run in development mode
mvn clean compile exec:java -Dexec.mainClass="com.nvhuong.devworkbench.App"

# Enable debug logging
# Modify log level in src/main/resources/logback.xml
```

### 🤝 Code of Conduct

- Respect others, communicate kindly
- Accept constructive criticism
- Focus on what's best for the project
- Be patient and inclusive with newcomers

### 📞 Contact

- **GitHub Issues**: [Submit Issue](https://github.com/nvhuong/dev-workbench/issues)
- **Discussions**: [Join Discussion](https://github.com/nvhuong/dev-workbench/discussions)

### 🙏 Acknowledgments

Thanks to all developers who have contributed to DevWorkbench!

Contact: phone 0964210295 · email [huong.nguyenvan.hd@gmail.com](mailto:huong.nguyenvan.hd@gmail.com).
