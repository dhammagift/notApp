#!/usr/bin/env bash
# First launch on a clean emulator, the way a user gets it: install, tap the launcher icon, wait.
# Done for both packages (Play build and the GitHub copy). Leaves logcat, screenshots and the
# on-screen text in out/, and fails if the app crashed, hit an ANR or is not on screen.
set -u
mkdir -p out
fail=0
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
