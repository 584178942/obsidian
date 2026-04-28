package com.siyu.mdm.custom.device.util.mqtt

/**
 * MQTT 主题统一管理类
 * 所有MQTT通信主题在此集中定义，避免硬编码和重复定义
 * 按业务类型分类，便于查找和维护
 */
object MqttTopicManager {
    // ========================== 系统级主题（所有设备共享） ==========================
    /** 设备离线遗嘱主题（用于设备异常断开时自动发送离线通知） */
    const val DEVICE_WILL = "mdm/report"

    /** 系统广播主题（服务器向所有设备发送广播指令） */
    const val SYSTEM_BROADCAST = "system/broadcast"

    /** 系统通知主题（如服务器维护通知、版本更新通知等） */
    const val SYSTEM_NOTIFICATION = "system/notification"


    // ========================== 设备级通用主题（按功能分类） ==========================
    /** 设备状态上报主题（设备主动上报在线状态、电量等信息） */
    const val DEVICE_STATUS_REPORT = "device/status/report"

    /** 设备事件上报主题（设备发生重要事件时上报，如异常重启、网络切换等） */
    const val DEVICE_EVENT_REPORT = "device/event/report"

    /** 设备固件更新主题（服务器下发固件更新指令） */
    const val DEVICE_FIRMWARE_UPDATE = "device/firmware/update"


    // ========================== 设备专属主题（需动态拼接设备ID） ==========================
    /**
     * 获取设备私有指令主题（服务器向指定设备发送单独指令）
     * 格式：device/{设备唯一标识}
     * @param deviceId 设备唯一标识（如IMEI）
     */
    fun getDevicePrivateCommandTopic(deviceId: String): String {
        return "mdm/$deviceId"
    }

    /**
     * 获取设备响应指令结果主题（设备向服务器回复指令执行结果）
     * 格式：device/command/result/{设备唯一标识}
     * @param deviceId 设备唯一标识（如IMEI）
     */
    fun getDeviceCommandResultTopic(deviceId: String): String {
        return "mdm/command/$deviceId"
    }

    /**
     * 获取设备配置同步主题（服务器向指定设备同步配置信息）
     * 格式：device/config/sync/{设备唯一标识}
     * @param deviceId 设备唯一标识（如IMEI）
     */
    fun getDeviceConfigTopic(deviceId: String): String {
        return "device/config/$deviceId"
    }


    // ========================== 应用管理相关主题 ==========================
    /** 应用安装指令主题（服务器下发应用安装指令） */
    const val APP_INSTALL_COMMAND = "app/install/command"

    /** 应用安装结果上报主题（设备上报应用安装结果） */
    const val APP_INSTALL_RESULT = "app/install/result"

    /** 应用卸载指令主题（服务器下发应用卸载指令） */
    const val APP_UNINSTALL_COMMAND = "app/uninstall/command"


    // ========================== 辅助方法（可选） ==========================
    /**
     * 验证主题格式是否合法
     * @param topic 待验证的主题
     * @return true=合法，false=非法
     */
    fun isValidTopic(topic: String): Boolean {
        if (topic.isBlank()) return false
        // 主题不能包含通配符（发布时）
        if (topic.contains("+") || topic.contains("#")) return false
        // 主题长度限制（MQTT协议建议不超过65535字节）
        if (topic.length > 1000) return false
        return true
    }
}
