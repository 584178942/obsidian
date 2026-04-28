package com.siyu.mdm.enterprise.util.mdm.vivo

import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.AppManager

/**
 * vivo应用管理器实现（占位）
 */
class VivoAppManager : AppManager {

    private val TAG = "VivoAppManager"

    override fun install(apkPath: String) {
        LogUtils.w(TAG, "vivo应用安装功能未实现")
        throw UnsupportedOperationException("vivo应用安装功能待实现")
    }

    override fun uninstall(packageName: String) {
        LogUtils.w(TAG, "vivo应用卸载功能未实现")
        throw UnsupportedOperationException("vivo应用卸载功能待实现")
    }

    override fun setInstallBlacklist(packageNames: Set<String>) {
        LogUtils.w(TAG, "vivo安装黑名单功能未实现")
        throw UnsupportedOperationException("vivo安装黑名单功能待实现")
    }

    override fun setRuntimeBlacklist(packageNames: Set<String>) {
        LogUtils.w(TAG, "vivo运行黑名单功能未实现")
        throw UnsupportedOperationException("vivo运行黑名单功能待实现")
    }

    override fun canInstall(packageName: String): Boolean = false

    override fun canRun(packageName: String): Boolean = false

    override fun addInstallBlockList(packageNames: List<String>): Boolean = false

    override fun removeInstallBlockList(packageNames: List<String>): Boolean = false

    override fun getInstallBlockList(): List<String> = emptyList()

    override fun isSupported(): Boolean = false
}
