#!/usr/bin/env bash
# Build DevWorkbench and configure Kilo Code to run its MCP server with java -jar.
#
# Usage:
#   scripts/install-kilo-mcp.sh
#   scripts/install-kilo-mcp.sh --allow-writes
#   scripts/install-kilo-mcp.sh --workspace /absolute/workspace
#   scripts/install-kilo-mcp.sh --project
#   scripts/install-kilo-mcp.sh --jar /path/dev-workbench.jar --skip-build
#   scripts/install-kilo-mcp.sh --uninstall

set -euo pipefail

SERVER_NAME="dev-workbench"
SCOPE="global"
ALLOW_WRITES=0
WORKSPACE=""
SOURCE_JAR=""
SKIP_BUILD=0
UNINSTALL=0
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

usage() {
  sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
}

die() {
  echo "Lỗi: $*" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --project) SCOPE="project" ;;
    --global) SCOPE="global" ;;
    --allow-writes) ALLOW_WRITES=1 ;;
    --workspace) shift; [ $# -gt 0 ] || die "--workspace cần một đường dẫn"; WORKSPACE="$1" ;;
    --jar) shift; [ $# -gt 0 ] || die "--jar cần đường dẫn tới file JAR"; SOURCE_JAR="$1" ;;
    --skip-build) SKIP_BUILD=1 ;;
    --uninstall) UNINSTALL=1 ;;
    -h|--help) usage; exit 0 ;;
    *) die "Tùy chọn không hợp lệ: $1 (xem --help)" ;;
  esac
  shift
done

if [ "$SCOPE" = "project" ]; then
  CONFIG_FILE="$PWD/kilo.jsonc"
else
  CONFIG_FILE="${XDG_CONFIG_HOME:-$HOME/.config}/kilo/kilo.jsonc"
fi

if [ "$UNINSTALL" -eq 0 ]; then
  command -v java >/dev/null 2>&1 || die "Không tìm thấy Java. Hãy cài JDK 21 trở lên."
  command -v python3 >/dev/null 2>&1 || die "Không tìm thấy python3 để cập nhật cấu hình Kilo."

  JAVA_BIN="$(command -v java)"
  JAVA_MAJOR="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9][0-9]*\).*/\1/p')"
  [ -n "$JAVA_MAJOR" ] || die "Không xác định được phiên bản Java."
  [ "$JAVA_MAJOR" -ge 21 ] || die "Cần Java 21 trở lên; phiên bản hiện tại là $JAVA_MAJOR."

  if [ "$SKIP_BUILD" -eq 0 ]; then
    command -v mvn >/dev/null 2>&1 || die "Không tìm thấy Maven."
    echo "==> Build DevWorkbench bằng Java $JAVA_MAJOR"
    (cd "$PROJECT_ROOT" && mvn -B -pl dev-workbench-app -am clean package -DskipTests)
    REVISION="$(sed -n 's:.*<revision>\([^<]*\)</revision>.*:\1:p' "$PROJECT_ROOT/pom.xml" | head -1)"
    [ -n "$REVISION" ] || die "Không đọc được revision từ pom.xml"
    SOURCE_JAR="$PROJECT_ROOT/dev-workbench-app/target/dev-workbench-$REVISION.jar"
  elif [ -z "$SOURCE_JAR" ]; then
    die "--skip-build phải đi cùng --jar /đường/dẫn/dev-workbench.jar"
  fi

  [ -f "$SOURCE_JAR" ] || die "Không tìm thấy JAR: $SOURCE_JAR"
  SOURCE_JAR="$(cd "$(dirname "$SOURCE_JAR")" && pwd)/$(basename "$SOURCE_JAR")"

  if [ "$(uname -s)" = "Darwin" ]; then
    INSTALL_DIR="$HOME/Library/Application Support/DevWorkbench/mcp"
  else
    INSTALL_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/dev-workbench/mcp"
  fi
  INSTALLED_JAR="$INSTALL_DIR/dev-workbench.jar"
  mkdir -p "$INSTALL_DIR"
  TEMP_JAR="$INSTALL_DIR/.dev-workbench.jar.tmp.$$"
  cp "$SOURCE_JAR" "$TEMP_JAR"
  mv -f "$TEMP_JAR" "$INSTALLED_JAR"

  echo "==> Kiểm tra MCP server"
  "$JAVA_BIN" -Dfile.encoding=UTF-8 -jar "$INSTALLED_JAR" mcp serve --help >/dev/null

  if [ -n "$WORKSPACE" ]; then
    [ -d "$WORKSPACE" ] || die "Không tìm thấy workspace: $WORKSPACE"
    WORKSPACE="$(cd "$WORKSPACE" && pwd)"
  fi
