buildscript {
    dependencies {
        // AGP 9.0 has built-in KGP 2.2.10; override to 2.4.10 via classpath
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.googleDevtoolsKsp.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    // kotlin.android removed — AGP 9.0 has built-in Kotlin
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.secrets) apply false
    // google-devtools-ksp removed from plugins — applied via buildscript classpath
}
