package com.siyu.mdm.custom.device.util

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import com.blankj.utilcode.util.LogUtils
import com.huawei.android.app.admin.DeviceApplicationManager
import com.huawei.android.app.admin.DeviceBluetoothManager
import com.huawei.android.app.admin.DeviceCameraManager
import com.huawei.android.app.admin.DeviceControlManager
import com.huawei.android.app.admin.DeviceHwSystemManager
import com.huawei.android.app.admin.DeviceNetworkManager
import com.huawei.android.app.admin.DevicePackageManager
import com.huawei.android.app.admin.DevicePhoneManager
import com.huawei.android.app.admin.DeviceRestrictionManager
import com.huawei.android.app.admin.DeviceSettingsManager
import com.huawei.android.app.admin.DeviceTelephonyManager
import com.huawei.android.app.admin.DeviceVpnManager
import com.huawei.android.app.admin.DeviceWifiPolicyManager
import com.siyu.mdm.custom.HuaweiMDM
import com.siyu.mdm.custom.device.App
import com.siyu.mdm.custom.device.App.Companion.instance
import com.siyu.mdm.custom.device.CustomOverlayService
import com.siyu.mdm.custom.device.SampleDeviceReceiver
import com.siyu.mdm.custom.device.ui.FullScreenActivity
import java.util.concurrent.atomic.AtomicReference

/**
 * 华为MDM接口实现类 - 版本1
 * 
 * 该类是MDM（移动设备管理）功能的核心实现类，实现了华为设备管理相关API的封装和调用
 * 主要功能包括应用管理、硬件限制控制、网络管理、设备安全管理等企业级移动设备管理能力
 * 所有操作均需要先通过checkAdminPermission()验证管理员权限
 */
class HuaweiMDMManager : HuaweiMDM {
    /**
     * 应用上下文，使用懒加载方式获取App实例
     */
    override val context: Context by lazy { App.instance }
    
    /**
     * 管理员组件名称，用于标识设备管理员身份
     */
    override val adminComponentName: ComponentName by lazy {
        ComponentName(context, SampleDeviceReceiver::class.java)
    }

    // MDM SDK管理器实例
    private val devicePackageManager = DevicePackageManager()              // 应用包管理
    private val deviceApplicationManager = DeviceApplicationManager()      // 应用管理
    private var deviceRestrictionManager: Any? = null
    
    init {
        try {
            val drmClass = Class.forName("com.huawei.android.app.admin.DeviceRestrictionManager")
            deviceRestrictionManager = drmClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            LogUtils.d("成功初始化DeviceRestrictionManager")
        } catch (e: Exception) {
            LogUtils.w("无法初始化DeviceRestrictionManager，设备可能不支持华为MDM功能", e)
        }
    }
    
    /**
     * 使用反射安全地调用DeviceRestrictionManager的方法
     */
    private fun <T> safeCallDeviceRestrictionMethod(methodName: String, vararg args: Any?): T? {
        if (deviceRestrictionManager == null) {
            LogUtils.w("DeviceRestrictionManager未初始化，跳过调用: $methodName")
            return null
        }
        
        return try {
            val method = deviceRestrictionManager!!.javaClass.getMethod(methodName, *args.map { it?.javaClass ?: Any::class.java }.toTypedArray())
            method.invoke(deviceRestrictionManager, *args) as T?
        } catch (e: Exception) {
            LogUtils.w("调用DeviceRestrictionManager方法失败: $methodName", e)
            null
        }
    }
    private val deviceCameraManager = DeviceCameraManager()                // 相机管理
    private val deviceControlManager = DeviceControlManager()              // 设备控制管理
    internal val deviceSettingsManager = DeviceSettingsManager()           // 设备设置管理
    private val deviceNetworkManager = DeviceNetworkManager()              // 网络管理
    private val deviceBluetoothManager = DeviceBluetoothManager()          // 蓝牙管理
    private val deviceWifiPolicyManager = DeviceWifiPolicyManager()        // WiFi策略管理
    private val devicePhoneManager = DevicePhoneManager()                  // 电话管理
    private val deviceVpnManager = DeviceVpnManager()                      // VPN管理
    private val deviceTelephonyManager = DeviceTelephonyManager()          // 电信管理
    private val deviceHwSystemManager = DeviceHwSystemManager()            // 华为系统管理

