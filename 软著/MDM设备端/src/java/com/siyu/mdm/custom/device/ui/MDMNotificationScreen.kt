/*
package com.siyu.mdm.custom.device.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

*/
/**
 * MDM设备管理告知页面，向用户说明设备管理情况
 *//*

@Composable
fun MDMNotificationScreen(
    onConfirm: () -> Unit
) {
    Scaffold(
        backgroundColor = MaterialTheme.colors.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(ScrollState(0)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 设备管理图标
            Image(
                painter = painterResource(id = R.drawable.ic_device_management),
                contentDescription = "设备管理图标",
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 32.dp)
            )

            // 标题
            Text(
                text = "设备管理通知",
                style = MaterialTheme.typography.h5.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = MaterialTheme.colors.onSurface
            )

            // 介绍文本
            Text(
                text = "此设备由您的组织管理。组织可能会实施安全政策以保护企业数据。",
                style = MaterialTheme.typography.body1.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            // 管理范围标题
            Text(
                text = "管理范围包括：",
                style = MaterialTheme.typography.h6.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                color = MaterialTheme.colors.onSurface
            )

            // 管理范围列表
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                ManagementItem("设备安全设置（如密码要求）")
                ManagementItem("应用安装和卸载权限")
                ManagementItem("网络连接管理")
                ManagementItem("远程数据擦除（在特定情况下）")
            }

            // 数据收集标题
            Text(
                text = "可能收集的数据：",
                style = MaterialTheme.typography.h6.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                color = MaterialTheme.colors.onSurface
            )

            // 数据收集说明
            Text(
                text = "组织可能会收集设备信息、应用使用情况和位置数据（如果适用），以确保符合安全政策。个人数据的收集和使用将遵循组织的隐私政策。",
                style = MaterialTheme.typography.body1.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
            )

            // 确认按钮
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Text(
                    text = "我已了解并同意",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

*/
/**
 * 管理范围列表项
 *//*

@Composable
private fun ManagementItem(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.body1.copy(
            fontSize = 16.sp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
    )
}
    */
