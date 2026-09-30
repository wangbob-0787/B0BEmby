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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

/**
 * 按钮行左组（靠左，播放类）—— 父亲 2026-09-30 定：
 * 快退10秒 · 播放/暂停 · 快进10秒 · 倍速 · 下一集 · 选集
 * （快退/播放/快进/下一集是动作键，不走菜单项枚举）
 */
val PLAYER_PLAY_MENU = listOf(
    PlayerMenuItem.SPEED,
    PlayerMenuItem.EPISODES
)

/**
 * 按钮行右组（靠右，信息类）—— 父亲 2026-09-30 定：
 * 字幕 · 弹幕 · 声音 · 信息 · 演员 · 更多
 */
val PLAYER_INFO_MENU = listOf(
    PlayerMenuItem.SUBTITLE,
    PlayerMenuItem.DANMAKU,
    PlayerMenuItem.AUDIO,
    PlayerMenuItem.INFO,
    PlayerMenuItem.CAST,
    PlayerMenuItem.MORE
)

/** 主菜单 = 左组 + 右组（调用方按内容有无过滤） */
val PLAYER_MAIN_MENU = PLAYER_PLAY_MENU + PLAYER_INFO_MENU

/** 「更多」页（播放校正与视频质量功能重复已去掉；返回靠遥控器返回键；
 *  跳过片头不在按钮行分组里，收到这里） */
val PLAYER_MORE_MENU = listOf(
    PlayerMenuItem.QUALITY,
    PlayerMenuItem.PLAY_MODE,
    PlayerMenuItem.BUFFER,
    PlayerMenuItem.INTRO
)

/** 白字黑描边:底部区域没有底衬,靠阴影保证亮画面上可读 */
val OverlayTextStyle = TextStyle(
    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 6f)
)

/** 官方主题绿(与详情页焦点色一致) */
val EmbyGreen = Color(0xFF52B54B)

// ── 官方 Emby TV 2.1.54g 播放界面实测色板（2026-09-30 规格，见 notes/ui-spec/emby-tv-playback-spec-20260930.md）──
// 焦点态在官方是「实心绿块 + 图标提亮」，不是白框/放大；进度条是细条（4px=2dp）。
val SpecFocusGreen = Color(0xFF428A39)   // 焦点态实心块
val SpecIconIdle = Color(0xFFA4A3A3)     // 图标 · 非焦点
val SpecIconFocus = Color(0xFFC6C6C6)    // 图标 · 焦点态
val SpecBarPlayed = Color(0xFF45913D)    // 进度条 · 已播
val SpecBarRest = Color(0xFF262626)      // 进度条 · 未播
val SpecPanelBg = Color(0xFF303030)      // 二级菜单面板底色（不透明）
val SpecClockText = Color(0xFFA6A6A6)    // 顶部时钟/次要文字
/** 焦点容器边长：官方实测 56px @1080p = 28dp；父亲要求图标更醒目，容器放大到 40dp */
val SpecFocusBox = 40.dp
/**
 * 按钮图标边长：官方图标可见高度 38–46px（19–23dp）。
 * 注意 Compose Material 图标自带内边距（24dp viewport 里图形只占约 18dp），
 * 直接给 20dp 只会画出约 30px —— 父亲实测"还是小"。给 26dp（≈52px viewport、可见约 39px）才对得上官方。
 */
val SpecIconSize = 26.dp

val OverlayTextStyleSoft = TextStyle(
    shadow = Shadow(color = Color.Black, offset = Offset(1.5f, 1.5f), blurRadius = 5f)
)

/** 播放结束时刻(当前钟点 + 剩余时长),进度条右侧显示用 */
fun formatEndClock(remainingMs: Long): String {
    val end = System.currentTimeMillis() + remainingMs.coerceAtLeast(0L)
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(end))
}

