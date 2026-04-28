package com.siyu.mdm.custom.device.util.screenoffalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.media.AudioAttributes
import android.provider.Settings
import android.os.Build.MANUFACTURER

object NotificationHelper {
    const val CHANNEL_ID = "ALERT_CHANNEL"
    private const val CHANNEL_NAME = "熄屏提醒通道"
    private const val CHANNEL_DESCRIPTION = "用于在熄屏状态下发出提醒的通知通道"

    fun createNotificationChannel(context: Context) {
        // 仅在Android O及以上版本需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 针对华为设备的特殊处理
            val importance = if (isHuaweiDevice()) {
                // 华为设备使用最高优先级
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_HIGH
            }
            
            // 创建通知渠道
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                
                // 针对华为设备优化震动模式
                if (isHuaweiDevice()) {
                    // 华为设备使用更快的震动模式，提高唤醒成功率
                    vibrationPattern = longArrayOf(50, 100, 150)
                } else {
                    vibrationPattern = longArrayOf(100, 200, 300)
                }
                
                enableVibration(true)
                // 绕过免打扰模式
                setBypassDnd(true)
                // 锁屏显示通知
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // 设置通知声音
                setSound(
                    Settings.System.DEFAULT_NOTIFICATION_URI,
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        // 华为设备使用ALARM类型声音以提高优先级
                        .setUsage(if (isHuaweiDevice()) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                // 允许指示灯
                enableLights(true)
            }

            // 注册通知渠道
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    // 检测是否为华为设备
    fun isHuaweiDevice(): Boolean {
        return MANUFACTURER.equals("huawei", ignoreCase = true) || 
               MANUFACTURER.equals("honor", ignoreCase = true)
    }
}
