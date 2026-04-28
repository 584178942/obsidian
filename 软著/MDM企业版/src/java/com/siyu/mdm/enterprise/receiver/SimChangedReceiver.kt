package com.siyu.mdm.enterprise.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.service.MqttService

/**
 * SIM卡状态变化Receiver
 * 检测SIM卡变更，用于机卡绑定验证
 */
class SimChangedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SimChangedReceiver"
        private const val SIM_STATE_LOADED = "LOADED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val simState = intent.getStringExtra("ss") ?: return

        LogUtils.i(TAG, "SIM卡状态变化: $simState")

        if (simState == SIM_STATE_LOADED) {
            handleSimLoaded(context)
        }
    }

    private fun handleSimLoaded(context: Context) {
        LogUtils.i(TAG, "SIM卡已加载，开始机卡绑定验证")

        // 通知MqttService进行SIM验证
        val serviceIntent = Intent(context, MqttService::class.java).apply {
            action = MqttService.ACTION_SIM_STATE_CHANGED
            putExtra(MqttService.EXTRA_SIM_STATE, SIM_STATE_LOADED)
            putExtra(MqttService.EXTRA_TIMESTAMP, System.currentTimeMillis())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
