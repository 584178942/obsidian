package com.siyu.mdm.enterprise.util.mdm.huawei

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.huawei.android.app.admin.DeviceApplicationManager
import com.huawei.android.app.admin.DevicePackageManager
import com.siyu.mdm.enterprise.App
import com.siyu.mdm.enterprise.SampleDeviceReceiver
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.AppManager

class HuaweiAppManager : AppManager {

    private val TAG = "HuaweiAppManager"

    private val context: Context = App.instance

    private val adminComponentName by lazy {
        android.content.ComponentName(context, SampleDeviceReceiver::class.java)
    }

    private val deviceApplicationManager by lazy {
        HuaweiMDMAbility.getDeviceApplicationManager()
    }

    private val devicePackageManager by lazy {
        HuaweiMDMAbility.getDevicePackageManager()
    }

    private fun checkAdminPermission(): Boolean {
        return HuaweiMDMAbility.isDeviceAdmin()
    }

    override fun install(apkPath: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法安装应用")
                return
            }

            try {
                devicePackageManager?.let {
                    it.installPackage(adminComponentName, apkPath)
                    LogUtils.i(TAG, "应用静默安装成功: $apkPath")
                } ?: run {
                    installWithNativeAPI(apkPath)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "静默安装失败: ${e.message}", e)
                installWithNativeAPI(apkPath)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "安装应用异常: ${e.message}", e)
        }
    }

    private fun installWithNativeAPI(apkPath: String) {
        try {
            val uri = Uri.parse(apkPath)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LogUtils.i(TAG, "应用安装请求已发送: $apkPath")
        } catch (e: Exception) {
            LogUtils.e(TAG, "原生API安装应用失败: ${e.message}", e)
        }
    }

    override fun uninstall(packageName: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法卸载应用")
                return
            }

            try {
                devicePackageManager?.let {
                    it.uninstallPackage(adminComponentName, packageName, false)
                    LogUtils.i(TAG, "应用静默卸载成功: $packageName")
                } ?: run {
                    uninstallWithNativeAPI(packageName)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "静默卸载失败: ${e.message}", e)
                uninstallWithNativeAPI(packageName)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "卸载应用异常: ${e.message}", e)
        }
    }

    private fun uninstallWithNativeAPI(packageName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                LogUtils.i(TAG, "应用卸载请求已发送: $packageName")
            } else {
                LogUtils.w(TAG, "原生API卸载应用需要Android 5.0+")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "原生API卸载应用失败: ${e.message}", e)
        }
    }

    override fun setInstallBlacklist(packageNames: Set<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置安装黑名单")
                return
            }

            try {
                deviceApplicationManager?.let {
                    val result = it.addInstallPackageBlockList(adminComponentName, ArrayList(packageNames.toList()))
                    if (result) {
                        LogUtils.i(TAG, "安装黑名单设置成功: $packageNames")
                    } else {
                        LogUtils.w(TAG, "安装黑名单设置失败")
                    }
                } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法设置安装黑名单")
            } catch (e: Exception) {
                LogUtils.e(TAG, "设置安装黑名单失败: ${e.message}", e)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置安装黑名单异常: ${e.message}", e)
        }
    }

    override fun setRuntimeBlacklist(packageNames: Set<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置运行黑名单")
                return
            }

            try {
                deviceApplicationManager?.let {
                    it.addDisallowedRunningApp(adminComponentName, ArrayList(packageNames.toList()))
                    LogUtils.i(TAG, "运行黑名单设置成功: $packageNames")
                } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法设置运行黑名单")
            } catch (e: Exception) {
                LogUtils.e(TAG, "设置运行黑名单失败: ${e.message}", e)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置运行黑名单异常: ${e.message}", e)
        }
    }

    override fun canInstall(packageName: String): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法检查安装权限")
                return false
            }

            val blacklist = getInstallBlockList()
            !blacklist.contains(packageName)
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查安装权限失败: ${e.message}", e)
            false
        }
    }

    override fun canRun(packageName: String): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法检查运行权限")
                return true
            }

            val blacklist = getRuntimeBlockList()
            !blacklist.contains(packageName)
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查运行权限失败: ${e.message}", e)
            true
        }
    }

    override fun addInstallBlockList(packageNames: List<String>): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法添加安装阻止列表")
                return false
            }

            deviceApplicationManager?.let {
                val result = it.addInstallPackageBlockList(adminComponentName, ArrayList(packageNames))
                if (result) {
                    LogUtils.i(TAG, "安装阻止列表添加成功: $packageNames")
                }
                result
            } ?: false
        } catch (e: Exception) {
            LogUtils.e(TAG, "添加安装阻止列表失败: ${e.message}", e)
            false
        }
    }

    override fun removeInstallBlockList(packageNames: List<String>): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法移除安装阻止列表")
                return false
            }

            deviceApplicationManager?.let {
                val result = it.removeInstallPackageBlockList(adminComponentName, ArrayList(packageNames))
                if (result) {
                    LogUtils.i(TAG, "安装阻止列表移除成功: $packageNames")
                }
                result
            } ?: false
        } catch (e: Exception) {
            LogUtils.e(TAG, "移除安装阻止列表失败: ${e.message}", e)
            false
        }
    }

    override fun getInstallBlockList(): List<String> {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法获取安装阻止列表")
                return emptyList()
            }

            deviceApplicationManager?.let {
                val list = it.getInstallPackageBlockList(adminComponentName)
                list?.toList() ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取安装阻止列表失败: ${e.message}", e)
            emptyList()
        }
    }

    private fun getRuntimeBlockList(): List<String> {
        return try {
            deviceApplicationManager?.let {
                val list = it.getDisallowedRunningApp(adminComponentName)
                list?.toList() ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取运行阻止列表失败: ${e.message}", e)
            emptyList()
        }
    }

    fun addPersistentApp(packageNames: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法添加持久化应用")
                return
            }

            deviceApplicationManager?.let {
                it.addPersistentApp(adminComponentName, packageNames)
                LogUtils.i(TAG, "持久化应用添加成功: $packageNames")
            } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法添加持久化应用")
        } catch (e: Exception) {
            LogUtils.e(TAG, "添加持久化应用失败: ${e.message}", e)
        }
    }

    fun removePersistentApp(packageNames: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法移除持久化应用")
                return
            }

            deviceApplicationManager?.let {
                it.removePersistentApp(adminComponentName, packageNames)
                LogUtils.i(TAG, "持久化应用移除成功: $packageNames")
            } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法移除持久化应用")
        } catch (e: Exception) {
            LogUtils.e(TAG, "移除持久化应用失败: ${e.message}", e)
        }
    }

    fun getPersistentAppList(): List<String> {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法获取持久化应用列表")
                return emptyList()
            }

            deviceApplicationManager?.let {
                val list = it.getPersistentApp(adminComponentName)
                list?.toList() ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取持久化应用列表失败: ${e.message}", e)
            emptyList()
        }
    }

    fun setSingleApp(packageName: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置独占应用")
                return
            }

            deviceApplicationManager?.let {
                val result = it.addSingleApp(adminComponentName, packageName)
                if (result) {
                    LogUtils.i(TAG, "独占应用设置成功: $packageName")
                } else {
                    LogUtils.w(TAG, "独占应用设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法设置独占应用")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置独占应用失败: ${e.message}", e)
        }
    }

    fun clearSingleApp() {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法清除独占应用")
                return
            }

            deviceApplicationManager?.let {
                val currentApp = it.getSingleApp(adminComponentName)
                if (currentApp != null) {
                    it.clearSingleApp(adminComponentName, currentApp)
                    LogUtils.i(TAG, "独占应用已清除: $currentApp")
                } else {
                    LogUtils.i(TAG, "当前无独占应用")
                }
            } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法清除独占应用")
        } catch (e: Exception) {
            LogUtils.e(TAG, "清除独占应用失败: ${e.message}", e)
        }
    }

    fun getCurrentSingleApp(): String? {
        return try {
            deviceApplicationManager?.getSingleApp(adminComponentName)
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取当前独占应用失败: ${e.message}", e)
            null
        }
    }

    fun setTaskLockAppList(packageNames: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置任务锁应用列表")
                return
            }

            deviceApplicationManager?.let {
                val result = it.setTaskLockAppList(adminComponentName, ArrayList(packageNames))
                if (result) {
                    LogUtils.i(TAG, "任务锁应用列表设置成功: $packageNames")
                } else {
                    LogUtils.w(TAG, "任务锁应用列表设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法设置任务锁应用列表")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置任务锁应用列表失败: ${e.message}", e)
        }
    }

    fun getTaskLockAppList(): List<String> {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法获取任务锁应用列表")
                return emptyList()
            }

            deviceApplicationManager?.let {
                val list = it.getTaskLockAppList(adminComponentName)
                list?.toList() ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取任务锁应用列表失败: ${e.message}", e)
            emptyList()
        }
    }

    fun killApplicationProcess(packageName: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法停止应用进程")
                return
            }

            deviceApplicationManager?.let {
                it.killApplicationProcess(adminComponentName, packageName)
                LogUtils.i(TAG, "应用进程已停止: $packageName")
            } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法停止应用进程")
        } catch (e: Exception) {
            LogUtils.e(TAG, "停止应用进程失败: ${e.message}", e)
        }
    }

    fun clearPackageData(packageName: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法清除应用数据")
                return
            }

            devicePackageManager?.let {
                it.clearPackageData(adminComponentName, packageName)
                LogUtils.i(TAG, "应用数据已清除: $packageName")
            } ?: LogUtils.w(TAG, "DevicePackageManager不可用，无法清除应用数据")
        } catch (e: Exception) {
            LogUtils.e(TAG, "清除应用数据失败: ${e.message}", e)
        }
    }

    fun clearPackageCacheData(packageName: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法清除应用缓存")
                return
            }

            deviceApplicationManager?.let {
                it.clearPackageCacheData(adminComponentName, packageName)
                LogUtils.i(TAG, "应用缓存已清除: $packageName")
            } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法清除应用缓存")
        } catch (e: Exception) {
            LogUtils.e(TAG, "清除应用缓存失败: ${e.message}", e)
        }
    }

    fun setComponentLaunchedByLauncher(componentName: String) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法设置开机拉起组件")
                return
            }

            deviceApplicationManager?.let {
                val result = it.setComponentLaunchedByLauncher(adminComponentName, componentName)
                if (result) {
                    LogUtils.i(TAG, "开机拉起组件设置成功: $componentName")
                } else {
                    LogUtils.w(TAG, "开机拉起组件设置失败")
                }
            } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法设置开机拉起组件")
        } catch (e: Exception) {
            LogUtils.e(TAG, "设置开机拉起组件失败: ${e.message}", e)
        }
    }

    fun getComponentLaunchedByLauncher(): String? {
        return try {
            deviceApplicationManager?.getComponentLaunchedByLauncher(adminComponentName)
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取开机拉起组件失败: ${e.message}", e)
            null
        }
    }

    fun clearComponentLaunchedByLauncher() {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法清除开机拉起组件")
                return
            }

            deviceApplicationManager?.let {
                val result = it.clearComponentLaunchedByLauncher(adminComponentName)
                if (result) {
                    LogUtils.i(TAG, "开机拉起组件已清除")
                } else {
                    LogUtils.w(TAG, "开机拉起组件清除失败")
                }
            } ?: LogUtils.w(TAG, "DeviceApplicationManager不可用，无法清除开机拉起组件")
        } catch (e: Exception) {
            LogUtils.e(TAG, "清除开机拉起组件失败: ${e.message}", e)
        }
    }

    fun getTopAppPackageName(): String? {
        return try {
            deviceApplicationManager?.getTopAppPackageName(adminComponentName)
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取顶层应用包名失败: ${e.message}", e)
            null
        }
    }

    override fun isSupported(): Boolean {
        return checkAdminPermission()
    }

    fun getAppDiagnosticReport(): String {
        val report = StringBuilder()
        report.appendLine("=== 华为应用管理诊断报告 ===")
        report.appendLine("管理员权限: ${if (checkAdminPermission()) "✅ 已激活" else "❌ 未激活"}")
        report.appendLine()
        report.appendLine("【DeviceApplicationManager】")
        report.appendLine("- 实例: ${if (deviceApplicationManager != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("- 持久化应用: ${getPersistentAppList().size} 个")
        report.appendLine("- 安装阻止列表: ${getInstallBlockList().size} 个")
        report.appendLine("- 运行阻止列表: ${getRuntimeBlockList().size} 个")
        report.appendLine("- 任务锁应用: ${getTaskLockAppList().size} 个")
        report.appendLine("- 当前独占应用: ${getCurrentSingleApp() ?: "无"}")
        report.appendLine("- 顶层应用: ${getTopAppPackageName() ?: "未知"}")
        report.appendLine()
        report.appendLine("【DevicePackageManager】")
        report.appendLine("- 实例: ${if (devicePackageManager != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("=================================")
        return report.toString()
    }
}
