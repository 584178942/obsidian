package com.siyu.mdm.enterprise

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.siyu.mdm.enterprise.util.LogUtils

/**
 * 设备管理员Receiver
 * 
 * 处理设备管理员启用/禁用事件
 * 支持华为MDM设备的激活和去激活回调
 */
class SampleDeviceReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "SampleDeviceReceiver"
        
        /** 设备管理员已启用广播 */
        const val ACTION_ADMIN_ENABLED = "com.siyu.mdm.enterprise.action.ADMIN_ENABLED"
        
        /** 设备管理员已禁用广播 */
        const val ACTION_ADMIN_DISABLED = "com.siyu.mdm.enterprise.action.ADMIN_DISABLED"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        
        LogUtils.i(TAG, "======================================")
        LogUtils.i(TAG, "设备管理员已启用")
        LogUtils.i(TAG, "组件: ${intent.component}")
        LogUtils.i(TAG, "华为MDM功能已激活")
        LogUtils.i(TAG, "======================================")
        
        Toast.makeText(
            context,
            "设备管理员已启用，企业管理功能已激活",
            Toast.LENGTH_SHORT
        ).show()
        
        // 发送广播通知应用设备管理员已激活
        context.sendBroadcast(Intent(ACTION_ADMIN_ENABLED))
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        
        LogUtils.w(TAG, "======================================")
        LogUtils.w(TAG, "设备管理员已禁用")
        LogUtils.w(TAG, "华为MDM功能已停用")
        LogUtils.w(TAG, "======================================")
        
        Toast.makeText(
            context,
            "设备管理员已禁用，企业管理功能已停用",
            Toast.LENGTH_SHORT
        ).show()
        
        // 发送广播通知应用设备管理员已禁用
        context.sendBroadcast(Intent(ACTION_ADMIN_DISABLED))
    }

    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, userHandle)
        LogUtils.w(TAG, "密码验证失败")
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
        LogUtils.i(TAG, "密码验证成功")
    }

    override fun onPasswordChanged(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordChanged(context, intent, userHandle)
        LogUtils.i(TAG, "密码已更改")
    }

    override fun onPasswordExpiring(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordExpiring(context, intent, userHandle)
        LogUtils.w(TAG, "密码即将过期")
    }


    override fun onUserAdded(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onUserAdded(context, intent, userHandle)
        LogUtils.i(TAG, "新用户已添加: $userHandle")
    }

    override fun onUserRemoved(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onUserRemoved(context, intent, userHandle)
        LogUtils.w(TAG, "用户已被移除: $userHandle")
    }

    override fun onUserSwitched(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onUserSwitched(context, intent, userHandle)
        LogUtils.i(TAG, "用户已切换: $userHandle")
    }

   /* override fun onProfileAdded(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onProfileAdded(context, intent, userHandle)
        LogUtils.i(TAG, "工作资料已添加: $userHandle")
    }

    override fun onProfileRemoved(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onProfileRemoved(context, intent, userHandle)
        LogUtils.w(TAG, "工作资料已被移除: $userHandle")
    }*/

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        LogUtils.w(TAG, "收到设备管理员禁用请求")
        
        return "禁用设备管理员将导致企业管理功能不可用，确定要继续吗？"
    }
/*
    override fun onDisabledByUser(context: Context, intent: Intent) {
        super.onDisabledByUser(context, intent)
        LogUtils.w(TAG, "设备管理员已被用户禁用")
    }

    override fun onEnabledByUser(context: Context, intent: Intent) {
        super.onEnabledByUser(context, intent)
        LogUtils.i(TAG, "设备管理员已被用户启用")
    }*/
}
