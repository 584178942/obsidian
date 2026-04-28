package com.siyu.mdm.custom.device.util

import ApkInstaller
import android.Manifest
import android.Manifest.permission.READ_PHONE_STATE
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.os.Build.MANUFACTURER
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.App.Companion.instance
import com.siyu.mdm.custom.device.SampleDeviceReceiver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.siyu.mdm.custom.device.json.UpdateBean
import com.siyu.mdm.custom.device.json.DeviceInfo
import com.siyu.mdm.custom.device.service.MqttService
import com.siyu.mdm.custom.device.ui.FullScreenActivity
import com.siyu.mdm.custom.device.util.AppConstants.BIND
import com.siyu.mdm.custom.device.util.AppConstants.BIND_STATE
import com.siyu.mdm.custom.device.util.AppConstants.FIRST_STATE
import com.siyu.mdm.custom.device.util.AppConstants.LOCK_STATE
import com.siyu.mdm.custom.device.util.AppConstants.UN_BIND
import com.siyu.mdm.custom.device.util.AppConstants.UN_LOCK
import java.io.File
import java.lang.reflect.Method
import kotlin.math.min


class InitiateUtils() {
    // 检测是否为华为设备
    private val isHuaweiDevice: Boolean by lazy {
        MANUFACTURER.equals("huawei", ignoreCase = true) || MANUFACTURER.equals("honor", ignoreCase = true)
    }
    
    // 使用反射安全地获取DeviceApplicationManager（华为特定类）
    private var deviceApplicationManager: Any? = null
    private var deviceHwSystemManager: Any? = null
    private var devicePackageManager: Any? = null
    private var huaweiMDMManager: Any? = null
    private val mAdminName = ComponentName(instance, SampleDeviceReceiver::class.java)
    
    init {
        // 只在华为设备上初始化华为特定类
        if (isHuaweiDevice) {
            initHuaweiSpecificManagers()
        } else {
            LogUtils.d("非华为设备，跳过华为特定MDM功能初始化")
        }
    }
    
    /**
     * 初始化华为特定的管理器
     */
    private fun initHuaweiSpecificManagers() {
        try {
            // 初始化DeviceApplicationManager
            val damClass = Class.forName("com.huawei.android.app.admin.DeviceApplicationManager")
            deviceApplicationManager = damClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            LogUtils.d("成功初始化DeviceApplicationManager")
        } catch (e: Exception) {
            LogUtils.w("无法初始化DeviceApplicationManager，设备可能不支持华为MDM功能", e)
        }
        
        try {
            // 初始化DeviceHwSystemManager
            val dhmClass = Class.forName("com.huawei.android.app.admin.DeviceHwSystemManager")
            deviceHwSystemManager = dhmClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            LogUtils.d("成功初始化DeviceHwSystemManager")
        } catch (e: Exception) {
            LogUtils.w("无法初始化DeviceHwSystemManager，设备可能不支持华为MDM功能", e)
        }
        
        try {
            // 初始化DevicePackageManager
            val dpmClass = Class.forName("com.huawei.android.app.admin.DevicePackageManager")
            devicePackageManager = dpmClass.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
            LogUtils.d("成功初始化DevicePackageManager")
        } catch (e: Exception) {
            LogUtils.w("无法初始化DevicePackageManager，设备可能不支持华为MDM功能", e)
        }
        
        try {
            // 初始化HuaweiMDMManager
            huaweiMDMManager = Class.forName("com.siyu.mdm.custom.device.util.HuaweiMDMManager").getDeclaredConstructor().newInstance()
            LogUtils.d("成功初始化HuaweiMDMManager")
        } catch (e: Exception) {
            LogUtils.w("无法初始化HuaweiMDMManager", e)
        }
    }
    
    /**
     * 使用反射安全地调用华为特定管理器的方法
     */
    private fun <T> safeCallHuaweiMethod(target: Any?, methodName: String, vararg args: Any?): T? {
        if (target == null) {
            LogUtils.w("目标管理器未初始化，跳过调用: $methodName")
            return null
        }
        
        return try {
            val method = target.javaClass.getMethod(methodName, *args.map { it?.javaClass ?: Any::class.java }.toTypedArray())
            method.invoke(target, *args) as T?
        } catch (e: Exception) {
            LogUtils.w("调用华为特定方法失败: $methodName", e)
            null
        }
    }

