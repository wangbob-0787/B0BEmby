package com.xxxx.emby_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.xxxx.emby_tv.Utils
import com.xxxx.emby_tv.Utils.formatDuration
import com.xxxx.emby_tv.data.model.BaseItemDto
import com.xxxx.emby_tv.data.model.MediaSourceInfoDto
import com.xxxx.emby_tv.data.model.MediaStreamDto
import com.xxxx.emby_tv.data.model.PersonInfo
import com.xxxx.emby_tv.ui.PersonCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    /** 图标行使用的图标 */
    val icon: ImageVector
        get() = when (this) {
            INFO -> Icons.Default.Info
            EPISODES -> Icons.Default.ViewList
            CAST -> Icons.Default.Person
            SUBTITLE -> Icons.Default.ClosedCaption
            DANMAKU -> Icons.Default.Chat
            SPEED -> Icons.Default.Speed
            INTRO -> Icons.Default.SkipNext
            AUDIO -> Icons.Default.VolumeUp
            MORE -> Icons.Default.MoreHoriz
            QUALITY -> Icons.Default.HighQuality
            PLAY_MODE -> Icons.Default.Repeat
            CORRECTION -> Icons.Default.Tune
            BUFFER -> Icons.Default.Memory
            BACK -> Icons.Default.ArrowBack
        }
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

/** 官方主题绿(与详情页焦点色一致) */
val EmbyGreen = Color(0xFF52B54B)

val OverlayTextStyleSoft = TextStyle(
    shadow = Shadow(color = Color.Black, offset = Offset(1.5f, 1.5f), blurRadius = 5f)
)

/**
 * 左右键快进快退时唤出的进度条:只有时间与进度条,不展开控制条、不抢焦点。
 * 用途:播放中未按 ↓、直接按左右键时,给一个"快进/快退到哪了"的可视反馈。
 */
