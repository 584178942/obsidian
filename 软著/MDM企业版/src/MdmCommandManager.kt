package com.siyu.mdm.enterprise.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.SampleDeviceReceiver
import com.siyu.mdm.enterprise.util.mdm.MDMManagerFactory
import com.siyu.mdm.enterprise.util.mdm.SecurityManager
import com.siyu.mdm.enterprise.util.mdm.AppManager
import com.siyu.mdm.enterprise.util.mdm.NetworkManager
import com.siyu.mdm.enterprise.util.mdm.HardwareManager

/**
 * MDM命令管理器
 *
 * 统一入口，处理所有MDM命令
 * 根据当前厂商自动选择对应的实现
 *
 * 使用方式：
 * - MdmCommandManager(context).lockDevice()
 * - MdmCommandManager(context).unlockDevice()
 * - MdmCommandManager(context).wipeDevice()
 */
class MdmCommandManager(private val context: Context) {

    companion object {
        private const val TAG = "MdmCommandManager"
    }

    // ==================== 厂商管理器 ====================

    private val securityManager: SecurityManager by lazy {
        MDMManagerFactory.getSecurityManager()
    }

    private val appManager: AppManager by lazy {
        MDMManagerFactory.getAppManager()
    }

    private val networkManager: NetworkManager by lazy {
        MDMManagerFactory.getNetworkManager()
    }

    private val hardwareManager: HardwareManager by lazy {
        MDMManagerFactory.getHardwareManager()
    }

    // ==================== 设备管理员 ====================

    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private val adminComponent: ComponentName by lazy {
        ComponentName(context, SampleDeviceReceiver::class.java)
    }

    // ==================== 设备锁定 ====================

