package com.siyu.mdm.enterprise.util.mqtt

import com.siyu.mdm.enterprise.util.HeartBeatConfig
import org.json.JSONObject

/**
 * MQTT消息构建器
 *
 * 用于构建各类型的JSON消息
 * 支持扩展新的消息类型
 */
object MqttMessageBuilder {

    // ==================== 基础字段 ====================

    private const val KEY_DEVICE_ID = "deviceId"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_MESSAGE_TYPE = "messageType"
    private const val KEY_DATA = "data"
    private const val KEY_RESULT = "result"
    private const val KEY_ERROR = "error"

    // ==================== 消息类型 ====================

    const val TYPE_STATUS = "status"
    const val TYPE_HEARTBEAT = "heartbeat"
    const val TYPE_ALERT = "alert"
    const val TYPE_COMMAND_RESULT = "command_result"
    const val TYPE_REGISTER = "register"
    const val TYPE_UNREGISTER = "unregister"

    /**
     * 构建设备状态消息
     */
    fun buildStatusMessage(
        deviceId: String,
        status: String,
        extra: Map<String, Any>? = null
    ): String {
        return buildBaseMessage(deviceId, TYPE_STATUS).apply {
            put(KEY_DATA, JSONObject().apply {
                put("status", status)
                extra?.forEach { (k, v) -> put(k, v) }
            })
        }.toString()
    }

    /**
     * 构建心跳消息
     */
    fun buildHeartbeatMessage(
        deviceId: String,
        batteryLevel: Int,
        isCharging: Boolean,
        isScreenOn: Boolean,
        networkType: String? = null
    ): String {
        return buildBaseMessage(deviceId, TYPE_HEARTBEAT).apply {
            put(KEY_DATA, JSONObject().apply {
                put("batteryLevel", batteryLevel)
                put("isCharging", isCharging)
                put("isScreenOn", isScreenOn)
                put("heartbeatState", HeartBeatConfig.getStateDescription(isCharging, isScreenOn, batteryLevel))
                networkType?.let { put("networkType", it) }
            })
        }.toString()
    }

    /**
     * 构建告警消息
     */
    fun buildAlertMessage(
        deviceId: String,
        alertType: String,
        alertLevel: String,
        message: String,
        extra: Map<String, Any>? = null
    ): String {
        return buildBaseMessage(deviceId, TYPE_ALERT).apply {
            put(KEY_DATA, JSONObject().apply {
                put("alertType", alertType)
                put("alertLevel", alertLevel)
                put("message", message)
                extra?.forEach { (k, v) -> put(k, v) }
            })
        }.toString()
    }

    /**
     * 构建命令执行结果消息
     */
    fun buildCommandResultMessage(
        deviceId: String,
        commandId: String,
        success: Boolean,
        message: String? = null,
        data: Map<String, Any>? = null
    ): String {
        return buildBaseMessage(deviceId, TYPE_COMMAND_RESULT).apply {
            put(KEY_DATA, JSONObject().apply {
                put("commandId", commandId)
                put(KEY_RESULT, success)
                message?.let { put("message", it) }
                data?.forEach { (k, v) -> put(k, v) }
            })
        }.toString()
    }

    /**
     * 构建设备注册消息
     */
    fun buildRegisterMessage(
        deviceId: String,
        deviceInfo: Map<String, Any>
    ): String {
        return buildBaseMessage(deviceId, TYPE_REGISTER).apply {
            put(KEY_DATA, JSONObject(deviceInfo))
        }.toString()
    }

    /**
     * 构建基础消息结构
     */
    private fun buildBaseMessage(deviceId: String, messageType: String): JSONObject {
        return JSONObject().apply {
            put(KEY_DEVICE_ID, deviceId)
            put(KEY_TIMESTAMP, System.currentTimeMillis())
            put(KEY_MESSAGE_TYPE, messageType)
        }
    }

    /**
     * 解析消息类型
     */
    fun parseMessageType(message: String): String? {
        return try {
            JSONObject(message).optString(KEY_MESSAGE_TYPE)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析消息数据
     */
    fun parseMessageData(message: String): JSONObject? {
        return try {
            JSONObject(message).optJSONObject(KEY_DATA)
        } catch (e: Exception) {
            null
        }
    }
}
