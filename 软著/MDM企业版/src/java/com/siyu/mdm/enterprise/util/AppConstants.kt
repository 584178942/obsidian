package com.siyu.mdm.enterprise.util

/**
 * 应用常量
 *
 * 包含：
 * - SharedPreferences相关
 * - 业务常量
 */
object AppConstants {

    // ==================== SharedPreferences ====================

    /** SharedPreferences文件名 */
    const val SP_NAME = "mdm_enterprise_sp"

    /** SharedPreferences键名 - 锁定状态 */
    const val KEY_LOCK_STATE = "key_lock_state"

    // ==================== 状态常量 ====================

    /** 锁定状态 */
    const val LOCK = "lock"

    /** 解锁状态 */
    const val UN_LOCK = "unlock"

    // ==================== Intent Action ====================

    /** 心跳Action */
    const val ACTION_HEARTBEAT = "com.siyu.mdm.enterprise.action.HEARTBEAT"

    /** 锁定Action */
    const val ACTION_LOCK = "com.siyu.mdm.enterprise.action.LOCK"

    /** 解锁Action */
    const val ACTION_UNLOCK = "com.siyu.mdm.enterprise.action.UNLOCK"

    /** 恢复出厂设置Action */
    const val ACTION_WIPE = "com.siyu.mdm.enterprise.action.WIPE"

    // ==================== Intent Extra Key ====================

    /** 设备ID */
    const val EXTRA_DEVICE_ID = "device_id"

    /** 锁定文本 */
    const val EXTRA_LOCK_TEXT = "lock_text"

    /** 锁定副文本 */
    const val EXTRA_LOCK_SUB_TEXT = "lock_sub_text"

    // ==================== 结果码 ====================

    /** 成功 */
    const val RESULT_SUCCESS = 0

    /** 失败 */
    const val RESULT_FAILED = -1

    /** 不支持 */
    const val RESULT_NOT_SUPPORTED = -2
}
