package com.siyu.mdm.custom.device.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.util.screenoffalert.AlertService

class ScreenStateReceiver : BroadcastReceiver() {
    private val handler = Handler(Looper.getMainLooper())
    // 移除震动任务，因为现在只能通过MQTT命令启动震动提醒
    
    private var context: Context? = null

    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                LogUtils.i("ScreenState", "屏幕亮起")
                mScreenPowerStatus = true
                // 清除可能的延迟任务
                handler.removeCallbacksAndMessages(null)
            }
            Intent.ACTION_SCREEN_OFF -> {
                LogUtils.i("ScreenState", "屏幕熄灭")
                mScreenPowerStatus = false
                // 不再自动启动震动服务，只能通过MQTT命令启动
            }
        }
    }
    
    companion object {
        var mScreenPowerStatus: Boolean = true
    }
}