fi

command -v python3 >/dev/null 2>&1 || die "Không tìm thấy python3 để cập nhật cấu hình Kilo."
mkdir -p "$(dirname "$CONFIG_FILE")"
BACKUP=""
if [ -f "$CONFIG_FILE" ]; then
  BACKUP="$CONFIG_FILE.bak-$(date +%Y%m%d-%H%M%S)"
  cp "$CONFIG_FILE" "$BACKUP"
fi

JAVA_BIN="${JAVA_BIN:-}" INSTALLED_JAR="${INSTALLED_JAR:-}" WORKSPACE="$WORKSPACE" \
ALLOW_WRITES="$ALLOW_WRITES" UNINSTALL="$UNINSTALL" SERVER_NAME="$SERVER_NAME" \
CONFIG_FILE="$CONFIG_FILE" python3 - <<'PY'
import json
import os
import re
import sys
import tempfile

path = os.environ["CONFIG_FILE"]
server = os.environ["SERVER_NAME"]
uninstall = os.environ["UNINSTALL"] == "1"


def strip_jsonc(text):
    out, index, size = [], 0, len(text)
    in_string = False
    while index < size:
        char = text[index]
        if in_string:
            out.append(char)
            if char == "\\" and index + 1 < size:
                out.append(text[index + 1])
                index += 2
                continue
            if char == '"':
                in_string = False
            index += 1
            continue
        if char == '"':
            in_string = True
            out.append(char)
            index += 1
        elif char == "/" and index + 1 < size and text[index + 1] == "/":
            while index < size and text[index] != "\n":
                index += 1
        elif char == "/" and index + 1 < size and text[index + 1] == "*":
            end = text.find("*/", index + 2)
            if end == -1:
                raise ValueError("comment /* chưa đóng")
            index = end + 2
        else:
            out.append(char)
            index += 1
    return re.sub(r",(\s*[}\]])", r"\1", "".join(out))


data = {}
if os.path.exists(path) and os.path.getsize(path) > 0:
    try:
        with open(path, encoding="utf-8") as handle:
            data = json.loads(strip_jsonc(handle.read()))
    except (ValueError, json.JSONDecodeError) as error:
        sys.exit(f"Lỗi: không đọc được {path}: {error}")
    if not isinstance(data, dict):
        sys.exit(f"Lỗi: {path} phải chứa một JSON object")

mcp = data.get("mcp")
if not isinstance(mcp, dict):
    mcp = {}

if uninstall:
    mcp.pop(server, None)
    print(f"Đã xóa '{server}' khỏi {path}")
else:
    command = [
        os.environ["JAVA_BIN"],
        "-Dfile.encoding=UTF-8",
        "-jar",
        os.environ["INSTALLED_JAR"],
        "mcp",
        "serve",
    ]
    workspace = os.environ.get("WORKSPACE", "")
    if workspace:
        command.append(workspace)
    if os.environ.get("ALLOW_WRITES") == "1":
        command.append("--allow-writes")
    mcp[server] = {
        "type": "local",
        "command": command,
        "enabled": True,
        "timeout": 600000,
    }
    print(f"Đã cấu hình '{server}' trong {path}")

data["mcp"] = mcp
fd, temporary = tempfile.mkstemp(dir=os.path.dirname(path) or ".", prefix=".kilo-", suffix=".tmp")
with os.fdopen(fd, "w", encoding="utf-8") as handle:
    json.dump(data, handle, indent=2, ensure_ascii=False)
    handle.write("\n")
os.replace(temporary, path)
PY

if [ -n "$BACKUP" ]; then
  echo "Bản sao cấu hình cũ: $BACKUP"
fi

if [ "$UNINSTALL" -eq 0 ]; then
  echo "Hoàn tất. Kilo Code sẽ chạy MCP bằng:"
  echo "  $JAVA_BIN -Dfile.encoding=UTF-8 -jar '$INSTALLED_JAR' mcp serve"
  echo "Hãy restart Kilo Code hoặc mở một CLI session mới."
fi
