package com.siyu.mdm.custom.device.util.screenoffalert

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlertControlScreen()
                }
    
    // 处理权限请求结果
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        // 使用PermissionHelper中的扩展函数处理权限结果
        val allGranted = handlePermissionResult(requestCode, permissions, grantResults)
        
        // 如果所有权限都已授予，更新权限状态
        if (allGranted) {
            runOnUiThread {
                // 在UI线程中更新状态
                val currentPermissions = PermissionHelper.checkAllPermissions(this)
                if (currentPermissions) {
                    // 可以在这里添加UI更新逻辑
                }
            }
        }
    }
            }
        }
    }
}

@Composable
fun AlertControlScreen() {
    val context = LocalContext.current
    var isAlertRunning by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }
    var showPermissionDetails by remember { mutableStateOf(false) }
    var currentPermissionStep by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    // 检查并请求必要的权限
    LaunchedEffect(Unit) {
        permissionsGranted = PermissionHelper.checkAllPermissions(context)
    }

    // 请求下一步权限 - 使用lateinit延迟初始化
    val requestNextPermission = remember {
        mutableStateOf<() -> Unit>({ /* 初始占位符 */ })
    }

    // 电池优化权限请求器
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 检查电池优化权限是否已授予
        val newPermissionStatus = PermissionHelper.checkAllPermissions(context)
        if (newPermissionStatus) {
            permissionsGranted = true
            currentPermissionStep = 0
        } else {
            // 权限仍未授予，显示详细引导
            PermissionHelper.showFullPermissionGuide(context)
        }
    }
    
    // 通知权限请求器
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // 通知权限授予后，继续下一步权限请求
            currentPermissionStep++
            requestNextPermission.value.invoke()
        } else {
            PermissionHelper.showErrorDialog(context, "权限被拒绝", "通知权限对于应用正常运行至关重要。请前往设置手动授予权限。")
        }
    }
    
    // 定义权限请求逻辑
    requestNextPermission.value = {
        when (currentPermissionStep) {
            0 -> {
                // 第一步：请求通知权限（Android 13+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // 低于Android 13跳过此步骤
                    currentPermissionStep++
                    requestNextPermission.value.invoke()
                }
            }
            1 -> {
                // 第二步：请求电池优化白名单
                val intent = if (NotificationHelper.isHuaweiDevice()) {
                    // 华为设备特殊处理
                    Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:${context.packageName}")
                    }
                } else {
                    // 其他设备通用处理
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                batteryOptimizationLauncher.launch(intent)
            }
        }
    }





    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "熄屏提醒应用",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 应用介绍
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Text(
                text = "此应用可以在设备屏幕关闭时提供震动提醒功能。为确保功能正常运行，需要您授予必要的权限。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
        }

        if (!permissionsGranted) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { 
                        currentPermissionStep = 0
                        requestNextPermission.value.invoke()
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("开始权限设置")
                }
                
                TextButton(onClick = { showPermissionDetails = !showPermissionDetails }) {
                    Text(if (showPermissionDetails) "收起权限说明" else "查看权限说明")
                }
                
                if (showPermissionDetails) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .wrapContentHeight(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("需要的权限：", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            
                            // 通知权限说明
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Text("• 通知权限：用于创建前台服务通知，确保应用在后台稳定运行",
                                    modifier = Modifier.padding(bottom = 4.dp))
                            }
                            
                            // 震动权限说明
                            Text("• 震动权限：用于在屏幕关闭时触发设备震动",
                                modifier = Modifier.padding(bottom = 4.dp))
                            
                            // 电池优化权限说明
                            Text("• 电池优化豁免：防止系统在屏幕关闭后限制应用后台活动",
                                modifier = Modifier.padding(bottom = 4.dp))
                            
                            // 设备特定说明
                            if (NotificationHelper.isHuaweiDevice()) {
                                Text("\n华为设备特别说明：", fontWeight = FontWeight.Bold, 
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                Text("华为设备需要额外在'手机管家'中设置应用为'允许后台活动'，否则可能无法在熄屏后正常震动。",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        } else {
            // 权限已授予，显示控制按钮
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isAlertRunning) {
                    // 禁用手动启动，只能通过MQTT命令启动
                    Button(
                        onClick = {
                            // 提示用户只能通过MQTT命令启动
                            PermissionHelper.showErrorDialog(context, "功能限制", "震动提醒只能通过MQTT命令的方式打开")
                        },
                        enabled = false, // 禁用按钮
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text("开始提醒（已禁用）")
                    }
                } else {
                    Button(
                        onClick = {
                            val intent = Intent(context, AlertService::class.java)
                            context.stopService(intent)
                            isAlertRunning = false
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text("停止提醒")
                    }
                }

                // 状态说明
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .wrapContentHeight(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isAlertRunning) {
                            "提醒正在运行中...\n\n" +
                            "✅ 前台服务已启动\n" +
                            "✅ 震动功能已激活\n" +
                            "✅ 即使屏幕关闭也会持续提醒"
                        } else {
                            "点击'开始提醒'按钮启动熄屏提醒功能。\n\n" +
                            "💡 提示：\n" +
                            "• 请确保不要使用清理软件关闭此应用\n" +
                            "• 如遇到问题，请重新检查权限设置"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 底部帮助信息
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = { PermissionHelper.showFullPermissionGuide(context) }) {
                Text("使用帮助")
            }
        }
    }
}
