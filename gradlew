#!/bin/sh
# LuauAI Gradle launcher for Termux; does not require gradle-wrapper.jar.
set -eu
APP_HOME="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
GRADLE_VERSION="8.7"
if command -v gradle >/dev/null 2>&1; then
  exec gradle -p "$APP_HOME" "$@"
fi
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/luauai-distributions"
GRADLE_HOME="$BASE/gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$BASE"
  command -v curl >/dev/null 2>&1 || { echo "Instale curl: pkg install curl" >&2; exit 1; }
  command -v unzip >/dev/null 2>&1 || { echo "Instale unzip: pkg install unzip" >&2; exit 1; }
  TMP="$BASE/gradle-$GRADLE_VERSION.zip"
  echo "Baixando Gradle $GRADLE_VERSION..."
  curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$TMP"
  unzip -q -o "$TMP" -d "$BASE"
  rm -f "$TMP"
fi
exec "$GRADLE_HOME/bin/gradle" -p "$APP_HOME" "$@"
