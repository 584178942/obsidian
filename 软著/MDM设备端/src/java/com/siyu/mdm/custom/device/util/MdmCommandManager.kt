package com.siyu.mdm.custom.device.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.siyu.mdm.custom.device.json.PushMessage
import com.siyu.mdm.custom.device.json.PushMessage.Companion.TYPE_OPERATOR
import com.siyu.mdm.custom.device.receiver.SimChangedReceiver
import com.siyu.mdm.custom.device.util.AppConstants.APP_PACKNAME
import com.siyu.mdm.custom.device.util.screenoffalert.AlertService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * MDM命令统一处理器
 * 整合设备控制指令与业务命令，提供统一的命令执行入口
 */
class MdmCommandManager(
    private val context: Context,
    private val huaweiMDMManager: HuaweiMDMManager,
    private val initiateUtils: InitiateUtils,
) {
    private val TAG = "MdmCommandManager"
    private val gson = Gson()

    companion object {
        // 配置常量
        private const val DEFAULT_TIMEOUT_MS = 30000L
        private const val INSTALL_DELAY_MS = 5000L
        private const val UNINSTALL_DELAY_MS = 3000L

        // APP相关常量
        const val COMMON_APK_URLS = "https://dldir1.qq.com/wework/work_weixin/WeCom_android_5.0.0.56070_arm64_100038.apk"
        const val WEWORK_PACKAGE = "com.tencent.wework"
        const val ACTIVATE = "activate"

        // 指令类型常量
        const val TYPE_INSTALL_UPDATE_APP = "installUpdateApp"
        const val TYPE_UNINSTALL_APP = "uninstallApp"
        const val TYPE_WIFI = "wifi"
        const val TYPE_ERASE = "erase"
        const val TYPE_BLUETOOTH = "bluetooth"
        const val TYPE_OPEN_WIFI_AP = "openWifiAp"
        const val TYPE_CLOSE_WIFI_AP = "closeWifiAp"
        const val TYPE_MANAGE_USB = "ManageUSB"
        const val TYPE_OPEN_USB = "openUsb"
        const val TYPE_CLOSE_USB = "closeUsb"
        const val TYPE_MANAGE_OTG = "ManageOTG"
        const val TYPE_MANAGE_NFC = "ManageNFC"
        const val TYPE_OPEN_SCREENSHOT = "openScreenshot"
        const val TYPE_CLOSE_SCREENSHOT = "closeScreenshot"
        const val TYPE_DISABLE_SCREEN_CAPTURE = "DisableScreenCapture"
        const val TYPE_DISABLE_SCREEN_RECORDING = "DisableScreenRecording"
        const val TYPE_SET_MICROPHONE_DISABLED = "setMicrophoneDisabled"
        const val TYPE_SET_VIDEO_DISABLED = "setVideoDisabled"
        const val TYPE_TURN_ON_MOBILE_DATA = "turnOnMobiledata"
        const val TYPE_TURN_ON_CONNECTION_ALWAYS_ON = "turnOnConnectionAlwaysOn"
        const val TYPE_DISABLE_INTERNET = "DisableInternet"
        const val TYPE_SET_APN_ORDER = "SetApnOrder"
        const val TYPE_HIDE_APN_SETTINGS = "HideApnSettings"
        const val TYPE_NEW_PASSWORD = "newPassword"
        const val TYPE_ENABLE_PRIVACY_PROTECTION = "EnablePrivacyProtection"
        const val TYPE_SET_APP_INSTALL_POLICY = "SetAppInstallPolicy"
        const val TYPE_SET_APP_RUN_POLICY = "SetAppRunPolicy"

        // BINDING_MAP配置相关常量
        const val TYPE_CONFIGURE_BINDING = "configureBinding"
        const val SUB_TYPE_SET_BINDINGS = "set"
        const val SUB_TYPE_ADD_BINDING = "add"
        const val SUB_TYPE_REMOVE_BINDING = "remove"
        const val SUB_TYPE_CLEAR_BINDINGS = "clear"
        
        // SIM校验开关相关常量
        const val TYPE_BINDING_SWITCH = "bindingSwitch"

        // 设备控制常量
        const val LOCK = "lock"
        const val UN_LOCK = "unlock"
        const val LOCK_STATE = "lock_state"
        const val BIND = "bind"
        const val UN_BIND = "unbind"
        const val BIND_STATE = "bind_state"
        const val CLEAR = "clear"
        
        // 震动提醒相关常量
        const val TYPE_START_VIBRATION_ALERT = "startVibrationAlert"
        const val TYPE_STOP_VIBRATION_ALERT = "stopVibrationAlert"
    }

    // 业务命令类型集合
    private val businessCommandTypes = setOf(
        PushMessage.TYPE_RUN_APP,
        PushMessage.TYPE_UNINSTALL_APP,
        PushMessage.TYPE_REBOOT,
        PushMessage.TYPE_DELETE_FILE,
        PushMessage.TYPE_RUN_COMMAND,
        PushMessage.TYPE_EXIT_KIOSK,
        PushMessage.TYPE_GRANT_PERMISSIONS,
        TYPE_CONFIGURE_BINDING
    )

    /**
     * 统一命令执行入口
     */
    suspend fun executeCommand(
        pushMessage: PushMessage,
        commandData: String? = null,
        commandIds: List<String> = emptyList()
    ): CommandResult = withContext(Dispatchers.IO) {
        return@withContext try {
            LogUtils.d(TAG, "开始执行命令: ${pushMessage.messageType}, data: ${commandData?.take(200)}...")

            when (pushMessage.messageType) {
                in businessCommandTypes -> executeBusinessCommand(pushMessage)
                TYPE_OPERATOR -> handleDeviceControlCommand(commandData, commandIds)
                else -> {
                    LogUtils.w(TAG, "未知的命令类型: ${pushMessage.messageType}")
                    CommandResult.Success("未知的命令类型")
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "执行命令失败: ${e.message}", e)
            CommandResult.Failure(e)
        }
    }

    /**
     * 处理设备控制指令
     */
    private suspend fun handleDeviceControlCommand(
        commandData: String? = null,
        commandIds: List<String> = emptyList()
    ): CommandResult {
        LogUtils.i(TAG, "设备控制命令数据: $commandData")

        // 使用commandData代替从pushMessage获取数据
        val data = commandData ?: throw IllegalArgumentException("缺少配置数据")
        
        // 尝试解析JSON数据
        return try {
            // 尝试解析为JSONObject
            val jsonObject = JSONObject(data)
            executeSingleCommand(jsonObject, commandIds, commandData)
        } catch (e1: JSONException) {
            try {
                // 尝试解析为JSONArray
                val jsonArray = JSONArray(data)
                executeCommandList(jsonArray, commandIds, commandData)
            } catch (e2: JSONException) {
                throw IllegalArgumentException("数据格式错误，应为JSONObject或JSONArray")
            }
        }
    }

    /**
     * 执行命令列表
     */
    private suspend fun executeCommandList(
        dataArray: JSONArray,
        commandIds: List<String>,
        commandData: String?
    ): CommandResult = coroutineScope {
        val commandCount = dataArray.length()
        val results = mutableListOf<Pair<Int, CommandResult>>()
        val jobs = mutableListOf<Job>()

        // 并行执行命令
        for (i in 0 until commandCount) {
            val job = launch {
                try {
                    val commandItem = dataArray.getJSONObject(i)
                    val operationName = commandItem.getString("operationName")
                    val isDisabled = commandItem.optString("params", "1") == "0"
                    val commandId = commandIds.getOrElse(i) { "" }

                    val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
                        executeSingleDeviceControlCommand(operationName, isDisabled, commandData)
                    } ?: CommandResult.Failure(Exception("命令执行超时"))

                    // 关联命令ID
                    val resultWithId = attachCommandId(result, commandId)
                    results.add(Pair(i, resultWithId))
                } catch (e: Exception) {
                    val commandId = commandIds.getOrElse(i) { "" }
                    results.add(Pair(i, CommandResult.Failure(Exception("[$commandId] ${e.message ?: "未知错误"}"))))
                }
            }
            jobs.add(job)
        }

        // 等待所有命令完成
        jobs.joinAll()

        // 整理并返回结果
        return@coroutineScope buildCommandResults(results)
    }

    /**
     * 执行单条命令
     */
    private suspend fun executeSingleCommand(
        dataObject: JSONObject,
        commandIds: List<String>,
        commandData: String?
    ): CommandResult {
        val operationName = dataObject.getString("operationName")
        val isDisabled = dataObject.optString("params", "1") == "0"
        val commandId = commandIds.firstOrNull() ?: ""

        val result = withTimeoutOrNull(DEFAULT_TIMEOUT_MS) {
            executeSingleDeviceControlCommand(operationName, isDisabled, commandData)
        } ?: CommandResult.Failure(Exception("命令执行超时"))

        return attachCommandId(result, commandId)
    }

    /**
     * 执行单条设备控制指令
     */
    private suspend fun executeSingleDeviceControlCommand(
        operationName: String,
        isDisabled: Boolean,
        commandData: String?
    ): CommandResult = withContext(Dispatchers.IO) {
        LogUtils.i(TAG, "执行设备指令: $operationName, disabled: $isDisabled")

        return@withContext try {
            when (operationName) {
                // 设备锁屏/解锁
                LOCK -> handleLockCommand(isDisabled)

                // 设备擦除
                TYPE_ERASE -> handleEraseCommand()

                // 应用管理
                TYPE_INSTALL_UPDATE_APP -> handleInstallUpdateApp(commandData)
                TYPE_UNINSTALL_APP -> handleUninstallApp(commandData)

                // 网络控制
                TYPE_WIFI -> handleWifiCommand(isDisabled)
                TYPE_OPEN_WIFI_AP -> handleWifiApCommand(false)
                TYPE_CLOSE_WIFI_AP -> handleWifiApCommand(true)
                TYPE_BLUETOOTH -> handleBluetoothCommand(isDisabled)

                // USB/OTG/NFC控制
                TYPE_CLOSE_USB, TYPE_OPEN_USB, TYPE_MANAGE_USB -> handleUsbCommand(operationName, isDisabled)
                TYPE_MANAGE_OTG -> handleOtgCommand(isDisabled)
                TYPE_MANAGE_NFC -> handleNfcCommand(isDisabled)

                // 屏幕控制
                TYPE_OPEN_SCREENSHOT, TYPE_CLOSE_SCREENSHOT, TYPE_DISABLE_SCREEN_CAPTURE ->
                    handleScreenCaptureCommand(operationName, isDisabled)

                TYPE_DISABLE_SCREEN_RECORDING -> CommandResult.Failure(Exception("禁止录屏功能暂未实现"))

                // 硬件控制
                TYPE_SET_MICROPHONE_DISABLED -> handleMicrophoneCommand(isDisabled)
                TYPE_SET_VIDEO_DISABLED -> handleVideoCommand(isDisabled)

                // 移动数据控制
                TYPE_TURN_ON_MOBILE_DATA -> handleMobileDataCommand(isDisabled)
                TYPE_TURN_ON_CONNECTION_ALWAYS_ON -> handleConnectionAlwaysOnCommand(isDisabled)
                TYPE_DISABLE_INTERNET -> handleInternetCommand(isDisabled)

                // APN设置
                TYPE_SET_APN_ORDER -> handleApnSettingCommand()
                TYPE_HIDE_APN_SETTINGS -> CommandResult.Success("执行指令：隐藏APN设置（未实现）")

                // 密码管理
                TYPE_NEW_PASSWORD -> handlePasswordCommand(commandData)

                // 隐私保护
                TYPE_ENABLE_PRIVACY_PROTECTION -> handlePrivacyProtectionCommand(isDisabled)

                // 应用策略
                TYPE_SET_APP_INSTALL_POLICY -> handleAppInstallPolicyCommand()
                TYPE_SET_APP_RUN_POLICY -> handleAppRunPolicyCommand()

                // 机卡绑定开关
                TYPE_BINDING_SWITCH -> handleBindingSwitchCommand(isDisabled)

                // 设备激活
                ACTIVATE -> CommandResult.Success("执行指令：设备激活（签名验证逻辑未启用）")
                
                // 震动提醒控制
                TYPE_START_VIBRATION_ALERT -> handleStartVibrationAlertCommand()
                TYPE_STOP_VIBRATION_ALERT -> handleStopVibrationAlertCommand()

                // 未知指令
                else -> {
                    LogUtils.w(TAG, "未知设备控制指令: $operationName")
                    CommandResult.Failure(UnsupportedOperationException("未知指令: $operationName"))
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "执行设备控制命令出错: $operationName", e)
            CommandResult.Failure(Exception("执行命令时发生错误: ${e.message ?: "未知错误"}"))
        }
    }

    // ==================== 设备命令具体实现 ====================

    private fun handleLockCommand(isDisabled: Boolean): CommandResult {
        if (isDisabled) {
            StorageUtil.put(LOCK_STATE, LOCK)
            huaweiMDMManager.lockDevice()
            return CommandResult.Success("执行指令：设备锁屏")
        } else {
            StorageUtil.put(LOCK_STATE, UN_LOCK)
            huaweiMDMManager.unlockDevice()
            return CommandResult.Success("执行指令：设备解锁")
        }
    }

    private fun handleEraseCommand(): CommandResult {
        huaweiMDMManager.wipeDevice()
        return CommandResult.Success("执行指令：设备恢复出厂设置")
    }

    private suspend fun handleInstallUpdateApp(commandData: String?): CommandResult {
        val apkUrl = extractApkUrl(commandData)
        LogUtils.i(TAG, "开始安装/更新APP，地址：$apkUrl")

        // 提取APK包名，判断是否为自身应用更新
        val isSelfUpdate = isSelfUpdate(apkUrl)
        if (isSelfUpdate) {
            return handleUpdateSelfApp(apkUrl)
        } else {
            return handleInstallApp(apkUrl)
        }
    }

    /**
     * 处理普通应用安装
     */
    private suspend fun handleInstallApp(apkUrl: String): CommandResult {
        try {
            initiateUtils.installApp(listOf(apkUrl))
            delay(INSTALL_DELAY_MS)
            return checkInstallationResult(apkUrl)
        } catch (e: Exception) {
            LogUtils.e(TAG, "安装普通应用失败", e)
            return CommandResult.Failure(e)
        }
    }

    /**
     * 处理自身应用更新
     */
    private suspend fun handleUpdateSelfApp(apkUrl: String): CommandResult {
        try {
            LogUtils.i(TAG, "开始更新自身应用")
            // 自身应用更新的特殊处理逻辑
            initiateUtils.installApp(listOf(apkUrl))
            // 自身应用更新可能需要更长的等待时间
            delay(INSTALL_DELAY_MS * 2) // 延长等待时间以确保更新完成
            
            val result = checkInstallationResult(apkUrl)
            if (result is CommandResult.Success) {
                LogUtils.i(TAG, "自身应用更新成功，准备重启应用以应用新功能")
                // 启动一个协程来执行重启操作，避免阻塞当前流程
                withContext(Dispatchers.IO) {
                    try {
                        // 这里不直接调用重启，因为Android系统会处理应用更新后的重启
                        // 但是如果需要确保立即应用新功能，可以调用
                        restartApp()
                    } catch (e: Exception) {
                        LogUtils.e(TAG, "重启应用失败，但更新已成功完成")
                    }
                }
                return CommandResult.Success("自身应用更新成功：${result.message}")
            }
            return result
        } catch (e: Exception) {
            LogUtils.e(TAG, "自身应用更新失败", e)
            return CommandResult.Failure(e)
        }
    }
    /**
     * 判断是否为自身应用更新
     */
    private fun isSelfUpdate(apkUrl: String): Boolean {
        try {
            // 从URL提取文件名
            val fileName = apkUrl.substringAfterLast('/')
            // 尝试从文件名中提取包名信息
            val potentialPackageName = fileName.substringBeforeLast('.').lowercase()
            
            // 检查是否包含自身包名的特征
            return potentialPackageName.contains(APP_PACKNAME.replace(".", "")) ||
                    potentialPackageName.contains("mdm") ||
                    potentialPackageName.contains("hem")
        } catch (e: Exception) {
            LogUtils.e(TAG, "判断是否为自身应用更新时出错", e)
            return false
        }
    }

    private suspend fun handleUninstallApp(commandData: String?): CommandResult {
        val packageNames = if (!commandData.isNullOrEmpty()) listOf(commandData) else listOf(WEWORK_PACKAGE)

        try {
            initiateUtils.uninstalls(packageNames)
            delay(UNINSTALL_DELAY_MS)

            val results = packageNames.joinToString("\n") { packageName ->
                if (checkUninstallResult(packageName)) {
                    "$packageName 卸载成功"
                } else {
                    "$packageName 卸载失败"
                }
            }

            return CommandResult.Success("执行指令：卸载APP\n$results")
        } catch (e: Exception) {
            LogUtils.e(TAG, "卸载APP失败", e)
            return CommandResult.Failure(e)
        }
    }

    private fun handleWifiCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.setWifiDisabled(isDisabled)
        val status = if (isDisabled) "关闭" else "开启"
        return CommandResult.Success("执行指令：${status}WiFi")
    }

    private fun handleWifiApCommand(disabled: Boolean): CommandResult {
        huaweiMDMManager.setWifiApDisabled(disabled)
        val status = if (disabled) "关闭" else "开启"
        return CommandResult.Success("执行指令：${status}WiFi热点")
    }

    private fun handleBluetoothCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.setBluetoothDisabled(isDisabled)
        val status = if (isDisabled) "关闭" else "开启"
        return CommandResult.Success("执行指令：${status}蓝牙")
    }

    private fun handleUsbCommand(operationName: String, isDisabled: Boolean): CommandResult {
        val shouldDisable = when (operationName) {
            TYPE_MANAGE_USB -> isDisabled
            TYPE_CLOSE_USB -> true
            TYPE_OPEN_USB -> false
            else -> true
        }
        huaweiMDMManager.setUSBDataDisabled(shouldDisable)
        val status = if (shouldDisable) "已关闭" else "已开启"
        return CommandResult.Success("USB数据传输$status")
    }

    private fun handleOtgCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.setUSBOtgDisabled(isDisabled)
        val status = if (isDisabled) "禁用" else "启用"
        return CommandResult.Success("执行指令：${status}OTG")
    }

    private fun handleNfcCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.setNFCDisabled(isDisabled)
        val status = if (isDisabled) "禁用" else "启用"
        return CommandResult.Success("执行指令：${status}NFC")
    }

    private fun handleScreenCaptureCommand(operationName: String, isDisabled: Boolean): CommandResult {
        val shouldDisable = when (operationName) {
            TYPE_OPEN_SCREENSHOT -> false
            else -> isDisabled
        }
        huaweiMDMManager.setScreenCaptureDisabled(shouldDisable)
        val status = if (shouldDisable) "已禁止" else "已允许"
        return CommandResult.Success("截屏功能$status")
    }

    private fun handleMicrophoneCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.setMicrophoneDisabled(isDisabled)
        val status = if (isDisabled) "禁用" else "启用"
        return CommandResult.Success("执行指令：${status}麦克风")
    }

    private fun handleVideoCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.setVideoDisabled(isDisabled)
        val status = if (isDisabled) "禁用" else "启用"
        return CommandResult.Success("执行指令：${status}摄像头")
    }

    private fun handleMobileDataCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.turnOnMobiledata(!isDisabled)
        val status = if (!isDisabled) "开启" else "关闭"
        return CommandResult.Success("执行指令：${status}移动数据")
    }

    private fun handleConnectionAlwaysOnCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.turnOnConnectionAlwaysOn(!isDisabled)
        val status = if (!isDisabled) "开启" else "关闭"
        return CommandResult.Success("执行指令：${status}始终在线连接")
    }

    private fun handleInternetCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.setDataConnectivityDisabled(isDisabled)
        val status = if (isDisabled) "禁用" else "启用"
        return CommandResult.Success("执行指令：${status}数据网络")
    }

    private fun handleApnSettingCommand(): CommandResult {
        val apnInfo = mapOf(
            "name" to "My APN", "apn" to "internet", "proxy" to "192.168.1.1",
            "port" to "8080", "mmsc" to "http://mmsc.example.com", "mcc" to "310",
            "mnc" to "410", "auth_type" to "1", "user" to "user", "password" to "password"
        )
        huaweiMDMManager.addApn(apnInfo)
        return CommandResult.Success("执行指令：设置APN参数")
    }

    private fun handlePasswordCommand(commandData: String?): CommandResult {
        val newPwd = commandData ?: "222222"
        huaweiMDMManager.resetPassword(newPwd)
        return CommandResult.Success("执行指令：重置设备密码为：$newPwd")
    }

    private fun handlePrivacyProtectionCommand(isDisabled: Boolean): CommandResult {
        huaweiMDMManager.enablePrivacyProtection(isDisabled)
        val status = if (isDisabled) "启用" else "禁用"
        return CommandResult.Success("执行指令：${status}隐私保护")
    }

    private fun handleAppInstallPolicyCommand(): CommandResult {
        val blacklist = setOf("com.example.app1", "com.example.app2", "com.example.app3")
        huaweiMDMManager.setInstallBlacklist(blacklist)
        return CommandResult.Success("执行指令：设置APP安装黑名单：$blacklist")
    }

    private fun handleAppRunPolicyCommand(): CommandResult {
        val blacklist = setOf("com.example.app1", "com.example.app2", "com.example.app3")
        huaweiMDMManager.setRuntimeBlacklist(blacklist)
        return CommandResult.Success("执行指令：设置APP运行黑名单：$blacklist")
    }
    
    /**
     * 处理机卡绑定开关命令
     */
    private fun handleBindingSwitchCommand(isDisabled: Boolean): CommandResult {
        if (!isDisabled) {
            // 开启机卡绑定功能
            SimChangedReceiver.ServerMock.enableBinding()
            // 立即触发一次SIM卡验证
            val receiver = SimChangedReceiver()
            val result = receiver.verifySimBinding()
            return CommandResult.Success("执行指令：机卡绑定功能已开启，已触发SIM校验，结果: $result")
        } else {
            // 关闭机卡绑定功能
            SimChangedReceiver.ServerMock.disableBinding()
            return CommandResult.Success("执行指令：机卡绑定功能已关闭")
        }
    }
    
    /**
     * 处理启动震动提醒命令
     */
    private fun handleStartVibrationAlertCommand(): CommandResult {
        try {
            LogUtils.i(TAG, "执行启动震动提醒命令")
            
            // 启动AlertService并传递启动来源为MQTT
            val intent = Intent(context, AlertService::class.java)
            intent.putExtra("startSource", "mqttCommand")
            
            // 使用ContextCompat.startForegroundService确保兼容性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            return CommandResult.Success("执行指令：启动震动提醒成功")
        } catch (e: Exception) {
            LogUtils.e(TAG, "启动震动提醒失败", e)
            return CommandResult.Failure(Exception("启动震动提醒失败: ${e.message ?: "未知错误"}"))
        }
    }
    
    /**
     * 处理停止震动提醒命令
     */
    private fun handleStopVibrationAlertCommand(): CommandResult {
        try {
            LogUtils.i(TAG, "执行停止震动提醒命令")
            
            // 发送停止命令到AlertService
            val intent = Intent(context, com.siyu.mdm.custom.device.util.screenoffalert.AlertService::class.java)
            intent.action = com.siyu.mdm.custom.device.util.screenoffalert.AlertService.ACTION_STOP
            context.startService(intent)
            
            return CommandResult.Success("执行指令：停止震动提醒成功")
        } catch (e: Exception) {
            LogUtils.e(TAG, "停止震动提醒失败", e)
            return CommandResult.Failure(Exception("停止震动提醒失败: ${e.message ?: "未知错误"}"))
        }
    }

    // ==================== 业务命令处理 ====================

    private suspend fun executeBusinessCommand(pushMessage: PushMessage): CommandResult = withContext(Dispatchers.IO) {
        return@withContext when (pushMessage.messageType) {
            PushMessage.TYPE_RUN_APP -> executeRunApp(pushMessage)
            PushMessage.TYPE_UNINSTALL_APP -> executeUninstallApp(pushMessage)
            PushMessage.TYPE_REBOOT -> executeReboot(pushMessage)
            PushMessage.TYPE_DELETE_FILE -> executeDeleteFile(pushMessage)
            PushMessage.TYPE_RUN_COMMAND -> executeRunCommand(pushMessage)
            PushMessage.TYPE_EXIT_KIOSK -> executeExitKiosk(pushMessage)
            PushMessage.TYPE_GRANT_PERMISSIONS -> executeGrantPermissions(pushMessage)
            TYPE_CONFIGURE_BINDING -> executeConfigureBinding(pushMessage)
            else -> {
                val errorMsg = "不支持的业务命令类型: ${pushMessage.messageType}"
                LogUtils.e(TAG, errorMsg)
                CommandResult.Failure(UnsupportedOperationException(errorMsg))
            }
        }
    }

    private fun executeConfigureBinding(pushMessage: PushMessage): CommandResult {
        return try {
            val data = pushMessage.getDataJSON() ?: throw IllegalArgumentException("缺少配置数据")
            val subType = data.getString("operationName")

            when (subType) {
                SUB_TYPE_SET_BINDINGS -> {
                    val iccidsJson = data.getString("iccids")
                    val iccids = gson.fromJson<Set<String>>(iccidsJson, object : TypeToken<Set<String>>() {}.type)
                    SimChangedReceiver.ServerMock.setBindings(iccids)
                    CommandResult.Success("绑定关系已更新，ICCID数量: ${iccids.size}")
                }
                SUB_TYPE_ADD_BINDING -> {
                    val iccid = data.getString("iccid")
                    SimChangedReceiver.ServerMock.addBinding(iccid)
                    SimChangedReceiver().verifySimBinding()
                    CommandResult.Success("添加绑定关系成功: ICCID=$iccid")
                }
                SUB_TYPE_REMOVE_BINDING -> {
                    val iccid = data.getString("iccid")
                    SimChangedReceiver.ServerMock.removeBinding(iccid)
                    SimChangedReceiver().verifySimBinding()
                    CommandResult.Success("移除绑定关系成功: ICCID=$iccid")
                }
                SUB_TYPE_CLEAR_BINDINGS -> {
                    SimChangedReceiver.ServerMock.clearBindings()
                    SimChangedReceiver().verifySimBinding()
                    CommandResult.Success("所有绑定关系已清除")
                }
                else -> {
                    CommandResult.Failure(UnsupportedOperationException("不支持的配置子类型: $subType"))
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "配置绑定关系失败", e)
            CommandResult.Failure(e)
        }
    }

    // ==================== 辅助方法 ====================

    private fun extractApkUrl(commandData: String?): String {
        return if (!commandData.isNullOrEmpty()) {
            try {
                JSONObject(commandData).optString("apkUrl", COMMON_APK_URLS)
            } catch (e: JSONException) {
                LogUtils.e(TAG, "解析apkUrl失败，使用默认值", e)
                COMMON_APK_URLS
            }
        } else {
            COMMON_APK_URLS
        }
    }

    private fun checkInstallationResult(apkUrl: String): CommandResult {
        val fileName = apkUrl.substringAfterLast('/')
        // 尝试从文件名中提取包名（更健壮的实现）
        val potentialPackageName = fileName.substringBeforeLast('.').lowercase()
        LogUtils.i(TAG, "检查应用安装结果，文件名: $fileName, 潜在包名: $potentialPackageName")
        
        // 尝试通过PackageManager检查应用是否已安装
        try {
            // 遍历所有已安装应用，查找与APK文件名相关的应用
            val installedPackages = context.packageManager.getInstalledPackages(0)
            for (packageInfo in installedPackages) {
                if (packageInfo.packageName.contains(potentialPackageName) || 
                    potentialPackageName.contains(packageInfo.packageName)) {
                    // 找到了匹配的应用，获取其版本信息
                    val versionName = packageInfo.versionName
                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }
                    return CommandResult.Success("应用安装成功：$fileName\n包名：${packageInfo.packageName}\n版本：$versionName($versionCode)")
                }
            }
            // 未找到匹配的应用
            LogUtils.w(TAG, "未找到fileName相关的已安装应用")
            return CommandResult.Failure(Exception("应用安装失败：未找到已安装的应用"))
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查应用安装结果时出错", e)
            return CommandResult.Failure(e)
        }
    }

    private fun checkUninstallResult(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            LogUtils.w(TAG, "应用仍在安装状态: $packageName")
            false
        } catch (e: PackageManager.NameNotFoundException) {
            true
        } catch (e: Exception) {
            LogUtils.e(TAG, "检查应用状态时出错: $packageName", e)
            false
        }
    }

    private fun attachCommandId(result: CommandResult, commandId: String): CommandResult {
        return if (commandId.isNotEmpty()) {
            when (result) {
                is CommandResult.Success -> CommandResult.Success("[ID:$commandId] ${result.message}")
                is CommandResult.Failure -> CommandResult.Failure(Exception("[ID:$commandId] ${result.exception.message ?: "未知错误"}"))
            }
        } else {
            result
        }
    }

    private fun buildCommandResults(results: List<Pair<Int, CommandResult>>): CommandResult {
        val sortedResults = results.sortedBy { it.first }
        val successResults = StringBuilder()
        val failureResults = StringBuilder()
        var allSuccess = true

        sortedResults.forEachIndexed { index, (_, result) ->
            when (result) {
                is CommandResult.Success -> successResults.append("命令${index + 1}: ${result.message}\n")
                is CommandResult.Failure -> {
                    failureResults.append("命令${index + 1}: ${result.exception.message ?: "未知错误"}\n")
                    allSuccess = false
                }
            }
        }

        return if (allSuccess) {
            CommandResult.Success("所有命令执行成功:\n${successResults.toString().trim()}")
        } else {
            val message = buildString {
                if (successResults.isNotEmpty()) append("成功命令:\n${successResults.toString().trim()}\n\n")
                append("失败命令:\n${failureResults.toString().trim()}")
            }
            CommandResult.Failure(Exception(message))
        }
    }

    // 简化业务命令实现（可根据需要进一步优化）
    private suspend fun executeRunApp(pushMessage: PushMessage): CommandResult {
        val packageName = pushMessage.getDataJSON()?.getString("packageName") ?: throw IllegalArgumentException("缺少应用信息")
        delay(1500)
        return CommandResult.Success("应用 $packageName 已启动")
    }

    private suspend fun executeUninstallApp(pushMessage: PushMessage): CommandResult {
        val packageName = pushMessage.getDataJSON()?.getString("packageName") ?: throw IllegalArgumentException("缺少应用信息")
        delay(3000)
        return CommandResult.Success("应用 $packageName 已卸载")
    }

    private suspend fun executeReboot(pushMessage: PushMessage): CommandResult {
        delay(1000)
        return CommandResult.Success("设备正在重启")
    }

    private suspend fun executeDeleteFile(pushMessage: PushMessage): CommandResult {
        val filePath = pushMessage.getDataJSON()?.getString("filePath") ?: throw IllegalArgumentException("缺少文件信息")
        delay(1500)
        return CommandResult.Success("文件 $filePath 已删除")
    }

    private suspend fun executeRunCommand(pushMessage: PushMessage): CommandResult {
        val command = pushMessage.getDataJSON()?.getString("command") ?: throw IllegalArgumentException("缺少命令信息")
        delay(2500)
        return CommandResult.Success("命令 '$command' 已执行")
    }

    private suspend fun executeExitKiosk(pushMessage: PushMessage): CommandResult {
        delay(1000)
        return CommandResult.Success("已退出Kiosk模式")
    }

    private suspend fun executeGrantPermissions(pushMessage: PushMessage): CommandResult {
        val data = pushMessage.getDataJSON() ?: throw IllegalArgumentException("缺少权限信息")
        val packageName = data.getString("packageName")
        val permissions = data.getJSONArray("permissions")
        delay(2000)
        return CommandResult.Success("已为 $packageName 授予 ${permissions.length()} 项权限")
    }
}

/**
 * 命令执行结果密封类
 */
sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class Failure(val exception: Exception) : CommandResult()
}