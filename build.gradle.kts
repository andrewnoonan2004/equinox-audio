// AGP 9 compiles Kotlin natively ("built-in Kotlin"), so the standalone
// org.jetbrains.kotlin.android plugin is intentionally not applied.
plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
}
