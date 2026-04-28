package com.siyu.mdm.enterprise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.ui.theme.MDMEnterpriseTheme
import com.siyu.mdm.enterprise.util.mdm.MDMManagerFactory

/**
 * MDM功能管理页面
 */
class MdmManagementActivity : ComponentActivity() {

    private val TAG = "MdmManagementActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MDMEnterpriseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MdmManagementScreen()
                }
            }
        }
        LogUtils.i(TAG, "当前厂商: ${MDMManagerFactory.currentVendor}")
    }
}

@Composable
fun MdmManagementScreen() {
    var wifiEnabled by remember { mutableStateOf(false) }
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var screenCaptureEnabled by remember { mutableStateOf(false) }

    val securityManager = remember {
        try {
            MDMManagerFactory.getSecurityManager()
        } catch (e: Exception) {
            LogUtils.e("MDM", "获取SecurityManager失败: ${e.message}")
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MDM功能管理",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
        )

        // 显示当前厂商
        Text(
            text = "当前厂商: ${MDMManagerFactory.currentVendor}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // WiFi控制
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wi-Fi")
            Button(
                onClick = {
                    wifiEnabled = !wifiEnabled
                    LogUtils.d("MDM", "Wi-Fi ${if (wifiEnabled) "已启用" else "已禁用"}")
                }
            ) {
                Text(if (wifiEnabled) "禁用" else "启用")
            }
        }

        // 蓝牙控制
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("蓝牙")
            Button(
                onClick = {
                    bluetoothEnabled = !bluetoothEnabled
                    LogUtils.d("MDM", "蓝牙 ${if (bluetoothEnabled) "已启用" else "已禁用"}")
                }
            ) {
                Text(if (bluetoothEnabled) "禁用" else "启用")
            }
        }

        // 截屏控制
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("截屏")
            Button(
                onClick = {
                    screenCaptureEnabled = !screenCaptureEnabled
                    LogUtils.d("MDM", "截屏 ${if (screenCaptureEnabled) "已启用" else "已禁用"}")
                }
            ) {
                Text(if (screenCaptureEnabled) "禁用" else "启用")
            }
        }
    }
}
