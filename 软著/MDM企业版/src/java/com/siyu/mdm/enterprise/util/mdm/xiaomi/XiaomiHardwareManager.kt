package com.siyu.mdm.enterprise.util.mdm.xiaomi

import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.util.mdm.HardwareManager

/**
 * 小米硬件管理器实现（占位）
 */
class XiaomiHardwareManager : HardwareManager {

    private val TAG = "XiaomiHardwareManager"

    override fun setScreenCaptureDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米截屏控制功能未实现")
        throw UnsupportedOperationException("小米截屏控制功能待实现")
    }

    override fun setMicrophoneDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米麦克风控制功能未实现")
        throw UnsupportedOperationException("小米麦克风控制功能待实现")
    }

    override fun setVideoDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米摄像头控制功能未实现")
        throw UnsupportedOperationException("小米摄像头控制功能待实现")
    }

    override fun setCameraDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米摄像头控制功能未实现")
        throw UnsupportedOperationException("小米摄像头控制功能待实现")
    }

    override fun setMultiWindowDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米多窗口控制功能未实现")
        throw UnsupportedOperationException("小米多窗口控制功能待实现")
    }

    override fun setNotificationDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米通知控制功能未实现")
        throw UnsupportedOperationException("小米通知控制功能待实现")
    }

    override fun setUnknownSourceInstallDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米未知来源安装控制功能未实现")
        throw UnsupportedOperationException("小米未知来源安装控制功能待实现")
    }

    override fun setFileShareDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米文件分享控制功能未实现")
        throw UnsupportedOperationException("小米文件分享控制功能待实现")
    }

    override fun setRestoreFactoryDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米恢复出厂设置保护功能未实现")
        throw UnsupportedOperationException("小米恢复出厂设置保护功能待实现")
    }

    override fun setPowerSaveModeDisabled(disabled: Boolean) {
        LogUtils.w(TAG, "小米省电模式控制功能未实现")
        throw UnsupportedOperationException("小米省电模式控制功能待实现")
    }

    override fun setPrivacyProtection(enabled: Boolean) {
        LogUtils.w(TAG, "小米隐私保护功能未实现")
        throw UnsupportedOperationException("小米隐私保护功能待实现")
    }

    override fun isSupported(): Boolean = false
}
