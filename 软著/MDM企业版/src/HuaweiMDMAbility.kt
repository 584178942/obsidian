package com.siyu.mdm.enterprise.util.mdm.huawei

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.huawei.android.app.admin.DeviceApplicationManager
import com.huawei.android.app.admin.DeviceBluetoothManager
import com.huawei.android.app.admin.DeviceCameraManager
import com.huawei.android.app.admin.DeviceControlManager
import com.huawei.android.app.admin.DeviceEmailManager
import com.huawei.android.app.admin.DeviceFirewallManager
import com.huawei.android.app.admin.DeviceHwSystemManager
import com.huawei.android.app.admin.DeviceInfraredManager
import com.huawei.android.app.admin.DeviceLocationManager
import com.huawei.android.app.admin.DeviceNetworkManager
import com.huawei.android.app.admin.DeviceP2PManager
import com.huawei.android.app.admin.DevicePackageManager
import com.huawei.android.app.admin.DevicePasswordManager
import com.huawei.android.app.admin.DevicePhoneManager
import com.huawei.android.app.admin.DeviceRestrictionManager
import com.huawei.android.app.admin.DeviceSettingsManager
import com.huawei.android.app.admin.DeviceStorageManagerEx
import com.huawei.android.app.admin.DeviceTelephonyManager
import com.huawei.android.app.admin.DeviceTelevisionManager
import com.huawei.android.app.admin.DeviceVpnManager
import com.huawei.android.app.admin.DeviceWifiPolicyManager
import com.siyu.mdm.enterprise.App
import com.siyu.mdm.enterprise.SampleDeviceReceiver
import com.siyu.mdm.enterprise.util.LogUtils

object HuaweiMDMAbility {

    private const val TAG = "HuaweiMDMAbility"

    private val context: Context = App.instance

    val adminComponentName: ComponentName by lazy {
        ComponentName(context, SampleDeviceReceiver::class.java)
    }

    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    fun isDeviceAdmin(): Boolean {
        return try {
            devicePolicyManager.isAdminActive(adminComponentName)
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查设备管理员权限失败: ${e.message}", e)
            false
        }
    }

    fun isDeviceOwner(): Boolean {
        return try {
            devicePolicyManager.isDeviceOwnerApp(adminComponentName.packageName)
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查设备所有者失败: ${e.message}", e)
            false
        }
    }

