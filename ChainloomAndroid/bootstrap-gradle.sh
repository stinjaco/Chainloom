#!/usr/bin/env bash
set -euo pipefail
VERSION=8.13
ROOT="$(cd "$(dirname "$0")" && pwd)"
BOOT="$ROOT/.gradle-bootstrap"
ZIP="$BOOT/gradle-${VERSION}-bin.zip"
DIST="$BOOT/gradle-${VERSION}"
TMP="$BOOT/wrapper-project"
mkdir -p "$BOOT"
if [ ! -x "$DIST/bin/gradle" ]; then
  echo "Downloading official Gradle ${VERSION}..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip"
  else
    echo "Install curl or wget, then rerun this script." >&2; exit 1
  fi
  python3 - <<PY
import zipfile
zipfile.ZipFile(r'''$ZIP''').extractall(r'''$BOOT''')
PY
fi
rm -rf "$TMP"
mkdir -p "$TMP"
printf "rootProject.name='wrapper-bootstrap'\n" > "$TMP/settings.gradle"
printf "tasks.wrapper { gradleVersion = '${VERSION}'; distributionType = Wrapper.DistributionType.BIN }\n" > "$TMP/build.gradle"
(cd "$TMP" && "$DIST/bin/gradle" wrapper)
cp "$TMP/gradlew" "$ROOT/gradlew"
cp "$TMP/gradlew.bat" "$ROOT/gradlew.bat"
mkdir -p "$ROOT/gradle/wrapper"
cp "$TMP/gradle/wrapper/gradle-wrapper.jar" "$ROOT/gradle/wrapper/gradle-wrapper.jar"
cp "$TMP/gradle/wrapper/gradle-wrapper.properties" "$ROOT/gradle/wrapper/gradle-wrapper.properties"
chmod +x "$ROOT/gradlew"
printf '\nGradle wrapper created. Next run: ./gradlew assembleDebug\n'
