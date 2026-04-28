package com.siyu.mdm.custom.device.util.mqtt

import android.content.Context
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.util.InitiateUtils
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MqttManager private constructor(context: Context) {
    companion object {
        private const val TAG = "MqttManager"
        @Volatile
        private var instance: MqttManager? = null
        /**
         * 获取单例实例
         * @param context 上下文，内部会使用applicationContext避免内存泄漏
         */
        fun getInstance(context: Context): MqttManager {
            return instance ?: synchronized(this) {
                instance ?: MqttManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * 销毁单例，释放资源
         */
        fun destroyInstance() {
            instance?.disconnect()
            instance = null
        }
    }

    private val context: Context = context.applicationContext
    private var client: MqttAndroidClient? = null
    private var connectOptions: MqttConnectOptions? = null
    private var connectionCallback: ConnectionCallback? = null
    private var messageCallback: MessageCallback? = null

    @Volatile
    private var isConnected = false
    // 生成客户端ID，优先使用IMEI，失败则使用UUID
    private val clientId: String = runCatching {
        InitiateUtils().getSerialNumber().toString()
    }.getOrElse {
        LogUtils.w(TAG, "获取IMEI失败，使用UUID作为clientId", it)
        "mqtt_client_${UUID.randomUUID()}"
    }

    // 用于重连后恢复订阅的主题列表
    private val subscribedTopics = ConcurrentHashMap<String, Int>()

    /**
     * 设置MQTT连接参数
     * @param serverUri 服务器地址，格式为host:port
     * @param username 用户名（可选）
     * @param password 密码（可选）
     * @param cleanSession 是否清除会话
     * @param keepAliveInterval 心跳间隔(秒)
     * @param connectionTimeout 连接超时(秒)
     * @param automaticReconnect 是否自动重连
     * @param ssl 是否使用SSL加密
     * @param willTopic 遗嘱主题（可选）
     * @param willMessage 遗嘱消息（可选）
     * @param willQos 遗嘱消息QoS
     * @param willRetained 遗嘱消息是否保留
     */
    fun setConnectionOptions(
        serverUri: String,
        username: String? = null,
        password: String? = null,
        cleanSession: Boolean = true,
        keepAliveInterval: Int = 60,
        connectionTimeout: Int = 30,
        automaticReconnect: Boolean = true,
        ssl: Boolean = false,
        willTopic: String? = null,
        willMessage: String? = null,
        willQos: Int = 1,
        willRetained: Boolean = true
    ): MqttManager {
        val serverURI = if (ssl) "ssl://$serverUri" else "tcp://$serverUri"

        // 关闭现有连接并创建新客户端
        if (client?.isConnected == true) {
            disconnect()
        }
        client = MqttAndroidClient(context, serverURI, clientId)
        setupCallbacks()

        LogUtils.d(TAG, "MQTT服务器地址: $serverURI, 客户端ID: $clientId")

        connectOptions = MqttConnectOptions().apply {
            this.isCleanSession = cleanSession
            this.keepAliveInterval = keepAliveInterval
            this.connectionTimeout = connectionTimeout
            this.isAutomaticReconnect = automaticReconnect

            username?.let { this.userName = it }
            password?.let { this.password = it.toCharArray() }

            // 配置遗嘱消息
            if (willTopic != null && willMessage != null) {
                setWill(willTopic, willMessage.toByteArray(), willQos, willRetained)
            }
        }
        return this
    }

    /**
     * 连接到MQTT服务器
     */
    fun connect() {
        if (isConnected) {
            LogUtils.d(TAG, "已经处于连接状态，无需重复连接")
            connectionCallback?.onConnected()
            return
        }

        val client = client ?: run {
            val error = "MQTT客户端未初始化，请先调用setConnectionOptions"
            LogUtils.e(TAG, error)
            connectionCallback?.onConnectionFailed(IllegalStateException(error))
            return
        }

        val options = connectOptions ?: run {
            val error = "连接参数未设置，请先调用setConnectionOptions"
            LogUtils.e(TAG, error)
            connectionCallback?.onConnectionFailed(IllegalStateException(error))
            return
        }

        try {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    isConnected = true
                    LogUtils.d(TAG, "MQTT连接成功")
                    connectionCallback?.onConnected()
                    // 恢复订阅
                    restoreSubscriptions()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    isConnected = false
                    LogUtils.e(TAG, "MQTT连接失败: ${exception.message}", exception)
                    connectionCallback?.onConnectionFailed(exception)
                }
            })
        } catch (e: MqttException) {
            isConnected = false
            LogUtils.e(TAG, "MQTT连接异常: ${e.message}", e)
            connectionCallback?.onConnectionFailed(e)
        }
    }

    /**
     * 断开与MQTT服务器的连接
     */
    fun disconnect() {
        if (!isConnected) {
            LogUtils.d(TAG, "已经处于断开状态，无需重复断开")
            connectionCallback?.onDisconnected()
            return
        }

        val client = client ?: run {
            LogUtils.d(TAG, "客户端未初始化，视为已断开")
            isConnected = false
            connectionCallback?.onDisconnected()
            return
        }

        try {
            client.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    isConnected = false
                    LogUtils.d(TAG, "MQTT断开连接成功")
                    connectionCallback?.onDisconnected()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    LogUtils.e(TAG, "MQTT断开连接失败: ${exception.message}", exception)
                    connectionCallback?.onDisconnectionFailed(exception)
                }
            })
        } catch (e: MqttException) {
            LogUtils.e(TAG, "MQTT断开连接异常: ${e.message}", e)
            connectionCallback?.onDisconnectionFailed(e)
        }
    }

    /**
     * 发布消息
     * @param topic 主题
     * @param payload 消息内容
     * @param qos 服务质量等级 (0, 1, 2)
     * @param retained 是否保留消息
     */
    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        // 首先检查client对象是否存在
        val currentClient = client ?: run {
            LogUtils.e(TAG, "客户端未初始化，无法发布消息")
            return
        }
        
        // 检查连接状态，同时验证client的实际连接状态
        if (!isConnected) {
            // 如果isConnected为false但client实际已连接，更新状态
            if (currentClient.isConnected) {
                LogUtils.d(TAG, "发现状态不同步，isConnected=false但client实际已连接，更新状态")
                isConnected = true
            } else {
                val error = "无法发布消息：未连接到MQTT服务器"
                LogUtils.e(TAG, error)
                return
            }
        }

        // 获取网络策略管理器
        if (qos < 0 || qos > 2) {
            LogUtils.e(TAG, "无效的QoS值: $qos，必须是0、1或2")
            return
        }

        try {
            val message = MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                this.isRetained = retained
            }

            currentClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    LogUtils.d(TAG, "消息发布成功: 主题=$topic, QoS=$qos")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    LogUtils.e(TAG, "消息发布失败: 主题=$topic, 原因=${exception.message}", exception)
                }
            })
        } catch (e: MqttException) {
            LogUtils.e(TAG, "消息发布异常: 主题=$topic, 原因=${e.message}", e)
        } catch (e: IllegalArgumentException) {
            LogUtils.e(TAG, "无效的发布参数: 主题=$topic, 原因=${e.message}", e)
        }
    }

    /**
     * 订阅主题
     * @param topic 主题
     * @param qos 服务质量等级 (0, 1, 2)
     */
    fun subscribe(topic: String, qos: Int = 1) {
        // 首先检查client对象是否存在
        val currentClient = client ?: run {
            LogUtils.e(TAG, "客户端未初始化，无法订阅主题, 主题=$topic")
            return
        }
        
        // 检查连接状态，同时验证client的实际连接状态
        if (!isConnected) {
            // 如果isConnected为false但client实际已连接，更新状态
            if (currentClient.isConnected) {
                LogUtils.d(TAG, "发现状态不同步，isConnected=false但client实际已连接，更新状态, 主题=$topic")
                isConnected = true
            } else {
                LogUtils.e(TAG, "无法订阅主题：未连接到MQTT服务器, 主题=$topic")
                return
            }
        }

        if (qos < 0 || qos > 2) {
            LogUtils.e(TAG, "无效的QoS值: $qos，必须是0、1或2, 主题=$topic")
            return
        }

        try {
            currentClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    subscribedTopics[topic] = qos
                    LogUtils.d(TAG, "主题订阅成功: 主题=$topic, QoS=$qos")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    LogUtils.e(TAG, "主题订阅失败: 主题=$topic, 原因=${exception.message}", exception)
                }
            })
        } catch (e: MqttException) {
            LogUtils.e(TAG, "主题订阅异常: 主题=$topic, 原因=${e.message}", e)
        }
    }

    /**
     * 取消订阅主题
     * @param topic 主题
     */
    fun unsubscribe(topic: String) {
        // 首先检查client对象是否存在
        val currentClient = client ?: run {
            LogUtils.e(TAG, "客户端未初始化，无法取消订阅, 主题=$topic")
            return
        }
        
        // 检查连接状态，同时验证client的实际连接状态
        if (!isConnected) {
            // 如果isConnected为false但client实际已连接，更新状态
            if (currentClient.isConnected) {
                LogUtils.d(TAG, "发现状态不同步，isConnected=false但client实际已连接，更新状态, 主题=$topic")
                isConnected = true
            } else {
                LogUtils.e(TAG, "无法取消订阅：未连接到MQTT服务器, 主题=$topic")
                return
            }
        }

        try {
            currentClient.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    subscribedTopics.remove(topic)
                    LogUtils.d(TAG, "取消订阅成功: 主题=$topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    LogUtils.e(TAG, "取消订阅失败: 主题=$topic, 原因=${exception.message}", exception)
                }
            })
        } catch (e: MqttException) {
            LogUtils.e(TAG, "取消订阅异常: 主题=$topic, 原因=${e.message}", e)
        }
    }

    /**
     * 设置连接回调
     */
    fun setConnectionCallback(callback: ConnectionCallback): MqttManager {
        this.connectionCallback = callback
        return this
    }

    /**
     * 设置消息回调
     */
    fun setMessageCallback(callback: MessageCallback): MqttManager {
        this.messageCallback = callback
        return this
    }

    /**
     * 检查连接状态
     * @return 当前连接状态
     */
    fun isConnected(): Boolean {
        // 如果client存在但状态不同步，更新连接状态
        val currentClient = client
        if (currentClient != null && currentClient.isConnected != isConnected) {
            LogUtils.d(TAG, "发现连接状态不同步，更新状态：${isConnected} -> ${currentClient.isConnected}")
            isConnected = currentClient.isConnected
        }
        return isConnected
    }

    /**
     * 获取客户端ID
     */
    fun getClientId(): String = clientId

    /**
     * 获取已订阅的主题列表
     */
    fun getSubscribedTopics(): Map<String, Int> = subscribedTopics.toMap()

    /**
     * 设置内部回调
     */
    private fun setupCallbacks() {
        client?.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                LogUtils.d(TAG, "连接完成: 服务器=$serverURI, 重连=$reconnect")
                // 无论是否是重连，都设置连接状态为true
                isConnected = true
                if (reconnect) {
                    connectionCallback?.onReconnected()
                    // 重连后恢复订阅
                    restoreSubscriptions()
                } else {
                    connectionCallback?.onConnected()
                }
            }

            override fun connectionLost(cause: Throwable?) {
                isConnected = false
                LogUtils.e(TAG, "连接丢失: ${cause?.message}", cause)
                connectionCallback?.onConnectionLost(cause)
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload)
                LogUtils.d(TAG, "收到消息: 主题=$topic, QoS=${message.qos}, 保留=${message.isRetained}")
                messageCallback?.onMessageReceived(topic, payload, message.qos, message.isRetained)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                LogUtils.d(TAG, "消息投递完成: ${token?.message}")
            }
        })
    }

    /**
     * 恢复订阅的主题（用于重连后）
     */
    private fun restoreSubscriptions() {
        if (subscribedTopics.isEmpty()) {
            LogUtils.d(TAG, "没有需要恢复的订阅主题")
            return
        }

        LogUtils.d(TAG, "开始恢复订阅的主题，共${subscribedTopics.size}个")
        subscribedTopics.forEach { (topic, qos) ->
            try {
                client?.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken) {
                        LogUtils.d(TAG, "恢复订阅成功: 主题=$topic, QoS=$qos")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                        LogUtils.e(TAG, "恢复订阅失败: 主题=$topic, 原因=${exception.message}", exception)
                    }
                })
            } catch (e: Exception) {
                LogUtils.e(TAG, "恢复订阅异常: 主题=$topic, 原因=${e.message}", e)
            }
        }
    }

    /**
     * 连接回调接口
     * 所有方法都有默认实现，方便用户只实现需要的方法
     */
    interface ConnectionCallback {
        /**
         * 连接成功
         */
        fun onConnected() {}

        /**
         * 连接失败
         * @param exception 失败原因
         */
        fun onConnectionFailed(exception: Throwable) {}

        /**
         * 断开连接成功
         */
        fun onDisconnected() {}

        /**
         * 断开连接失败
         * @param exception 失败原因
         */
        fun onDisconnectionFailed(exception: Throwable) {}

        /**
         * 重连成功
         */
        fun onReconnected() {}

        /**
         * 连接丢失
         * @param cause 丢失原因（可能为null）
         */
        fun onConnectionLost(cause: Throwable?) {}
    }

    /**
     * 消息回调接口
     */
    interface MessageCallback {
        /**
         * 收到消息
         * @param topic 消息主题
         * @param payload 消息内容
         * @param qos 服务质量等级
         * @param retained 是否为保留消息
         */
        fun onMessageReceived(topic: String, payload: String, qos: Int, retained: Boolean)
    }
}