/**
 * 左右键快进快退时唤出的进度条:一行 = 已播时间 + 进度条 + (剩余时间 / 结束时刻),
 * 不展开控制条、不抢焦点。
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
    val remaining = (duration - position).coerceAtLeast(0L)
    // 官方 Emby TV 样式(2026-09-30 投影实测):
    //   细条 4px(=2dp) · 已播 #45913D · 未播 #262626 · 绿色竖线游标 · 目标时间写在进度条下方左侧
    //   官方不显示"快进/快退"字样,方向靠游标移动体现。
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 54.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatDuration(position),
                color = Color.White,
                fontSize = 18.sp,
                style = OverlayTextStyleSoft
            )
            Spacer(modifier = Modifier.width(14.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
            ) {
                val barWidth = maxWidth
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.CenterStart)
                        .background(SpecBarRest)
                ) {
                    if (duration > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(
                                    (buffered.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                )
                                .background(Color.White.copy(alpha = 0.22f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(
                                    (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                )
                                .background(SpecBarPlayed)
                        )
                    }
                }
                // 绿色竖线游标(官方实测:宽约 30px 高约 41px 的绿竖条)
                if (duration > 0) {
                    val fraction = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (barWidth * fraction) - 1.5.dp)
                            .width(3.dp)
                            .height(20.dp)
                            .background(SpecBarPlayed)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "-" + formatDuration(remaining) + " / " + formatEndClock(remaining),
                color = Color(0xFFD0D0D0),
                fontSize = 18.sp,
                style = OverlayTextStyleSoft
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        // 目标时间(官方放在进度条下方左侧,与左边已播时间同列)
        Text(
            text = formatDuration(position),
            color = SpecIconIdle,
            fontSize = 15.sp,
            style = OverlayTextStyleSoft
        )
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
    subtitle: String,
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
    // 下一集（右侧动作键，父亲 2026-09-30 定）
    onNextEpisode: () -> Unit = {},
    // 左上角剧集片名 Logo（官方播放界面左上角就是这张 ClearLogo，实测 x231-306, y60-146）
    logoUrl: String? = null,
    // 控制条重新出现时焦点落在哪个一级图标上(-1 = 落在播放暂停);
    // 从信息/演职人员整屏返回时用它把焦点还给刚才那项,而不是跳到暂停
    initialFocusIndex: Int = -1,
) {
    val playKeyFocus = remember { List(3) { FocusRequester() } } // 0 后退 / 1 播放暂停 / 2 前进
    val itemFocus = remember { List(PLAYER_MAIN_MENU.size) { FocusRequester() } }
    val nextEpisodeFocus = remember { FocusRequester() }
    var lastIconIndex by remember { mutableIntStateOf(initialFocusIndex.coerceAtLeast(0)) }
    var sheetWasOpen by remember { mutableStateOf(false) }

    // 面板出现时:默认焦点落在播放暂停;若是从信息/演职人员返回,还给原来那个图标
    LaunchedEffect(Unit) {
        delay(60)
        if (initialFocusIndex >= 0) {
            runCatching {
                itemFocus[initialFocusIndex.coerceIn(0, itemFocus.lastIndex)].requestFocus()
            }
        } else {
            runCatching { playKeyFocus[1].requestFocus() }
        }
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
        // 左上角剧集片名 Logo（官方用剧集的 ClearLogo，实测位置 x231-306、y60-146 → 内边距 54dp/28dp、高 43dp）
        if (!logoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    // 官方实测（F1 截图，1920×1080）：logo 显示 71×120px、距左 235px、距顶 60px。
                    // 但父亲 2026-09-30 要求往左移 —— 现在与剧名/时间文字的左边缘（48dp）对齐。
                    .padding(start = 48.dp, top = 30.dp)
                    .height(60.dp)
            )
        }
        // 底部渐变遮罩：白色图标/文字直接压在亮画面上会糊成一团（父亲 2026-09-30 实测）。
        // 官方在满屏内容时同样靠 scrim 压暗底部；高度 260dp 覆盖到进度条上方。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 官方实测：剧名/时间等文字的左边距 96px(=48dp)、底部 42px(=21dp)
                .padding(horizontal = 48.dp)
                .padding(bottom = 21.dp)
        ) {
            // 第一行:剧名（官方实测字面高 50px @1080p → 约 25sp）+ 右侧「结束 HH:mm」
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = OverlayTextStyle,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    // 官方在剧名右侧显示「结束 下午8:50」(实测 x1064-1181, y857-890)
                    text = "结束 " + formatEndClock((duration - position).coerceAtLeast(0L)),
                    color = SpecClockText,
                    fontSize = 15.sp,
                    maxLines = 1,
                    style = OverlayTextStyleSoft
                )
            }

            // 第二行:集名 + 播放方式(灰色,字号与进度条上的数字一致)
            val subLine = listOf(
                subtitle,
                if (playMethodLabel.isNotEmpty()) "（$playMethodLabel）" else ""
            ).filter { it.isNotEmpty() }.joinToString("  ")
            if (subLine.isNotEmpty()) {
                Spacer(modifier = Modifier.height(9.dp))   // 官方：剧名底 839 → 副行顶 857 = 18px
                Text(
                    text = subLine,
                    color = SpecIconIdle,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = OverlayTextStyleSoft
                )
            }

            Spacer(modifier = Modifier.height(20.dp))  // 官方：段间距 39px ≈ 19.5dp

            // 第三行:已播时间 + 进度条 + (剩余时间 / 结束时刻),三者同一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(position),
                    color = Color.White,
                    fontSize = 18.sp,
                    style = OverlayTextStyleSoft
                )
                Spacer(modifier = Modifier.width(24.dp))  // 官方：左时间右边界 183 → 进度条起点 230 = 47px
                // 进度条：官方是 4px(=2dp) 细条、无圆点；已播 #45913D、未播 #262626（2026-09-30 规格）
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.CenterStart)
                            .background(SpecBarRest)
                    ) {
                        if (duration > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(
                                        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                    )
                                    .background(SpecBarPlayed)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(26.dp))  // 官方：进度条终点 1679 → 右时间起点 1734 = 55px
                Text(
                    // 官方右侧只显示剩余时间(如 -17:57),不带结束时刻 —— 2026-09-30 规格
                    text = "-" + formatDuration((duration - position).coerceAtLeast(0L)),
                    color = Color(0xFFD0D0D0),
                    fontSize = 18.sp,
                    style = OverlayTextStyleSoft
                )
            }

            Spacer(modifier = Modifier.height(20.dp))  // 官方：段间距 39px ≈ 19.5dp

            // 第四行:按钮行 —— 分组由父亲 2026-09-30 定
            //   左组(靠左,播放类):快退10秒 · 播放/暂停 · 快进10秒 · 倍速 · 下一集 · 选集
            //   右组(靠右,信息类):字幕 · 弹幕 · 声音 · 信息 · 演员 · 更多
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── 左组:播放类 ──
                PanelIcon(
                    icon = Icons.Default.Replay10,
                    description = "后退10秒",
                    focusRequester = playKeyFocus[0],
                    onClick = onSeekBack
                )
                Spacer(modifier = Modifier.width(8.dp))
                PanelIcon(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = "播放暂停",
                    focusRequester = playKeyFocus[1],
                    onClick = onPlayPause,
                    downFocus = itemFocus[0]
                )
                Spacer(modifier = Modifier.width(8.dp))
                PanelIcon(
                    icon = Icons.Default.Forward10,
                    description = "前进10秒",
                    focusRequester = playKeyFocus[2],
                    onClick = onSeekForward
                )
                Spacer(modifier = Modifier.width(8.dp))
                PanelMenuButton(
                    item = PlayerMenuItem.SPEED,
                    selected = activeItem == PlayerMenuItem.SPEED,
                    focusRequester = itemFocus[PLAYER_MAIN_MENU.indexOf(PlayerMenuItem.SPEED).coerceIn(0, itemFocus.lastIndex)],
                    onClick = {
                        lastIconIndex = PLAYER_MAIN_MENU.indexOf(PlayerMenuItem.SPEED).coerceAtLeast(0)
                        onMenuSelect(PlayerMenuItem.SPEED)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                PanelIcon(
                    icon = Icons.Default.SkipNext,
                    description = "下一集",
                    focusRequester = nextEpisodeFocus,
                    onClick = onNextEpisode
                )
                Spacer(modifier = Modifier.width(8.dp))
                PanelMenuButton(
                    item = PlayerMenuItem.EPISODES,
                    selected = activeItem == PlayerMenuItem.EPISODES,
                    focusRequester = itemFocus[PLAYER_MAIN_MENU.indexOf(PlayerMenuItem.EPISODES).coerceIn(0, itemFocus.lastIndex)],
                    onClick = {
                        lastIconIndex = PLAYER_MAIN_MENU.indexOf(PlayerMenuItem.EPISODES).coerceAtLeast(0)
                        onMenuSelect(PlayerMenuItem.EPISODES)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))

                // 中间留空,把右组推到最右
                Spacer(modifier = Modifier.weight(1f))

                // ── 右组:信息类 ──
                menuItems.filter { it in PLAYER_INFO_MENU }.forEach { item ->
                    PanelMenuButton(
                        item = item,
                        selected = activeItem == item,
                        focusRequester = itemFocus[PLAYER_MAIN_MENU.indexOf(item).coerceIn(0, itemFocus.lastIndex)],
                        onClick = {
                            lastIconIndex = PLAYER_MAIN_MENU.indexOf(item).coerceAtLeast(0)
                            onMenuSelect(item)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

    }
}

@Composable
private fun PanelMenuButton(
    item: PlayerMenuItem,
    selected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    // 与三个播放键同构：同一个 40dp 容器、同一种居中方式、下划线画在容器内部底边
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SpecFocusGreen.copy(alpha = 0.55f) else Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = SpecFocusGreen,
            focusedContentColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier
            .size(SpecFocusBox)
            .focusRequester(focusRequester)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(SpecIconSize)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .width(24.dp)
                        .height(3.dp)
                        .background(EmbyGreen, RoundedCornerShape(2.dp))
                )
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
    // 官方形态：28dp 圆角方块，焦点时整块填充 #428A39、图标提亮；无缩放、无描边
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = SpecFocusGreen,
            focusedContentColor = Color.White
        ),
        modifier = Modifier
            .size(SpecFocusBox)
            .focusRequester(focusRequester)
            .then(
                if (downFocus != null) {
                    Modifier.focusProperties { down = downFocus }
                } else {
                    Modifier
                }
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.size(SpecIconSize)
            )
        }
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
            .background(SpecPanelBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .focusGroup()
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                color = SpecIconIdle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
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
    // 操作型条目(如"字幕提前/延后")不要勾选框:它们不是可选项,方框看着像"取消按钮"
    showCheckbox: Boolean = true,
    externalBringIntoView: BringIntoViewRequester? = null,
) {
    LaunchedEffect(firstFocus) {
        if (firstFocus != null) {
            delay(60)
            runCatching { firstFocus.requestFocus() }
        }
    }
    val scope = rememberCoroutineScope()
    val ownBringIntoView = remember { BringIntoViewRequester() }
    val bringIntoView = externalBringIntoView ?: ownBringIntoView
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SpecFocusGreen.copy(alpha = 0.30f) else Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = SpecFocusGreen.copy(alpha = 0.45f),
            focusedContentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged {
                if (it.isFocused) scope.launch { bringIntoView.bringIntoView() }
            }
            .then(
                if (firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 11.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
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
                } else if (!showCheckbox) {
                    // 操作型条目:右侧不放任何方框
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
        if (season != null && episode != null) append("S$season E$episode ")
        mediaInfo.name?.takeIf { it.isNotBlank() && it != title }?.let { append("$it ") }
        mediaInfo.productionYear?.let { append("$it ") }
        mediaInfo.runTimeTicks?.let { append("${it / 600_000_000}m ") }
        mediaInfo.officialRating?.takeIf { it.isNotBlank() }?.let { append(it) }
    }.trim()
    val overview = mediaInfo.overview?.takeIf { it.isNotBlank() } ?: "暂无剧情简介"

    // 无框:左边海报(高度与整条一致) + 右边文字;整条左右边距相等、几乎充满屏宽(照官方排版)
    Row(
        modifier = modifier
            .focusRequester(firstFocus)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Bottom
    ) {
        if (posterUrl.isNotEmpty()) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(152.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            // 第一行文字比海报顶低两行(照官方:海报更高,文字块底部对齐)
            Spacer(modifier = Modifier.height(46.dp))
            Text(
                text = title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = OverlayTextStyle
            )
            if (metaLine.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = metaLine,
                    color = Color(0xFFE0E0E0),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = OverlayTextStyleSoft
                )
            }
            if (techLine.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = techLine,
                    color = Color(0xFFBDBDBD),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = OverlayTextStyleSoft
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = overview,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFFEDEDED),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = OverlayTextStyleSoft
            )
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
        // 打开选集时要停在当前播放这一集,并把焦点也放在它上面(照官方客户端)
        val currentRequester = remember { BringIntoViewRequester() }
        val currentIndex = episodes.indexOfFirst { it.id == currentId }
        LaunchedEffect(episodes) {
            if (currentIndex >= 0) {
                delay(120)
                runCatching { currentRequester.bringIntoView() }
            }
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
                firstFocus = if (i == if (currentIndex >= 0) currentIndex else 0) firstFocus else null,
                externalBringIntoView = if (i == currentIndex) currentRequester else null
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
    // 无框:标题 + 一排大头像卡片;卡片可以超出屏幕边,左右键横向滚动(照官方排版)
    Column(modifier = modifier) {
        Text(
            text = "演职人员",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            style = OverlayTextStyle
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (people.isEmpty()) {
            Text(
                text = "暂无演职人员信息",
                color = Color(0xFFBDBDBD),
                fontSize = 15.sp,
                style = OverlayTextStyleSoft
            )
            return@Column
        }
        val rowFocus = remember { FocusRequester() }
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            delay(80)
            runCatching { rowFocus.requestFocus() }
        }
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .focusRequester(rowFocus)
                .focusGroup()
                .focusable()
                // 自己接左右键滚动:tv-material 的 Surface 焦点移动在这条横排里不带动滚动,
                // 父亲实测"按左右键不动",所以显式滚到下一/上一张卡片
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (e.key != Key.DirectionLeft && e.key != Key.DirectionRight) {
                        return@onPreviewKeyEvent false
                    }
                    val step = if (e.key == Key.DirectionRight) 1 else -1
                    val target = (listState.firstVisibleItemIndex + step)
                        .coerceIn(0, (people.size - 1).coerceAtLeast(0))
                    scope.launch { listState.animateScrollToItem(target) }
                    true
                },
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // key 必须带下标:同一个人可能在本集担任多个角色(如《金色》徐兵既是导演又是编剧),
            // 只按 id 做 key 会重复 → Compose 抛 IllegalArgumentException 直接闪退(父亲 2026-09-30 报)
            itemsIndexed(
                people,
                key = { index, person -> "${person.id ?: person.hashCode()}-$index" }
            ) { _, person ->
                PersonCard(
                    person = person,
                    imgWidth = 130.dp,
                    aspectRatio = 0.68f,
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
            onClick = { onTimeOffsetChange(timeOffsetMs - 500L) },
            showCheckbox = false
        )
        SheetRow(
            label = "字幕延后 0.5 秒",
            selected = false,
            onClick = { onTimeOffsetChange(timeOffsetMs + 500L) },
            showCheckbox = false
        )
        SheetRow(
            label = "偏移复位",
            selected = timeOffsetMs == 0L,
            onClick = { onTimeOffsetChange(0L) },
            trailing = String.format("%.1fs", timeOffsetMs / 1000f),
            showCheckbox = false
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
