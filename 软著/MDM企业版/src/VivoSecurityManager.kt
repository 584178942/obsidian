package com.siyu.mdm.enterprise.util.mdm.vivo

import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.SecurityManager

/**
 * vivo安全管理器实现（占位）
 *
 * TODO: 待确认vivo MDM SDK和权限
 */
class VivoSecurityManager : SecurityManager {

    private val TAG = "VivoSecurityManager"

    override fun checkAdminPermission(): Boolean {
        LogUtils.w(TAG, "vivo SecurityManager未实现，请检查权限配置")
        return false
    }

    override fun lock(mainText: String?, subText: String?) {
        LogUtils.w(TAG, "vivo设备锁定功能未实现")
        throw UnsupportedOperationException("vivo设备锁定功能待实现")
    }

    override fun unlock() {
        LogUtils.w(TAG, "vivo设备解锁功能未实现")
        throw UnsupportedOperationException("vivo设备解锁功能待实现")
    }

    override fun wipe() {
        LogUtils.w(TAG, "vivo恢复出厂设置功能未实现")
        throw UnsupportedOperationException("vivo恢复出厂设置功能待实现")
    }

    override fun resetPassword(newPassword: String) {
        LogUtils.w(TAG, "vivo密码重置功能未实现")
        throw UnsupportedOperationException("vivo密码重置功能待实现")
    }

    override fun isSupported(): Boolean = false
}
