package com.siyu.mdm.enterprise.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mqtt.MqttConfig
import com.siyu.mdm.enterprise.util.mqtt.MqttConnectionListener
import com.siyu.mdm.enterprise.util.mqtt.MqttManager
import com.siyu.mdm.enterprise.util.mqtt.MqttMessageHandler
import com.siyu.mdm.enterprise.util.mqtt.MqttMessageType
import com.siyu.mdm.enterprise.util.mqtt.MqttPowerManager
import com.siyu.mdm.enterprise.util.mqtt.MqttTopicManager

/**
 * MQTT服务
 *
 * 前台服务，负责：
 * - MQTT长连接管理
 * - 心跳保活
 * - 消息收发
 * - 设备状态上报
 *
 * 扩展点：
 * - 添加MqttMessageHandler处理特定消息
 * - 添加MqttConnectionListener监听连接状态
 */
class MqttService : Service() {

    companion object {
        private const val TAG = "MqttService"

        // Action
        const val ACTION_START = "com.siyu.mdm.enterprise.action.MQTT_START"
        const val ACTION_STOP = "com.siyu.mdm.enterprise.action.MQTT_STOP"
        const val ACTION_SEND_HEARTBEAT = "com.siyu.mdm.enterprise.action.SEND_HEARTBEAT"
        const val ACTION_APP_UPDATED = "com.siyu.mdm.enterprise.action.APP_UPDATED"
        const val ACTION_SIM_STATE_CHANGED = "com.siyu.mdm.enterprise.action.SIM_STATE_CHANGED"

        // Extra
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_SIM_STATE = "sim_state"
        const val EXTRA_TIMESTAMP = "timestamp"

        // Notification
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "mdm_mqtt_channel"
        private const val CHANNEL_NAME = "MQTT连接"
    }

    // ==================== 组件 ====================

    private lateinit var mqttManager: MqttManager
    private var wakeLock: PowerManager.WakeLock? = null

    // ==================== 心跳 ====================

    private var lastHeartbeatTime = 0L
    private var currentHeartbeatInterval = 5 * 60 * 1000L // 默认5分钟
    private var isForegroundServiceStarted = false  // 标记是否已启动前台服务

    // ==================== 设备ID和主题 ====================
    
    /** 设备ID - 使用Android ID */
    private val deviceId: String by lazy {
        getAndroidId()
    }
    
    /** 自动订阅的主题列表 */
    private val autoSubscribeTopics: List<String> by lazy {
        listOf(
            MqttTopicManager.getDevicePrivateCommandTopic(deviceId),
            MqttTopicManager.getDeviceConfigTopic(deviceId)
        )
    }
    
    /** QoS级别 */
    private val subscribeQos = 1

    // ==================== 生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        LogUtils.i(TAG, "MQTT服务创建")
        createNotificationChannel()
        
        // 初始化电源管理器
        MqttPowerManager.initialize(this) {
            LogUtils.i(TAG, "网络恢复，尝试重连...")
            startMqtt()
        }
        
        // 开始监听网络状态
        MqttPowerManager.startNetworkListening()
        
        startMqtt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.i(TAG, "MQTT服务启动: ${intent?.action}")

        // ✅ 立即启动前台服务（Android 8.0+ 要求必须在5秒内调用 startForeground）
        if (!isForegroundServiceStarted) {
            startForeground(NOTIFICATION_ID, createNotification())
            isForegroundServiceStarted = true
            LogUtils.i(TAG, "前台服务已启动")
        }

        when (intent?.action) {
            ACTION_START -> startMqtt()
            ACTION_STOP -> stopMqtt()
            ACTION_SEND_HEARTBEAT -> sendHeartbeat()
            ACTION_APP_UPDATED -> handleAppUpdated(intent)
            ACTION_SIM_STATE_CHANGED -> handleSimStateChanged(intent)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        LogUtils.i(TAG, "MQTT服务销毁")
        releaseWakeLock()
        
        // 释放电源管理器
        MqttPowerManager.release()
        
        // 安全检查：确保mqttManager已初始化再调用destroy
        if (::mqttManager.isInitialized) {
            mqttManager.destroy()
        } else {
            LogUtils.w(TAG, "mqttManager未初始化，跳过销毁")
        }
        
        // 重置标记
        isForegroundServiceStarted = false
        
        super.onDestroy()
    }

    // ==================== 设备信息获取 ====================

    fun getAndroidId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    private fun getBatteryLevel(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 0
    }

