#!/usr/bin/env bash
# First launch on a clean emulator, the way a user gets it: install, tap the launcher icon, wait.
# Done for both packages (Play build and the GitHub copy). Leaves logcat, screenshots and the
# on-screen text in out/, and fails if the app crashed, hit an ANR or is not on screen.
set -u
mkdir -p out
fail=0

# Tap the n-th (0-based) on-screen node whose attribute matches a regex, e.g. tap text 'Fill' 0.
tap() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  xy=$(python3 - "$1" "$2" "${3:-0}" <<'PY'
import re, sys, xml.etree.ElementTree as ET
attr, rx, n = sys.argv[1], re.compile(sys.argv[2]), int(sys.argv[3])
hits = [e for e in ET.parse('/tmp/ui.xml').iter('node') if rx.search(e.get(attr, ''))]
if len(hits) > n:
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', hits[n].get('bounds')))
    print((x1 + x2) // 2, (y1 + y2) // 2)
PY
)
  [ -n "$xy" ] || { echo "nothing to tap: $1 ~ $2 [$3]"; return 1; }
  adb shell input tap $xy
  sleep 2
}

top() { adb shell dumpsys activity activities | grep -m1 -E "topResumedActivity|mResumedActivity"; }
for pair in "NotApp.apk:gift.dhamma.noapp" "NotApp-git.apk:gift.dhamma.noapp.git"; do
  apk=${pair%%:*}; pkg=${pair##*:}
  adb uninstall "$pkg" >/dev/null 2>&1
  adb install "$apk" || { echo "install failed: $apk"; fail=1; continue; }
  adb logcat -c
  for run in 1 2; do
    adb shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null
    sleep 12
    adb exec-out screencap -p > "out/$pkg-run$run.png"
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb pull /sdcard/ui.xml "out/$pkg-run$run.xml" >/dev/null
    adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity|ResumedActivity" > "out/$pkg-run$run-top.txt"
    cat "out/$pkg-run$run-top.txt"
    grep -q "$pkg" "out/$pkg-run$run-top.txt" || { echo "::error::$pkg run $run: app is not on screen"; fail=1; }
    adb shell input keyevent KEYCODE_HOME
    sleep 2
  done
  # A turn of the screen (or, on a foldable, opening/closing it) recreates the Activity. With items
  # configured, the list must still be there afterwards - not the quick sheet, not another app.
  adb uninstall "$pkg" >/dev/null 2>&1
  adb install "$apk" >/dev/null
  adb shell settings put system accelerometer_rotation 0
  adb shell settings put system user_rotation 0
  adb shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 6
  if tap text '^Fill$' && tap checkable true 0 && tap checkable true 1 && tap text '^Fill [0-9]+ slots?$'; then
    adb exec-out screencap -p > "out/$pkg-rotate-before.png"
    echo "before turn: $(top)"
    adb shell settings put system user_rotation 1
    sleep 6
    adb exec-out screencap -p > "out/$pkg-rotate-after.png"
    after=$(top); echo "after turn: $after"
    echo "$after" | grep -q "com.noapp.container.IconDefault\|MainActivity" || { echo "::error::$pkg: turning the screen left the item list ($after)"; fail=1; }
    adb shell settings put system user_rotation 0
  else
    echo "::warning::$pkg: could not fill items, turn check skipped"
  fi
  adb shell input keyevent KEYCODE_HOME

  # Then the first minutes of use: a fresh install again, and random taps on whatever the app shows
  # (fixed seed, so a failure replays). Monkey stops at the first crash or ANR and says so.
  adb uninstall "$pkg" >/dev/null 2>&1
  adb install "$apk" >/dev/null
  adb shell monkey -p "$pkg" -s 42 --throttle 200 --pct-syskeys 0 --pct-appswitch 0 \
    --ignore-security-exceptions -v 1500 > "out/$pkg-monkey.txt" 2>&1
  adb exec-out screencap -p > "out/$pkg-monkey-end.png"
  if grep -qE "// CRASH|NOT RESPONDING" "out/$pkg-monkey.txt"; then
    echo "::error::$pkg crashed under random taps, see out/$pkg-monkey.txt"
    grep -A40 -E "// CRASH|NOT RESPONDING" "out/$pkg-monkey.txt" | head -60
    fail=1
  fi
  adb logcat -d -v threadtime > "out/$pkg-logcat.txt"
  # Our own process lines and anything the system said about it.
  grep -nE "FATAL EXCEPTION|AndroidRuntime|ANR in $pkg|Process $pkg .* has died|Force finishing activity $pkg|Force removing|$pkg.*(crash|died)" "out/$pkg-logcat.txt" | head -80 | tee "out/$pkg-problems.txt"
  if grep -qE "FATAL EXCEPTION|ANR in $pkg|Process $pkg .* has died" "out/$pkg-logcat.txt"; then
    echo "::error::$pkg crashed or hung on first launch, see out/$pkg-problems.txt"; fail=1
  fi
done
exit $fail
