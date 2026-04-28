# Add project specific ProGuard rules here.

# ==================== AndroidX ====================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# ==================== Kotlin ====================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ==================== MQTT ====================
-keep class org.eclipse.paho.** { *; }
-dontwarn org.eclipse.paho.**

# ==================== 华为MDM SDK ====================
-keep class com.huawei.mdm.** { *; }
-keep interface com.huawei.mdm.** { *; }
-dontwarn com.huawei.mdm.**

# ==================== UtilCode ====================
-keep class com.blankj.utilcode.** { *; }
-dontwarn com.blankj.utilcode.**

# ==================== JSON ====================
-keep class org.json.** { *; }
-dontwarn org.json.**

# ==================== 应用业务 ====================
-keep class com.siyu.mdm.enterprise.** { *; }
-dontwarn com.siyu.mdm.enterprise.**

# ==================== Compose ====================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ==================== 反射 ====================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# ==================== 银行等保相关 ====================
# 敏感信息不输出日志
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
