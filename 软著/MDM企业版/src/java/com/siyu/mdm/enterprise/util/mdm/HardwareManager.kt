package com.siyu.mdm.enterprise.util.mdm

/**
 * 硬件管理器接口
 * 定义截屏、麦克风、摄像头等硬件功能的控制操作
 *
 * 各厂商实现类应实现此接口
 */
interface HardwareManager {

    /**
     * 设置截屏功能状态
     * @param disabled true=禁用，false=启用
     */
    fun setScreenCaptureDisabled(disabled: Boolean)

    /**
     * 设置麦克风状态
     * @param disabled true=禁用，false=启用
     */
    fun setMicrophoneDisabled(disabled: Boolean)

    /**
     * 设置摄像头视频录制状态
     * @param disabled true=禁用，false=启用
     */
    fun setVideoDisabled(disabled: Boolean)

    /**
     * 设置摄像头状态
     * @param disabled true=禁用，false=启用
     */
    fun setCameraDisabled(disabled: Boolean)

    /**
     * 设置多窗口模式状态
     * @param disabled true=禁用，false=启用
     */
    fun setMultiWindowDisabled(disabled: Boolean)

    /**
     * 设置通知状态
     * @param disabled true=禁用，false=启用
     */
    fun setNotificationDisabled(disabled: Boolean)

    /**
     * 设置未知来源应用安装状态
     * @param disabled true=禁用，false=启用
     */
    fun setUnknownSourceInstallDisabled(disabled: Boolean)

    /**
     * 设置文件分享状态
     * @param disabled true=禁用，false=启用
     */
    fun setFileShareDisabled(disabled: Boolean)

    /**
     * 设置恢复出厂设置保护状态
     * @param disabled true=禁用，false=启用
     */
    fun setRestoreFactoryDisabled(disabled: Boolean)

    /**
     * 设置省电模式状态
     * @param disabled true=禁用，false=启用
     */
    fun setPowerSaveModeDisabled(disabled: Boolean)

    /**
     * 设置隐私保护模式
     * @param enabled true=启用，false=禁用
     */
    fun setPrivacyProtection(enabled: Boolean)

    /**
     * 检查功能是否受支持
     * @return true if this feature is supported on current device
     */
    fun isSupported(): Boolean = true
}
