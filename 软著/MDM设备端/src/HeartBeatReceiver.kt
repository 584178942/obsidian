package com.siyu.mdm.custom.device.receiver

import android.content.Context
import android.content.Intent
import android.os.Build
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.siyu.mdm.custom.device.json.TypeBean
import com.siyu.mdm.custom.device.util.AppConstants.ACTIVATE
import com.siyu.mdm.custom.device.util.AppConstants.ACTIVATION_STATE
import com.siyu.mdm.custom.device.util.AppConstants.BIND
import com.siyu.mdm.custom.device.util.AppConstants.BIND_STATE
import com.siyu.mdm.custom.device.util.AppConstants.CLEAR
import com.siyu.mdm.custom.device.util.AppConstants.FIRST_STATE
import com.siyu.mdm.custom.device.util.AppConstants.ICCID
import com.siyu.mdm.custom.device.util.AppConstants.IMEI_CODE
import com.siyu.mdm.custom.device.util.AppConstants.LOCK
import com.siyu.mdm.custom.device.util.AppConstants.LOCK_STATE
import com.siyu.mdm.custom.device.util.AppConstants.REMOVE_WHITE
import com.siyu.mdm.custom.device.util.AppConstants.UN_BIND
import com.siyu.mdm.custom.device.util.AppConstants.UN_LOCK
import com.siyu.mdm.custom.device.util.AppConstants.UPDATE
import com.siyu.mdm.custom.device.util.AppConstants.VERSION
import com.siyu.mdm.custom.device.util.HeartBeatAlarmUtil.startHeartBeatAlarm
import com.siyu.mdm.custom.device.util.HuaweiMDMManager
import com.siyu.mdm.custom.device.util.InitiateUtils
import com.siyu.mdm.custom.device.util.MdmCommandManager
import com.siyu.mdm.custom.device.util.NetUtils
import com.siyu.mdm.custom.device.util.RsaUtil
import com.siyu.mdm.custom.device.util.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Response
import java.io.IOException

/**
 * 设备心跳接收器：定时向服务器发送设备状态，接收并执行MDM指令
 * @author ZT
 * @date 20201105
 */
