package com.siyu.mdm.custom.device.util

/**
 * @author ZT
 * @data 2020/09/24
 */
object AppConstants {
    /**z
     * true 测试环境
     * false 正式环境
     */
    const val IS_TEST: Boolean = true
    const val APP_PACKNAME: String = "com.siyu.mdm.custom.device"
    const val APP_WAKE_UP_COUNT: String = "appWakeUpCount"
    // 定义目标Action常量（建议在公共类中统一管理）
    const val ACTION_MDM_INSTALL_RESULT = "com.siyu.mdm.action.INSTALL_RESULT"
    const val FLAG_DATABL: Int = 6
    const val FLAG_DATAWL: Int = 7
    const val FLAG_FORBID: Int = 1
    const val FLAG_INSTALLBL: Int = 4
    const val FLAG_INSTALLWL: Int = 5
    const val FLAG_PERSIST: Int = 0
    const val FLAG_UNINSTALLBL: Int = 2
    const val FLAG_UNINSTALLWL: Int = 3
    const val FLAG_WIFIBL: Int = 8
    const val FLAG_WIFIWL: Int = 9
    const val FLAG_HOMEKEY_DISPATCHED: Int = -0x80000000

    const val PHONE_NUMBER: String = "phoneNumber"
    const val WORK_TAG_COMMON: String = "com.siyu.mdm.custom.device"
    const val DEFAULT_PUSH_ALARM_KEEPALIVE_TIME_SEC: Int = 300
    const val DEFAULT_PUSH_WORKER_KEEPALIVE_TIME_SEC: Int = 900

    /**
     * 是否第一次开机
     */
    const val FIRST_STATE: String = "firstState"

    const val POLL_TIME: String = "pollTime"
    const val LOCK_STATE: String = "lockState"
    const val LATITUDE: String = "latitude"
    const val LONGITUDE: String = "longitude"
    const val BIND_PHONE_NUMBER: String = "bindPhoneNumber"

    const val VERSION: String = "version"
    const val ICCID: String = "iccId"
    const val IMEI_CODE: String = "imeiCode"

    /**
     * 机卡绑定开关
     */
    const val BIND_STATE: String = "bindState"

    /**
     * 锁机
     */
    const val LOCK: String = "lock"

    /**
     * 解锁
     */
    const val UN_LOCK: String = "unlock"

    /**
     * 格式化
     */
    const val CLEAR: String = "clear"

    /**
     * 安装
     */
    const val INSTALL: String = "install"

    /**
     * 卸载
     */
    const val REMOVE: String = "remove"

    /**
     * 锁机
     */
    const val SWITCH_IMG: String = "switchImg"

    /**
     * 添加白名单
     */
    const val ADD_WHITE: String = "addWhite"

    /**
     * 添加WIFI白名单
     */
    const val WIFI_WHITE: String = "wifiWhite"

    /**
     * 添加白名单
     */
    const val REMOVE_WHITE: String = "removeWhite"

    /**
     * 清楚wifi白名单
     */
    const val REMOVE_WIFI: String = "removeWifi"
    const val ACTIVATION_STATE: String = "activationState"
    const val ACTIVATE: String = "activate"

    /**
     * 添加白名单
     */
    const val UPDATE: String = "update"

    /**
     * 添加白名单
     */
    const val BIND: String = "bind"

    /**
     * 添加白名单
     */
    const val UN_BIND: String = "unbind"

    /**
     * 通话记录
     */
    const val CALL_RECORDS: String = "callRecords"

    /**
     * 接口名 heartbeat 心跳
     */
    const val HEART_BEAT: String = "heartbeat"

    const val HEART_BEAT_REQUEST: String = "heartbeat/request"

    /**
     * 添加白名单
     */
    const val UNINSTALL_PATTERN: Int = 1

    /**
     * 锁屏文字
     */
    const val LOCK_MSG: String = "LOCK_MSG"

    /**
     * 存储文件name
     */
    const val COM_SGT_SECURITY: String = "com_sgt_security"

    /**
     * 默认锁屏文字
     */
    const val DEFAULT_LOCK_MSG: String = "终端强制锁定，如需解锁请与管理员联系"


    /**
     * 5秒
     */
    const val FIVE_SECOND: Int = 5000

    /**
     * 15秒
     */
    const val FIFTH_SECOND: Int = 15000

    /**
     * 20秒
     */
    const val TWO_SECOND: Int = 20000

    /**
     * 亮屏5秒
     */
    const val SPACE_SECOND: Int = 5

    const val CONTRACT_COMPLETED_ACTION: String = "CONTRACT_COMPLETED_ACTION"

    const val IMSI_CODE: String = "imsiCode"

    const val TYPE_NONE: Int = -1

    const val TYPE_MOBILE: Int = 0

    const val TYPE_WIFI: Int = 1

    const val INSTALL_LIST_STR: String = "installListStr"
    const val ANDROID_INTENT_ACTION_SCREEN_ON: String = "android.intent.action.SCREEN_ON"

    const val ANDROID_INTENT_ACTION_SCREEN_OFF: String = "android.intent.action.SCREEN_OFF"

    /**
     * 亮屏默认最小时间间隔
     */
    var BRIGHT_SCREEN_MIN: Int = 20

    /**
     * 亮屏默认最大时间间隔
     */
    var BRIGHT_SCREEN_MAX: Int = 30

    /**
     * 暗屏默认最小时间间隔
     */
    var DARK_SCREEN_MIN: Int = 60 * 60

    /**
     * 暗屏默认最大时间间隔
     */
    var DARK_SCREEN_MAX: Int = 60 * 60 * 2

    /**
     * 默认间隔
     */
    var DEFAULT_INTERVAL: Int = 30 * 60
}
