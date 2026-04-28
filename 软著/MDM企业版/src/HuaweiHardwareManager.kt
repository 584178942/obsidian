package com.siyu.mdm.enterprise.util.mdm.huawei

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.siyu.mdm.enterprise.App
import com.siyu.mdm.enterprise.SampleDeviceReceiver
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.HardwareManager

class HuaweiHardwareManager : HardwareManager {

    private val TAG = "HuaweiHardwareManager"

    private val context: Context = App.instance

    private val adminComponentName: ComponentName by lazy {
        ComponentName(context, SampleDeviceReceiver::class.java)
    }

    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val deviceRestrictionManager by lazy {
        HuaweiMDMAbility.getDeviceRestrictionManager()
    }

    private val deviceSettingsManager by lazy {
        HuaweiMDMAbility.getDeviceSettingsManager()
    }

    private val deviceCameraManager by lazy {
        HuaweiMDMAbility.getDeviceCameraManager()
    }

    private fun checkAdminPermission(): Boolean {
        return HuaweiMDMAbility.isDeviceAdmin()
    }

    override fun setScreenCaptureDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制截屏")
                return
            }

            try {
                setScreenCaptureWithHuaweiAPI(disabled)
            } catch (e: NoSuchMethodError) {
                LogUtils.w(TAG, "华为API不支持，使用原生API: ${e.message}")
                setScreenCaptureWithNativeAPI(disabled)
            } catch (e: SecurityException) {
                LogUtils.e(TAG, "华为API权限不足: ${e.message}", e)
                setScreenCaptureWithNativeAPI(disabled)
            } catch (e: Exception) {
                LogUtils.e(TAG, "截屏控制异常: ${e.message}", e)
                setScreenCaptureWithNativeAPI(disabled)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "截屏设置异常: ${e.message}", e)
        }
    }

    private fun setScreenCaptureWithHuaweiAPI(disabled: Boolean) {
        try {
            deviceRestrictionManager?.let {
                it.setScreenCaptureDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "截屏${if (disabled) "禁用" else "启用"}成功（华为API）")
            } ?: setScreenCaptureWithNativeAPI(disabled)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun setScreenCaptureWithNativeAPI(disabled: Boolean) {
        try {
            devicePolicyManager.setScreenCaptureDisabled(adminComponentName, disabled)
            LogUtils.i(TAG, "截屏${if (disabled) "禁用" else "启用"}成功（原生API）")
        } catch (e: Exception) {
            LogUtils.e(TAG, "截屏设置异常: ${e.message}", e)
        }
    }

    override fun setMicrophoneDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制麦克风")
                return
            }

            try {
                setMicrophoneWithHuaweiAPI(disabled)
            } catch (e: NoSuchMethodError) {
                LogUtils.w(TAG, "华为麦克风API不支持，使用原生API: ${e.message}")
                setMicrophoneWithNativeAPI(disabled)
            } catch (e: SecurityException) {
                LogUtils.e(TAG, "华为API权限不足: ${e.message}", e)
                setMicrophoneWithNativeAPI(disabled)
            } catch (e: Exception) {
                LogUtils.e(TAG, "麦克风控制异常: ${e.message}", e)
                setMicrophoneWithNativeAPI(disabled)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "麦克风设置异常: ${e.message}", e)
        }
    }

    private fun setMicrophoneWithHuaweiAPI(disabled: Boolean) {
        try {
            deviceRestrictionManager?.let {
                it.setMicrophoneDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "麦克风${if (disabled) "禁用" else "启用"}成功（华为API）")
            } ?: setMicrophoneWithNativeAPI(disabled)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun setMicrophoneWithNativeAPI(disabled: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                deviceRestrictionManager?.setMicrophoneDisabled(adminComponentName, disabled)

                LogUtils.i(TAG, "麦克风${if (disabled) "禁用" else "启用"}成功（原生API）")
            } else {
                LogUtils.w(TAG, "原生API麦克风控制需要Android 7.0+")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "原生API设置麦克风失败: ${e.message}", e)
        }
    }

    override fun setVideoDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制摄像头视频录制")
                return
            }

            try {
                deviceCameraManager?.let {
                    val result = it.setVideoDisabled(adminComponentName, disabled)
                    if (result) {
                        LogUtils.i(TAG, "摄像头视频录制${if (disabled) "禁用" else "启用"}成功")
                    } else {
                        LogUtils.w(TAG, "摄像头视频录制设置失败")
                    }
                } ?: LogUtils.w(TAG, "DeviceCameraManager不可用，无法控制摄像头视频录制")
            } catch (e: Exception) {
                LogUtils.e(TAG, "摄像头视频录制控制失败: ${e.message}", e)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "摄像头视频录制设置异常: ${e.message}", e)
        }
    }

    override fun setCameraDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制摄像头")
                return
            }

            try {
                devicePolicyManager.setCameraDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "摄像头${if (disabled) "禁用" else "启用"}成功")
            } catch (e: Exception) {
                LogUtils.e(TAG, "摄像头控制失败: ${e.message}", e)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "摄像头设置异常: ${e.message}", e)
        }
    }

    override fun setMultiWindowDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制多窗口模式")
                return
            }

            deviceRestrictionManager?.let {
                it.setMultiWindowDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "多窗口模式${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制多窗口模式")
        } catch (e: Exception) {
            LogUtils.e(TAG, "多窗口模式控制失败: ${e.message}", e)
        }
    }

    override fun setNotificationDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制通知")
                return
            }

            deviceSettingsManager?.let {
                it.setNotificationDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "通知${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制通知")
        } catch (e: Exception) {
            LogUtils.e(TAG, "通知控制失败: ${e.message}", e)
        }
    }

    override fun setUnknownSourceInstallDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制未知来源安装")
                return
            }

            deviceRestrictionManager?.let {
                //it.setInstallSourceDenied(adminComponentName, disabled)
                LogUtils.i(TAG, "未知来源安装${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制未知来源安装")
        } catch (e: Exception) {
            LogUtils.e(TAG, "未知来源安装控制失败: ${e.message}", e)
        }
    }

    override fun setFileShareDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制文件分享")
                return
            }

            deviceRestrictionManager?.let {
               // it.setShareFileDenied(adminComponentName, disabled)
                LogUtils.i(TAG, "文件分享${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制文件分享")
        } catch (e: Exception) {
            LogUtils.e(TAG, "文件分享控制失败: ${e.message}", e)
        }
    }

    override fun setRestoreFactoryDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制恢复出厂设置")
                return
            }

            deviceRestrictionManager?.let {
                //it.setRestoreFactoryDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "恢复出厂设置${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制恢复出厂设置")
        } catch (e: Exception) {
            LogUtils.e(TAG, "恢复出厂设置控制失败: ${e.message}", e)
        }
    }

    override fun setPowerSaveModeDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制省电模式")
                return
            }

            deviceRestrictionManager?.let {
               // it.setPowerSaveModeDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "省电模式${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制省电模式")
        } catch (e: Exception) {
            LogUtils.e(TAG, "省电模式控制失败: ${e.message}", e)
        }
    }

    override fun setPrivacyProtection(enabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置隐私保护模式")
                return
            }

            deviceRestrictionManager?.let {
              //  it.setPrivacyProtectionEnabled(adminComponentName, enabled)
                LogUtils.i(TAG, "隐私保护模式${if (enabled) "启用" else "禁用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法设置隐私保护模式")
        } catch (e: Exception) {
            LogUtils.e(TAG, "隐私保护模式设置失败: ${e.message}", e)
        }
    }

    override fun isSupported(): Boolean {
        return try {
            checkAdminPermission()
        } catch (e: Exception) {
            false
        }
    }

    fun setUSBDataDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制USB数据")
                return
            }

            deviceRestrictionManager?.let {
                it.setUSBDataDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "USB数据${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制USB数据")
        } catch (e: Exception) {
            LogUtils.e(TAG, "USB数据控制失败: ${e.message}", e)
        }
    }

    fun setUSBOtgDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制USB OTG")
                return
            }

            deviceRestrictionManager?.let {
                it.setUSBOtgDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "USB OTG${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制USB OTG")
        } catch (e: Exception) {
            LogUtils.e(TAG, "USB OTG控制失败: ${e.message}", e)
        }
    }

    fun setStatusBarExpandPanelDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制状态栏下拉")
                return
            }

            deviceRestrictionManager?.let {
                it.setStatusBarExpandPanelDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "状态栏下拉${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制状态栏下拉")
        } catch (e: Exception) {
            LogUtils.e(TAG, "状态栏下拉控制失败: ${e.message}", e)
        }
    }

    fun setTaskButtonDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制任务键")
                return
            }

            deviceRestrictionManager?.let {
                it.setTaskButtonDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "任务键${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制任务键")
        } catch (e: Exception) {
            LogUtils.e(TAG, "任务键控制失败: ${e.message}", e)
        }
    }

    fun setHomeButtonDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制Home键")
                return
            }

            deviceRestrictionManager?.let {
                it.setHomeButtonDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "Home键${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制Home键")
        } catch (e: Exception) {
            LogUtils.e(TAG, "Home键控制失败: ${e.message}", e)
        }
    }

    fun setBackButtonDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制返回键")
                return
            }

            deviceRestrictionManager?.let {
                it.setBackButtonDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "返回键${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制返回键")
        } catch (e: Exception) {
            LogUtils.e(TAG, "返回键控制失败: ${e.message}", e)
        }
    }

    fun setClipboardDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制剪贴板")
                return
            }

            deviceRestrictionManager?.let {
                it.setClipboardDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "剪贴板${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制剪贴板")
        } catch (e: Exception) {
            LogUtils.e(TAG, "剪贴板控制失败: ${e.message}", e)
        }
    }

    fun setGPSDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制GPS")
                return
            }

            deviceRestrictionManager?.let {
                it.setGPSDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "GPS${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制GPS")
        } catch (e: Exception) {
            LogUtils.e(TAG, "GPS控制失败: ${e.message}", e)
        }
    }

    fun setAdbDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制ADB")
                return
            }

            deviceRestrictionManager?.let {
                it.setAdbDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "ADB${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制ADB")
        } catch (e: Exception) {
            LogUtils.e(TAG, "ADB控制失败: ${e.message}", e)
        }
    }

    fun setSafeModeDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制安全模式")
                return
            }

            deviceRestrictionManager?.let {
                it.setSafeModeDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "安全模式${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制安全模式")
        } catch (e: Exception) {
            LogUtils.e(TAG, "安全模式控制失败: ${e.message}", e)
        }
    }

    fun setExternalStorageDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制外部存储")
                return
            }

            deviceRestrictionManager?.let {
                it.setExternalStorageDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "外部存储${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceRestrictionManager不可用，无法控制外部存储")
        } catch (e: Exception) {
            LogUtils.e(TAG, "外部存储控制失败: ${e.message}", e)
        }
    }

    fun getHardwareDiagnosticReport(): String {
        val report = StringBuilder()
        report.appendLine("=== 华为硬件控制诊断报告 ===")
        report.appendLine("管理员权限: ${if (checkAdminPermission()) "✅ 已激活" else "❌ 未激活"}")
        report.appendLine("DeviceRestrictionManager: ${if (deviceRestrictionManager != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceCameraManager: ${if (deviceCameraManager != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("截屏: ${if (devicePolicyManager.getScreenCaptureDisabled(adminComponentName)) "❌ 禁用" else "✅ 启用"}")
        report.appendLine("摄像头: ${if (devicePolicyManager.getCameraDisabled(adminComponentName)) "❌ 禁用" else "✅ 启用"}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //report.appendLine("麦克风: ${if (devicePolicyManager.getMicrophoneDisabled(adminComponentName)) "❌ 禁用" else "✅ 启用"}")
        }
        report.appendLine("=================================")
        return report.toString()
    }
}
