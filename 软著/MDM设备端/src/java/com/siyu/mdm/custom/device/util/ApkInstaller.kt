import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.blankj.utilcode.util.LogUtils
import com.siyu.mdm.custom.device.App
import com.siyu.mdm.custom.device.util.HuaweiMDMManager
import java.io.File

class ApkInstaller(
    private val context: Context,
    private val callback: InstallCallback? = null
) {

    // 安装队列
    private val apkQueue = mutableListOf<File>()

    // 卸载队列
    private val uninstallQueue = mutableListOf<String>()

    // 安装回调接口
    interface InstallCallback {
        fun onInstallStarted(apkFile: File)
        fun onInstallSuccess(apkFile: File)
        fun onInstallFailed(apkFile: File, error: Exception?)
        fun onUninstallStarted(packageName: String)
        fun onUninstallSuccess(packageName: String)
        fun onUninstallFailed(packageName: String, error: Exception?)
        fun onAllCompleted()
    }

    /**
     * 批量安装 APK 文件
     */
    fun enqueueApks(
        apkFiles: Array<File>,
        huaweiMDMManager: HuaweiMDMManager,
        mAdminName: ComponentName
    ) {
        apkQueue.addAll(apkFiles)
        startNextInstall(huaweiMDMManager, mAdminName)
    }

    private fun startNextInstall(
        devicePackageManager: HuaweiMDMManager,
        mAdminName: ComponentName
    ) {
        if (apkQueue.isNotEmpty()) {
            val apkFile = apkQueue.removeAt(0)
            var uri: Uri? = null
            // 如果系统版本是 Android Nougat（API 24）及以上
            uri = FileProvider.getUriForFile(
                App.instance,
                "com.siyu.mdm.custom.device.fileProvider",
                apkFile);
            LogUtils.i("1", uri.toString())
            callback?.onInstallStarted(apkFile)
            try {
                // 尝试静默安装
                devicePackageManager.installPackage( uri.toString())
                callback?.onInstallSuccess(apkFile)
            } catch (e: Exception) {
                e.printStackTrace()
                LogUtils.e(e.localizedMessage)
                callback?.onInstallFailed(apkFile, e)
            }

            startNextInstall(devicePackageManager, mAdminName)
        } else if (uninstallQueue.isEmpty()) {
            // 如果安装和卸载都完成
            callback?.onAllCompleted()
        }
    }

    /**
     * 批量卸载应用
     */
    fun enqueueUninstalls(
        packageNames: List<String>,
        huaweiMDMManager: HuaweiMDMManager,
        mAdminName: ComponentName
    ) {
        uninstallQueue.addAll(packageNames)
        startNextUninstall(huaweiMDMManager, mAdminName)
    }

    private fun startNextUninstall(
        huaweiMDMManager: HuaweiMDMManager,
        mAdminName: ComponentName
    ) {
        if (uninstallQueue.isNotEmpty()) {
            val packageName = uninstallQueue.removeAt(0)
            callback?.onUninstallStarted(packageName)

            try {
                // 尝试静默卸载
                huaweiMDMManager.uninstallPackage( packageName )
                callback?.onUninstallSuccess(packageName)
            } catch (e: Exception) {
                e.printStackTrace()
                // 静默卸载失败，回退到标准卸载方式
                uninstallAppStandard(packageName)
            }

            startNextUninstall(huaweiMDMManager, mAdminName)
        } else if (apkQueue.isEmpty()) {
            // 如果安装和卸载都完成
            callback?.onAllCompleted()
        }
    }

    private fun uninstallAppStandard(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = "package:$packageName".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

}