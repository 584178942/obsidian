package com.siyu.mdm.custom.device.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.siyu.mdm.custom.device.service.MqttService
import com.siyu.mdm.custom.device.util.HuaweiMDMManager
import com.siyu.mdm.custom.device.util.InitiateUtils
import com.siyu.mdm.custom.device.util.StorageUtil

class SimChangedReceiver : BroadcastReceiver() {
    private val initiateUtils = InitiateUtils()
    private val huaweiMDMManager = HuaweiMDMManager()

    companion object {
        private const val TAG = "SimChangedReceiver"
        private const val SIM_STATE_LOADED = "LOADED"
        private const val BINDING_CONFIG_KEY = "SERVER_MOCK_BINDING_ICCID_SET"
        const val BINDING_ENABLED_KEY = "SERVER_MOCK_BINDING_ENABLED"
        private val gson = Gson()

        // 默认绑定关系设置为空
        private val DEFAULT_BINDINGS = emptySet<String>()
    }

    @RequiresPermission(allOf = [
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.READ_PHONE_STATE
    ])
    override fun onReceive(context: Context, intent: Intent) {
        val simState = intent.getStringExtra("ss") ?: return

        LogUtils.i(TAG, "SIM卡状态变化: $simState")

        when (simState) {
            SIM_STATE_LOADED -> handleSimLoaded(context)
            else -> LogUtils.d(TAG, "SIM卡状态: $simState，无需处理")
        }
    }

    private fun handleSimLoaded(context: Context) {
        when (val result = verifySimBinding()) {
            is VerificationResult.Success -> {
                LogUtils.i(TAG, "机卡绑定验证成功")
                huaweiMDMManager.unlockDevice()
                notifyMqttService(context, SIM_STATE_LOADED)
            }
            is VerificationResult.Failure -> {
                LogUtils.e(TAG, "机卡绑定验证失败: ${result.reason}")
                handleVerificationFailure(result.reason)
            }
            is VerificationResult.NoBinding -> {
                LogUtils.i(TAG, "无绑定关系，跳过验证")
                // 绑定关系为空时不锁机
                huaweiMDMManager.unlockDevice()
                notifyMqttService(context, SIM_STATE_LOADED)
            }
        }
    }

    /**
     * 检查机卡绑定是否开启
     */
    private fun isBindingEnabled(): Boolean {
        return StorageUtil.getBoolean(BINDING_ENABLED_KEY, false)
    }

    @Throws(SecurityException::class)
        fun verifySimBinding(): VerificationResult {
            return try {
                // 检查机卡绑定开关是否开启
                if (!isBindingEnabled()) {
                    LogUtils.i(TAG, "机卡绑定功能未开启，跳过验证")
                    return VerificationResult.Success
                }

                val iccids = getValidIccids()
                // ICCID为空时不锁机，返回成功
                if (iccids.isEmpty()) {
                    LogUtils.i(TAG, "ICCID为空，不进行锁机操作")
                    return VerificationResult.Success
                }

                // 检查绑定关系是否为空
                if (ServerMock.isBindingEmpty()) {
                    return VerificationResult.NoBinding
                }
                val isValid = ServerMock.verify(iccids.toTypedArray())
                if (isValid) VerificationResult.Success else VerificationResult.Failure("SIM卡未绑定")

            } catch (e: SecurityException) {
                LogUtils.e(TAG, "验证时权限被拒绝", e)
                VerificationResult.Failure("权限被拒绝: ${e.message}")
            } catch (e: Exception) {
                LogUtils.e(TAG, "验证过程发生异常", e)
                VerificationResult.Failure("验证异常: ${e.message}")
            }
        }