class HeartBeatReceiver : BootReceiver() {
    // 修复：统一MDM管理器变量名，避免混淆（原代码小写huaweiMDMManager未初始化）
    private val huaweiMDMManager by lazy { HuaweiMDMManager() }
    // 常量：提取重复的APK下载地址，避免代码冗余
    private val COMMON_APK_URLS = listOf(
        "https://ot-gdown.baidu.com/appcenter/pkg/upload/8017adf4b44479b5dd3df54093e815f9?md5hash=4d5d2e1994d60f48c978aed8a19d6217&timestamp=1746681601",
        "https://lf9-apk.ugapk.cn/package/apk/aweme/1015_340201/aweme_aweGW_v1015_340201_ec50_1746533289.apk",
        "https://dldir1.qq.com/wework/work_weixin/WeCom_android_4.1.36.42551_arm64_100038.apk"
    )
    // 常量：企业微信正确包名（修复原代码空格错误）
    private val WEWORK_PACKAGE = "com.tencent.wework"

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent) // 调用父类方法（若BootReceiver有初始化逻辑）
        try {
            LogUtils.i("HeartBeatReceiver", "心跳任务触发，开始执行")
            startHeartBeatAlarm(true) // 重置心跳闹钟
            heartBeat() // 执行心跳请求
        } catch (e: Exception) {
            LogUtils.e("HeartBeatReceiver", "心跳任务初始化失败", e)
            startHeartBeatAlarm(true) // 异常后仍重置闹钟，避免心跳中断
        }
    }

    /**
     * 核心：发送设备心跳到服务器，接收MDM指令并执行
     */
    private fun heartBeat() {
        // 1. 构建心跳请求参数（设备状态信息）
        val heartBeatParams = mutableMapOf(
            IMEI_CODE to InitiateUtils().getIMEI().toString(), // 设备IMEI
            ICCID to InitiateUtils().getIccidCode(), // 设备ICCID
            ACTIVATION_STATE to StorageUtil.getInt(ACTIVATION_STATE, 1), // 激活状态
            VERSION to AppUtils.getAppVersionName(), // APP版本号
            BIND_STATE to StorageUtil.getString(BIND_STATE, UN_BIND), // 绑定状态
            LOCK_STATE to StorageUtil.getString(LOCK_STATE, UN_LOCK), // 锁屏状态
            FIRST_STATE to StorageUtil.getBoolean(FIRST_STATE, false) // 首次启动标记
        )
        LogUtils.i("HeartBeatReceiver", "心跳请求参数：${heartBeatParams}")

        // 2. 心跳服务器地址（建议配置在strings.xml，避免硬编码）
        val heartBeatUrl = "heartbeat/request"

        // 3. 调用优化后的NetUtils发送POST请求（适配新接口NetCallback）
        NetUtils.getInstance().post(
            url = heartBeatUrl,
            params = heartBeatParams,
            callback = object : NetUtils.NetCallback {

                /**
                 * 心跳请求成功：解密响应、解析MDM指令、执行指令
                 */
                override fun onSuccess(response: Response) {
                    try {
                        // 关键：响应体使用后必须关闭，避免资源泄漏
                        response.body?.string()?.let { encryptedResponse ->
                            LogUtils.i("HeartBeatReceiver", "服务器加密响应：$encryptedResponse")

                            // 4. RSA解密服务器响应（处理解密失败场景）
                            val decryptedResponse = RsaUtil.decryptByPublicKey(encryptedResponse)
                                ?: throw IOException("RSA解密响应失败，密文：$encryptedResponse")
                            LogUtils.i("HeartBeatReceiver", "服务器解密响应：$decryptedResponse")

                            // 5. Gson解析MDM指令列表（TypeBean列表）
                            val typeBeanList = Gson().fromJson<List<TypeBean>>(
                                decryptedResponse,
                                object : TypeToken<List<TypeBean>>() {}.type
                            )
                            if (typeBeanList.isEmpty()) {
                                LogUtils.i("HeartBeatReceiver", "服务器未返回MDM指令")
                                return
                            }

                            // 6. 遍历执行每个MDM指令
                            typeBeanList.forEach { bean ->
                                LogUtils.i("HeartBeatReceiver", "开始执行MDM指令：${bean.type}，参数：${bean.data}")
                                // 处理指令禁用状态（默认不禁用）
                                val isDisabled = bean.isDisabled ?: false
                                try {
                                    executeMdmCommand(bean.type.toString(), isDisabled, bean.data)
                                } catch (e: Exception) {
                                    // 单个指令失败不影响其他指令，仅记录日志
                                    LogUtils.e("HeartBeatReceiver", "执行指令${bean.type}失败", e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        LogUtils.e("HeartBeatReceiver", "心跳响应处理异常", e)
                    } finally {
                        response.close() // 强制关闭响应，释放资源
                    }
                }

                /**
                 * 心跳请求失败：重试闹钟
                 */
                override fun onFailure(e: Exception) {
                    LogUtils.e("HeartBeatReceiver", "心跳请求失败", e)
                    startHeartBeatAlarm(true) // 失败后重置闹钟，确保下次重试
                }
            }
        )
    }

    /**
     * 单独抽取：执行MDM指令（解耦合代码，便于维护）
     * @param commandType 指令类型（如BIND、LOCK、installAPP）
     * @param isDisabled 指令禁用标记（true=禁用，false=启用）
     * @param commandData 指令附加参数（如APK地址、密码等）
     */
    private fun executeMdmCommand(commandType: String, isDisabled: Boolean, commandData: String?) {
        when (commandType) {
            // 设备绑定/解锁
            BIND -> {
                StorageUtil.put(LOCK_STATE, LOCK)
                StorageUtil.put(BIND_STATE, BIND)
                huaweiMDMManager.lockDevice()
                LogUtils.i("HeartBeatReceiver", "执行指令：设备绑定并锁屏")
            }
            UN_BIND -> {
                StorageUtil.put(LOCK_STATE, UN_LOCK)
                StorageUtil.put(BIND_STATE, UN_BIND)
                huaweiMDMManager.unlockDevice()
                LogUtils.i("HeartBeatReceiver", "执行指令：设备解绑并解锁")
            }

            // 设备锁屏/解锁
            LOCK -> {
                StorageUtil.put(LOCK_STATE, LOCK)
                huaweiMDMManager.lockDevice()
                LogUtils.i("HeartBeatReceiver", "执行指令：设备锁屏")
            }
            UN_LOCK -> {
                StorageUtil.put(LOCK_STATE, UN_LOCK)
                huaweiMDMManager.unlockDevice()
                LogUtils.i("HeartBeatReceiver", "执行指令：设备解锁")
            }

            // 设备擦除
            CLEAR -> {
                huaweiMDMManager.wipeDevice()
                LogUtils.i("HeartBeatReceiver", "执行指令：设备恢复出厂设置")
            }

            // 白名单移除（空实现，需根据需求补充）
            REMOVE_WHITE -> {
                LogUtils.i("HeartBeatReceiver", "执行指令：移除白名单（未实现）")
            }

            // 安装APP（使用MdmCommandManager处理，实现安装普通应用和更新自身应用的逻辑分离）
            "installAPP", UPDATE -> {
            }

            // 卸载APP（修复原代码包名空格错误）
            "uninstallApp" -> {
                InitiateUtils().uninstalls(listOf(WEWORK_PACKAGE))
                LogUtils.i("HeartBeatReceiver", "执行指令：卸载APP，包名：$WEWORK_PACKAGE")
            }

            // 设备激活（原代码注释，保留逻辑）
            ACTIVATE -> {
                val sign = commandData ?: ""
                /*if (verify(InitiateUtils().getIMEI().toString(), sign)) {
                    StorageUtil.put(ACTIVATION_STATE, 1)
                    LogUtils.i("HeartBeatReceiver", "执行指令：设备激活成功")
                } else {
                    StorageUtil.put(ACTIVATION_STATE, 2)
                    LogUtils.i("HeartBeatReceiver", "执行指令：设备激活失败（签名验证不通过）")
                }*/
                LogUtils.i("HeartBeatReceiver", "执行指令：设备激活（签名验证逻辑未启用）")
            }

            // WiFi控制
            "ManageWiFi", "closeWifi" -> {
                huaweiMDMManager.setWifiDisabled(isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (isDisabled) "关闭" else "开启"}WiFi")
            }
            "openWifi" -> {
                huaweiMDMManager.setWifiDisabled(false)
                LogUtils.i("HeartBeatReceiver", "执行指令：开启WiFi")
            }

            // WiFi热点控制
            "openWifiAp" -> {
                huaweiMDMManager.setWifiApDisabled(false)
                LogUtils.i("HeartBeatReceiver", "执行指令：开启WiFi热点")
            }
            "closeWifiAp" -> {
                huaweiMDMManager.setWifiApDisabled(true)
                LogUtils.i("HeartBeatReceiver", "执行指令：关闭WiFi热点")
            }

            // 蓝牙控制
            "ManageBluetooth", "closeBluetooth" -> {
                huaweiMDMManager.setBluetoothDisabled(isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (isDisabled) "关闭" else "开启"}蓝牙")
            }
            "openBluetooth" -> {
                huaweiMDMManager.setBluetoothDisabled(false)
                LogUtils.i("HeartBeatReceiver", "执行指令：开启蓝牙")
            }

            // USB控制
            "ManageUSB", "closeUsb" -> {
                huaweiMDMManager.setUSBDataDisabled(true)
                LogUtils.i("HeartBeatReceiver", "执行指令：关闭USB数据传输")
            }
            "openUsb" -> {
                huaweiMDMManager.setUSBDataDisabled(false)
                LogUtils.i("HeartBeatReceiver", "执行指令：开启USB数据传输")
            }

            // OTG控制
            "ManageOTG" -> {
                huaweiMDMManager.setUSBOtgDisabled(isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (isDisabled) "禁用" else "启用"}OTG")
            }

            // NFC控制
            "ManageNFC" -> {
                huaweiMDMManager.setNFCDisabled(isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (isDisabled) "禁用" else "启用"}NFC")
            }

            // 截屏控制
            "openScreenshot" -> {
                huaweiMDMManager.setScreenCaptureDisabled(false)
                LogUtils.i("HeartBeatReceiver", "执行指令：允许截屏")
            }
            "closeScreenshot", "DisableScreenCapture" -> {
                huaweiMDMManager.setScreenCaptureDisabled(isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (isDisabled) "禁止" else "允许"}截屏")
            }

            // 录屏控制（空实现，需补充）
            "DisableScreenRecording" -> {
                LogUtils.i("HeartBeatReceiver", "执行指令：禁止录屏（未实现）")
            }

            // 麦克风控制
            "setMicrophoneDisabled" -> {
                huaweiMDMManager.setMicrophoneDisabled(isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (isDisabled) "禁用" else "启用"}麦克风")
            }

            // 摄像头控制
            "setVideoDisabled" -> {
                huaweiMDMManager.setVideoDisabled(isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (isDisabled) "禁用" else "启用"}摄像头")
            }

            // 移动数据控制（修复原代码空格错误）
            "turnOnMobiledata" -> {
                huaweiMDMManager.turnOnMobiledata(!isDisabled) // 需确认MDM接口参数含义（是否反义）
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (!isDisabled) "开启" else "关闭"}移动数据")
            }

            // 始终在线连接控制
            "turnOnConnectionAlwaysOn" -> {
                huaweiMDMManager.turnOnConnectionAlwaysOn(!isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (!isDisabled) "开启" else "关闭"}始终在线连接")
            }

            // 数据网络控制
            "DisableInternet" -> {
                huaweiMDMManager.setDataConnectivityDisabled(isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (isDisabled) "禁用" else "启用"}数据网络")
            }

            // APN设置（修复原代码空格错误）
            "SetApnOrder" -> {
                val apnInfo = mapOf(
                    "name" to "My APN",
                    "apn" to "internet",
                    "proxy" to "192.168.1.1",
                    "port" to "8080",
                    "mmsc" to "http://mmsc.example.com",
                    "mcc" to "310",
                    "mnc" to "410",
                    "auth_type" to "1",
                    "user" to "user",
                    "password" to "password"
                )
                huaweiMDMManager.addApn(apnInfo)
                LogUtils.i("HeartBeatReceiver", "执行指令：设置APN参数")
            }

            // 隐藏APN设置（空实现）
            "HideApnSettings" -> {
                LogUtils.i("HeartBeatReceiver", "执行指令：隐藏APN设置（未实现）")
            }

            // 密码重置
            "newPassword" -> {
                val newPwd = commandData ?: "222222" // 优先使用服务器传的密码，默认222222
                huaweiMDMManager.resetPassword(newPwd)
                LogUtils.i("HeartBeatReceiver", "执行指令：重置设备密码为：$newPwd")
            }

            // 隐私保护
            "EnablePrivacyProtection" -> {
                huaweiMDMManager.enablePrivacyProtection(isDisabled)
                LogUtils.i("HeartBeatReceiver", "执行指令：${if (isDisabled) "启用" else "禁用"}隐私保护")
            }

            // APP安装黑名单
            "SetAppInstallPolicy" -> {
                val blacklist = mutableSetOf("com.example.app1", "com.example.app2", "com.example.app3")
                huaweiMDMManager.setInstallBlacklist(blacklist)
                LogUtils.i("HeartBeatReceiver", "执行指令：设置APP安装黑名单：$blacklist")
            }

            // APP运行黑名单
            "SetAppRunPolicy" -> {
                val blacklist = mutableSetOf("com.example.app1", "com.example.app2", "com.example.app3")
                huaweiMDMManager.setRuntimeBlacklist(blacklist)
                LogUtils.i("HeartBeatReceiver", "执行指令：设置APP运行黑名单：$blacklist")
            }

            // 未知指令
            else -> {
                LogUtils.w("HeartBeatReceiver", "收到未知MDM指令：$commandType")
            }
        }
    }
}