    // 应用管理相关集合 - 使用AtomicReference确保线程安全
    private val installWhitelist = AtomicReference(mutableSetOf<String>()) // 安装白名单
    private val installBlacklist = AtomicReference(mutableSetOf<String>()) // 安装黑名单
    private val runtimeWhitelist = AtomicReference(mutableSetOf<String>()) // 运行白名单
    private val runtimeBlacklist = AtomicReference(mutableSetOf<String>()) // 运行黑名单
    private val trustedStores = AtomicReference(mutableSetOf<String>())    // 可信应用商店

    /**
     * 检查当前应用是否具有设备管理员权限
     * @return 是否具有管理员权限
     */
    override fun checkAdminPermission(): Boolean {
        val devicePolicyManager = 
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return devicePolicyManager.isAdminActive(adminComponentName)
    }

    // AppManagement接口实现
    /**
     * 设置应用安装白名单
     * @param packageNames 允许安装的应用包名集合
     */
    override fun setInstallWhitelist(packageNames: Set<String>) {
        installWhitelist.set(mutableSetOf<String>().apply { addAll(packageNames) })
        updateInstallWhitelist()
    }

    /**
     * 设置应用安装黑名单
     * @param packageNames 禁止安装的应用包名集合
     */
    override fun setInstallBlacklist(packageNames: Set<String>) {
        installBlacklist.set(mutableSetOf<String>().apply { addAll(packageNames) })
        updateInstallBlacklist()
    }

