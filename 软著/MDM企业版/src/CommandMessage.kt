package com.siyu.mdm.enterprise.util.mqtt.command

import com.siyu.mdm.enterprise.util.LogUtils
import org.json.JSONObject

/**
 * MQTT命令消息
 * 
 * 解析服务器下发的命令消息
 * 
 * 消息格式：
 * ```json
 * {
 *   "commandId": "uuid-string",
 *   "action": "lock|unlock|wipe|...",
 *   "params": {...},
 *   "timestamp": 1704067200000
 * }
 * ```
 */
data class CommandMessage(
    val commandId: String,
    val action: String,
    val params: JSONObject?,
    val timestamp: Long
) {
    companion object {
        private const val TAG = "CommandMessage"

        /**
         * 解析JSON字符串为CommandMessage
         */
        fun fromJson(jsonString: String): CommandMessage? {
            return try {
                val json = JSONObject(jsonString)
                val commandId = json.optString("commandId", "")
                val action = json.optString("action", "")
                val params = json.optJSONObject("params")
                val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                if (commandId.isEmpty() || action.isEmpty()) {
                    LogUtils.e(TAG, "命令解析失败: commandId或action无效")
                    return null
                }

                CommandMessage(
                    commandId = commandId,
                    action = action,
                    params = params,
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                LogUtils.e(TAG, "命令JSON解析失败: $jsonString", e)
                null
            }
        }
    }

    /**
     * 获取参数值
     */
    fun getString(key: String, default: String = ""): String {
        return params?.optString(key, default) ?: default
    }

    fun getInt(key: String, default: Int = 0): Int {
        return params?.optInt(key, default) ?: default
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return params?.optBoolean(key, default) ?: default
    }
}
