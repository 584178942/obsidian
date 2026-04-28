package com.siyu.mdm.custom.device.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.service.MqttService
import com.siyu.mdm.custom.device.util.AppConstants.LOCK
import com.siyu.mdm.custom.device.util.AppConstants.LOCK_STATE
import com.siyu.mdm.custom.device.util.AppConstants.UN_LOCK
import com.siyu.mdm.custom.device.util.HeartBeatAlarmUtil.startHeartBeatAlarm
import com.siyu.mdm.custom.device.util.InitiateUtils
import com.siyu.mdm.custom.device.util.StorageUtil
import java.util.Objects

/**
 * 开机自启
 * @author Z T
 * @date 20200924
 */
open class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LogUtils.i( "ACTION_BOOT_COMPLETED","接收到系统开机广播")
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            LogUtils.i( "ACTION_BOOT_COMPLETED","接收到系统开机广播，准备启动应用...");
            //startHeartBeatAlarm(true)
            val intent2 = Intent(context, MqttService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent2)
            } else {
                context.startService(intent2)
            }

            if (Objects.equals(StorageUtil.getString(LOCK_STATE, UN_LOCK), LOCK)){
                InitiateUtils().startLockActivity();
            }

        }
    }
}
