package com.siyu.mdm.enterprise.util.mdm.xiaomi

import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.SecurityManager

/**
 * 小米安全管理器实现（占位）
 *
 * TODO: 待确认小米MDM SDK和权限
 *
 * 小米设备管理功能需要:
 * 1. 确认小米 MDM SDK 包名
 * 2. 确认相关权限名称
 * 3. 确认支持的设备管理功能范围
 */
class XiaomiSecurityManager : SecurityManager {

    private val TAG = "XiaomiSecurityManager"

    override fun checkAdminPermission(): Boolean {
        LogUtils.w(TAG, "小米SecurityManager未实现，请检查权限配置")
        return false
    }

    override fun lock(mainText: String?, subText: String?) {
        LogUtils.w(TAG, "小米设备锁定功能未实现")
        throw UnsupportedOperationException("小米设备锁定功能待实现")
    }

    override fun unlock() {
        LogUtils.w(TAG, "小米设备解锁功能未实现")
        throw UnsupportedOperationException("小米设备解锁功能待实现")
    }

    override fun wipe() {
        LogUtils.w(TAG, "小米恢复出厂设置功能未实现")
        throw UnsupportedOperationException("小米恢复出厂设置功能待实现")
    }

    override fun resetPassword(newPassword: String) {
        LogUtils.w(TAG, "小米密码重置功能未实现")
        throw UnsupportedOperationException("小米密码重置功能待实现")
    }

    override fun isSupported(): Boolean = false
}
