#!/bin/bash

# Ubuntu/Debian hệ thống DEB Kịch bản đóng gói gói

# nghiên cứu java có sẵn
if ! command -v java &> /dev/null; then
    echo "❌ Không được phát hiện java，Vui lòng cài đặt nó trước JDK 21+ và cấu hình các biến môi trường."
    exit 1
fi
# nghiên cứu mvn có sẵn
if ! command -v mvn &> /dev/null; then
    echo "❌ Không được phát hiện mvn，Vui lòng cài đặt nó trước Maven và cấu hình các biến môi trường."
    exit 1
fi
# nghiên cứu jlink có sẵn
if ! command -v jlink &> /dev/null; then
    echo "❌ Không được phát hiện jlink，Vui lòng xác nhận JDK 21+ Đã cài đặt chính xác."
    exit 1
fi
# nghiên cứu jpackage có sẵn
if ! command -v jpackage &> /dev/null; then
    echo "❌ Không được phát hiện jpackage，Vui lòng xác nhận JDK 21+ Đã cài đặt chính xác."
    exit 1
fi
# nghiên cứu objcopy Nó có sẵn không (jlink bắt buộc)
if ! command -v objcopy &> /dev/null; then
    echo "❌ Không được phát hiện objcopy，Vui lòng cài đặt nó trước binutils："
    echo "   sudo apt-get install binutils"
    exit 1
fi
# Lấy đường dẫn thư mục gốc của dự án (bao gồm pom.xml）
PROJECT_ROOT=$(cd "$(dirname "$0")/.."; pwd)

# Chuyển sang thư mục gốc của dự án và đọc số phiên bản
cd "$PROJECT_ROOT" || exit 1
# Trực tiếp từ pom.xml phân tích revision
VERSION=$(grep -m 1 '<revision>' pom.xml | sed 's/.*<revision>\(.*\)<\/revision>.*/\1/')
echo "🔧 Bắt đầu đóng gói EasyPostman DEB Gói, phiên bản: $VERSION"
APP_NAME="EasyPostman"
JAR_NAME_WITH_VERSION="easy-postman-$VERSION.jar"
JAR_NAME="easy-postman.jar"  # Đã sửa tên không có số phiên bản
MAIN_CLASS="com.laker.postman.App"
ICON_DIR="assets/linux/EasyPostman.png"
OUTPUT_DIR="dist"
APP_TARGET_DIR="easy-postman-app/target"

# nghiên cứu JDK phiên bản hay không >= 21
JAVA_VERSION=$(java -version 2>&1 | grep version | awk '{print substr($3, 2, 3)}' | tr -d '"')
if (( $(echo "$JAVA_VERSION < 21" | bc -l) )); then
    echo "❌ Cần thiết JDK 21 hoặc cao hơn. Phiên bản hiện tại: $JAVA_VERSION"
    exit 1
fi

# Step 1: Dọn dẹp & Xây dựng dự án
echo "🚀 Bắt đầu xây dựng dự án..."
mvn clean package -DskipTests=false
if [ $? -ne 0 ]; then
    echo "❌ Maven Xây dựng không thành công, vui lòng kiểm tra nhật ký lỗi"
    exit 1
fi
# nghiên cứu jar Liệu gói có được tạo thành công hay không
if [ ! -f "${APP_TARGET_DIR}/$JAR_NAME_WITH_VERSION" ]; then
    echo "❌ Bản dựng không được tạo jar gói: ${APP_TARGET_DIR}/$JAR_NAME_WITH_VERSION"
    exit 1
fi

# Đổi tên JAR như một tên cố định (convenience jpackage cấu hình và các bản cập nhật tiếp theo)
echo "📝 Đổi tên JAR: $JAR_NAME_WITH_VERSION -> $JAR_NAME"
cp "${APP_TARGET_DIR}/$JAR_NAME_WITH_VERSION" "${APP_TARGET_DIR}/$JAR_NAME"

