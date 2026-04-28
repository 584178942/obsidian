package com.siyu.mdm.custom.device.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.service.MqttService
import com.siyu.mdm.custom.device.util.AppConstants.ACTION_MDM_INSTALL_RESULT

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action ?: return
        // 获取包名（系统广播通过Uri传递包名）
        val packageUri: Uri? = intent.data
        val packageName = packageUri?.schemeSpecificPart ?: "unknown_package"

        // 分别处理安装、卸载、替换安装事件
        when (action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                // 安装成功（注意：替换安装也会触发此Action，需结合PACKAGE_REPLACED判断）
                // isReplacing为true时表示是替换安装，false是全新安装
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!isReplacing) {
                    LogUtils.i("MdmBroadcastReceiver", "应用全新安装: $packageName")
                   // handleInstallSuccess(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // 卸载成功（EXTRA_DATA_REMOVED为true表示数据也被删除）
                val isDataRemoved = intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false)
                LogUtils.i("MdmBroadcastReceiver", "应用卸载: $packageName, 数据是否删除: $isDataRemoved")
               // handleUninstall(packageName, isDataRemoved)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                // 覆盖安装（替换安装）
                LogUtils.i("MdmBroadcastReceiver", "应用覆盖安装: $packageName")
                val intent2 = Intent(context, MqttService::class.java)
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent2)
                } else {
                    context.startService(intent2)
                }
            }
        }

    }
}
