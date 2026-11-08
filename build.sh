#!/bin/zsh
# Build TV Home with SDK tools only (no Gradle, no downloads).
#   ./build.sh test   run the JVM unit tests for local.tvhome.core
#   ./build.sh        run tests, then build and sign build/tvhome.apk
#   ./build.sh --new-key   same, creating a signing key first (only for a first build: a new key
#                          can't update an installed copy; Android makes you uninstall it first)
set -euo pipefail
cd "$(dirname "$0")"
OUT=build
SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=$SDK/build-tools/36.0.0
JAR=$SDK/platforms/android-34/android.jar
KS=keystore/debug.jks

run_tests() {
  rm -rf $OUT/test-classes && mkdir -p $OUT/test-classes
  javac -d $OUT/test-classes $(find src/local/tvhome/core test -name '*.java')
  java -Dconf=home.conf.example -cp $OUT/test-classes local.tvhome.core.TestMain
}

build_apk() {
  rm -rf $OUT/gen $OUT/classes $OUT/dex $OUT/res.zip $OUT/unsigned.apk $OUT/aligned.apk $OUT/tvhome.apk
  mkdir -p $OUT/gen $OUT/classes $OUT/dex
  $BT/aapt2 compile --dir res -o $OUT/res.zip
  $BT/aapt2 link -I $JAR --manifest AndroidManifest.xml \
    --min-sdk-version 31 --target-sdk-version 34 \
    --java $OUT/gen -o $OUT/unsigned.apk $OUT/res.zip
  javac --release 11 -cp $JAR -d $OUT/classes $(find src $OUT/gen -name '*.java')
  $BT/d8 --min-api 31 --lib $JAR --output $OUT/dex $(find $OUT/classes -name '*.class')
  (cd $OUT/dex && zip -q -j ../unsigned.apk classes.dex)
  $BT/zipalign -f 4 $OUT/unsigned.apk $OUT/aligned.apk
  if [[ ! -f $KS ]]; then
    if [[ "$NEW_KEY" != 1 ]]; then
      echo "no signing key at $KS. First build? Run ./build.sh --new-key. Lost it? An installed copy must be uninstalled first." >&2
      exit 1
    fi
    mkdir -p keystore
    keytool -genkeypair -keystore $KS -storepass android -keypass android -alias tvhome \
      -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=TV Home" >/dev/null 2>&1
  fi
  $BT/apksigner sign --ks $KS --ks-pass pass:android --ks-key-alias tvhome \
    --out $OUT/tvhome.apk $OUT/aligned.apk
  rm -f $OUT/unsigned.apk $OUT/aligned.apk
  echo "built $OUT/tvhome.apk"
}

NEW_KEY=0
case "${1:-apk}" in
  test) run_tests ;;
  apk) run_tests && build_apk ;;
  --new-key) NEW_KEY=1; run_tests && build_apk ;;
  *) echo "unknown target: $1" >&2; exit 2 ;;
esac
