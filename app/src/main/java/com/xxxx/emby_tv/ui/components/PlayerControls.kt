package com.xxxx.emby_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.xxxx.emby_tv.Utils.formatDuration
import com.xxxx.emby_tv.data.model.MediaStreamDto
import com.xxxx.emby_tv.data.model.MediaSourceInfoDto
import kotlinx.coroutines.delay

/** 播放控制面板上的一级菜单项 */
enum class PlayerMenuItem(val label: String) {
    INFO("信息"),
    EPISODES("选集"),
    CAST("演职人员"),
    SUBTITLE("字幕"),
    DANMAKU("弹幕"),
    SPEED("播放速度"),
    INTRO("跳过片头"),
    AUDIO("声音"),
    MORE("更多"),
    QUALITY("视频质量"),
    PLAY_MODE("播放模式"),
    CORRECTION("播放校正"),
    BUFFER("缓冲设置"),
    BACK("返回");

    /** 二级菜单标题(与一级文字一致,除"返回") */
    val sheetTitle: String get() = label
}

/** 主菜单(常用放外面);选集/演职人员由调用方按内容有无过滤 */
val PLAYER_MAIN_MENU = listOf(
    PlayerMenuItem.INFO,
    PlayerMenuItem.EPISODES,
    PlayerMenuItem.CAST,
    PlayerMenuItem.SUBTITLE,
    PlayerMenuItem.DANMAKU,
    PlayerMenuItem.SPEED,
    PlayerMenuItem.INTRO,
    PlayerMenuItem.AUDIO,
    PlayerMenuItem.MORE
)

/** 「更多」页 */
val PLAYER_MORE_MENU = listOf(
    PlayerMenuItem.QUALITY,
    PlayerMenuItem.PLAY_MODE,
    PlayerMenuItem.CORRECTION,
    PlayerMenuItem.BUFFER,
    PlayerMenuItem.BACK
)

/** 白字黑描边:底部区域没有底衬,靠阴影保证亮画面上可读 */
val OverlayTextStyle = TextStyle(
    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 6f)
)

val OverlayTextStyleSoft = TextStyle(
    shadow = Shadow(color = Color.Black, offset = Offset(1.5f, 1.5f), blurRadius = 5f)
)

/**
 * 底部播放控制区:标题行 + 进度 + 播放三键 + 一级菜单文字行;
 * 二级菜单从一级菜单上方浮出(半透明框)。整块没有底色。
 */
