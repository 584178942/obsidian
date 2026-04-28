package com.siyu.mdm.custom.device

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.appcompat.app.AlertDialog
import com.blankj.utilcode.util.LogUtils

class CustomOverlayService : AccessibilityService() {

    private lateinit var overlayWindowManager: WindowManager
    private var overlayView: View? = null
    private val TAG = "CustomOverlayService"

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            // 初始化WindowManager
            overlayWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            // 加载布局
            overlayView = LayoutInflater.from(this).inflate(R.layout.layout_watermark, null)
            // 设置布局参数
            val params: WindowManager.LayoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                // 根据Android版本使用适当的窗口类型
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or  // 不抢占焦点
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or  // 不可触摸
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,  // 显示在屏幕上
                PixelFormat.TRANSLUCENT
            )
            // 设置位置，例如屏幕中央
            params.gravity = Gravity.CENTER
            // 添加到WindowManager
            overlayWindowManager.addView(overlayView, params)
            LogUtils.i(TAG, "无障碍服务已成功启动，覆盖层已添加")
        } catch (e: Exception) {
            LogUtils.e(TAG, "初始化覆盖层失败", e)
            // 处理初始化失败的情况
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (overlayView != null) {
                overlayWindowManager.removeView(overlayView)
                overlayView = null
                LogUtils.i(TAG, "服务已销毁，覆盖层已移除")
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "移除覆盖层时发生错误", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 无障碍事件处理，不做任何操作
    }

    override fun onInterrupt() {
        // 中断时的处理
        LogUtils.w(TAG, "无障碍服务被中断")
    }

    companion object {
        /**
         * 检查无障碍服务是否已启用
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val serviceName = context.packageName + "/" + CustomOverlayService::class.java.canonicalName
            try {
                val accessibilityEnabled = Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
                if (accessibilityEnabled == 1) {
                    val settingValue = Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                    )
                    if (settingValue != null) {
                        val colonSplitter = settingValue.split(":")
                        return colonSplitter.any { it.equals(serviceName, ignoreCase = true) }
                    }
                }
            } catch (e: Settings.SettingNotFoundException) {
                LogUtils.e( "检查无障碍服务状态时发生错误", e)
            }
            return false
        }

        /**
         * 引导用户启用无障碍服务
         */
        fun guideUserToEnableAccessibility(context: Context) {
            if (!isAccessibilityServiceEnabled(context)) {
                AlertDialog.Builder(context)
                    .setTitle("启用无障碍服务")
                    .setMessage("需要启用无障碍服务才能显示水印。请点击确定并在设置中开启服务。")
                    .setPositiveButton("确定") { _, _ ->
                        // 跳转到无障碍设置页面
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        /**
         * 便捷方法：检查并启动无障碍服务
         */
        fun checkAndStartService(context: Context): Boolean {
            if (isAccessibilityServiceEnabled(context)) {
                // 如果已启用，启动服务
                context.startService(Intent(context, CustomOverlayService::class.java))
                return true
            } else {
                // 如果未启用，引导用户启用
                guideUserToEnableAccessibility(context)
                return false
            }
        }
    }
}