    /**
     * 更新设备上的应用安装白名单
     * 内部方法，用于将内存中的白名单同步到设备策略
     */
    private fun updateInstallWhitelist() {
        if (checkAdminPermission()) {
            try {
                devicePackageManager.addInstallPackageTrustList(adminComponentName, ArrayList(installWhitelist.get()))
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "更新安装白名单失败", e)
            }
        }
    }

    /**
     * 更新设备上的应用安装黑名单
     * 内部方法，用于将内存中的黑名单同步到设备策略
     */
    private fun updateInstallBlacklist() {
        if (checkAdminPermission()) {
            try {
                deviceApplicationManager.addInstallPackageBlockList(adminComponentName, ArrayList(installBlacklist.get()))
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "更新安装黑名单失败", e)
            }
        }
    }

    /**
     * 设置应用运行白名单
     * @param packageNames 允许运行的应用包名集合
     */
    override fun setRuntimeWhitelist(packageNames: Set<String>) {
        runtimeWhitelist.set(mutableSetOf<String>().apply { addAll(packageNames) })
        updateRuntimeWhitelist()
    }

    /**
     * 更新设备上的应用运行白名单
     * 内部方法，用于将内存中的运行白名单同步到设备策略
     */
    private fun updateRuntimeWhitelist() {
        if (checkAdminPermission()) {
            try {
                deviceApplicationManager.setTaskLockAppList(adminComponentName, ArrayList(runtimeWhitelist.get()))
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "更新运行白名单失败", e)
            }
        }
    }

    /**
     * 设置应用运行黑名单
     * @param packageNames 禁止运行的应用包名集合
     */
    override fun setRuntimeBlacklist(packageNames: Set<String>) {
        runtimeBlacklist.set(mutableSetOf<String>().apply { addAll(packageNames) })
        updateRuntimeBlacklist()
    }

    /**
     * 更新设备上的应用运行黑名单
     * 内部方法，用于将内存中的运行黑名单同步到设备策略
     */
    private fun updateRuntimeBlacklist() {
        if (checkAdminPermission()) {
            try {
                deviceApplicationManager.addDisallowedRunningApp(adminComponentName, ArrayList(runtimeBlacklist.get()))
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "更新运行黑名单失败", e)
            }
        }
    }

    /**
     * 判断指定应用是否允许安装
     * 优先检查白名单，白名单为空时检查黑名单
     * @param packageName 应用包名
     * @return 是否允许安装
     */
    override fun canInstall(packageName: String): Boolean {
        return if (installWhitelist.get().isNotEmpty()) {
            installWhitelist.get().contains(packageName)
        } else {
            !installBlacklist.get().contains(packageName)
        }
    }

    /**
     * 判断指定应用是否允许运行
     * 优先检查白名单，白名单为空时检查黑名单
     * @param packageName 应用包名
     * @return 是否允许运行
     */
    override fun canRun(packageName: String): Boolean {
        return if (runtimeWhitelist.get().isNotEmpty()) {
            runtimeWhitelist.get().contains(packageName)
        } else {
            !runtimeBlacklist.get().contains(packageName)
        }
    }

    override fun addInstallPackageBlockList(packageNames: List<String>): Boolean {
        if (!checkAdminPermission()) return false
        return try {
            deviceApplicationManager.addInstallPackageBlockList(adminComponentName, ArrayList(packageNames))
            true
        } catch (e: SecurityException) {
            LogUtils.e("MDM", "添加安装阻止列表失败", e)
            false
        }
    }

    override fun removeInstallPackageBlockList(packageNames: List<String>): Boolean {
        if (!checkAdminPermission()) return false
        return try {
            deviceApplicationManager.removeInstallPackageBlockList(adminComponentName, ArrayList(packageNames))
            true
        } catch (e: SecurityException) {
            LogUtils.e("MDM", "删除不允许安装应用名单失败", e)
            false
        }
    }

    override fun getInstallPackageBlockList(): List<String> {
        // 检查管理员权限，无权限返回空列表
        if (!checkAdminPermission()) {
            LogUtils.w("MDM", "获取安装阻止列表失败：无管理员权限")
            return emptyList()
        }
        return try {
            // 调用华为MDM接口获取列表，若返回null则转为空列表
            deviceApplicationManager.getInstallPackageBlockList(adminComponentName) ?: emptyList()
        } catch (e: SecurityException) {
            LogUtils.e("MDM", "获取安装阻止列表失败：权限异常", e)
            emptyList()
        } catch (e: Exception) {
            LogUtils.e("MDM", "获取安装阻止列表失败：未知错误", e)
            emptyList()
        }
    }

    /**
     * 安装指定路径的应用包
     * @param packagePath 应用安装包的本地路径
     */
    override fun installPackage(packagePath: String) {
        if (checkAdminPermission()) {
            try {
                devicePackageManager.installPackage(adminComponentName, packagePath)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "安装应用失败", e)
            }
        }
    }

    /**
     * 卸载指定路径的应用包
     * @param packagePath 应用包路径
     */
    override fun uninstallPackage(packagePath: String) {
        if (checkAdminPermission()) {
            try {
                devicePackageManager.uninstallPackage(adminComponentName, packagePath, false)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "卸载应用失败", e)
            }
        }
    }

    // 其他AppManagement方法...

    // HardwareRestriction接口实现
    /**
     * 设置是否禁用麦克风功能
     * @param isDisabled 是否禁用麦克风
     */
    override fun setMicrophoneDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setMicrophoneDisabled", adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用麦克风失败", e)
            }
        }
    }

    /**
     * 设置是否禁用摄像头视频录制功能
     * @param isDisabled 是否禁用视频录制
     */
    override fun setVideoDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceCameraManager.setVideoDisabled(adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用摄像头失败", e)
            }
        }
    }

    /**
     * 设置是否禁用WiFi功能
     * @param isDisabled 是否禁用WiFi
     */
    override fun setWifiDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setWifiDisabled", adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用 WLAN 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用WiFi热点功能
     * @param isDisabled 是否禁用WiFi热点
     */
    override fun setWifiApDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setWifiApDisabled", adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用 WLAN 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用蓝牙功能
     * @param isDisabled 是否禁用蓝牙
     */
    override fun setBluetoothDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setBluetoothDisabled", adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "配置蓝牙安全模式。 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用USB数据传输功能
     * @param isDisabled 是否禁用USB数据传输
     */
    override fun setUSBDataDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setUSBDataDisabled", adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用 USB 调试模式、数据传输 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用USB OTG功能
     * @param isDisabled 是否禁用USB OTG
     */
    override fun setUSBOtgDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setUSBOtgDisabled", adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用 USBOtg 功能 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用NFC功能
     * @param isDisabled 是否禁用NFC
     */
    override fun setNFCDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setNFCDisabled", adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用 NFC 功能 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用系统截屏功能
     * @param isDisabled 是否禁用截屏
     */
    override fun setScreenCaptureDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setScreenCaptureDisabled", adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用系统截屏失败", e)
            }
        }
    }

    /**
     * 设置是否禁用摄像头功能
     * 注意：当前实现为空（被注释掉）
     * @param disabled 是否禁用摄像头
     */
    override fun setCameraDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                //deviceCameraManager.setCameraDisabled(adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用系统截屏失败", e)
            }
        }
    }

    /**
     * 设置是否禁用系统多窗口功能
     * @param disabled 是否禁用多窗口
     */
    override fun setMultiWindowDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setMultiWindowDisabled", adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用系统多窗口功能失败", e)
            }
        }
    }

    /**
     * 设置是否禁用通知功能
     * @param disabled 是否禁用通知
     */
    override fun setNotificationDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceSettingsManager.setNotificationDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁止/允许所有系统以及三方应用通知消息失败", e)
            }
        }
    }

    /**
     * 设置是否禁用设置搜索功能
     * @param disabled 是否禁用设置搜索
     */
    override fun setSearchIndexDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceSettingsManager.setSearchIndexDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁止/允许设置搜索失败", e)
            }
        }
    }

    /**
     * 设置是否禁用开发者选项
     * 注意：当前仅打印日志"尽情期待"
     * @param disabled 是否禁用开发者选项
     */
    override fun setDeveloperDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                LogUtils.e("MDM", "尽情期待")
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "开启/关闭 WiFi 热点失败", e)
            }
        }
    }

    /**
     * 设置是否禁用外部来源应用安装
     * @param disabled 是否禁用外部来源应用安装
     */
    override fun setUnknownSourceAppInstallDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceSettingsManager.setUnknownSourceAppInstallDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用 安装外部来源应用 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用文件分享功能
     * @param disabled 是否禁用文件分享
     */
    override fun setFileShareDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setFileShareDisabled", adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁止/允许文件分享失败", e)
            }
        }
    }

    /**
     * 设置是否禁用蓝牙文件传输功能
     * @param disabled 是否禁用蓝牙文件传输
     */
    override fun setBluetoothDataTransferDisable(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceBluetoothManager.setBluetoothDataTransferDisable(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁止/允许蓝牙文件传输失败", e)
            }
        }
    }

    /**
     * 开启或关闭WiFi功能
     * @param disabled 是否禁用WiFi（false表示开启，true表示关闭）
     */
    override fun turnOnWifi(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceWifiPolicyManager.turnOnWifi(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "开启/关闭 WiFi 热点失败", e)
            }
        }
    }

    override fun setWifiHotspot(wifiConfig: WifiConfiguration) {
        if (checkAdminPermission()) {
            try {
                deviceWifiPolicyManager.setWifiHotspot(adminComponentName, wifiConfig)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "开启/关闭 WiFi 热点失败", e)
            }
        }
    }

    override fun setSdCardDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                LogUtils.e("MDM", "敬请期待")
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "热点失败", e)
            }
        }
    }

    override fun setRestoreFactoryDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceSettingsManager.setRestoreFactoryDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用恢复出厂设置 失败", e)
            }
        }
    }

    override fun setSafeModeDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setSafeModeDisabled", adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用安全模式 失败", e)
            }
        }
    }

    override fun setAddUserDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceSettingsManager.setAddUserDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用添加多用户 失败", e)
            }
        }
    }

    override fun setTimeAndDateSetDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceSettingsManager.setTimeAndDateSetDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用更改系统日期和时间设置 失败", e)
            }
        }
    }

    override fun setPasswordDisabled(wifiConfig: WifiConfiguration) {
        if (checkAdminPermission()) {
            try {
                LogUtils.i("MDM", "尽情期待")
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "开启/关闭 WiFi 热点失败", e)
            }
        }
    }

    /**
     * 重置密码功能
     * 注意：当前仅打印日志"尽情期待"
     * @param disabled 密码重置参数
     */
    override fun resetPassword(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                LogUtils.i("MDM", "尽情期待")
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "开启/关闭 WiFi 热点失败", e)
            }
        }
    }

    /**
     * 设置是否禁用飞行模式
     * @param disabled 是否禁用飞行模式
     */
    override fun setAirplaneModeDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceTelephonyManager.setAirplaneModeDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用飞行模式 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用通话功能
     * @param disabled 是否禁用通话
     */
    override fun setVoiceDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setVoiceDisabled", adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用通话 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用短信功能
     * @param disabled 是否禁用短信
     */
    override fun setSMSDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setSMSDisabled", adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用短信 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用APN（接入点名称）配置
     * @param disabled 是否禁用APN配置
     */
    override fun setAccessPointNameDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                devicePhoneManager.setAccessPointNameDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用 APN 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用SIM卡2
     * @param disabled 是否禁用SIM卡2
     */
    override fun setSlot2Disabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceTelephonyManager.setSlot2Disabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用卡槽 2 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用VPN功能
     * @param disabled 是否禁用VPN
     */
    override fun setVpnDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceVpnManager.setVpnDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用 VPN 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用指纹解锁功能
     * @param disabled 是否禁用指纹解锁
     */
    override fun setUnlockByFingerprintDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceSettingsManager.setUnlockByFingerprintDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "设置禁止/允许指纹解锁屏幕 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用开发者选项
     * @param disabled 是否禁用开发者选项
     */
    override fun setDevelopmentOptionDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceSettingsManager.setDevelopmentOptionDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁止/允许开发人员选项 失败", e)
            }
        }
    }

    /**
     * 设置是否开启USB调试模式
     * @param disabled 是否禁用USB调试（false表示开启，true表示关闭）
     */
    override fun turnOnUsbDebugMode(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceControlManager.turnOnUsbDebugMode(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "开启/关闭 USB 调试模式 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用状态栏下拉菜单
     * @param disabled 是否禁用状态栏下拉菜单
     */
    override fun setStatusBarExpandPanelDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setStatusBarExpandPanelDisabled", adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用状态栏下拉菜单 失败", e)
            }
        }
    }

    /**
     * 设置是否禁用省电模式
     * @param disabled 是否禁用省电模式
     */
    override fun setPowerSaveModeDisabled(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceHwSystemManager.setPowerSaveModeDisabled(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用/启用省电模式 失败", e)
            }
        }
    }

    /**
     * 设置是否启用数据节省模式
     * @param disabled 是否禁用数据节省模式
     */
    override fun setDataSaverMode(disabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceHwSystemManager.setDataSaverMode(adminComponentName, disabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "开启/关闭 WiFi 热点失败", e)
            }
        }
    }

    // 其他HardwareRestriction方法...

    // NetworkManagement接口实现
    /**
     * 设置是否禁用数据连接
     * @param isDisabled 是否禁用数据连接
     */
    override fun setDataConnectivityDisabled(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                safeCallDeviceRestrictionMethod<Any>("setDataConnectivityDisabled", adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "禁用数据连接失败", e)
            }
        }
    }

    /**
     * 设置是否开启连接始终在线功能
     * @param isDisabled 是否禁用连接始终在线
     */
    override fun turnOnConnectionAlwaysOn(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceControlManager.turnOnConnectionAlwaysOn(adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "始终连接数据业务失败", e)
            }
        }
    }

    /**
     * 设置是否开启移动数据连接
     * @param isDisabled 是否禁用移动数据（false表示开启，true表示关闭）
     */
    override fun turnOnMobiledata(isDisabled: Boolean) {
        if (checkAdminPermission()) {
            try {
                deviceControlManager.turnOnMobiledata(adminComponentName, isDisabled)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "强制开启/关闭数据连接失败", e)
            }
        }
    }

    /**
     * 添加APN接入点配置
     * @param apnInfo 包含APN配置信息的映射表
     */
    override fun addApn(apnInfo: Map<String, String>) {
        if (checkAdminPermission()) {
            try {
                deviceNetworkManager.addApn(adminComponentName, apnInfo)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "增加 APN失败", e)
            }
        }
    }

    /**
     * 删除指定ID的APN接入点配置
     * @param apnId 要删除的APN配置ID
     */
    override fun deleteApn(apnId: String) {
        if (checkAdminPermission()) {
            try {
                deviceNetworkManager.deleteApn(adminComponentName, apnId)
            } catch (e: SecurityException) {
                LogUtils.e("MDM", "删除 APN失败", e)
            }
        }
    }

    // 其他NetworkManagement方法...

    // SecurityManagement接口实现
    /**
     * 重置设备密码
     * @param newPassword 新的密码字符串
     * 注意：此功能需要应用具有设备管理员权限
     */
    override fun resetPassword(newPassword: String) {
        if (!checkAdminPermission()) return
        try {
            val devicePolicyManager = 
                context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (devicePolicyManager.isDeviceOwnerApp(adminComponentName.packageName)) {
                val result = devicePolicyManager.resetPassword(
                    newPassword,
                    DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY
                )
                LogUtils.i("LockScreen", "密码修改结果: $result")
            } else {
                LogUtils.i("LockScreen", "应用不是设备管理员")
            }
        } catch (e: SecurityException) {
            LogUtils.e("LockScreen", "密码修改异常", e)
        }
    }
    /**
     * 启动锁定屏幕活动
     * @param mainText 主文本内容，默认为设备已锁定
     * @param subText 副标题内容，默认为请联系管理员解锁
     */
    override fun lockDevice(mainText: String?, subText: String?) {
        try {
            deviceApplicationManager.addSingleApp(adminComponentName, instance.packageName);
            val dpm: DevicePolicyManager = instance.getSystemService(DevicePolicyManager::class.java)
            // 检查当前 App 是否为 Device Owner, 只有 Device Owner 才可以设置锁定
            //  if (!dpm.isDeviceOwnerApp(contextApp.getPackageName())) return;
            // 添加 com.foo.bar 应用到锁定任务模式的许可名单
            dpm.setLockTaskPackages(
                adminComponentName,
                arrayOf<String>(instance.packageName)
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // 设置锁定任务模式下系统状态栏显示时间、电量和网络等信息
                dpm.setLockTaskFeatures(
                    adminComponentName,
                    DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO
                )

                // dpm.addUserRestriction(component, UserManager.DISALLOW_CREATE_WINDOWS);
                // LogUtils.info("setLockTaskFeatures","LOCK_TASK_FEATURE_GLOBAL_ACTIONS"+dpm.getLockTaskFeatures(component));
            }
            val intent = Intent(instance, FullScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 设置自定义文字内容
            mainText?.let {
                intent.putExtra(FullScreenActivity.EXTRA_MAIN_TEXT, it)
            }
            subText?.let {
                intent.putExtra(FullScreenActivity.EXTRA_SUB_TEXT, it)
            }

            var am = instance.getSystemService(ActivityManager::class.java)
            if (!am.isInLockTaskMode) {
                intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }
            var options: ActivityOptions? = null

            options = ActivityOptions.makeBasic()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                options.setLockTaskEnabled(true)
            }
            instance.startActivity(intent, options.toBundle())
        } catch (e: SecurityException) {
            e.localizedMessage?.let { LogUtils.e( it) }
        } catch (unused: ActivityNotFoundException) {
            LogUtils.e(
                "startLockActivity ActivityNotFoundException Error :" + unused.localizedMessage
            )
        }
    }

    /**
     * 解锁设备，退出单应用锁定模式
     * 该方法会清除单应用设置并移除所有锁定任务包配置
     */
    override fun unlockDevice() {
        try {
            deviceApplicationManager.clearSingleApp(adminComponentName, instance.packageName);
            val dpm: DevicePolicyManager = 
                instance.getSystemService(DevicePolicyManager::class.java)
            dpm.setLockTaskPackages(
                adminComponentName,
                arrayOf<String>()
            )
        } catch (e: Exception) {
            LogUtils.e( "closeLockActivity:" + e.localizedMessage
            )
        }
    }

    /**
     * 执行设备恢复出厂设置操作
     * 注意：此操作会清除设备上的所有数据，包括用户数据、应用程序和设置
     * 此功能需要应用具有设备管理员权限
     */
    override fun wipeDevice() {
        val devicePolicyManager = instance.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            if (devicePolicyManager.isAdminActive(adminComponentName)) {
                devicePolicyManager.wipeData(0)
                LogUtils.i( "恢复出厂设置已触发")
            } else {
                LogUtils.i( "Application is not the device owner.")
            }
        } catch (e: SecurityException) {
            LogUtils.i( "权限不足: ${e.message}")
        } catch (e: Exception) {
            LogUtils.i("恢复出厂设置失败: ${e.message}")
        }
    }
    
    /**
     * 检查无障碍服务是否开启
     */
    fun isAccessibilityServiceEnabled(serviceComponentName: ComponentName): Boolean {
        var accessibilityEnabled = 0
        val service: String = 
            serviceComponentName.packageName + "/" + serviceComponentName.className
        try {
            accessibilityEnabled = 
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            LogUtils.e("Accessibility", "获取无障碍服务状态失败", e)
        }
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                colonSplitter.setString(settingValue)
                while (colonSplitter.hasNext()) {
                    val componentName = colonSplitter.next()
                    if (componentName.equals(service, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }
    
    /**
     * 引导用户启用无障碍服务
     */
    fun enableAccessibilityService(serviceComponentName: ComponentName) {
        if (checkAdminPermission()) {
            try {
                if (!isAccessibilityServiceEnabled(serviceComponentName)) {
                    // 跳转到无障碍设置页面
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    LogUtils.i("Accessibility", "已引导用户前往无障碍设置页面")
                } else {
                    LogUtils.i("Accessibility", "无障碍服务已启用")
                }
            } catch (e: Exception) {
                LogUtils.e("Accessibility", "引导用户启用无障碍服务失败", e)
            }
        }
    }


    /**
     * 引导用户禁用无障碍服务
     */
    fun disableAccessibilityService(serviceComponentName: ComponentName) {

        if (checkAdminPermission()) {
            try {
                if (isAccessibilityServiceEnabled(serviceComponentName)) {
                    // 跳转到无障碍设置页面
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    LogUtils.i("Accessibility", "已引导用户前往无障碍设置页面")
                } else {
                    LogUtils.i("Accessibility", "无障碍服务未启用")
                }
            } catch (e: Exception) {
                LogUtils.e("Accessibility", "引导用户禁用无障碍服务失败", e)
            }
        }
    }

    /**
     * DevicePolicyManager的扩展函数，用于批量授予应用程序权限
     * @param adminComponent 设备管理员组件名
     * @param packageName 目标应用包名
     * @param permissions 要授予的权限列表
     * @return 权限授予操作是否成功完成
     * 注意：SYSTEM_ALERT_WINDOW权限需要特殊处理，不会通过此方法授予
     */
    fun DevicePolicyManager.grantPermissions(
        adminComponent: ComponentName,
        packageName: String,
        vararg permissions: String
    ): Boolean {
        return try {
            // 根据Android版本选择正确的权限授予方式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 对于Android 6.0以上，使用标准的权限授予方式
                for (permission in permissions) {
                    if (permission == android.Manifest.permission.SYSTEM_ALERT_WINDOW) {
                        // SYSTEM_ALERT_WINDOW需要特殊处理
                        continue // 这个权限需要单独处理
                    }
                    // 设置普通权限
                    setPermissionGrantState(adminComponent, packageName, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED)
                }
            } else {
                // 对于旧版本Android，权限在安装时授予
                LogUtils.i("PermissionHelper", "Android版本低于6.0，权限在安装时已授予")
            }
            true
        } catch (e: Exception) {
            LogUtils.e("PermissionHelper", "权限授予扩展函数执行失败: ${e.message}")
            false
        }
    }

    /**
     * 设置水印应用所需的全部权限并启动水印服务
     * 此方法会自动处理以下事项：
     * 1. 检查并引导用户授予悬浮窗权限（SYSTEM_ALERT_WINDOW）
     * 2. 使用DevicePolicyManager授予其他必要权限
     * 3. 检查并引导用户启用无障碍服务
     * 4. 启动水印前台服务
     * @param context 上下文对象
     * @return 水印服务是否成功设置并启动
     *         - 返回false表示需要用户手动完成某些权限授予操作
     */
    fun setupWatermarkService(context: Context): Boolean {
        val packageName = context.packageName
        
        try {
            // 1. 确保授予SYSTEM_ALERT_WINDOW权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(context)) {
                    LogUtils.i("WatermarkHelper", "需要引导用户授予悬浮窗权限")
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
            
            // 2. 使用DevicePolicyManager授予其他必要权限
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            
            // 授予普通权限
            dpm.grantPermissions(
                adminComponent = adminComponentName,
                packageName = packageName,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.FOREGROUND_SERVICE
            )
            
            // 3. 检查并启用无障碍服务
            val overlayServiceComponent = ComponentName(context, CustomOverlayService::class.java)
            if (!isAccessibilityServiceEnabled(overlayServiceComponent)) {
                enableAccessibilityService(overlayServiceComponent)
                return false // 需要用户手动启用
            }
            
            // 4. 启动水印服务
            val intent = Intent(context, CustomOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            LogUtils.i("WatermarkHelper", "水印服务设置成功")
            return true
            
        } catch (e: Exception) {
            LogUtils.e("WatermarkHelper", "水印服务设置失败: ${e.message}")
            return false
        }
    }

    /**
     * 检查水印服务是否正在运行
     * 水印服务基于无障碍服务实现，因此通过检查无障碍服务状态来判断
     * @param context 上下文对象
     * @return 水印服务是否正在运行
     */
    fun isWatermarkServiceRunning(context: Context): Boolean {
        val componentName = ComponentName(context, CustomOverlayService::class.java)
        return isAccessibilityServiceEnabled(componentName)
    }

    /**
     * 停止正在运行的水印服务
     * @param context 上下文对象
     */
    fun stopWatermarkService(context: Context) {
        try {
            val intent = Intent(context, CustomOverlayService::class.java)
            context.stopService(intent)
            LogUtils.i("WatermarkHelper", "已尝试停止水印服务")
        } catch (e: Exception) {
            LogUtils.e("WatermarkHelper", "停止水印服务失败: ${e.message}")
        }
    }

    /**
     * 启用或禁用隐私保护功能
     * 注意：此方法尚未实现
     * @param disabled true表示禁用隐私保护，false表示启用隐私保护
     */
    override fun enablePrivacyProtection(disabled: Boolean) {
        TODO("Not yet implemented")
    }
}

/**
 * 华为MDM接口实现类 - 版本2，展示接口扩展能力
 */
/*
class HuaweiMDMManagerV2 : HuaweiMDMManagerV1() {
    // 扩展V1版本功能，新增网络诊断功能
    fun diagnoseNetwork() {
        LogUtils.i("MDM_V2", "开始网络诊断...")
        // 实现网络诊断逻辑
    }

    // 重写父类方法以增强功能
    override fun setWifiDisabled(isDisabled: Boolean) {
        LogUtils.i("MDM_V2", "设置WiFi状态: $isDisabled")
        super.setWifiDisabled(isDisabled)
    }
}*/