@Composable
fun SeekHud(
    position: Long,
    duration: Long,
    buffered: Long,
    forward: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 58.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formatDuration(position) + " / " + formatDuration(duration),
                color = Color.White,
                fontSize = 20.sp,
                style = OverlayTextStyleSoft
            )
            Text(
                text = if (forward) "快进" else "快退",
                color = Color.White,
                fontSize = 18.sp,
                style = OverlayTextStyleSoft
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 与控制条内同款进度条(白条 + 当前位置圆点)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        ) {
            val barWidth = maxWidth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.CenterStart)
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
            if (duration > 0) {
                val fraction = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (barWidth * fraction) - 6.dp)
                        .size(12.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

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
    val playKeyFocus = remember { List(3) { FocusRequester() } } // 0 后退 / 1 播放暂停 / 2 前进
    val itemFocus = remember { List(PLAYER_MAIN_MENU.size) { FocusRequester() } }
    var lastIconIndex by remember { mutableIntStateOf(0) }
    var sheetWasOpen by remember { mutableStateOf(false) }

    // 面板出现时焦点落在播放暂停
    LaunchedEffect(Unit) {
        delay(60)
        runCatching { playKeyFocus[1].requestFocus() }
    }

    // 二级菜单关掉后,焦点回到刚才点开它的那个图标
    LaunchedEffect(activeItem) {
        if (activeItem != null) {
            sheetWasOpen = true
        } else if (sheetWasOpen) {
            sheetWasOpen = false
            delay(60)
            runCatching {
                itemFocus[lastIconIndex.coerceAtMost(itemFocus.lastIndex)].requestFocus()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 58.dp)
                .padding(bottom = 26.dp)
        ) {
            // 标题:片名(大) + (播放方式)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = OverlayTextStyle,
                    modifier = Modifier.weight(1f, fill = false)
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
                    fontSize = 18.sp,
                    style = OverlayTextStyleSoft
                )
                Text(
                    text = "-" + formatDuration((duration - position).coerceAtLeast(0L)) +
                            " / " + formatDuration(duration),
                    color = Color(0xFFD0D0D0),
                    fontSize = 16.sp,
                    style = OverlayTextStyleSoft
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 进度条(加粗 + 当前位置圆点,3 米外也能看出播到哪)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            ) {
                val barWidth = maxWidth
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .align(Alignment.CenterStart)
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
                if (duration > 0) {
                    val fraction = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (barWidth * fraction) - 6.dp)
                            .size(12.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 播放三键
            Row(verticalAlignment = Alignment.CenterVertically) {
                PanelIcon(
                    icon = Icons.Default.Replay10,
                    description = "后退10秒",
                    focusRequester = playKeyFocus[0],
                    onClick = onSeekBack
                )
                Spacer(modifier = Modifier.width(10.dp))
                PanelIcon(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = "播放暂停",
                    focusRequester = playKeyFocus[1],
                    onClick = onPlayPause,
                    downFocus = itemFocus[0]
                )
                Spacer(modifier = Modifier.width(10.dp))
                PanelIcon(
                    icon = Icons.Default.Forward10,
                    description = "前进10秒",
                    focusRequester = playKeyFocus[2],
                    onClick = onSeekForward
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 一级菜单(图标行;焦点项名称显示在行上方)
            var focusedLabel by remember { mutableStateOf("") }
            if (focusedLabel.isNotEmpty()) {
                Text(
                    text = focusedLabel,
                    color = Color.White,
                    fontSize = 18.sp,
                    style = OverlayTextStyleSoft
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                menuItems.forEachIndexed { index, item ->
                    val selected = activeItem == item
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Surface(
                            onClick = {
                                lastIconIndex = index
                                onMenuSelect(item)
                            },
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = if (selected) EmbyGreen.copy(alpha = 0.45f) else Color.Transparent,
                                contentColor = Color.White,
                                focusedContainerColor = EmbyGreen.copy(alpha = 0.55f),
                                focusedContentColor = Color.White
                            ),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                            modifier = Modifier
                                .focusRequester(itemFocus[index.coerceAtMost(itemFocus.lastIndex)])
                                .onFocusChanged { if (it.isFocused) focusedLabel = item.label }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(9.dp)
                                    .size(26.dp)
                            )
                        }
                        // 当前打开的是哪一项,用下划线标出来(官方:不要只靠颜色传达信息)
                        Box(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .width(24.dp)
                                .height(3.dp)
                                .background(
                                    if (selected) EmbyGreen else Color.Transparent,
                                    RoundedCornerShape(2.dp)
                                )
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
    downFocus: FocusRequester? = null,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.30f),
            focusedContentColor = Color.White
        ),
        modifier = Modifier
            .focusRequester(focusRequester)
            .then(
                if (downFocus != null) {
                    Modifier.focusProperties { down = downFocus }
                } else {
                    Modifier
                }
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f)
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
            .background(Color(0xE66E6E6E), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .focusGroup()
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        val boxModifier = if (contentHeight != null) {
            Modifier.width(contentWidth).height(contentHeight)
        } else {
            Modifier.width(contentWidth).heightIn(max = 240.dp)
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

/** 二级菜单通用行(firstFocus 传非空时,该行会请求焦点;showArrow 显示"还有下一层"的箭头) */
@Composable
fun SheetRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    firstFocus: FocusRequester? = null,
    trailing: String? = null,
    showArrow: Boolean = false,
) {
    LaunchedEffect(firstFocus) {
        if (firstFocus != null) {
            delay(60)
            runCatching { firstFocus.requestFocus() }
        }
    }
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }
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
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged {
                if (it.isFocused) scope.launch { bringIntoView.bringIntoView() }
            }
            .then(
                if (firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 9.dp, vertical = 7.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!selected && !trailing.isNullOrEmpty()) {
                    Text(text = trailing, fontSize = 14.sp, color = Color(0xFFBDBDBD))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (showArrow) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                } else if (selected) {
                    // 选中改「绿色实心方框 + 深色勾」:原来绿勾压中灰底只有 1.7:1,官方要高对比,这样约 6:1
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(EmbyGreen, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF101010),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(
                                2.dp,
                                Color.White.copy(alpha = 0.85f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

// ---------------- 各二级菜单内容 ----------------

/** 信息:海报 + 剧名 + 元数据 + 剧情简介(直接叠在画面上,无底色无边框) */
@Composable
fun InfoSheet(
    mediaInfo: BaseItemDto,
    serverUrl: String,
    techLine: String,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester,
) {
    val posterUrl = remember(mediaInfo.id, mediaInfo.seriesId, serverUrl) {
        if (serverUrl.isEmpty()) {
            ""
        } else {
            val seriesId = mediaInfo.seriesId
            val seriesTag = mediaInfo.seriesPrimaryImageTag
            // 剧集用剧的海报,不要用当前这一集的剧照
            if (!seriesId.isNullOrEmpty() && !seriesTag.isNullOrEmpty()) {
                "$serverUrl/emby/Items/$seriesId/Images/Primary?maxHeight=400&tag=$seriesTag&quality=80"
            } else {
                Utils.getImageUrl(serverUrl, mediaInfo, false)
            }
        }
    }
    val title = mediaInfo.seriesName ?: mediaInfo.name ?: ""
    val metaLine = buildString {
        val season = mediaInfo.parentIndexNumber
        val episode = mediaInfo.indexNumber
        if (season != null && episode != null) append("S$season:E$episode ")
        mediaInfo.name?.takeIf { it.isNotBlank() && it != title }?.let { append("$it ") }
        mediaInfo.productionYear?.let { append("$it ") }
        mediaInfo.runTimeTicks?.let { append("${it / 600_000_000}m ") }
        mediaInfo.officialRating?.takeIf { it.isNotBlank() }?.let { append(it) }
    }.trim()

    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White
        ),
        modifier = modifier
            .background(Color(0xCC6E6E6E), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .focusRequester(firstFocus)
    ) {
        Row {
            if (posterUrl.isNotEmpty()) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(80.dp)
                        .height(120.dp)
                        .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(11.dp))
            }
            Column(modifier = Modifier.width(310.dp)) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = OverlayTextStyle
                )
                if (metaLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = metaLine,
                        color = Color(0xFFE0E0E0),
                        fontSize = 14.sp,
                        style = OverlayTextStyleSoft
                    )
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = mediaInfo.overview?.takeIf { it.isNotBlank() } ?: "暂无剧情简介",
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFFEDEDED),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    style = OverlayTextStyleSoft
                )
                if (techLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = techLine,
                        color = Color(0xFFBDBDBD),
                        fontSize = 13.sp,
                        style = OverlayTextStyleSoft
                    )
                }
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
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
            return@Column
        }
        if (episodes.isEmpty()) {
            Text(
                text = "没有其它剧集",
                color = Color(0xFFAFAFAF),
                fontSize = 16.sp,
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

/** 演职人员:标题 + 横向头像卡片,直接叠在画面上(与信息页同一风格) */
@Composable
fun CastListSheet(
    people: List<PersonInfo>,
    serverUrl: String,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester,
) {
    Column(
        modifier = modifier
            .background(Color(0xCC6E6E6E), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "演职人员",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = OverlayTextStyle
        )
        if (people.isEmpty()) {
            Text(
                text = "暂无演职人员信息",
                color = Color(0xFFBDBDBD),
                fontSize = 14.sp,
                style = OverlayTextStyleSoft
            )
            return@Column
        }
        val rowFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            delay(80)
            runCatching { rowFocus.requestFocus() }
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .focusRequester(rowFocus)
                .focusGroup()
                .focusable(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            items(people, key = { it.id ?: it.hashCode() }) { person ->
                PersonCard(
                    person = person,
                    imgWidth = 75.dp,
                    aspectRatio = 0.66f,
                    serverUrl = serverUrl
                )
            }
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
        if (tracks.isEmpty()) {
            Text(
                text = "暂无可切换音轨",
                color = Color(0xFFBDBDBD),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp)
            )
            return@Column
        }
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

/** 视频质量档位显示名 */
fun qualityLabel(bitrate: Int): String = when (bitrate) {
    QUALITY_ORIGINAL -> "原画"
    10_000_000 -> "1080p 10 兆"
    5_000_000 -> "1080p 5 兆"
    else -> "1080p 1 兆"
}

/** 播放模式显示名 */
fun playModeName(mode: Int): String = when (mode) {
    0 -> "列表循环"
    1 -> "单集循环"
    else -> "播完停止"
}

/** 播放校正显示名 */
fun correctionName(value: Int): String = if (value == 1) "服务端转码" else "关闭"
