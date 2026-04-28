package com.siyu.mdm.custom.device.json

import android.service.credentials.Action
import java.util.LinkedList

// 保留Jackson忽略未知字段的注解，与原Java类行为一致
class ServerConfig {
    // 1. 基础配置属性（均为可空类型，匹配原Java包装类/字符串的nullability）
    var newNumber: String? = null
    var backgroundColor: String? = null
    var textColor: String? = null
    var backgroundImageUrl: String? = null
    var password: String? = null
    var phone: String? = null
    var imei: String? = null
    var iconSize: Int? = null // 对应Java Integer
    var title: String? = null
    var displayStatus: Boolean = false // 对应Java boolean（基本类型，默认false）
    var gps: Boolean? = null // 对应Java Boolean
    var bluetooth: Boolean? = null
    var wifi: Boolean? = null
    var mobileData: Boolean? = null
    var kioskMode: Boolean? = null
    var mainApp: String? = null
    var lockStatusBar: Boolean? = null
    var systemUpdateType: Int? = null
    var systemUpdateFrom: String? = null
    var systemUpdateTo: String? = null
    var appUpdateFrom: String? = null
    var appUpdateTo: String? = null
    var downloadUpdates: String? = null
    var factoryReset: Boolean? = null
    var reboot: Boolean? = null
    var lock: Boolean? = null
    var lockMessage: String? = null
    var passwordReset: String? = null
    var pushOptions: String? = null
    var keepaliveTime: Int? = null
    var requestUpdates: String? = null
    var disableLocation: Boolean? = null
    var appPermissions: String? = null
    var usbStorage: Boolean? = null
    var autoBrightness: Boolean? = null
    var brightness: Int? = null
    var manageTimeout: Boolean? = null
    var timeout: Int? = null
    var lockVolume: Boolean? = null
    var manageVolume: Boolean? = null
    var volume: Int? = null
    var passwordMode: String? = null
    var timeZone: String? = null
    var allowedClasses: String? = null
    var orientation: Int? = null
    var kioskHome: Boolean? = null
    var kioskRecents: Boolean? = null
    var kioskNotifications: Boolean? = null
    var kioskSystemInfo: Boolean? = null
    var kioskKeyguard: Boolean? = null
    var kioskLockButtons: Boolean? = null
    var restrictions: String? = null
    var description: String? = null
    var custom1: String? = null
    var custom2: String? = null
    var custom3: String? = null
    var runDefaultLauncher: Boolean? = null
    var newServerUrl: String? = null
    var lockSafeSettings: Boolean = false // 对应Java boolean
    var permissive: Boolean = false // 对应Java boolean
    var kioskExit: Boolean = false // 对应Java boolean
    var disableScreenshots: Boolean = false // 对应Java boolean
    var autostartForeground: Boolean = false // 对应Java boolean
    var showWifi: Boolean = false // 对应Java boolean
    var appName: String? = null
    var vendor: String? = null

    // 2. 集合属性（初始化与原Java一致，使用LinkedList保持有序性）
   /* var applications: MutableList<Application> = LinkedList()
    var applicationSettings: MutableList<ApplicationSetting> = LinkedList()
    var files: MutableList<RemoteFile> = LinkedList()*/
    var actions: MutableList<Action> = LinkedList()

    // 3. 静态常量（Kotlin中通过companion object封装，const val确保编译期常量）
    companion object {
        const val TITLE_NONE = "none"
        const val TITLE_DEVICE_ID = "deviceId"
        const val TITLE_DESCRIPTION = "description"
        const val TITLE_CUSTOM1 = "custom1"
        const val TITLE_CUSTOM2 = "custom2"
        const val TITLE_CUSTOM3 = "custom3"
        const val TITLE_IMEI = "imei"
        const val TITLE_SERIAL = "serialNumber"
        const val TITLE_EXTERNAL_IP = "externalIp"
        const val DEFAULT_ICON_SIZE = 100

        const val SYSTEM_UPDATE_DEFAULT = 0
        const val SYSTEM_UPDATE_INSTANT = 1
        const val SYSTEM_UPDATE_SCHEDULE = 2
        const val SYSTEM_UPDATE_MANUAL = 3

        const val PUSH_OPTIONS_MQTT_WORKER = "mqttWorker"
        const val PUSH_OPTIONS_MQTT_ALARM = "mqttAlarm"
        const val PUSH_OPTIONS_POLLING = "polling"

        const val APP_PERMISSIONS_ASK_LOCATION = "asklocation"
        const val APP_PERMISSIONS_DENY_LOCATION = "denylocation"
        const val APP_PERMISSIONS_ASK_ALL = "askall"
    }

    // 4. 自定义getter（匹配原Java的isKioskMode()逻辑，Kotlin中用自定义属性简化）
    val isKioskMode: Boolean
        get() = kioskMode == true // 等价于原Java的"kioskMode != null && kioskMode"
}

// 注：以下为原Java类依赖的自定义类型，需确保项目中已存在对应Kotlin/Java类
// class Application
// class ApplicationSetting
// class RemoteFile
// class Action