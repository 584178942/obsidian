package com.siyu.mdm.enterprise.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.service.MqttService

/**
 * 应用安装/卸载广播接收器
 *
 * 监听以下事件：
 * - PACKAGE_ADDED：应用安装（全新安装）
 * - PACKAGE_REPLACED：应用覆盖安装（更新）
 * - PACKAGE_REMOVED：应用卸载
 *
 * 注意：
 * - PACKAGE_REPLACED 也会触发 PACKAGE_ADDED，需通过 EXTRA_REPLACING 区分
 * - 卸载带数据删除时，会同时触发 PACKAGE_REMOVED，可通过 EXTRA_DATA_REMOVED 判断
 */
class InstallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "InstallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action ?: return
        val packageUri = intent.data
        val packageName = packageUri?.schemeSpecificPart ?: "unknown"

        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> handlePackageAdded(context, intent, packageName)
            Intent.ACTION_PACKAGE_REMOVED -> handlePackageRemoved(intent, packageName)
            Intent.ACTION_PACKAGE_REPLACED -> handlePackageReplaced(context, packageName)
        }
    }

    /**
     * 处理应用安装
     * @param isReplacing 是否为覆盖安装（true=更新，false=全新安装）
     */
    private fun handlePackageAdded(context: Context, intent: Intent, packageName: String) {
        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        when {
            isReplacing -> {
                // 覆盖安装不处理，在 PACKAGE_REPLACED 中统一处理
            }
            else -> {
                // 全新安装
                LogUtils.i(TAG, "应用全新安装: $packageName")
                // TODO: 通知服务器应用安装成功
            }
        }
    }

    /**
     * 处理应用卸载
     * @param isDataRemoved 是否同时删除了应用数据
     */
    private fun handlePackageRemoved(intent: Intent, packageName: String) {
        val isDataRemoved = intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false)

        LogUtils.i(TAG, "应用卸载: $packageName, 删除数据: $isDataRemoved")

        // TODO: 根据业务需求处理
        // 例如：如果是系统预装应用被卸载，可能需要重新安装
    }

    /**
     * 处理应用覆盖安装（更新）
     */
    private fun handlePackageReplaced(context: Context, packageName: String) {
        LogUtils.i(TAG, "应用覆盖安装(更新): $packageName")

        // 覆盖安装后可能需要重启相关服务
        val serviceIntent = Intent(context, MqttService::class.java).apply {
            action = MqttService.ACTION_APP_UPDATED
            putExtra(MqttService.EXTRA_PACKAGE_NAME, packageName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // TODO: 通知服务器应用更新成功
    }
}
