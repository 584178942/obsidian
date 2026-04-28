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
import com.siyu.mdm.enterprise.ui.theme.MDMEnterpriseTheme

/**
 * USB绑定管理Activity
 */
class USBBindActivity : ComponentActivity() {

    private val TAG = "USBBindActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MDMEnterpriseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    USBBindScreen()
                }
            }
        }
        LogUtils.i(TAG, "USB绑定管理页面创建")
    }
}

@Composable
fun USBBindScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "USB绑定管理",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
        )

        Text(
            text = "USB绑定功能开发中...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
