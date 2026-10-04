#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
./gradlew assembleDebug
printf '
APK: %s/app/build/outputs/apk/debug/app-debug.apk
' "$PWD"
