package com.siyu.mdm.enterprise.util.mdm.xiaomi

import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.AppManager

/**
 * 小米应用管理器实现（占位）
 *
 * TODO: 待确认小米MDM SDK和权限
 */
class XiaomiAppManager : AppManager {

    private val TAG = "XiaomiAppManager"

    override fun install(apkPath: String) {
        LogUtils.w(TAG, "小米应用安装功能未实现")
        throw UnsupportedOperationException("小米应用安装功能待实现")
    }

    override fun uninstall(packageName: String) {
        LogUtils.w(TAG, "小米应用卸载功能未实现")
        throw UnsupportedOperationException("小米应用卸载功能待实现")
    }

    override fun setInstallBlacklist(packageNames: Set<String>) {
        LogUtils.w(TAG, "小米安装黑名单功能未实现")
        throw UnsupportedOperationException("小米安装黑名单功能待实现")
    }

    override fun setRuntimeBlacklist(packageNames: Set<String>) {
        LogUtils.w(TAG, "小米运行黑名单功能未实现")
        throw UnsupportedOperationException("小米运行黑名单功能待实现")
    }

    override fun canInstall(packageName: String): Boolean = false

    override fun canRun(packageName: String): Boolean = false

    override fun addInstallBlockList(packageNames: List<String>): Boolean = false

    override fun removeInstallBlockList(packageNames: List<String>): Boolean = false

    override fun getInstallBlockList(): List<String> = emptyList()

    override fun isSupported(): Boolean = false
}
