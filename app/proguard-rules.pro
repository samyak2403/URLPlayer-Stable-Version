# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep generic signatures, annotations and debugging info
-keepattributes Signature,*Annotation*,SourceFile,LineNumberTable,Exceptions,InnerClasses

# Application-specific classes
-keep class com.samyak.urlplayer.** { *; }
-keep class com.samyak.urlplayer.models.** { *; }
-keep class com.samyak.urlplayer.screen.** { *; }
-keep class com.samyak.urlplayer.adapters.** { *; }
-keep class com.samyak.urlplayer.utils.** { *; }
-keep class com.samyak.urlplayer.databinding.** { *; }
-keep public class com.samyak.urlplayer.MyApplication

# ExoPlayer specific rules
-dontwarn com.google.android.exoplayer2.**
-keep class com.google.android.exoplayer2.** { *; }
-keep interface com.google.android.exoplayer2.** { *; }
-keepclassmembers class com.google.android.exoplayer2.** { *; }

# Cast SDK rules
-keep class com.google.android.gms.cast.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.cast.**
-keepclassmembers class com.google.android.gms.cast.framework.** { *; }

# MediaRouter rules
-keep class androidx.mediarouter.** { *; }
-keep class android.support.v7.mediarouter.** { *; }

# AdMob rules
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep public class com.google.android.gms.ads.MobileAds { public *; }

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson rules
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(...);
}

# Shimmer rules
-keep class com.facebook.shimmer.** { *; }

# Kotlin rules
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# AndroidX rules
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**
-dontwarn android.support.**

# Material Design rules
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# RecyclerView
-keep public class * extends androidx.recyclerview.widget.RecyclerView$LayoutManager {
    public <init>(...);
}

# Standard Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep onClick handlers
-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}

# Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepnames class * implements java.io.Serializable

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Media rules
-keep class android.media.** { *; }
-keep class android.media.audiofx.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove debugging info
-renamesourcefileattribute SourceFile