@Composable
fun PlayerControlPanel(
    modifier: Modifier = Modifier,
    title: String,
    playMethodLabel: String,
    position: Long,
    duration: Long,
    buffered: Long,
    isPlaying: Boolean,
    menuItems: List<PlayerMenuItem>,
    activeItem: PlayerMenuItem?,
    onMenuSelect: (PlayerMenuItem) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val playFocus = remember { FocusRequester() }
    val itemFocus = remember { List(PLAYER_MAIN_MENU.size) { FocusRequester() } }

    // 面板出现时焦点落在播放暂停
    LaunchedEffect(Unit) {
        delay(60)
        runCatching { playFocus.requestFocus() }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp)
                .padding(bottom = 26.dp)
        ) {
            // 标题:片名(大) + (播放方式)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    style = OverlayTextStyle
                )
                if (playMethodLabel.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "（$playMethodLabel）",
                        color = Color(0xFFE0E0E0),
                        fontSize = 22.sp,
                        style = OverlayTextStyle
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(position),
                    color = Color.White,
                    fontSize = 15.sp,
                    style = OverlayTextStyleSoft
                )
                Text(
                    text = "-" + formatDuration((duration - position).coerceAtLeast(0L)) +
                            " / " + formatDuration(duration),
                    color = Color(0xFFD0D0D0),
                    fontSize = 15.sp,
                    style = OverlayTextStyleSoft
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.30f))
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

            Spacer(modifier = Modifier.height(14.dp))

            // 播放三键
            Row(verticalAlignment = Alignment.CenterVertically) {
                PanelIcon(
                    icon = Icons.Default.Replay10,
                    description = "后退10秒",
                    focusRequester = itemFocus[0],
                    onClick = onSeekBack
                )
                Spacer(modifier = Modifier.width(10.dp))
                PanelIcon(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = "播放暂停",
                    focusRequester = playFocus,
                    onClick = onPlayPause
                )
                Spacer(modifier = Modifier.width(10.dp))
                PanelIcon(
                    icon = Icons.Default.Forward10,
                    description = "前进10秒",
                    focusRequester = itemFocus[1],
                    onClick = onSeekForward
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 一级菜单(纯文字)
            Row(verticalAlignment = Alignment.CenterVertically) {
                menuItems.forEachIndexed { index, item ->
                    val selected = activeItem == item
                    Surface(
                        onClick = { onMenuSelect(item) },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = if (selected) Color.White else Color(0xFFD8D8D8),
                            focusedContainerColor = Color.White.copy(alpha = 0.26f),
                            focusedContentColor = Color.White
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .focusRequester(itemFocus[index.coerceAtMost(itemFocus.lastIndex)])
                    ) {
                        Text(
                            text = item.label,
                            fontSize = 19.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            style = OverlayTextStyle,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun PanelIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.26f),
            focusedContentColor = Color.White
        ),
        modifier = Modifier.focusRequester(focusRequester)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier
                .padding(6.dp)
                .size(30.dp)
        )
    }
}

/** 二级菜单外壳:半透明深色框(这里允许半透明) */
@Composable
fun SheetShell(
    modifier: Modifier = Modifier,
    title: String,
    contentWidth: Dp = 420.dp,
    contentHeight: Dp? = null,
    scrollable: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .background(Color(0xD9101010), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .focusGroup()
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        val boxModifier = if (contentHeight != null) {
            Modifier.width(contentWidth).height(contentHeight)
        } else {
            Modifier.width(contentWidth).heightIn(max = 420.dp)
        }
        if (scrollable) {
            Column(modifier = boxModifier.verticalScroll(rememberScrollState())) {
                content()
            }
        } else {
            Column(modifier = boxModifier) {
                content()
            }
        }
    }
}

/** 二级菜单通用行(firstFocus 传非空时,该行会请求焦点) */
@Composable
fun SheetRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    firstFocus: FocusRequester? = null,
    trailing: String? = null,
) {
    LaunchedEffect(firstFocus) {
        if (firstFocus != null) {
            delay(60)
            runCatching { firstFocus.requestFocus() }
        }
    }
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.30f),
            focusedContentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(
                if (firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 11.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 17.sp)
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else if (!trailing.isNullOrEmpty()) {
                Text(text = trailing, fontSize = 15.sp, color = Color(0xFFBDBDBD))
            }
        }
    }
}

// ---------------- 各二级菜单内容 ----------------

/** 信息:剧情简介 + 一行技术小字 */
@Composable
fun InfoSheet(
    overview: String?,
    techLine: String,
    firstFocus: FocusRequester,
) {
    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.22f),
            focusedContentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(firstFocus)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = overview?.takeIf { it.isNotBlank() } ?: "暂无剧情简介",
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            if (techLine.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = techLine, color = Color(0xFFAFAFAF), fontSize = 13.sp)
            }
        }
    }
}

