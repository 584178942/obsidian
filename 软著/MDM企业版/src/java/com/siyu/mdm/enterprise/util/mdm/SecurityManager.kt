package com.siyu.mdm.enterprise.util.mdm

/**
 * 设备安全管理器接口
 * 定义设备安全相关的操作，如锁定、解锁、恢复出厂设置等
 *
 * 各厂商实现类应实现此接口
 */
interface SecurityManager {

    /**
     * 检查是否具有设备管理员权限
     * @return true if has admin permission
     */
    fun checkAdminPermission(): Boolean

    /**
     * 锁定设备
     * @param mainText 锁屏界面主文本（可选）
     * @param subText 锁屏界面副文本（可选）
     */
    fun lock(mainText: String? = null, subText: String? = null)

    /**
     * 解锁设备
     * 退出单应用锁定模式
     */
    fun unlock()

    /**
     * 恢复出厂设置
     * 警告：此操作会清除设备上的所有数据
     */
    fun wipe()

    /**
     * 重置设备密码
     * @param newPassword 新密码
     */
    fun resetPassword(newPassword: String)

    /**
     * 检查功能是否受支持
     * @return true if this feature is supported on current device
     */
    fun isSupported(): Boolean = true
}
