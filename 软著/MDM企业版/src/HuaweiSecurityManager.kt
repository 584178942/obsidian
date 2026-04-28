package com.siyu.mdm.enterprise.util.mdm.huawei

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import com.siyu.mdm.enterprise.App
import com.siyu.mdm.enterprise.SampleDeviceReceiver
import com.siyu.mdm.enterprise.ui.FullScreenActivity
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.SecurityManager
import java.util.Locale

class HuaweiSecurityManager : SecurityManager {

    private val TAG = "HuaweiSecurityManager"

    private val context: Context = App.instance

    private val adminComponentName by lazy {
        ComponentName(context, SampleDeviceReceiver::class.java)
    }

    private val devicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val deviceControlManager by lazy {
        HuaweiMDMAbility.getDeviceControlManager()
    }

    override fun checkAdminPermission(): Boolean {
        return HuaweiMDMAbility.isDeviceAdmin()
    }

    override fun lock(mainText: String?, subText: String?) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "❌ 应用未激活设备管理员，无法锁定设备")
                LogUtils.e(TAG, "请在系统设置中激活设备管理员权限")
                throw SecurityException("应用未激活设备管理员")
            }

            try {
                lockWithLockTaskMode(mainText, subText)
            } catch (e: SecurityException) {
                LogUtils.e(TAG, "锁定任务模式权限不足，尝试使用原生锁定: ${e.message}", e)
                lockWithNativeAPI()
            } catch (e: ActivityNotFoundException) {
                LogUtils.e(TAG, "锁定Activity未找到: ${e.message}", e)
                lockWithNativeAPI()
            } catch (e: Exception) {
                LogUtils.e(TAG, "锁定设备异常: ${e.message}", e)
                lockWithNativeAPI()
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "❌ 锁定设备失败: ${e.message}", e)
            throw e
        }
    }

    private fun lockWithLockTaskMode(mainText: String?, subText: String?) {
        try {
            val instance = App.instance
            
            devicePolicyManager.let { dpm ->
                if (!dpm.isDeviceOwnerApp(instance.packageName)) {
                    LogUtils.w(TAG, "当前应用不是Device Owner，无法使用锁定任务模式")
                    lockWithNativeAPI()
                    return
                }

                dpm.setLockTaskPackages(
                    adminComponentName,
                    arrayOf(instance.packageName)
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dpm.setLockTaskFeatures(
                        adminComponentName,
                        DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO
                    )
                }
            }

            val intent = Intent(instance, FullScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                mainText?.let {
                    putExtra(FullScreenActivity.EXTRA_MAIN_TEXT, it)
                }
                
                subText?.let {
                    putExtra(FullScreenActivity.EXTRA_SUB_TEXT, it)
                }
            }

            val activityManager = instance.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (!activityManager.isInLockTaskMode) {
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }

            val options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                options.setLockTaskEnabled(true)
            }

            instance.startActivity(intent, options.toBundle())
            LogUtils.i(TAG, "设备锁定成功（锁定任务模式）")
            
        } catch (e: SecurityException) {
            LogUtils.e(TAG, "锁定任务模式SecurityException: ${e.message}", e)
            throw e
        } catch (e: ActivityNotFoundException) {
            LogUtils.e(TAG, "锁定Activity未找到: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            LogUtils.e(TAG, "锁定任务模式异常: ${e.message}", e)
            throw e
        }
    }

    private fun lockWithNativeAPI() {
        try {
            devicePolicyManager.lockNow()
            LogUtils.i(TAG, "设备锁定成功（原生API）")
        } catch (e: Exception) {
            LogUtils.e(TAG, "原生API锁定设备失败: ${e.message}", e)
        }
    }

    override fun unlock() {
        LogUtils.i(TAG, "设备解锁（非锁定模式，无需解锁）")
    }

    override fun wipe() {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法恢复出厂设置")
                return
            }

            try {
                devicePolicyManager.wipeData(0)
                LogUtils.i(TAG, "恢复出厂设置指令已发送")
            } catch (e: Exception) {
                LogUtils.e(TAG, "恢复出厂设置失败: ${e.message}", e)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "恢复出厂设置异常: ${e.message}", e)
        }
    }

    override fun resetPassword(newPassword: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法重置密码")
                return
            }

            try {
                devicePolicyManager.resetPassword(newPassword, 0)
                LogUtils.i(TAG, "密码重置成功")
            } catch (e: Exception) {
                LogUtils.e(TAG, "重置密码失败: ${e.message}", e)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "重置密码异常: ${e.message}", e)
        }
    }

    override fun isSupported(): Boolean {
        return try {
            checkAdminPermission()
        } catch (e: Exception) {
            false
        }
    }

    fun shutdown() {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法关机")
                return
            }

            try {
                deviceControlManager?.let {
                    it.shutdownDevice(adminComponentName)
                    LogUtils.i(TAG, "关机指令已发送")
                } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法关机")
            } catch (e: Exception) {
                LogUtils.e(TAG, "关机失败: ${e.message}", e)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "关机异常: ${e.message}", e)
        }
    }

    fun reboot() {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法重启")
                return
            }

            try {
                deviceControlManager?.let {
                    it.rebootDevice(adminComponentName)
                    LogUtils.i(TAG, "重启指令已发送")
                } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法重启")
            } catch (e: Exception) {
                LogUtils.e(TAG, "重启失败: ${e.message}", e)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "重启异常: ${e.message}", e)
        }
    }

    fun isRooted(): Boolean {
        return try {
            deviceControlManager?.let {
                it.isRooted(adminComponentName)
            } ?: false
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取root状态失败: ${e.message}", e)
            false
        }
    }

    fun captureScreen(): Bitmap? {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法截屏")
                return null
            }

            deviceControlManager?.let {
                val bitmap = it.captureScreen(adminComponentName)
                LogUtils.i(TAG, "截屏成功")
                bitmap
            } ?: run {
                LogUtils.w(TAG, "DeviceControlManager不可用，无法截屏")
                null
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "截屏失败: ${e.message}", e)
            null
        }
    }

    fun setSysTime(millis: Long) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置系统时间")
                return
            }

            deviceControlManager?.let {
                it.setSysTime(adminComponentName, millis)
                LogUtils.i(TAG, "系统时间设置成功")
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法设置系统时间")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置系统时间失败: ${e.message}", e)
        }
    }

    fun setDefaultLauncher(packageName: String, className: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置默认桌面")
                return
            }

            deviceControlManager?.let {
                it.setDefaultLauncher(adminComponentName, packageName, className)
                LogUtils.i(TAG, "默认桌面设置成功: $packageName/$className")
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法设置默认桌面")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置默认桌面失败: ${e.message}", e)
        }
    }

    fun clearDefaultLauncher() {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法清除默认桌面")
                return
            }

            deviceControlManager?.let {
                it.clearDefaultLauncher(adminComponentName)
                LogUtils.i(TAG, "默认桌面已清除")
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法清除默认桌面")
        } catch (e: Exception) {
            LogUtils.e(TAG, "清除默认桌面失败: ${e.message}", e)
        }
    }

    fun turnOnEyeComfort(enabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置护眼模式")
                return
            }

            deviceControlManager?.let {
                val result = it.turnOnEyeComfort(adminComponentName, enabled)
                if (result) {
                    LogUtils.i(TAG, "护眼模式${if (enabled) "开启" else "关闭"}成功")
                } else {
                    LogUtils.w(TAG, "护眼模式设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法设置护眼模式")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置护眼模式失败: ${e.message}", e)
        }
    }

    fun setSystemLanguage(locale: Locale) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置系统语言")
                return
            }

            deviceControlManager?.let {
                val result = it.setSystemLanguage(adminComponentName, locale)
                if (result) {
                    LogUtils.i(TAG, "系统语言设置成功: ${locale.displayLanguage}")
                } else {
                    LogUtils.w(TAG, "系统语言设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法设置系统语言")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置系统语言失败: ${e.message}", e)
        }
    }

    fun turnOnUsbDebugMode(enabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置USB调试")
                return
            }

            deviceControlManager?.let {
                val result = it.turnOnUsbDebugMode(adminComponentName, enabled)
                if (result) {
                    LogUtils.i(TAG, "USB调试${if (enabled) "开启" else "关闭"}成功")
                } else {
                    LogUtils.w(TAG, "USB调试设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法设置USB调试")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置USB调试失败: ${e.message}", e)
        }
    }

    fun setMediaControlDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置媒体控制")
                return
            }

            deviceControlManager?.let {
                val result = it.setMediaControlDisabled(adminComponentName, disabled)
                if (result) {
                    LogUtils.i(TAG, "媒体控制${if (disabled) "禁用" else "启用"}成功")
                } else {
                    LogUtils.w(TAG, "媒体控制设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法设置媒体控制")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置媒体控制失败: ${e.message}", e)
        }
    }

    fun setForcedActiveDeviceAdmin() {
        try {
            deviceControlManager?.let {
                val result = it.setForcedActiveDeviceAdmin(adminComponentName, context)
                if (result) {
                    LogUtils.i(TAG, "强制激活设备管理员成功")
                } else {
                    LogUtils.w(TAG, "强制激活设备管理员失败")
                }
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法强制激活设备管理员")
        } catch (e: Exception) {
            LogUtils.e(TAG, "强制激活设备管理员失败: ${e.message}", e)
        }
    }

    fun removeActiveDeviceAdmin() {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法解除激活设备管理员")
                return
            }

            deviceControlManager?.let {
                val result = it.removeActiveDeviceAdmin(adminComponentName)
                if (result) {
                    LogUtils.i(TAG, "解除激活设备管理员成功")
                } else {
                    LogUtils.w(TAG, "解除激活设备管理员失败")
                }
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法解除激活设备管理员")
        } catch (e: Exception) {
            LogUtils.e(TAG, "解除激活设备管理员失败: ${e.message}", e)
        }
    }

    fun setDelayDeactiveDeviceAdmin(delayTime: Int) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置延时解除激活")
                return
            }

            deviceControlManager?.let {
                val result = it.setDelayDeactiveDeviceAdmin(adminComponentName, delayTime, context)
                if (result) {
                    LogUtils.i(TAG, "延时解除激活设置成功: $delayTime 秒")
                } else {
                    LogUtils.w(TAG, "延时解除激活设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法设置延时解除激活")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置延时解除激活失败: ${e.message}", e)
        }
    }

    fun turnOnConnectionAlwaysOn(enabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置始终连接数据业务")
                return
            }
            deviceControlManager?.turnOnConnectionAlwaysOn(adminComponentName, enabled)
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置始终连接数据业务失败: ${e.message}", e)
        }
    }

    fun turnOnAutoRotation(enabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置自动旋转")
                return
            }

            deviceControlManager?.let {
                val result = it.turnOnAutoRotation(adminComponentName, enabled)
                if (result) {
                    LogUtils.i(TAG, "自动旋转${if (enabled) "开启" else "关闭"}成功")
                } else {
                    LogUtils.w(TAG, "自动旋转设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceControlManager不可用，无法设置自动旋转")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置自动旋转失败: ${e.message}", e)
        }
    }

    fun getDiagnosticReport(): String {
        val report = StringBuilder()
        report.appendLine("=== 华为设备控制诊断报告 ===")
        report.appendLine("管理员权限: ${if (checkAdminPermission()) "✅ 已激活" else "❌ 未激活"}")
        report.appendLine("设备所有者: ${if (HuaweiMDMAbility.isDeviceOwner()) "✅ 是" else "❌ 否"}")
        report.appendLine("DeviceControlManager: ${if (deviceControlManager != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("Root状态: ${if (isRooted()) "✅ 已Root" else "❌ 未Root"}")
        report.appendLine("================================")
        return report.toString()
    }
}
