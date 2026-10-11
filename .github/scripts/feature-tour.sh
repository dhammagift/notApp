#!/usr/bin/env bash
# Feature tour: Not App driven the way a person uses it, one case after another on one emulator, each case
# recorded as its own video (out/tour/NN-name.mp4) with a screenshot at its end. The cases build on each
# other (the list filled in 01 is what the later ones open), so they run in order.
# Every case is PASS/FAIL in out/tour/results.txt. A case fails on its own check, on a crash or ANR of the app
# in the log while it ran, or on the app's "crashed last time" screen. The script fails if any case did.
#   bash .github/scripts/feature-tour.sh [apk] [package]       (default: NotApp.apk gift.dhamma.noapp)
set -u
APK=${1:-NotApp.apk}
PKG=${2:-gift.dhamma.noapp}
C=com.noapp.container
OUT=out/tour
mkdir -p "$OUT"
: > "$OUT/results.txt"
UI=/tmp/tour-ui.xml

read -r W H < <(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | tr x ' ')
DENS=$(adb shell wm density | grep -oE '[0-9]+' | tail -1)
dp() { echo $(( $1 * DENS / 160 )); }

# --- the screen ---------------------------------------------------------------------------------------------
dump() {
  for _ in 1 2 3; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb pull /sdcard/ui.xml "$UI" >/dev/null 2>&1 && return 0
    sleep 1
  done
  return 1
}
# Centre of the n-th node whose attribute matches a regex (python's re), from the last dump: "x y [x1 y1 x2 y2]".
find_node() {
  python3 - "$UI" "$1" "$2" "${3:-0}" <<'PY'
import re, sys, xml.etree.ElementTree as ET
f, attr, rx, n = sys.argv[1], sys.argv[2], re.compile(sys.argv[3]), int(sys.argv[4])
try: nodes = list(ET.parse(f).iter('node'))
except Exception: sys.exit(1)
hits = [e for e in nodes if rx.search(e.get(attr, ''))]
if len(hits) <= n: sys.exit(1)
x1, y1, x2, y2 = map(int, re.findall(r'\d+', hits[n].get('bounds')))
print((x1 + x2) // 2, (y1 + y2) // 2, x1, y1, x2, y2)
PY
}
has() { dump && find_node "$@" >/dev/null; }
wait_for() {  # attr rx seconds
  local t=0
  while [ "$t" -lt "${3:-10}" ]; do has "$1" "$2" && return 0; sleep 1; t=$((t + 1)); done
  echo "  not on screen: $1 ~ $2"; return 1
}
tap() {  # attr rx [n]
  local xy
  dump && xy=$(find_node "$1" "$2" "${3:-0}") || { echo "  nothing to tap: $1 ~ $2 [${3:-0}]"; cp "$UI" "$OUT/notap-$(date +%s).xml" 2>/dev/null; return 1; }
  set -- $xy
  adb shell input tap "$1" "$2"; sleep 2
}
# The checkable switch on the same line as a row's text (Compose: only the Switch is clickable, not the row).
switch_of() {  # rx -> "x y checked"
  dump || return 1
  python3 - "$UI" "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).iter('node'))
b = lambda e: list(map(int, re.findall(r'\d+', e.get('bounds'))))
rows = [e for e in nodes if re.search(sys.argv[2], e.get('text', ''))]
if not rows: sys.exit(1)
_, y1, _, y2 = b(rows[0])
sw = [e for e in nodes if e.get('checkable') == 'true' and b(e)[1] <= y2 + 120 and b(e)[3] >= y1 - 40]
if not sw: sys.exit(1)
x1, s1, x2, s2 = b(sw[0])
print((x1 + x2) // 2, (s1 + s2) // 2, sw[0].get('checked'))
PY
}
set_switch() {  # rx on|off
  local s; s=$(switch_of "$1") || { echo "  no switch for: $1"; return 1; }
  set -- $s "$2"
  if { [ "$4" = on ] && [ "$3" = false ]; } || { [ "$4" = off ] && [ "$3" = true ]; }; then adb shell input tap "$1" "$2"; sleep 2; fi
}
# Row headlines top to bottom (the Config list): label texts of the rows whose supporting line says "· Item N".
row_order() {
  dump && python3 - "$UI" <<'PY'
import re, sys, xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).iter('node'))
b = lambda e: list(map(int, re.findall(r'\d+', e.get('bounds'))))
sup = sorted((e for e in nodes if re.search(r'· (Item \d+|Main · tap icon)$', e.get('text', ''))), key=lambda e: b(e)[1])
heads = []
for s in sup:
    # The headline: the text right above the supporting line, starting in the same column (the badge is left of it).
    above = [e for e in nodes if e.get('text') and e is not s and abs(b(e)[0] - b(s)[0]) < 8 and 0 < b(s)[1] - b(e)[1] < 120]
    if above: heads.append(max(above, key=lambda e: b(e)[1]).get('text'))
print('|'.join(heads))
PY
}
tap_field() {  # rx: the EditText whose current text matches (a label chip can carry the same word)
  local xy
  dump && xy=$(python3 - "$UI" "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
for e in ET.parse(sys.argv[1]).iter('node'):
    if 'EditText' in e.get('class', '') and re.search(sys.argv[2], e.get('text', '')):
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', e.get('bounds'))); print((x1 + x2) // 2, (y1 + y2) // 2); break
else: sys.exit(1)
PY
) || { echo "  no field: $1"; return 1; }
  adb shell input tap $xy; sleep 1
}
hide_kb() { adb shell dumpsys input_method | grep -q 'mInputShown=true' && adb shell input keyevent KEYCODE_BACK; sleep 1; }
# Dismiss the list sheet the way a thumb does: drag it down by its handle (or from the middle when there is none).
sheet_down() {
  local xy y; dump && xy=$(find_node content-desc '(?i)drag handle') && y=$(echo "$xy" | cut -d' ' -f2) || y=$((H / 2))
  adb shell input swipe $((W / 2)) "$y" $((W / 2)) $((H - $(dp 10))) 150; sleep 3
}
top() { adb shell dumpsys activity activities | grep -m1 -E "mResumedActivity|topResumedActivity"; }
home() { adb shell input keyevent KEYCODE_HOME; sleep 2; }
back() { adb shell input keyevent KEYCODE_BACK; sleep 2; }
launch() { adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 5; }
config() { adb shell am start -n "$PKG/$C.MainActivity" --ez extra_open_config true >/dev/null; sleep 4; }
settings() { config && tap content-desc '^Settings$' && wait_for text '^Settings$' 8; }
# Dismiss what can stand in the way: the review card (always on in a review-test build) and a snackbar.
later() { has text '^Later$' && tap text '^Later$'; return 0; }

