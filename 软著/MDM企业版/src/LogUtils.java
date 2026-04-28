package com.siyu.mdm.enterprise.util;

import android.util.Log;

import com.siyu.mdm.enterprise.BuildConfig;

/**
 * 日志工具类（优化版）
 * 自动记录调用者的文件名、行号、方法名。
 * 输出格式: [FileName:Line] ClassName.methodName -> 消息内容
 */
public class LogUtils {

    private static volatile String TAG = "MDM-Enterprise";
    private static volatile boolean isLogSwitch = BuildConfig.DEBUG;
    private static volatile int stackOffset = 0;

    // 需要跳过的包名前缀（系统/框架类）
    private static final String[] SKIP_PACKAGES = {
            "android.", "java.", "javax.", "dalvik.", "com.android.", "libcore.",
            "kotlin.jvm.internal.", "kotlin.io.", "kotlin.collections.",
            "com.intellij.", "org.jetbrains.", "io.flutter.",
            "VMStack", "Runtime", "Zygote", "ActivityThread", "MethodAndArgsCaller"
    };

    private LogUtils() {}

    // ==================== 基础日志方法 ====================
    public static void d(String tag, String msg) {
        if (isLogSwitch) Log.d(tag, getLogInfo() + msg);
    }
    public static void i(String tag, String msg) {
        if (isLogSwitch) Log.i(tag, getLogInfo() + msg);
    }
    public static void w(String tag, String msg) {
        if (isLogSwitch) Log.w(tag, getLogInfo() + msg);
    }
    public static void e(String tag, String msg) {
        if (isLogSwitch) Log.e(tag, getLogInfo() + msg);
    }
    public static void e(String tag, String msg, Throwable tr) {
        if (isLogSwitch) Log.e(tag, getLogInfo() + msg, tr);
    }

    // ==================== 便捷方法 ====================
    public static void d(String msg) { d(TAG, msg); }
    public static void i(String msg) { i(TAG, msg); }
    public static void w(String msg) { w(TAG, msg); }
    public static void e(String msg) { e(TAG, msg); }
    public static void e(String msg, Throwable tr) { e(TAG, msg, tr); }

    // ==================== 参数化日志 ====================
    public static void d(String tag, String format, Object... args) {
        if (isLogSwitch) Log.d(tag, getLogInfo() + formatMsg(format, args));
    }
    public static void i(String tag, String format, Object... args) {
        if (isLogSwitch) Log.i(tag, getLogInfo() + formatMsg(format, args));
    }
    public static void w(String tag, String format, Object... args) {
        if (isLogSwitch) Log.w(tag, getLogInfo() + formatMsg(format, args));
    }
    public static void e(String tag, String format, Object... args) {
        if (isLogSwitch) Log.e(tag, getLogInfo() + formatMsg(format, args));
    }
    public static void d(String format, Object... args) { d(TAG, format, args); }
    public static void i(String format, Object... args) { i(TAG, format, args); }
    public static void w(String format, Object... args) { w(TAG, format, args); }
    public static void e(String format, Object... args) { e(TAG, format, args); }

    private static String formatMsg(String format, Object... args) {
        try {
            return (args == null || args.length == 0) ? format : String.format(format, args);
        } catch (Exception e) {
            return format + " [FORMAT_ERROR]";
        }
    }

    // ==================== 核心：动态获取调用位置（跳过系统类） ====================
    private static String getLogInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        int targetIndex = -1;
        for (int i = 0; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();

            // 跳过 LogUtils 自身
            if (className.equals(LogUtils.class.getName())) {
                continue;
            }

            // 跳过系统/框架类
            boolean isSkip = false;
            for (String prefix : SKIP_PACKAGES) {
                if (className.startsWith(prefix) || className.contains(prefix)) {
                    isSkip = true;
                    break;
                }
            }
            if (isSkip) {
                continue;
            }

            // 找到第一个非 LogUtils、非系统类的元素，即为业务调用者
            targetIndex = i;
            break;
        }

        if (targetIndex == -1 || targetIndex + stackOffset >= stackTrace.length) {
            return "";
        }

        StackTraceElement element = stackTrace[targetIndex + stackOffset];
        String fileName = element.getFileName();
        String className = element.getClassName();
        String methodName = element.getMethodName();
        int lineNumber = element.getLineNumber();

        // 简化类名
        int lastDot = className.lastIndexOf('.');
        String simpleClassName = lastDot > 0 ? className.substring(lastDot + 1) : className;

        // 防止 fileName 为 null（某些运行时返回 null）
        if (fileName == null) fileName = "UnknownFile";

        return "[" + fileName + ":" + lineNumber + "] " + simpleClassName + "." + methodName + " -> ";
    }

    // ==================== 配置方法 ====================
    public static void setTag(String tag) { TAG = tag; }
    public static void setLogSwitch(boolean switchOn) { isLogSwitch = switchOn; }
    public static boolean isLogSwitch() { return isLogSwitch; }
    public static boolean isDebugEnabled() { return isLogSwitch; }
    public static boolean isInfoEnabled() { return isLogSwitch; }
    public static boolean isWarnEnabled() { return isLogSwitch; }
    public static boolean isErrorEnabled() { return isLogSwitch; }

    public static void setStackOffset(int offset) {
        if (offset >= 0 && offset <= 5) stackOffset = offset;
    }

    public static void printStackTrace() {
        if (!isLogSwitch) return;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        Log.i(TAG, "========== StackTrace Debug ==========");
        for (int i = 0; i < Math.min(stackTrace.length, 15); i++) {
            StackTraceElement e = stackTrace[i];
            Log.i(TAG, String.format("[%d] %s.%s (%s:%d)",
                    i, getSimpleClassName(e.getClassName()), e.getMethodName(),
                    e.getFileName(), e.getLineNumber()));
        }
        Log.i(TAG, "======================================");
    }

    private static String getSimpleClassName(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }
}