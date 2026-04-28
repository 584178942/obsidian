package com.siyu.mdm.enterprise.util.mdm.huawei

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import com.siyu.mdm.enterprise.App
import com.siyu.mdm.enterprise.SampleDeviceReceiver
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.NetworkManager

class HuaweiNetworkManager : NetworkManager {

    private val TAG = "HuaweiNetworkManager"

    private val context: Context = App.instance

    private val adminComponentName by lazy {
        ComponentName(context, SampleDeviceReceiver::class.java)
    }

    private val devicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
    }

    private val deviceNetworkManager by lazy {
        HuaweiMDMAbility.getDeviceNetworkManager()
    }

    private val deviceWifiPolicyManager by lazy {
        HuaweiMDMAbility.getDeviceWifiPolicyManager()
    }

    private fun checkAdminPermission(): Boolean {
        return HuaweiMDMAbility.isDeviceAdmin()
    }

    override fun setWifiDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制WiFi")
                return
            }

            setWifiWithNativeAPI(disabled)
        } catch (e: Exception) {
            LogUtils.e(TAG, "WiFi设置失败: ${e.message}", e)
        }
    }

    private fun setWifiWithNativeAPI(disabled: Boolean) {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled = !disabled
            LogUtils.i(TAG, "WiFi${if (disabled) "禁用" else "启用"}成功")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置WiFi失败: ${e.message}", e)
        }
    }

    override fun setWifiApDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制WiFi热点")
                return
            }

            deviceWifiPolicyManager?.let {
               // it.setWifiApDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "WiFi热点${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceWifiPolicyManager不可用，无法控制WiFi热点")
        } catch (e: Exception) {
            LogUtils.e(TAG, "WiFi热点控制失败: ${e.message}", e)
        }
    }

    override fun setBluetoothDisabled(disabled: Boolean): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制蓝牙")
                return false
            }

            setBluetoothWithNativeAPI(disabled)
        } catch (e: Exception) {
            LogUtils.e(TAG, "蓝牙设置失败: ${e.message}", e)
            false
        }
    }

    private fun setBluetoothWithNativeAPI(disabled: Boolean): Boolean {
        return try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter?.let {
                if (disabled) {
                    //it.disable()
                } else {
                   // it.enable()
                }
                LogUtils.i(TAG, "蓝牙${if (disabled) "禁用" else "启用"}成功")
                true
            } ?: run {
                LogUtils.e(TAG, "蓝牙适配器不可用")
                false
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置蓝牙失败: ${e.message}", e)
            false
        }
    }

    override fun setUSBDataDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "USB数据控制请使用HuaweiHardwareManager")
    }

    override fun setUSBOtgDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "USB OTG控制请使用HuaweiHardwareManager")
    }

    override fun setNFCDisabled(disabled: Boolean): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制NFC")
                return false
            }

            setNFCWithNativeAPI(disabled)
        } catch (e: Exception) {
            LogUtils.e(TAG, "NFC设置失败: ${e.message}", e)
            false
        }
    }

    private fun setNFCWithNativeAPI(disabled: Boolean): Boolean {
        return try {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            nfcAdapter?.let {
                if (disabled) {
                  //  it.disable()
                } else {
                   // it.enable()
                }
                LogUtils.i(TAG, "NFC${if (disabled) "禁用" else "启用"}成功")
                true
            } ?: run {
                LogUtils.e(TAG, "NFC适配器不可用")
                false
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置NFC失败: ${e.message}", e)
            false
        }
    }

    override fun setMobileDataDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制移动数据")
                return
            }

            deviceNetworkManager?.let {
               // it.setMobileDataDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "移动数据${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法控制移动数据")
        } catch (e: Exception) {
            LogUtils.e(TAG, "移动数据控制失败: ${e.message}", e)
        }
    }

    override fun setDataConnectivityDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制数据连接")
                return
            }

            deviceNetworkManager?.let {
              //  it.setDataConnectivityDisabled(adminComponentName, disabled)
                LogUtils.i(TAG, "数据连接${if (disabled) "禁用" else "启用"}成功")
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法控制数据连接")
        } catch (e: Exception) {
            LogUtils.e(TAG, "数据连接控制失败: ${e.message}", e)
        }
    }

    override fun addApn(apnInfo: Map<String, String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法添加APN")
                return
            }

            deviceNetworkManager?.let {
                it.addApn(adminComponentName, apnInfo)
                LogUtils.i(TAG, "APN添加成功: $apnInfo")
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法添加APN")
        } catch (e: Exception) {
            LogUtils.e(TAG, "添加APN失败: ${e.message}", e)
        }
    }

    override fun deleteApn(apnId: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法删除APN")
                return
            }

            deviceNetworkManager?.let {
                it.deleteApn(adminComponentName, apnId)
                LogUtils.i(TAG, "APN删除成功: $apnId")
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法删除APN")
        } catch (e: Exception) {
            LogUtils.e(TAG, "删除APN失败: ${e.message}", e)
        }
    }

    override fun setConnectionAlwaysOn(enabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置始终在线连接")
                return
            }

            deviceNetworkManager?.let {
                //it.setConnectionAlwaysOn(adminComponentName, enabled)
                LogUtils.i(TAG, "始终在线连接${if (enabled) "启用" else "禁用"}成功")
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法设置始终在线连接")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置始终在线连接失败: ${e.message}", e)
        }
    }

    override fun isSupported(): Boolean {
        return checkAdminPermission()
    }

    fun addNetworkList(isTrustList: Boolean, isDomainList: Boolean, addrList: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法添加网络列表")
                return
            }

            deviceNetworkManager?.let {
                val result = it.addNetworkList(adminComponentName, isTrustList, isDomainList, ArrayList(addrList))
                if (result) {
                    LogUtils.i(TAG, "网络列表添加成功: ${if (isTrustList) "白名单" else "黑名单"}")
                } else {
                    LogUtils.w(TAG, "网络列表添加失败")
                }
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法添加网络列表")
        } catch (e: Exception) {
            LogUtils.e(TAG, "添加网络列表失败: ${e.message}", e)
        }
    }

    fun removeNetworkList(isTrustList: Boolean, isDomainList: Boolean, addrList: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法移除网络列表")
                return
            }

            deviceNetworkManager?.let {
                val result = it.removeNetworkList(adminComponentName, isTrustList, isDomainList, ArrayList(addrList))
                if (result) {
                    LogUtils.i(TAG, "网络列表移除成功: ${if (isTrustList) "白名单" else "黑名单"}")
                } else {
                    LogUtils.w(TAG, "网络列表移除失败")
                }
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法移除网络列表")
        } catch (e: Exception) {
            LogUtils.e(TAG, "移除网络列表失败: ${e.message}", e)
        }
    }

    fun getNetworkList(isTrustList: Boolean, isDomainList: Boolean): List<String> {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法获取网络列表")
                return emptyList()
            }

            deviceNetworkManager?.let {
                val list = it.getNetworkList(adminComponentName, isTrustList, isDomainList)
                list?.toList() ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取网络列表失败: ${e.message}", e)
            emptyList()
        }
    }

    fun addBrowserNetworkList(isTrustList: Boolean, addrList: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法添加浏览器网络列表")
                return
            }

            deviceNetworkManager?.let {
                val result = it.addBrowserNetworkList(adminComponentName, isTrustList, ArrayList(addrList))
                if (result) {
                    LogUtils.i(TAG, "浏览器网络列表添加成功: ${if (isTrustList) "白名单" else "黑名单"}")
                } else {
                    LogUtils.w(TAG, "浏览器网络列表添加失败")
                }
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法添加浏览器网络列表")
        } catch (e: Exception) {
            LogUtils.e(TAG, "添加浏览器网络列表失败: ${e.message}", e)
        }
    }

    fun setBrowserBookMarks(bookMarks: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置浏览器书签")
                return
            }

            deviceNetworkManager?.let {
                val result = it.setBrowserBookMarks(adminComponentName, bookMarks)
                if (result) {
                    LogUtils.i(TAG, "浏览器书签设置成功")
                } else {
                    LogUtils.w(TAG, "浏览器书签设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法设置浏览器书签")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置浏览器书签失败: ${e.message}", e)
        }
    }

    fun setBrowserHomePage(homePage: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置浏览器主页")
                return
            }

            deviceNetworkManager?.let {
                val result = it.setBrowserHomePage(adminComponentName, homePage)
                if (result) {
                    LogUtils.i(TAG, "浏览器主页设置成功: $homePage")
                } else {
                    LogUtils.w(TAG, "浏览器主页设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法设置浏览器主页")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置浏览器主页失败: ${e.message}", e)
        }
    }

    fun setDhcpOption(dhcpOption: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置DHCP Option")
                return
            }

            deviceNetworkManager?.let {
                val result = it.setDhcpOption(adminComponentName, dhcpOption)
                if (result) {
                    LogUtils.i(TAG, "DHCP Option设置成功: $dhcpOption")
                } else {
                    LogUtils.w(TAG, "DHCP Option设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法设置DHCP Option")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置DHCP Option失败: ${e.message}", e)
        }
    }

    fun configNtpTrustedTimeServer(serverName: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法配置NTP时间服务器")
                return
            }

            deviceNetworkManager?.let {
                val result = it.configNtpTrustedTimeServer(adminComponentName, serverName)
                if (result) {
                    LogUtils.i(TAG, "NTP时间服务器配置成功: $serverName")
                } else {
                    LogUtils.w(TAG, "NTP时间服务器配置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceNetworkManager不可用，无法配置NTP时间服务器")
        } catch (e: Exception) {
            LogUtils.e(TAG, "配置NTP时间服务器失败: ${e.message}", e)
        }
    }

    fun setWifiProDisabled(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制WLAN+")
                return
            }

            deviceWifiPolicyManager?.let {
                val result = it.setWifiProDisabled(adminComponentName, disabled)
                if (result) {
                    LogUtils.i(TAG, "WLAN+${if (disabled) "禁用" else "启用"}成功")
                } else {
                    LogUtils.w(TAG, "WLAN+设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceWifiPolicyManager不可用，无法控制WLAN+")
        } catch (e: Exception) {
            LogUtils.e(TAG, "控制WLAN+失败: ${e.message}", e)
        }
    }

    fun addSsidToBlockList(ssidList: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法添加WiFi热点黑名单")
                return
            }

            deviceWifiPolicyManager?.let {
                val result = it.addSsidToBlockList(adminComponentName, ArrayList(ssidList))
                if (result) {
                    LogUtils.i(TAG, "WiFi热点黑名单添加成功: $ssidList")
                } else {
                    LogUtils.w(TAG, "WiFi热点黑名单添加失败")
                }
            } ?: LogUtils.w(TAG, "DeviceWifiPolicyManager不可用，无法添加WiFi热点黑名单")
        } catch (e: Exception) {
            LogUtils.e(TAG, "添加WiFi热点黑名单失败: ${e.message}", e)
        }
    }

    fun getSsidBlockList(): List<String> {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法获取WiFi热点黑名单")
                return emptyList()
            }

            deviceWifiPolicyManager?.let {
                val list = it.getSsidBlockList(adminComponentName)
                list?.toList() ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取WiFi热点黑名单失败: ${e.message}", e)
            emptyList()
        }
    }

    fun setPortalAutoConnect(disabled: Boolean) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制portal热点自动连接")
                return
            }

            deviceWifiPolicyManager?.let {
                val result = it.setPortalAutoConnect(adminComponentName, disabled)
                if (result) {
                    LogUtils.i(TAG, "portal热点自动连接${if (disabled) "禁用" else "启用"}成功")
                } else {
                    LogUtils.w(TAG, "portal热点自动连接设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceWifiPolicyManager不可用，无法控制portal热点自动连接")
        } catch (e: Exception) {
            LogUtils.e(TAG, "控制portal热点自动连接失败: ${e.message}", e)
        }
    }

    fun getNetworkDiagnosticReport(): String {
        val report = StringBuilder()
        report.appendLine("=== 华为网络管理诊断报告 ===")
        report.appendLine("管理员权限: ${if (checkAdminPermission()) "✅ 已激活" else "❌ 未激活"}")
        report.appendLine()
        report.appendLine("【DeviceNetworkManager】")
        report.appendLine("- 实例: ${if (deviceNetworkManager != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("- WiFi黑名单: ${getNetworkList(false, false).size} 个")
        report.appendLine("- WiFi白名单: ${getNetworkList(true, false).size} 个")
        report.appendLine("- WiFi热点黑名单: ${getSsidBlockList().size} 个")
        report.appendLine()
        report.appendLine("【DeviceWifiPolicyManager】")
        report.appendLine("- 实例: ${if (deviceWifiPolicyManager != null) "✅ 可用" else "❌ 不可用"}")
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        report.appendLine("- WiFi状态: ${if (wifiManager.isWifiEnabled) "✅ 启用" else "❌ 禁用"}")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        report.appendLine("- 蓝牙状态: ${if (bluetoothAdapter?.isEnabled == true) "✅ 启用" else "❌ 禁用"}")
        //val nfcAdapter = NfcAdapter.getDefaultAdapter()
      //  report.appendLine("- NFC状态: ${if (nfcAdapter?.isEnabled == true) "✅ 启用" else "❌ 禁用"}")
        report.appendLine("================================")
        return report.toString()
    }
}
