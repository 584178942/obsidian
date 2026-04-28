package com.siyu.mdm.enterprise.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.siyu.mdm.enterprise.util.LogUtils

/**
 * 屏幕状态变化Receiver
 * 检测屏幕亮起/熄灭事件
 */
class ScreenStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ScreenStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                LogUtils.d(TAG, "屏幕亮起")
                // TODO: 处理屏幕亮起事件
            }
            Intent.ACTION_SCREEN_OFF -> {
                LogUtils.d(TAG, "屏幕熄灭")
                // TODO: 处理屏幕熄灭事件
            }
        }
    }
}
