package com.siyu.mdm.custom.device.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siyu.mdm.custom.device.util.DongleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 状态管理ViewModel
class DongleViewModel : ViewModel() {
    private val _dongleStatus = MutableLiveData<String>("未连接设备")
    val dongleStatus: LiveData<String> = _dongleStatus

    private val _currentSn = MutableLiveData<String?>()
    val currentSn: LiveData<String?> = _currentSn

    private val _isWhitelisted = MutableLiveData<Boolean>(false)
    val isWhitelisted: LiveData<Boolean> = _isWhitelisted

    private val _isBound = MutableLiveData<Boolean>(false)
    val isBound: LiveData<Boolean> = _isBound

    private val _whitelist = MutableLiveData<List<String>>(emptyList())
    val whitelist: LiveData<List<String>> = _whitelist

    private lateinit var dongleManager: DongleManager

    fun initManager(context: Context) {
        dongleManager = DongleManager.getInstance(context)
        dongleManager.initListener(object : DongleManager.OnDongleStatusChangeListener {
            override fun onDongleAttached(device: UsbDevice, sn: String, isWhitelisted: Boolean, isBound: Boolean) {
                _dongleStatus.postValue("已连接: ${device.deviceName}")
                _currentSn.postValue(sn)
                _isWhitelisted.postValue(isWhitelisted)
                _isBound.postValue(isBound)
                loadWhitelist()
            }

            override fun onDongleDetached(device: UsbDevice, sn: String?) {
                _dongleStatus.postValue("设备已断开")
                _currentSn.postValue(null)
                _isWhitelisted.postValue(false)
                _isBound.postValue(false)
            }

            override fun onDonglePermissionDenied(device: UsbDevice) {
                _dongleStatus.postValue("需要USB权限")
            }

            override fun onDongleInvalid(device: UsbDevice) {
                _dongleStatus.postValue("无效的Dongle设备")
            }
        })
        loadWhitelist()
    }

    fun loadWhitelist() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dongleManager.getWhitelist().toList()
            _whitelist.postValue(list)
        }
    }

    fun addToWhitelist(sn: String?) {
        sn ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dongleManager.addToWhitelist(sn)
            _isWhitelisted.postValue(true)
            loadWhitelist()
        }
    }

    fun removeFromWhitelist(sn: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dongleManager.removeFromWhitelist(sn)
            if (sn == currentSn.value) {
                _isWhitelisted.postValue(false)
            }
            loadWhitelist()
        }
    }

    fun bindCurrentDevice() {
        val sn = currentSn.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            dongleManager.bindDongle(sn)
            _isBound.postValue(true)
        }
    }

    fun unbindDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            dongleManager.unbindDongle()
            _isBound.postValue(false)
        }
    }

    fun requestUsbPermission(context: Context, device: UsbDevice) {
        viewModelScope.launch(Dispatchers.Main) {
            // 权限请求已在DongleManager内部处理
        }
    }
}

// 主Activity
class USBBindActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DongleManager已经有自己的广播接收器处理权限请求

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DongleManagementScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // DongleManager内部会管理自己的广播接收器
    }
}

// 主界面Composable
@Composable
fun DongleManagementScreen(
    viewModel: DongleViewModel = DongleViewModel()
) {
    val context = LocalContext.current
    val dongleStatus by viewModel.dongleStatus.observeAsState("初始化中...")
    val currentSn by viewModel.currentSn.observeAsState(null)
    val isWhitelisted by viewModel.isWhitelisted.observeAsState(false)
    val isBound by viewModel.isBound.observeAsState(false)
    val whitelist by viewModel.whitelist.observeAsState(emptyList())
    var isLoading by remember { mutableStateOf(true) }

    // 初始化管理器
    LaunchedEffect(Unit) {
        viewModel.initManager(context)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题区域
        Text(
            text = "Dongle 设备管理",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 设备状态卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(
                2.dp,
                if (currentSn != null) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "设备状态",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = dongleStatus,
                    fontSize = 16.sp,
                    color = if (currentSn != null) Color(0xFF4CAF50) else Color(0xFFF44336)
                )

                currentSn?.let { sn ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "设备SN: $sn")

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isWhitelisted) "已在白名单" else "未在白名单",
                            color = if (isWhitelisted) Color(0xFF4CAF50) else Color(0xFFFFC107)
                        )
                        Text(
                            text = if (isBound) "已绑定" else "未绑定",
                            color = if (isBound) Color(0xFF2196F3) else Color(0xFFFFC107)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isWhitelisted) {
                            Button(
                                onClick = { viewModel.addToWhitelist(sn) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "添加到白名单")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("添加到白名单")
                            }
                        }

                        if (isWhitelisted && !isBound) {
                            Button(
                                onClick = { viewModel.bindCurrentDevice() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                            ) {
                              //  Icon(Icons.Default.Link, contentDescription = "绑定设备")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("绑定设备")
                            }
                        } else if (isBound) {
                            Button(
                                onClick = { viewModel.unbindDevice() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                            ) {
                              //  Icon(Icons.Default.Link, contentDescription = "解除绑定")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("解除绑定")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 白名单管理区域
        Text(
            text = "白名单设备",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (whitelist.isEmpty()) {
            Text(
                text = "白名单为空",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(whitelist) { sn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = sn)
                            Button(
                                onClick = { viewModel.removeFromWhitelist(sn) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "移除", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 刷新按钮
        Button(
            onClick = { viewModel.loadWhitelist() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "刷新")
            Spacer(modifier = Modifier.width(8.dp))
            Text("刷新设备列表")
        }
    }
}
