package com.siyu.mdm.enterprise.ui

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
import com.siyu.mdm.enterprise.util.PermissionHelper
import com.siyu.mdm.enterprise.util.mdm.MDMManagerFactory
import com.siyu.mdm.enterprise.util.mdm.NetworkManager
import com.siyu.mdm.enterprise.ui.theme.MDMEnterpriseTheme

/**
 * 权限演示Activity
 * 展示如何正确请求和使用各种权限
 */
class PermissionDemoActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PermissionDemoActivity"
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 1001
        private const val REQUEST_NFC_PERMISSION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MDMEnterpriseTheme {
                PermissionDemoScreen(
                    onRequestBluetoothPermissions = { requestBluetoothPermissions() },
                    onRequestWriteSettingsPermission = { requestWriteSettingsPermission() },
                    onTestBluetoothToggle = { disabled -> testBluetoothToggle(disabled) },
                    onTestNFCToggle = { disabled -> testNFCToggle(disabled) },
                    onOpenSettings = { type -> openSettings(type) }
                )
            }
        }
    }

    private fun requestBluetoothPermissions() {
        LogUtils.i(TAG, "请求蓝牙权限")
        
        PermissionHelper.requestRuntimePermissions(
            activity = this,
            permissions = PermissionHelper.RuntimePermissions.BLUETOOTH_PERMISSIONS,
            requestCode = REQUEST_BLUETOOTH_PERMISSIONS
        )
    }

    private fun requestWriteSettingsPermission() {
        LogUtils.i(TAG, "请求WRITE_SECURE_SETTINGS权限")
        
        PermissionHelper.requestWriteSecureSettingsPermission(this) { granted ->
            if (granted) {
                LogUtils.i(TAG, "WRITE_SECURE_SETTINGS权限已授予")
            } else {
                LogUtils.w(TAG, "WRITE_SECURE_SETTINGS权限请求已发起，等待用户授权")
            }
        }
    }

    private fun testBluetoothToggle(disabled: Boolean) {
        LogUtils.i(TAG, "测试蓝牙控制: disabled=$disabled")
        
        try {
            val networkManager = MDMManagerFactory.getNetworkManager()
            val success = networkManager.setBluetoothDisabled(disabled)
            
            LogUtils.i(TAG, "蓝牙控制结果: ${if (success) "成功" else "失败"}")
        } catch (e: UnsupportedOperationException) {
            LogUtils.e(TAG, "当前设备不支持蓝牙控制: ${e.message}")
        } catch (e: Exception) {
            LogUtils.e(TAG, "蓝牙控制异常: ${e.message}", e)
        }
    }

    private fun testNFCToggle(disabled: Boolean) {
        LogUtils.i(TAG, "测试NFC控制: disabled=$disabled")
        
        try {
            val networkManager = MDMManagerFactory.getNetworkManager()
            val success = networkManager.setNFCDisabled(disabled)
            
            LogUtils.i(TAG, "NFC控制结果: ${if (success) "成功" else "失败"}")
        } catch (e: UnsupportedOperationException) {
            LogUtils.e(TAG, "当前设备不支持NFC控制: ${e.message}")
        } catch (e: Exception) {
            LogUtils.e(TAG, "NFC控制异常: ${e.message}", e)
        }
    }

    private fun openSettings(type: String) {
        when (type) {
            "bluetooth" -> PermissionHelper.openBluetoothSettings(this)
            "nfc" -> PermissionHelper.openNFCSettings(this)
            "wifi" -> PermissionHelper.openWifiSettings(this)
            "app" -> PermissionHelper.openAppSettings(this)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                    LogUtils.i(TAG, "蓝牙权限已授予")
                } else {
                    LogUtils.w(TAG, "蓝牙权限被拒绝")
                }
            }
            REQUEST_NFC_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    LogUtils.i(TAG, "NFC权限已授予")
                } else {
                    LogUtils.w(TAG, "NFC权限被拒绝")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDemoScreen(
    onRequestBluetoothPermissions: () -> Unit,
    onRequestWriteSettingsPermission: () -> Unit,
    onTestBluetoothToggle: (Boolean) -> Unit,
    onTestNFCToggle: (Boolean) -> Unit,
    onOpenSettings: (String) -> Unit
) {
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var nfcEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("权限演示") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 权限状态卡片
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "权限状态",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("蓝牙: ${if (bluetoothEnabled) "已启用" else "未启用"}")
                    Text("NFC: ${if (nfcEnabled) "已启用" else "未启用"}")
                }
            }

            // 权限请求按钮
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "权限请求",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onRequestBluetoothPermissions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("请求蓝牙权限")
                    }
                    
                    Button(
                        onClick = onRequestWriteSettingsPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("请求WRITE_SECURE_SETTINGS权限")
                    }
                }
            }

            // 功能测试卡片
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "功能测试",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTestBluetoothToggle(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("禁用蓝牙")
                        }
                        
                        Button(
                            onClick = { onTestBluetoothToggle(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("启用蓝牙")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTestNFCToggle(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("禁用NFC")
                        }
                        
                        Button(
                            onClick = { onTestNFCToggle(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("启用NFC")
                        }
                    }
                }
            }

            // 快速设置入口
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "快速设置入口",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenSettings("bluetooth") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("蓝牙")
                        }
                        
                        OutlinedButton(
                            onClick = { onOpenSettings("nfc") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("NFC")
                        }
                        
                        OutlinedButton(
                            onClick = { onOpenSettings("wifi") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("WiFi")
                        }
                    }
                }
            }

            // 说明文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "权限说明",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• 蓝牙控制: 需要 BLUETOOTH_CONNECT + BLUETOOTH_SCAN 权限",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• NFC控制: 需要 WRITE_SECURE_SETTINGS 系统权限",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• 这些权限普通应用无法获得，仅限设备管理员或系统应用",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
