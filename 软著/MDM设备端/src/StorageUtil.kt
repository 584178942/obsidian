package com.siyu.mdm.custom.device.util

import android.content.Context
import com.siyu.mdm.custom.device.App.Companion.instance
import android.content.SharedPreferences
object StorageUtil {
    private fun getSharedPreferences(): SharedPreferences {
        return if (instance.isDeviceProtectedStorage) {
            val deviceProtectedContext = instance.createDeviceProtectedStorageContext()
            deviceProtectedContext.getSharedPreferences(instance.packageName, Context.MODE_PRIVATE)
        } else {
            // 返回普通SharedPreferences
            instance.getSharedPreferences(instance.packageName, Context.MODE_PRIVATE)
        }
    }

    // 存储方法
    fun putString(key: String, value: String?) {
        put(key, value)
    }

    fun putInt(key: String, value: Int) {
        put(key, value)
    }

    fun putBoolean(key: String, value: Boolean) {
        put(key, value)
    }

    fun putFloat(key: String, value: Float) {
        put(key, value)
    }

    fun putLong(key: String, value: Long) {
        put(key, value)
    }

    fun put(key: String, value: Any?) {
        val editor = getSharedPreferences().edit()
        when (value) {
            null -> {
                editor.putString(key, null)
            }
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Float -> editor.putFloat(key, value)
            is Long -> editor.putLong(key, value)
            else -> editor.putString(key, value.toString())
        }
        editor.apply()
    }

    // 获取方法
    fun getString(key: String, defaultValue: String? = null): String? {
        return getSharedPreferences().getString(key, defaultValue)
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return getSharedPreferences().getInt(key, defaultValue)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return getSharedPreferences().getBoolean(key, defaultValue)
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return getSharedPreferences().getFloat(key, defaultValue)
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return getSharedPreferences().getLong(key, defaultValue)
    }
}