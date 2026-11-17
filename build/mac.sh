#!/bin/bash

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

# Lấy đường dẫn thư mục gốc của dự án (bao gồm pom.xml）
PROJECT_ROOT=$(cd "$(dirname "$0")/.."; pwd)

# Chuyển sang thư mục gốc của dự án và đọc số phiên bản
cd "$PROJECT_ROOT" || exit 1
# Trực tiếp từ pom.xml phân tích revision
VERSION=$(grep -m 1 '<revision>' pom.xml | sed 's/.*<revision>\(.*\)<\/revision>.*/\1/')
echo "🔧 Bắt đầu đóng gói EasyPostman Phiên bản: $VERSION"
APP_NAME="EasyPostman"
JAR_NAME_WITH_VERSION="easy-postman-$VERSION.jar"
JAR_NAME="easy-postman.jar"  # Đã sửa tên không có số phiên bản
MAIN_CLASS="com.laker.postman.App"
ICON_DIR="assets/mac/EasyPostman.icns"
OUTPUT_DIR="dist"
APP_TARGET_DIR="easy-postman-app/target"

# nghiên cứu icns Liệu tập tin biểu tượng có tồn tại hay không
if [ ! -f "$ICON_DIR" ]; then
    echo "❌ tập tin biểu tượng $ICON_DIR Không tồn tại, vui lòng kiểm tra assets/mac/EasyPostman.icns tồn tại."
    exit 1
fi

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

# Step 3.5: Tạo chỉ chứa chủ jar thư mục tạm thời
DIST_INPUT_DIR="target/dist-input"
rm -rf ${DIST_INPUT_DIR}
mkdir -p ${DIST_INPUT_DIR}
cp "${APP_TARGET_DIR}/${JAR_NAME}" ${DIST_INPUT_DIR}/
echo "✅ Sử dụng cố định JAR tên: ${JAR_NAME}"

# Step 4: sử dụng jpackage Đóng gói DMG
# --input Chỉ bao gồm những gì cần thiết cho lần chạy cuối cùng jar Gói và phụ thuộc.
echo "📦 sử dụng jpackage Đóng gói macOS ứng dụng..."
jpackage \
    --input ${DIST_INPUT_DIR} \
    --main-jar ${JAR_NAME} \
    --main-class ${MAIN_CLASS} \
    --add-launcher EasyPostmanMCP=build/easy-postman-mcp-launcher.properties \
    --runtime-image target/runtime \
    --type dmg \
    --name "${APP_NAME}" \
    --app-version "${VERSION}" \
    --dest "${OUTPUT_DIR}" \
    --icon "${ICON_DIR}" \
    --vendor "Laker" \
    --copyright "© 2025 Laker" \
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
    --java-options "-Dsun.java2d.metal=false" \
    --java-options "-Dapple.awt.application.appearance=system" \
    --java-options "-Dswing.aatext=true" \
    --java-options "-Dhttp.keepAlive=true" \
    --java-options "--add-opens java.desktop/java.awt=ALL-UNNAMED" \
    --java-options "--add-opens java.desktop/sun.lwawt=ALL-UNNAMED" \
    --java-options "--add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED" \
    --java-options "--add-exports java.desktop/com.apple.eawt=ALL-UNNAMED"

# Hiển thị thống kê kích thước tập tin
JAR_SIZE=$(du -h "${APP_TARGET_DIR}/${JAR_NAME}" | awk '{print $1}')
RUNTIME_SIZE=$(du -sh target/runtime | awk '{print $1}')
DMG_SIZE=$(du -h "${OUTPUT_DIR}/${APP_NAME}-${VERSION}.dmg" | awk '{print $1}')

# Hoàn thành lời nhắc
echo "🎉 Bao bì đã hoàn tất! Đường dẫn đầu ra: .$(pwd)/${OUTPUT_DIR}"
echo ""
echo "📊 So sánh kích thước:"
echo "   - Fat JAR: ${JAR_SIZE}"
echo "   - Runtime: ${RUNTIME_SIZE}"
echo "   - DMG: ${DMG_SIZE}"
