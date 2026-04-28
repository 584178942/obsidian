package com.siyu.mdm.enterprise.util.mdm.huawei

import android.content.ComponentName
import android.content.Context
import com.siyu.mdm.enterprise.App
import com.siyu.mdm.enterprise.SampleDeviceReceiver
import com.siyu.mdm.enterprise.util.LogUtils

class HuaweiBluetoothManager {

    private val TAG = "HuaweiBluetoothManager"

    private val context: Context = App.instance

    private val adminComponentName by lazy {
        ComponentName(context, SampleDeviceReceiver::class.java)
    }

    private val deviceBluetoothManager by lazy {
        HuaweiMDMAbility.getDeviceBluetoothManager()
    }

    private fun checkAdminPermission(): Boolean {
        return HuaweiMDMAbility.isDeviceAdmin()
    }

    fun addBluetoothDevicesToBlockList(devices: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法添加蓝牙黑名单")
                return
            }

            deviceBluetoothManager?.let {
                val result = it.addBluetoothDevicesToBlockList(adminComponentName, ArrayList(devices))
                if (result) {
                    LogUtils.i(TAG, "蓝牙黑名单添加成功: $devices")
                } else {
                    LogUtils.w(TAG, "蓝牙黑名单添加失败")
                }
            } ?: LogUtils.w(TAG, "DeviceBluetoothManager不可用，无法添加蓝牙黑名单")
        } catch (e: Exception) {
            LogUtils.e(TAG, "添加蓝牙黑名单失败: ${e.message}", e)
        }
    }

    fun addBluetoothDevicesToTrustList(devices: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法添加蓝牙白名单")
                return
            }

            deviceBluetoothManager?.let {
                val result = it.addBluetoothDevicesToTrustList(adminComponentName, ArrayList(devices))
                if (result) {
                    LogUtils.i(TAG, "蓝牙白名单添加成功: $devices")
                } else {
                    LogUtils.w(TAG, "蓝牙白名单添加失败")
                }
            } ?: LogUtils.w(TAG, "DeviceBluetoothManager不可用，无法添加蓝牙白名单")
        } catch (e: Exception) {
            LogUtils.e(TAG, "添加蓝牙白名单失败: ${e.message}", e)
        }
    }

    fun removeBluetoothDevicesFromBlockList(devices: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法从蓝牙黑名单移除")
                return
            }

            deviceBluetoothManager?.let {
                val result = it.removeBluetoothDevicesFromBlockList(adminComponentName, ArrayList(devices))
                if (result) {
                    LogUtils.i(TAG, "蓝牙黑名单移除成功: $devices")
                } else {
                    LogUtils.w(TAG, "蓝牙黑名单移除失败")
                }
            } ?: LogUtils.w(TAG, "DeviceBluetoothManager不可用，无法从蓝牙黑名单移除")
        } catch (e: Exception) {
            LogUtils.e(TAG, "从蓝牙黑名单移除失败: ${e.message}", e)
        }
    }

    fun removeBluetoothDevicesFromTrustList(devices: List<String>) {
        try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法从蓝牙白名单移除")
                return
            }

            deviceBluetoothManager?.let {
                val result = it.removeBluetoothDevicesFromTrustList(adminComponentName, ArrayList(devices))
                if (result) {
                    LogUtils.i(TAG, "蓝牙白名单移除成功: $devices")
                } else {
                    LogUtils.w(TAG, "蓝牙白名单移除失败")
                }
            } ?: LogUtils.w(TAG, "DeviceBluetoothManager不可用，无法从蓝牙白名单移除")
        } catch (e: Exception) {
            LogUtils.e(TAG, "从蓝牙白名单移除失败: ${e.message}", e)
        }
    }

    fun getBluetoothDevicesFromBlockList(): List<String> {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法获取蓝牙黑名单")
                return emptyList()
            }

            deviceBluetoothManager?.let {
                val list = it.getBluetoothDevicesFromBlockList(adminComponentName)
                list?.toList() ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取蓝牙黑名单失败: ${e.message}", e)
            emptyList()
        }
    }

    fun getBluetoothDevicesFromTrustList(): List<String> {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法获取蓝牙白名单")
                return emptyList()
            }

            deviceBluetoothManager?.let {
                val list = it.getBluetoothDevicesFromTrustList(adminComponentName)
                list?.toList() ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取蓝牙白名单失败: ${e.message}", e)
            emptyList()
        }
    }

    fun isDeviceInBlockList(deviceAddress: String): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法查询蓝牙黑名单")
                return false
            }

            deviceBluetoothManager?.let {
                it.isDeviceInBlockList(adminComponentName, deviceAddress)
            } ?: false
        } catch (e: Exception) {
            LogUtils.e(TAG, "查询蓝牙黑名单失败: ${e.message}", e)
            false
        }
    }

    fun isDeviceInTrustList(deviceAddress: String): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法查询蓝牙白名单")
                return false
            }

            deviceBluetoothManager?.let {
                it.isDeviceInTrustList(adminComponentName, deviceAddress)
            } ?: false
        } catch (e: Exception) {
            LogUtils.e(TAG, "查询蓝牙白名单失败: ${e.message}", e)
            false
        }
    }

    fun setBluetoothDataTransferDisabled(disabled: Boolean): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制蓝牙文件传输")
                return false
            }

            deviceBluetoothManager?.let {
                val result = it.setBluetoothDataTransferDisable(adminComponentName, disabled)
                if (result) {
                    LogUtils.i(TAG, "蓝牙文件传输${if (disabled) "禁用" else "启用"}成功")
                } else {
                    LogUtils.w(TAG, "蓝牙文件传输设置失败")
                }
                result
            } ?: run {
                LogUtils.w(TAG, "DeviceBluetoothManager不可用，无法控制蓝牙文件传输")
                false
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "控制蓝牙文件传输失败: ${e.message}", e)
            false
        }
    }

    fun setBluetoothPairingDisabled(disabled: Boolean): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制蓝牙配对")
                return false
            }

            deviceBluetoothManager?.let {
                val result = it.setBluetoothPairingDisable(adminComponentName, disabled)
                if (result) {
                    LogUtils.i(TAG, "蓝牙配对${if (disabled) "禁用" else "启用"}成功")
                } else {
                    LogUtils.w(TAG, "蓝牙配对设置失败")
                }
                result
            } ?: run {
                LogUtils.w(TAG, "DeviceBluetoothManager不可用，无法控制蓝牙配对")
                false
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "控制蓝牙配对失败: ${e.message}", e)
            false
        }
    }

    fun setDiscoverableDisabled(disabled: Boolean): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制蓝牙可被发现模式")
                return false
            }

            deviceBluetoothManager?.let {
                val result = it.setDiscoverableDisabled(adminComponentName, disabled)
                if (result) {
                    LogUtils.i(TAG, "蓝牙可被发现模式${if (disabled) "禁用" else "启用"}成功")
                } else {
                    LogUtils.w(TAG, "蓝牙可被发现模式设置失败")
                }
                result
            } ?: run {
                LogUtils.w(TAG, "DeviceBluetoothManager不可用，无法控制蓝牙可被发现模式")
                false
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "控制蓝牙可被发现模式失败: ${e.message}", e)
            false
        }
    }

    fun setLimitedDiscoverableDisabled(disabled: Boolean): Boolean {
        return try {
            if (!checkAdminPermission()) {
                LogUtils.e(TAG, "应用未激活设备管理员，无法控制蓝牙有限可被发现模式")
                return false
            }

            deviceBluetoothManager?.let {
                val result = it.setLimitedDiscoverableDisabled(adminComponentName, disabled)
                if (result) {
                    LogUtils.i(TAG, "蓝牙有限可被发现模式${if (disabled) "禁用" else "启用"}成功")
                } else {
                    LogUtils.w(TAG, "蓝牙有限可被发现模式设置失败")
                }
                result
            } ?: run {
                LogUtils.w(TAG, "DeviceBluetoothManager不可用，无法控制蓝牙有限可被发现模式")
                false
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "控制蓝牙有限可被发现模式失败: ${e.message}", e)
            false
        }
    }

    fun getBluetoothDiagnosticReport(): String {
        val report = StringBuilder()
        report.appendLine("=== 华为蓝牙管理诊断报告 ===")
        report.appendLine("管理员权限: ${if (checkAdminPermission()) "✅ 已激活" else "❌ 未激活"}")
        report.appendLine("DeviceBluetoothManager: ${if (deviceBluetoothManager != null) "✅ 可用" else "❌ 不可用"}")
        report.appendLine("蓝牙黑名单: ${getBluetoothDevicesFromBlockList().size} 个设备")
        report.appendLine("蓝牙白名单: ${getBluetoothDevicesFromTrustList().size} 个设备")
        report.appendLine("=================================")
        return report.toString()
    }
}
