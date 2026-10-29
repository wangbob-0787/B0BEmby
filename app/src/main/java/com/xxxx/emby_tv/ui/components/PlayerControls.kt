package com.xxxx.emby_tv.ui.components

import com.xxxx.emby_tv.util.pixelScrollRow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MovieCreation
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
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
import com.xxxx.emby_tv.util.hoverToFocus
import com.xxxx.emby_tv.util.pointerRegion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.boundsInWindow

/** 播放控制面板上的一级菜单项（名称与顺序照 VR 版，父亲 2026-10-28 定） */
enum class PlayerMenuItem(val label: String) {
    SUBTITLE("字幕"),
    DANMAKU("弹幕设置"),
    SPEED("播放速度"),
    EPISODES("选集"),
    PICK("选片"),
    INFO("信息"),
    CAST("演职人员"),
    MORE("更多"),
    AUDIO("音频选择"),
    QUALITY("视频质量"),
    PLAY_MODE("播放模式"),
    BUFFER("缓冲设置"),
    BACK("返回");

    /** 二级菜单标题(与一级文字一致,除"返回") */
    val sheetTitle: String get() = label

    /** 图标行使用的图标（与 VR 控制条同一套 Material 图标） */
    val icon: ImageVector
        get() = when (this) {
            SUBTITLE -> Icons.Default.ClosedCaption
            DANMAKU -> Icons.Default.Chat
            SPEED -> Icons.Default.Speed
            EPISODES -> Icons.Default.ViewList
            PICK -> Icons.Default.MovieCreation
            INFO -> Icons.Default.Info
            CAST -> Icons.Default.Groups
            MORE -> Icons.Default.MoreHoriz
            AUDIO -> Icons.Default.VolumeUp
            QUALITY -> Icons.Default.HighQuality
            PLAY_MODE -> Icons.Default.Repeat
            BUFFER -> Icons.Default.Memory
            BACK -> Icons.Default.ArrowBack
        }
}

/**
 * 控制条分组（父亲 2026-10-28 11:07 定：TV 内容与 VR 完全一致，
 * 只去掉 VR 影院三项与跳过片头，交互形态按遥控器保留）。
 *
 *   左组：字幕 · 弹幕
 *   中组：快退10 · 播放/暂停 · 快进10（动作键，不在枚举里）
 *   右组：倍速 · 选集 · 音轨 · 画质 · 信息 · 演职人员 · 更多
 */
val PLAYER_LEFT_MENU = listOf(
    PlayerMenuItem.SUBTITLE,
    PlayerMenuItem.DANMAKU
)

/** 右组菜单项（「退出」是动作键，不在列表里，画在最后） */
val PLAYER_RIGHT_MENU = listOf(
    PlayerMenuItem.SPEED,
    PlayerMenuItem.EPISODES,
    // 音轨与画质从「更多」里提到控制条上（父亲 2026-10-28：选片与退出撤掉后腾出位置）
    PlayerMenuItem.AUDIO,
    PlayerMenuItem.QUALITY,
    PlayerMenuItem.INFO,
    PlayerMenuItem.CAST,
    PlayerMenuItem.MORE
)

/** 主菜单 = 左组 + 右组（调用方按内容有无过滤；焦点下标表按这个顺序建） */
val PLAYER_MAIN_MENU = PLAYER_LEFT_MENU + PLAYER_RIGHT_MENU

/**
 * 「更多」二级菜单：播放模式 · 缓冲设置。
 * 音频与画质已提到一级控制条；VR 的选座 / 座椅调节 / 影厅亮度 / 播放版本是影院专属，TV 不做。
 */
val PLAYER_MORE_MENU = listOf(
    PlayerMenuItem.PLAY_MODE,
    PlayerMenuItem.BUFFER
)

/** 白字黑描边:底部区域没有底衬,靠阴影保证亮画面上可读 */
val OverlayTextStyle = TextStyle(
    shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 6f)
)

