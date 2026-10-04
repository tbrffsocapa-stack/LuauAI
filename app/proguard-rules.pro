# Luau AI ProGuard Rules
-keep class com.luauai.** { *; }
-keep class com.luauai.ai.engine.LlamaEngine { *; }
-keep class com.luauai.ai.engine.LlamaEngine$TokenCallback { *; }
-dontwarn org.jsoup.**
-keep class org.jsoup.** { *; }
