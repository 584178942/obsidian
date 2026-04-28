package com.siyu.mdm.custom.device

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.huawei.android.app.admin.DeviceRestrictionManager
import com.huawei.hem.license.HemLicenseManager
import com.siyu.mdm.custom.device.service.MqttService
import com.siyu.mdm.custom.device.util.AppConstants.FIRST_STATE
import com.siyu.mdm.custom.device.SampleDeviceReceiver
import com.siyu.mdm.custom.device.util.HuaweiMDMManager
import com.siyu.mdm.custom.device.util.InitiateUtils
import com.siyu.mdm.custom.device.util.StorageUtil
import com.siyu.mdm.custom.device.ui.USBBindActivity
import com.siyu.mdm.custom.device.util.screenoffalert.AlertService


class MainActivity : ComponentActivity() {
    val TAG = "MainActivity"
    var hemInstance: HemLicenseManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hemInstance = HemLicenseManager.getInstance(this);
        hemInstance?.setStatusListener { errorCode, msg ->
            LogUtils.i(TAG, "errorCode=$errorCode,msg=$msg");
            if (errorCode == 2000) {
                LogUtils.i(TAG, "成功");
                InitiateUtils().initApp()

                //初始化策略
                huaweiMDMManager.setWifiApDisabled(true)
                huaweiMDMManager.setBluetoothDisabled(true)
                huaweiMDMManager.setScreenCaptureDisabled(true)
            } else {
                LogUtils.i(TAG, "失败");
            }
        }
        
        setContent {
            // 启动MqttService
            val intent = Intent(this, MqttService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent)
            } else {
               startService(intent)
            }
            
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val restriction = DeviceRestrictionManager();
                    val mAdminName = ComponentName(this, SampleDeviceReceiver::class.java)
                    var showMdmInfo by remember { mutableStateOf(StorageUtil.getBoolean(FIRST_STATE, true)) }
                    
                    // MDM告知页面对话框
                    if (showMdmInfo) {
                        Dialog(onDismissRequest = { showMdmInfo = false }) {
                            Surface(
                                modifier = Modifier.fillMaxSize(0.9f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                MdmInformationDialog {
                                    showMdmInfo = false
                                    StorageUtil.putBoolean(FIRST_STATE, false)
                                    hemInstance?.activeLicense();
                                    finish()
                                }
                            }
                        }
                    }
                     
                    Greeting(restriction, mAdminName, this)
                }
            }
        }
    }
}

/**
 * MDM设备管理告知对话框内容
 */
@Composable
fun MdmInformationDialog(onConfirm: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("此设备由您的组织管理", 
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("我已了解并同意")
        }
    }
}

@Composable
fun Greeting(
    restriction: DeviceRestrictionManager,
    mAdminName: ComponentName,
    activity: MainActivity
) {
    val context: Context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("设备管理主界面", style = MaterialTheme.typography.headlineSmall, 
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp))
        
        // 管理功能入口区域
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // USB管理入口
            Button(
                onClick = {
                    val intent = Intent(context, com.siyu.mdm.custom.device.ui.USBBindActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("USB绑定管理")
            }
            
            // 通知管理入口
            Button(
                onClick = {
                    // 启动通知测试
                    val intent = Intent(context, AlertService::class.java)
                    intent.putExtra("source", "manual")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("通知管理")
            }
            
            // MDM功能管理入口
            Button(
                onClick = {
                    val intent = Intent(context, MdmManagementActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("MDM功能管理")
            }
        }
    }
}

// MDM功能管理页面
class MdmManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MdmManagementScreen()
                }
            }
        }
    }
}

@Composable
fun MdmManagementScreen() {
    val context: Context = LocalContext.current
    
    // 状态变量 - 使用默认初始值
    var wifiApEnabled by remember { mutableStateOf<Boolean>(false) }
    var bluetoothEnabled by remember { mutableStateOf<Boolean>(false) }
    var screenCaptureEnabled by remember { mutableStateOf<Boolean>(false) }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("MDM功能管理", style = MaterialTheme.typography.headlineSmall, 
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp))
        
        // MDM功能控制区域
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Wi-Fi 热点控制
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Wi-Fi热点")
                Button(
                    onClick = {
                        wifiApEnabled = !wifiApEnabled
                        huaweiMDMManager.setWifiApDisabled(!wifiApEnabled)
                        LogUtils.d("MDM", "Wi-Fi热点 ${if (wifiApEnabled) "已启用" else "已禁用"}")
                    }
                ) {
                    Text(if (wifiApEnabled) "禁用" else "启用")
                }
            }
            
            // 蓝牙控制
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("蓝牙")
                Button(
                    onClick = {
                        bluetoothEnabled = !bluetoothEnabled
                        huaweiMDMManager.setBluetoothDisabled(!bluetoothEnabled)
                        LogUtils.d("MDM", "蓝牙 ${if (bluetoothEnabled) "已启用" else "已禁用"}")
                    }
                ) {
                    Text(if (bluetoothEnabled) "禁用" else "启用")
                }
            }
            
            // 屏幕截图控制
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("屏幕截图")
                Button(
                    onClick = {
                        screenCaptureEnabled = !screenCaptureEnabled
                        huaweiMDMManager.setScreenCaptureDisabled(!screenCaptureEnabled)
                        LogUtils.d("MDM", "屏幕截图 ${if (screenCaptureEnabled) "已启用" else "已禁用"}")
                    }
                ) {
                    Text(if (screenCaptureEnabled) "禁用" else "启用")
                }
            }
            
            // 震动提醒测试按钮
            Button(
                onClick = {
                    val intent = Intent(context, AlertService::class.java)
                    intent.putExtra("source", "mdm_management")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("测试震动提醒")
            }
        }
    }
}

val huaweiMDMManager = HuaweiMDMManager()
