package com.siyu.mdm.custom.device.util.screenoffalert

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import android.Manifest
import android.content.pm.PackageManager
import android.os.Vibrator
import com.siyu.mdm.custom.device.util.screenoffalert.AlertService

object PermissionHelper {
    private const val TAG = "PermissionHelper"

    // 权限类型枚举
    enum class PermissionType {
        NOTIFICATION,
        VIBRATION,
        BATTERY_OPTIMIZATION
    }

    // 检查所有必要的权限
    fun checkAllPermissions(context: Context): Boolean {
        // 检查通知权限
        val hasNotificationPermission = checkNotificationPermission(context)

        // 检查震动权限
        val hasVibrationPermission = checkVibrationPermission(context)

        // 检查电池优化权限
        val hasBatteryOptimizationPermission = checkBatteryOptimizationPermission(context)

        Log.d(TAG, "权限检查结果 - 通知: $hasNotificationPermission, 震动: $hasVibrationPermission, 电池优化: $hasBatteryOptimizationPermission")

        return hasNotificationPermission && hasVibrationPermission && hasBatteryOptimizationPermission
    }

    // 获取缺失的权限列表
    fun getMissingPermissions(context: Context): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (!checkNotificationPermission(context)) {
            missingPermissions.add("通知权限")
        }
        
        if (!checkVibrationPermission(context)) {
            missingPermissions.add("震动权限")
        }
        
        if (!checkBatteryOptimizationPermission(context)) {
            missingPermissions.add("电池优化豁免权限")
        }
        
        return missingPermissions
    }

    // 检查通知权限
    fun checkNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // 低于Android 13不需要显式通知权限
        }
    }

    // 检查震动权限
    fun checkVibrationPermission(context: Context): Boolean {
        val hasPermission = checkSelfPermission(
            context,
            Manifest.permission.VIBRATE
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            Log.w(TAG, "震动权限未授予")
        }
        return hasPermission
    }

    // 检查电池优化权限
    fun checkBatteryOptimizationPermission(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val hasPermission = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        
        if (!hasPermission) {
            Log.w(TAG, "电池优化权限未授予")
        }
        return hasPermission
    }

    // 请求特定类型的权限
    fun requestPermission(context: Context, permissionType: PermissionType) {
        try {
            when (permissionType) {
                PermissionType.NOTIFICATION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context is AlertActivity) {
                        context.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                    }
                }
                PermissionType.VIBRATION -> {
                    if (context is AlertActivity) {
                        context.requestPermissions(arrayOf(Manifest.permission.VIBRATE), 1002)
                    }
                }
                PermissionType.BATTERY_OPTIMIZATION -> {
                    // 根据设备制造商使用不同的意图
                    val manufacturer = Build.MANUFACTURER.lowercase()
                    val intent = if (manufacturer.contains("huawei")) {
                        // 华为设备特殊处理
                        Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:${context.packageName}")
                        }
                    } else {
                        // 通用处理
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    }
                    
                    // 确保意图可以被处理
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        // 如果无法直接请求，引导用户到设置页面
                        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        settingsIntent.data = Uri.fromParts("package", context.packageName, null)
                        context.startActivity(settingsIntent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求权限时出错: ${e.message}", e)
            showErrorDialog(context, "权限请求失败", "无法请求权限，请手动前往设置授予权限。")
        }
    }

    // 显示权限对话框
    fun showPermissionDialog(context: Context, permissionName: String, onGranted: (() -> Unit)? = null) {
        AlertDialog.Builder(context)
            .setTitle("需要权限")
            .setMessage("为了确保应用正常运行，需要您授予$permissionName\n\n" +
                    "• 通知权限：用于创建前台服务，确保后台稳定运行\n" +
                    "• 震动权限：用于在屏幕关闭时触发设备震动\n" +
                    "• 电池优化豁免：防止系统在屏幕关闭后限制应用功能")
            .setPositiveButton("立即授予") { _, _ ->
                when (permissionName) {
                    "通知权限" -> {
                        requestPermission(context, PermissionType.NOTIFICATION)
                    }
                    "震动权限" -> {
                        requestPermission(context, PermissionType.VIBRATION)
                    }
                    "电池优化豁免权限" -> {
                        requestPermission(context, PermissionType.BATTERY_OPTIMIZATION)
                    }
                }
                
                onGranted?.invoke()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 显示错误对话框
    fun showErrorDialog(context: Context, title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    // 显示完整的权限引导对话框
    fun showFullPermissionGuide(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        var guideMessage = """
为了确保应用正常运行，请按照以下步骤操作：

1. 打开设备设置
2. 找到并进入"应用管理"或"应用程序"
3. 找到并选择"熄屏提醒应用"
4. 进入"权限管理"
5. 授予所有必要权限（特别是通知和震动权限）
6. 进入"电池优化"设置
7. 将应用添加到"白名单"或"不受限制"列表
        """
        
        // 针对不同厂商的特殊引导
        if (manufacturer.contains("huawei")) {
            guideMessage += "\n\n华为设备特别提示：\n" +
                    "1. 请同时在'手机管家'中进行相同设置\n" +
                    "2. 进入'启动管理'，关闭应用的自动管理\n" +
                    "3. 开启'允许后台活动'选项\n" +
                    "4. 关闭'高耗电提醒'功能"
        } else if (manufacturer.contains("xiaomi")) {
            guideMessage += "\n\n小米设备特别提示：\n" +
                    "1. 请在'安全中心'中设置应用为'自启动'\n" +
                    "2. 进入'电量和性能'，关闭'神隐模式'\n" +
                    "3. 设置'后台运行权限'为'无限制'"
        } else if (manufacturer.contains("oppo")) {
            guideMessage += "\n\nOPPO设备特别提示：\n" +
                    "1. 请在'手机管家'中设置应用为'允许后台运行'\n" +
                    "2. 关闭'耗电保护'功能\n" +
                    "3. 设置'启动管理'为'允许自启动'"
        } else if (manufacturer.contains("vivo")) {
            guideMessage += "\n\nvivo设备特别提示：\n" +
                    "1. 请在'i管家'中设置应用为'允许后台高耗电'\n" +
                    "2. 开启'自启动'权限\n" +
                    "3. 关闭'智能省电'功能"
        }
        
        guideMessage += "\n\n如果按照上述步骤操作后仍有问题，请尝试重启设备或重新安装应用。"        
        
        AlertDialog.Builder(context)
            .setTitle("权限设置详细指南")
            .setMessage(guideMessage)
            .setPositiveButton("前往设置") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            }
            .setNegativeButton("稍后再说", null)
            .show()
    }

    // 检查服务是否可以启动
    fun canServiceStart(context: Context): Boolean {
        // 检查所有必要权限
        if (!checkAllPermissions(context)) {
            Log.e(TAG, "无法启动服务：缺少必要权限")
            return false
        }
        
        // 检查设备是否支持震动
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val hasVibrator = vibrator?.hasVibrator() ?: false
        
        if (!hasVibrator) {
            Log.e(TAG, "无法启动服务：设备不支持震动")
            showErrorDialog(context, "设备不支持", "您的设备不支持震动功能，无法使用此应用。")
            return false
        }
        
        // 检查前台服务是否可用（针对Android 14+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+前台服务增强限制检查
            try {
                val intent = Intent(context, AlertService::class.java)
                val componentName = ContextCompat.startForegroundService(context, intent)
                if (componentName == null) {
                    Log.e(TAG, "无法启动前台服务：系统限制")
                    return false
                }
                // 如果测试启动成功，立即停止
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "前台服务测试启动失败: ${e.message}")
                return false
            }
        }
        
        return true
    }

    // 尝试启动服务并处理可能的错误 - 现在已禁用手动启动
    fun tryStartService(context: Context): Boolean {
        try {
            // 提示用户只能通过MQTT命令启动震动提醒
            showErrorDialog(context, "功能限制", "震动提醒只能通过MQTT命令的方式打开")
            Log.d(TAG, "手动启动服务已禁用，只能通过MQTT命令启动")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "处理请求时发生异常：${e.message}", e)
            showErrorDialog(context, "操作失败", "无法处理请求，请重试")
            return false
        }
    }
    
    // 权限请求步骤管理类
    class PermissionRequestManager {
        var currentPermissionStep = 0
        
        // 处理下一步权限请求
        fun requestNextPermission(context: Context, notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>, 
                                 batteryOptimizationLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>) {
            when (currentPermissionStep) {
                0 -> {
                    // 第一步：请求通知权限（Android 13+）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // 低于Android 13跳过此步骤
                        currentPermissionStep++
                        this.requestNextPermission(context, notificationPermissionLauncher, batteryOptimizationLauncher)
                    }
                }
                1 -> {
                    // 第二步：请求电池优化白名单
                    val intent = if (Build.MANUFACTURER.lowercase().contains("huawei")) {
                        // 华为设备特殊处理
                        Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:${context.packageName}")
                        }
                    } else {
                        // 其他设备通用处理
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    }
                    batteryOptimizationLauncher.launch(intent)
                }
            }
        }
        
        // 重置权限请求步骤
        fun reset() {
            currentPermissionStep = 0
        }
    }
}

