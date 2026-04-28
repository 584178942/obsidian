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
import com.siyu.mdm.enterprise.service.MqttService
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.ui.theme.MDMEnterpriseTheme
import com.siyu.mdm.enterprise.ui.USBBindActivity

/**
 * 主Activity
 * 应用启动界面
 */
class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动MQTT服务
        startMqttService()

        setContent {
            MDMEnterpriseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onNavigateToUSB = { navigateToUSB() }
                    )
                }
            }
        }

        LogUtils.i(TAG, "MainActivity创建完成")
    }

    private fun startMqttService() {
        val intent = android.content.Intent(this, MqttService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun navigateToUSB() {
        startActivity(android.content.Intent(this, USBBindActivity::class.java))
    }
}

/**
 * 主界面内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToUSB: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MDM 企业管理") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "设备管理主界面",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNavigateToUSB,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("USB绑定管理")
            }
        }
    }
}
