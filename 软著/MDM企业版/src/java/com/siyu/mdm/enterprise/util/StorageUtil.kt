package com.siyu.mdm.enterprise.util

import android.content.Context
import android.content.SharedPreferences
import com.siyu.mdm.enterprise.App

/**
 * 存储工具类
 * 使用Android标准SharedPreferences
 */
object StorageUtil {

    private const val DEFAULT_SP_NAME = "mdm_enterprise_sp"

    @Volatile
    private var sp: SharedPreferences? = null

    private fun getSP(context: Context = App.instance): SharedPreferences {
        return sp ?: synchronized(this) {
            sp ?: context.getSharedPreferences(DEFAULT_SP_NAME, Context.MODE_PRIVATE).also { sp = it }
        }
    }

    fun putString(key: String, value: String): Boolean {
        return getSP().edit().putString(key, value).commit()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return getSP().getString(key, defaultValue) ?: defaultValue
    }

    fun putInt(key: String, value: Int): Boolean {
        return getSP().edit().putInt(key, value).commit()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return getSP().getInt(key, defaultValue)
    }

    fun putLong(key: String, value: Long): Boolean {
        return getSP().edit().putLong(key, value).commit()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return getSP().getLong(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean): Boolean {
        return getSP().edit().putBoolean(key, value).commit()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return getSP().getBoolean(key, defaultValue)
    }

    fun remove(key: String): Boolean {
        return getSP().edit().remove(key).commit()
    }

    fun clear(): Boolean {
        return getSP().edit().clear().commit()
    }

    fun contains(key: String): Boolean {
        return getSP().contains(key)
    }
}
