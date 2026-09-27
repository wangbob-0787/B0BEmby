package com.xxxx.emby_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.xxxx.emby_tv.Utils.formatDuration
import com.xxxx.emby_tv.data.model.MediaStreamDto
import kotlinx.coroutines.delay

/** 播放控制条上可展开的小浮层 */
enum class PlayerSheet { NONE, SUBTITLE, AUDIO, DANMAKU }

/** 底部控制条里的一枚图标按钮 */
@Composable
private fun ControlIcon(
    icon: ImageVector,
    description: String,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.24f),
            focusedContentColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier
                .padding(9.dp)
                .size(26.dp)
        )
    }
}

/** 底部控制条最下面一行的文字入口 */
@Composable
private fun ControlTab(
    text: String,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFFCFCFCF),
            focusedContainerColor = Color.White.copy(alpha = 0.24f),
            focusedContentColor = Color.White
        ),
        modifier = Modifier
            .padding(end = 6.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/**
 * 底部播放控制条:标题行(含字幕/音轨/弹幕/更多) + 时间 + 进度条 + 播放三键 + 文字入口。
 * zone 0 = 图标区, zone 1 = 文字入口区，供上层判断"再按下键是否收起"。
 */
@Composable
fun PlayerControlsBar(
    modifier: Modifier = Modifier,
    seriesTitle: String,
    episodeLabel: String?,
    statusLine: String,
    position: Long,
    duration: Long,
    buffered: Long,
    isPlaying: Boolean,
    onZoneChange: (Int) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onPlayPause: () -> Unit,
    onOpenSheet: (PlayerSheet) -> Unit,
    onOpenSettings: (String) -> Unit,
) {
    val iconFocus = remember { List(7) { FocusRequester() } }
    val tabFocus = remember { List(4) { FocusRequester() } }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { iconFocus[1].requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(horizontal = 44.dp, vertical = 14.dp)
    ) {
        // 标题行
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (!episodeLabel.isNullOrEmpty()) {
                    Text(text = episodeLabel, color = Color(0xFFB0B0B0), fontSize = 13.sp)
                }
                Text(
                    text = seriesTitle,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (statusLine.isNotEmpty()) {
                Text(
                    text = statusLine.replace("\n", " "),
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(18.dp))
            }
            ControlIcon(Icons.Default.ClosedCaption, "字幕", iconFocus[3], { onZoneChange(0) }) {
                onOpenSheet(PlayerSheet.SUBTITLE)
            }
            ControlIcon(Icons.Default.Audiotrack, "音轨", iconFocus[4], { onZoneChange(0) }) {
                onOpenSheet(PlayerSheet.AUDIO)
            }
            ControlIcon(Icons.Default.Chat, "弹幕", iconFocus[5], { onZoneChange(0) }) {
                onOpenSheet(PlayerSheet.DANMAKU)
            }
            ControlIcon(Icons.Default.Settings, "更多", iconFocus[6], { onZoneChange(0) }) {
                onOpenSettings("Info")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 时间
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatDuration(position), color = Color.White, fontSize = 13.sp)
            Text(
                text = "-" + formatDuration((duration - position).coerceAtLeast(0L)) +
                        " / " + formatDuration(duration),
                color = Color(0xFFB0B0B0),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.25f))
        ) {
            if (duration > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(
                            (buffered.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        )
                        .background(Color.White.copy(alpha = 0.45f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(
                            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        )
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 播放三键
        Row(verticalAlignment = Alignment.CenterVertically) {
            ControlIcon(Icons.Default.Replay10, "后退10秒", iconFocus[0], { onZoneChange(0) }) {
                onSeekBack()
            }
            ControlIcon(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                description = "播放暂停",
                focusRequester = iconFocus[1],
                onFocused = { onZoneChange(0) },
                onClick = onPlayPause
            )
            ControlIcon(Icons.Default.Forward10, "前进10秒", iconFocus[2], { onZoneChange(0) }) {
                onSeekForward()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 文字入口
        Row(verticalAlignment = Alignment.CenterVertically) {
            ControlTab("信息", tabFocus[0], { onZoneChange(1) }) { onOpenSettings("Info") }
            ControlTab("选集", tabFocus[1], { onZoneChange(1) }) { onOpenSettings("Episodes") }
            ControlTab("演职人员", tabFocus[2], { onZoneChange(1) }) { onOpenSettings("People") }
            ControlTab("播放设置", tabFocus[3], { onZoneChange(1) }) { onOpenSettings("Buffer") }
        }
    }
}

/** 小浮层外壳:右下角悬浮,不遮整屏 */
@Composable
private fun SheetShell(
    modifier: Modifier = Modifier,
    title: String,
    content: LazyListScope.() -> Unit,
) {
    val groupFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(80)
        runCatching { groupFocus.requestFocus() }
    }
    Column(
        modifier = modifier
            .width(520.dp)
            .heightIn(max = 430.dp)
            .background(Color(0xF2141414), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(text = title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(groupFocus)
                .focusGroup()
                .focusable()
        ) {
            content()
        }
    }
}

/** 浮层里的一行 */
@Composable
private fun SheetRow(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.28f),
            focusedContentColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 15.sp)
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
            }
        }
    }
}

/** 字幕浮层:轨道 + 时间偏移 */
@Composable
fun SubtitleSheet(
    modifier: Modifier = Modifier,
    tracks: List<MediaStreamDto>,
    selectedIndex: Int,
    timeOffsetMs: Long,
    onSelect: (Int) -> Unit,
    onTimeOffsetChange: (Long) -> Unit,
) {
    SheetShell(modifier = modifier, title = "字幕") {
        item {
            val selected = selectedIndex == -1
            SheetRow(label = "关闭字幕", selected = selected) { onSelect(-1) }
        }
        items(tracks) { track ->
            val index = track.index ?: -1
            val title = track.displayTitle ?: track.language ?: "未知轨道"
            SheetRow(label = title, selected = selectedIndex == index) { onSelect(index) }
        }
        item {
            val seconds = timeOffsetMs / 1000f
            SheetRow(
                label = "偏移 " + String.format("%.1f", seconds) + " 秒  +0.5"
            ) { onTimeOffsetChange(timeOffsetMs + 500L) }
        }
        item {
            SheetRow(label = "偏移 " + String.format("%.1f", timeOffsetMs / 1000f) + " 秒  -0.5") {
                onTimeOffsetChange(timeOffsetMs - 500L)
            }
        }
        item {
            SheetRow(label = "偏移复位", selected = timeOffsetMs == 0L) { onTimeOffsetChange(0L) }
        }
    }
}

/** 音轨浮层 */
@Composable
fun AudioSheet(
    modifier: Modifier = Modifier,
    tracks: List<MediaStreamDto>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    SheetShell(modifier = modifier, title = "音轨") {
        items(tracks) { track ->
            val index = track.index ?: -1
            val title = track.displayTitle ?: track.language ?: "未知轨道"
            SheetRow(label = title, selected = selectedIndex == index) { onSelect(index) }
        }
    }
}

/** 弹幕浮层:开关 + 字号 */
@Composable
fun DanmakuSheet(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    scale: Float,
    onScaleChange: (Float) -> Unit,
) {
    val options = listOf(
        0.8f to "小",
        1.0f to "标准",
        1.3f to "大",
        1.6f to "特大"
    )
    SheetShell(modifier = modifier, title = "弹幕") {
        item {
            SheetRow(
                label = if (enabled) "显示弹幕:开" else "显示弹幕:关",
                selected = enabled
            ) { onEnabledChange(!enabled) }
        }
        items(options) { (value, label) ->
            SheetRow(
                label = "字号:$label",
                selected = kotlin.math.abs(scale - value) < 0.01f
            ) { onScaleChange(value) }
        }
    }
}
