#!/bin/zsh
# Install the app on an Android phone from a web page, over a throwaway Cloudflare tunnel.
#
#   ./scripts/web_install.zsh            # build the signed release APK, then serve it
#   ./scripts/web_install.zsh --no-build # serve the newest APK already built
#
# Builds githubRelease (JDK 17, signed with the key in signature/), starts a local static
# server, fronts it with an HTTPS quick-tunnel, and prints a link. Open it on the phone,
# tap Install, and let the browser install the APK. Ctrl-C to stop; the tunnel and the
# server are torn down on exit.
#
# The first time, Android asks to allow "Install unknown apps" for the browser. An APK
# only updates an existing install when it is signed with the same key, so this uses the
# release build: a debug build would be refused over a release install.
#
# Requirements: cloudflared (brew install cloudflared).

set -e

PROJECT_DIR="${0:A:h:h}"
SERVE="$(mktemp -d)"
PORT=8790
TITLE="ReadYou Reloaded"
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/jdk-17.0.2+8/Contents/Home"

command -v cloudflared >/dev/null || { echo "✗ cloudflared missing: brew install cloudflared" >&2; exit 1; }

if [[ "$1" != "--no-build" ]]; then
  echo "==> building signed release APK"
  (cd "$PROJECT_DIR" && ./gradlew -q assembleGithubRelease)
fi
APK=$(ls -t "$PROJECT_DIR"/app/build/outputs/apk/github/release/*.apk 2>/dev/null | head -1)
[[ -f "$APK" ]] || { echo "✗ no release APK — run without --no-build" >&2; exit 1; }
NAME="${APK:t}"
SIZE=$(( $(stat -f %z "$APK") / 1048576 ))

cp "$APK" "$SERVE/$NAME"

cat > "$SERVE/index.html" <<EOF
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Install $TITLE</title></head>
<body style="font-family:system-ui,sans-serif;text-align:center;padding:60px 24px">
<h2>$TITLE</h2><p>${NAME%.apk} · ${SIZE} MB</p>
<p><a style="display:inline-block;background:#3b6ea5;color:#fff;padding:16px 40px;border-radius:14px;text-decoration:none;font-size:20px"
 href="$NAME" download>Install</a></p>
<p style="color:#888;font-size:14px">When the download finishes, open it and tap Install.<br>
The first time, allow this browser to install unknown apps.</p>
</body></html>
EOF

# Static server with the APK content type, so the browser offers to install it rather
# than saving an unknown binary.
python3 - "$SERVE" "$PORT" >/dev/null 2>&1 <<'PY' &
import http.server, socketserver, sys, os
serve_dir, port = sys.argv[1], int(sys.argv[2])
os.chdir(serve_dir)
class H(http.server.SimpleHTTPRequestHandler):
    extensions_map = {**http.server.SimpleHTTPRequestHandler.extensions_map,
        '.apk': 'application/vnd.android.package-archive', '.html': 'text/html'}
socketserver.TCPServer(("127.0.0.1", port), H).serve_forever()
PY
SERVER_PID=$!

# Anonymous Cloudflare quick-tunnel (no account needed).
LOG=$(mktemp)
cloudflared tunnel --url "http://localhost:$PORT" --no-autoupdate >"$LOG" 2>&1 &
TUNNEL_PID=$!

cleanup() { kill $SERVER_PID $TUNNEL_PID 2>/dev/null || true; rm -rf "$SERVE" "$LOG"; }
trap cleanup EXIT INT TERM

echo "==> bringing up tunnel…"
BASE=""
for i in {1..40}; do
  BASE=$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' "$LOG" | head -1 || true)
  [[ -n "$BASE" ]] && break
  sleep 1
done
[[ -n "$BASE" ]] || { echo "✗ tunnel didn't come up:" >&2; cat "$LOG" >&2; exit 1; }

echo ""
echo "✓ Ready. On the phone, open:"
echo ""
echo "    $BASE"
echo ""
echo "Leave this running; Ctrl-C when done."
wait $TUNNEL_PID
