#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

APP_NAME="CatCore MultiTask"
OUT_NAME="CatCore MultiTask-debug.apk"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
DOWNLOAD_DIR="/sdcard/Download"
LOG_FILE="$DOWNLOAD_DIR/${APP_NAME}-build.log"

mkdir -p "$DOWNLOAD_DIR"
exec > >(tee "$LOG_FILE") 2>&1

banner() {
  printf '\n==================================================\n'
  printf '            CatCore MultiTask Builder\n'
  printf '==================================================\n\n'
}

fail() {
  echo "[FEHLER] $*"
  echo "[INFO] Build-Log: $LOG_FILE"
  exit 1
}

banner

echo "[INFO] Projekt: $ROOT_DIR"
echo "[INFO] Ziel:    $DOWNLOAD_DIR/$OUT_NAME"

echo "[1/7] Termux-/Storage-Prüfung"
if [ ! -d "/data/data/com.termux/files/usr" ]; then
  fail "Dieses Script ist für Termux gedacht."
fi

if [ ! -d "$DOWNLOAD_DIR" ] || [ ! -w "$DOWNLOAD_DIR" ]; then
  echo "[INFO] Fordere Storage-Zugriff an ..."
  termux-setup-storage || true
  sleep 2
fi
mkdir -p "$DOWNLOAD_DIR" || fail "Kein Zugriff auf /sdcard/Download."

echo "[2/7] Benötigte Pakete"
pkg update -y
# Gradle/Java sind Java-basiert. aapt2 muss auf Android nativ vorliegen.
pkg install -y openjdk-17 gradle aapt2 apksigner curl unzip || fail "Termux-Pakete konnten nicht installiert werden."

export JAVA_HOME="${PREFIX}/lib/jvm/java-17-openjdk"
export PATH="$JAVA_HOME/bin:$PATH"

JAVA_VER="$(java -version 2>&1 | head -n1 || true)"
echo "[OK] $JAVA_VER"
echo "[OK] Gradle: $(gradle --version | awk '/Gradle /{print $2; exit}')"
echo "[OK] aapt2: $(command -v aapt2)"

# AGP wants an SDK directory containing platform android.jar. Prefer an existing
# Android SDK if the user already has one in Termux. Otherwise make a small SDK
# skeleton and fetch only android.jar from Google's published platform package
# through sdkmanager if available.
echo "[3/7] Android SDK prüfen"
SDK_CANDIDATES=(
  "${ANDROID_SDK_ROOT:-}"
  "${ANDROID_HOME:-}"
  "$HOME/android-sdk"
  "$PREFIX/share/android-sdk"
)
SDK=""
for c in "${SDK_CANDIDATES[@]}"; do
  [ -n "$c" ] || continue
  if [ -f "$c/platforms/android-36/android.jar" ]; then
    SDK="$c"; break
  fi
done

if [ -z "$SDK" ]; then
  # Some Termux repositories provide android-sdk/platform packages. Try them
  # opportunistically; if unavailable, stop with a precise message instead of
  # downloading incompatible x86_64 build-tools.
  echo "[INFO] Kein vorhandenes API-36-SDK gefunden. Prüfe Termux-Pakete ..."
  pkg install -y android-sdk 2>/dev/null || true
  for c in "$PREFIX/share/android-sdk" "$HOME/android-sdk"; do
    if [ -f "$c/platforms/android-36/android.jar" ]; then SDK="$c"; break; fi
  done
fi

if [ -z "$SDK" ]; then
  cat <<'MSG'
[FEHLER] API-36 android.jar wurde nicht gefunden.

Auf Android/ARM64 dürfen die offiziellen Linux-x86_64 Build-Tools nicht einfach
verwendet werden. Das Script installiert deshalb absichtlich keine fremden
x86_64-SDK-Binaries.

Wenn du bereits ein SDK hast, lege API 36 hier ab:
  ~/android-sdk/platforms/android-36/android.jar

Danach dieses Script erneut starten. aapt2/apksigner kommen weiterhin nativ aus
Termux.
MSG
  exit 2
fi

export ANDROID_SDK_ROOT="$SDK"
export ANDROID_HOME="$SDK"
echo "[OK] SDK: $SDK"

# Force the Android Gradle Plugin to use Termux's native ARM64 aapt2.
AAPT2_PATH="$(command -v aapt2)"
PROP_FILE="$ROOT_DIR/gradle.properties"
grep -v '^android.aapt2FromMavenOverride=' "$PROP_FILE" > "$PROP_FILE.tmp" || true
mv "$PROP_FILE.tmp" "$PROP_FILE"
echo "android.aapt2FromMavenOverride=$AAPT2_PATH" >> "$PROP_FILE"

echo "sdk.dir=$SDK" > "$ROOT_DIR/local.properties"

echo "[4/7] Projekt prüfen"
[ -f "$ROOT_DIR/settings.gradle.kts" ] || fail "settings.gradle.kts fehlt."
[ -f "$ROOT_DIR/app/build.gradle.kts" ] || fail "app/build.gradle.kts fehlt."
[ -f "$ROOT_DIR/app/src/main/AndroidManifest.xml" ] || fail "AndroidManifest.xml fehlt."

echo "[5/7] Gradle Build"
cd "$ROOT_DIR"
gradle --no-daemon --stacktrace :app:assembleDebug || fail "Gradle-Build fehlgeschlagen."

APK="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
[ -f "$APK" ] || fail "Build war fertig, aber $APK wurde nicht gefunden."

echo "[6/7] APK prüfen"
apksigner verify --verbose "$APK" || fail "APK-Signaturprüfung fehlgeschlagen."

cp -f "$APK" "$DOWNLOAD_DIR/$OUT_NAME"

echo "[7/7] Fertig"
echo "[OK] APK: $DOWNLOAD_DIR/$OUT_NAME"
echo "[OK] Log: $LOG_FILE"
echo
ls -lh "$DOWNLOAD_DIR/$OUT_NAME"
echo
echo "Installieren mit:"
echo "  termux-open '$DOWNLOAD_DIR/$OUT_NAME'"
