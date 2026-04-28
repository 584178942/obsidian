package com.siyu.mdm.custom.device.util
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.provider.Settings
import android.telephony.TelephonyManager
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.util.StorageUtil
import com.siyu.mdm.custom.device.util.AppConstants
import com.siyu.mdm.custom.device.receiver.SimChangedReceiver

object DeviceStatusProvider {
    private const val TAG = "DeviceStatusProvider"

    fun getBatteryLevel(context: Context): Int {
        try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.registerReceiver(null, ifilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(null, ifilter)
            }

            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    return level * 100 / scale
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取电池电量失败", e)
        }
        return -1
    }
    fun getSignalStrength(context: Context): Int {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            LogUtils.w(TAG, "缺少READ_PHONE_STATE权限，无法获取信号强度")
            return -120
        }

        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val networkType = telephonyManager.networkType
            
            // 注意：在Android较高版本中，直接获取信号强度的API可能受限
            // 这里返回一个模拟值，实际应用中可能需要通过TelephonyCallback或PhoneStateListener获取
            return when (networkType) {
                TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_NR -> -50 // 强信号
                TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSPA -> -70
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EDGE -> -90
                else -> -110 // 弱信号
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取信号强度失败", e)
        }
        return -120
    }


     fun isWifiEnabled(context: Context): Boolean {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return wifiManager.isWifiEnabled
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取WiFi状态失败", e)
        }
        return false
    }

    fun isMobileDataEnabled(context: Context): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // Android 7.0及以上版本使用NetworkCapabilities
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                return capabilities != null && capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
            } else {
                // 低版本使用反射获取
                val connectivityClass = Class.forName(connectivityManager.javaClass.name)
                val method = connectivityClass.getMethod("getMobileDataEnabled")
                method.isAccessible = true
                return method.invoke(connectivityManager) as Boolean
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取移动数据状态失败", e)
        }
        return false
    }

    fun isAirplaneModeEnabled(context: Context): Boolean {
        try {
            return Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) == 1
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取飞行模式状态失败", e)
        }
        return false
    }

    fun isBluetoothEnabled(): Boolean {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled
        } catch (e: Exception) {
            LogUtils.e(TAG, "获取蓝牙状态失败", e)
        }
        return false
    }


     fun isDeviceLocked(context: Context): Boolean {
        // 从存储中获取锁机状态
        return StorageUtil.getBoolean(AppConstants.LOCK_STATE, false)
    }

    fun isBindingEnabled(context: Context): Boolean {
          return StorageUtil.getBoolean(SimChangedReceiver.BINDING_ENABLED_KEY, false)
    }

    fun getDeviceStatus(context: Context): DeviceStatus {
        return DeviceStatus(
            batteryLevel = getBatteryLevel(context),
            signalStrength = getSignalStrength(context),
            isWifiEnabled = isWifiEnabled(context),
            isMobileDataEnabled = isMobileDataEnabled(context),
            isAirplaneModeEnabled = isAirplaneModeEnabled(context),
            isBluetoothEnabled = isBluetoothEnabled(),
            isDeviceLocked = isDeviceLocked(context),
            isBindingEnabled = isBindingEnabled(context)
        )
    }

    data class DeviceStatus(
        val batteryLevel: Int,
        val signalStrength: Int,
        val isWifiEnabled: Boolean,
        val isMobileDataEnabled: Boolean,
        val isAirplaneModeEnabled: Boolean,
        val isBluetoothEnabled: Boolean,
        val isDeviceLocked: Boolean,
        val isBindingEnabled: Boolean
    )
}