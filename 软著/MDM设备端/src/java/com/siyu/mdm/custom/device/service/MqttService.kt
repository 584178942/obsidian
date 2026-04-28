package com.siyu.mdm.custom.device.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PermissionUtils
import com.siyu.mdm.custom.device.R
import com.siyu.mdm.custom.device.json.PushMessageJson
import com.siyu.mdm.custom.device.ui.MainActivity 
import com.siyu.mdm.custom.device.util.CommandResult
import com.siyu.mdm.custom.device.util.HuaweiMDMManager
import com.siyu.mdm.custom.device.util.InitiateUtils
import com.siyu.mdm.custom.device.util.MdmCommandManager
import com.siyu.mdm.custom.device.util.mqtt.MqttManager
import com.siyu.mdm.custom.device.util.mqtt.MqttTopicManager
import com.siyu.mdm.custom.device.util.mqtt.MqttUtils
import com.siyu.mdm.custom.device.util.mqtt.MqttUtils.getServerUri
import com.siyu.mdm.custom.device.util.mqtt.MqttUtils.saveServerUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class MqttService : Service() {
    // ====================== 通知相关常量（统一管理，避免魔法数字） ======================
    private val NOTIFY_ID = 1 // 前台通知ID（固定，与服务绑定）
    private val CHANNEL_ID = "com.siyu.mdm.mqtt.foreground" // 通知渠道ID（带包名防冲突）
    private val CHANNEL_NAME = "MQTT设备服务" // 渠道名称（用户可见）
    private val CHANNEL_DESC = "维持MQTT连接，确保设备正常接收控制指令" // 渠道描述（用户理解用途）
    private  val FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE = 0x00000008
    // MDM命令管理器与MQTT配置
    private val mdmManager: MdmCommandManager = MdmCommandManager(
        this,
        huaweiMDMManager = HuaweiMDMManager(),
        initiateUtils = InitiateUtils()
    )
    private val AUTO_SUBSCRIBE_QOS = 1
    private val deviceID = InitiateUtils().getSerialNumber().toString()
    private val AUTO_SUBSCRIBE_TOPICS = listOf(
        MqttTopicManager.getDevicePrivateCommandTopic(deviceID)
    )

    // 服务核心变量
    // ====================== 动作和额外数据常量（用于组件间通信） ======================
    companion object {
        // SIM卡状态变化相关动作和数据键
        const val ACTION_SIM_STATE_CHANGED = "com.siyu.mdm.action.SIM_STATE_CHANGED"
        const val EXTRA_SIM_STATE = "sim_state"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_DEVICE_INFO = "device_info"
    }

    // ====================== 服务核心变量 ======================
    private val binder = LocalBinder()
    private val TAG = "MqttService"
    private var mqttManager: MqttManager? = null
    private var serviceCallback: ServiceCallback? = null
    private var isServiceStarted = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    inner class LocalBinder : Binder() {
        val service: MqttService get() = this@MqttService
    }

    override fun onCreate() {
        super.onCreate()
        LogUtils.d(TAG, "Service created")
        showNotification() // 启动前台通知
        initMqttClient()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.d(TAG, "Service started")
        if (!isServiceStarted) {
            isServiceStarted = true
            connect()
        }
        
        // 处理SIM卡状态变化意图
        if (intent != null) {
            when (intent.action) {
                ACTION_SIM_STATE_CHANGED -> {
                    handleSimStateChanged(intent)
                }
                // 可以在这里添加其他动作的处理逻辑
            }
        }
        
        return START_STICKY
    }
    
    /**
     * 处理SIM卡状态变化
     */
    private fun handleSimStateChanged(intent: Intent) {
        try {
            val simState = intent.getStringExtra(EXTRA_SIM_STATE)
            val message = intent.getStringExtra(EXTRA_MESSAGE)
            val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0)
            val deviceInfoJson = intent.getStringExtra(EXTRA_DEVICE_INFO)
            
            LogUtils.i(TAG, "接收到SIM卡状态变化: state=$simState, message=$message")
            
            // 构建SIM卡状态变化消息
            val simStateMessage = JSONObject()
            simStateMessage.put("action", "sim_state_change")
            simStateMessage.put("sim_state", simState)
            simStateMessage.put("message", message)
            simStateMessage.put("timestamp", timestamp)
            if (deviceInfoJson != null) {
                simStateMessage.put("device_info", JSONObject(deviceInfoJson))
            }
            
            // 发送到服务器 - 使用设备事件上报主题
            val topic = MqttTopicManager.getDeviceCommandResultTopic(deviceID)
            publish(topic, simStateMessage.toString(), 1, false)
            LogUtils.i(TAG, "已发送SIM卡状态变化消息到服务器: $topic")
        } catch (e: Exception) {
            LogUtils.e(TAG, "处理SIM卡状态变化失败", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        LogUtils.d(TAG, "Service bound")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtils.d(TAG, "Service destroyed")
        disconnect()
        serviceScope.cancel()
        // 可选：手动取消通知（前台服务销毁后通知通常会自动消失，特殊场景需加）
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFY_ID)
    }

    // ====================== 优化后的前台通知核心方法 ======================
    /**
     * 显示前台服务通知（解决权限、重复创建、高版本适配问题）
     */
    private fun showNotification() {
        // 1. Android 13+ 必需检查通知权限（否则通知不显示）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.isGranted(android.Manifest.permission.POST_NOTIFICATIONS)) {
                LogUtils.e(TAG, "Android 13+ 缺少通知权限，无法显示前台通知")
                Toast.makeText(this, "请授予通知权限以维持服务运行", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 2. 检查是否已存在相同ID的通知（避免重复创建）
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val hasExistingNotify = notificationManager.activeNotifications.any { it.id == NOTIFY_ID }
        if (hasExistingNotify) {
            LogUtils.i(TAG, "已存在前台通知，无需重复创建")
            return
        }

        // 3. 高版本创建通知渠道（仅O+，且确保只创建一次）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT // 正常重要性，不弹窗打扰
                ).apply {
                    description = CHANNEL_DESC // 渠道描述（用户在设置中可见）
                    setSound(null, null) // 关闭通知声音（前台服务无需提醒）
                    vibrationPattern = longArrayOf() // 关闭震动
                    setShowBadge(false) // 不显示应用角标
                }
                notificationManager.createNotificationChannel(channel)
                LogUtils.i(TAG, "通知渠道创建成功：$CHANNEL_ID")
            }
        }

        // 4. 启动前台服务（Android 14+ 必需指定服务类型，合规性要求）

        val notification = getNotification()

        // 关键：手动定义 API 34 常量（避免直接引用未识别的系统常量）
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+（API 34+）：使用系统常量
            FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            // 低版本：无需指定类型（传 0 或不传递）
            0
        }

        // 启动前台服务（根据版本传递类型参数）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFY_ID, notification, foregroundType)
        } else {
            // 低版本不支持第三个参数，直接调用两个参数的方法
            startForeground(NOTIFY_ID, notification)
        }
        LogUtils.i(TAG, "前台通知启动成功")

    }

    /**
     * 创建通知对象（优化文本、点击交互、低版本兼容）
     */
    private fun getNotification(): Notification {
        // 通知点击意图：跳转到主页（用户点击能进入应用，提升体验）
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // 高版本必需IMMUTABLE
        )

        // 构建通知（用 NotificationCompat 兼容所有版本）
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 建议替换为专门的通知图标
            .setContentTitle("${Build.MODEL} 设备服务运行中") // 标题带设备名，明确身份
            .setContentText("保持MQTT连接，确保指令正常接收") // 非空文本，告知用户服务用途
            .setContentIntent(pendingIntent) // 点击跳转主页
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 兼容版优先级
            .setOngoing(true) // 禁止用户手动关闭（前台服务核心特性）
            .setSilent(true) // 静默通知，不触发声音/震动
            .setOnlyAlertOnce(true) // 仅首次显示时提醒，避免重复打扰
            .build()
    }

    // ====================== 原有核心逻辑（MQTT初始化、消息处理等） ======================
    private fun initMqttClient() {
        val willMessage = MqttUtils.buildMessage(deviceID)
        mqttManager = MqttManager.getInstance(applicationContext)
            .setConnectionOptions(
                serverUri = getServerUri(),
                willTopic = MqttTopicManager.DEVICE_WILL,
                willMessage = willMessage
            )
            .setConnectionCallback(object : MqttManager.ConnectionCallback {
                override fun onConnected() {
                    LogUtils.d(TAG, "MQTT connected")
                    autoSubscribeTopics()
                    serviceCallback?.onMqttConnected()
                }

                override fun onConnectionFailed(exception: Throwable) {
                    LogUtils.e(TAG, "MQTT connection failed: ${exception.message}")
                    serviceCallback?.onMqttConnectionFailed(exception.message ?: "Unknown error")
                }

                override fun onDisconnected() {
                    LogUtils.d(TAG, "MQTT disconnected")
                    serviceCallback?.onMqttDisconnected()
                }

                override fun onConnectionLost(cause: Throwable?) {
                    LogUtils.e(TAG, "MQTT connection lost: ${cause?.message}")
                    serviceCallback?.onMqttConnectionLost(cause?.message ?: "Connection lost")
                }

                override fun onReconnected() {
                    LogUtils.d(TAG, "MQTT reconnected")
                    autoSubscribeTopics()
                    serviceCallback?.onMqttReconnected()
                }

                override fun onDisconnectionFailed(exception: Throwable) {
                    LogUtils.e(TAG, "MQTT disconnection failed: ${exception.message}")
                }
            })
            .setMessageCallback(object : MqttManager.MessageCallback {
                override fun onMessageReceived(topic: String, payload: String, qos: Int, retained: Boolean) {
                    handleReceivedMessage(topic, payload)
                    serviceCallback?.onMqttMessageReceived(topic, payload)
                }
            })
    }

    private fun autoSubscribeTopics() {
        AUTO_SUBSCRIBE_TOPICS.forEach { topic ->
            subscribe(topic, AUTO_SUBSCRIBE_QOS)
            LogUtils.d(TAG, "自动订阅主题：$topic (QoS: $AUTO_SUBSCRIBE_QOS)")
            if (topic == MqttTopicManager.getDevicePrivateCommandTopic(deviceID)) {
                val initMessage = MqttUtils.buildMessage(deviceID, "register")
                if (initMessage.isNotEmpty()) {
                    publish(MqttTopicManager.DEVICE_WILL, initMessage, 1, true)
                }
            }
        }
    }

    /**
     * 处理接收到的MQTT消息
     * @param topic 消息主题
     * @param payload 消息内容
     */
    private fun handleReceivedMessage(topic: String, payload: String) {
        try {
            when (topic) {
                MqttTopicManager.getDevicePrivateCommandTopic(deviceID) -> {
                    handleDeviceCommandMessage(payload)
                }
                MqttTopicManager.getDeviceConfigTopic(deviceID) -> {
                    handleDeviceConfigMessage(payload)
                }
                else -> {
                    LogUtils.d(TAG, "未知主题消息 [$topic]：$payload")
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "处理MQTT消息失败：topic=$topic, payload=$payload", e)
        }
    }

    /**
     * 处理设备命令消息
     */
    private fun handleDeviceCommandMessage(payload: String) {
        try {
            val jsonObject = JSONObject(payload)
            val messageType = jsonObject.getString("topic")
            val commandData = jsonObject.getString("data")

            // 处理data字段可能是JSONArray或JSONObject的情况
            val dataObj = try {
                // 尝试直接作为JSONObject获取
                jsonObject.getJSONObject("data")
            } catch (e: JSONException) {
                // 如果是JSONArray，创建一个新的JSONObject包装它
                try {
                    val array = jsonObject.getJSONArray("data")
                    JSONObject().put("items", array)
                } catch (ex: JSONException) {
                    // 如果都不是，则记录日志并返回null
                    LogUtils.w(TAG, "data字段既不是JSONObject也不是JSONArray: $commandData")
                    null
                }
            }
            val msg = PushMessageJson(messageType, dataObj)

            
            // 提取data中的id - 支持data为对象或数组的情况
            val (commandId, commandIds) = extractCommandIds(jsonObject)
            
            serviceScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        if (commandIds?.isNotEmpty() == true) {
                            // 传递命令ID列表给executeCommand方法
                            mdmManager.executeCommand(msg, commandData, commandIds)
                        } else {
                            mdmManager.executeCommand(msg, commandData)
                        }
                    }
                    LogUtils.d(TAG, "MDM命令执行成功：$messageType")
                    serviceCallback?.onMdmCommandSuccess(messageType, result.toString())

                    // 发送命令执行结果到后台，传递id
                    val resultCommandId = if (commandIds?.isNotEmpty() == true) commandIds.firstOrNull() else commandId
                    publishCommandResult(messageType, result, resultCommandId)
                } catch (e: Exception) {
                    LogUtils.e(TAG, "MDM命令执行失败：$messageType", e)
                    serviceCallback?.onMdmCommandFailed(messageType, e.message ?: "Unknown error")

                    // 发送命令执行失败结果到后台，传递id
                    val resultCommandId = if (commandIds?.isNotEmpty() == true) commandIds.firstOrNull() else commandId
                    publishCommandResult(messageType, CommandResult.Failure(e), resultCommandId)
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "解析设备命令消息失败：$payload", e)
        }
    }

    /**
     * 处理设备配置消息
     */
    private fun handleDeviceConfigMessage(payload: String) {
        try {
            val jsonObject = JSONObject(payload)
            val serverUri = jsonObject.getString("server_uri")
            saveServerUri(serverUri)
            LogUtils.d(TAG, "更新MQTT服务器地址成功：$serverUri")
        } catch (e: Exception) {
            LogUtils.e(TAG, "解析设备配置消息失败：$payload", e)
        }
    }

    /**
     * 从JSON对象中提取命令ID
     * @return Pair<单个命令ID, 命令ID列表>
     */
    private fun extractCommandIds(jsonObject: JSONObject): Pair<String?, List<String>?> {
        var commandId: String? = null
        var commandIds: List<String>? = null
        
        try {
            // 尝试获取data字段
            val data = jsonObject.get("data")
            if (data is JSONObject) {
                // data是单个对象的情况
                commandId = data.optString("id", "").takeIf { it.isNotEmpty() }
            } else if (data is JSONArray) {
                // data是数组的情况
                commandIds = mutableListOf()
                for (i in 0 until data.length()) {
                    try {
                        val item = data.getJSONObject(i)
                        val id = item.optString("id", "")
                        if (id.isNotEmpty()) {
                            (commandIds as MutableList<String>).add(id)
                        }
                    } catch (e: Exception) {
                        LogUtils.w(TAG, "提取数组中第${i+1}个元素的ID失败", e)
                    }
                }
                // 如果提取的ID列表为空，则设为null
                if (commandIds.isEmpty()) {
                    commandIds = null
                }
            }
        } catch (e: Exception) {
            LogUtils.w(TAG, "提取命令ID失败", e)
        }
        
        return Pair(commandId, commandIds)
    }

    // MQTT核心操作方法
    fun subscribe(topic: String, qos: Int = 1) {
        mqttManager?.subscribe(topic, qos)
        LogUtils.d(TAG, "订阅主题：$topic (QoS: $qos)")
    }
    
    fun connect() {
        mqttManager?.connect()
        LogUtils.d(TAG, "正在连接MQTT服务器...")
    }
    
    fun disconnect() {
        mqttManager?.disconnect()
        LogUtils.d(TAG, "正在断开MQTT连接...")
    }
    
    fun unsubscribe(topic: String) {
        mqttManager?.unsubscribe(topic)
        LogUtils.d(TAG, "取消订阅主题：$topic")
    }

    /**
     * 发送命令执行结果到后台服务器
     * @param commandType 命令类型
     * @param result 命令执行结果
     * @param commandId 命令ID（可选）
     */
    private fun publishCommandResult(commandType: String, result: CommandResult, commandId: String? = null) {
        try {
            val resultTopic = MqttTopicManager.DEVICE_WILL

            // 使用MqttUtils的工具方法构建结果消息，包含commandId
            val resultMessage = if (!commandId.isNullOrEmpty()) {
                MqttUtils.buildCommandResultMessage(deviceID, commandId, result)
            } else {
                MqttUtils.buildCommandResultMessage(deviceID, commandType, result)
            }
            
            // 验证消息是否构建成功
            if (resultMessage.isNotEmpty()) {
                // 发送结果到服务器
                mqttManager?.publish(resultTopic, resultMessage, 1, false)
               // LogUtils.d(TAG, "已发送命令执行结果到服务器: $commandType")
            } else {
                LogUtils.e(TAG, "构建命令结果消息失败: $commandType")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "发送命令执行结果失败: $commandType", e)
        }
    }
    
    /**
     * 发布MQTT消息
     * @param topic 主题
     * @param payload 消息内容
     * @param qos 服务质量
     * @param retained 是否保留
     */
    fun publish(topic: String, payload: String, qos: Int = 1, retained: Boolean = false) {
        mqttManager?.publish(topic, payload, qos, retained)
        LogUtils.d(TAG, "发布消息到主题：$topic (QoS: $qos, retained: $retained)")
    }

    // 回调管理方法
    fun setServiceCallback(callback: ServiceCallback) {
        this.serviceCallback = callback
    }

    fun removeServiceCallback() {
        this.serviceCallback = null
    }

    /**
     * 服务回调接口
     */
    interface ServiceCallback {
        fun onMqttConnected()
        fun onMqttConnectionFailed(errorMessage: String)
        fun onMqttDisconnected()
        fun onMqttConnectionLost(errorMessage: String)
        fun onMqttReconnected()
        fun onMqttMessageReceived(topic: String, message: String)
        fun onMdmCommandSuccess(commandType: String, result: String)
        fun onMdmCommandFailed(commandType: String, errorMessage: String)
    }
}