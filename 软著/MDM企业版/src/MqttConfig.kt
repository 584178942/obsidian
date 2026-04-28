package com.siyu.mdm.enterprise.util.mqtt

/**
 * MQTT配置
 *
 * 支持配置项：
 * - 服务器地址（支持多协议）
 * - 客户端ID自动生成
 * - 连接参数（超时、保活、遗嘱消息）
 * - 重连策略
 * - 消息质量（QoS）
 */
object MqttConfig {

    // ==================== 服务器配置 ====================

    /** MQTT Broker 地址 - Linux服务器 */
    const val MQTT_SERVER_URI = "tcp://192.168.1.199:1883"

    /** WebSocket协议地址（可选） */
    const val WEBSOCKET_SERVER_URI = "ws://192.168.1.199:1883/ws"

    // ==================== 客户端标识 ====================

    /** 客户端ID前缀 */
    const val CLIENT_ID_PREFIX = "mdm_enterprise_"

    // ==================== 连接参数 ====================

    /** 连接超时时间：30秒 */
    const val CONNECTION_TIMEOUT = 30

    /** 心跳间隔：60秒 */
    const val KEEP_ALIVE_INTERVAL = 60

    /** 清除会话 */
    const val CLEAN_SESSION = true

    /** 自动重连 */
    const val AUTO_RECONNECT = true

    // ==================== 遗嘱消息 ====================

    /** 遗嘱主题 - 与MqttTopicManager保持一致 */
    const val WILL_TOPIC = MqttTopicManager.DEVICE_WILL

    /** 遗嘱消息内容 - JSON格式 */
    const val WILL_MESSAGE = "{\"action\":\"offline\",\"code\":\"%s\"}"

    /** 遗嘱QoS */
    const val WILL_QOS = 1

    /** 遗嘱 retained */
    const val WILL_RETAINED = true

    // ==================== 消息质量 ====================

    /** 设备上报消息QoS */
    const val PUBLISH_QOS = 1

    /** 设备订阅消息QoS */
    const val SUBSCRIBE_QOS = 1

    // ==================== 重连策略 ====================

    /** 首次重连延迟：10秒 */
    const val FIRST_RECONNECT_DELAY = 10_000L

    /** 最大重连延迟：5分钟 */
    const val MAX_RECONNECT_DELAY = 5 * 60_000L

    /** 重连指数 */
    const val RECONNECT_BACKOFF_MULTIPLIER = 1.5

    // ==================== 消息缓存 ====================

    /** 离线消息缓存大小 */
    const val MAX_OFFLINE_MESSAGES = 100

    /** 消息发送超时：30秒 */
    const val MESSAGE_TIMEOUT = 30_000L
}
