package com.siyu.mdm.enterprise.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.service.MqttService
import com.siyu.mdm.enterprise.util.AppConstants
import com.siyu.mdm.enterprise.util.StorageUtil
import java.util.Objects

/**
 * 开机自启Receiver
 * 设备启动后自动启动MQTT服务
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            LogUtils.i(TAG, "接收到系统开机广播，准备启动应用")

            // 启动MQTT服务
            val mqttIntent = Intent(context, MqttService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(mqttIntent)
            } else {
                context.startService(mqttIntent)
            }

            // 如果之前是锁定状态，恢复锁定
            if (Objects.equals(
                    StorageUtil.getString(AppConstants.KEY_LOCK_STATE, AppConstants.UN_LOCK),
                    AppConstants.LOCK
                )
            ) {
                LogUtils.i(TAG, "设备之前处于锁定状态，准备恢复锁定")
                // TODO: 调用安全管理器恢复锁定
            }

            LogUtils.i(TAG, "开机自启完成")
        }
    }
}
