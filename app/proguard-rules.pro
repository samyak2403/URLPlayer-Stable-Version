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

# Media3/ExoPlayer specific rules
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }

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
-dontwarn javax.annotation.**

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

# ML Kit Barcode Scanning
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.mlkit.common.** { *; }
-dontwarn com.google.mlkit.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Custom Toast Library
-keep class com.samyak2403.custom_toast.** { *; }

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

# Keep JavaScript interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Vertical SeekBar
-keep class com.h6ah4i.android.widget.verticalseekbar.** { *; }

# DoubleTapPlayerView
-keep class com.github.vkay94.dtpv.** { *; }

# Keep R classes
-keep class **.R
-keep class **.R$* {
    <fields>;
}

# Additional rules to reduce APK size

# Enable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable
-optimizationpasses 8
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses 'com.samyak.urlplayer'
-flattenpackagehierarchy 'com.samyak.urlplayer'

# Remove unused code
-assumenosideeffects class java.lang.String {
    public String trim();
    public String substring(int);
    public String substring(int, int);
    public String toString();
    public static String valueOf(int);
    public static String valueOf(long);
    public static String valueOf(float);
    public static String valueOf(double);
    public static String valueOf(java.lang.Object);
    public static String valueOf(char[]);
    public static String valueOf(char[], int, int);
    public static String copyValueOf(char[]);
    public static String copyValueOf(char[], int, int);
}

# Remove more debugging info
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

# Remove System.out calls
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# More aggressive shrinking for AndroidX
-keep class androidx.core.app.** { *; }
-keep class androidx.fragment.app.** { *; }
-keep class androidx.appcompat.app.** { *; }
-keep class androidx.appcompat.widget.** { *; }
-keep class androidx.recyclerview.widget.** { *; }
-keep class androidx.viewpager2.widget.** { *; }
-keep class androidx.constraintlayout.widget.** { *; }
-keep class com.google.android.material.bottomnavigation.** { *; }
-keep class com.google.android.material.appbar.** { *; }
-keep class com.google.android.material.tabs.** { *; }
-keep class com.google.android.material.button.** { *; }
-keep class com.google.android.material.textfield.** { *; }
-dontwarn androidx.**

# More aggressive shrinking for Media3
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.extractor.** { *; }
-dontwarn androidx.media3.**

# More aggressive shrinking for Google Play Services
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }
-keep class com.google.android.gms.cast.framework.** { *; }
-dontwarn com.google.android.gms.**

# Reduce resources
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Shrink unused resources
-keep class **.R$raw { *; }
-keep class **.R$drawable { *; }
-keep class **.R$layout { *; }
-keep class **.R$string { *; }
-keep class **.R$color { *; }
-keep class **.R$dimen { *; }
-keep class **.R$id { *; }
-keep class **.R$style { *; }
-keep class **.R$styleable { *; }
-keep class **.R$menu { *; }

# Reduce native libraries
-keep class * extends com.google.android.exoplayer2.upstream.UdpDataSource { *; }
-keep class * extends com.google.android.exoplayer2.upstream.RtpDataSource { *; }
-keep class * extends com.google.android.exoplayer2.upstream.HttpDataSource { *; }

# Reduce Kotlin reflection
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.Metadata { *; }

# Reduce Coroutines
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepclassmembernames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keepclassmembernames class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# Reduce Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.**

# Reduce Firebase
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.firebase.database.** { *; }
-dontwarn com.google.firebase.**

# Reduce ML Kit
-keep class com.google.mlkit.vision.barcode.** { *; }
-dontwarn com.google.mlkit.**

# Reduce CameraX
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }
-dontwarn androidx.camera.**

# Reduce ZXing
-keep class com.google.zxing.BarcodeFormat { *; }
-keep class com.google.zxing.DecodeHintType { *; }
-keep class com.google.zxing.MultiFormatWriter { *; }
-keep class com.google.zxing.common.BitMatrix { *; }
-dontwarn com.google.zxing.**

# Keep only necessary Toast library components
-keep class com.samyak2403.custom_toast.TastyToast { *; }
-keep class com.samyak2403.custom_toast.TastyToast$Type { *; }