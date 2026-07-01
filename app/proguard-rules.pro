# ============================================================
# BioAcupunt — ProGuard / R8 rules
# Security: enables code shrinking & obfuscation in release builds
# while preserving runtime reflection needed by Room/Retrofit/Moshi.
# ============================================================

# ── Kotlin metadata ────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlin.**

# ── Kotlinx Serialization ──────────────────────────────────
-keepattributes RuntimeVisibleAnnotations
-keep,includedescriptorclasses class com.bioacupunt.**$$serializer { *; }
-keepclassmembers class com.bioacupunt.** {
    *** Companion;
}
-keepclasseswithmembers class com.bioacupunt.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Room ────────────────────────────────────────────────────
-keep class com.bioacupunt.**.data.local.** { *; }
-keep class com.bioacupunt.**.SyncQueueEntity { *; }
-dontwarn androidx.room.paging.**

# ── Retrofit / OkHttp / Moshi ───────────────────────────────
-keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault
-keep class com.bioacupunt.data.remote.** { *; }
-keep interface com.bioacupunt.data.remote.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# ── WorkManager ──────────────────────────────────────────────
-keep class com.bioacupunt.sync.SyncWorker { *; }
-keep class com.bioacupunt.sync.SyncWorkerFactory { *; }
-keep class androidx.work.impl.WorkManagerInitializer

# ── Domain & data models (serialized / reflected) ────────────
-keep class com.bioacupunt.**.domain.model.** { *; }
-keep class com.bioacupunt.auth.domain.model.** { *; }
-keep class com.bioacupunt.agenda.domain.model.** { *; }
-keep class com.bioacupunt.crm.domain.model.** { *; }
-keep class com.bioacupunt.biblioteca.domain.model.** { *; }

# ── Jetpack Compose ───────────────────────────────────────────
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ── Security: do not warn about EncryptedSharedPreferences internals ──
-dontwarn androidx.security.crypto.**
-keep class androidx.security.crypto.** { *; }

# ── Remove logging calls in release for performance & no leaks ────
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
