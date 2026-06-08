# TDLib JNI bindings — keep classes referenced from native code
-keep class org.drinkless.td.libcore.telegram.** { *; }
-keepclassmembers class org.drinkless.td.libcore.telegram.** { *; }

# Keep our TdApi result data classes used via reflection by the wrapper
-keep class com.amn3zia.app.core.tdlib.** { *; }