    /**
     * Application activation
     */
    fun initApp(){
        val packageNameList = ArrayList<String>()
        packageNameList.add(instance.packageName)
        setPermissions()
        
        // 只在华为设备上调用华为特定方法
        if (isHuaweiDevice) {
            safeCallHuaweiMethod<Any>(deviceApplicationManager, "addPersistentApp", mAdminName, packageNameList)
            safeCallHuaweiMethod<Any>(deviceHwSystemManager, "setSuperTrustListForHwSystemManger", mAdminName, packageNameList)
        }
    }

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager?
        return dpm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                dpm.isDeviceOwnerApp(context.packageName)
    }

   /* // 在开源版本中，没有风味，默认是"opensource"
    fun getLauncherVariant(): String {
        return if (BuildConfig.FLAVOR.isNullOrEmpty()) "opensource" else BuildConfig.FLAVOR
    }*/
   fun setPermissions() {
        LogUtils.i("mAdminName.getPackageName()", mAdminName.packageName);
        val sDevicePolicyManager = instance.getSystemService(DevicePolicyManager::class.java)

        if (sDevicePolicyManager.isAdminActive(mAdminName)) {
            try {
                // 设置权限授予状态
                sDevicePolicyManager.setPermissionGrantState(mAdminName,
                    mAdminName.packageName,
                    READ_PHONE_STATE,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

               /* sDevicePolicyManager.setPermissionGrantState(mAdminName,
                    mAdminName.getPackageName(),
                    Manifest.permission.SYSTEM_ALERT_WINDOW,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

                sDevicePolicyManager.setPermissionGrantState(mAdminName,
                    mAdminName.getPackageName(),
                    Manifest.permission.CALL_PHONE,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

                sDevicePolicyManager.setPermissionGrantState(mAdminName,
                    mAdminName.getPackageName(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

                sDevicePolicyManager.setPermissionGrantState(mAdminName,
                    mAdminName.getPackageName(),
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);*/
                LogUtils.i("Permissions", "Permissions granted successfully.");
            } catch (e:Exception) {
                LogUtils.i("Permissions", "Failed to grant permissions: " + e.localizedMessage);
            }
        } else {
            LogUtils.i("Permissions", "Device owner is not active or API level is below M.");
        }
    }

    fun setComponentLaunchedByLauncher(string: String){
        // 只在华为设备上调用华为特定方法
        if (isHuaweiDevice) {
            val result = safeCallHuaweiMethod<Boolean>(deviceApplicationManager, "setComponentLaunchedByLauncher", mAdminName, string)
            LogUtils.i("setComponentLaunchedByLauncher", "$result")
        }
    }

     /**
      * 启动锁定屏幕活动
      * @param mainText 主文本内容，默认为设备已锁定
      * @param subText 副标题内容，默认为请联系管理员解锁
      */
     fun startLockActivity(mainText: String? = null, subText: String? = null) {
         try {
             // 只在华为设备上调用华为特定方法
             if (isHuaweiDevice) {
                 safeCallHuaweiMethod<Any>(deviceApplicationManager, "addSingleApp", mAdminName, instance.packageName)
             }
             
             val dpm: DevicePolicyManager = instance.getSystemService(DevicePolicyManager::class.java)
             // 检查当前 App 是否为 Device Owner, 只有 Device Owner 才可以设置锁定
             //  if (!dpm.isDeviceOwnerApp(contextApp.getPackageName())) return;
             // 添加 com.foo.bar 应用到锁定任务模式的许可名单
             dpm.setLockTaskPackages(
                 mAdminName,
                 arrayOf<String>(instance.packageName)
             )
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                 // 设置锁定任务模式下系统状态栏显示时间、电量和网络等信息
                 dpm.setLockTaskFeatures(
                     mAdminName,
                     DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO
                 )

                 // dpm.addUserRestriction(component, UserManager.DISALLOW_CREATE_WINDOWS);
                 // LogUtils.info("setLockTaskFeatures","LOCK_TASK_FEATURE_GLOBAL_ACTIONS"+dpm.getLockTaskFeatures(component));
             }
             val intent = Intent(instance, FullScreenActivity::class.java)
             intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
             
             // 设置自定义文字内容
             mainText?.let {
                 intent.putExtra(FullScreenActivity.EXTRA_MAIN_TEXT, it)
             }
             subText?.let {
                 intent.putExtra(FullScreenActivity.EXTRA_SUB_TEXT, it)
             }
             
             var am = instance.getSystemService(ActivityManager::class.java)
             if (!am.isInLockTaskMode) {
                 intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
             }
             var options: ActivityOptions? = null

             options = ActivityOptions.makeBasic()
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                 options.setLockTaskEnabled(true)
             }
             instance.startActivity(intent, options.toBundle())
         } catch (e: SecurityException) {
             e.localizedMessage?.let { LogUtils.e( it) }
         } catch (unused: ActivityNotFoundException) {
             LogUtils.e(
                 "startLockActivity ActivityNotFoundException Error :" + unused.localizedMessage
             )
         }
     }
    fun closeLockActivity() {
        try {
            // 只在华为设备上调用华为特定方法
            if (isHuaweiDevice) {
                safeCallHuaweiMethod<Any>(deviceApplicationManager, "clearSingleApp", mAdminName, instance.packageName)
            }
            
            val dpm: DevicePolicyManager = 
                instance.getSystemService(DevicePolicyManager::class.java)
            dpm.setLockTaskPackages(
               mAdminName,
                arrayOf<String>()
            )
        } catch (e: Exception) {
            LogUtils.e( "closeLockActivity:" + e.localizedMessage
            )
        }
    }
    @SuppressLint("HardwareIds", "MissingPermission")
    fun getIMEI(): String? {
        return try {
            val telephonyManager = instance.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            // 检查权限
            if (checkSelfPermission(instance,READ_PHONE_STATE) != PERMISSION_GRANTED
            ) {
                return "PERMISSION_REQUIRED"
            }
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    telephonyManager.getImei(0).toString() // 获取第一个 SIM 卡槽的 IMEI
                }
                else -> {
                    telephonyManager.deviceId // 旧版本方式
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            "PERMISSION_DENIED"
        } catch (e: Exception) {
            e.printStackTrace()
            "ERROR: ${e.message}"
        }
    }
    @SuppressLint("MissingPermission")
    fun getSerialNumber(): String? {
        // 1. Android P（API 28）及以上：使用Build.getSerial()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val serial = Build.getSerial()
                LogUtils.d( "通过Build.getSerial()获取序列号: $serial")
                return serial
            } catch (e: SecurityException) {
                LogUtils.e( "Build.getSerial()需要READ_PHONE_STATE权限", e)
            }
        }

        // 2. 反射获取系统属性"ril.serialnumber"
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod: Method = systemPropertiesClass.getMethod("get", String::class.java)
            val serialNumber = getMethod.invoke(systemPropertiesClass, "ril.serialnumber") as String?
            if (!serialNumber.isNullOrEmpty()) {
                LogUtils.d("通过ril.serialnumber获取序列号: $serialNumber")
                return serialNumber
            }
        } catch (e: Exception) {
            LogUtils.w("反射获取ril.serialnumber失败", e)
        }

        // 3. 降级方案：使用Build.SERIAL（已弃用）
        val buildSerial = Build.SERIAL
        LogUtils.d( "通过Build.SERIAL获取序列号: $buildSerial")
        return buildSerial.takeIf { it.isNotEmpty() } ?: "unknown"
    }

  fun getIccidCode(): Array<String?> {
        val iccids = arrayOfNulls<String>(2)
      val sm = SubscriptionManager.from(instance)
            ?: return iccids // 设备不支持 SIM 卡管理
        // 检查权限
      if (checkSelfPermission(instance, READ_PHONE_STATE) == PERMISSION_GRANTED) {
          try {
              val sis = sm.activeSubscriptionInfoList
              if (sis.isNullOrEmpty()) {
                  return iccids // 无 SIM 卡信息
              }
              // 填充 ICCID
              for (i in 0 until min(sis.size.toDouble(), 2.0).toInt()) {
                  val si = sis[i]
                  iccids[i] = si.iccId
              }
          } catch (e: SecurityException) {
              e.printStackTrace() // 捕获权限异常
          } catch (e: java.lang.Exception) {
              e.printStackTrace() // 其他异常处理
          }

          return iccids
      }
      return iccids // 立即返回，等待下次调用
  }
    fun wipeDevice(instance: Context) {
        val devicePolicyManager = instance.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            if (devicePolicyManager.isAdminActive(mAdminName)) {
                devicePolicyManager.wipeData(0)
                LogUtils.i( "恢复出厂设置已触发")
            } else {
                LogUtils.i( "Application is not the device owner.")
            }
        } catch (e: SecurityException) {
            LogUtils.i( "权限不足: ${e.message}")
        } catch (e: Exception) {
            LogUtils.i("恢复出厂设置失败: ${e.message}")
        }
    }
    private fun heartBeat() {
            OkHttpManager.instance.request<UpdateBean> {
                url = "https://api.example.com/login"
                method = "POST"
                params = mutableMapOf(
                    "imeiCode" to getIMEI().toString(),
                    "iccId" to getIccidCode()[0].toString(),
                    "activationState" to '1',
                    "version" to instance.packageManager.getPackageInfo(instance.packageName, 0),
                    BIND_STATE to UN_BIND,
                    LOCK_STATE to UN_LOCK,
                    FIRST_STATE to true
                )
                responseType = UpdateBean::class.java
                onSuccess = {
                    /* saveAuthToken(response.token)
                     navigateToHome()*/
                }
                onError = { e ->
                   // showError("登录失败: ${e.message}")
                }

           // hideLoading()
        }
    }

    fun getImsis(): Array<String?> {
        val imsis = arrayOfNulls<String>(2) // 存储卡槽0和卡槽1的IMSI

        // 1. 权限检查：无READ_PHONE_STATE权限返回空数组
        if (ContextCompat.checkSelfPermission(instance, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            LogUtils.d("getImsis", "无READ_PHONE_STATE权限，返回空IMSI数组")
            return imsis
        }

        // 2. 版本检查：低于LOLLIPOP_MR1不支持多卡槽API
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            LogUtils.d("getImsis", "系统版本低于LOLLIPOP_MR1，不支持多卡槽")
            return imsis
        }

        // 3. 获取SubscriptionManager服务
        val subscriptionManager = instance.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: run {
                LogUtils.d("getImsis", "获取SubscriptionManager失败")
                return imsis
            }

        try {
            // 4. 获取所有激活的订阅信息（替代直接按slotId获取，更可靠）
            val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
            if (activeSubscriptions.isNullOrEmpty()) {
                LogUtils.d("getImsis", "无激活的SIM卡订阅信息")
                return imsis
            }

            // 5. 遍历激活的订阅，按slotId匹配到对应的卡槽
            for (subscription in activeSubscriptions) {
                val slotId = subscription.simSlotIndex // 订阅对应的卡槽ID（0或1）
                val subId = subscription.subscriptionId // 订阅ID

                // 只处理0和1两个卡槽（超出范围忽略）
                if (slotId < 0 || slotId >= 2) {
                    LogUtils.d("getImsis", "无效的卡槽ID：$slotId，跳过")
                    continue
                }

                // 6. 根据订阅ID创建对应的TelephonyManager
                val telephonyManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    (instance.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
                        .createForSubscriptionId(subId)
                } else {
                    // 低版本直接使用默认TelephonyManager（可能不准确，但兼容）
                    instance.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                }

                // 7. 获取IMSI（subscriberId）并赋值
                val imsi = telephonyManager.subscriberId
                imsis[slotId] = imsi
                LogUtils.d("getImsis", "卡槽$slotId 订阅ID:$subId，获取到IMSI:${imsi ?: "null"}")
            }
        } catch (e: Exception) {
            LogUtils.e("getImsis", "获取IMSI时发生异常", e)
        }

        return imsis
    }

    fun installApp(apkUrls: List<String>) {
        try {
            LogUtils.i( "开始安装应用，APK数量: ${apkUrls.size}")
            val destinationDir = instance.getExternalFilesDir(null)?.absolutePath ?: run {
                LogUtils.e("无法获取存储路径，安装失败")
                return
            }
            LogUtils.i("下载目录: $destinationDir")

            NetUtils.getInstance().downloadApks(
                apkUrls = apkUrls,
                destinationDir = destinationDir,
                onAllComplete = {
                    LogUtils.i("所有APK下载完成，开始安装")
                    val apkFiles = File(destinationDir).listFiles { _, name -> 
                        name.endsWith(".apk") 
                    } ?: run {
                        LogUtils.e("没有找到下载的APK文件")
                        return@downloadApks
                    }

                    if (apkFiles.isEmpty()) {
                        LogUtils.e( "下载的APK文件列表为空")
                        return@downloadApks
                    }

                    LogUtils.i( "找到${apkFiles.size}个APK文件，准备安装")
                    val installer = ApkInstaller(instance, installCallback)
                    installer.enqueueApks(apkFiles, huaweiMDMManager as HuaweiMDMManager, mAdminName)
                },
                onSingleComplete = { file ->
                    LogUtils.i( "单个APK下载完成: ${file.absolutePath}")
                },
                onError = { url, e ->
                    LogUtils.e( "下载APK失败: $url, 错误: ${e.message}", e)
                }
            )
        } catch (e: Exception) {
            LogUtils.e("执行应用安装过程中发生异常", e)
        }
    }
    fun uninstalls(packageNames: List<String>) {

        val installer = ApkInstaller(instance)
        installer.enqueueUninstalls(packageNames, huaweiMDMManager as HuaweiMDMManager, mAdminName)
    }

    /**
     * 保存本地机卡绑定关系
     * @param deviceSn 设备序列号
     * @param iccid SIM卡ICCID
     * @return 是否保存成功
     */
    fun saveLocalSimBinding(deviceSn: String, iccid: String): Boolean {
        return try {
            if (deviceSn.isEmpty() || iccid.isEmpty()) {
                LogUtils.e("保存机卡绑定失败：设备SN或ICCID为空")
                return false
            }

            // 获取现有的绑定关系
            val bindings = getLocalSimBindings().toMutableMap()
            // 更新或添加绑定
            bindings[deviceSn] = iccid
            // 保存到存储
            StorageUtil.putString("SIM_BINDINGS", Gson().toJson(bindings))
            LogUtils.i("成功保存本地机卡绑定关系：SN=$deviceSn, ICCID=$iccid")
            true
        } catch (e: Exception) {
            LogUtils.e("保存本地机卡绑定关系失败", e)
            false
        }
    }

    /**
     * 获取所有本地机卡绑定关系
     * @return 设备SN与ICCID的映射
     */
    fun getLocalSimBindings(): Map<String, String> {
        return try {
            val json = StorageUtil.getString("SIM_BINDINGS", "{}")
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            LogUtils.e("获取本地机卡绑定关系失败", e)
            emptyMap()
        }
    }

    /**
     * 删除本地机卡绑定关系
     * @param deviceSn 设备序列号
     * @return 是否删除成功
     */
    fun removeLocalSimBinding(deviceSn: String): Boolean {
        return try {
            if (deviceSn.isEmpty()) {
                LogUtils.e("删除机卡绑定失败：设备SN为空")
                return false
            }

            val bindings = getLocalSimBindings().toMutableMap()
            if (bindings.containsKey(deviceSn)) {
                bindings.remove(deviceSn)
                StorageUtil.putString("SIM_BINDINGS", Gson().toJson(bindings))
                LogUtils.i("成功删除本地机卡绑定关系：SN=$deviceSn")
                true
            } else {
                LogUtils.w("删除机卡绑定失败：未找到该设备的绑定关系")
                false
            }
        } catch (e: Exception) {
            LogUtils.e("删除本地机卡绑定关系失败", e)
            false
        }
    }
}
val installCallback = object : ApkInstaller.InstallCallback {
    override fun onInstallStarted(apkFile: File) {
        LogUtils.i("ApkInstaller", "开始安装: ${apkFile.absolutePath}")
    }

    override fun onInstallSuccess(apkFile: File) {
        LogUtils.i("ApkInstaller", "安装成功: ${apkFile.absolutePath}")
        // 安装成功后删除APK文件
        try {
            if (apkFile.exists() && apkFile.delete()) {
                LogUtils.i("ApkInstaller", "成功删除APK文件: ${apkFile.absolutePath}")
            } else {
                LogUtils.w("ApkInstaller", "删除APK文件失败: ${apkFile.absolutePath}")
            }
        } catch (e: Exception) {
            LogUtils.e("ApkInstaller", "删除APK文件时发生异常: ${e.message}")
        }
    }

    override fun onInstallFailed(apkFile: File, error: Exception?) {
        LogUtils.i("ApkInstaller", "安装失败: ${apkFile.absolutePath}, 错误: ${error?.message}")
    }

    override fun onUninstallStarted(packageName: String) {
        LogUtils.i("ApkInstaller", "开始卸载: $packageName")
    }

    override fun onUninstallSuccess(packageName: String) {
        LogUtils.i("ApkInstaller", "卸载成功: $packageName")
    }

    override fun onUninstallFailed(packageName: String, error: Exception?) {
        LogUtils.i("ApkInstaller", "卸载失败: $packageName, 错误: ${error?.message}")
    }

    override fun onAllCompleted() {
        LogUtils.i("ApkInstaller", "所有任务完成")
    }
}

    /**
     * 重启应用
     * 在应用自身更新后调用，确保新版本正常启动
     */
    fun restartApp() {
        LogUtils.i("开始重启应用")
        try {
            // 创建启动主Activity的意图
            val packageManager = instance.packageManager
            val intent = packageManager.getLaunchIntentForPackage(instance.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 添加延迟，确保更新完全完成
            Thread { 
                try {
                    Thread.sleep(1000) // 延迟1秒
                    instance.startActivity(intent)
                    
                    // 启动必要的服务
                    val mqttServiceIntent = Intent(instance, MqttService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) {
                        instance.startForegroundService(mqttServiceIntent)
                    } else {
                        instance.startService(mqttServiceIntent)
                    }
                    
                    LogUtils.i("应用重启成功")
                } catch (e: Exception) {
                    LogUtils.e("重启应用时发生异常", e)
                }
            }.start()
        } catch (e: Exception) {
            LogUtils.e("准备重启应用时发生异常", e)
        }
    }