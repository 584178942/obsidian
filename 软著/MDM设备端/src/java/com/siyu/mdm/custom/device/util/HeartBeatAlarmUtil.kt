package com.siyu.mdm.custom.device.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.App
import com.siyu.mdm.custom.device.receiver.HeartBeatReceiver
import com.siyu.mdm.custom.device.receiver.ScreenStateReceiver.Companion.mScreenPowerStatus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object HeartBeatAlarmUtil {

    private const val TAG = "HeartBeatAlarmUtil"
    private const val HEART_BEAT_REQUEST_CODE = 999

    /**
     * 启动心跳闹钟
     */
    @SuppressLint("ScheduleExactAlarm")
    fun startHeartBeatAlarm(isNetwork: Boolean) {
        val context = App.instance
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: run {
                LogUtils.i(TAG, "Failed to get AlarmManager service")
                return
            }

        val pendingIntent = createPendingIntent(context)

        // 计算下一次触发的时间间隔
        val intervalMillis = calculateIntervalMillis(isNetwork)
        val triggerTime = System.currentTimeMillis() + intervalMillis

        logNextTriggerTime(triggerTime)

        // 设置精确闹钟
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    /**
     * 创建PendingIntent
     */
    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent().apply {
            component = ComponentName(context.packageName, HeartBeatReceiver::class.java.name)
        }
        return PendingIntent.getBroadcast(
            context,
            HEART_BEAT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 计算心跳间隔时间（毫秒）
     */
    private fun calculateIntervalMillis(isNetwork: Boolean): Long {
        val baseInterval = if (!mScreenPowerStatus) {
            getRandomInterval(
                min = AppConstants.DARK_SCREEN_MIN,
                max = AppConstants.DARK_SCREEN_MAX,
                default = AppConstants.SPACE_SECOND
            )
        } else {
            getRandomInterval(
                min = AppConstants.BRIGHT_SCREEN_MIN,
                max = AppConstants.BRIGHT_SCREEN_MAX,
                default = AppConstants.SPACE_SECOND
            )
        }
        return TimeUnit.SECONDS.toMillis(baseInterval.toLong())
    }

    /**
     * 获取随机时间间隔
     */
    private fun getRandomInterval(min: Int, max: Int, default: Int): Int {
        return if (max - min == 0) {
            default
        } else {
            Random.nextInt(min, max)
        }
    }

    /**
     * 打印下次触发时间
     */
    private fun logNextTriggerTime(triggerTime: Long) {
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
        val formattedTime = format.format(Date(triggerTime))
        LogUtils.i(TAG, "Next heartbeat scheduled at: $formattedTime")
    }
}