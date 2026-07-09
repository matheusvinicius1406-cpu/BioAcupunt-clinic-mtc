# Minimal ProGuard rules for BioAcupunt clinic.
# Keep entry points and AndroidX/WorkManager/Room-safe defaults.
-keep class com.bioacupunt.BioAcupuntApp { *; }
-keep class com.bioacupunt.MainActivity { *; }
-keep class com.bioacupunt.di.AppContainer { *; }
-keep class com.bioacupunt.sync.** { *; }

-dontwarn androidx.**
-dontwarn com.google.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.squareup.**
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlin.Metadata

# Keep Room-generated code
-keep class androidx.room.** { *; }
-keep @androidx.room.* class * { *; }

# Keep WorkManager workers
-keep class androidx.work.** { *; }

# Keep Compose/Material3 generated classes
-keep class androidx.compose.** { *; }
-keep class com.bioacupunt.ui.** { *; }

# Serialization / Moshi
-keep class kotlinx.serialization.** { *; }
-keep class com.bioacupunt.ai.** { *; }
-keepattributes *Annotation*,EnclosingMethod,Signature,InnerClasses
