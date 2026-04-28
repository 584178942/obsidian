package com.siyu.mdm.enterprise.util.mdm.xiaomi

import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.NetworkManager

/**
 * 小米网络管理器实现（占位）
 */
class XiaomiNetworkManager : NetworkManager {

    private val TAG = "XiaomiNetworkManager"

    override fun setWifiDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米WiFi控制功能未实现")
        throw UnsupportedOperationException("小米WiFi控制功能待实现")
    }

    override fun setWifiApDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米WiFi热点控制功能未实现")
        throw UnsupportedOperationException("小米WiFi热点控制功能待实现")
    }

    override fun setBluetoothDisabled(disabled: Boolean): Boolean {
        LogUtils.w(TAG, "小米蓝牙控制功能未实现")
        return false
    }

    override fun setUSBDataDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米USB数据控制功能未实现")
        throw UnsupportedOperationException("小米USB数据控制功能待实现")
    }

    override fun setUSBOtgDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米OTG控制功能未实现")
        throw UnsupportedOperationException("小米OTG控制功能待实现")
    }

    override fun setNFCDisabled(disabled: Boolean): Boolean {
        LogUtils.w(TAG, "小米NFC控制功能未实现")
        return false
    }

    override fun setMobileDataDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米移动数据控制功能未实现")
        throw UnsupportedOperationException("小米移动数据控制功能待实现")
    }

    override fun setDataConnectivityDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米数据连接控制功能未实现")
        throw UnsupportedOperationException("小米数据连接控制功能待实现")
    }

    override fun addApn(apnInfo: Map<String, String>) {
        LogUtils.w(TAG, "小米APN配置功能未实现")
        throw UnsupportedOperationException("小米APN配置功能待实现")
    }

    override fun deleteApn(apnId: String) {
        LogUtils.w(TAG, "小米删除APN功能未实现")
        throw UnsupportedOperationException("小米删除APN功能待实现")
    }

    override fun setConnectionAlwaysOn(enabled: Boolean) {
        LogUtils.w(TAG, "小米始终在线连接功能未实现")
        throw UnsupportedOperationException("小米始终在线连接功能待实现")
    }

    override fun isSupported(): Boolean = false
}
