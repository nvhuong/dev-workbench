# 🚀 Build Guide

## Prerequisites

### Required

- **Java 21** or higher
- **Maven 3.6+**
- **Git**

### Recommended

- **[JetBrains Runtime (JBR)](https://github.com/JetBrains/JetBrainsRuntime)** - Optimized JDK for Swing applications

> 💡 **Why JetBrains Runtime?**
> 
> JetBrains Runtime is a JDK distribution optimized for Swing/AWT applications, providing:
> - ✨ Better Swing/AWT rendering performance
> - 🎨 Improved font rendering and HiDPI support
> - 🐛 Fixes for Swing-related bugs in standard JDK
> - 🚀 Optimized GC and performance tuning for desktop applications
> 
> **Download**: [JetBrains Runtime Releases](https://github.com/JetBrains/JetBrainsRuntime/releases)

---

## 🔧 Build from Source

### 1. Clone the Repository

```bash
# GitHub
git clone https://github.com/nvhuong/dev-workbench.git
cd dev-workbench

cd dev-workbench
```

### 2. Build the Project

```bash
# Clean and package
mvn clean package

# Skip tests (faster)
mvn clean package -DskipTests
```

This will generate:
- `dev-workbench-app/target/dev-workbench-{version}.jar` - Main executable JAR
- `dev-workbench-mcp/target/dev-workbench-mcp-{version}.jar` - MCP protocol module; it is merged into the main release JAR and is not distributed separately

### 3. Run the Application

```bash
# Run directly
java -jar dev-workbench-app/target/dev-workbench-*.jar

# Or with custom JVM options
java -Xms256m -Xmx2g -jar dev-workbench-app/target/dev-workbench-*.jar

# Verify the MCP Server included in the main JAR
java -jar dev-workbench-app/target/dev-workbench-*.jar mcp serve --help
```

---

## 📦 Generate Native Installers

### macOS

#### Requirements
- macOS 10.13+ (High Sierra or later)
- Xcode Command Line Tools: `xcode-select --install`
- JDK 21+ with jpackage support

#### Build DMG

```bash
# Make script executable
chmod +x build/mac.sh

# Run build script
./build/mac.sh
```

**Output**: `dist/DevWorkbench-{version}-macos-{arch}.dmg`
- For Apple Silicon (M1/M2/M3/M4): `macos-arm64.dmg`
- For Intel: `macos-x86_64.dmg`
- The installed app contains both `Contents/MacOS/DevWorkbench` and `Contents/MacOS/DevWorkbenchMCP`, sharing the main JAR and bundled JRE

#### Manual Build

```bash
# Prepare jpackage input with the fixed JAR name used inside packages
mkdir -p target/dist-input
cp dev-workbench-app/target/dev-workbench-{version}.jar target/dist-input/dev-workbench.jar

# For Apple Silicon
jpackage \
  --input target/dist-input \
  --name DevWorkbench \
  --main-jar dev-workbench.jar \
  --main-class com.nvhuong.devworkbench.App \
  --add-launcher DevWorkbenchMCP=build/dev-workbench-mcp-launcher.properties \
  --type dmg \
  --icon assets/mac/DevWorkbench.icns \
  --app-version {version} \
  --vendor "nvhuong" \
  --mac-package-name "DevWorkbench"

# For Intel
jpackage \
  --input target/dist-input \
  --name DevWorkbench \
  --main-jar dev-workbench.jar \
  --main-class com.nvhuong.devworkbench.App \
  --add-launcher DevWorkbenchMCP=build/dev-workbench-mcp-launcher.properties \
  --type dmg \
  --icon assets/mac/DevWorkbench.icns \
  --app-version {version} \
  --vendor "nvhuong" \
  --mac-package-name "DevWorkbench" \
  --java-options "--add-opens java.base/java.lang=ALL-UNNAMED"
```

---

### Windows

#### Requirements
- Windows 10/11
- JDK 21+ with jpackage support
- [WiX Toolset 3.11+](https://wixtoolset.org/) (for MSI)
- [Inno Setup 6+](https://jrsoftware.org/isinfo.php) (for EXE installer)

#### Build EXE Installer

```batch
# Run build script
build\win-exe.bat
```

**Output**:
- `dist/DevWorkbench-{version}-windows-x64.exe` - Installer
- `dist/DevWorkbench-{version}-windows-x64-portable.zip` - Portable version
- Both contain `DevWorkbench.exe`, `DevWorkbenchMCP.exe`, `app/`, and `runtime/`; after extracting the portable ZIP, MCP works without a separate Java installation

#### Manual Build

```batch
rem Prepare jpackage input
if not exist target\dist-input mkdir target\dist-input
copy dev-workbench-app\target\dev-workbench-{version}.jar target\dist-input\dev-workbench.jar

rem Create an app image with both launchers and the bundled JRE
jpackage ^
  --type app-image ^
  --input target\dist-input ^
  --name DevWorkbench ^
  --main-jar dev-workbench.jar ^
  --main-class com.nvhuong.devworkbench.App ^
  --add-launcher DevWorkbenchMCP=build\dev-workbench-mcp-launcher.properties ^
  --dest target ^
  --icon assets\win\DevWorkbench.ico ^
  --app-version {version} ^
  --vendor "nvhuong"

rem Create a portable ZIP; run build\win-exe.bat for the production EXE installer
echo This is a portable version>target\DevWorkbench\.portable
mkdir target\portable
xcopy target\DevWorkbench target\portable\DevWorkbench\ /E /I
cd target\portable
powershell Compress-Archive -Path DevWorkbench -DestinationPath ..\DevWorkbench-{version}-windows-x64-portable.zip
cd ..\..
```

---

### Linux (Ubuntu/Debian)

#### Requirements
- Ubuntu 18.04+ or Debian 10+
- JDK 21+ with jpackage support
- dpkg-dev: `sudo apt install dpkg-dev`

#### Build DEB Package

```bash
# Make script executable
chmod +x build/linux-deb.sh

# Run build script
./build/linux-deb.sh
```

**Output**: a DEB file under `dist/` for the current build machine architecture, for example `dist/*.deb`

After installation, `/opt/devworkbench/bin/DevWorkbench` and `/opt/devworkbench/bin/DevWorkbenchMCP` share the bundled JRE.

#### Manual Build

```bash
mkdir -p target/dist-input
cp dev-workbench-app/target/dev-workbench-{version}.jar target/dist-input/dev-workbench.jar

jpackage \
  --input target/dist-input \
  --name devworkbench \
  --main-jar dev-workbench.jar \
  --main-class com.nvhuong.devworkbench.App \
  --add-launcher DevWorkbenchMCP=build/dev-workbench-mcp-launcher.properties \
  --type deb \
  --icon assets/linux/DevWorkbench.png \
  --app-version {version} \
  --vendor "nvhuong" \
  --linux-package-name "devworkbench" \
  --linux-menu-group "Development" \
  --linux-shortcut
```

#### Install DEB Package

```bash
sudo dpkg -i dist/<generated-package>.deb

# If dependencies missing
sudo apt-get install -f
```

---

### Linux (RPM-based)

#### Requirements
- Fedora/RHEL/CentOS
- JDK 21+ with jpackage support
- rpm-build: `sudo dnf install rpm-build`

#### Build RPM Package

```bash
# Make script executable
chmod +x build/linux-rpm.sh

# Run build script
./build/linux-rpm.sh
```

**Output**: an RPM file under `dist/` for the current build machine architecture, for example `dist/*.rpm`

---

## 🔍 Advanced Build Options

### Custom JVM Options

Edit the launcher scripts or use jpackage `--java-options`:

```bash
jpackage \
  --java-options "-Xms256m" \
  --java-options "-Xmx2g" \
  --java-options "-Dfile.encoding=UTF-8" \
  # ... other options
```

### Custom Runtime with jlink

Create a minimal JRE with only required modules:

```bash
# List required modules
jdeps --print-module-deps target/dev-workbench-{version}.jar

# Create custom runtime
jlink \
  --add-modules java.base,java.desktop,java.logging,java.naming,java.net.http,jdk.httpserver,java.sql,java.xml \
  --output target/custom-runtime \
  --strip-debug \
  --compress 2 \
  --no-header-files \
  --no-man-pages

# Package with custom runtime
jpackage \
  --runtime-image target/custom-runtime \
  # ... other options
```

---

## 🐛 Troubleshooting

### macOS: "App is damaged and can't be opened"

```bash
# Remove quarantine attribute
sudo xattr -rd com.apple.quarantine /Applications/DevWorkbench.app
```

### Windows: "Windows protected your PC"

Click "More info" → "Run anyway". This is a SmartScreen warning for unsigned applications.

### Linux: Missing dependencies

```bash
# Ubuntu/Debian
sudo apt-get install -f

# Fedora/RHEL
sudo dnf install <missing-package>
```

### Build fails with "jpackage: command not found"

Ensure you're using JDK 21+ (not JRE) and jpackage is in PATH:

```bash
# Check Java version
java -version

# Check jpackage
jpackage --version

# If not found, use full path
$JAVA_HOME/bin/jpackage --version
```

---

## 📚 Additional Resources

- [jpackage Documentation](https://docs.oracle.com/en/java/javase/17/jpackage/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [JetBrains Runtime](https://github.com/JetBrains/JetBrainsRuntime)

---

## 💬 Need Help?

- 💬 GitHub Discussions: [https://github.com/nvhuong/dev-workbench/discussions](https://github.com/nvhuong/dev-workbench/discussions)
- 🐛 Report Issues: [https://github.com/nvhuong/dev-workbench/issues](https://github.com/nvhuong/dev-workbench/issues)

Contact: phone 0964210295 · email [huong.nguyenvan.hd@gmail.com](mailto:huong.nguyenvan.hd@gmail.com).
