package com.siyu.mdm.enterprise.util.mdm

/**
 * 网络管理器接口
 * 定义WiFi、蓝牙、USB、NFC等网络相关硬件的控制操作
 *
 * 各厂商实现类应实现此接口
 */
interface NetworkManager {

    /**
     * 设置WiFi状态
     * @param disabled true=禁用，false=启用
     */
    fun setWifiDisabled(disabled: Boolean)

    /**
     * 设置WiFi热点状态
     * @param disabled true=禁用，false=启用
     */
    fun setWifiApDisabled(disabled: Boolean)

    /**
     * 设置蓝牙状态
     * @param disabled true=禁用，false=启用
     * @return 操作是否成功
     */
    fun setBluetoothDisabled(disabled: Boolean): Boolean

    /**
     * 设置USB数据传输状态
     * @param disabled true=禁用，false=启用
     */
    fun setUSBDataDisabled(disabled: Boolean)

    /**
     * 设置USB OTG状态
     * @param disabled true=禁用，false=启用
     */
    fun setUSBOtgDisabled(disabled: Boolean)

    /**
     * 设置NFC状态
     * @param disabled true=禁用，false=启用
     * @return 操作是否成功
     */
    fun setNFCDisabled(disabled: Boolean): Boolean

    /**
     * 设置移动数据状态
     * @param disabled true=禁用，false=启用
     */
    fun setMobileDataDisabled(disabled: Boolean)

    /**
     * 设置数据连接状态
     * @param disabled true=禁用，false=启用
     */
    fun setDataConnectivityDisabled(disabled: Boolean)

    /**
     * 添加APN配置
     * @param apnInfo APN配置信息Map
     */
    fun addApn(apnInfo: Map<String, String>)

    /**
     * 删除APN配置
     * @param apnId APN配置ID
     */
    fun deleteApn(apnId: String)

    /**
     * 设置始终在线连接
     * @param enabled true=启用，false=禁用
     */
    fun setConnectionAlwaysOn(enabled: Boolean)

    /**
     * 检查功能是否受支持
     * @return true if this feature is supported on current device
     */
    fun isSupported(): Boolean = true
}
