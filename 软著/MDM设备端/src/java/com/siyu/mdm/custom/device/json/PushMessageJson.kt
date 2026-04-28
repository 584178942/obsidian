package com.siyu.mdm.custom.device.json

import org.json.JSONObject

/**
 * 扩展自PushMessage，直接存储JSON格式的payload（避免重复解析）
 * 用于已解析JSON payload的场景，提升处理效率
 */
class PushMessageJson : PushMessage {
    // 带参构造函数（直接初始化消息类型和JSON payload）
    constructor(messageType: String?, data: JSONObject?) : super() {
        this.messageType = messageType
        this.data = data
    }

    /**
     * 重写父类方法，直接返回预解析的JSON对象（无需再次解析字符串）
     */
    fun getPayloadJSON(): JSONObject? = data

    /**
     * 设置JSON格式的payload
     */
    fun setPayloadJSON(data: JSONObject?) {
        this.data = data
    }
}
