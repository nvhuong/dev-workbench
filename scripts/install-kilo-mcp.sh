#!/usr/bin/env bash
# Adds the DevWorkbench MCP server to Kilo Code (extension and CLI).
#
# Usage:
#   scripts/install-kilo-mcp.sh                     # global config: ~/.config/kilo/kilo.jsonc
#   scripts/install-kilo-mcp.sh --project           # this project only: ./kilo.jsonc
#   scripts/install-kilo-mcp.sh --allow-writes      # also enable write tools (--allow-writes)
#   scripts/install-kilo-mcp.sh --workspace DIR     # restrict to one workspace directory
#   scripts/install-kilo-mcp.sh --command PATH      # use this DevWorkbenchMCP launcher
#   scripts/install-kilo-mcp.sh --uninstall         # remove the dev-workbench entry
#
# An existing config is backed up to <file>.bak-<time> first. Comments in an existing
# file are not preserved in the rewritten file (the backup keeps them).

set -euo pipefail

SERVER_NAME="dev-workbench"
SCOPE="global"
ALLOW_WRITES=0
WORKSPACE=""
COMMAND_PATH=""
UNINSTALL=0

usage() {
  sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'
}

die() {
  echo "error: $*" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --project) SCOPE="project" ;;
    --global) SCOPE="global" ;;
    --allow-writes) ALLOW_WRITES=1 ;;
    --workspace) shift; [ $# -gt 0 ] || die "--workspace needs a directory"; WORKSPACE="$1" ;;
    --command) shift; [ $# -gt 0 ] || die "--command needs a path"; COMMAND_PATH="$1" ;;
    --uninstall) UNINSTALL=1 ;;
    -h|--help) usage; exit 0 ;;
    *) die "unknown option: $1 (see --help)" ;;
  esac
  shift
done

command -v python3 >/dev/null 2>&1 || die "python3 is required to edit the Kilo config"

# 1. Find the DevWorkbenchMCP launcher.
find_launcher() {
  if [ -n "$COMMAND_PATH" ]; then
    echo "$COMMAND_PATH"
    return
  fi
  if [ -n "${DEVWORKBENCH_MCP:-}" ]; then
    echo "$DEVWORKBENCH_MCP"
    return
  fi
  local candidates=(
    "/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP"
    "/opt/devworkbench/bin/DevWorkbenchMCP"
    "/usr/bin/DevWorkbenchMCP"
    "/usr/local/bin/DevWorkbenchMCP"
    "$HOME/Applications/DevWorkbench.app/Contents/MacOS/DevWorkbenchMCP"
  )
  local candidate
  for candidate in "${candidates[@]}"; do
    if [ -x "$candidate" ]; then
      echo "$candidate"
      return
    fi
  done
  if command -v DevWorkbenchMCP >/dev/null 2>&1; then
    command -v DevWorkbenchMCP
    return
  fi
  echo ""
}

# 2. Decide the config file.
if [ "$SCOPE" = "project" ]; then
  CONFIG_FILE="$PWD/kilo.jsonc"
else
  CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/kilo"
  CONFIG_FILE="$CONFIG_DIR/kilo.jsonc"
fi

if [ "$UNINSTALL" -eq 0 ]; then
  LAUNCHER="$(find_launcher)"
  if [ -z "$LAUNCHER" ]; then
    die "DevWorkbenchMCP not found. Install DevWorkbench first, or pass --command /path/to/DevWorkbenchMCP"
  fi
  [ -x "$LAUNCHER" ] || die "not executable: $LAUNCHER"
  if [ -n "$WORKSPACE" ]; then
    [ -d "$WORKSPACE" ] || die "workspace directory not found: $WORKSPACE"
    WORKSPACE="$(cd "$WORKSPACE" && pwd)"
  fi
  # Quick check that the launcher answers before we write anything.
  if ! "$LAUNCHER" --help >/dev/null 2>&1; then
    die "$LAUNCHER --help failed; check the DevWorkbench installation"
  fi
fi

mkdir -p "$(dirname "$CONFIG_FILE")"

# 3. Back up an existing file, then merge with Python. The file may be JSONC (comments and trailing
#    commas allowed), so comments are stripped before parsing.
BACKUP=""
if [ -f "$CONFIG_FILE" ]; then
  BACKUP="$CONFIG_FILE.bak-$(date +%Y%m%d-%H%M%S)"
  cp "$CONFIG_FILE" "$BACKUP"
fi

LAUNCHER="${LAUNCHER:-}" WORKSPACE="$WORKSPACE" ALLOW_WRITES="$ALLOW_WRITES" \
UNINSTALL="$UNINSTALL" SERVER_NAME="$SERVER_NAME" CONFIG_FILE="$CONFIG_FILE" \
python3 - <<'PY'
import json, os, re, sys, tempfile

path = os.environ["CONFIG_FILE"]
server = os.environ["SERVER_NAME"]
uninstall = os.environ["UNINSTALL"] == "1"


def strip_jsonc(text):
    """Remove // and /* */ comments and trailing commas, leaving string contents untouched."""
    out, i, n = [], 0, len(text)
    in_string = False
    while i < n:
        c = text[i]
        if in_string:
            out.append(c)
            if c == "\\" and i + 1 < n:
                out.append(text[i + 1])
                i += 2
                continue
            if c == '"':
                in_string = False
            i += 1
            continue
        if c == '"':
            in_string = True
            out.append(c)
            i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            end = text.find("*/", i + 2)
            if end == -1:
                raise ValueError("unterminated /* comment")
            i = end + 2
        else:
            out.append(c)
            i += 1
    cleaned = "".join(out)
    # Trailing commas before } or ]
    return re.sub(r",(\s*[}\]])", r"\1", cleaned)


data = {}
if os.path.exists(path) and os.path.getsize(path) > 0:
    with open(path, encoding="utf-8") as handle:
        try:
            data = json.loads(strip_jsonc(handle.read()))
        except (ValueError, json.JSONDecodeError) as error:
            sys.exit(f"error: cannot parse {path}: {error}. A backup was saved next to it; fix the file and retry.")
    if not isinstance(data, dict):
        sys.exit(f"error: {path} must contain a JSON object")

mcp = data.get("mcp")
if not isinstance(mcp, dict):
    mcp = {}

if uninstall:
    mcp.pop(server, None)
    print(f"removed '{server}' from {path}")
else:
    launcher = os.environ["LAUNCHER"]
    command = [launcher]
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
    print(f"added '{server}' to {path}")

data["mcp"] = mcp
# Write atomically so a failure never leaves a half-written config.
fd, tmp = tempfile.mkstemp(dir=os.path.dirname(path) or ".", prefix=".kilo-", suffix=".tmp")
with os.fdopen(fd, "w", encoding="utf-8") as handle:
    json.dump(data, handle, indent=2, ensure_ascii=False)
    handle.write("\n")
os.replace(tmp, path)
PY

if [ -n "$BACKUP" ]; then
  echo "backup: $BACKUP"
fi

if [ "$UNINSTALL" -eq 0 ]; then
  echo
  echo "Done. Restart Kilo Code (or start a new CLI session) and ask:"
  echo "  Dùng DevWorkbench MCP liệt kê các workspace. Không chạy request nào."
  if [ "$ALLOW_WRITES" -eq 1 ]; then
    echo "Write tools are enabled: only use them with clients and workspaces you trust."
  fi
fi
