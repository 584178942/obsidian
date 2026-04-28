package com.siyu.mdm.custom.device.util.mqtt


import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.siyu.mdm.custom.device.util.CommandResult
import com.siyu.mdm.custom.device.util.InitiateUtils
import com.siyu.mdm.custom.device.util.StorageUtil
import org.json.JSONException
import org.json.JSONObject

/**
 * MQTT工具类，封装遗嘱消息构建等通用功能
 */
object MqttUtils {
    private const val TAG = "MqttUtils"
    private const val KEY_SERVER_URI = "server_uri"

    // 默认服务器地址
    private const val DEFAULT_SERVER_URI = "8.136.228.195:40820"
    /**
     * 构建MQTT遗嘱消息（Will Message）
     * 用于设备异常断开连接时，服务器自动接收离线通知
     *
     * @param deviceId 设备唯一标识（如IMEI，必传）
     * @param action 动作类型（默认"offline"表示离线，可传"register"表示注册）
     * @return 构建成功的JSON字符串，失败返回空字符串
     */
    fun buildMessage(deviceId: String, action: String = "offline"): String {
        // 1. 必传参数校验：设备ID不能为空
        if (deviceId.isBlank()) {
            LogUtils.e(TAG, "构建消息失败：设备ID为空")
            return ""
        }

        // 2. 构建JSON消息体
        return try {
            JSONObject()
                .put("action", action)   // 动作类型：offline/register等
                .put("code", deviceId)   // 设备唯一标识
                .put("iccid", Gson().toJson(InitiateUtils().getIccidCode()))
                .toString()              // 生成紧凑JSON格式
        } catch (e: JSONException) {
            // 捕获JSON构建异常（如参数类型错误）
            LogUtils.e(TAG, "构建消息JSON失败", e)
            ""
        }
    }

    fun saveServerUri(serverUri: String) {
        StorageUtil.put(KEY_SERVER_URI, serverUri)
    }

    /**
     * 获取当前MQTT服务器地址（优先取保存的值，无则返回默认值）
     * @param context 上下文
     * @return 服务器地址字符串
     */
    fun getServerUri(): String {
        return StorageUtil.getString(KEY_SERVER_URI, DEFAULT_SERVER_URI).toString()
    }

    /**
     * 清除保存的服务器地址（恢复默认值）
     * @param context 上下文
     */
    fun clearServerUri() {
        StorageUtil.put(KEY_SERVER_URI, DEFAULT_SERVER_URI)
    }

    /**
     * 构建命令执行结果的MQTT消息
     * @param deviceId 设备唯一标识
     * @param taskId 任务ID
     * @param result 命令执行结果（Success或Failure）
     * @return 构建成功的JSON字符串，失败返回空字符串
     */
    fun buildCommandResultMessage(deviceId: String, taskId: String, result: CommandResult): String {
        if (deviceId.isBlank()) {
            LogUtils.e(TAG, "构建命令结果消息失败：设备ID为空")
            return ""
        }
        return try {
            val resultMessage = JSONObject()
            resultMessage.put("action", "operator")
            resultMessage.put("code", deviceId)
            resultMessage.put("taskId", taskId)
            when (result) {
                is CommandResult.Success -> {
                    resultMessage.put("data", true)
                    LogUtils.d(TAG, "构建命令执行成功结果: ")
                }
                is CommandResult.Failure -> {
                    resultMessage.put("data", false)
                    LogUtils.d(TAG, " : $deviceId, 错误: ${result.exception.message}")
                }
            }
            resultMessage.toString()
        } catch (e: JSONException) {
            LogUtils.e(TAG, "构建命令结果消息JSON失败", e)
            ""
        }
    }
    
    /**
     * 构建命令执行结果的MQTT消息（带命令ID）
     * @param deviceId 设备唯一标识
     * @param taskId 任务ID
     * @param result 命令执行结果（Success或Failure）
     * @param commandId 命令ID
     * @return 构建成功的JSON字符串，失败返回空字符串
     */
    fun buildCommandResultMessage(deviceId: String, taskId: String, result: CommandResult, commandId: String): String {
        if (deviceId.isBlank() || commandId.isBlank()) {
            LogUtils.e(TAG, "构建命令结果消息失败：设备ID或命令ID为空")
            return ""
        }
        return try {
            val resultMessage = JSONObject()
            resultMessage.put("action", "operator")
            resultMessage.put("code", deviceId)
            resultMessage.put("taskId", taskId)
            resultMessage.put("id", commandId)  // 添加命令ID到结果中
            when (result) {
                is CommandResult.Success -> {
                    resultMessage.put("data", "true")
                    LogUtils.d(TAG, "构建命令执行成功结果: ")
                }
                is CommandResult.Failure -> {
                    resultMessage.put("data", "false")
                    LogUtils.d(TAG, " : $deviceId, 错误: ${result.exception.message}")
                }
            }
            resultMessage.toString()
        } catch (e: JSONException) {
            LogUtils.e(TAG, "构建命令结果消息JSON失败", e)
            ""
        }
    }
}
