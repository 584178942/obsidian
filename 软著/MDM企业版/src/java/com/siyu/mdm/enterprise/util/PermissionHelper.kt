package com.siyu.mdm.enterprise.util

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限请求帮助类
 * 用于管理运行时权限请求和特殊权限引导
 */
object PermissionHelper {

    private const val TAG = "PermissionHelper"

    // 运行时需要请求的权限列表
    object RuntimePermissions {
        val BLUETOOTH_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        val NFC_PERMISSION = arrayOf(Manifest.permission.NFC)
    }

    /**
     * 检查是否具有WRITE_SECURE_SETTINGS权限
     * 这是控制NFC、移动数据等需要的关键权限
     */
    fun hasWriteSecureSettingsPermission(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * 请求WRITE_SECURE_SETTINGS权限
     * 需要用户手动授权，无法通过代码自动请求
     *
     * @param activity 用于打开系统设置界面
     * @param onResult 授权结果回调
     */
    fun requestWriteSecureSettingsPermission(
        activity: Activity,
        onResult: (granted: Boolean) -> Unit
    ) {
        if (hasWriteSecureSettingsPermission(activity)) {
            LogUtils.d(TAG, "WRITE_SECURE_SETTINGS权限已授予")
            onResult(true)
            return
        }

        LogUtils.w(TAG, "请求WRITE_SECURE_SETTINGS权限，需要用户手动授权")
        
        try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            
            // 注意：无法直接获取结果，用户需要在设置中授权
            // 应用应该监听Activity lifecycle或使用Settings.Secure观察者模式来检测权限变化
            onResult(false)
        } catch (e: Exception) {
            LogUtils.e(TAG, "无法打开WRITE_SETTINGS设置页面", e)
            onResult(false)
        }
    }

    /**
     * 检查运行时权限是否已授予
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查一组权限是否都已授予
     */
    fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    /**
     * 请求运行时权限
     *
     * @param activity Activity实例
     * @param permissions 要请求的权限数组
     * @param requestCode 请求码，用于在onRequestPermissionsResult中识别
     * @return 是否成功发起请求（如果权限已全部授予则返回false）
     */
    fun requestRuntimePermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int
    ): Boolean {
        if (hasAllPermissions(activity, permissions)) {
            LogUtils.d(TAG, "所有权限已授予")
            return false
        }

        val permissionsToRequest = permissions.filter { 
            !hasPermission(activity, it) 
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            LogUtils.d(TAG, "请求权限: ${permissionsToRequest.joinToString()}")
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest,
                requestCode
            )
            return true
        }

        return false
    }

    /**
     * 检查是否应该显示权限请求 rationale
     * 用于向用户解释为什么需要这个权限
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * 打开应用设置页面
     * 用于用户在拒绝权限后手动授权
     */
    fun openAppSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${activity.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            LogUtils.i(TAG, "已打开应用设置页面")
        } catch (e: Exception) {
            LogUtils.e(TAG, "无法打开应用设置页面", e)
        }
    }

    /**
     * 打开蓝牙设置页面
     */
    fun openBluetoothSettings(activity: Activity) {
        try {
            val intent = Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    action = Settings.ACTION_BLUETOOTH_SETTINGS
                } else {
                    action = Settings.ACTION_BLUETOOTH_SETTINGS
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            LogUtils.i(TAG, "已打开蓝牙设置页面")
        } catch (e: Exception) {
            LogUtils.e(TAG, "无法打开蓝牙设置页面", e)
        }
    }

    /**
     * 打开NFC设置页面
     */
    fun openNFCSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_NFC_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            LogUtils.i(TAG, "已打开NFC设置页面")
        } catch (e: Exception) {
            LogUtils.e(TAG, "无法打开NFC设置页面", e)
        }
    }

    /**
     * 打开WiFi设置页面
     */
    fun openWifiSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            LogUtils.i(TAG, "已打开WiFi设置页面")
        } catch (e: Exception) {
            LogUtils.e(TAG, "无法打开WiFi设置页面", e)
        }
    }

    /**
     * 检查设备是否支持蓝牙
     */
    fun isBluetoothSupported(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    /**
     * 检查设备是否支持NFC
     */
    fun isNFCSupported(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    }

    /**
     * 检查设备是否支持WiFi
     */
    fun isWifiSupported(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)
    }

    /**
     * 检查NFC是否已启用（只读操作）
     * 不需要特殊权限
     */
    fun isNFCEnabled(context: Context): Boolean {
        if (!isNFCSupported(context)) {
            return false
        }
        
        return try {
            val nfcAdapter = android.nfc.NfcAdapter.getDefaultAdapter(context)
            nfcAdapter?.isEnabled ?: false
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查NFC状态失败", e)
            false
        }
    }

    /**
     * 获取NFC的详细状态信息
     * 返回一个包含所有相关信息的Map
     */
    fun getNFCStatusInfo(context: Context): Map<String, Any> {
        return mapOf(
            "supported" to isNFCSupported(context),
            "permission" to hasPermission(context, Manifest.permission.NFC),
            "writeSecureSettings" to hasWriteSecureSettingsPermission(context),
            "enabled" to isNFCEnabled(context),
            "canControl" to (hasWriteSecureSettingsPermission(context) && isNFCSupported(context))
        )
    }

    /**
     * 检查是否可以通过代码控制NFC
     * 实际上，除了系统应用外，普通应用都无法真正控制NFC
     */
    fun canControlNFC(context: Context): Boolean {
        return isNFCSupported(context) && hasWriteSecureSettingsPermission(context)
    }

    /**
     * 引导用户到合适的设置页面
     * 根据设备支持情况选择最佳页面
     */
    fun guideToNFCSettings(activity: Activity) {
        if (isNFCSupported(activity)) {
            openNFCSettings(activity)
        } else {
            // 设备不支持NFC，提示用户
            LogUtils.w(TAG, "设备不支持NFC功能")
        }
    }

    /**
     * 检查蓝牙是否已启用（只读操作）
     */
    fun isBluetoothEnabled(context: Context): Boolean {
        if (!isBluetoothSupported(context)) {
            return false
        }
        
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            bluetoothManager?.adapter?.isEnabled ?: false
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查蓝牙状态失败", e)
            false
        }
    }

    /**
     * 检查是否可以通过代码控制蓝牙
     */
    fun canControlBluetooth(context: Context): Boolean {
        if (!isBluetoothSupported(context)) {
            return false
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasAllPermissions(context, RuntimePermissions.BLUETOOTH_PERMISSIONS)
        } else {
            hasPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
        }
    }
}