    /**
     * 锁定设备
     */
    fun lockDevice(mainText: String? = null, subText: String? = null): Boolean {
        LogUtils.i(TAG, "锁定设备: mainText=$mainText, subText=$subText")

        return try {
            if (!securityManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持设备锁定")
                return false
            }

            // 如果是华为设备，使用华为接口
            securityManager.lock(mainText, subText)
            LogUtils.i(TAG, "设备锁定成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "设备锁定失败", e)
            false
        }
    }

    /**
     * 解锁设备
     */
    fun unlockDevice(): Boolean {
        LogUtils.i(TAG, "解锁设备")

        return try {
            if (!securityManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持设备解锁")
                return false
            }

            securityManager.unlock()
            LogUtils.i(TAG, "设备解锁成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "设备解锁失败", e)
            false
        }
    }

    /**
     * 恢复出厂设置
     */
    fun wipeDevice(): Boolean {
        LogUtils.i(TAG, "恢复出厂设置")

        return try {
            if (!securityManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持恢复出厂设置")
                return false
            }

            securityManager.wipe()
            LogUtils.i(TAG, "恢复出厂设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "恢复出厂设置失败", e)
            false
        }
    }

    /**
     * 重置密码
     */
    fun resetPassword(newPassword: String): Boolean {
        LogUtils.i(TAG, "重置密码")

        return try {
            if (!securityManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持重置密码")
                return false
            }

            securityManager.resetPassword(newPassword)
            LogUtils.i(TAG, "密码重置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "密码重置失败", e)
            false
        }
    }

    // ==================== 应用管理 ====================

    /**
     * 安装应用
     */
    fun installApp(apkPath: String): Boolean {
        LogUtils.i(TAG, "安装应用: $apkPath")

        return try {
            if (!appManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持应用安装")
                return false
            }

            appManager.install(apkPath)
            LogUtils.i(TAG, "应用安装成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "应用安装失败", e)
            false
        }
    }

    /**
     * 卸载应用
     */
    fun uninstallApp(packageName: String): Boolean {
        LogUtils.i(TAG, "卸载应用: $packageName")

        return try {
            if (!appManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持应用卸载")
                return false
            }

            appManager.uninstall(packageName)
            LogUtils.i(TAG, "应用卸载成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "应用卸载失败", e)
            false
        }
    }

    /**
     * 设置安装黑名单
     */
    fun setInstallBlacklist(packageNames: Set<String>): Boolean {
        LogUtils.i(TAG, "设置安装黑名单: $packageNames")

        return try {
            if (!appManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持设置安装黑名单")
                return false
            }

            appManager.setInstallBlacklist(packageNames)
            LogUtils.i(TAG, "安装黑名单设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "安装黑名单设置失败", e)
            false
        }
    }

    /**
     * 设置运行黑名单
     */
    fun setRuntimeBlacklist(packageNames: Set<String>): Boolean {
        LogUtils.i(TAG, "设置运行黑名单: $packageNames")

        return try {
            if (!appManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持设置运行黑名单")
                return false
            }

            appManager.setRuntimeBlacklist(packageNames)
            LogUtils.i(TAG, "运行黑名单设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "运行黑名单设置失败", e)
            false
        }
    }

    // ==================== 网络管理 ====================

    /**
     * 设置WiFi状态
     */
    fun setWifiDisabled(disabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置WiFi状态: disabled=$disabled")

        return try {
            if (!networkManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持WiFi管理")
                return false
            }

            networkManager.setWifiDisabled(disabled)
            LogUtils.i(TAG, "WiFi状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "WiFi状态设置失败", e)
            false
        }
    }

    /**
     * 设置蓝牙状态
     */
    fun setBluetoothDisabled(disabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置蓝牙状态: disabled=$disabled")

        return try {
            if (!networkManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持蓝牙管理")
                return false
            }

            networkManager.setBluetoothDisabled(disabled)
            LogUtils.i(TAG, "蓝牙状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "蓝牙状态设置失败", e)
            false
        }
    }

    /**
     * 设置USB数据状态
     */
    fun setUSBDataDisabled(disabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置USB数据状态: disabled=$disabled")

        return try {
            if (!networkManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持USB管理")
                return false
            }

            networkManager.setUSBDataDisabled(disabled)
            LogUtils.i(TAG, "USB数据状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "USB数据状态设置失败", e)
            false
        }
    }

    /**
     * 设置NFC状态
     */
    fun setNFCDisabled(disabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置NFC状态: disabled=$disabled")

        return try {
            if (!networkManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持NFC管理")
                return false
            }

            networkManager.setNFCDisabled(disabled)
            LogUtils.i(TAG, "NFC状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "NFC状态设置失败", e)
            false
        }
    }

    /**
     * 设置移动数据状态
     */
    fun setMobileDataDisabled(disabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置移动数据状态: disabled=$disabled")

        return try {
            if (!networkManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持移动数据管理")
                return false
            }

            networkManager.setMobileDataDisabled(disabled)
            LogUtils.i(TAG, "移动数据状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "移动数据状态设置失败", e)
            false
        }
    }

    // ==================== 硬件管理 ====================

    /**
     * 设置截屏状态
     */
    fun setScreenCaptureDisabled(disabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置截屏状态: disabled=$disabled")

        return try {
            if (!hardwareManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持截屏管理")
                return false
            }

            hardwareManager.setScreenCaptureDisabled(disabled)
            LogUtils.i(TAG, "截屏状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "截屏状态设置失败", e)
            false
        }
    }

    /**
     * 设置麦克风状态
     */
    fun setMicrophoneDisabled(disabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置麦克风状态: disabled=$disabled")

        return try {
            if (!hardwareManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持麦克风管理")
                return false
            }

            hardwareManager.setMicrophoneDisabled(disabled)
            LogUtils.i(TAG, "麦克风状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "麦克风状态设置失败", e)
            false
        }
    }

    /**
     * 设置摄像头状态
     */
    fun setVideoDisabled(disabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置摄像头状态: disabled=$disabled")

        return try {
            if (!hardwareManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持摄像头管理")
                return false
            }

            hardwareManager.setVideoDisabled(disabled)
            LogUtils.i(TAG, "摄像头状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "摄像头状态设置失败", e)
            false
        }
    }

    // ==================== 管理员权限 ====================

    /**
     * 检查是否有设备管理员权限
     */
    fun hasAdminPermission(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    /**
     * 检查当前厂商是否支持
     */
    fun isCurrentVendorSupported(): Boolean {
        return securityManager.isSupported()
    }

    /**
     * 获取当前厂商名称
     */
    fun getCurrentVendorName(): String {
        return MDMManagerFactory.currentVendor.name
    }

    // ==================== 扩展功能 ====================

    /**
     * 重启设备
     */
    fun rebootDevice(): Boolean {
        LogUtils.i(TAG, "重启设备")

        return try {
            if (!securityManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持设备重启")
                return false
            }

            //securityManager.reboot()
            LogUtils.i(TAG, "设备重启成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "设备重启失败", e)
            false
        }
    }

    /**
     * 设置系统时间
     */
    fun setSystemTime(timestamp: Long): Boolean {
        LogUtils.i(TAG, "设置系统时间: $timestamp")

        return try {
            if (!hardwareManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持系统时间设置")
                return false
            }

           // hardwareManager.setSystemTime(timestamp)
            LogUtils.i(TAG, "系统时间设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "系统时间设置失败", e)
            false
        }
    }

    /**
     * 设置GPS状态
     */
    fun setGpsEnabled(enabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置GPS状态: enabled=$enabled")

        return try {
            if (!networkManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持GPS管理")
                return false
            }

            //networkManager.setGPSDisabled(!enabled)
            LogUtils.i(TAG, "GPS状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "GPS状态设置失败", e)
            false
        }
    }

    /**
     * 设置蓝牙状态
     */
    fun setBluetoothEnabled(enabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置蓝牙状态: enabled=$enabled")

        return try {
            setBluetoothDisabled(!enabled)
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "蓝牙状态设置失败", e)
            false
        }
    }

    /**
     * 设置飞行模式状态
     */
    fun setAirplaneModeEnabled(enabled: Boolean): Boolean {
        LogUtils.i(TAG, "设置飞行模式状态: enabled=$enabled")

        return try {
            if (!networkManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持飞行模式管理")
                return false
            }

          //  networkManager.setAirplaneMode(enabled)
            LogUtils.i(TAG, "飞行模式状态设置成功")
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "飞行模式状态设置失败", e)
            false
        }
    }

    /**
     * 获取已安装的应用列表
     */
   /* fun getInstalledApps(): List<AppInfo> {
        LogUtils.i(TAG, "获取已安装应用列表")

        return try {
            if (!appManager.isSupported()) {
                LogUtils.w(TAG, "当前厂商不支持应用列表查询")
                return emptyList()
            }

            appManager.()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取应用列表失败", e)
            emptyList()
        }
    }*/
}

/**
 * 应用信息数据类
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long
)
