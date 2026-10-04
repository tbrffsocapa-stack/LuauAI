#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
echo "[1/4] Instalando ferramentas básicas do Termux..."
pkg update -y
pkg install -y openjdk-21 git curl unzip cmake ninja
export JAVA_HOME="${JAVA_HOME:-$PREFIX/lib/jvm/java-21-openjdk}"
export PATH="$JAVA_HOME/bin:$PATH"
SDK="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_HOME="$SDK"
mkdir -p "$SDK"
SDKMANAGER="$(command -v sdkmanager || true)"
if [ -z "$SDKMANAGER" ]; then
  for candidate in "$SDK/cmdline-tools/latest/bin/sdkmanager" "$SDK/tools/bin/sdkmanager" "$HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager"; do
    if [ -x "$candidate" ]; then SDKMANAGER="$candidate"; break; fi
  done
fi
if [ -z "$SDKMANAGER" ]; then
  echo "ERRO: sdkmanager não encontrado. Instale o Android command-line tools e configure ANDROID_HOME."
  echo "ANDROID_HOME atual: $ANDROID_HOME"
  exit 2
fi
echo "[2/4] Instalando plataforma, NDK e CMake necessários..."
"$SDKMANAGER" --sdk_root="$SDK" "platform-tools" "platforms;android-35" "build-tools;35.0.0" "ndk;26.1.10909125" "cmake;3.22.1"
echo "[3/4] Obtendo llama.cpp compatível com a API usada pelo JNI..."
LLAMA="app/src/main/cpp/llama.cpp"
if [ ! -f "$LLAMA/llama.cpp" ]; then
  rm -rf "$LLAMA"
  git clone --depth 1 --branch b1600 https://github.com/ggerganov/llama.cpp.git "$LLAMA"
fi
echo "[4/4] Compilando APK debug..."
./gradlew assembleDebug
printf '
APK gerado em: %s/app/build/outputs/apk/debug/app-debug.apk
' "$PWD"
