package com.xxxx.emby_tv.ui.components
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 整页等待：与播放页起播用同一个环（VR 版画法，绿色带箭头），不再出现系统那种白弧。 */
@Composable
fun Loading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        VrLoadingRing(size = 64.dp)
    }
}
