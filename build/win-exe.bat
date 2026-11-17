@echo off
SETLOCAL ENABLEEXTENSIONS

:: ====================================================================
:: EasyPostman Windows EXE Cài đặt tập lệnh xây dựng gói
:: sử dụng jpackage + Inno Setup Tạo EXE Trình cài đặt
:: ====================================================================

echo.
echo ========================================
echo   EasyPostman EXE Installer Builder
echo ========================================
echo.

:: Step 1: Xác định vị trí thư mục gốc của dự án
cd /d "%~dp0%\.."
set PROJECT_ROOT=%cd%

echo [1/9] Kiểm tra môi trường dự án...
if not exist "pom.xml" (
    echo ERROR: pom.xml not found in current directory!
    echo Expected location: %cd%\pom.xml
    pause
    exit /b 1
)

:: Step 2: Từ pom.xml Lấy số phiên bản
echo [2/9] Trích xuất số phiên bản...
FOR /F "tokens=* USEBACKQ" %%F IN (`
    powershell -Command "[regex]::Match((Get-Content '%cd%\pom.xml' -Raw), '<revision>(.*?)</revision>').Groups[1].Value"
`) DO SET VERSION=%%F

if not defined VERSION (
    echo ERROR: Failed to get version from pom.xml
    pause
    exit /b 1
)

echo         Version: %VERSION%

:: Đặt biến
set APP_NAME=EasyPostman
set JAR_NAME_WITH_VERSION=easy-postman-%VERSION%.jar
set JAR_NAME=easy-postman.jar
set MAIN_CLASS=com.laker.postman.App
set ICON_FILE=assets\win\EasyPostman.ico
set ISS_SCRIPT=build\easy-postman.iss
set OUTPUT_DIR=dist
set ARCH=x64
set APP_TARGET_DIR=easy-postman-app\target

:: Step 3: nghiên cứu Java Phiên bản
echo [3/9] nghiên cứu Java Phiên bản...
for /f tokens^=2^ delims^=^" %%a in ('java -version 2^>^&1 ^| findstr version') do set "JVER=%%a"
for /f tokens^=1^ delims^=.^" %%b in ("%JVER%") do set "MAJOR_JVER=%%b"

if %MAJOR_JVER% lss 21 (
    echo ERROR: JDK 21 or higher is required. Current version: %JVER%
    pause
    exit /b 1
)
echo         Java Version: %JVER% [OK]

:: Step 4: nghiên cứu Inno Setup Có nên cài đặt hay không
echo [4/9] nghiên cứu Inno Setup...
where iscc >nul 2>nul
if errorlevel 1 (
    echo ERROR: Inno Setup not found!
    echo Please install Inno Setup from: https://jrsoftware.org/isdl.php
    echo Or install via Chocolatey: choco install innosetup
    pause
    exit /b 1
)
for /f "delims=" %%i in ('where iscc') do set ISCC_PATH=%%i
echo         Inno Setup: %ISCC_PATH% [OK]

:: Step 5: Xây dựng dự án
echo [5/9] sử dụng Maven Xây dựng dự án...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Maven build failed
    pause
    exit /b 1
)

:: nghiên cứu JAR Cho dù tập tin được tạo ra
if not exist "%APP_TARGET_DIR%\%JAR_NAME_WITH_VERSION%" (
    echo ERROR: JAR file not found: %APP_TARGET_DIR%\%JAR_NAME_WITH_VERSION%
    pause
    exit /b 1
)
echo         JAR: %APP_TARGET_DIR%\%JAR_NAME_WITH_VERSION% [OK]

:: Step 6: Tạo tinh gọn JRE (jlink)
echo [6/9] sử dụng jlink Tạo tinh gọn JRE...
if exist target\runtime rd /s /q target\runtime
jlink ^
    --add-modules java.base,java.desktop,java.logging,jdk.unsupported,java.naming,java.net.http,jdk.httpserver,java.prefs,java.sql,java.security.sasl,java.security.jgss,jdk.crypto.ec,jdk.crypto.mscapi,java.management,java.management.rmi,jdk.crypto.cryptoki ^
    --strip-debug ^
    --no-header-files ^
    --no-man-pages ^
    --compress=2 ^
    --output target\runtime
if errorlevel 1 (
    echo ERROR: jlink failed
    pause
    exit /b 1
)
echo         Runtime: target\runtime [OK]

:: Step 7: Chuẩn bị cho việc đóng gói đầu vào
echo [7/9] Chuẩn bị cho việc đóng gói đầu vào...
set DIST_INPUT_DIR=target\dist-input
if exist %DIST_INPUT_DIR% rd /s /q %DIST_INPUT_DIR%
mkdir %DIST_INPUT_DIR%

:: Đổi tên JAR là một tên cố định
copy "%APP_TARGET_DIR%\%JAR_NAME_WITH_VERSION%" "%DIST_INPUT_DIR%\%JAR_NAME%" >nul
if errorlevel 1 (
    echo ERROR: Failed to copy JAR file
    pause
    exit /b 1
)
echo         Input: %DIST_INPUT_DIR%\%JAR_NAME% [OK]

:: Step 8: sử dụng jpackage Tạo hình ảnh ứng dụng
echo [8/9] sử dụng jpackage Tạo hình ảnh ứng dụng...
if exist target\%APP_NAME% rd /s /q target\%APP_NAME%

