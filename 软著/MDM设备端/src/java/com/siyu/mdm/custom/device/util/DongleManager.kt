package com.siyu.mdm.custom.device.util

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.widget.Toast


class DongleManager private constructor(private val context: Context) {
    // 目标设备参数（需替换为实际设备的VID/PID）
    companion object {
        private const val TARGET_VID = 4660
        private const val TARGET_PID = 22136
        private const val PREF_NAME = "DonglePrefs"
        private const val KEY_WHITELIST = "whitelist"
        private const val KEY_BOUND_SN = "bound_sn"

        @Volatile
        private var instance: DongleManager? = null

        fun getInstance(context: Context): DongleManager {
            return instance ?: synchronized(this) {
                instance ?: DongleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val usbManager: UsbManager by lazy {
        context.getSystemService(Context.USB_SERVICE) as UsbManager
    }
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    private val usbReceiver = DongleUsbReceiver()
    private var statusListener: OnDongleStatusChangeListener? = null

    // 初始化监听
    fun initListener(listener: OnDongleStatusChangeListener) {
        this.statusListener = listener
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(usbReceiver, filter)
    }

    // 释放资源
    fun release() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        statusListener = null
    }

    // 检查是否为目标Dongle设备
    private fun isTargetDongle(device: UsbDevice): Boolean {
        return device.vendorId == TARGET_VID && device.productId == TARGET_PID
    }

    // 获取设备SN（需根据实际通信协议实现）
    private fun getDongleSN(device: UsbDevice): String? {
        // 实际场景中需通过USB通信读取SN
        // 示例：return readSnFromUsb(device)
        return "SN-${device.deviceId}-${System.currentTimeMillis()}"
    }

    // 白名单相关操作
    fun getWhitelist(): MutableSet<String> {
        return prefs.getStringSet(KEY_WHITELIST, mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
    }

    fun addToWhitelist(sn: String) {
        val whitelist = getWhitelist().apply { add(sn) }
        prefs.edit().putStringSet(KEY_WHITELIST, whitelist).apply()
    }

    fun isInWhitelist(sn: String): Boolean {
        return getWhitelist().contains(sn)
    }

    // 从白名单中移除
    fun removeFromWhitelist(sn: String) {
        val whitelist = getWhitelist().apply { remove(sn) }
        prefs.edit().putStringSet(KEY_WHITELIST, whitelist).apply()
    }

    // 绑定相关操作
    fun bindDongle(sn: String) {
        prefs.edit().putString(KEY_BOUND_SN, sn).apply()
    }

    fun getBoundDongleSn(): String? {
        return prefs.getString(KEY_BOUND_SN, null)
    }

    fun unbindDongle() {
        prefs.edit().remove(KEY_BOUND_SN).apply()
    }

    // 请求USB权限
    private fun requestUsbPermission(device: UsbDevice) {
    }

    // 广播接收器（内部类）
    inner class DongleUsbReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    handleDeviceAttached(intent)
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    handleDeviceDetached(intent)
                }
            }
        }

        private fun handleDeviceAttached(intent: Intent) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
            if (isTargetDongle(device)) {
                if (usbManager.hasPermission(device)) {
                    processAttachedDevice(device)
                } else {
                    requestUsbPermission(device)
                }
            }
        }

        private fun handlePermissionResult(intent: Intent) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                processAttachedDevice(device)
            } else {
                Toast.makeText(context, "需要USB权限才能使用Dongle", Toast.LENGTH_SHORT).show()
                statusListener?.onDonglePermissionDenied(device)
            }
        }

        private fun processAttachedDevice(device: UsbDevice) {
            val sn = getDongleSN(device)
            if (sn.isNullOrEmpty()) {
                statusListener?.onDongleInvalid(device)
                return
            }

            // 自动加入白名单（可根据业务调整）
            if (!isInWhitelist(sn)) {
                addToWhitelist(sn)
            }

            val isBound = sn == getBoundDongleSn()
            statusListener?.onDongleAttached(device, sn, isInWhitelist(sn), isBound)
        }

        private fun handleDeviceDetached(intent: Intent) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
            if (isTargetDongle(device)) {
                val sn = getDongleSN(device)
                statusListener?.onDongleDetached(device, sn)
            }
        }
    }

    // 状态回调接口
    interface OnDongleStatusChangeListener {
        fun onDongleAttached(device: UsbDevice, sn: String, isWhitelisted: Boolean, isBound: Boolean)
        fun onDongleDetached(device: UsbDevice, sn: String?)
        fun onDonglePermissionDenied(device: UsbDevice)
        fun onDongleInvalid(device: UsbDevice)
    }
}
