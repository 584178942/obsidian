package com.siyu.mdm.custom

import android.content.ComponentName
import android.content.Context
import android.net.wifi.WifiConfiguration

/**
 * MDM基础接口 - 定义所有MDM实现必须具备的基本功能
 */
interface MDMBase {
    val adminComponentName: ComponentName
    fun checkAdminPermission(): Boolean
    val context: Context
}

/**
 * 应用管理接口 - 负责应用安装和运行控制
 */
interface AppManagement {
    fun setInstallWhitelist(packageNames: Set<String>)
    fun setInstallBlacklist(packageNames: Set<String>)
    fun setRuntimeWhitelist(packageNames: Set<String>)
    fun setRuntimeBlacklist(packageNames: Set<String>)
    fun canInstall(packageName: String): Boolean
    fun canRun(packageName: String): Boolean
    fun addInstallPackageBlockList(packageNames: List<String>): Boolean
    fun removeInstallPackageBlockList(packageNames: List<String>): Boolean
    fun getInstallPackageBlockList(): List<String>
    fun installPackage(packagePath:String)
    fun uninstallPackage(packagePath:String)
}

/**
 * 硬件限制接口 - 控制设备硬件功能
 */
interface HardwareRestriction {
    fun setMicrophoneDisabled(isDisabled: Boolean)
    fun setVideoDisabled(isDisabled: Boolean)
    fun setWifiDisabled(isDisabled: Boolean)
    fun setWifiApDisabled(isDisabled: Boolean)
    fun setBluetoothDisabled(isDisabled: Boolean)
    fun setUSBDataDisabled(isDisabled: Boolean)
    fun setUSBOtgDisabled(isDisabled: Boolean)
    fun setNFCDisabled(isDisabled: Boolean)
    fun setScreenCaptureDisabled(isDisabled: Boolean)
    fun setCameraDisabled(disabled: Boolean)
    fun setMultiWindowDisabled(disabled: Boolean)
    fun setNotificationDisabled(disabled: Boolean)
    fun setSearchIndexDisabled(disabled: Boolean)
    fun setDeveloperDisabled(disabled: Boolean)
    fun setUnknownSourceAppInstallDisabled(disabled: Boolean)

    fun setFileShareDisabled(disabled: Boolean)
    fun setBluetoothDataTransferDisable(disabled: Boolean)
    fun turnOnWifi(disabled: Boolean)
    fun setWifiHotspot(wifiConfig:WifiConfiguration )
    fun setSdCardDisabled(disabled: Boolean)
    fun setRestoreFactoryDisabled(disabled: Boolean)

    fun setSafeModeDisabled(disabled: Boolean)
    fun setAddUserDisabled(disabled: Boolean)
    fun setTimeAndDateSetDisabled(disabled: Boolean)
    fun setPasswordDisabled(wifiConfig:WifiConfiguration )
    fun resetPassword(disabled: Boolean)
    fun setAirplaneModeDisabled(disabled: Boolean)

    fun setVoiceDisabled(disabled: Boolean)
    fun setSMSDisabled(disabled: Boolean)
    fun setAccessPointNameDisabled(disabled: Boolean)
    fun setSlot2Disabled(disabled: Boolean )
    fun setVpnDisabled(disabled: Boolean)
    fun setUnlockByFingerprintDisabled(disabled: Boolean)

    fun setDevelopmentOptionDisabled(disabled: Boolean)
    fun turnOnUsbDebugMode(disabled: Boolean)
    fun setStatusBarExpandPanelDisabled(disabled: Boolean)
    fun setPowerSaveModeDisabled(disabled: Boolean)
    fun setDataSaverMode(disabled: Boolean)
}

/**
 * 网络管理接口 - 控制网络连接和APN
 */
interface NetworkManagement {
    fun setDataConnectivityDisabled(isDisabled: Boolean)
    fun turnOnConnectionAlwaysOn(isDisabled: Boolean)
    fun turnOnMobiledata(isDisabled: Boolean)
    fun addApn(apnInfo: Map<String, String>)
    fun deleteApn(apnId: String)
}

/**
 * 安全管理接口 - 设备安全相关功能
 */
interface SecurityManagement {
    fun resetPassword(newPassword: String)
    fun lockDevice(mainText: String? = null, subText: String? = null)
    fun unlockDevice()
    fun wipeDevice()
    //fun disableLocationAccess()
    //fun enableLocationAccess()
    ///fun enableAccessibilityService(serviceComponentName: ComponentName)
    /// fun disableAccessibilityService(serviceComponentName: ComponentName)
}

/**
 * 隐私保护接口 - 一键式隐私功能控制
 */
interface PrivacyProtection {
    fun enablePrivacyProtection(disabled: Boolean)
}

/**
 * 综合MDM接口 - 整合所有功能模块
 */
interface HuaweiMDM :
    MDMBase,
    AppManagement,
    HardwareRestriction,
    NetworkManagement,
    SecurityManagement,
    PrivacyProtection