# --- cases --------------------------------------------------------------------------------------------------
fails=0
case_run() {  # NN name function
  local name="$1-$2" fn=$3 ok=0 why=""
  echo "=== $name"
  adb logcat -c
  adb shell screenrecord --bit-rate 2000000 --time-limit 170 /sdcard/case.mp4 >/dev/null 2>&1 &
  local rec=$!
  sleep 1
  $fn || { ok=1; why="check failed"; }
  sleep 1
  adb shell pkill -INT screenrecord >/dev/null 2>&1; wait $rec 2>/dev/null; sleep 1
  adb pull /sdcard/case.mp4 "$OUT/$name.mp4" >/dev/null 2>&1
  adb exec-out screencap -p > "$OUT/$name.png"
  adb logcat -d -v threadtime > "$OUT/$name-logcat.txt"
  if grep -qE "FATAL EXCEPTION|ANR in $PKG|Process $PKG .* has died" "$OUT/$name-logcat.txt"; then ok=1; why="${why:+$why; }crash/ANR in the log"; fi
  if has text '^Not App crashed last time$'; then ok=1; why="${why:+$why; }crash screen"; tap text '^Dismiss$'; fi
  if [ $ok = 0 ]; then echo "PASS $name" | tee -a "$OUT/results.txt"
  else echo "FAIL $name: $why" | tee -a "$OUT/results.txt"; fails=$((fails + 1)); fi
}

