package com.siyu.mdm.enterprise.util.mdm.vivo

import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.NetworkManager

/**
 * vivo网络管理器实现（占位）
 */
class VivoNetworkManager : NetworkManager {

    private val TAG = "VivoNetworkManager"

    override fun setWifiDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo WiFi控制功能未实现")
        throw UnsupportedOperationException("vivo WiFi控制功能待实现")
    }

    override fun setWifiApDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo WiFi热点控制功能未实现")
        throw UnsupportedOperationException("vivo WiFi热点控制功能待实现")
    }

    override fun setBluetoothDisabled(disabled: Boolean): Boolean {
        LogUtils.w(TAG, "vivo蓝牙控制功能未实现")
        return false
    }

    override fun setUSBDataDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo USB数据控制功能未实现")
        throw UnsupportedOperationException("vivo USB数据控制功能待实现")
    }

    override fun setUSBOtgDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo OTG控制功能未实现")
        throw UnsupportedOperationException("vivo OTG控制功能待实现")
    }

    override fun setNFCDisabled(disabled: Boolean): Boolean {
        LogUtils.w(TAG, "vivo NFC控制功能未实现")
        return false
    }

    override fun setMobileDataDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo移动数据控制功能未实现")
        throw UnsupportedOperationException("vivo移动数据控制功能待实现")
    }

    override fun setDataConnectivityDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo数据连接控制功能未实现")
        throw UnsupportedOperationException("vivo数据连接控制功能待实现")
    }

    override fun addApn(apnInfo: Map<String, String>) {
        LogUtils.w(TAG, "vivo APN配置功能未实现")
        throw UnsupportedOperationException("vivo APN配置功能待实现")
    }

    override fun deleteApn(apnId: String) {
        LogUtils.w(TAG, "vivo删除APN功能未实现")
        throw UnsupportedOperationException("vivo删除APN功能待实现")
    }

    override fun setConnectionAlwaysOn(enabled: Boolean) {
        LogUtils.w(TAG, "vivo始终在线连接功能未实现")
        throw UnsupportedOperationException("vivo始终在线连接功能待实现")
    }

    override fun isSupported(): Boolean = false
}
