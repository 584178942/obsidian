package com.siyu.mdm.enterprise.util.mdm

/**
 * 应用管理器接口
 * 定义应用安装、卸载、黑白名单管理等操作
 *
 * 各厂商实现类应实现此接口
 */
interface AppManager {

    /**
     * 安装应用
     * @param apkPath APK文件路径
     */
    fun install(apkPath: String)

    /**
     * 卸载应用
     * @param packageName 应用包名
     */
    fun uninstall(packageName: String)

    /**
     * 设置应用安装黑名单
     * @param packageNames 禁止安装的应用包名集合
     */
    fun setInstallBlacklist(packageNames: Set<String>)

    /**
     * 设置应用运行黑名单
     * @param packageNames 禁止运行的应用包名集合
     */
    fun setRuntimeBlacklist(packageNames: Set<String>)

    /**
     * 判断指定应用是否允许安装
     * @param packageName 应用包名
     * @return true if can install
     */
    fun canInstall(packageName: String): Boolean

    /**
     * 判断指定应用是否允许运行
     * @param packageName 应用包名
     * @return true if can run
     */
    fun canRun(packageName: String): Boolean

    /**
     * 添加安装阻止列表
     * @param packageNames 包名列表
     * @return true if success
     */
    fun addInstallBlockList(packageNames: List<String>): Boolean

    /**
     * 移除安装阻止列表
     * @param packageNames 包名列表
     * @return true if success
     */
    fun removeInstallBlockList(packageNames: List<String>): Boolean

    /**
     * 获取安装阻止列表
     * @return 被阻止安装的应用包名列表
     */
    fun getInstallBlockList(): List<String>

    /**
     * 检查功能是否受支持
     * @return true if this feature is supported on current device
     */
    fun isSupported(): Boolean = true
}