APPS='^(Calendar|Clock|Contacts)$'

c_fill() {  # fresh install: the Config screen, then Fill with three apps
  launch
  wait_for text '^Item 1$' 10 || return 1
  tap text '^Fill$' && wait_for text '^Fill slots from apps$' 8 || return 1
  wait_for text '^Calendar$' 20 || return 1
  tap text '^Calendar$' && tap text '^Clock$' && tap text '^Contacts$' && tap text '^Fill 3 slots$' || return 1
  wait_for text '^Calendar$' 5 && has text '^Clock$' && has text '^Contacts$'
}
c_add_url() {  # FAB -> URL -> the Translate preset ({{word}}) -> Save
  later; tap content-desc '^Add item$' && tap content-desc '^URL$' || return 1
  wait_for text '^Edit ' 8 || return 1
  tap text '^Translate$' && tap text '^Save$' || return 1
  wait_for text '^Translate$' 5 && has text '^URL · '
}
c_add_app() {  # FAB -> App: the picker opens by itself -> Camera -> Save
  tap content-desc '^Add item$' && tap content-desc '^App$' || return 1
  wait_for text '^Choose app$' 8 && wait_for text '^Camera$' 20 || return 1
  tap text '^Camera$' && tap text '^Save$' || return 1
  wait_for text '^Camera$' 5
}
c_add_intent() {  # FAB -> Intent -> Choose activity… -> an app -> its first activity -> Save
  tap content-desc '^Add item$' && tap content-desc '^Intent$' || return 1
  wait_for text '^Choose activity' 8 && tap text '^Choose activity' || return 1
  wait_for text '^Messages$' 20 && tap text '^Messages$' || return 1
  wait_for text '^Choose activity$' 8 || return 1
  tap text '\.[A-Z][A-Za-z]+$' 0 || return 1
  tap text '^Save$' && wait_for text '^Intent · ' 5
}
c_edit() {  # open Translate, rename it, random emoji, Save
  tap text '^Translate$' && wait_for text '^Edit ' 8 || return 1
  tap_field '^Translate$' || return 1
  adb shell input keyevent KEYCODE_MOVE_END
  for _ in $(seq 12); do adb shell input keyevent KEYCODE_DEL; done
  adb shell input text 'Translator'; sleep 1
  hide_kb
  tap text '^Random$' && tap text '^Save$' || return 1
  wait_for text '^Translator$' 5
}
c_delete_undo() {  # swipe Clock away, Undo brings it back
  local xy; dump && xy=$(find_node text '^Clock$') || return 1
  set -- $xy
  adb shell input swipe $(dp 40) "$2" $((W - $(dp 40))) "$2" 300; sleep 2
  has text '^Removed "Clock"' || { echo "  no Removed snackbar"; return 1; }
  tap text '^Undo$' && wait_for text '^Clock$' 5
}
c_reorder() {  # long-press drag Contacts to the top; fall back to the star if draganddrop is not there
  local before after xy y1
  before=$(row_order); echo "  before: $before"
  dump && xy=$(find_node text '^Contacts$') && y1=$(find_node text '^Calendar$' | cut -d' ' -f2) || return 1
  set -- $xy
  adb shell input draganddrop "$1" "$2" "$1" "$((y1 - $(dp 20)))" 1500 >/dev/null 2>&1 || { echo "  no draganddrop"; }
  sleep 2
  after=$(row_order); echo "  after: $after"
  [ "${after%%|*}" = Contacts ] && return 0
  # Drag did not move it: put Contacts first with its star so the later cases see the same list, and fail.
  local i=0 r; IFS='|' read -ra r <<< "$after"
  for x in "${r[@]}"; do [ "$x" = Contacts ] && break; i=$((i + 1)); done
  tap content-desc '^Main item, opened by a plain tap$' "$i"
  return 1
}
c_list_launch() {  # LIST: the icon opens the sheet; an item opens its app
  home; launch
  top | grep -q QuickPickActivity || { echo "  $(top)"; return 1; }
  later; tap text '^Calendar$' || return 1
  sleep 3; top | grep -qi calendar || { echo "  $(top)"; return 1; }
  home
}
c_sheet_away() {  # swipe the sheet down: it closes (no floating button yet)
  launch; top | grep -q QuickPickActivity || return 1
  sheet_down
  ! top | grep -q QuickPickActivity
}
c_bubble() {  # floating button: on (with overlay granted), swipe away -> bubble, tap -> list, drag to ✕ -> gone
  adb shell appops set "$PKG" SYSTEM_ALERT_WINDOW allow
  settings || return 1
  set_switch '^Floating button in List and Mix modes$' on || return 1
  [ "$(switch_of '^Floating button in List and Mix modes$' | cut -d' ' -f3)" = true ] || { echo "  switch stayed off"; return 1; }
  home; launch; top | grep -q QuickPickActivity || return 1
  sheet_down
  adb shell dumpsys activity services "$PKG" | grep -q QuickPickPeekOverlayService || { echo "  no bubble service"; return 1; }
  local s bx by; s=$(dp 48); bx=$((W - s / 2 - $(dp 20))); by=$((H - s / 2 - $(dp 60)))
  adb exec-out screencap -p > "$OUT/bubble-shown.png"
  adb shell input tap "$bx" "$by"; sleep 3
  top | grep -q QuickPickActivity || { echo "  bubble tap: $(top)"; return 1; }
  sheet_down
  adb shell input swipe "$bx" "$by" $((W / 2)) $((H - $(dp 64))) 1200; sleep 3
  ! adb shell dumpsys activity services "$PKG" | grep -q QuickPickPeekOverlayService || { echo "  bubble still there"; return 1; }
}
c_bubble_stays_off() {  # N20: after ✕ with "bring back" off, the switch reads off, and stays off after another change
  settings || return 1
  [ "$(switch_of '^Floating button in List and Mix modes$' | cut -d' ' -f3)" = false ] || { echo "  floating button still on"; return 1; }
  tap text '^Dark$' && tap text '^System$' || return 1
  [ "$(switch_of '^Floating button in List and Mix modes$' | cut -d' ' -f3)" = false ]
}
c_recents() {  # recent apps row: usage access granted, the switch on, the sheet shows icons before the gear
  adb shell appops set "$PKG" GET_USAGE_STATS allow
  settings || return 1
  set_switch '^Recent apps in List and Mix modes$' on || return 1
  home; launch; top | grep -q QuickPickActivity || return 1
  wait_for content-desc '^Configure$' 5
}
c_theme() {  # Dark, Light, System
  settings || return 1
  tap text '^Dark$' && adb exec-out screencap -p > "$OUT/theme-dark.png" && tap text '^Light$' && tap text '^System$'
}
c_icon_style() {  # pick Bolt, open the app: exactly the IconBolt alias is the launcher entry; then back to Default
  settings || return 1
  tap text '^Bolt$' || return 1
  launch; sleep 3
  local q; q=$(adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER "$PKG" 2>/dev/null)
  echo "  launcher entry: $q"
  echo "$q" | grep -q IconBolt || return 1
  settings && tap text '^Default$' && launch
}
c_direct() {  # DIRECT: the icon opens item 1 (Contacts) right away, the ⚙ overlay shows; tap it -> Config
  config && later
  tap text '^☰ List$' && wait_for text '^Choose mode$' 8 || return 1
  tap text '^▶ Direct' || return 1
  has text '^How I get you back' && tap text '^Continue$' && back
  tap content-desc '^Close$' || back
  home; launch; sleep 1
  top | grep -qi contacts || { echo "  $(top)"; return 1; }
  adb shell dumpsys activity services "$PKG" | grep -q GearOverlayService || { echo "  no gear overlay"; return 1; }
  adb shell input tap $((W - $(dp 28))) $(( $(dp 24) + $(dp 28) )); sleep 3
  top | grep -q "$C" || { echo "  gear tap: $(top)"; return 1; }
  home
}
c_mix() {  # MIX: item 1 opens and the list comes up over it
  config && later
  tap text '^▶ Direct$' && wait_for text '^Choose mode$' 8 || return 1
  tap text '^◫ Mix' || return 1
  tap content-desc '^Close$' || back
  home; launch; sleep 2
  top | grep -q QuickPickActivity || { echo "  $(top)"; return 1; }
  adb shell dumpsys activity activities | grep -qi 'contacts' || { echo "  item 1 not under the list"; return 1; }
  back; home
  config && tap text '^◫ Mix$' && tap text '^☰ List' && { tap content-desc '^Close$' || back; }
}
c_longpress() {  # the launcher icon's long-press menu lists the items; one of them opens its app
  home
  adb shell dumpsys shortcut > "$OUT/shortcuts.txt" 2>&1
  grep -c "pin:" "$OUT/shortcuts.txt" | sed 's/^/  pin: shortcuts in dumpsys: /'
  adb shell input swipe $((W / 2)) $((H * 9 / 10)) $((W / 2)) $((H / 5)) 300; sleep 2
  local name; name=$(adb shell dumpsys package "$PKG" | grep -q 'gift.dhamma.noapp.git' && echo 'Not App Git' || echo 'Not App')
  local xy; dump && xy=$(find_node text "^$name\$") || { adb shell input text "Not"; sleep 3; dump && xy=$(find_node text "^$name\$"); } || return 1
  set -- $xy
  adb shell input swipe "$1" "$2" "$1" "$2" 1200; sleep 2
  adb exec-out screencap -p > "$OUT/longpress-menu.png"
  tap text '^Clock$' || return 1
  sleep 3; top | grep -qi clock || { echo "  $(top)"; return 1; }
  home
}
c_pin() {  # pin one item to the home screen: the launcher's dialog, then a pinned shortcut in dumpsys
  config && later
  tap content-desc '^Pin to home screen$' 0 || return 1
  adb exec-out screencap -p > "$OUT/pin-dialog.png"
  has text '(?i)^add automatically$' && tap text '(?i)^add automatically$'
  sleep 2
  adb shell dumpsys shortcut > "$OUT/shortcuts-after-pin.txt" 2>&1
  grep -iE 'pinned' "$OUT/shortcuts-after-pin.txt" | head -5
  grep -A30 -i 'pinned' "$OUT/shortcuts-after-pin.txt" | grep -q 'pin:'
}
c_tile() {  # Quick Settings tile: add it, open the shade, tap it -> the list
  local comp="$PKG/$C.NotAppTileService"
  adb shell cmd statusbar add-tile "$comp" >/dev/null 2>&1 \
    || adb shell settings put secure sysui_qs_tiles "$(adb shell settings get secure sysui_qs_tiles | tr -d '\r'),custom($comp)"
  adb shell cmd statusbar expand-settings; sleep 3
  adb exec-out screencap -p > "$OUT/tile-shade.png"
  if ! adb shell cmd statusbar click-tile "$comp" >/dev/null 2>&1; then
    tap text '^Not App' || return 1
  fi
  sleep 4
  top | grep -q QuickPickActivity || { echo "  $(top)"; return 1; }
  back; home
}
c_share() {  # share "kacchapa" to Not App -> "Send ... to:" -> Translator opens the URL with the word in it
  adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT kacchapa -n "$PKG/$C.MainActivity" >/dev/null
  wait_for text '^Send "kacchapa" to:' 8 || return 1
  tap text '^Translator$' || return 1
  sleep 4
  adb logcat -d | grep -E 'START u0' | grep -q 'kacchapa' || { echo "  word not in the started intent: $(top)"; return 1; }
  home
}
c_process_text() {  # text selection -> "Not App" in the menu -> the same sheet
  adb shell am start -a android.intent.action.PROCESS_TEXT -t text/plain --es android.intent.extra.PROCESS_TEXT kacchapa -n "$PKG/$C.ProcessTextActivity" >/dev/null
  wait_for text '^Send "kacchapa" to:' 8 || return 1
  back
}
c_backup_restore() {  # Backup to Downloads, a bad file is refused, the real backup restores
  settings || return 1
  tap text '^Backup$' || return 1
  sleep 3; adb exec-out screencap -p > "$OUT/backup-picker.png"
  tap text '(?i)^save$' || return 1
  sleep 3
  adb shell ls /sdcard/Download/ | grep -q noapp-config || { echo "  no backup file"; return 1; }
  adb shell cat /sdcard/Download/noapp-config.json > "$OUT/backup.json"
  grep -q '"slots"' "$OUT/backup.json" || return 1
  echo '{"hello":"world"}' > /tmp/bad.json; adb push /tmp/bad.json /sdcard/Download/not-a-backup.json >/dev/null
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/not-a-backup.json >/dev/null
  local order; order=$(config >/dev/null; row_order); settings
  tap text '^Restore$' || return 1
  sleep 3
  if ! has text '^not-a-backup.json$'; then tap content-desc '(?i)show roots' && tap text '^Downloads$'; fi
  tap text '^not-a-backup.json$' || return 1
  sleep 2
  [ "$(config >/dev/null; row_order)" = "$order" ] || { echo "  list changed after a bad file"; return 1; }
  settings && tap text '^Restore$' || return 1
  sleep 3
  if ! has text '^noapp-config.json$'; then tap content-desc '(?i)show roots' && tap text '^Downloads$'; fi
  tap text '^noapp-config.json$' || return 1
  sleep 2
  [ "$(config >/dev/null; row_order)" = "$order" ]
}
c_resize_editor() {  # N19: a size change (a foldable) in the editor keeps the editor and the unsaved text
  config && later
  tap text '^Calendar$' && wait_for text '^Edit ' 8 || return 1
  tap_field '^Calendar$' || return 1
  adb shell input keyevent KEYCODE_MOVE_END; adb shell input text 'X'; sleep 1; hide_kb
  adb shell wm size 1080x1200; sleep 5
  local ok=0
  wait_for text '^Edit ' 5 || ok=1
  has text 'CalendarX|XCalendar' || { echo "  unsaved text lost"; ok=1; }
  adb shell wm size reset; sleep 4
  tap content-desc '^Cancel$' || back
  return $ok
}

adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb uninstall "$PKG" >/dev/null 2>&1
adb install "$APK" >/dev/null || { echo "::error::install failed: $APK"; exit 1; }

case_run 01 fill c_fill
case_run 02 add-url c_add_url
case_run 03 add-app c_add_app
case_run 04 add-intent c_add_intent
case_run 05 edit c_edit
case_run 06 delete-undo c_delete_undo
case_run 07 reorder c_reorder
case_run 08 list-launch c_list_launch
case_run 09 sheet-away c_sheet_away
case_run 10 floating-button c_bubble
case_run 11 floating-button-stays-off c_bubble_stays_off
case_run 12 recent-apps c_recents
case_run 13 theme c_theme
case_run 14 icon-style c_icon_style
case_run 15 direct-mode c_direct
case_run 16 mix-mode c_mix
case_run 17 long-press-menu c_longpress
case_run 18 pin-to-home c_pin
case_run 19 quick-settings-tile c_tile
case_run 20 share-word c_share
case_run 21 text-selection c_process_text
case_run 22 backup-restore c_backup_restore
case_run 23 resize-in-editor c_resize_editor

echo; cat "$OUT/results.txt"
[ "$fails" = 0 ] || { echo "::error::$fails feature case(s) failed, see out/tour/results.txt"; exit 1; }