/** 选集:文字列表(遥控器上比缩略图好按) */
@Composable
fun EpisodeListSheet(
    seriesId: String?,
    currentId: String?,
    repository: com.xxxx.emby_tv.data.repository.EmbyRepository,
    onPlay: (com.xxxx.emby_tv.data.model.BaseItemDto) -> Unit,
    firstFocus: FocusRequester,
) {
    var episodes by remember { mutableStateOf<List<com.xxxx.emby_tv.data.model.BaseItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(seriesId) {
        if (seriesId == null) {
            loading = false
            return@LaunchedEffect
        }
        try {
            episodes = repository.getSeriesList(seriesId)
        } catch (e: Exception) {
            com.xxxx.emby_tv.util.ErrorHandler.logError("PlayerControls", "选集加载失败", e)
            episodes = emptyList()
        }
        loading = false
    }

    Column {
        if (loading) {
            Text(
                text = "加载中…",
                color = Color(0xFFAFAFAF),
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
            return@Column
        }
        if (episodes.isEmpty()) {
            Text(
                text = "没有其它剧集",
                color = Color(0xFFAFAFAF),
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
            return@Column
        }
        episodes.forEachIndexed { i, ep ->
            val season = ep.parentIndexNumber
            val number = ep.indexNumber
            val prefix = when {
                season != null && number != null -> "S${season}:E${number} "
                number != null -> "第 $number 集 "
                else -> ""
            }
            SheetRow(
                label = prefix + (ep.name ?: ""),
                selected = ep.id == currentId,
                onClick = { if (ep.id != currentId) onPlay(ep) },
                firstFocus = if (i == 0) firstFocus else null
            )
        }
    }
}

/** 演职人员:文字列表 */
@Composable
fun CastListSheet(
    people: List<com.xxxx.emby_tv.data.model.PersonInfo>,
    firstFocus: FocusRequester,
) {
    Column {
        if (people.isEmpty()) {
            Text(
                text = "暂无演职人员信息",
                color = Color(0xFFAFAFAF),
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
            return@Column
        }
        people.forEachIndexed { i, person ->
            val role = person.role?.takeIf { it.isNotBlank() }
            SheetRow(
                label = person.name ?: "",
                selected = false,
                onClick = {},
                firstFocus = if (i == 0) firstFocus else null,
                trailing = role
            )
        }
    }
}

/** 缓冲设置:几档预设 + 重置(细节参数不进遥控器菜单) */
@Composable
fun BufferPresetSheet(
    currentMinBufferMs: Int,
    onApplyPreset: (Int) -> Unit,
    onResetDefaults: () -> Unit,
    firstFocus: FocusRequester,
) {
    val presets = listOf(
        20_000 to "小（20 秒·省内存）",
        40_000 to "标准（40 秒）",
        80_000 to "大（80 秒·网络差时稳）"
    )
    Column {
        presets.forEachIndexed { i, (value, label) ->
            SheetRow(
                label = label,
                selected = currentMinBufferMs == value,
                onClick = { onApplyPreset(value) },
                firstFocus = if (i == 0) firstFocus else null
            )
        }
        SheetRow(
            label = "恢复默认缓冲参数",
            selected = false,
            onClick = onResetDefaults
        )
    }
}

/** 字幕 */
@Composable
fun SubtitleSheet(
    tracks: List<MediaStreamDto>,
    selectedIndex: Int,
    timeOffsetMs: Long,
    onSelect: (Int) -> Unit,
    onTimeOffsetChange: (Long) -> Unit,
    firstFocus: FocusRequester,
) {
    Column {
        SheetRow(
            label = "关闭字幕",
            selected = selectedIndex == -1,
            onClick = { onSelect(-1) },
            firstFocus = firstFocus
        )
        tracks.forEach { track ->
            val index = track.index ?: -1
            SheetRow(
                label = track.displayTitle ?: track.language ?: "未知轨道",
                selected = selectedIndex == index,
                onClick = { onSelect(index) }
            )
        }
        SheetRow(
            label = "字幕提前 0.5 秒",
            selected = false,
            onClick = { onTimeOffsetChange(timeOffsetMs - 500L) }
        )
        SheetRow(
            label = "字幕延后 0.5 秒",
            selected = false,
            onClick = { onTimeOffsetChange(timeOffsetMs + 500L) }
        )
        SheetRow(
            label = "偏移复位",
            selected = timeOffsetMs == 0L,
            onClick = { onTimeOffsetChange(0L) },
            trailing = String.format("%.1fs", timeOffsetMs / 1000f)
        )
    }
}

/** 声音(音轨) */
@Composable
fun AudioSheet(
    tracks: List<MediaStreamDto>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    firstFocus: FocusRequester,
) {
    Column {
        tracks.forEachIndexed { i, track ->
            val index = track.index ?: -1
            SheetRow(
                label = track.displayTitle ?: track.language ?: "未知轨道",
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                firstFocus = if (i == 0) firstFocus else null
            )
        }
    }
}

/** 弹幕 */
@Composable
fun DanmakuSheet(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    firstFocus: FocusRequester,
) {
    val options = listOf(0.8f to "小", 1.0f to "标准", 1.3f to "大", 1.6f to "特大")
    Column {
        SheetRow(
            label = if (enabled) "显示弹幕:开" else "显示弹幕:关",
            selected = enabled,
            onClick = { onEnabledChange(!enabled) },
            firstFocus = firstFocus
        )
        options.forEach { (value, label) ->
            SheetRow(
                label = "字号:$label",
                selected = kotlin.math.abs(scale - value) < 0.01f,
                onClick = { onScaleChange(value) }
            )
        }
    }
}

/** 播放速度 */
@Composable
fun SpeedSheet(
    current: Float,
    onChange: (Float) -> Unit,
    firstFocus: FocusRequester,
) {
    val options = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    Column {
        options.forEachIndexed { i, value ->
            SheetRow(
                label = if (value == 1.0f) "正常 (1.0x)" else String.format("%.2fx", value),
                selected = kotlin.math.abs(current - value) < 0.01f,
                onClick = { onChange(value) },
                firstFocus = if (i == 0) firstFocus else null
            )
        }
    }
}

/** 视频质量(服务端按码率上限决定是否转码与分辨率) */
@Composable
fun QualitySheet(
    current: Int,
    onChange: (Int) -> Unit,
    firstFocus: FocusRequester,
) {
    val options = listOf(
        QUALITY_ORIGINAL to "原画（不转码）",
        10_000_000 to "1080p（10 兆）",
        5_000_000 to "1080p（5 兆）",
        1_000_000 to "1080p（1 兆·省流）"
    )
    Column {
        options.forEachIndexed { i, (value, label) ->
            SheetRow(
                label = label,
                selected = current == value,
                onClick = { onChange(value) },
                firstFocus = if (i == 0) firstFocus else null
            )
        }
    }
}

const val QUALITY_ORIGINAL = 200_000_000

/** 播放模式 */
@Composable
fun PlayModeSheet(
    current: Int,
    onChange: (Int) -> Unit,
    firstFocus: FocusRequester,
) {
    val options = listOf(0 to "列表循环", 1 to "单集循环", 2 to "播完停止")
    Column {
        options.forEachIndexed { i, (value, label) ->
            SheetRow(
                label = label,
                selected = current == value,
                onClick = { onChange(value) },
                firstFocus = if (i == 0) firstFocus else null
            )
        }
    }
}

/** 播放校正 */
@Composable
fun CorrectionSheet(
    current: Int,
    onChange: (Int) -> Unit,
    firstFocus: FocusRequester,
) {
    Column {
        SheetRow(
            label = "关闭（保持原画质）",
            selected = current == 0,
            onClick = { onChange(0) },
            firstFocus = firstFocus
        )
        SheetRow(
            label = "服务端转码（画面异常时用）",
            selected = current == 1,
            onClick = { onChange(1) }
        )
    }
}

/** 跳过片头 */
@Composable
fun IntroSheet(
    enabled: Boolean,
    hasIntro: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    firstFocus: FocusRequester,
) {
    Column {
        SheetRow(
            label = if (enabled) "自动跳过片头:开" else "自动跳过片头:关",
            selected = enabled,
            onClick = { onEnabledChange(!enabled) },
            firstFocus = firstFocus
        )
        if (!hasIntro) {
            Text(
                text = "本集没有识别到片头区间",
                color = Color(0xFFAFAFAF),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

/** 供播放页使用的当前播放方式文案 */
fun playMethodLabel(session: com.xxxx.emby_tv.data.model.SessionDto?): String {
    val method = session?.playState?.playMethod ?: return ""
    return when (method) {
        "DirectPlay" -> "直接播放"
        "DirectStream" -> "直接串流"
        "Transcode" -> "转码"
        else -> method
    }
}

/** 技术小字(信息页用) */
fun techLineOf(source: MediaSourceInfoDto?, videoLabel: String): String {
    val parts = mutableListOf<String>()
    if (videoLabel.isNotEmpty()) parts.add(videoLabel)
    source?.container?.takeIf { it.isNotBlank() }?.let { parts.add(it.uppercase()) }
    source?.bitrate?.takeIf { it > 0 }?.let { parts.add("${it / 1_000_000} Mbps") }
    return parts.joinToString(" · ")
}
