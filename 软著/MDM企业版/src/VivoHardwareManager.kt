package com.siyu.mdm.enterprise.util.mdm.vivo

import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.HardwareManager

/**
 * vivo硬件管理器实现（占位）
 */
class VivoHardwareManager : HardwareManager {

    private val TAG = "VivoHardwareManager"

    override fun setScreenCaptureDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo截屏控制功能未实现")
        throw UnsupportedOperationException("vivo截屏控制功能待实现")
    }

    override fun setMicrophoneDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo麦克风控制功能未实现")
        throw UnsupportedOperationException("vivo麦克风控制功能待实现")
    }

    override fun setVideoDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo摄像头控制功能未实现")
        throw UnsupportedOperationException("vivo摄像头控制功能待实现")
    }

    override fun setCameraDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo摄像头控制功能未实现")
        throw UnsupportedOperationException("vivo摄像头控制功能待实现")
    }

    override fun setMultiWindowDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo多窗口控制功能未实现")
        throw UnsupportedOperationException("vivo多窗口控制功能待实现")
    }

    override fun setNotificationDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo通知控制功能未实现")
        throw UnsupportedOperationException("vivo通知控制功能待实现")
    }

    override fun setUnknownSourceInstallDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo未知来源安装控制功能未实现")
        throw UnsupportedOperationException("vivo未知来源安装控制功能待实现")
    }

    override fun setFileShareDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo文件分享控制功能未实现")
        throw UnsupportedOperationException("vivo文件分享控制功能待实现")
    }

    override fun setRestoreFactoryDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo恢复出厂设置保护功能未实现")
        throw UnsupportedOperationException("vivo恢复出厂设置保护功能待实现")
    }

    override fun setPowerSaveModeDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "vivo省电模式控制功能未实现")
        throw UnsupportedOperationException("vivo省电模式控制功能待实现")
    }

    override fun setPrivacyProtection(enabled: Boolean) {
        LogUtils.w(TAG, "vivo隐私保护功能未实现")
        throw UnsupportedOperationException("vivo隐私保护功能待实现")
    }

    override fun isSupported(): Boolean = false
}
