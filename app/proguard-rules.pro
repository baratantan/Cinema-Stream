# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Aggressive Renaming and Obfuscation
-repackageclasses ''
-allowaccessmodification

# Remove debugging attributes to prevent string and structure analysis
-dontkeepattributes SourceFile,LineNumberTable

# Preserve Annotations for Room, Retrofit, and Moshi
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Retrofit Rules ---
-keepattributes RuntimeVisibleDeclarations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# --- Moshi Rules ---
# Keep generated JsonAdapters
-keep class *JsonAdapter { *; }
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Keep the annotated Moshi data models to allow Moshi to find their constructors and adapters
-keep @com.squareup.moshi.JsonClass class * { *; }

# --- Kotlin Coroutines / Flow Rules ---
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}

# --- OkHttp Rules (Retrofit dependency) ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# --- Room Rules ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
