package com.siyu.mdm.enterprise

import android.app.Application
import com.siyu.mdm.enterprise.util.LogUtils

/**
 * Application类
 * 负责应用全局初始化
 */
class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initLog()
    }

    private fun initLog() {
        LogUtils.setTag("MDM-Enterprise")
        LogUtils.setLogSwitch(true)
    }
}