    private fun getValidIccids(): List<String> {
        return initiateUtils.getIccidCode()
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun handleVerificationFailure(reason: String) {
        val message = when {
            reason.contains("权限", ignoreCase = true) -> "权限不足，无法验证SIM卡"
            reason.contains("SN", ignoreCase = true) -> "设备序列号获取失败"
            reason.contains("ICCID", ignoreCase = true) -> "SIM卡信息获取失败"
            else -> "此设备未绑定当前SIM卡，请插入已授权的SIM卡"
        }

        huaweiMDMManager.lockDevice("机卡绑定验证失败", message)
    }

    private fun notifyMqttService(context: Context, simState: String) {
        try {
            Intent(context, MqttService::class.java).apply {
                action = MqttService.ACTION_SIM_STATE_CHANGED
                putExtra(MqttService.EXTRA_SIM_STATE, simState)
                putExtra(MqttService.EXTRA_TIMESTAMP, System.currentTimeMillis())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(this)
                } else {
                    context.startService(this)
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "通知MqttService失败", e)
        }
    }

    /**
     * 验证结果密封类
     */
    sealed class VerificationResult {
        object Success : VerificationResult()
        object NoBinding : VerificationResult() // 新增：无绑定关系状态
        data class Failure(val reason: String) : VerificationResult()
    }

    /**
     * 服务器验证模拟类
     */
    object ServerMock {
        private val bindingIccidSet: Set<String>
            get() = try {
                StorageUtil.getString(BINDING_CONFIG_KEY)?.takeIf { it.isNotEmpty() }
                    ?.let { json ->
                        gson.fromJson(json, object : TypeToken<Set<String>>() {}.type)
                    } ?: DEFAULT_BINDINGS
            } catch (e: Exception) {
                LogUtils.e(TAG, "解析绑定关系失败，使用默认值", e)
                DEFAULT_BINDINGS
            }

        /**
         * 检查绑定关系是否为空
         */
        fun isBindingEmpty(): Boolean = bindingIccidSet.isEmpty()

        /**
         * 设置绑定关系
         */
        fun setBindings(iccids: Set<String>) {
            try {
                if (iccids.isEmpty()) {
                    StorageUtil.putString(BINDING_CONFIG_KEY, "")
                    LogUtils.i(TAG, "绑定关系已清空")
                } else {
                    StorageUtil.putString(BINDING_CONFIG_KEY, gson.toJson(iccids))
                    LogUtils.i(TAG, "绑定关系已更新，数量: ${iccids.size}")
                }
                // 绑定关系变更后触发SIM校验
                triggerSimVerification()
            } catch (e: Exception) {
                LogUtils.e(TAG, "设置绑定关系失败", e)
            }
        }

        /**
         * 添加单个绑定关系
         */
        fun addBinding(iccid: String) {
            if (iccid.isNotEmpty()) {
                val currentBindings = bindingIccidSet.toMutableSet()
                currentBindings.add(iccid)
                setBindings(currentBindings)
                // 绑定关系变更后触发SIM校验
                triggerSimVerification()
            }
        }

        /**
         * 移除绑定关系
         */
        fun removeBinding(iccid: String) {
            val currentBindings = bindingIccidSet.toMutableSet()
            if (currentBindings.remove(iccid)) {
                setBindings(currentBindings)
                LogUtils.i(TAG, "已移除ICCID: $iccid 的绑定关系")
                // 绑定关系变更后触发SIM校验
                triggerSimVerification()
            }
        }

        /**
         * 清除所有绑定关系
         */
        fun clearBindings() {
            setBindings(emptySet())
            LogUtils.i(TAG, "已清除所有绑定关系")
            // 绑定关系变更后触发SIM校验
            triggerSimVerification()
        }

        /**
         * 验证ICCID是否匹配
         * 注意：当绑定关系为空时，此方法返回false，但外部会先检查是否为空
         */
        fun verify(iccids: Array<String>): Boolean {
            return iccids.any { bindingIccidSet.contains(it) }
        }

        /**
         * 启用机卡绑定功能
         */
        fun enableBinding() {
            StorageUtil.putBoolean(BINDING_ENABLED_KEY, true)
            LogUtils.i(TAG, "机卡绑定功能已启用")
        }

        /**
         * 禁用机卡绑定功能
         */
        fun disableBinding() {
            StorageUtil.putBoolean(BINDING_ENABLED_KEY, false)
            LogUtils.i(TAG, "机卡绑定功能已禁用")
        }

        /**
         * 检查机卡绑定功能是否开启
         */
        fun isBindingEnabled(): Boolean {
            return StorageUtil.getBoolean(BINDING_ENABLED_KEY, false)
        }

        /**
         * 获取当前绑定关系数量
         */
        fun getBindingCount(): Int = bindingIccidSet.size

        /**
         * 检查ICCID是否已绑定
         */
        fun containsIccid(iccid: String): Boolean = bindingIccidSet.contains(iccid)

        /**
         * 获取当前绑定关系
         */
        fun getCurrentBindings(): Set<String> = bindingIccidSet

        /**
         * 触发SIM卡验证
         * 当绑定关系发生变化时调用，立即验证当前SIM卡状态
         */
        private fun triggerSimVerification() {
            try {
                val receiver = SimChangedReceiver()
                val result = receiver.verifySimBinding()
                LogUtils.i(TAG, "绑定关系变更后触发SIM卡验证，结果: $result")
                
                // 处理验证失败的情况
                if (result is VerificationResult.Failure) {
                    LogUtils.e(TAG, "验证失败，执行锁机操作")
                    receiver.handleVerificationFailure(result.reason)
                } else if (result is VerificationResult.Success) {
                    LogUtils.i(TAG, "验证成功，保持设备解锁状态")
                    receiver.huaweiMDMManager.unlockDevice()
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "触发SIM卡验证失败", e)
            }
        }
    }
}