# ==============================================================================
# SpeedCheck ProGuard / R8 Rules for Release Code Optimization & Obfuscation
# ==============================================================================

# Preserve source file names and line numbers in stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable class *;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    public static final *** Companion;
}
-keepclassmembers class * implements androidx.navigation3.runtime.NavKey {
    *;
}

# Android DataStore Preferences & Coroutines
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory {
    java.lang.String FAST_SERVICE_KEY;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Google Play Services Location
-keep class com.google.android.gms.location.** { *; }
-keep interface com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# Android TextToSpeech
-keep class android.speech.tts.** { *; }

# App Data Models
-keep class com.speedcheck.android.model.** { *; }
-keep class com.speedcheck.android.data.** { *; }
