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

-keep        class android.support.v13.** { *; }
-keep        class android.support.v7.** { *; }
-keep        class android.support.v4.** { *; }

-keep public class org.bottiger.podcast.** {
  public void set*(***);
  public *** get*();
  public boolean is*();
}

# otto
-keepattributes *Annotation*
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}

# OKhhtp
-dontwarn com.squareup.okhttp.**
# Ignore warnings: https://github.com/square/okio/issues/60
-dontwarn okio.**

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

#retrofit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions

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

# Keep all the ACRA classes
-keep class org.acra.** { *; }
-dontwarn org.acra.**


#Fresco
# Keep our interfaces so they can be used by other ProGuard rules.
# See http://sourceforge.net/p/proguard/bugs/466/
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip

# Do not strip any method/class that is annotated with @DoNotStrip
-keep @com.facebook.common.internal.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.common.internal.DoNotStrip *;
}
-dontwarn okio.**
-dontwarn javax.annotation.**

-keep class com.facebook.imagepipeline.gif.** { *; }
-keep class com.facebook.imagepipeline.webp.** { *; }