/** 官方主题绿(与详情页焦点色一致) */
// val EmbyGreen = Color(0xFF52B54B)  // 2026-10-03 一致性收敛：播放页不再写死绿色，统一取 MaterialTheme.colorScheme.secondary

// ── 官方 Emby TV 2.1.54g 播放界面实测色板（2026-09-30 规格，见 notes/ui-spec/emby-tv-playback-spec-20260930.md）──
// 焦点态在官方是「实心绿块 + 图标提亮」，不是白框/放大；进度条是细条（4px=2dp）。
// val SpecFocusGreen = Color(0xFF428A39)  // 2026-10-03 同上（原为官方实测绿，现统一主题色）
val SpecIconIdle = Color(0xFFA4A3A3)     // 图标 · 非焦点
val SpecIconFocus = Color(0xFFC6C6C6)    // 图标 · 焦点态
// val SpecBarPlayed = Color(0xFF45913D)  // 2026-10-03 同上（进度条已播色统一主题色）
val SpecBarRest = Color(0x3DFFFFFF)      // 进度条 · 未播（与 VR 同值）
val SpecPanelBg = Color(0xFF303030)      // 二级菜单面板底色（不透明）
val SpecClockText = Color(0xFFA6A6A6)    // 顶部时钟/次要文字
/** 强调色：全站只留这一个绿（VR 侧口径 #2FD57C） */
val SpecAccent = Color(0xFF2FD57C)
/** 选中/活动底色：绿 24%（VR 同值 0x3D2FD57C） */
val SpecSelected = Color(0x3D2FD57C)

/** 光点扫到（得焦点）的底色，照 VR：白色 14%；与「已选中」的绿块区分开 */
val SpecHover = Color(0x24FFFFFF)
/** 焦点容器边长：官方实测 56px @1080p = 28dp；父亲要求图标更醒目，容器放大到 40dp */
val SpecFocusBox = 84.dp
/**
 * 按钮图标边长：官方图标可见高度 38–46px（19–23dp）。
 * 注意 Compose Material 图标自带内边距（24dp viewport 里图形只占约 18dp），
 * 直接给 20dp 只会画出约 30px —— 父亲实测"还是小"。给 26dp（≈52px viewport、可见约 39px）才对得上官方。
 */
/**
 * 图标 46dp、按钮盒 84dp、按钮缝 4dp、组间 40dp —— 照 VR 控制条换算
 * （VR 像素 159/87/5/75 ÷ PX_PER_DP 1.5 = 106/58/3.3/50；12 颗在 1280dp 屏上塞不下
 *  106dp，按屏宽反算成 84dp，见 notes/b0bemby-vr-tv-diff-2026-10-28.md §6.2）。
 */
