package com.siyu.mdm.enterprise.util.mqtt

import com.siyu.mdm.enterprise.util.LogUtils

/**
 * MQTT 主题统一管理类
 * 
 * 主题架构：
 * - 设备订阅：mdm/{deviceId}         （接收服务器指令）
 * - 设备发布：mdm/report             （设备在线/离线状态）
 * - 设备发布：mdm/command/{deviceId} （命令执行结果）
 * - 设备订阅：device/config/{deviceId}（接收配置更新）
 */
object MqttTopicManager {

    private const val TAG = "MqttTopicManager"

    // ========================== 系统级主题 ==========================
    
    /** 设备离线遗嘱主题 */
    const val DEVICE_WILL = "mdm/report"

    // ========================== 设备专属主题（动态拼接） ==========================
    
    /**
     * 获取设备私有指令主题
     * 格式：mdm/{deviceId}
     * 用途：服务器向指定设备发送单独指令
     */
    fun getDevicePrivateCommandTopic(deviceId: String): String {
        return "mdm/$deviceId"
    }

    /**
     * 获取设备响应指令结果主题
     * 格式：mdm/command/{deviceId}
     * 用途：设备向服务器回复指令执行结果
     */
    fun getDeviceCommandResultTopic(deviceId: String): String {
        return "mdm/command/$deviceId"
    }

    /**
     * 获取设备配置同步主题
     * 格式：device/config/{deviceId}
     * 用途：服务器向指定设备同步配置信息
     */
    fun getDeviceConfigTopic(deviceId: String): String {
        return "device/config/$deviceId"
    }

    // ========================== 兼容旧版本 ==========================
    
    /** @deprecated 使用 getDevicePrivateCommandTopic 代替 */
    @Deprecated("请使用 getDevicePrivateCommandTopic 方法")
    const val TOPIC_COMMAND = "mdm/server/command"

    /** @deprecated 使用 getDevicePrivateCommandTopic 代替 */
    @Deprecated("请使用 getDevicePrivateCommandTopic 方法")
    const val TOPIC_ALERT = "mdm/server/alert"

    /** @deprecated 使用 getDevicePrivateCommandTopic 代替 */
    @Deprecated("请使用 getDevicePrivateCommandTopic 方法")
    const val TOPIC_CONFIG = "mdm/server/config"

    /** @deprecated 使用 getDevicePrivateCommandTopic 代替 */
    @Deprecated("请使用 getDevicePrivateCommandTopic 方法")
    const val TOPIC_HEARTBEAT = "mdm/server/heartbeat"

    // ========================== 辅助方法 ==========================
    
    /**
     * 解析主题获取消息类型
     *
     * @param topic 完整主题
     * @return 消息类型，如果无法解析返回null
     */
    fun parseMessageType(topic: String): String? {
        return try {
            val parts = topic.split("/")
            if (parts.size >= 3) {
                parts[2]
            } else {
                null
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "解析主题失败: $topic", e)
            null
        }
    }

    /**
     * 解析主题获取设备ID
     *
     * @param topic 完整主题
     * @return 设备ID，如果无法解析返回null
     */
    fun parseDeviceId(topic: String): String? {
        return try {
            val parts = topic.split("/")
            if (parts.size >= 2) {
                parts[1]
            } else {
                null
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "解析主题失败: $topic", e)
            null
        }
    }

    /**
     * 构建设备特定的上行主题
     */
    fun buildDeviceTopic(deviceId: String, messageType: String): String {
        return "mdm/$deviceId/$messageType"
    }

    /**
     * 获取需要订阅的所有主题
     * 用于初始化MQTT客户端订阅
     */
    fun getSubscribableTopics(): List<String> {
        return listOf(
            TOPIC_COMMAND,  // 使用旧的主题兼容
            TOPIC_ALERT
        )
    }

    /**
     * 获取设备状态主题（离线告警用）
     */
    fun getDeviceStatusTopic(deviceId: String): String {
        return "mdm/$deviceId/status"
    }
}
