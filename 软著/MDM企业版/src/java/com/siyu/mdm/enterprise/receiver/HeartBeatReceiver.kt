package com.siyu.mdm.enterprise.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.siyu.mdm.enterprise.util.LogUtils

/**
 * 心跳Receiver
 * 接收心跳广播并执行心跳任务
 */
class HeartBeatReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HeartBeatReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        LogUtils.i(TAG, "心跳任务触发")
        try {
            // TODO: 执行心跳请求
            LogUtils.i(TAG, "心跳任务执行完成")
        } catch (e: Exception) {
            LogUtils.e(TAG, "心跳任务执行失败", e)
        }
    }
}
