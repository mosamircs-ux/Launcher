# NovaPulse Launcher ProGuard / R8 rules

# Keep Room entities and DAOs
-keep class com.novapulse.launcher.models.** { *; }
-keep class com.novapulse.launcher.databases.** { *; }

# Keep Google Mobile Ads & UMP classes
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**

# Keep custom views inflated from XML
-keepclasseswithmembers class com.novapulse.launcher.views.** {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembers class com.novapulse.launcher.fragments.** {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