# Step 2: Tạo thời gian chạy tối thiểu jlink
echo "⚙️ sử dụng jlink Tạo thời gian chạy tối thiểu..."
rm -rf target/runtime
jlink \
    --add-modules java.base,java.desktop,java.logging,jdk.unsupported,java.naming,java.net.http,jdk.httpserver,java.prefs,java.sql,java.security.sasl,java.security.jgss,jdk.crypto.ec,java.management,java.management.rmi,jdk.crypto.cryptoki \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=2 \
    --output target/runtime

if [ $? -ne 0 ]; then
    echo "❌ jlink Tạo không thành công"
    exit 1
fi

# Step 3: Chuẩn bị thư mục đầu ra
mkdir -p ${OUTPUT_DIR}

# Step 4: Tạo chỉ chứa chủ jar thư mục tạm thời
DIST_INPUT_DIR="target/dist-input"
rm -rf ${DIST_INPUT_DIR}
mkdir -p ${DIST_INPUT_DIR}
cp "${APP_TARGET_DIR}/${JAR_NAME}" ${DIST_INPUT_DIR}/

# Step 5: sử dụng jpackage Đóng gói DEB
echo "📦 sử dụng jpackage Đóng gói DEB gói..."
jpackage \
    --input ${DIST_INPUT_DIR} \
    --main-jar ${JAR_NAME} \
    --main-class ${MAIN_CLASS} \
    --add-launcher EasyPostmanMCP=build/easy-postman-mcp-launcher.properties \
    --runtime-image target/runtime \
    --type deb \
    --name "${APP_NAME}" \
    --app-version "${VERSION}" \
    --dest "${OUTPUT_DIR}" \
    --vendor "Laker" \
    --copyright "© 2025 Laker" \
    --description "A modern API testing tool similar to Postman" \
    --linux-shortcut \
    --linux-menu-group "Development" \
    --linux-app-category "Development" \
    --java-options "-Xms256m" \
    --java-options "-Xmx1g" \
    --java-options "-XX:MaxMetaspaceSize=256m" \
    --java-options "-XX:MetaspaceSize=128m" \
    --java-options "-XX:MaxDirectMemorySize=256m" \
    --java-options "-XX:+UseG1GC" \
    --java-options "-XX:MaxGCPauseMillis=200" \
    --java-options "-XX:InitiatingHeapOccupancyPercent=45" \
    --java-options "-XX:+UseStringDeduplication" \
    --java-options "-XX:+HeapDumpOnOutOfMemoryError" \
    --java-options "-XX:HeapDumpPath=./dumps" \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "-Dawt.useSystemAAFontSettings=on" \
    --java-options "-Dswing.aatext=true" \
    --java-options "-Dhttp.keepAlive=true" \
    --java-options "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED"

if [ $? -ne 0 ]; then
    echo "❌ jpackage Đóng gói không thành công"
    exit 1
fi

# Tự động nhận dạng jpackage được sản xuất DEB Tên tệp để tránh mã hóa cứng lời nhắc kiến ​​trúc amd64
DEB_FILE=$(find "${OUTPUT_DIR}" -maxdepth 1 -type f -name "*.deb" | sort | tail -n 1)

if [ -n "${DEB_FILE}" ]; then
    echo "📦 phát ra DEB: ${DEB_FILE}"
fi

# Hoàn thành lời nhắc
echo "🎉 DEB Gói hàng đã được đóng gói! Đường dẫn đầu ra: .$(pwd)/${OUTPUT_DIR}"
if [ -n "${DEB_FILE}" ]; then
    DEB_BASENAME=$(basename "${DEB_FILE}")
    echo "📝 Lệnh cài đặt: sudo dpkg -i ${OUTPUT_DIR}/${DEB_BASENAME}"
    echo "📝 hoặc sử dụng: sudo apt install ${OUTPUT_DIR}/${DEB_BASENAME}"
else
    echo "📝 Lệnh cài đặt: sudo dpkg -i ${OUTPUT_DIR}/<generated-package>.deb"
    echo "📝 hoặc sử dụng: sudo apt install ${OUTPUT_DIR}/<generated-package>.deb"
fi
echo "📝 Lệnh gỡ cài đặt: sudo dpkg -r easypostman"
