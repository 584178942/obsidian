package com.siyu.mdm.enterprise.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siyu.mdm.enterprise.util.LogUtils
import com.siyu.mdm.enterprise.ui.theme.MDMEnterpriseTheme

/**
 * 全屏锁定Activity
 * 设备被锁定时显示此界面
 */
class FullScreenActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MAIN_TEXT = "main_text"
        const val EXTRA_SUB_TEXT = "sub_text"
        private const val TAG = "FullScreenActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainText = intent.getStringExtra(EXTRA_MAIN_TEXT) ?: "设备已锁定"
        val subText = intent.getStringExtra(EXTRA_SUB_TEXT) ?: "请联系管理员解锁"

        LogUtils.i(TAG, "锁定界面显示: mainText=$mainText, subText=$subText")

        setContent {
            MDMEnterpriseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    LockScreen(mainText, subText)
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // 禁用返回键
        LogUtils.d(TAG, "返回键被禁用")
    }
}

@Composable
fun LockScreen(mainText: String, subText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = mainText,
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = subText,
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
