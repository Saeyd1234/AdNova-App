# AdNova ProGuard Rules
-keep class com.adnova.earning.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