val SpecIconSize = 46.dp
val SpecButtonGap = 4.dp
val SpecGroupGap = 40.dp

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
                                .background(MaterialTheme.colorScheme.secondary)
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
                            .background(MaterialTheme.colorScheme.secondary)
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
    /** 点/拖进度条跳到哪（毫秒）；空实现 = 不可拖 */
    onSeekTo: (Long) -> Unit = {},
    isPlaying: Boolean,
    menuItems: List<PlayerMenuItem>,
    activeItem: PlayerMenuItem?,
    onMenuSelect: (PlayerMenuItem) -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onPlayPause: () -> Unit,

    onPick: () -> Unit = {},
    onExitPlayer: () -> Unit = {},
    // 左上角剧集片名 Logo（官方播放界面左上角就是这张 ClearLogo，实测 x231-306, y60-146）
    logoUrl: String? = null,
    // 上报一级按钮的屏幕坐标（px：左边缘 x 与宽度）——二级菜单要锚定在"按钮正上方"
    onButtonPositions: (Map<PlayerMenuItem, Pair<Int, Int>>) -> Unit = {},
    // 控制条重新出现时焦点落在哪个一级图标上(-1 = 落在播放暂停);
    // 从信息/演职人员整屏返回时用它把焦点还给刚才那项,而不是跳到暂停
    initialFocusIndex: Int = -1,
) {
    val playKeyFocus = remember { List(3) { FocusRequester() } } // 0 后退 / 1 播放暂停 / 2 前进
    val itemFocus = remember { List(PLAYER_MAIN_MENU.size) { FocusRequester() } }
    val btnPositions = remember { mutableStateMapOf<PlayerMenuItem, Pair<Int, Int>>() }
    androidx.compose.runtime.SideEffect { onButtonPositions(btnPositions.toMap()) }
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
                alignment = Alignment.CenterStart,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    // 官方实测（F1 截图）：logo 显示 71×120px、距左 235px、距顶 60px；
                    // 父亲 2026-09-30 要求左移，与剧名/时间文字左边缘（48dp）对齐。
                    // 父亲 2026-10-02：改成**固定宽度**，图片小就放大、大就缩小（高度按比例自动）
                    // 父亲 2026-10-03：只卡宽度会让「又高又窄」的 logo 竖着占半个屏。
                    // 只写 sizeIn(maxWidth/maxHeight) 不够——它只限上限，图片仍按自身尺寸绘制，
                    // 又高又窄的图不会被压到 60dp。必须**同时给定尺寸框 + FillBounds**：
                    //   先 size(width=140dp, height=60dp) 把框钉死，再让图按比例缩放进框内并居中。
                    //   结果：宽扁的铺满 140dp 宽、竖高的顶到 60dp 高，都不会出框。
                    .padding(start = 48.dp, top = 30.dp)
                    .size(width = 140.dp, height = 60.dp)
            )
        }
        // 底部遮罩：亮画面上白字白图标会糊（父亲 2026-09-30 实测）。
        // 2026-10-28 按 VR 控制条改：上透明渐入，底下压一层 VR 的玻璃渐变 + 顶部细高光描边，
        // 色值取 VR panel/PlayerOsd.kt（0xF0252830 → 0xE6131518，描边 0x3DFFFFFF/0x08FFFFFF）。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomStart)
                /*
                 * 控制条矩形的真正来源（父亲 2026-10-29 报减法没生效）。
                 * 之前上报的是外层 fillMaxSize 的容器，矩形等于整屏，判定永远"在控制条里"，
                 * 于是点播放区也去补发确认键、激活焦点按钮。这里才是那条底部横条本身。
                 */
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    com.xxxx.emby_tv.util.InputBridge.playerPanelRect =
                        android.graphics.RectF(b.left, b.top, b.right, b.bottom)
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                            Color(0xE6131518)
                        ),
                        startY = 0f
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
                    fontSize = 24.sp,
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
                /*
                 * 进度条可点可拖（父亲 2026-10-29 报"只能靠左右键跳 10 秒"）：
                 * 触摸区加高到 28dp 便于按，细条仍画 2dp；拖动中按手指位置画游标，
                 * 松手才真正跳，避免每帧都 seek。
                 */
                var dragFrac by remember { mutableStateOf<Float?>(null) }
                val total = duration.coerceAtLeast(1L)
                val shownFrac = dragFrac ?: (position.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .pointerInput(total) {
                            detectTapGestures { offset ->
                                onSeekTo(((offset.x / size.width).coerceIn(0f, 1f) * total).toLong())
                            }
                        }
                        .pointerInput(total) {
                            detectHorizontalDragGestures(
                                onDragStart = { off ->
                                    dragFrac = (off.x / size.width).coerceIn(0f, 1f)
                                },
                                onHorizontalDrag = { _, delta ->
                                    dragFrac = ((dragFrac ?: 0f) + delta / size.width).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    dragFrac?.let { onSeekTo((it * total).toLong()) }
                                    dragFrac = null
                                },
                                onDragCancel = { dragFrac = null },
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.CenterStart)
                            .background(SpecBarRest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(shownFrac)
                                .background(SpecAccent)
                        )
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

            // 第四行:按钮行 —— 内容与顺序照 VR 控制条（父亲 2026-10-28 11:07 定）
            //   左组:字幕 · 弹幕
            //   中组:快退10 · 播放/暂停 · 快进10
            //   右组:倍速 · 选集 · 选片 · 信息 · 演职人员 · 更多 · 退出
            // 尺寸按 VR 规格换算（按钮盒 84dp、图标 46dp、按钮缝 4dp、组间 40dp、左右 56dp），
            // 12 颗在 1280dp 宽的电视屏上正好放下。
            val shownMenu = menuItems
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── 左组 ──
                shownMenu.filter { it in PLAYER_LEFT_MENU }.forEach { item ->
                    PanelMenuButton(
                        item = item,
                        selected = activeItem == item,
                        focusRequester = itemFocus[PLAYER_MAIN_MENU.indexOf(item).coerceIn(0, itemFocus.lastIndex)],
                        onClick = {
                            lastIconIndex = PLAYER_MAIN_MENU.indexOf(item).coerceAtLeast(0)
                            onMenuSelect(item)
                        },
                        onPositioned = { x, w -> btnPositions[item] = x to w }
                    )
                    Spacer(modifier = Modifier.width(SpecButtonGap))
                }

                Spacer(modifier = Modifier.width(SpecGroupGap))

                // ── 中组:三颗动作键 ──
                PanelIcon(
                    icon = Icons.Default.Replay10,
                    description = "后退10秒",
                    focusRequester = playKeyFocus[0],
                    onClick = onSeekBack
                )
                Spacer(modifier = Modifier.width(SpecButtonGap))
                PanelIcon(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = "播放暂停",
                    focusRequester = playKeyFocus[1],
                    onClick = onPlayPause,
                    downFocus = itemFocus[0]
                )
                Spacer(modifier = Modifier.width(SpecButtonGap))
                PanelIcon(
                    icon = Icons.Default.Forward10,
                    description = "前进10秒",
                    focusRequester = playKeyFocus[2],
                    onClick = onSeekForward
                )

                Spacer(modifier = Modifier.width(SpecGroupGap))

                // ── 右组 ──
                shownMenu.filter { it in PLAYER_RIGHT_MENU }.forEach { item ->
                    PanelMenuButton(
                        item = item,
                        selected = activeItem == item,
                        focusRequester = itemFocus[PLAYER_MAIN_MENU.indexOf(item).coerceIn(0, itemFocus.lastIndex)],
                        onClick = {
                            lastIconIndex = PLAYER_MAIN_MENU.indexOf(item).coerceAtLeast(0)
                            onMenuSelect(item)
                        },
                        onPositioned = { x, w -> btnPositions[item] = x to w }
                    )
                    Spacer(modifier = Modifier.width(SpecButtonGap))
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
    onPositioned: ((Int, Int) -> Unit)? = null,
) {
    // 形态照 VR 控制条：按钮盒 84dp 圆角，焦点/菜单开着时铺绿 24%，图标白色；
    // 焦点不放大（VR 那侧也是纯换底色，放大在电视上会让相邻按钮抖动）。
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) SpecSelected else Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = SpecHover,
            focusedContentColor = Color.White
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier
            .size(SpecFocusBox)
            .focusRequester(focusRequester)
            .hoverToFocus(focusRequester)
            .then(
                if (onPositioned != null) {
                    Modifier.onGloballyPositioned { c ->
                        onPositioned(c.positionInRoot().x.toInt(), c.size.width)
                    }
                } else Modifier
            )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = Color.White,
                modifier = Modifier.size(SpecIconSize)
            )
        }
    }
}

