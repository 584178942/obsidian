package com.siyu.mdm.enterprise.util.mdm

import android.os.Build
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.huawei.HuaweiAppManager
import com.siyu.mdm.enterprise.util.mdm.huawei.HuaweiHardwareManager
import com.siyu.mdm.enterprise.util.mdm.huawei.HuaweiNetworkManager
import com.siyu.mdm.enterprise.util.mdm.huawei.HuaweiSecurityManager
import com.siyu.mdm.enterprise.util.mdm.vivo.VivoAppManager
import com.siyu.mdm.enterprise.util.mdm.vivo.VivoHardwareManager
import com.siyu.mdm.enterprise.util.mdm.vivo.VivoNetworkManager
import com.siyu.mdm.enterprise.util.mdm.vivo.VivoSecurityManager
import com.siyu.mdm.enterprise.util.mdm.xiaomi.XiaomiAppManager
import com.siyu.mdm.enterprise.util.mdm.xiaomi.XiaomiHardwareManager
import com.siyu.mdm.enterprise.util.mdm.xiaomi.XiaomiNetworkManager
import com.siyu.mdm.enterprise.util.mdm.xiaomi.XiaomiSecurityManager

/**
 * MDM管理器工厂类
 * 根据设备厂商自动选择对应的MDM实现
 *
 * 使用方式:
 * ```
 * val securityManager = MDMManagerFactory.getSecurityManager()
 * securityManager.lock()
 * ```
 */
object MDMManagerFactory {

    private const val TAG = "MDMManagerFactory"

    /** 当前设备厂商 */
    val currentVendor: Vendor by lazy {
        detectVendor()
    }

    /**
     * 检测设备厂商
     */
    private fun detectVendor(): Vendor {
        val manufacturer = Build.MANUFACTURER
        LogUtils.d(TAG, "检测到设备厂商: $manufacturer")
        return Vendor.fromString(manufacturer).also {
            LogUtils.d(TAG, "厂商类型: $it")
        }
    }

    /**
     * 获取安全管理器实例
     * @return 对应厂商的SecurityManager实现
     * @throws UnsupportedOperationException 如果厂商不支持
     */
    fun getSecurityManager(): SecurityManager {
        LogUtils.d(TAG, "获取SecurityManager, 厂商: $currentVendor")
        return when (currentVendor) {
            Vendor.HUAWEI -> HuaweiSecurityManager()
            Vendor.XIAOMI -> XiaomiSecurityManager()
            Vendor.VIVO -> VivoSecurityManager()
            Vendor.UNKNOWN -> throw UnsupportedOperationException(
                "未知厂商 [$currentVendor] 不支持设备管理功能"
            )
        }
    }

    /**
     * 获取应用管理器实例
     * @return 对应厂商的AppManager实现
     * @throws UnsupportedOperationException 如果厂商不支持
     */
    fun getAppManager(): AppManager {
        LogUtils.d(TAG, "获取AppManager, 厂商: $currentVendor")
        return when (currentVendor) {
            Vendor.HUAWEI -> HuaweiAppManager()
            Vendor.XIAOMI -> XiaomiAppManager()
            Vendor.VIVO -> VivoAppManager()
            Vendor.UNKNOWN -> throw UnsupportedOperationException(
                "未知厂商 [$currentVendor] 不支持设备管理功能"
            )
        }
    }

    /**
     * 获取网络管理器实例
     * @return 对应厂商的NetworkManager实现
     * @throws UnsupportedOperationException 如果厂商不支持
     */
    fun getNetworkManager(): NetworkManager {
        LogUtils.d(TAG, "获取NetworkManager, 厂商: $currentVendor")
        return when (currentVendor) {
            Vendor.HUAWEI -> HuaweiNetworkManager()
            Vendor.XIAOMI -> XiaomiNetworkManager()
            Vendor.VIVO -> VivoNetworkManager()
            Vendor.UNKNOWN -> throw UnsupportedOperationException(
                "未知厂商 [$currentVendor] 不支持设备管理功能"
            )
        }
    }

    /**
     * 获取硬件管理器实例
     * @return 对应厂商的HardwareManager实现
     * @throws UnsupportedOperationException 如果厂商不支持
     */
    fun getHardwareManager(): HardwareManager {
        LogUtils.d(TAG, "获取HardwareManager, 厂商: $currentVendor")
        return when (currentVendor) {
            Vendor.HUAWEI -> HuaweiHardwareManager()
            Vendor.XIAOMI -> XiaomiHardwareManager()
            Vendor.VIVO -> VivoHardwareManager()
            Vendor.UNKNOWN -> throw UnsupportedOperationException(
                "未知厂商 [$currentVendor] 不支持设备管理功能"
            )
        }
    }

    /**
     * 检查是否支持设备管理功能
     * @return true if supported
     */
    fun isSupported(): Boolean = currentVendor != Vendor.UNKNOWN
}
