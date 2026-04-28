package com.siyu.mdm.enterprise.util.mqtt

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.PowerManager
import com.siyu.mdm.enterprise.util.LogUtils

/**
 * MQTT 电源管理策略
 * 
 * 智能控制功耗，根据设备状态调整连接策略
 */
object MqttPowerManager {
    
    private const val TAG = "MqttPowerManager"
    
    private var context: Context? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var connectivityManager: ConnectivityManager? = null
    private var powerManager: PowerManager? = null
    
    // 是否正在监听网络状态
    private var isListening = false
    
    // 网络恢复监听器
    private var onNetworkAvailable: (() -> Unit)? = null
    
    /**
     * 初始化
     */
    fun initialize(ctx: Context, onNetworkAvailable: () -> Unit) {
        context = ctx.applicationContext
        this.onNetworkAvailable = onNetworkAvailable
        connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        powerManager = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        
        LogUtils.i(TAG, "电源管理器初始化")
    }
    
    /**
     * 开始监听网络状态
     */
    fun startNetworkListening() {
        if (isListening) {
            LogUtils.d(TAG, "网络监听已在运行")
            return
        }
        
        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    LogUtils.i(TAG, "✅ 网络已恢复")
                    onNetworkAvailable?.invoke()
                }
                
                override fun onLost(network: Network) {
                    super.onLost(network)
                    LogUtils.w(TAG, "❌ 网络已断开")
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    super.onCapabilitiesChanged(network, capabilities)
                    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    LogUtils.d(TAG, "网络能力变化: hasInternet=$hasInternet")
                }
            }
            
            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
            isListening = true
            LogUtils.i(TAG, "网络监听已启动")
            
        } catch (e: Exception) {
            LogUtils.e(TAG, "启动网络监听失败", e)
        }
    }
    
    /**
     * 停止监听网络状态
     */
    fun stopNetworkListening() {
        if (!isListening || networkCallback == null) {
            return
        }
        
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback!!)
            isListening = false
            networkCallback = null
            LogUtils.i(TAG, "网络监听已停止")
        } catch (e: Exception) {
            LogUtils.e(TAG, "停止网络监听失败", e)
        }
    }
    
    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean {
        val connectivity = connectivityManager ?: return false
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * 获取心跳间隔（毫秒）
     * 根据设备状态智能调整
     */
    fun getHeartbeatInterval(
        batteryLevel: Int,
        isCharging: Boolean,
        isScreenOn: Boolean
    ): Long {
        return when {
            // 充电中且电量充足：高性能模式
            isCharging && batteryLevel > 50 -> {
                LogUtils.d(TAG, "心跳模式: 高性能（充电中）")
                60_000L // 1分钟
            }
            
            // 低电量：省电模式
            batteryLevel < 20 && !isCharging -> {
                LogUtils.d(TAG, "心跳模式: 省电（低电量）")
                300_000L // 5分钟
            }
            
            // 屏幕关闭且未充电：超省电模式
            !isScreenOn && !isCharging -> {
                LogUtils.d(TAG, "心跳模式: 超省电（熄屏）")
                600_000L // 10分钟
            }
            
            // 正常模式
            else -> {
                LogUtils.d(TAG, "心跳模式: 正常")
                120_000L // 2分钟
            }
        }
    }
    
    /**
     * 获取重连延迟（毫秒）
     * 根据断网时间智能调整
     */
    fun getReconnectDelay(disconnectTimeMillis: Long): Long {
        val minutes = disconnectTimeMillis / 60_000L
        
        return when {
            // 5分钟内：快速重连
            minutes < 5 -> {
                5_000L // 5秒
            }
            
            // 30分钟内：正常重连
            minutes < 30 -> {
                30_000L // 30秒
            }
            
            // 1小时内：较慢重连
            minutes < 60 -> {
                60_000L // 1分钟
            }
            
            // 1-6小时：慢速重连
            minutes < 360 -> {
                180_000L // 3分钟
            }
            
            // 6小时以上：极慢重连
            else -> {
                300_000L // 5分钟
            }
        }
    }
    
    /**
     * 检查是否应该省电
     */
    fun shouldSavePower(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return powerManager?.isPowerSaveMode ?: false
        }
        return false
    }
    
    /**
     * 获取最佳心跳间隔（考虑系统省电模式）
     */
    fun getOptimalHeartbeatInterval(
        batteryLevel: Int,
        isCharging: Boolean,
        isScreenOn: Boolean
    ): Long {
        // 如果系统开启省电模式，进一步降低频率
        return if (shouldSavePower()) {
            LogUtils.d(TAG, "系统省电模式: 进一步降低心跳频率")
            getHeartbeatInterval(batteryLevel, false, false)
        } else {
            getHeartbeatInterval(batteryLevel, isCharging, isScreenOn)
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopNetworkListening()
        context = null
        onNetworkAvailable = null
        connectivityManager = null
        powerManager = null
        LogUtils.i(TAG, "电源管理器已释放")
    }
}