@Composable
private fun PanelIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    downFocus: FocusRequester? = null,
) {
    // 与菜单按钮同构：同一尺寸盒、同一种焦点底色（绿 24%），焦点不放大
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = SpecHover,
            focusedContentColor = Color.White
        ),
        modifier = Modifier
            .size(SpecFocusBox)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester).hoverToFocus(focusRequester)
                } else Modifier
            )
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
    val sheetRegionKey = "sheet:" + title
    Column(
        modifier = modifier
            .background(SpecPanelBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            // 光点不在面板上时，摇杆上下不驱动这块面板（父亲 2026-10-28：光点指哪控哪）
            .pointerRegion(sheetRegionKey)
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
        /*
         * 菜单矩形上报给播放页：扳机落点要做减法（控制条之外、菜单之外才算「点画面」）。
         * 菜单是从控制条上方单独升起来的一块，所以它必须自己报一次，不能拿控制条的矩形代替。
         */
        val menuRectModifier = Modifier.onGloballyPositioned { coords ->
            val b = coords.boundsInWindow()
            com.xxxx.emby_tv.util.InputBridge.playerMenuRect =
                android.graphics.RectF(b.left, b.top, b.right, b.bottom)
        }
        androidx.compose.runtime.DisposableEffect(Unit) {
            onDispose { com.xxxx.emby_tv.util.InputBridge.playerMenuRect = null }
        }
        if (scrollable) {
            // 二级菜单/字幕菜单/弹幕菜单：选项多时摇杆推住就按像素滚（父亲 2026-10-29）
            val menuScroll = rememberScrollState()
            com.xxxx.emby_tv.util.RegisterVerticalScroll(menuScroll)
            Column(modifier = boxModifier.then(menuRectModifier).verticalScroll(menuScroll)) {
                content()
            }
        } else {
            Column(modifier = boxModifier.then(menuRectModifier)) {
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
    // 副行（字幕菜单用）：主行 Emby 的 DisplayTitle，副行母语名 Title。
    // 只有一行时中文轨认不出来，看着像全是英文字幕（VR 侧同款，父亲 2026-10-28 定）
    sub: String? = null,
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
    // 行自己的焦点锚点：firstFocus 没给时也能被指针悬停选中（PICO 光标 / 鼠标）
    val rowFocus = remember { FocusRequester() }
    val anchor = firstFocus ?: rowFocus
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(4.dp)),
        colors = ClickableSurfaceDefaults.colors(
            // 选中（当前生效项）与焦点（光标所在）都用 VR 的绿 24%，未选中透明
            containerColor = if (selected) Color(0x3D2FD57C) else Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = SpecHover,
            focusedContentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .onFocusChanged {
                if (it.isFocused) scope.launch { bringIntoView.bringIntoView() }
            }
            .focusRequester(anchor)
            .hoverToFocus(anchor),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 11.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!sub.isNullOrBlank()) {
                    Text(
                        text = sub,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp)),
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
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            // 第一行文字比海报顶低两行(照官方:海报更高,文字块底部对齐)
            Spacer(modifier = Modifier.height(46.dp))
            Text(
                text = title,
                fontSize = 24.sp,
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
    // 光点在照片排上时，摇杆左右横滚（滚轮按落点命中，指不到就不滚）
    val castRegionKey = "panel:cast"
    Column(
        modifier = modifier
            .pointerRegion(castRegionKey)
    ) {
        Text(
            text = "演职人员",
            fontSize = 24.sp,
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
        /*
         * 照片墙是这一页唯一的横向可滚区：显式把它登记成摇杆横向目标，
         * 别让别的横排覆盖掉。父亲 2026-10-29 实测这一排"能滚但慢、没惯性"，
         * 根因是它原来靠焦点一格一格移动，不走像素滚动，也带不上松手后的惯性。
         */
        var rowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        DisposableEffect(listState) {
            com.xxxx.emby_tv.util.PixelScroll.setHorizontal { px ->
                /*
                 * 只认光点落在照片墙上（父亲 2026-10-29 口径）：光点在别处就返回 false，
                 * 让上层退回去做快退快进，照片墙不跟着动。
                 */
                val b = rowBounds
                val inside = b == null || b.contains(
                    androidx.compose.ui.geometry.Offset(
                        com.xxxx.emby_tv.util.PointerTargets.cursorX,
                        com.xxxx.emby_tv.util.PointerTargets.cursorY
                    )
                )
                if (!inside) return@setHorizontal false
                scope.launch { listState.scrollBy(px) }
                true
            }
            onDispose { com.xxxx.emby_tv.util.PixelScroll.setHorizontal(null) }
        }
        LaunchedEffect(Unit) {
            delay(80)
            runCatching { rowFocus.requestFocus() }
        }
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .then(Modifier.pixelScrollRow(listState))
                .onGloballyPositioned { rowBounds = it.boundsInWindow() }
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

/** 缓冲设置：三档预设，与 VR 同口径（标准 15s 起播 · 50s 上限 / 较大 30 · 90 / 最大 60 · 180） */
@Composable
fun BufferPresetSheet(
    currentMinBufferMs: Int,
    onApplyPreset: (Int, Int) -> Unit,
    firstFocus: FocusRequester,
) {
    val presets = listOf(
        Triple("标准", 15_000, 50_000),
        Triple("较大", 30_000, 90_000),
        Triple("最大", 60_000, 180_000)
    )
    Column {
        presets.forEachIndexed { i, (label, minMs, maxMs) ->
            SheetRow(
                label = label,
                selected = currentMinBufferMs == minMs,
                onClick = { onApplyPreset(minMs, maxMs) },
                firstFocus = if (i == 0) firstFocus else null,
                trailing = "起播 " + (minMs / 1000) + " 秒 · 上限 " + (maxMs / 1000) + " 秒"
            )
        }
    }
}

/**
 * 字幕菜单（与 VR 完全一致，父亲 2026-10-29 00:11 定）
 *
 * · **没有「关」这一行**，也没有弹幕开关 —— 弹幕开关在「弹幕设置」菜单第一项；
 * · 点已经勾上的那一条 = 取消勾选，等于关闭字幕（VR 2026-10-06 晚就是这么定的）；
 * · 每行两行文字：主行 Emby 的 DisplayTitle，副行母语名 Title；
 * · 内封在前、外挂在后，组内按流序号升序；
 * · 一条字幕都没有时给一行占位「（这部片没有字幕）」，避免菜单整块空白；
 * · 字幕时间偏移在 VR 侧没有入口，这里也一并去掉（要校准先按 VR 的口径定，不各写一套）。
 */
@Composable
fun SubtitleSheet(
    tracks: List<MediaStreamDto>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    firstFocus: FocusRequester,
) {
    // 内封在前、外挂在后（sortedWith 稳定排序，同组保持服务端给的顺序）
    val sorted = tracks.sortedWith(compareBy({ it.isExternal == true }, { it.index ?: Int.MAX_VALUE }))
    Column {
        if (sorted.isEmpty()) {
            Text(
                text = "（这部片没有字幕）",
                color = Color(0xFFBDBDBD),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp)
            )
        }
        sorted.forEachIndexed { i, track ->
            val index = track.index ?: -1
            val label = track.displayTitle?.takeIf { it.isNotBlank() }
                ?: track.language?.takeIf { it.isNotBlank() }
                ?: track.codec?.uppercase()
                ?: "字幕"
            SheetRow(
                label = label,
                selected = selectedIndex == index,
                // 点已勾上的那条 = 取消（关字幕），不再单列「关」
                onClick = { onSelect(if (selectedIndex == index) -1 else index) },
                firstFocus = if (i == 0) firstFocus else null,
                sub = track.title?.takeIf { it.isNotBlank() && it != label }
            )
        }
    }
}

/** 音频选择（从「更多」进）：主行 DisplayTitle，副行编码 + 声道 */
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
            val label = track.displayTitle?.takeIf { it.isNotBlank() }
                ?: track.language?.takeIf { it.isNotBlank() }
                ?: track.codec?.uppercase()
                ?: "音轨"
            val extra = listOfNotNull(
                track.codec?.uppercase(),
                track.channels?.let { it.toString() + " 声道" }
            ).joinToString(" · ")
            SheetRow(
                label = label,
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                firstFocus = if (i == 0) firstFocus else null,
                sub = extra.takeIf { it.isNotBlank() && it != label }
            )
        }
    }
}

/** 弹幕设置：开关 · 字号 · 时间偏移（照 VR 七段，父亲 2026-10-28） */
@Composable
fun DanmakuSheet(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    offsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    firstFocus: FocusRequester,
) {
    val options = listOf(0.8f to "小", 1.0f to "标准", 1.3f to "大", 1.6f to "特大")
    Column {
        SheetRow(
            label = if (enabled) "显示弹幕：开" else "显示弹幕：关",
            selected = enabled,
            onClick = { onEnabledChange(!enabled) },
            firstFocus = firstFocus
        )
        options.forEach { (value, label) ->
            SheetRow(
                label = "字号：" + label,
                selected = kotlin.math.abs(scale - value) < 0.01f,
                onClick = { onScaleChange(value) }
            )
        }
        val secs = offsetMs / 1000f
        SheetRow(
            label = "弹幕偏移：" + when {
                offsetMs > 0 -> String.format("+%.1f 秒（提前）", secs)
                offsetMs < 0 -> String.format("%.1f 秒（推后）", secs)
                else -> "无"
            },
            selected = offsetMs != 0L,
            onClick = { onOffsetChange(0L) },
            showCheckbox = false
        )
        SheetRow(
            label = "弹幕提前 0.5 秒",
            selected = false,
            onClick = { onOffsetChange(offsetMs + 500L) },
            showCheckbox = false
        )
        SheetRow(
            label = "弹幕推后 0.5 秒",
            selected = false,
            onClick = { onOffsetChange(offsetMs - 500L) },
            showCheckbox = false
        )
        SheetRow(
            label = "弹幕提前 10 秒",
            selected = false,
            onClick = { onOffsetChange(offsetMs + 10_000L) },
            showCheckbox = false
        )
        SheetRow(
            label = "弹幕推后 10 秒",
            selected = false,
            onClick = { onOffsetChange(offsetMs - 10_000L) },
            showCheckbox = false
        )
    }
}

/** 播放速度：四档，与 VR 一致（0.5 / 1.0 / 1.5 / 2.0） */
@Composable
fun SpeedSheet(
    current: Float,
    onChange: (Float) -> Unit,
    firstFocus: FocusRequester,
) {
    val options = listOf(0.5f, 1.0f, 1.5f, 2.0f)
    Column {
        options.forEachIndexed { i, value ->
            SheetRow(
                label = if (value == 1.0f) "1.0x（正常）" else "" + value + "x",
                selected = kotlin.math.abs(current - value) < 0.01f,
                onClick = { onChange(value) },
                firstFocus = if (i == 0) firstFocus else null
            )
        }
    }
}

const val QUALITY_ORIGINAL = 200_000_000

/**
 * 视频质量：标签与 VR 统一成「原画 / 1080 / 1024 / 768」，
 * 行尾写清这一档的真实含义（原画=直推不转码，其余=服务端转码到该码率上限）。
 */
@Composable
fun QualitySheet(
    current: Int,
    onChange: (Int) -> Unit,
    firstFocus: FocusRequester,
) {
    val options = listOf(
        Triple(QUALITY_ORIGINAL, "原画", "直推不转码"),
        Triple(10_000_000, "1080", "转码 ≈10 兆"),
        Triple(5_000_000, "1024", "转码 ≈5 兆"),
        Triple(1_000_000, "768", "转码 ≈1 兆")
    )
    Column {
        options.forEachIndexed { i, (value, label, note) ->
            SheetRow(
                label = label,
                selected = current == value,
                onClick = { onChange(value) },
                firstFocus = if (i == 0) firstFocus else null,
                trailing = note
            )
        }
    }
}

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
    10_000_000 -> "1080"
    5_000_000 -> "1024"
    else -> "768"
}

/** 播放模式显示名 */
fun playModeName(mode: Int): String = when (mode) {
    0 -> "列表循环"
    1 -> "单集循环"
    else -> "播完停止"
}

