package com.siyu.mdm.custom.device.util.screenoffalert

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.R

class AlertService : Service() {
    // 定义操作常量
    companion object {
        const val ACTION_STOP = "com.siyu.mdm.action.STOP"
        private const val NOTIFICATION_ID = 1
        
        // 定义启动来源常量
        const val SOURCE_SCREEN_OFF = "screenOff"
        const val SOURCE_BUTTON_CLICK = "buttonClick"
    }
    
    private val binder = LocalBinder()
    private var isVibrating = false
    private var vibrator: Vibrator? = null
    private val vibrationPattern = longArrayOf(300, 200, 500) // 震动模式: 等待100ms, 震动200ms, 等待300ms
    private var wakeLock: PowerManager.WakeLock? = null
    private var screenWakeLock: PowerManager.WakeLock? = null // 用于唤醒屏幕的唤醒锁
    private var currentStartSource: String? = null // 记录当前服务启动来源
    
    inner class LocalBinder : Binder() {
        val service: AlertService
            get() = this@AlertService
    }

    override fun onCreate() {
        super.onCreate()
        // 初始化震动器
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        
        // 获取PowerManager实例
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        
        //
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ScreenOffAlert::WakeLock"
        )
        wakeLock?.acquire(10*60*1000L /*10 minutes*/)
        
        // 获取FULL_WAKE_LOCK或ACQUIRE_CAUSES_WAKEUP，用于唤醒屏幕
        // 根据不同Android版本使用不同的标志位
        val screenWakeLockFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_BRIGHT_WAKE_LOCK
        } else {
            @Suppress("DEPRECATION")
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
        }
        
        screenWakeLock = powerManager.newWakeLock(
            screenWakeLockFlags,
            "ScreenOffAlert::ScreenWakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 检查是否是停止命令
        if (intent?.action == ACTION_STOP) {
            stopVibration()
            stopSelf()
            return START_NOT_STICKY
        }
        
        // 记录启动来源
        val startSource = intent?.getStringExtra("startSource")
        if (startSource != null) {
            currentStartSource = startSource
            LogUtils.d("AlertService", "服务启动来源: $startSource")
        }
        
        // 创建通知渠道
        NotificationHelper.createNotificationChannel(this)
        
        // 创建通知点击意图 - 修改为跳转到微信发消息页面
        val notificationIntent = Intent()
        notificationIntent.apply {
            // 微信的包名
            setPackage("com.tencent.mm")
            // 微信主界面活动
            action = "android.intent.action.MAIN"
            addCategory("android.intent.category.LAUNCHER")
            // 添加标志让微信打开到发消息界面
            putExtra("LauncherUI.From.Scaner.Shortcut", true)
            // 确保在新任务中启动
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // 创建停止按钮意图
        val stopIntent = Intent(this, AlertService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 构建通知
        val notification: Notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("熄屏提醒")
            .setContentText("提醒功能正在运行中...")
            .setSmallIcon(R.drawable.ic_alert)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_alert,
                "停止提醒",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        // 启动前台服务
        startForeground(NOTIFICATION_ID, notification)
        
        // 重置震动状态并强制重新启动震动，确保每次启动服务都能正确震动
        resetVibrationState()
        startVibration()
        
        return START_STICKY
    }
    
    /**
     * 重置震动状态，确保重新启动服务时能正确震动
     */
    private fun resetVibrationState() {
        // 确保震动状态为false
        isVibrating = false
        
        // 重新初始化震动器，确保它是有效的
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            LogUtils.d("AlertService", "震动器已重新初始化")
        } catch (e: Exception) {
            LogUtils.e("AlertService", "震动器重新初始化失败: ${e.message}")
        }
    }
    private fun startVibration() {
        if (!isVibrating) {
            isVibrating = true
            
            // 添加亮屏功能
            try {
                LogUtils.d("AlertService", "尝试唤醒屏幕")
                if (screenWakeLock?.isHeld == false) {
                    screenWakeLock?.acquire(5000L) // 亮屏5秒，避免长时间亮屏耗电
                }
            } catch (e: Exception) {
                LogUtils.e("AlertService", "唤醒屏幕失败: ${e.message}")
            }
            
            // 检查震动器是否可用
            if (vibrator?.hasVibrator() == true) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // API 26及以上版本使用VibrationEffect
                        val effect = VibrationEffect.createWaveform(
                            vibrationPattern,
                            0 // 重复震动模式
                        )
                        // 针对华为设备的特殊处理
                        if (isHuaweiDevice()) {
                            // 华为设备可能需要额外的权限或特殊处理
                            // 尝试使用不同的震动模式
                            val huaweiPattern = longArrayOf(50, 100, 150) // 更快的震动模式
                            val huaweiEffect = VibrationEffect.createWaveform(huaweiPattern, 0)
                            vibrator?.vibrate(huaweiEffect)
                        } else {
                            vibrator?.vibrate(effect)
                        }
                    } else {
                        // API 26以下版本使用旧的vibrate方法
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(vibrationPattern, 0)
                    }
                } catch (e: Exception) {
                    LogUtils.e("AlertService", "震动失败: ${e.message}")
                }
            } else {
                LogUtils.e("AlertService", "设备不支持震动或震动器不可用")
            }
        }
    }
    
    // 检测是否为华为设备
    private fun isHuaweiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER
        return manufacturer.equals("huawei", ignoreCase = true) || 
               manufacturer.equals("honor", ignoreCase = true)
    }

    private fun stopVibration() {
        if (isVibrating) {
            isVibrating = false
            vibrator?.cancel()
            LogUtils.d("AlertService", "停止震动")
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVibration()
        // 释放所有唤醒锁
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        if (screenWakeLock?.isHeld == true) {
            screenWakeLock?.release()
        }
    }

    fun onHandleIntent(intent: Intent?) {
        if (intent?.action == ACTION_STOP) {
            stopVibration()
            stopSelf()
        }
    }


}