// 扩展函数，方便调用权限检查
fun checkAllPermissions(context: Context) = PermissionHelper.checkAllPermissions(context)
fun checkOtherPermissions(context: Context, callback: (Boolean) -> Unit) {
    val vibrationGranted = PermissionHelper.checkVibrationPermission(context)
    val batteryGranted = PermissionHelper.checkBatteryOptimizationPermission(context)
    
    if (!batteryGranted) {
        PermissionHelper.requestPermission(context, PermissionHelper.PermissionType.BATTERY_OPTIMIZATION)
    }
    
    callback(vibrationGranted && batteryGranted)
}

// Activity扩展函数：处理权限请求结果
fun AlertActivity.handlePermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
    var allGranted = true
    
    for (i in permissions.indices) {
        val permission = permissions[i]
        val isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED
        
        if (!isGranted) {
            allGranted = false
            val permissionName = when (permission) {
                Manifest.permission.POST_NOTIFICATIONS -> "通知权限"
                Manifest.permission.VIBRATE -> "震动权限"
                else -> "必要权限"
            }
            
            // 如果用户拒绝了权限，显示详细引导
            AlertDialog.Builder(this)
                .setTitle("权限被拒绝")
                .setMessage("$permissionName“对于应用正常运行至关重要。\n\n" +
                        "请点击'去设置'手动授予权限，否则应用可能无法正常工作。")
                .setPositiveButton("去设置") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
    
    return allGranted
}

// 扩展函数：检查并请求所有权限
fun AlertActivity.checkAndRequestAllPermissions(): Boolean {
    val missingPermissions = PermissionHelper.getMissingPermissions(this)
    
    if (missingPermissions.isEmpty()) {
        return true
    }
    
    // 显示详细的权限引导
    PermissionHelper.showFullPermissionGuide(this)
    return false
}
