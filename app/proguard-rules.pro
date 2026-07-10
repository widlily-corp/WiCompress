# Add project specific Proguard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Widlily\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and targets by changing the BuildType properties in build.gradle.kts.

# Keep Room database annotations
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

# Keep JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}
