package com.siyu.mdm.custom.device.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.siyu.mdm.custom.device.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp

class FullScreenActivity : ComponentActivity() {

    // 定义 Intent Extra 键，用于传递文字内容
    companion object {
        const val EXTRA_MAIN_TEXT = "main_text"
        const val EXTRA_SUB_TEXT = "sub_text"
        const val DEFAULT_MAIN_TEXT = "设备已锁定"
        const val DEFAULT_SUB_TEXT = "请联系管理员解锁"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 从 Intent 中获取传递的文字内容，没有则使用默认值
        val mainText = intent.getStringExtra(EXTRA_MAIN_TEXT) ?: DEFAULT_MAIN_TEXT
        val subText = intent.getStringExtra(EXTRA_SUB_TEXT) ?: DEFAULT_SUB_TEXT

        setContent {
            LockScreenContent(
                mainText = mainText,
                subText = subText
            )
        }
    }

    @Preview(showBackground = true, name = "锁定屏幕预览")
    @Composable
    fun LockScreenPreview() {
        LockScreenContent(
            mainText = DEFAULT_MAIN_TEXT,
            subText = DEFAULT_SUB_TEXT
        )
    }

    @Composable
    fun LockScreenContent(
        mainText: String,
        subText: String,
        modifier: Modifier = Modifier
    ) {
        // 可以在这里添加状态管理，支持动态修改文字
        val (currentMainText, setCurrentMainText) = remember { mutableStateOf(mainText) }
        val (currentSubText, setCurrentSubText) = remember { mutableStateOf(subText) }

        // 示例：如果需要从网络或其他地方更新文字，可以在这里使用LaunchedEffect
        LaunchedEffect(Unit) {
            // 模拟延迟更新文字的场景
            // delay(3000)
            // setCurrentMainText("设备已锁定 - 已更新")
        }

        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            // 背景图片
            Image(
                painter = painterResource(id = R.drawable.lock),
                contentDescription = "锁定屏幕背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 文字内容容器
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                // 主标题
                Text(
                    text = currentMainText,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            blurRadius = 8f
                        )
                    ),
                    modifier = Modifier.align(Alignment.Center)
                )

                // 副标题
                Text(
                    text = currentSubText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            blurRadius = 6f
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                )
            }
        }
    }
}
