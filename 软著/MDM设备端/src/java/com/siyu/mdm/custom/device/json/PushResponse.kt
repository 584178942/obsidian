package com.siyu.mdm.custom.device.json
class PushResponse {
    data class PushResponse(
        // 响应状态（如"ok"表示成功）
        var status: String? = null,
        // 推送消息列表（可能为null或空列表）
        var data: List<PushMessage>? = null
    )
}