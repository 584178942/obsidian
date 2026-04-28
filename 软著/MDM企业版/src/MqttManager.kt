package com.siyu.mdm.enterprise.util.mqtt

import android.content.Context
import android.provider.Settings
import com.siyu.mdm.enterprise.util.LogUtils
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MQTT管理器
 *
 * 核心功能：
 * - MQTT连接管理（连接、断开、重连）
 * - 消息订阅与发布
 * - 消息回调处理
 * - 生命周期管理
 *
 * 扩展点：
 * - 通过MqttMessageHandler扩展消息处理
 * - 通过MqttConnectionListener扩展连接状态监听
 */
class MqttManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "MqttManager"

        @Volatile
        private var instance: MqttManager? = null

        fun getInstance(context: Context): MqttManager {
            return instance ?: synchronized(this) {
                instance ?: MqttManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // ==================== 状态 ====================

    private val isConnected = AtomicBoolean(false)
    private var client: MqttClient? = null
    private var deviceId: String? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var disconnectTimeMillis: Long = 0L // 断开时间戳

    // ==================== 监听器 ====================

    private val connectionListeners = mutableListOf<MqttConnectionListener>()
    private val messageHandlers = mutableListOf<MqttMessageHandler>()

    // ==================== 连接参数 ====================

    private val serverUri: String
        get() = MqttConfig.MQTT_SERVER_URI

    private val clientId: String
        get() = MqttConfig.CLIENT_ID_PREFIX + (deviceId ?: getAndroidId())

    // ==================== 设备ID获取 ====================

    private fun getAndroidId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    // ==================== 生命周期 ====================

    /**
     * 初始化MQTT客户端
     */
    fun initialize(deviceId: String) {
        this.deviceId = deviceId
        try {
            client = MqttClient(
                serverUri,
                clientId,
                MemoryPersistence()
            ).apply {
                setCallback(createCallback())
                setTimeToWait(MqttConfig.MESSAGE_TIMEOUT)
            }
            LogUtils.i(TAG, "MQTT客户端初始化成功: $clientId")
        } catch (e: MqttException) {
            LogUtils.e(TAG, "MQTT客户端初始化失败", e)
        }
    }

    /**
     * 连接MQTT服务器
     */
    fun connect() {
        if (isConnected.get()) {
            LogUtils.w(TAG, "MQTT已连接，跳过连接")
            return
        }

        try {
            // 创建 Will Message (遗嘱消息)
            val willMessage = MqttMessage().apply {
                payload = String.format(MqttConfig.WILL_MESSAGE, clientId).toByteArray()
                qos = MqttConfig.WILL_QOS
                isRetained = MqttConfig.WILL_RETAINED
            }
            
            LogUtils.i(TAG, "📋 遗嘱消息配置:")
            LogUtils.i(TAG, "  主题: ${MqttConfig.WILL_TOPIC}")
            LogUtils.i(TAG, "  内容: ${String(willMessage.payload)}")
            LogUtils.i(TAG, "  QoS: ${MqttConfig.WILL_QOS}")
            LogUtils.i(TAG, "  Retained: ${MqttConfig.WILL_RETAINED}")

            // 创建连接选项
            val options = MqttConnectOptions().apply {
                connectionTimeout = MqttConfig.CONNECTION_TIMEOUT
                keepAliveInterval = MqttConfig.KEEP_ALIVE_INTERVAL
                isCleanSession = MqttConfig.CLEAN_SESSION
                isAutomaticReconnect = MqttConfig.AUTO_RECONNECT
                userName = "admin"
                password = "admin".toCharArray()
                setWill(MqttConfig.WILL_TOPIC, willMessage.payload, willMessage.qos, willMessage.isRetained)
            }
            
            LogUtils.i(TAG, "📋 连接配置:")
            LogUtils.i(TAG, "  服务器: ${MqttConfig.MQTT_SERVER_URI}")
            LogUtils.i(TAG, "  客户端ID: $clientId")
            LogUtils.i(TAG, "  心跳间隔: ${MqttConfig.KEEP_ALIVE_INTERVAL}秒")
            LogUtils.i(TAG, "  自动重连: ${MqttConfig.AUTO_RECONNECT}")

            client?.connect(options)
            isConnected.set(true)
            LogUtils.i(TAG, "✅ MQTT连接成功")

            // 订阅主题
            subscribeTopics()

            // 通知监听器
            notifyConnectionListeners(true)
        } catch (e: MqttException) {
            LogUtils.e(TAG, "❌ MQTT连接失败", e)
            isConnected.set(false)
            notifyConnectionListeners(false)
        }
    }

    /**
     * 断开MQTT连接
     */
    fun disconnect() {
        try {
            client?.disconnect()
            isConnected.set(false)
            LogUtils.i(TAG, "MQTT连接已断开")
            notifyConnectionListeners(false)
        } catch (e: MqttException) {
            LogUtils.e(TAG, "MQTT断开连接失败", e)
        }
    }

    /**
     * 订阅主题
     */
    private fun subscribeTopics() {
        val topics = MqttTopicManager.getSubscribableTopics()
        try {
            val qosArray = topics.map { MqttConfig.SUBSCRIBE_QOS }.toIntArray()
            client?.subscribe(topics.toTypedArray(), qosArray)
            LogUtils.i(TAG, "MQTT主题订阅成功: $topics")
        } catch (e: MqttException) {
            LogUtils.e(TAG, "MQTT主题订阅失败", e)
        }
    }
    
    /**
     * 订阅单个主题
     * @param topic 主题
     * @param qos 服务质量
     */
    fun subscribe(topic: String, qos: Int = MqttConfig.SUBSCRIBE_QOS) {
        if (!isConnected.get()) {
            LogUtils.w(TAG, "MQTT未连接，无法订阅主题: $topic")
            return
        }
        
        try {
            client?.subscribe(topic, qos)
            LogUtils.d(TAG, "MQTT主题订阅成功: topic=$topic, qos=$qos")
        } catch (e: MqttException) {
            LogUtils.e(TAG, "MQTT主题订阅失败: topic=$topic", e)
        }
    }

    /**
     * 发布消息
     */
    fun publish(topic: String, message: String, qos: Int = MqttConfig.PUBLISH_QOS): Boolean {
        if (!isConnected.get()) {
            LogUtils.w(TAG, "MQTT未连接，无法发布消息")
            return false
        }

        return try {
            val mqttMessage = MqttMessage(message.toByteArray()).apply {
                this.qos = qos
                isRetained = false
            }
            client?.publish(topic, mqttMessage)
            LogUtils.d(TAG, "MQTT消息发布成功: topic=$topic, qos=$qos")
            true
        } catch (e: MqttException) {
            LogUtils.e(TAG, "MQTT消息发布失败: topic=$topic", e)
            false
        }
    }

    /**
     * 发布设备状态
     */
    fun publishStatus(status: String, extra: Map<String, Any>? = null): Boolean {
        val topic = MqttTopicManager.buildDeviceTopic(
            deviceId ?: getAndroidId(),
            "status"
        )
        val message = MqttMessageBuilder.buildStatusMessage(
            deviceId ?: getAndroidId(),
            status,
            extra
        )
        return publish(topic, message)
    }

    /**
     * 发布心跳
     */
    fun publishHeartbeat(
        batteryLevel: Int,
        isCharging: Boolean,
        isScreenOn: Boolean,
        networkType: String? = null
    ): Boolean {
        val topic = MqttTopicManager.buildDeviceTopic(
            deviceId ?: getAndroidId(),
            "heartbeat"
        )
        val message = MqttMessageBuilder.buildHeartbeatMessage(
            deviceId ?: getAndroidId(),
            batteryLevel,
            isCharging,
            isScreenOn,
            networkType
        )
        return publish(topic, message)
    }

    /**
     * 发布告警
     */
    fun publishAlert(alertType: String, alertLevel: String, message: String, extra: Map<String, Any>? = null): Boolean {
        val topic = MqttTopicManager.buildDeviceTopic(
            deviceId ?: getAndroidId(),
            "alert"
        )
        val fullMessage = MqttMessageBuilder.buildAlertMessage(
            deviceId ?: getAndroidId(),
            alertType,
            alertLevel,
            message,
            extra
        )
        return publish(topic, fullMessage)
    }

    // ==================== 消息处理 ====================

    private fun createCallback(): MqttCallback {
        return object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                isConnected.set(false)
                LogUtils.e(TAG, "❌ MQTT连接丢失: ${cause?.message ?: "未知原因"}")
                notifyConnectionListeners(false)
                
                // 启动自动重连
                scheduleReconnect()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                topic ?: return
                message ?: return

                val payload = String(message.payload)
                LogUtils.i(TAG, "📩 ═══════════════════════════════════════")
                LogUtils.i(TAG, "📩 收到服务器消息:")
                LogUtils.i(TAG, "📩 主题: $topic")
                LogUtils.i(TAG, "📩 内容: $payload")
                LogUtils.i(TAG, "📩 QoS: ${message.qos}")
                LogUtils.i(TAG, "📩 ═══════════════════════════════════════")

                // 分发给处理器
                dispatchMessage(topic, payload)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                LogUtils.d(TAG, "消息投递完成")
            }
        }
    }

    private fun dispatchMessage(topic: String, payload: String) {
        // 按优先级排序处理
        messageHandlers.sortedBy { it.getPriority() }.forEach { handler ->
            if (handler.handleMessage(topic, payload)) {
                LogUtils.d(TAG, "消息已处理: ${handler.messageType}")
                return
            }
        }
    }

    // ==================== 监听器管理 ====================

    fun addConnectionListener(listener: MqttConnectionListener) {
        connectionListeners.add(listener)
    }

    fun removeConnectionListener(listener: MqttConnectionListener) {
        connectionListeners.remove(listener)
    }

    private fun notifyConnectionListeners(connected: Boolean) {
        connectionListeners.forEach { listener ->
            try {
                if (connected) {
                    listener.onConnected()
                } else {
                    listener.onDisconnected()
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "通知连接状态失败", e)
            }
        }
    }

    fun addMessageHandler(handler: MqttMessageHandler) {
        messageHandlers.add(handler)
    }

    fun removeMessageHandler(handler: MqttMessageHandler) {
        messageHandlers.remove(handler)
    }

    // ==================== 状态查询 ====================

    fun isConnected(): Boolean = isConnected.get()

    fun getDeviceId(): String? = deviceId

    // ==================== 重连机制 ====================

    /**
     * 调度重连（智能延迟）
     */
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        
        // 记录断开时间（如果还没有记录）
        if (disconnectTimeMillis == 0L) {
            disconnectTimeMillis = System.currentTimeMillis()
        }
        
        reconnectAttempts++
        
        // 使用智能重连延迟
        val delay = MqttPowerManager.getReconnectDelay(
            System.currentTimeMillis() - disconnectTimeMillis
        )
        
        LogUtils.i(TAG, "🔄 计划 ${delay / 1000} 秒后重连 (第 $reconnectAttempts 次尝试)")
        LogUtils.i(TAG, "🔄 断网时间: ${(System.currentTimeMillis() - disconnectTimeMillis) / 60000} 分钟")
        
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            delay(delay)
            if (!isConnected.get()) {
                LogUtils.i(TAG, "🔄 开始重连...")
                doReconnect()
            }
        }
    }

    /**
     * 执行重连（完整重连流程）
     */
    private fun doReconnect() {
        try {
            // 如果客户端不存在或已关闭，重新创建
            if (client == null) {
                LogUtils.w(TAG, "⚠️ MQTT客户端未初始化，重新创建")
                initialize(deviceId ?: getAndroidId())
            }
            
            client?.let { mqttClient ->
                // 检查是否已经连接
                if (mqttClient.isConnected) {
                    LogUtils.i(TAG, "✅ MQTT已经连接，无需重连")
                    isConnected.set(true)
                    reconnectAttempts = 0
                    return
                }
                
                // 尝试断开连接
                try {
                    if (mqttClient.isConnected) {
                        mqttClient.disconnect()
                    }
                } catch (e: Exception) {
                    LogUtils.w(TAG, "断开旧连接失败（忽略）: ${e.message}")
                }
                
                // 重新连接
                LogUtils.i(TAG, "🔄 执行重连...")
                mqttClient.reconnect()
                
                // 等待连接建立
                var waitCount = 0
                while (!mqttClient.isConnected && waitCount < 30) {
                    Thread.sleep(1000)
                    waitCount++
                }
                
                if (mqttClient.isConnected) {
                    LogUtils.i(TAG, "✅ 重连成功")
                    isConnected.set(true)
                    reconnectAttempts = 0
                    disconnectTimeMillis = 0L // 重置断开时间
                    
                    // 重新订阅主题
                    subscribeTopics()
                    autoSubscribeTopics()
                    
                    // 通知监听器
                    notifyConnectionListeners(true)
                    
                    LogUtils.i(TAG, "✅ 重连完成，设备已重新连接")
                } else {
                    LogUtils.e(TAG, "❌ 重连超时")
                    scheduleReconnect()
                }
            } ?: run {
                LogUtils.e(TAG, "❌ MQTT客户端为null，重连失败")
                scheduleReconnect()
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "❌ 重连失败: ${e.message}", e)
            scheduleReconnect()
        }
    }
    
    /**
     * 自动订阅设备专属主题
     */
    private fun autoSubscribeTopics() {
        try {
            val deviceIdValue = deviceId ?: getAndroidId()
            val privateTopic = "mdm/$deviceIdValue"
            val configTopic = "device/config/$deviceIdValue"
            
            client?.subscribe(privateTopic, MqttConfig.SUBSCRIBE_QOS)
            client?.subscribe(configTopic, MqttConfig.SUBSCRIBE_QOS)
            
            LogUtils.i(TAG, "✅ 自动订阅主题成功: $privateTopic, $configTopic")
        } catch (e: Exception) {
            LogUtils.e(TAG, "❌ 自动订阅主题失败", e)
        }
    }

    // ==================== 销毁 ====================

    fun destroy() {
        reconnectJob?.cancel()
        disconnect()
        client?.close()
        client = null
        instance = null
        reconnectAttempts = 0
        LogUtils.i(TAG, "MQTT管理器已销毁")
    }
}

/**
 * MQTT连接状态监听器
 */
interface MqttConnectionListener {
    fun onConnected()
    fun onDisconnected()
}
