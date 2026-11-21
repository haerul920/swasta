# Aturan R8 untuk rilis Swasana.

# kotlinx.serialization: pertahankan serializer yang dibuat compiler plugin.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class id.swasana.**$$serializer { *; }
-keepclassmembers class id.swasana.** { *** Companion; }
-keepclasseswithmembers class id.swasana.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit: pertahankan generik dan anotasi antarmuka API.
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface id.swasana.data.remote.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Room dan Hilt menghasilkan kodenya sendiri; entitas dipertahankan lewat anotasi.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Stack trace yang bisa dibaca di laporan crash.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
