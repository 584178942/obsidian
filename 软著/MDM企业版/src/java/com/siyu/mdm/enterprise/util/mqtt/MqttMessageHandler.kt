package com.siyu.mdm.enterprise.util.mqtt

import org.json.JSONObject

/**
 * MQTT消息处理器接口
 *
 * 用于扩展不同类型的消息处理
 * 定义消息类型并处理对应的业务逻辑
 *
 * 使用方式：
 * 1. 实现此接口处理特定类型的消息
 * 2. 注册到MqttMessageManager
 * 3. 消息到达时自动分发到对应处理器
 */
interface MqttMessageHandler {

    /**
     * 获取处理器支持的消息类型
     * 返回null表示处理所有消息
     */
    val messageType: String?

    /**
     * 处理MQTT消息
     *
     * @param topic 消息主题
     * @param message 消息内容
     * @return true表示消息已处理，false表示未处理
     */
    fun handleMessage(topic: String, message: String): Boolean

    /**
     * 获取处理优先级
     * 数值越小优先级越高
     */
    fun getPriority(): Int = 100
}

/**
 * 内置消息类型常量
 */
object MqttMessageType {
    const val COMMAND = "command"
    const val ALERT = "alert"
    const val CONFIG = "config"
    const val HEARTBEAT = "heartbeat"
    const val UNKNOWN = "unknown"
}
