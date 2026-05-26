package com.chenyi.agent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chenyi.agent.ui.theme.*

/**
 * 关于对话框
 * 
 * 显示：
 * - 应用信息
 * - 开源许可
 * - 版本信息
 * - 官网链接
 */
@Composable
fun AboutDialog(
    appVersion: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BgSecondary,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 应用名称
                Text(
                    text = "晨翼Agent",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = Brush.linearGradient(GradientPrimary)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 版本信息
                Text(
                    text = "版本 $appVersion",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Build ${appVersion.replace(".", "").take(5)}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 分隔线
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(1.dp)
                        .background(Brush.linearGradient(GradientPrimary))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 应用描述
                Text(
                    text = "基于 Rust 内核的智能 Agent",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "支持截图、OCR、自动化操作",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 官网链接
                val linkText = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = AccentCyan,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("官网: xintiandi.online")
                    }
                }

                ClickableText(
                    text = linkText,
                    style = LocalTextStyle.current.copy(
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    ),
                    onClick = { offset ->
                        // 这里可以添加打开链接的逻辑
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                val downloadLink = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = AccentCyan,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append("下载: oneapi.xintiandi.online/chenyi-agent/")
                    }
                }

                ClickableText(
                    text = downloadLink,
                    style = LocalTextStyle.current.copy(
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    ),
                    onClick = { offset ->
                        // 这里可以添加打开链接的逻辑
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 开源许可标题
                Text(
                    text = "开源许可",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 开源库列表
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OpenSourceItem("Rust Kernel", "Apache 2.0")
                    OpenSourceItem("Kotlin", "Apache 2.0")
                    OpenSourceItem("Jetpack Compose", "Apache 2.0")
                    OpenSourceItem("Material Design 3", "Apache 2.0")
                    OpenSourceItem("RapidOCR", "Apache 2.0")
                    OpenSourceItem("DataStore", "Apache 2.0")
                    OpenSourceItem("Coil", "Apache 2.0")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 版权信息
                Text(
                    text = "© 2026 晨翼Agent Team",
                    fontSize = 10.sp,
                    color = TextMuted.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("关闭", color = BgPrimary)
                }
            }
        }
    }
}

@Composable
private fun OpenSourceItem(
    name: String,
    license: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            color = TextMuted
        )
        Text(
            text = license,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = AccentCyan.copy(alpha = 0.7f)
        )
    }
}