jpackage ^
    --type app-image ^
    --input %DIST_INPUT_DIR% ^
    --main-jar %JAR_NAME% ^
    --main-class %MAIN_CLASS% ^
    --add-launcher EasyPostmanMCP=build\easy-postman-mcp-launcher.properties ^
    --runtime-image target\runtime ^
    --dest target ^
    --icon %ICON_FILE% ^
    --name %APP_NAME% ^
    --app-version %VERSION% ^
    --vendor "Laker" ^
    --copyright "© 2025 Laker" ^
    --java-options "-Xms256m" ^
    --java-options "-Xmx1g" ^
    --java-options "-XX:MaxMetaspaceSize=256m" ^
    --java-options "-XX:MetaspaceSize=128m" ^
    --java-options "-XX:MaxDirectMemorySize=256m" ^
    --java-options "-XX:+UseG1GC" ^
    --java-options "-XX:MaxGCPauseMillis=200" ^
    --java-options "-XX:InitiatingHeapOccupancyPercent=45" ^
    --java-options "-XX:+UseStringDeduplication" ^
    --java-options "-XX:+HeapDumpOnOutOfMemoryError" ^
    --java-options "-XX:HeapDumpPath=./dumps" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --java-options "-Dswing.aatext=true" ^
    --java-options "-Dhttp.keepAlive=true" ^
    --java-options "-Djavax.accessibility.assistive_technologies="

if errorlevel 1 (
    echo ERROR: jpackage failed
    pause
    exit /b 1
)

if not exist "target\%APP_NAME%\%APP_NAME%.exe" (
    echo ERROR: App image not created properly
    pause
    exit /b 1
)
if not exist "target\%APP_NAME%\EasyPostmanMCP.exe" (
    echo ERROR: MCP launcher was not created
    pause
    exit /b 1
)
echo         App Image: target\%APP_NAME% [OK]

:: Step 9: sử dụng Inno Setup Tạo EXE Trình cài đặt
echo [9/9] sử dụng Inno Setup Tạo EXE Trình cài đặt...
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

if not exist "%ISS_SCRIPT%" (
    echo ERROR: Inno Setup script not found: %ISS_SCRIPT%
    pause
    exit /b 1
)

:: LƯU Ý: Đường dẫn có liên quan đến  ISS vị trí tập lệnh (build thư mục), vì vậy cần thiết ..\
iscc ^
    /DMyAppVersion=%VERSION% ^
    /DMyAppSourceDir=..\target\%APP_NAME% ^
    /DMyOutputDir=..\%OUTPUT_DIR% ^
    /DMyArch=%ARCH% ^
    %ISS_SCRIPT%

if errorlevel 1 (
    echo ERROR: Inno Setup compilation failed
    pause
    exit /b 1
)

:: Xác minh tập tin đầu ra
set EXE_FILE=%OUTPUT_DIR%\%APP_NAME%-%VERSION%-windows-%ARCH%.exe
if not exist "%EXE_FILE%" (
    echo ERROR: EXE file not found: %EXE_FILE%
    echo Files in %OUTPUT_DIR%:
    dir /b "%OUTPUT_DIR%"
    pause
    exit /b 1
)

:: Hiển thị kích thước tập tin
for %%F in ("%EXE_FILE%") do set SIZE=%%~zF
set /a SIZE_MB=%SIZE% / 1048576

:: Thế hệ bổ sung của phiên bản di động ZIP，và viết .portable biểu tượng
echo [Portable] Tạo một phiên bản di động ZIP...
set PORTABLE_ROOT=target\portable
set PORTABLE_APP_DIR=%PORTABLE_ROOT%\%APP_NAME%
set PORTABLE_ZIP=%OUTPUT_DIR%\%APP_NAME%-%VERSION%-windows-%ARCH%-portable.zip

if exist "%PORTABLE_ROOT%" rd /s /q "%PORTABLE_ROOT%"
mkdir "%PORTABLE_APP_DIR%"
xcopy "target\%APP_NAME%" "%PORTABLE_APP_DIR%\" /E /I /Y >nul
if errorlevel 1 (
    echo ERROR: Failed to copy portable app image
    pause
    exit /b 1
)

echo This is a portable version>"%PORTABLE_APP_DIR%\.portable"
powershell -Command "if (Test-Path '%PORTABLE_ZIP%') { Remove-Item '%PORTABLE_ZIP%' -Force }; Compress-Archive -Path '%PORTABLE_APP_DIR%' -DestinationPath '%PORTABLE_ZIP%' -Force"
if errorlevel 1 (
    echo ERROR: Failed to create portable ZIP
    pause
    exit /b 1
)

echo.
echo ========================================
echo   Xây dựng thành công! .
echo ========================================
echo   tập tin: %EXE_FILE%
echo   kích thước: %SIZE_MB% MB
echo   con đường: %cd%\%OUTPUT_DIR%
echo   Phiên bản di động: %PORTABLE_ZIP%
echo ========================================
echo.
echo Cách sử dụng:
echo   - Cài đặt tương tác: %APP_NAME%-%VERSION%-windows-%ARCH%.exe
echo   - Cài đặt âm thầm:   %APP_NAME%-%VERSION%-windows-%ARCH%.exe /VERYSILENT
echo   - Im lặng+Bắt đầu:  %APP_NAME%-%VERSION%-windows-%ARCH%.exe /VERYSILENT /AUTOSTART
echo   - Phiên bản di động ZIP: %APP_NAME%-%VERSION%-windows-%ARCH%-portable.zip
echo.

ENDLOCAL
pause
