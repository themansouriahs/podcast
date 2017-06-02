# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\apl\AppData\Local\Android\android-studio\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-dontoptimize

-keep class org.bottiger.podcast.** {*;}
-keep class com.dragontek.mygpoclient.** {*;}

-keep class com.google.** { *; }
-dontwarn com.google.**

-keep        class android.support.v13.** { *; }
-keep        class android.support.v7.** { *; }
-keep        class android.support.v4.** { *; }

-keep public class org.bottiger.podcast.** {
  public void set*(***);
  public *** get*();
  public boolean is*();
}

## http://stackoverflow.com/questions/21342700/proguard-causing-runtimeexception-unmarshalling-unknown-type-code-in-parcelabl
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

##
## Remove verbose, non critical logs
## http://stackoverflow.com/questions/13218772/removing-log-call-using-proguard
##
-assumenosideeffects class android.util.Log {
public static *** d(...);
public static *** v(...);
public static *** i(...);
public static *** w(...);
#public static *** e(...);
#public static *** wtf(...);
    }

# otto
-keepattributes *Annotation*
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}

#glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

# okhttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
# Ignore warnings: https://github.com/square/okio/issues/60
-dontwarn okio.**

#retrofit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

#amazon
-dontwarn com.amazon.**
-dontwarn com.amazonaws.**
-keep class com.amazon.** {*;}
-keep class com.amazonaws.** {*;}
-keepattributes *Annotation*

# Jackson xml parser
-keepattributes Signature
-keepattributes *Annotation*,EnclosingMethod
-keepnames class com.fasterxml.jackson.** {
*;
}
-keepnames interface com.fasterxml.jackson.** {
    *;
}
-keepclassmembers public final enum org.codehaus.jackson.annotate.JsonAutoDetect$Visibility {
    public static final org.codehaus.jackson.annotate.JsonAutoDetect$Visibility *;
}
-dontwarn javax.xml.**
-dontwarn javax.xml.stream.events.**
-dontwarn com.fasterxml.jackson.databind.**

# apache
-dontwarn org.apache.commons.**
-dontwarn org.apache.http.**

# com.revy.material
-dontwarn com.rey.material.**

# webview
-dontwarn android.webkit.**

#Jsoup
-keep public class org.jsoup.** {
public *;
}

#ACRA specifics
# Restore some Source file names and restore approximate line numbers in the stack traces,
# otherwise the stack traces are pretty useless
-keepattributes SourceFile,LineNumberTable

# ACRA needs "annotations" so add this...
# Note: This may already be defined in the default "proguard-android-optimize.txt"
# file in the SDK. If it is, then you don't need to duplicate it. See your
# "project.properties" file to get the path to the default "proguard-android-optimize.txt".
-keepattributes *Annotation*

# RxJava/RxAndroid
#-keep class sun.misc.Unsafe.** { *; }
#-keep class rx.internal.util.unsafe.** { *; }
-dontwarn sun.misc.**

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
   long producerNode;
   long consumerNode;
}