package com.siyu.mdm.custom.device.json
import org.json.JSONException
import org.json.JSONObject

/**
 * MDM推送消息模型类
 * 封装服务器下发的推送指令类型和负载数据
 */
open class PushMessage(
    var messageType: String? = null,
    var code: String? = null,
    open var data: JSONObject? = null
) {
    companion object {
        // 推送消息类型常量（与服务器端定义保持一致）
        const val TYPE_CONFIG_UPDATING = "configUpdating"       // 配置更新中
        const val TYPE_CONFIG_UPDATED = "configUpdated"         // 配置已更新

        const val TYPE_OPERATOR = "operator"         // 配置项

        const val TYPE_RUN_APP = "runApp"                       // 运行应用
        const val TYPE_UNINSTALL_APP = "uninstallApp"           // 卸载应用
        const val TYPE_DELETE_FILE = "deleteFile"               // 删除文件
        const val TYPE_PURGE_DIR = "purgeDir"                   // 清空目录
        const val TYPE_DELETE_DIR = "deleteDir"                 // 删除目录
        const val TYPE_PERMISSIVE_MODE = "permissiveMode"       // 宽容模式
        const val TYPE_RUN_COMMAND = "runCommand"               // 执行命令
        const val TYPE_REBOOT = "reboot"                         // 重启设备
        const val TYPE_EXIT_KIOSK = "exitKiosk"                 // 退出 kiosk 模式
        const val TYPE_CLEAR_DOWNLOADS = "clearDownloadHistory" // 清除下载历史
        const val TYPE_INTENT = "intent"                         // 发送意图
        const val TYPE_GRANT_PERMISSIONS = "grantPermissions"   // 授予权限
        const val TYPE_ADMIN_PANEL = "adminPanel"               // 管理员面板
    }

    /**
     * 将data字符串转换为JSONObject
     * @return 转换后的JSONObject，若转换失败或data为空则返回null
     */
    open fun getDataJSON(): JSONObject? {
        return try {
            data?.let { it}
        } catch (e: JSONException) {
            // 无效的JSON格式时返回null
            null
        }
    }
}
