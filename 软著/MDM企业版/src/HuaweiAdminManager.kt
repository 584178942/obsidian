package com.siyu.mdm.enterprise.util.mdm.huawei

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.siyu.mdm.enterprise.App
import com.siyu.mdm.enterprise.SampleDeviceReceiver
import com.siyu.mdm.enterprise.util.LogUtils


object HuaweiAdminManager {

    private const val TAG = "HuaweiAdminManager"

    private val context: Context = App.instance

    val adminComponentName: ComponentName by lazy {
        ComponentName(context, SampleDeviceReceiver::class.java)
    }

    private val devicePolicyManager: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    fun isDeviceAdmin(): Boolean {
        return try {
            devicePolicyManager.isAdminActive(adminComponentName)
        } catch (e: SecurityException) {
            LogUtils.e(TAG, "检查设备管理员权限失败: ${e.message}", e)
            false
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查设备管理员异常: ${e.message}", e)
            false
        }
    }

    fun isDeviceOwner(): Boolean {
        return try {
            devicePolicyManager.isDeviceOwnerApp(adminComponentName.packageName)
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查设备所有者失败: ${e.message}", e)
            false
        }
    }

    fun activateAdmin() {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "启用设备管理员以使用企业设备管理功能"
                )
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            LogUtils.i(TAG, "设备管理员激活请求已发送")
        } catch (e: Exception) {
            LogUtils.e(TAG, "激活设备管理员失败: ${e.message}", e)
        }
    }

    fun activateAdmin(description: String) {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, description)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            LogUtils.i(TAG, "设备管理员激活请求已发送: $description")
        } catch (e: Exception) {
            LogUtils.e(TAG, "激活设备管理员失败: ${e.message}", e)
        }
    }

    fun removeAdmin() {
        try {
            if (!isDeviceAdmin()) {
                LogUtils.i(TAG, "应用不是设备管理员，无需移除")
                return
            }

            // 尝试使用华为MDM API强制移除设备管理员
            val deviceControlManager = HuaweiMDMAbility.getDeviceControlManager()
            if (deviceControlManager != null) {
                try {
                    val result = deviceControlManager.removeActiveDeviceAdmin(adminComponentName)
                    if (result) {
                        LogUtils.i(TAG, "设备管理员移除成功（华为API）")
                    } else {
                        LogUtils.w(TAG, "华为API移除失败，尝试打开设置页面")
                        openAdminSettings()
                    }
                } catch (e: Exception) {
                    LogUtils.e(TAG, "华为API移除失败: ${e.message}", e)
                    openAdminSettings()
                }
            } else {
                // 华为API不可用，打开设置页面让用户手动移除
                LogUtils.w(TAG, "华为API不可用，打开设置页面")
                openAdminSettings()
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "移除设备管理员失败: ${e.message}", e)
            openAdminSettings()
        }
    }

    private fun openAdminSettings() {
        try {
            // 尝试打开设备管理员设置页面
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            LogUtils.i(TAG, "设备管理员设置页面已打开，请手动移除")
        } catch (e: Exception) {
            LogUtils.e(TAG, "打开设置页面失败: ${e.message}", e)
        }
    }

    fun activateWithLicense(licenseInfo: String) {
        try {
            LogUtils.i(TAG, "尝试通过License激活华为MDM")
            LogUtils.w(TAG, "License激活需要集成HEM SDK")
            LogUtils.w(TAG, "建议使用标准设备管理员激活流程")
            activateAdmin("启用设备管理员以使用企业设备管理功能")
        } catch (e: Exception) {
            LogUtils.e(TAG, "License激活失败: ${e.message}", e)
            activateAdmin("启用设备管理员以使用企业设备管理功能")
        }
    }

    fun getAdminStatus(): Map<String, Any> {
        return try {
            mapOf(
                "isAdminActive" to isDeviceAdmin(),
                "isDeviceOwner" to isDeviceOwner(),
                "adminPackage" to adminComponentName.packageName,
                "adminClass" to adminComponentName.className,
                "mdmKitVersion" to getMDMKitVersion()
            )
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取管理员状态失败: ${e.message}", e)
            emptyMap()
        }
    }

    fun getMDMKitVersion(): String {
        return try {
            val clazz = Class.forName("com.huawei.mdm.util.MDMVersion")
            val method = clazz.getMethod("getVersion")
            method.invoke(null) as? String ?: "未知"
        } catch (e: Exception) {
            "未集成或版本获取失败"
        }
    }

    fun getDiagnosticReport(): String {
        val report = StringBuilder()
        report.appendLine("=== 华为设备管理员诊断报告 ===")
        report.appendLine("设备管理员: ${if (isDeviceAdmin()) "✅ 已激活" else "❌ 未激活"}")
        report.appendLine("设备所有者: ${if (isDeviceOwner()) "✅ 是" else "❌ 否"}")
        report.appendLine("组件: ${adminComponentName.packageName}")
        report.appendLine("MDM Kit版本: ${getMDMKitVersion()}")
        report.appendLine()
        report.appendLine("华为API服务:")
        report.appendLine("- DevicePolicyManager: ✅")
        report.appendLine("==================================")
        return report.toString()
    }

    fun initCheck() {
        LogUtils.i(TAG, "=== 华为MDM初始化检查 ===")
        LogUtils.i(TAG, getDiagnosticReport())

        if (!isDeviceAdmin()) {
            LogUtils.w(TAG, "应用未激活设备管理员，部分功能将不可用")
            LogUtils.i(TAG, "建议调用 HuaweiAdminManager.activateAdmin() 引导用户激活")
        } else {
            LogUtils.i(TAG, "设备管理员已激活，所有功能可用")

            if (!isDeviceOwner()) {
                LogUtils.w(TAG, "应用仅为设备管理员（非所有者），部分功能需要所有者权限")
            }
        }
    }
}
