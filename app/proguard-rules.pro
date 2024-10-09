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
-keep class io.agora.**{*;}
-keepclassmembers class ai.deepar.ar.DeepAR { *; }
-keepclassmembers class ai.deepar.ar.core.videotexture.VideoTextureAndroidJava { *; }
-keep class ai.deepar.ar.core.videotexture.VideoTextureAndroidJava
-keep class com.meetfriend.app.api.** { *; }
-keep class com.meetfriend.app.responseclasses.** { *; }
-keep class cn.jzvd.**{*;}
#-keep public class cn.jzvd.Jzvd {*; }
#-keep public class cn.jzvd.JZDataSource {*; }
#-keep public class cn.jzvd.JzvdStd {*; }
#-keep public class cn.jzvd.JZUtils {*; }
#-keep public class cn.jzvd.JZTextureView {*; }
-keep class com.meetfriend.app.videoplayer.** {*; }
#-keep public class com.meetfriend.app.videoplayer.JzvdStdOutgoer {*; }
#-keep public class com.meetfriend.app.videoplayer.JzvdStd {*; }
#-keep public class com.meetfriend.app.videoplayer.JZMediaMp4ExoKotlin {*; }
#-keep public class com.meetfriend.app.videoplayer.ViewPagerLayoutManager {*; }
#-keep public class com.meetfriend.app.videoplayer.JZMediaExoKotlin {*; }
-keep class com.meetfriend.app.cache.ExoCacheManager
-dontwarn java.nio.file.Files
-dontwarn java.nio.file.Path
-dontwarn java.nio.file.OpenOption
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keep class tv.danmaku.ijk.media.player.** { *; }
-dontwarn tv.danmaku.ijk.media.player.*
-keep interface tv.danmaku.ijk.media.player.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-keep class androidx.mediarouter.app.MediaRouteActionProvider {
  *;
}

# support design library
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }


-keep class com.facebook.infer.annotation.** { *; }
-dontwarn com.facebook.infer.annotation.**


#     Glide
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
-keep class com.facebook.** {
   *;
}

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.splunk.** { *; }

-keep class com.arthenica.mobileffmpeg.Config {
    native <methods>;
    void log(long, int, byte[]);
    void statistics(long, int, float, float, long , int, double, double);
}

-keep class com.arthenica.mobileffmpeg.AbiDetect {
    native <methods>;
}
-keepclassmembers enum com.meetfriend.app.api.** { *; }
-keepclassmembers enum com.meetfriend.app.responseclasses.** { *; }

-keep class com.google.android.play.core.** { *; }

-keepclassmembers,allowobfuscation class * {
 @com.google.gson.annotations.SerializedName <fields>;
}