package com.siyu.mdm.custom.device.json
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

// 主策略类
data class Policy(
    @SerializedName("createBy") val createBy: String,
    @SerializedName("createTime") val createTime: String,
    @SerializedName("deptId") val deptId: String,
    @SerializedName("id") val id: String,
    @SerializedName("policyContent") val policyContent: String,
    @SerializedName("policyType") val policyType: String,
    @SerializedName("tenantId") val tenantId: Int,
    @SerializedName("updateBy") val updateBy: String? = null,
    @SerializedName("updateTime") val updateTime: String
) {
    // 解析后的策略内容，根据policyType自动转换
    // 修改委托实现，明确指定返回类型
    val parsedContent: PolicyContent? by lazy(LazyThreadSafetyMode.NONE) {
        when (policyType) {
            "bluetoothPolicy" -> parseBluetoothPolicy()
            "LimitPolicy" -> parseLimitPolicy()
            "packagePolicy" -> parsePackagePolicy()
            "ssidPolicy" -> parseSsidPolicy()
            "WifiPolicy" -> null // 特殊处理，因为WifiPolicy返回List<WifiConfig>
            else -> null
        }
    }
    // 特殊处理WifiPolicy，返回List<WifiConfig>
    val wifiConfigs: List<WifiConfig>? by lazy(LazyThreadSafetyMode.NONE) {
        if (policyType == "WifiPolicy") parseWifiPolicy() else null
    }
    private fun parseBluetoothPolicy(): BluetoothPolicy? {
        return try {
            Gson().fromJson(policyContent, BluetoothPolicy::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLimitPolicy(): LimitPolicy? {
        return try {
            Gson().fromJson(policyContent, LimitPolicy::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePackagePolicy(): PackagePolicy? {
        return try {
            Gson().fromJson(policyContent, PackagePolicy::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSsidPolicy(): SsidPolicy? {
        return try {
            Gson().fromJson(policyContent, SsidPolicy::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWifiPolicy(): List<WifiConfig>? {
        return try {
            Gson().fromJson<List<WifiConfig>>(policyContent, object : TypeToken<List<WifiConfig>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
}

// 策略内容的密封类
sealed class PolicyContent

// 蓝牙策略
data class BluetoothPolicy(
    @SerializedName("type") val type: Int
) : PolicyContent()

// 限制策略
data class LimitPolicy(
    @SerializedName("screenCapture") val screenCapture: Boolean,
    @SerializedName("unknownSource") val unknownSource: Boolean,
    @SerializedName("multiWindow") val multiWindow: Boolean,
    @SerializedName("notification") val notification: Boolean,
    @SerializedName("setupSearch") val setupSearch: Boolean,
    @SerializedName("developer") val developer: Boolean,
    @SerializedName("fileShare") val fileShare: Boolean,
    @SerializedName("bluetoothFileTransfer") val bluetoothFileTransfer: Boolean,
    @SerializedName("wifi") val wifi: Boolean,
    @SerializedName("nfc") val nfc: Boolean,
    @SerializedName("hotspot") val hotspot: Boolean,
    @SerializedName("sd") val sd: Boolean,
    @SerializedName("microphone") val microphone: Boolean,
    @SerializedName("factory") val factory: Boolean,
    @SerializedName("safemode") val safemode: Boolean,
    @SerializedName("multipleUsers") val multipleUsers: Boolean,
    @SerializedName("time") val time: Boolean,
    @SerializedName("forcePassword") val forcePassword: Boolean,
    @SerializedName("setPowerOnPassword") val setPowerOnPassword: Boolean,
    @SerializedName("flight") val flight: Boolean,
    @SerializedName("shortMessageSend") val shortMessageSend: Boolean,
    @SerializedName("id") val id: Boolean,
    @SerializedName("apn") val apn: Boolean,
    @SerializedName("vpn") val vpn: Boolean,
    @SerializedName("videoCam") val videoCam: Boolean,
    @SerializedName("fingerprint") val fingerprint: Boolean,
    @SerializedName("usb") val usb: Boolean,
    @SerializedName("usbDebug") val usbDebug: Boolean,
    @SerializedName("otgSwitch") val otgSwitch: Boolean,
    @SerializedName("workdayLockScreen") val workdayLockScreen: Boolean,
    @SerializedName("pullDown") val pullDown: Boolean,
    @SerializedName("powerSave") val powerSave: Boolean,
    @SerializedName("trafficSave") val trafficSave: Boolean
) : PolicyContent()

// 应用包策略
data class PackagePolicy(
    @SerializedName("install") val install: Int,
    @SerializedName("installList") val installList: String,
    @SerializedName("use") val use: Int,
    @SerializedName("useList") val useList: String,
    @SerializedName("uninstallList") val uninstallList: String,
    @SerializedName("uninstall") val uninstall: Int
) : PolicyContent()

// SSID策略
data class SsidPolicy(
    @SerializedName("type") val type: Int,
    @SerializedName("list") val list: String
) : PolicyContent()

// WiFi配置
data class WifiConfig(
    @SerializedName("ssid") val ssid: String,
    @SerializedName("password") val password: String,
    @SerializedName("autoConnect") val autoConnect: Boolean
) : PolicyContent()