    fun getDeviceApplicationManager(): DeviceApplicationManager? {
        return try {
            DeviceApplicationManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceApplicationManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceBluetoothManager(): DeviceBluetoothManager? {
        return try {
            DeviceBluetoothManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceBluetoothManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceCameraManager(): DeviceCameraManager? {
        return try {
            DeviceCameraManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceCameraManager失败: ${e.message}", e)
            null
        }
    }
    fun getDeviceControlManager(): DeviceControlManager? {
        return try {
            DeviceControlManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceControlManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceEmailManager(): DeviceEmailManager? {
        return try {
            DeviceEmailManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceEmailManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceFirewallManager(): DeviceFirewallManager? {
        return try {
            DeviceFirewallManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceFirewallManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceHwSystemManager(): DeviceHwSystemManager? {
        return try {
            DeviceHwSystemManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceHwSystemManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceInfraredManager(): DeviceInfraredManager? {
        return try {
            DeviceInfraredManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceInfraredManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceLocationManager(): DeviceLocationManager? {
        return try {
            DeviceLocationManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceLocationManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceNetworkManager(): DeviceNetworkManager? {
        return try {
            DeviceNetworkManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceNetworkManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceP2PManager(): DeviceP2PManager? {
        return try {
            DeviceP2PManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceP2PManager失败: ${e.message}", e)
            null
        }
    }

    fun getDevicePackageManager(): DevicePackageManager? {
        return try {
            DevicePackageManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DevicePackageManager失败: ${e.message}", e)
            null
        }
    }

    fun getDevicePasswordManager(): DevicePasswordManager? {
        return try {
            DevicePasswordManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DevicePasswordManager失败: ${e.message}", e)
            null
        }
    }

    fun getDevicePhoneManager(): DevicePhoneManager? {
        return try {
            DevicePhoneManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DevicePhoneManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceRestrictionManager(): DeviceRestrictionManager? {
        return try {
            DeviceRestrictionManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceRestrictionManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceSettingsManager(): DeviceSettingsManager? {
        return try {
            DeviceSettingsManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceSettingsManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceStorageManagerEx(): DeviceStorageManagerEx? {
        return try {
            DeviceStorageManagerEx()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceStorageManagerEx失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceTelephonyManager(): DeviceTelephonyManager? {
        return try {
            DeviceTelephonyManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceTelephonyManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceTelevisionManager(): DeviceTelevisionManager? {
        return try {
            DeviceTelevisionManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceTelevisionManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceVpnManager(): DeviceVpnManager? {
        return try {
            DeviceVpnManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceVpnManager失败: ${e.message}", e)
            null
        }
    }

    fun getDeviceWifiPolicyManager(): DeviceWifiPolicyManager? {
        return try {
            DeviceWifiPolicyManager()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取DeviceWifiPolicyManager失败: ${e.message}", e)
            null
        }
    }

    fun getAllAbilityReport(): String {
        val report = StringBuilder()
        report.appendLine("=== 华为MDM能力获取报告 ===")
        report.appendLine()
        report.appendLine("【管理员状态】")
        report.appendLine("设备管理员: ${if (isDeviceAdmin()) "✅ 已激活" else "❌ 未激活"}")
        report.appendLine("设备所有者: ${if (isDeviceOwner()) "✅ 是" else "❌ 否"}")
        report.appendLine()
        report.appendLine("【应用管理类】")
        report.appendLine("DeviceApplicationManager: ${if (getDeviceApplicationManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DevicePackageManager: ${if (getDevicePackageManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine()
        report.appendLine("【设备控制类】")
        report.appendLine("DeviceControlManager: ${if (getDeviceControlManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine()
        report.appendLine("【网络管理类】")
        report.appendLine("DeviceNetworkManager: ${if (getDeviceNetworkManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceWifiPolicyManager: ${if (getDeviceWifiPolicyManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceFirewallManager: ${if (getDeviceFirewallManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine()
        report.appendLine("【蓝牙管理类】")
        report.appendLine("DeviceBluetoothManager: ${if (getDeviceBluetoothManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine()
        report.appendLine("【硬件限制类】")
        report.appendLine("DeviceRestrictionManager: ${if (getDeviceRestrictionManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceCameraManager: ${if (getDeviceCameraManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceInfraredManager: ${if (getDeviceInfraredManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine()
        report.appendLine("【系统设置类】")
        report.appendLine("DeviceSettingsManager: ${if (getDeviceSettingsManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceHwSystemManager: ${if (getDeviceHwSystemManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine()
        report.appendLine("【密码电话类】")
        report.appendLine("DevicePasswordManager: ${if (getDevicePasswordManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DevicePhoneManager: ${if (getDevicePhoneManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceTelephonyManager: ${if (getDeviceTelephonyManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine()
        report.appendLine("【位置存储类】")
        report.appendLine("DeviceLocationManager: ${if (getDeviceLocationManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceStorageManagerEx: ${if (getDeviceStorageManagerEx() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine()
        report.appendLine("【其他管理类】")
        report.appendLine("DeviceEmailManager: ${if (getDeviceEmailManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceP2PManager: ${if (getDeviceP2PManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceTelevisionManager: ${if (getDeviceTelevisionManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("DeviceVpnManager: ${if (getDeviceVpnManager() != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine()
        report.appendLine("===========================")
        return report.toString()
    }

    fun initAndCheck() {
        LogUtils.i(TAG, "=== 华为MDM能力初始化检查 ===")
        LogUtils.i(TAG, getAllAbilityReport())

        if (!isDeviceAdmin()) {
            LogUtils.w(TAG, "应用未激活设备管理员，MDM能力不可用")
            LogUtils.i(TAG, "建议调用 HuaweiAdminHelper.activateAdmin() 引导用户激活")
        } else {
            LogUtils.i(TAG, "设备管理员已激活，MDM能力可用")

            if (!isDeviceOwner()) {
                LogUtils.w(TAG, "应用仅为设备管理员（非所有者），部分能力可能受限")
            }
        }
    }
}
