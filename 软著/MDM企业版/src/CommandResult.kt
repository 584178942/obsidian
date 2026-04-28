package com.siyu.mdm.enterprise.util.mqtt.command

/**
 * 命令执行结果
 * 
 * 用于封装命令执行的状态和结果信息，并转换为JSON用于MQTT上报
 */
data class CommandResult(
    val commandId: String,
    val action: String,
    val success: Boolean,
    val message: String,
    val data: Map<String, Any>? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 转换为JSON字符串用于MQTT上报
     */
    fun toJson(): String {
        return buildString {
            append("{")
            append("\"commandId\":\"$commandId\",")
            append("\"action\":\"$action\",")
            append("\"success\":$success,")
            append("\"message\":\"$message\",")
            append("\"timestamp\":$timestamp")
            data?.let {
                if (it.isNotEmpty()) {
                    append(",\"data\":{")
                    append(it.entries.joinToString(",") { (key, value) ->
                        "\"$key\":${formatValue(value)}"
                    })
                    append("}")
                }
            }
            append("}")
        }
    }

    private fun formatValue(value: Any): String {
        return when (value) {
            is String -> "\"$value\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            else -> "\"$value\""
        }
    }

    companion object {
        /**
         * 创建成功结果
         */
        fun success(commandId: String, action: String, message: String = "执行成功"): CommandResult {
            return CommandResult(commandId, action, true, message)
        }

        /**
         * 创建失败结果
         */
        fun failure(commandId: String, action: String, message: String, e: Exception? = null): CommandResult {
            val errorMsg = if (e != null) "$message: ${e.message}" else message
            return CommandResult(commandId, action, false, errorMsg)
        }

        /**
         * 创建查询结果
         */
        fun queryResult(commandId: String, action: String, data: Map<String, Any>): CommandResult {
            return CommandResult(commandId, action, true, "查询成功", data)
        }
    }
}
