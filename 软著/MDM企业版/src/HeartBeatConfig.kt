package com.siyu.mdm.enterprise.util

/**
 * 心跳配置
 *
 * 根据设备状态动态调整心跳间隔，优化省电
 *
 * | 场景 | 间隔 | 说明 |
 * |------|------|------|
 * | 充电中 | 5分钟 | 正常心跳 |
 * | 亮屏使用 | 5分钟 | 用户活跃 |
 * | 熄屏空闲 | 15分钟 | 省电优先 |
 * | 低电量 | 30分钟 | 电量<20% |
 * | 离线告警 | 立即 | 网络异常 |
 */
object HeartBeatConfig {

    // ==================== 心跳间隔配置（单位：毫秒） ====================

    /** 正常心跳间隔：5分钟 */
    const val NORMAL_INTERVAL = 5 * 60 * 1000L

    /** 熄屏省电心跳间隔：15分钟 */
    const val POWER_SAVING_INTERVAL = 15 * 60 * 1000L

    /** 低电量心跳间隔：30分钟 */
    const val LOW_BATTERY_INTERVAL = 30 * 60 * 1000L

    /** 心跳超时时间：2分钟 */
    const val TIMEOUT = 2 * 60 * 1000L

    // ==================== 电量阈值 ====================

    /** 低电量阈值：20% */
    const val LOW_BATTERY_THRESHOLD = 20

    // ==================== 心跳状态 ====================

    enum class HeartBeatState {
        /** 正常状态 */
        NORMAL,

        /** 省电状态（熄屏） */
        POWER_SAVING,

        /** 低电量状态 */
        LOW_BATTERY,

        /** 离线状态 */
        OFFLINE
    }

    /**
     * 根据设备状态获取心跳间隔
     *
     * @param isCharging 是否在充电
     * @param isScreenOn 屏幕是否亮起
     * @param batteryLevel 当前电量百分比
     * @return 推荐的心跳间隔（毫秒）
     */
    fun getInterval(isCharging: Boolean, isScreenOn: Boolean, batteryLevel: Int): Long {
        return when {
            // 充电中：正常间隔
            isCharging -> NORMAL_INTERVAL

            // 低电量：省电间隔
            batteryLevel < LOW_BATTERY_THRESHOLD -> LOW_BATTERY_INTERVAL

            // 熄屏空闲：省电间隔
            !isScreenOn -> POWER_SAVING_INTERVAL

            // 亮屏使用：正常间隔
            else -> NORMAL_INTERVAL
        }
    }

    /**
     * 根据设备状态获取心跳状态描述
     */
    fun getStateDescription(isCharging: Boolean, isScreenOn: Boolean, batteryLevel: Int): String {
        return when {
            isCharging -> "充电中 - 正常心跳(${NORMAL_INTERVAL / 1000 / 60}分钟)"
            batteryLevel < LOW_BATTERY_THRESHOLD -> "低电量($batteryLevel%) - 省电心跳(${LOW_BATTERY_INTERVAL / 1000 / 60}分钟)"
            !isScreenOn -> "熄屏空闲 - 省电心跳(${POWER_SAVING_INTERVAL / 1000 / 60}分钟)"
            else -> "正常使用 - 正常心跳(${NORMAL_INTERVAL / 1000 / 60}分钟)"
        }
    }
}
