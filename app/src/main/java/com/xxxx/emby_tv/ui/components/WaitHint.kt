package com.xxxx.emby_tv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

/**
 * 页面等待态（父亲 2026-10-13 定的口径，与 VR 一致）。
 *
 * 三态：数据没到写「正在加载…」；报错过、或 8 秒还没回来写「请检查网络连接」；
 * 之后每 5 秒自动重试一次 —— **不加重试按钮**（父亲口径：换页/重新点海报就是重试）。
 *
 * 数据到了（[ready] 为 true）就什么都不画，由页面自己渲染内容。
 */
@Composable
fun WaitHint(
    ready: Boolean,
    failed: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ready) return
    var timedOut by remember { mutableStateOf(false) }

    // 心跳：报过错，或 8 秒还没回来，都算失败
    LaunchedEffect(failed) {
        timedOut = failed
        if (failed) return@LaunchedEffect
        delay(8_000)
        timedOut = true
    }

    // 失败后每 5 秒自动重试一次
    LaunchedEffect(timedOut) {
        if (!timedOut) return@LaunchedEffect
        while (true) {
            delay(5_000)
            onRetry()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (timedOut) "请检查网络连接" else "正在加载…",
            color = if (timedOut) Color.White.copy(alpha = 0.86f) else Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 60.dp)
        )
    }
}
