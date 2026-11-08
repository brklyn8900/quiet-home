#!/bin/zsh
# Install TV Home on the TV and push the list (home.conf) and background pictures (backgrounds/).
#   ./install.sh          install the APK, push home.conf and the pictures
#   ./install.sh conf     only push home.conf and the pictures (after editing the list or the folder)
#   ./install.sh show X   push like "conf", then show the picture whose name contains X for 30 minutes
set -euo pipefail
cd "$(dirname "$0")"
TV=${TV:-$(cat .tv 2>/dev/null || true)}   # or keep your TV's address (<ip>:5555) in .tv, which git ignores
ADB=(${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb -s ${TV:?set TV=<tv-ip>:5555 or put it in .tv})
DIR=/sdcard/Android/data/local.tvhome/files

if [[ "${1:-}" != conf && "${1:-}" != show ]]; then
  "${ADB[@]}" install -r build/tvhome.apk
fi
"${ADB[@]}" shell mkdir -p $DIR
CONF=home.conf
[[ -f $CONF ]] || CONF=home.conf.example   # copy the example to home.conf to make it your own
"${ADB[@]}" push $CONF $DIR/home.conf.tmp >/dev/null
"${ADB[@]}" shell mv $DIR/home.conf.tmp $DIR/home.conf
echo "pushed $CONF"

# Pictures: shrink to TV size (1920 px on the long side) as JPEG, then replace the TV's folder with this set.
OUTPICS=build/backgrounds
rm -rf $OUTPICS && mkdir -p $OUTPICS
n=0
mkdir -p backgrounds
for f in backgrounds/*(N.); do
  case "${f:l}" in
    *.jpg|*.jpeg|*.png|*.heic) ;;
    *) continue ;;
  esac
  n=$((n + 1))
  sips -s format jpeg -s formatOptions 85 -Z 1920 "$f" --out "$OUTPICS/$(printf '%03d' $n)-${${f:t:r}// /_}.jpg" >/dev/null
done
"${ADB[@]}" shell rm -rf $DIR/backgrounds
"${ADB[@]}" shell mkdir -p $DIR/backgrounds
if (( n > 0 )); then
  "${ADB[@]}" push $OUTPICS/. $DIR/backgrounds/ >/dev/null
fi
echo "pushed $n background picture(s)"

if [[ "${1:-}" == show ]]; then
  want="${2:?usage: ./install.sh show <part of the picture name>}"
  match=( $OUTPICS/*${want}*.jpg(N) )
  if (( ${#match} != 1 )); then echo "need exactly one picture matching '$want', found ${#match}" >&2; exit 1; fi
  name=${match[1]:t}
  now="$("${ADB[@]}" shell date +%s | tr -d '\r')000"  # TV shell math is 32-bit, so append ms as text
  "${ADB[@]}" shell "printf '%s\n%s\n' '$name' '$now' > $DIR/show-now"
  "${ADB[@]}" shell am start -n local.tvhome/.HomeActivity >/dev/null
  echo "showing $name for 30 minutes"
fi