    private fun isBatteryCharging(): Boolean {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isMobileConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    // ==================== MQTT操作 ====================

    private fun startMqtt() {
        // startForeground() 已移至 onStartCommand() 中调用，避免超时问题
        
        mqttManager = MqttManager.getInstance(this)
        mqttManager.initialize(getAndroidId())
        LogUtils.i(TAG, "MQTT初次连接，设备ID: $deviceId")
        
        // ✅ 遗嘱消息已在 MqttManager.connect() 中自动配置
        
        // 添加连接状态监听
        mqttManager.addConnectionListener(object : MqttConnectionListener {
            override fun onConnected() {
                LogUtils.i(TAG, "MQTT连接成功")
                // ✅ 自动订阅主题
                autoSubscribeTopics()
                sendOnlineStatus()
            }

            override fun onDisconnected() {
                LogUtils.w(TAG, "MQTT连接断开")
            }
        })

        // 添加默认消息处理器
        mqttManager.addMessageHandler(DefaultMqttMessageHandler())

        // 启动连接
        mqttManager.connect()

        // 立即发送一次心跳
        sendHeartbeat()
    }

    private fun stopMqtt() {
        // 安全检查：确保mqttManager已初始化再调用disconnect
        if (::mqttManager.isInitialized) {
            mqttManager.disconnect()
        } else {
            LogUtils.w(TAG, "mqttManager未初始化，跳过断开连接")
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ==================== 心跳 ====================

    private fun sendHeartbeat() {
        if (!::mqttManager.isInitialized || !mqttManager.isConnected()) {
            LogUtils.w(TAG, "MQTT未连接，跳过心跳")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastHeartbeatTime < currentHeartbeatInterval) {
            LogUtils.d(TAG, "心跳间隔未到，跳过")
            return
        }

        // 根据设备状态调整心跳间隔
        updateHeartbeatInterval()

        val batteryLevel = getBatteryLevel()
        val isCharging = isBatteryCharging()
        val isScreenOn = isNetworkAvailable()

        mqttManager.publishHeartbeat(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            isScreenOn = isScreenOn,
            networkType = getNetworkType()
        )

        lastHeartbeatTime = now
        LogUtils.d(TAG, "心跳发送完成: 电量=$batteryLevel%, 充电=$isCharging, 亮屏=$isScreenOn")
    }

    private fun updateHeartbeatInterval() {
        val batteryLevel = getBatteryLevel()
        val isCharging = isBatteryCharging()
        val isScreenOn = isNetworkAvailable()

        // 使用智能心跳配置
        currentHeartbeatInterval = MqttPowerManager.getOptimalHeartbeatInterval(
            batteryLevel,
            isCharging,
            isScreenOn
        )

        LogUtils.d(TAG, "心跳间隔更新: ${currentHeartbeatInterval / 1000}s (电量=$batteryLevel%, 充电=$isCharging)")
    }

    private fun getNetworkType(): String {
        return when {
            isWifiConnected() -> "WiFi"
            isMobileConnected() -> "Mobile"
            else -> "Unknown"
        }
    }

    // ==================== 主题订阅 ====================
    
    /**
     * 自动订阅所有需要的主题
     */
    private fun autoSubscribeTopics() {
        autoSubscribeTopics.forEach { topic ->
            mqttManager.subscribe(topic, subscribeQos)
            LogUtils.d(TAG, "自动订阅主题：$topic (QoS: $subscribeQos)")
        }
    }

    // ==================== 事件处理 ====================

    private fun handleAppUpdated(intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
        LogUtils.i(TAG, "应用更新: $packageName")

        mqttManager.publishAlert(
            alertType = "app_updated",
            alertLevel = "info",
            message = "应用已更新",
            extra = mapOf("packageName" to packageName)
        )
    }

    private fun handleSimStateChanged(intent: Intent) {
        val simState = intent.getStringExtra(EXTRA_SIM_STATE) ?: return
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        LogUtils.i(TAG, "SIM卡状态变化: $simState")

        mqttManager.publishAlert(
            alertType = "sim_changed",
            alertLevel = "warn",
            message = "SIM卡状态变更",
            extra = mapOf(
                "simState" to simState,
                "timestamp" to timestamp
            )
        )
    }

    // ==================== 状态上报 ====================

    private fun sendOnlineStatus() {
        val deviceId = mqttManager.getDeviceId() ?: getAndroidId()
        val extra = mapOf(
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "sdkInt" to Build.VERSION.SDK_INT,
            "appVersion" to getAppVersion()
        )

        mqttManager.publishStatus("online", extra)
    }

    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    // ==================== 通知 ====================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MQTT长连接服务"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("MDM服务运行中")
            .setContentText("MQTT连接正常")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(Notification.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    // ==================== WakeLock ====================

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MqttService::WakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 最多10分钟
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}

/**
 * 默认MQTT消息处理器
 *
 * 处理服务器下发的命令消息
 */
class DefaultMqttMessageHandler : MqttMessageHandler {

    override val messageType: String? = null // 处理所有类型

    override fun handleMessage(topic: String, message: String): Boolean {
        // 解析消息类型并处理
        val messageType = MqttTopicManager.parseMessageType(topic)
        LogUtils.i("DefaultHandler", "处理消息: topic=$topic, type=$messageType")

        // 根据消息类型分发处理
        return when (messageType) {
            MqttMessageType.COMMAND -> handleCommand(message)
            MqttMessageType.CONFIG -> handleConfig(message)
            else -> false
        }
    }

    private fun handleCommand(message: String): Boolean {
        // TODO: 解析命令并执行
        // 使用MdmCommandManager执行相应命令
        LogUtils.i("DefaultHandler", "收到命令: $message")
        return true
    }

    private fun handleConfig(message: String): Boolean {
        // TODO: 处理配置下发
        LogUtils.i("DefaultHandler", "收到配置: $message")
        return true
    }

    override fun getPriority(): Int = 1000 // 低优先级
}
