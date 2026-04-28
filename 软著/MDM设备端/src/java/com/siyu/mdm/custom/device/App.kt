package com.siyu.mdm.custom.device

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.receiver.ScreenStateReceiver

class App : Application() {
    companion object {
        lateinit var instance: App
            private set // 限制外部直接设置instance
    }

    private lateinit var screenStateReceiver: ScreenStateReceiver

    override fun onCreate() {
        super.onCreate()
        instance = this
        //应用初始化
        LogUtils.getConfig()
            .setLogSwitch(true) // 调试模式开启日志
            .setConsoleSwitch(true) // 控制台输出日志
            .setGlobalTag("mdmService") // 设置全局日志标签
        // 记录应用启动日志
        LogUtils.i("App", "应用启动")
        
        // 动态注册ScreenStateReceiver以确保能接收到屏幕状态变化广播
        registerScreenStateReceiver()
    }
    
    private fun registerScreenStateReceiver() {
        screenStateReceiver = ScreenStateReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_SCREEN_ON)
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenStateReceiver, intentFilter)
        LogUtils.i("App", "ScreenStateReceiver已动态注册")
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // 应用销毁时注销广播接收器
        try {
            unregisterReceiver(screenStateReceiver)
            LogUtils.i("App", "ScreenStateReceiver已注销")
        } catch (e: Exception) {
            LogUtils.e("App", "注销ScreenStateReceiver失败: ${e.message}")
        }
    }
}