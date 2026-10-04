# LuauAI — projeto organizado para Termux

## Preparação (primeira vez)
Na raiz do projeto, execute:

```bash
bash scripts/setup_termux.sh
```

O script instala ferramentas do Termux, solicita os pacotes do Android SDK (API 35, NDK 26.1 e CMake 3.22.1), baixa a revisão `b1600` de llama.cpp e tenta compilar o APK.

## Compilar novamente

```bash
./gradlew assembleDebug
```

APK esperado: `app/build/outputs/apk/debug/app-debug.apk`

## Observações
- `build.gradle.kts` na raiz contém os plugins com `apply false`.
- `app/build.gradle.kts` contém a configuração real do app Android.
- `HomeScreen(1).kt` é idêntico a `HomeScreen.kt`; foi guardado em `_review/` para não provocar declarações duplicadas.
- O arquivo acidental `arquivo.lua` foi removido.
- A preparação precisa de conexão à internet para baixar Gradle/SDK/llama.cpp.
