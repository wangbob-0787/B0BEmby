package com.xxxx.emby_tv.ui

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.tv.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.MimeTypes
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.xxxx.emby_tv.R
import com.xxxx.emby_tv.Utils
import com.xxxx.emby_tv.data.repository.EmbyRepository
import com.xxxx.emby_tv.data.model.BaseItemDto
import com.xxxx.emby_tv.data.model.MediaDto
import com.xxxx.emby_tv.data.model.MediaStreamDto
import com.xxxx.emby_tv.data.model.SessionDto
import com.xxxx.emby_tv.ui.components.PlayerControlPanel
import com.xxxx.emby_tv.ui.components.PlayerMenuItem
import com.xxxx.emby_tv.ui.components.SeekHud
import com.xxxx.emby_tv.ui.components.PLAYER_MAIN_MENU
import com.xxxx.emby_tv.ui.components.PLAYER_MORE_MENU
import com.xxxx.emby_tv.ui.components.QUALITY_ORIGINAL
import com.xxxx.emby_tv.ui.components.SheetShell
import com.xxxx.emby_tv.ui.components.InfoSheet
import com.xxxx.emby_tv.ui.components.SubtitleSheet
import com.xxxx.emby_tv.ui.components.AudioSheet
import com.xxxx.emby_tv.ui.components.DanmakuSheet
import com.xxxx.emby_tv.ui.components.SpeedSheet
import com.xxxx.emby_tv.ui.components.QualitySheet
import com.xxxx.emby_tv.ui.components.PlayModeSheet
import com.xxxx.emby_tv.ui.components.EpisodeListSheet
import com.xxxx.emby_tv.ui.components.CastListSheet
import com.xxxx.emby_tv.ui.components.BufferPresetSheet
import com.xxxx.emby_tv.ui.components.SheetRow
import com.xxxx.emby_tv.ui.components.qualityLabel
import com.xxxx.emby_tv.ui.components.playModeName
import com.xxxx.emby_tv.ui.components.playMethodLabel
import com.xxxx.emby_tv.ui.components.techLineOf
import com.xxxx.emby_tv.ui.components.getAudioTrack
import com.xxxx.emby_tv.ui.components.getVideoTrack

import com.xxxx.emby_tv.ui.player.PlayerTrackManager
import com.xxxx.emby_tv.ui.player.SubtitleConfigBuilder
import com.xxxx.emby_tv.ui.player.SubtitleOffsetController
import com.xxxx.emby_tv.danmaku.LiveDanmakuApi
import com.xxxx.emby_tv.danmaku.DanmakuTrack
import com.xxxx.emby_tv.danmaku.DanmakuView
import com.xxxx.emby_tv.ui.viewmodel.PlayerViewModel
import com.xxxx.emby_tv.util.ErrorHandler
import com.xxxx.emby_tv.util.DiagLog
import com.xxxx.emby_tv.data.local.PreferencesManager
import com.xxxx.emby_tv.data.remote.EmbyApi
import com.xxxx.emby_tv.data.remote.EmbyApi.CLIENT_VERSION
import com.xxxx.emby_tv.data.remote.HttpClient
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 播放器界面（Screen）- 使用 PlayerViewModel
 */
// 字幕顶部预留比例：UI 100% 映射到实际 88%，保证字幕不出屏
private const val SUBTITLE_TOP_RESERVED_FRACTION = 0.12f

// 音频安全模式（进程级）：
// 部分电视固件在"多声道 PCM → 立体声"系统降混路径上存在 bug，触发后音频 HAL 卡死，
// AudioTrack 创建持续返回 DEAD_OBJECT（有画面无声音、一直转圈加载），只能重启电视恢复。
// 触发过一次故障后，本进程内后续播放器一律直接以立体声安全模式构建，从源头避开该路径；
// 重启 APP 后自动重试原生多声道。
private var audioSafeModeChannels = -1
private const val AUDIO_RECOVERY_TAG = "AudioRecovery"

/**
 * 手写声道混合矩阵。不使用 ChannelMixingMatrix.create()：
 * 它只实现了少数组合（如 6->2、8->2），对 3->2、4->2、5->2、7->2 等会直接抛
 * UnsupportedOperationException（曾导致进入播放页即闪退）。
 *
 * 一维系数数组布局（由 media3 源码确认）：coefficients[inputChannel * outputChannelCount + outputChannel]
 *
 * @param inputChannels 输入声道数 (1..8)
 * @param outputChannels 输出声道数，小于输入声道数时执行降混
 */
private fun buildChannelMixingMatrix(inputChannels: Int, outputChannels: Int): ChannelMixingMatrix {
    val coefficients: FloatArray = if (inputChannels == outputChannels) {
        // 恒等矩阵
        FloatArray(inputChannels * outputChannels).also { c ->
            for (ch in 0 until inputChannels) c[ch * outputChannels + ch] = 1f
        }
    } else if (outputChannels == 2) {
        // 降混到立体声。各声道数的标准 WAV 声道序：
        // 3:FL,FR,FC  4:FL,FR,BL,BR  5:FL,FR,FC,BL,BR  6:FL,FR,FC,LFE,BL,BR
        // 7:FL,FR,FC,LFE,BC,SL,SR  8:FL,FR,FC,LFE,BL,BR,SL,SR
        // 约定（同 ITU-R/系统降混）：FL/FR 直通，FC 双侧 0.707，LFE 丢弃，
        // BL/SL 折叠到 FL，BR/SR 折叠到 FR，BC 双侧 0.707
        val g = 0.70710678f
        val toStereo: Array<Pair<Float, Float>> = when (inputChannels) {
            1 -> arrayOf(1f to 1f)
            3 -> arrayOf(1f to 0f, 0f to 1f, g to g)
            4 -> arrayOf(1f to 0f, 0f to 1f, g to 0f, 0f to g)
            5 -> arrayOf(1f to 0f, 0f to 1f, g to g, g to 0f, 0f to g)
            6 -> arrayOf(1f to 0f, 0f to 1f, g to g, 0f to 0f, g to 0f, 0f to g)
            7 -> arrayOf(1f to 0f, 0f to 1f, g to g, 0f to 0f, g to g, g to 0f, 0f to g)
            8 -> arrayOf(1f to 0f, 0f to 1f, g to g, 0f to 0f, g to 0f, 0f to g, g to 0f, 0f to g)
            else -> Array(inputChannels) { i ->
                when (i) {
                    0 -> 1f to 0f
                    1 -> 0f to 1f
                    2 -> g to g   // FC
                    3 -> 0f to 0f // LFE
                    else -> if (i % 2 == 0) g to 0f else 0f to g
                }
            }
        }
        FloatArray(inputChannels * 2).also { c ->
            toStereo.forEachIndexed { i, pair ->
                c[i * 2] = pair.first      // 输入声道 i → 输出 FL
                c[i * 2 + 1] = pair.second // 输入声道 i → 输出 FR
            }
        }
    } else {
        // 罕见组合（输入 > 输出 >= 3）：前 outputChannels 路恒等，多余声道丢弃
        FloatArray(inputChannels * outputChannels).also { c ->
            for (ch in 0 until outputChannels) c[ch * outputChannels + ch] = 1f
        }
    }
    return ChannelMixingMatrix(inputChannels, outputChannels, coefficients)
}
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    mediaId: String,
    playbackPositionTicks: Long = 0L,
    onPlaybackStateChanged: (isPlaying: Boolean) -> Unit = {},
    onNavigateToPlayer: (BaseItemDto) -> Unit = {},
    onRePlayer: (BaseItemDto, Long) -> Unit = { _, _ -> },
    onExit: () -> Unit = {},  // 退出：回首页（不是退出应用）
    onPickPoster: () -> Unit = {},  // 选片：回打开这部片的那张海报墙
) {
    val context = LocalContext.current
    val view = LocalView.current

    // 获取 Repository 用于直接访问
    val repository = remember { EmbyRepository.getInstance(context) }
    val serverUrl = repository.serverUrl ?: ""
    val apiKey = repository.apiKey ?: ""

    // 使用 rememberCoroutineScope() 替代 GlobalScope，确保协程可取消
    val scope = rememberCoroutineScope()

    // 状态变量
    var isPlaying by remember { mutableStateOf(false) }
    var isShowInfo by remember { mutableStateOf(false) }
    var media by remember { mutableStateOf<MediaDto>(MediaDto()) }
    var mediaInfo by remember { mutableStateOf<BaseItemDto>(BaseItemDto()) }
    var session by remember { mutableStateOf<SessionDto?>(null) }
    var hasReportedPlaying by remember { mutableStateOf(false) }

    // 收藏状态 - 在播放页层面管理，菜单打开/关闭时保持状态
    var isFavorite by remember { mutableStateOf(false) }

    var subtitleTracks by remember { mutableStateOf<List<MediaStreamDto>>(emptyList()) }
    var selectedSubtitleIndex by remember { mutableStateOf(-99) }
    var audioTracks by remember { mutableStateOf<List<MediaStreamDto>>(emptyList()) }
    var selectedAudioIndex by remember { mutableStateOf(-1) }
    var videoUrl by remember { mutableStateOf<String?>(null) }

    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var buffered by remember { mutableStateOf(0L) }

    var playbackCorrection by remember { mutableStateOf(0) } // 0: off, 1: server transcode
    var playMode by remember { mutableStateOf(0) } // 0: list loop, 1: single loop, 2: no loop
    var endedHandled by remember { mutableStateOf(false) }
    // 播放控制面板(按 ↓ 唤出):一级菜单 + 二级菜单
    var showPanel by remember { mutableStateOf(false) }
    var activeItem by remember { mutableStateOf<PlayerMenuItem?>(null) }
    var inMoreMenu by remember { mutableStateOf(false) }

    // 一级按钮的屏幕坐标（px：左边缘 + 宽度），由 PlayerControlPanel 上报。
    // 二级菜单据此锚定在"点它的那个按钮"正上方（父亲 2026-09-30 要求）
    var btnPositions by remember { mutableStateOf<Map<com.xxxx.emby_tv.ui.components.PlayerMenuItem, Pair<Int, Int>>>(emptyMap()) }
    val density = LocalDensity.current
    val screenWidthPx = LocalView.current.width      // 用于把菜单夹在屏幕安全边距内
    var maxStreamingBitrate by remember { mutableIntStateOf(QUALITY_ORIGINAL) }
    var interactionTick by remember { mutableIntStateOf(0) }
    // 最近一次处理返回键的时刻:长按/连按返回只算第一下,避免一口气退光所有层级
    var lastBackAt by remember { mutableLongStateOf(0L) }
    // 最近一次点开的一级菜单图标下标:从信息/演职人员返回时把焦点还给这一项
    var lastMenuIndex by remember { mutableIntStateOf(-1) }
    // 未按 ↓ 时按左右键快进快退:只唤出一条进度条(不展开整条控制条,不抢焦点)
    var seekHud by remember { mutableStateOf(false) }
    var seekHudForward by remember { mutableStateOf(true) }
    // 最后一次快进/快退的时刻:进度条按它计时 5 秒收起(别的按键不给它续命)
    var seekHudAt by remember { mutableLongStateOf(0L) }
    val sheetFirstFocus = remember { FocusRequester() }
    var showStats by remember { mutableStateOf(false) }

    // 继续播放/从头开始 按钮状态
    // 续播选择界面整体取消（父亲 2026-09-30）：进播放页不再弹「从头开始 / 继续播放」，
    // 也没有 3 秒倒计时。播放位置仍按 playbackPositionTicks 续播（这是播放器自身行为）。

    // 用于跟踪是否已经尝试过转码回退
    var hasTriedTranscodeFallback by remember { mutableStateOf(false) }
    /*
     * 换路重试计数。服务端在起转码的瞬间会偶发 500（2026-10-31 实测：报错那一刻服务端刚
     * 起 ffmpeg，三秒后同样请求就是 200），只允许换一次太容易白白失败一次给父亲看。
     */
    var fallbackAttempts by remember { mutableIntStateOf(0) }
    var currentTracks by remember { mutableStateOf<Tracks?>(null) }

    val preferencesManager = remember { PreferencesManager(context) }
    // 详情页选过的「版本」（Emby MediaSource id）。键名与 VR 共用：ver_choice_<itemId>
    val savedVersionSourceId = remember(mediaId) {
        preferencesManager.getString("ver_choice" + "_" + mediaId)?.takeIf { it.isNotBlank() }
    }
    // 画面上那一行"人话"提示（播放失败原因等）。父亲口径：不加重试按钮，重新点海报就是重试
    var hudText by remember { mutableStateOf<String?>(null) }

    // 缓冲设置 - 从 PreferencesManager 加载保存的值，使用 preferencesManager 作为 key 确保重新加载
    var minBufferMs by remember(preferencesManager.minBufferMs) { mutableIntStateOf(preferencesManager.minBufferMs) }
    var maxBufferMs by remember(preferencesManager.maxBufferMs) { mutableIntStateOf(preferencesManager.maxBufferMs) }
    var playbackBufferMs by remember(preferencesManager.playbackBufferMs) { mutableIntStateOf(preferencesManager.playbackBufferMs) }
    var rebufferMs by remember(preferencesManager.rebufferMs) { mutableIntStateOf(preferencesManager.rebufferMs) }
    var bufferSizeBytes by remember(preferencesManager.bufferSizeBytes) { mutableIntStateOf(preferencesManager.bufferSizeBytes) }

    // 倍速设置 - 从 PreferencesManager 加载保存的值
    var playbackSpeed by remember(preferencesManager.playbackSpeed) { mutableFloatStateOf(preferencesManager.playbackSpeed) }

    // 字幕位置设置 - 从 PreferencesManager 加载保存的值
    var subtitleBottomPadding by remember(preferencesManager.subtitleBottomPadding) { mutableFloatStateOf(preferencesManager.subtitleBottomPadding) }

    // 字幕时间偏移 - 仅当前播放会话生效
    var subtitleTimeOffsetMs by remember { mutableLongStateOf(0L) }

    /*
     * 播放页标记：让摇杆在播放页保持"方向语义"（下唤控制条、上音量、左右快退快进），
     * 不被滚轮通道吃掉（父亲 2026-10-29 报"往下要扫一下控制条才出来"）。
     */
    androidx.compose.runtime.DisposableEffect(Unit) {
        com.xxxx.emby_tv.util.InputBridge.playerScreenActive = true
        onDispose {
            com.xxxx.emby_tv.util.InputBridge.playerScreenActive = false
            com.xxxx.emby_tv.util.InputBridge.playerPanelVisible = false
            com.xxxx.emby_tv.util.InputBridge.playerTapToggle = null
        }
    }
    val subtitleOffsetController = remember { SubtitleOffsetController() }
    val overlaySubtitleView = remember { mutableStateOf<SubtitleView?>(null) }
    // 独立弹幕层:自解析 ASS 的 \move 定位,逐帧绘制(不依赖 Media3 的 SSA 解析,后者不支持 \move)
    // 弹幕设置(记忆到本地)
    val danmakuPrefs = remember { context.getSharedPreferences("emby_tv_prefs", Context.MODE_PRIVATE) }
    var danmakuScale by remember { mutableFloatStateOf(danmakuPrefs.getFloat("danmaku_scale", 1.0f)) }
    // 弹幕时间偏移（正 = 提前），与 VR 同一个口径，只对本次播放生效
    var danmakuOffsetMs by remember { mutableLongStateOf(0L) }
    // 右上角实时网速(父亲 2026-09-19 要求)
    var showSpeed by remember { mutableStateOf(danmakuPrefs.getBoolean("show_speed", true)) }
    var danmakuEnabled by remember { mutableStateOf(danmakuPrefs.getBoolean("danmaku_enabled", true)) }
    var danmakuTrack by remember { mutableStateOf<DanmakuTrack?>(null) }
    val danmakuViewRef = remember { mutableStateOf<DanmakuView?>(null) }

    // 收集设备支持的杜比视界profile
    val supportedDvProfiles by playerViewModel.supportedDvProfiles.collectAsState()


    val playbackInfoFailText = stringResource(R.string.failed_get_playback_info)
    var playbackTrigger by remember { mutableStateOf(0) }
    var currentVideoDecoderName by remember { mutableStateOf("") }
    var isTunnelingSafe by remember { mutableStateOf(false) }

    /**
     * 2026 深度探测：查询底层 MediaCodec 列表，确认硬件是否声明了隧道能力
     */
    fun checkActualHardwareTunnelingSupport(): Boolean {
        return try {
            // 检查最常用的两种 4K 格式
            val mimes = listOf(MimeTypes.VIDEO_DOLBY_VISION, MimeTypes.VIDEO_H265)
            val isMinesOK = mimes.any { mime ->
                val decoderInfos = MediaCodecUtil.getDecoderInfos(mime, false, false)
                decoderInfos.any { info ->
                    // 核心判断：硬件必须显式声明 it.tunneling 为 true
                    info.hardwareAccelerated && info.tunneling && !info.softwareOnly
                }
            }
            if (!isMinesOK) return false
            supportedDvProfiles.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // TrackSelector 构建函数（播放器重建时复用；copiedParameters 非空时直接继承原播放器参数）
    fun createTrackSelector(copiedParameters: TrackSelectionParameters? = null): DefaultTrackSelector {
        return DefaultTrackSelector(context).apply {
            if (copiedParameters != null) {
                setParameters(copiedParameters)
            } else {
                // 1. 获取基础参数
                val baseParameters = buildUponParameters()
                    .setPreferredTextLanguage("zh")
                    // 开启这个：允许尝试超出硬件声明能力的解码（对杜比 P7 至关重要）
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .build()

                // 2. 动态判断隧道模式：仅在电视支持且非音频软解时开启
                //    非 1.0 倍速时一律关隧道 —— 隧道模式走 MediaCodec 直通渲染,不支持改变播放速率,
                //    这也是"倍速调了没反应"的原因(切倍速时会重建播放器让开关跟着变)
                isTunnelingSafe = checkActualHardwareTunnelingSupport()
                val tunnelingEnabled = isTunnelingSafe && preferencesManager.playbackSpeed == 1.0f
                DiagLog.w(
                    context, "tunnel",
                    "隧道模式=${tunnelingEnabled} (硬件支持=${isTunnelingSafe}, 当前倍速=${preferencesManager.playbackSpeed}x)"
                )

                setParameters(
                    baseParameters.buildUpon()
                        .setTunnelingEnabled(tunnelingEnabled)
                        .build()
                )
            }
        }
    }


    /**
     * 构建渲染器工厂。
     * 核心：把多声道 PCM 在应用内降混到设备实际支持的输出声道数（如 5.1 → 2.0）再创建 AudioTrack，
     * 避免触发部分电视固件在"系统级多声道降混"路径上的 AudioTrack 创建失败 bug（HAL 卡死需重启电视）。
     *
     * 注意：AudioCapabilities.maxChannelCount 会把直通（DD+/Atmos bitstream）能力也算进去，
     * 在部分电视上严重虚高（实测 TCL 报 10），因此以 AudioDeviceInfo 上各输出设备的
     * 实际 PCM 声道数为准，两者取小。
     */
    fun createRenderersFactory(): DefaultRenderersFactory {
        val reportedMaxChannels =
            AudioCapabilities.getCapabilities(context).maxChannelCount.coerceIn(2, 8)
        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devicePcmChannels = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .flatMap { device -> device.channelCounts.toList() }
            .filter { it in 2..8 }
            .distinct()
            .sorted()
        val capableChannels = if (devicePcmChannels.isNotEmpty()) {
            minOf(reportedMaxChannels, devicePcmChannels.last())
        } else {
            reportedMaxChannels
        }
        val targetChannels = if (audioSafeModeChannels > 0) audioSafeModeChannels else capableChannels
        Log.i(
            AUDIO_RECOVERY_TAG,
            "音频输出配置: 上报最大声道数=$reportedMaxChannels, 设备实测PCM声道=$devicePcmChannels, " +
                "可用声道数=$capableChannels, 目标输出声道数=$targetChannels"
        )
        DiagLog.w(
            context, "audio",
            "变速路径: 强制 Sonic(原有 AudioTrack.playbackParams 被关闭)"
        )
        return object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return try {
                    val channelMixer = ChannelMixingAudioProcessor()
                    // 全量注册 1..8 声道矩阵：超出目标的降混，未超出的恒等通过。
                    // 必须保证任何 PCM 输入都有矩阵，否则 media3 会抛 UnhandledAudioFormatException
                    // 并升级为致命播放错误（黑屏）。
                    for (inputChannels in 1..8) {
                        val outputChannels =
                            if (inputChannels > targetChannels) targetChannels else inputChannels
                        channelMixer.putChannelMixingMatrix(
                            buildChannelMixingMatrix(inputChannels, outputChannels)
                        )
                    }
                    val speedActive = preferencesManager.playbackSpeed != 1.0f
                    DiagLog.w(
                        context, "audio",
                        "AudioSink 构建 倍速=${preferencesManager.playbackSpeed}x float输出=$enableFloatOutput " +
                                "→ 强制Sonic=true float=${!speedActive && enableFloatOutput} PCM=${speedActive}"
                    )
                    DefaultAudioSink.Builder(context)
                        // 变速时不能开 float 输出:Sonic 只处理 16bit PCM,float 会让它失效 →
                        // ExoPlayer 判定"不支持变速"并把速度 mask 回 1.0(实测日志:声明 2.0x,5 秒后变 1.0x)
                        .setEnableFloatOutput(if (speedActive) false else enableFloatOutput)
                        // 电视固件(海信/Vidda 实测)不支持 AudioTrack 的 playbackParams 变速:
                        // 开着它时播放器内部 speed 显示改成功、实际音画都不动 —— 强制走 Sonic 软件变速
                        .setEnableAudioTrackPlaybackParams(false)
                        // 保留 Sonic（倍速支持）并前置声道降混处理器
                        .setAudioProcessors(arrayOf(channelMixer, SonicAudioProcessor()))
                        .apply {
                            // 倍速 ≠ 1.0 时必须走 PCM:音频直通(passthrough)下 ExoPlayer 同样会放弃变速
                            if (speedActive) {
                                setAudioCapabilities(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES)
                            }
                        }
                        .build()
                } catch (e: Exception) {
                    // 兜底：任何矩阵/构建异常都回落到原生 sink，绝不影响进入播放页
                    Log.e(AUDIO_RECOVERY_TAG, "构建降混音频管线失败，回落原生输出", e)
                    DefaultAudioSink.Builder(context)
                        .setEnableFloatOutput(enableFloatOutput)
                        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                        .build()
                }
            }
        }.apply {
            // 1. 核心：增加解码器自动降级判断
            setMediaCodecSelector { mimeType, requiresSecure, requiresTunneling ->
                // 1. 获取默认解码器（如果是杜比视频，首选通常是 DV 解码器）
                val dvDecoders =
                    MediaCodecUtil.getDecoderInfos(mimeType, requiresSecure, requiresTunneling)

                if (mimeType == MimeTypes.VIDEO_DOLBY_VISION) {
                    // 2. 获取 HEVC 备选解码器
                    val hevcDecoders = MediaCodecUtil.getDecoderInfos(
                        MimeTypes.VIDEO_H265,
                        requiresSecure,
                        requiresTunneling
                    )

                    // 3. 智能判断排序
                    val combined = ArrayList<MediaCodecInfo>()

                    // 检查是否有任何一个杜比解码器明确声称支持当前 Level/Profile
                    // Media3 会自动过滤掉完全不支持的，但对于 P7，很多电视报的是 "SUPPORT_UNKNOWN" 或功能受限
                    // 而且必须要有杜比视界的profile
                    val isHardwareLikelyToHandleDV =
                        dvDecoders.any { it.hardwareAccelerated && !it.softwareOnly } && supportedDvProfiles.isNotEmpty()

                    if (isHardwareLikelyToHandleDV) {
                        // 高性能电视：杜比优先，HEVC 垫后
                        combined.addAll(dvDecoders)
                        combined.addAll(hevcDecoders)
                    } else {
                        // 低性能电视（或 DV 解码器缺失）：HEVC 优先，确保能播
                        combined.addAll(hevcDecoders)
                        combined.addAll(dvDecoders)
                    }
                    combined
                } else {
                    dvDecoders
                }
            }

            // 逻辑：ExoPlayer 会先扫描系统 MediaCodecList。
            // 1. 如果电视硬件报支持该 Codec，优先用硬解。
            // 2. 如果电视硬件不支持（如 TrueHD/DTS），则自动切换到你的 FFmpeg 扩展。
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
//        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

            // 这确保了渲染器能够处理复杂的字幕样式
            setEnableDecoderFallback(true)
        }
    }

    // 同步检查并修复约束条件 - 在 loadControl 创建之前执行
    if (maxBufferMs < minBufferMs || minBufferMs < rebufferMs || rebufferMs < playbackBufferMs) {
        val defaults = preferencesManager.getBufferDefaults()
        minBufferMs = defaults.minBufferMs
        maxBufferMs = defaults.maxBufferMs
        playbackBufferMs = defaults.playbackBufferMs
        rebufferMs = defaults.rebufferMs
        bufferSizeBytes = defaults.bufferSizeBytes
        preferencesManager.resetBufferDefaults()
    }

    // 缓存控制配置 - 针对 TV 端视频流媒体优化（构建函数，播放器重建时复用）
    fun createLoadControl(): DefaultLoadControl {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryClass = activityManager.memoryClass
        val largeHeap = context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP != 0

        // 直接读配置(权威值):改完缓冲设置立刻重建播放器时,Compose 状态变量这一帧可能还没刷新,
        // 读状态会拿到旧值 → 表现为"缓冲设置不生效"
        val minMs = preferencesManager.minBufferMs
        val maxMs = preferencesManager.maxBufferMs
        val playMs = preferencesManager.playbackBufferMs
        val reBufMs = preferencesManager.rebufferMs
        val sizeBytes = preferencesManager.bufferSizeBytes

        val targetBytes = when {
            largeHeap || memoryClass >= 512 -> sizeBytes.coerceAtLeast(256 * 1024 * 1024)
            memoryClass >= 256 -> sizeBytes.coerceAtLeast(128 * 1024 * 1024)
            memoryClass >= 128 -> sizeBytes.coerceAtLeast(64 * 1024 * 1024)
            else -> sizeBytes.coerceAtLeast(32 * 1024 * 1024)
        }
        Log.i(
            "B0BEmbyPlayer",
            "LoadControl min=${minMs} max=${maxMs} play=${playMs} rebuf=${reBufMs} targetBytes=$targetBytes"
        )
        DiagLog.w(
            context, "buffer",
            "LoadControl min=${minMs} max=${maxMs} play=${playMs} rebuf=${reBufMs} targetBytes=$targetBytes"
        )

        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minMs,
                maxMs,
                playMs,
                reBufMs
            )
            .setTargetBufferBytes(targetBytes)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val bandwidthMeter = remember {
        DefaultBandwidthMeter.getSingletonInstance(context)
    }

    val okHttpClient = HttpClient.getClient(context)

// 创建支持 OkHttp 的工厂
    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        .setUserAgent(EmbyApi.CLIENT + "/" + CLIENT_VERSION)
        .setDefaultRequestProperties(
            mapOf(
                "X-Emby-Client" to EmbyApi.CLIENT,
                "X-Emby-Client-Version" to CLIENT_VERSION,
                "X-Emby-Device-Name" to EmbyApi.DEVICE_NAME,
            )
        )

    val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(dataSourceFactory)

    // ExoPlayer 构建函数（音频自动恢复时需要整体重建播放器）
    fun buildPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context, createRenderersFactory())
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(createTrackSelector())
            .setLoadControl(createLoadControl())
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                playWhenReady = true // Ensure it tries to play immediately
            }
    }

    var player by remember { mutableStateOf(buildPlayer()) }

    /*
     * 点画面 = 播放/暂停（父亲 2026-10-29：光点在播放区扣扳机，画面暂停的同时还触发了
     * 焦点所在的那个按钮）。现在点画面的动作由播放页自己接：扳机落在控制条以外就只切
     * 播放/暂停，不再补发确认键去激活按钮。
     */
    androidx.compose.runtime.LaunchedEffect(showPanel) {
        com.xxxx.emby_tv.util.InputBridge.playerPanelVisible = showPanel
    }
    androidx.compose.runtime.LaunchedEffect(player) {
        com.xxxx.emby_tv.util.InputBridge.playerTapToggle = {
            runCatching { if (player.isPlaying) player.pause() else player.play() }
        }
    }

    // 缓冲参数只能在建播放器时注入(LoadControl 不可运行时替换),
    // 所以改完缓冲设置必须重建播放器 —— 保留当前位置、媒体项、轨道与播放状态
    fun rebuildPlayerKeepingPosition() {
        val oldPlayer = player
        val resumePositionMs = oldPlayer.currentPosition
        val currentMediaItem = oldPlayer.currentMediaItem
        val trackParameters = oldPlayer.trackSelectionParameters
        val wasPlaying = oldPlayer.isPlaying
        oldPlayer.stop()
        val newPlayer = buildPlayer()
        if (currentMediaItem != null) {
            newPlayer.setMediaItem(currentMediaItem, resumePositionMs)
        }
        newPlayer.trackSelectionParameters = trackParameters
        newPlayer.prepare()
        newPlayer.playWhenReady = wasPlaying
        player = newPlayer
        Log.i("B0BEmbyPlayer", "已按新缓冲参数重建播放器(position=${resumePositionMs}ms)")
        DiagLog.w(context, "buffer", "已按新缓冲参数重建播放器 position=${resumePositionMs}ms 续播")
    }

    // ===== 音频故障自动恢复（全程自动，无用户交互）=====
    // 0: 正常；1: 已用立体声安全模式重建过播放器；2: 已降级为无声播放
    var audioRecoveryStage by remember { mutableIntStateOf(0) }
    // 当前播放器实例是否出现过音频输出错误（播放器重建时自动重置）
    var currentPlayerAudioError by remember { mutableStateOf(false) }

    fun handleAudioSinkFailure() {
        when (audioRecoveryStage) {
            0 -> {
                audioRecoveryStage = 1
                audioSafeModeChannels = 2
                Log.w(AUDIO_RECOVERY_TAG, "音频输出初始化失败，自动切换立体声安全模式并重建播放器")
                val oldPlayer = player
                val resumePositionMs = oldPlayer.currentPosition
                val currentMediaItem = oldPlayer.currentMediaItem
                val trackParameters = oldPlayer.trackSelectionParameters
                oldPlayer.stop()
                val newPlayer = buildPlayer()
                if (currentMediaItem != null) {
                    newPlayer.setMediaItem(currentMediaItem, resumePositionMs)
                }
                newPlayer.trackSelectionParameters = trackParameters
                newPlayer.prepare()
                newPlayer.playWhenReady = true
                player = newPlayer // Compose 自动释放旧实例、重新绑定监听与画面
            }
            1 -> {
                audioRecoveryStage = 2
                Log.w(AUDIO_RECOVERY_TAG, "立体声安全模式仍无音频，自动转为无声播放（视频继续）")
                val p = player
                p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
                hudText = context.getString(R.string.audio_output_degraded)
            }
        }
    }

    var isBuffering by remember { mutableStateOf(true) } // Start true assuming we wait for load
    var downloadSpeed by remember { mutableStateOf(0L) }
    var lastSpeedUpdate by remember { mutableStateOf(0L) }

    /*
     * 摇杆直连 seek（2026-10-29 父亲报「扳住快进几秒就停、随后焦点在按钮之间跳」）：
     * 摇杆推住时不再发方向键，直接按毫秒偏移 seek，焦点与按键流无关。
     */
    DisposableEffect(player) {
        com.xxxx.emby_tv.util.PlayerSeekBus.seekBy = { deltaMs ->
            if (duration > 0L) {
                val np = (player.currentPosition + deltaMs).coerceIn(0L, duration)
                player.seekTo(np)
                position = np
                if (!player.isPlaying) player.play()
            }
        }
        onDispose { com.xxxx.emby_tv.util.PlayerSeekBus.seekBy = null }
    }

    // Long press state
    var leftKeyDownTime by remember { mutableStateOf(0L) }
    var rightKeyDownTime by remember { mutableStateOf(0L) }

    // 即使暂停播放也不会熄屏
    DisposableEffect(view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = previous
        }
    }

    // 应用倍速（player 重建后也需重新应用）
    LaunchedEffect(player, playbackSpeed) {
        player.setPlaybackSpeed(playbackSpeed)
        Log.i("B0BEmbyPlayer", "应用倍速 ${playbackSpeed}x player=${player.hashCode()}")
        DiagLog.w(
            context, "speed",
            "应用倍速 ${playbackSpeed}x → 播放器实际 ${player.playbackParameters.speed}x"
        )
    }

    // 倍速实锤:每 5 秒记一次"播放位置增量 / 真实经过时间",真 2x 应该 ≈ 2.0
    LaunchedEffect(player) {
        var lastPos = player.currentPosition
        var lastAt = System.currentTimeMillis()
        while (isActive) {
            delay(5000)
            val nowPos = player.currentPosition
            val nowAt = System.currentTimeMillis()
            val wallSec = (nowAt - lastAt) / 1000.0
            val deltaSec = (nowPos - lastPos) / 1000.0
            val measured = if (wallSec > 0) deltaSec / wallSec else 0.0
            val af = player.audioFormat
            DiagLog.w(
                context, "speedProbe",
                "位置+${"%.1f".format(deltaSec)}s / 经过${"%.1f".format(wallSec)}s = " +
                        "${"%.2f".format(measured)}x(播放器声明 ${player.playbackParameters.speed}x " +
                        "音频=${af?.sampleMimeType ?: "无"} ${af?.channelCount ?: 0}ch)"
            )
            lastPos = nowPos
            lastAt = nowAt
        }
    }

    // 字幕时间偏移：接管字幕渲染（内置 subtitleView 已隐藏，由 overlay SubtitleView 显示）
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onCues(cueGroup: CueGroup) {
                subtitleOffsetController.onCues(cueGroup)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                subtitleOffsetController.reset()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (isActive) {
            overlaySubtitleView.value?.setCues(
                subtitleOffsetController.cuesFor(player.currentPosition, subtitleTimeOffsetMs)
            )
            delay(50)
        }
    }

    LaunchedEffect(leftKeyDownTime) {
        if (leftKeyDownTime > 0) {
            delay(500)
            while (isActive) {
                val newPos = (player.currentPosition - 30000).coerceAtLeast(0)
                player.seekTo(newPos)
                position = newPos // Update progress bar
                if (!player.isPlaying) {
                    player.play() // Auto-play if paused
                }
                delay(200)
            }
        }
    }

    LaunchedEffect(rightKeyDownTime) {
        if (rightKeyDownTime > 0) {
            delay(500)
            while (isActive) {
                val newPos = (player.currentPosition + 30000).coerceAtMost(duration)
                player.seekTo(newPos)
                position = newPos // Update progress bar
                if (!player.isPlaying) {
                    player.play() // Auto-play if paused
                }
                delay(200)
            }
        }
    }


    // 实现转码回退逻辑
    fun fallbackToServerTranscode(force: Boolean = false) {
        if (hasTriedTranscodeFallback && !force) {
            return
        }

        hasTriedTranscodeFallback = true
        Log.d("PlayerScreen", "开始执行转码回退逻辑")

        // 使用 rememberCoroutineScope() 替代 GlobalScope，确保协程可取消
        scope.launch(Dispatchers.IO) {
            val requestAudioIndex = if (selectedAudioIndex <= -1) null else selectedAudioIndex
            val requestSubtitleIndex =
                if (selectedSubtitleIndex <= -1) null else selectedSubtitleIndex

            try {
                if (media.mediaSources?.firstOrNull()?.transcodingUrl != null) {
                    repository.stopActiveEncodings(
                        media.playSessionId
                    )
                }
                val mediaResult = repository.getPlaybackInfo(
                    mediaId,
                    if (position > 0) position * 10000 else playbackPositionTicks,
                    requestAudioIndex,
                    requestSubtitleIndex,
                    true,
                    maxStreamingBitrate,
                    mediaSourceId = savedVersionSourceId
                )

                if (mediaResult.mediaSources.isNullOrEmpty()) {
                    Log.e("PlayerScreen", "转码回退失败：无法获取播放信息")
                    return@launch
                }

                // 直接访问mediaSources属性
                val source = mediaResult.mediaSources.firstOrNull()

                // 获取转码URL - 优先使用直链（音频两类 URL 可能都为 null，回退拼静态直链）
                var path = source?.directStreamUrl ?: source?.transcodingUrl
                val audioOnly = source?.mediaStreams?.none { it.type.equals("Video", ignoreCase = true) } != false
                if (audioOnly) {
                    if (path == null) {
                        val ext = source?.container?.takeIf { it.isNotBlank() } ?: "mp3"
                        path = "/Audio/$mediaId/stream.$ext?static=true&api_key=$apiKey"
                    } else if (!path.contains("static=")) {
                        path = path + (if (path.contains("?")) "&" else "?") + "static=true"
                    }
                }

                if (path != null) {
                    val newVideoUrl = "${serverUrl}/emby$path"

                    // 切换到主线程更新UI
                    withContext(Dispatchers.Main) {
                        Log.d("PlayerScreen", "转码回退成功，更新视频URL")
                        videoUrl = newVideoUrl

                        media = mediaResult

                        //重要步骤
                        player.stop()

                        // 重新设置播放源
                        val mediaItem = MediaItem.Builder()
                            .setUri(newVideoUrl)
                            .build()

                        player.setMediaItem(mediaItem, position)
                        Log.e(
                            "FFmpegCheck",
                            "FFmpeg Library Available: ${androidx.media3.decoder.ffmpeg.FfmpegLibrary.isAvailable()}"
                        )
                        player.prepare()
                        player.playWhenReady = true
                    }
                } else {
                    Log.e("PlayerScreen", "转码回退失败：没有找到转码URL")
                }
            } catch (e: Exception) {
                Log.e("PlayerScreen", "转码回退过程中出现异常: ${e.message}", e)
            }
        }
    }


    // 加载设置（playbackCorrection 不再持久化，每次进入播放页默认关闭，只对当前视频生效）
    LaunchedEffect(Unit) {
        try {
            val prefs = context.getSharedPreferences("emby_tv_prefs", Context.MODE_PRIVATE)
            playMode = prefs.getInt("play_mode", 0)
        } catch (e: Exception) {
            ErrorHandler.logError("PlayerScreen", "操作失败", e)
        }
    }

    // 提供一个函数供 UI 调用切换（例如点击列表时调用）
    fun changeTrack(audioIndex: Int, subIndex: Int) {
        val needChange =
            (selectedAudioIndex != audioIndex && audioIndex > -1) || (selectedSubtitleIndex != subIndex && subIndex > -1)
        selectedAudioIndex = audioIndex
        selectedSubtitleIndex = subIndex
        hasTriedTranscodeFallback = false
        fallbackAttempts = 0
        if (needChange && !(media.mediaSources?.firstOrNull()?.supportsDirectPlay
                ?: false)
        ) playbackTrigger++ // 只有手动修改时，才递增触发器，重启协程
    }

    // 控制面板标题:剧名 + S/E(电影用片名)
    // 第一行标题:剧集显示剧名,电影显示片名
    fun panelTitle(): String {
        val series = mediaInfo.seriesName
        val name = mediaInfo.name
        return when {
            !series.isNullOrEmpty() -> series
            !name.isNullOrEmpty() -> name
            else -> ""
        }
    }

    // 第二行副标题:剧集为 "S季:E集  集名",电影为空
    fun panelSubtitle(): String {
        if (mediaInfo.seriesName.isNullOrEmpty()) return ""
        val season = mediaInfo.parentIndexNumber
        val episode = mediaInfo.indexNumber
        val name = mediaInfo.name ?: ""
        val ep = when {
            season != null && episode != null -> "S$season E$episode"
            episode != null -> "第 $episode 集"
            else -> ""
        }
        // 官方副行格式（F1 实测）：
        //   S1 E1  金色 - S01E01 - 集标题  25 8月 2026  46m  CC (Und)
        // 即：季集(空格分隔,不是冒号) · 剧集-集号-集名 · 首播日期 · 时长 · 字幕标记
        val dateText = mediaInfo.premiereDate?.let { d ->
            // 形如 2026-08-24T00:00:00.0000000Z，不依赖日期解析库，直接切字符串
            runCatching {
                val y = d.substringBefore('-')
                val mo = d.substringAfter('-').substringBefore('-')
                val day = d.substringAfter('-').substringAfter('-').substringBefore('T')
                if (y.length == 4 && mo.length == 2 && day.length == 2) {
                    "${day.toInt()} ${mo.toInt()}月 $y"
                } else ""
            }.getOrDefault("")
        } ?: ""
        val runtimeText = mediaInfo.runTimeTicks?.let { t ->
            val minutes = t / 10_000_000L / 60L
            if (minutes > 0) "${minutes}m" else ""
        } ?: ""
        return listOf(ep, name, dateText, runtimeText)
            .filter { it.isNotEmpty() }
            .joinToString("  ")
    }

    // 数据加载逻辑
    LaunchedEffect(mediaId, playbackCorrection, playbackTrigger, maxStreamingBitrate) {
        //  这里的逻辑只会运行一次（初始化时）或者在手动递增 trigger 时运行
        val requestAudioIndex = if (selectedAudioIndex <= -1) null else selectedAudioIndex
        val requestSubtitleIndex = if (selectedSubtitleIndex <= -1) null else selectedSubtitleIndex

        try {
            if (media.mediaSources?.firstOrNull()?.transcodingUrl != null) {
                repository.stopActiveEncodings(
                    media.playSessionId
                )
            }
            DiagLog.w(context, "playerStart", "mediaId=$mediaId position=$position")
            val mediaResult = repository.getPlaybackInfo(
                mediaId,
                if (position > 0) position * 10000 else playbackPositionTicks,
                requestAudioIndex,
                requestSubtitleIndex,
                // 首次播放不要因为"以前某次失败过"就降级成 h264(会把 4K/HDR 一起丢掉);
                // 只有用户显式设置(playbackCorrection==1)才降级,失败后的回退仍走下面的专用分支
                playbackCorrection == 1,
                maxStreamingBitrate,
                // 详情页选过的版本（MediaSourceId），没选过就交给服务端按默认挑
                mediaSourceId = savedVersionSourceId
            )

            if (mediaResult.mediaSources.isNullOrEmpty()) {
                DiagLog.w(context, "playerNoSources", "mediaId=$mediaId PlaybackInfo 无 MediaSources")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    // 系统 Toast 在电视/头显上经常看不见；改成画面上写人话，不加重试按钮
                    hudText = playbackInfoFailText
                }
                return@LaunchedEffect
            }

            // 直接使用 mediaResult 对象，赋值给状态
            media = mediaResult
            val mediaInfoResult = repository.getMediaInfo(mediaId)
            mediaInfo = mediaInfoResult

            // 更新收藏状态
            isFavorite = mediaInfoResult.userData?.isFavorite == true

            val source = mediaResult.mediaSources.firstOrNull()
            val streams = source?.mediaStreams ?: emptyList()
            /*
             * 字幕菜单里不列弹幕文件（父亲 2026-10-29）。
             * 我们自己的弹幕是外挂 ASS，文件名/标题带「弹幕」（早期也有 danmaku / danmu 写法）；
             * 它走专门的弹幕菜单，混进字幕列表只会让人选错。
             */
            subtitleTracks = streams.filter { s ->
                s.type == "Subtitle" && !isDanmakuSubtitle(s)
            }

            audioTracks = streams.filter { s ->
                s.type == "Audio"
            }

            /*
             * 音轨 / 字幕的初始选择：优先用详情页在这一版上选过的（VR 同名键
             * aud_choice_<条目>_<版本>、sub_choice_<条目>_<版本>，"序号|是否图形字幕"），
             * 没选过才落回服务端的默认轨。
             */
            val savedAud = savedVersionSourceId
                ?.let { preferencesManager.getInt("aud_choice" + "_" + mediaId + "_" + it, -1) }
                ?: -1
            val savedSub = savedVersionSourceId?.let {
                preferencesManager.getString("sub_choice" + "_" + mediaId + "_" + it)
                    ?.substringBefore('|')?.toIntOrNull()
            } ?: -1
            if (selectedAudioIndex == -1) {
                selectedAudioIndex = savedAud.takeIf { v -> streams.any { it.type == "Audio" && it.index == v } }
                    ?: source?.defaultAudioStreamIndex ?: -1
            }
            if (selectedSubtitleIndex == -99) {
                selectedSubtitleIndex = savedSub.takeIf { v -> streams.any { it.type == "Subtitle" && it.index == v } }
                    ?: source?.defaultSubtitleStreamIndex ?: -1
            }

            // 构建 URL (参考 Flutter 逻辑)
            var path = source?.directStreamUrl

            // 如果强制转码(correction=1) 或者 没有直链(path=null)，则尝试使用转码链接
            if (playbackCorrection == 1 || path == null) {
                val transcodeUrl = source?.transcodingUrl
                if (transcodeUrl != null) {
                    path = transcodeUrl
                }
            }

            // 音频条目两处修正（2026-09-27 实测 虚拟.wav）：
            // 1) Emby 给的 DirectStreamUrl 形如 /audio/{id}/stream?...（不带 static），
            //    直接拉会 500；必须补 static=true → 206 原样直出。
            // 2) 若两类 URL 都没返回（音乐条目常见），自己拼 /Audio/{id}/stream.{容器}?static=true&api_key=...。
            if (mediaInfoResult.type.equals("Audio", ignoreCase = true)) {
                if (path == null) {
                    val ext = source?.container?.takeIf { it.isNotBlank() } ?: "mp3"
                    path = "/Audio/$mediaId/stream.$ext?static=true&api_key=$apiKey"
                    Log.i("PlayerScreen", "音频直链回退(自拼): $path")
                } else if (!path.contains("static=")) {
                    path = path + (if (path.contains("?")) "&" else "?") + "static=true"
                    Log.i("PlayerScreen", "音频直链补 static=true: $path")
                }
            }

            videoUrl = if (path != null) "${serverUrl}/emby$path" else null
            DiagLog.w(context, "playerUrl",
                "mediaId=$mediaId type=${mediaInfoResult.type} container=${source?.container} " +
                "directStream=${source?.directStreamUrl != null} transcode=${source?.transcodingUrl != null} " +
                "url=${videoUrl?.take(220)}")
            hasReportedPlaying = false
        } catch (e: Throwable) {
            Log.e("PlayerScreen", "加载播放信息失败", e)
            DiagLog.w(context, "playerUrl异常", "mediaId=$mediaId ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /*
     * 弹幕层：不再读本地 ASS、也不占 Emby 字幕轨，改成向 .15 的现拉服务要一份
     *（父亲 2026-10-28 口径，服务由乐乐维护）。拉不到就当没有弹幕，绝不影响出画面。
     */
    /*
     * 键要带上片名与集号：mediaId 一到就开跑，但详情可能还没回来，
     * 那时剧名是空的、一条请求都不会发（父亲 2026-10-29 实测服务端零请求）。
     * 现在详情一变就重试，并且剧名还空着时按秒退重试几轮。
     */
    LaunchedEffect(
        danmakuEnabled, mediaId,
        mediaInfo.name, mediaInfo.seriesName,
        mediaInfo.indexNumber, mediaInfo.parentIndexNumber, mediaInfo.productionYear
    ) {
        if (!danmakuEnabled) {
            danmakuTrack = null
            return@LaunchedEffect
        }
        /*
         * 剧名/集号的取法（父亲 2026-10-29 报「TV 版弹幕没接进来」）。
         *
         * 服务端日志实证：剧集条目缺「所属剧集」时，我们直接把整条文件名当剧名发过去
         *（`小镇良方 第11集 一起挡过屎的日子&ep=2147483647`），服务端当然 404。
         * 现在从名字里剥出「第N集/话/期」：前面那段当剧名、N 当集号，集号夹在 0..2000。
         */
        val rawName = mediaInfo.name ?: ""
        val epMark = Regex("第\\s*(\\d{1,4})\\s*[集话期回]").find(rawName)
        var title = mediaInfo.seriesName.orEmpty()
        var ep = when {
            mediaInfo.indexNumber == null || mediaInfo.indexNumber == Int.MAX_VALUE -> null
            else -> mediaInfo.indexNumber
        }
        if (title.isBlank()) {
            title = if (epMark != null) rawName.substring(0, epMark.range.first).trim() else rawName
            if (ep == null) ep = epMark?.groupValues?.get(1)?.toIntOrNull()
        }
        // 详情还没到：等它（最多 6 秒），别把"还没加载"当成"没有弹幕"
        var waited = 0
        while (title.isBlank() && waited < 6000) {
            kotlinx.coroutines.delay(500)
            waited += 500
            title = mediaInfo.seriesName.orEmpty()
            val mark2 = Regex("第\\s*(\\d{1,4})\\s*[集话期回]").find(mediaInfo.name ?: "")
            if (title.isBlank()) {
                title = if (mark2 != null) (mediaInfo.name ?: "").substring(0, mark2.range.first).trim()
                else (mediaInfo.name ?: "")
            }
            if (ep == null) ep = mark2?.groupValues?.get(1)?.toIntOrNull()
        }
        if (title.isBlank()) {
            Log.w("PlayerScreen", "弹幕跳过：片名还没加载出来")
            danmakuTrack = null
            return@LaunchedEffect
        }
        val isEpisode = epMark != null || !mediaInfo.seriesName.isNullOrBlank()
        val kind = if (isEpisode) "tv" else "movie"
        ep = if (isEpisode) (ep ?: 1).coerceIn(0, 2000) else 0
        val year = mediaInfo.productionYear
        val season = if (isEpisode) mediaInfo.parentIndexNumber else null
        val fetched = withContext(Dispatchers.IO) {
            runCatching { LiveDanmakuApi.fetch(title, ep, year, kind, season) }.getOrNull()
        }
        danmakuTrack = fetched
        if (fetched != null) {
            Log.i("PlayerScreen", "弹幕层加载完成: " + fetched.items.size + " 条（"
                    + title + " 第" + ep + "集）")
        } else {
            Log.w("PlayerScreen", "弹幕没取到：" + title + " ep=" + ep + " kind=" + kind)
        }
    }

    // 设置 MediaItem 和 字幕
    LaunchedEffect(videoUrl) {
        if (videoUrl != null) {
            val source = media.mediaSources?.firstOrNull()
            val mediaSourceId = source?.id ?: ""

            // 使用 SubtitleConfigBuilder 构建字幕配置
            val subtitleConfigs = SubtitleConfigBuilder.buildSubtitleConfigs(
                subtitleTracks = subtitleTracks,
                serverUrl = serverUrl,
                mediaId = mediaId,
                mediaSourceId = mediaSourceId,
                apiKey = apiKey,
                selectedSubtitleIndex = selectedSubtitleIndex
            )

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(videoUrl)
                .setSubtitleConfigurations(subtitleConfigs)

           // 计算起始位置：优先级为播放进度 > 参数传入位置 > 0
            val startPositionMs = if (position > 0) {
                position
            } else if (playbackPositionTicks > 0) {
                playbackPositionTicks / 10000
            } else {
                0L
            }

            val mediaItem = mediaItemBuilder.build()

            // 直接在setMediaItem中设置跳转位置（Media3 API支持）
            player.setMediaItem(mediaItem, startPositionMs)

            if (startPositionMs > 0) {
                Log.d("Player", "Setting initial position via setMediaItem: $startPositionMs ms")
            }

            player.prepare()
            player.playWhenReady = true
        }
    }

    // 更新选中字幕 - 使用 PlayerTrackManager
    LaunchedEffect(selectedSubtitleIndex, subtitleTracks, currentTracks) {
        PlayerTrackManager.selectSubtitle(
            player,
            subtitleTracks,
            selectedSubtitleIndex,
            currentTracks
        )
    }

    // 更新选中音频 - 使用 PlayerTrackManager
    LaunchedEffect(selectedAudioIndex, audioTracks, currentTracks) {
        PlayerTrackManager.selectAudio(player, audioTracks, selectedAudioIndex, currentTracks)
    }


    // Handle Ended Logic for List Loop
    LaunchedEffect(endedHandled) {
        if (endedHandled && playMode == 0) {
            val seriesId = mediaInfo.seriesId
            if (seriesId != null) {
                try {
                    val list = repository.getSeriesList(seriesId)

                    @Suppress("UNCHECKED_CAST")
                    val episodes = list

                    val currentId = mediaId
                    val currentIndex = episodes.indexOfFirst { it.id == currentId }

                    if (currentIndex >= 0 && currentIndex < episodes.size - 1) {
                        val nextEpisode = episodes[currentIndex + 1]
                        onNavigateToPlayer(nextEpisode)
                    } else {

                    }
                } catch (e: Exception) {
                    ErrorHandler.logError("PlayerScreen", "操作失败", e)

                }
            } else {

            }
        }
    }

    // Session Reporting & Updates - 使用 playerViewModel 定期报告进度
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var tickCount = 0
            while (isActive) {
                try {
                    // 每 9 秒报告一次进度
                    if (tickCount % 9 == 0) {
                        playerViewModel.reportProgress(
                            mediaId = mediaId,
                            media = media,
                            position = position,
                            selectedSubtitleIndex = selectedSubtitleIndex,
                            selectedAudioIndex = selectedAudioIndex
                        )
                    }
                    tickCount++
                } catch (e: Exception) {
                    ErrorHandler.logError("PlayerScreen", "报告进度失败", e)
                }
                delay(1000)
            }
        }
    }

    // Initial Playing Report - 使用 playerViewModel 报告播放开始
    LaunchedEffect(isPlaying, videoUrl) {
        if (isPlaying && videoUrl != null && !hasReportedPlaying) {
            try {
                playerViewModel.reportPlaying(
                    mediaId = mediaId,
                    media = media,
                    position = position,
                    selectedSubtitleIndex = selectedSubtitleIndex,
                    selectedAudioIndex = selectedAudioIndex,
                    playbackRate = playbackSpeed
                )
                hasReportedPlaying = true
            } catch (e: Exception) {
                ErrorHandler.logError("PlayerScreen", "报告播放开始失败", e)
            }
        }
    }

    // Load session once after reporting playing
    // Load session once after reporting playing (with retry)
    LaunchedEffect(hasReportedPlaying, media.playSessionId) {
        if (!hasReportedPlaying) return@LaunchedEffect

        val currentId = media.playSessionId ?: return@LaunchedEffect
        val retries = 4

        repeat(retries) { attempt ->
            try {
                // Delay waiting for server to process playing report (500ms initial + retry interval)
                delay(1200)

                val sessions = repository.getPlayingSessions()
                val source =
                    media.mediaSources?.firstOrNull()
                val mediaSourceId = source?.id

                val found = sessions
                    .find { s ->
                        // Match by NowPlayingItem.Id
                        val nowPlayingId = s.nowPlayingItem?.id
                        if (nowPlayingId == mediaId || (mediaSourceId != null && nowPlayingId == mediaSourceId)) {
                            return@find true
                        }
                        false
                    }

                if (found != null) {
                    // 检查是否需要继续等待转码信息
                    val playMethod = found.playState?.playMethod
                    val transcodingInfo = found.transcodingInfo

                    if (playMethod == "Transcode" && transcodingInfo == null) {
                        android.util.Log.d(
                            "PlayerSession",
                            "Found session but transcodingInfo is null for Transcode mode, retrying..."
                        )
                        session = null  // 清空，继续重试
                    } else {

                        session = found
                        android.util.Log.d(
                            "PlayerSession",
                            "Matched session on attempt ${5 - retries}, playMethod=$playMethod"
                        )

                        return@LaunchedEffect
                    }
                }


            } catch (e: Exception) {
                ErrorHandler.logError("PlayerScreen", "操作失败", e)

            }
            // 如果运行到这里，说明本次没找到，repeat 会继续下一次
            if (attempt == retries - 1) {
                Log.w("Session", "达到最大重试次数，未能找到 Session: $currentId")
            }
        }
    }


    // 使用 LaunchedEffect 监听 player 实例
    // 当 player 变化或 Composable 销毁时，这个协程会自动重启或取消
    LaunchedEffect(player) {
        while (true) {
            if (player.isPlaying || player.playbackState == Player.STATE_BUFFERING) {
                // 1. 更新当前播放位置
                val rawPosition = player.currentPosition
                position = if (rawPosition > 0) rawPosition else 0L

                // 2. 更新缓存进度
                buffered = player.bufferedPosition.coerceAtLeast(0L)

            }

            // 每 800 毫秒更新一次进度
            delay(800)
        }
    }

    // 缓冲卡死看门狗：长时间缓冲且缓冲进度零增长时，若当前播放器存在音频输出错误则触发自动恢复；
    // 同时把停滞的网速显示归零，避免一直显示冻结的加载速度
    LaunchedEffect(player) {
        var lastBufferedPosition = -1L
        var lastChangeTime = System.currentTimeMillis()
        while (isActive) {
            delay(1000)
            val p = player
            val currentBuffered = p.bufferedPosition
            if (currentBuffered != lastBufferedPosition) {
                lastBufferedPosition = currentBuffered
                lastChangeTime = System.currentTimeMillis()
            } else {
                downloadSpeed = 0
                if (p.playbackState == Player.STATE_BUFFERING && !p.isPlaying &&
                    currentBuffered > 0 &&
                    System.currentTimeMillis() - lastChangeTime >= 15_000 &&
                    currentPlayerAudioError && audioRecoveryStage < 2
                ) {
                    Log.w(AUDIO_RECOVERY_TAG, "检测到音频故障导致的缓冲卡死，触发自动恢复")
                    handleAudioSinkFailure()
                    lastChangeTime = System.currentTimeMillis()
                }
            }
        }
    }

    // 播放器监听
    DisposableEffect(player) {
        val p = player
        currentPlayerAudioError = false
        p.addAnalyticsListener(object : AnalyticsListener {

            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedMs: Long,
                initializationDurationMs: Long,
            ) {
                currentVideoDecoderName = decoderName
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedMs: Long,
                initializationDurationMs: Long,
            ) {
                Log.i("DecoderInfo", "音频解码器已初始化: $decoderName")
            }

            override fun onAudioSinkError(
                eventTime: AnalyticsListener.EventTime,
                audioSinkException: Exception,
            ) {
                Log.e(
                    AUDIO_RECOVERY_TAG,
                    "音频输出错误(stage=$audioRecoveryStage): ${audioSinkException.message}"
                )
                currentPlayerAudioError = true
                handleAudioSinkFailure()
            }
        })

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                onPlaybackStateChanged(playing)
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) {
                    val durationMs = p.duration // 此时时长已可用
                    if (durationMs != C.TIME_UNSET) {
                        // 执行逻辑
                        duration = durationMs
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                DiagLog.w(context, "playerState", "state=$state (1=IDLE 2=BUFFERING 3=READY 4=ENDED)")
                if (state == Player.STATE_BUFFERING) {
                    isBuffering = true
                } else if (state == Player.STATE_READY) {
                    isBuffering = false
                    // 在STATE_READY时获取准确时长
                    val rawDuration = p.duration
                    if (rawDuration > 0) {
                        duration = rawDuration
                        Log.d("Player", "Duration updated from READY state: $duration ms")
                    }
                } else if (state == Player.STATE_IDLE) {
                    // 出错/停止后的空闲态：清掉缓冲指示，避免永远转圈
                    isBuffering = false
                }


                if (state == Player.STATE_ENDED) {
                    isBuffering = false
                    onPlaybackStateChanged(false)
                    // Handle loop logic
                    if (playMode == 1) { // Single Loop
                        p.seekTo(0)
                        p.play()
                    } else if (playMode == 0) { // List Loop

                        val seriesId = mediaInfo.seriesId

                        if (seriesId != null) {
                            // Trigger next episode logic
                            // ...
                            if (!endedHandled) {
                                endedHandled = true
                                // Trigger logic handled in LaunchedEffect
                            }
                        } else {
                            // 播放结束，发送停止报告
                            playerViewModel.reportStopped(
                                mediaId = mediaId,
                                media = media,
                                position = position,
                                selectedSubtitleIndex = selectedSubtitleIndex,
                                selectedAudioIndex = selectedAudioIndex,
                                playbackRate = playbackSpeed
                            )

                        }
                    } else {
                        // 播放结束，发送停止报告
                        playerViewModel.reportStopped(
                            mediaId = mediaId,
                            media = media,
                            position = position,
                            selectedSubtitleIndex = selectedSubtitleIndex,
                            selectedAudioIndex = selectedAudioIndex,
                            playbackRate = playbackSpeed
                        )

                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlayerScreen", "播放器错误: ${error.message}", error)
                DiagLog.w(context, "playerError",
                    "${error.errorCodeName}: ${error.message} | cause=${error.cause?.javaClass?.simpleName}: ${error.cause?.message}")

                val causeMsg = error.cause?.message ?: ""
                if (causeMsg.contains("SOCKS", ignoreCase = true) ||
                    causeMsg.contains("Proxy", ignoreCase = true) ||
                    causeMsg.contains("Connection refused", ignoreCase = true) ||
                    causeMsg.contains("Malformed reply", ignoreCase = true)
                ) {
                    scope.launch {
                        hudText = context.getString(R.string.error_proxy_connection)
                    }
                    return
                }
                // 音频输出类错误 → 交给音频自动恢复（安全模式重建/无声降级），不消耗一次性的转码回退
                val isAudioTrackError =
                    error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_INIT_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_OFFLOAD_WRITE_FAILED ||
                        generateSequence<Throwable>(error.cause) { it.cause }.any {
                            it is AudioSink.InitializationException ||
                                it is AudioSink.ConfigurationException ||
                                it is AudioSink.WriteException
                        }
                if (isAudioTrackError) {
                    Log.w(
                        AUDIO_RECOVERY_TAG,
                        "播放错误为音频输出类(code=${error.errorCode})，触发音频自动恢复"
                    )
                    handleAudioSinkFailure()
                    return
                }
                /*
                 * 出错要让画面说人话（父亲 2026-10-29 报"黑屏/转圈，什么原因都不说"）。
                 * 第一次出错会换一路重试，第二次直接停：两种情况各给一句中文，不再只写日志。
                 * 不含技术名词，也不放"重试"按钮。
                 */
                val reason = when {
                    causeMsg.isNotBlank() -> causeMsg
                    !error.message.isNullOrBlank() -> error.message.orEmpty()
                    else -> error.errorCodeName
                }
                val canRetry = fallbackAttempts < 2
                hudText = if (canRetry) {
                    "这一段没取到，正在换一种播放方式重试（" + reason.take(40) + "）"
                } else {
                    "起播失败：" + reason.take(60)
                }
                if (canRetry) {
                    fallbackAttempts++
                    fallbackToServerTranscode(force = true)
                }
            }
        }

        val bandwidthListener = object : BandwidthMeter.EventListener {
            override fun onBandwidthSample(
                elapsedMs: Int,
                bytesTransferred: Long,
                bitrateEstimate: Long,
            ) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSpeedUpdate >= 1000) {
                    downloadSpeed = bitrateEstimate
                    lastSpeedUpdate = currentTime
                }
            }
        }

        bandwidthMeter.addEventListener(
            Handler(Looper.getMainLooper()),
            bandwidthListener
        )
        p.addListener(listener)

        onDispose {
            bandwidthMeter.removeEventListener(bandwidthListener)
            p.stop()
            p.removeListener(listener)
            p.setVideoSurface(null)
            p.release()
        }
    }

    // 切出应用(回主页/切后台)立即暂停:官方 TV 质量清单要求视频应用切出即暂停
    val hostActivity = LocalContext.current as? android.app.Activity
    DisposableEffect(hostActivity) {
        val callbacks = object : android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityStopped(activity: android.app.Activity) {
                if (activity === hostActivity) runCatching { player.pause() }
            }

            override fun onActivityCreated(
                activity: android.app.Activity,
                savedInstanceState: android.os.Bundle?
            ) {
            }

            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(
                activity: android.app.Activity,
                outState: android.os.Bundle
            ) {
            }

            override fun onActivityDestroyed(activity: android.app.Activity) {}
        }
        hostActivity?.application?.registerActivityLifecycleCallbacks(callbacks)
        onDispose { hostActivity?.application?.unregisterActivityLifecycleCallbacks(callbacks) }
    }

    // 退出播放页时上报停止（与播放器实例重建解耦，音频自动恢复重建时不会误报）
    DisposableEffect(Unit) {
        onDispose {
            playerViewModel.reportStopped(
                mediaId = mediaId,
                media = media,
                position = position,
                selectedSubtitleIndex = selectedSubtitleIndex,
                selectedAudioIndex = selectedAudioIndex,
                playbackRate = playbackSpeed
            )
        }
    }

    // 播放页根容器的焦点：这一页所有按键都从它往下分发
    val playerRootFocus = remember { FocusRequester() }

    /*
     * 收起面板 / 关闭二级菜单的统一出口：先把焦点交回根容器，再让控件从组合里消失。
     *
     * 顺序不能反。持有焦点的按钮一旦先被移除，Compose 只会把焦点清空、不会交还父容器，
     * 之后整页按键就没人接（父亲 2026-10-31 反复遇到"控制条唤不出、只能退出重进"）。
     * 所有收起动作走同一条路径，就不需要定时轮询兜底。
     */
    val collapseThen: (() -> Unit) -> Unit = { collapse ->
        runCatching { playerRootFocus.requestFocus() }
        collapse()
    }

    /*
     * 返回键：播放页无条件接管。
     *
     * 原先把 enabled 挂成「面板/菜单开着」，都关着就把返回交还系统；实测遥控器按返回
     * 画面毫无反应、也退不出去（父亲 2026-10-31）。播放页是应用内的界面状态而不是独立页面，
     * 系统那条路不一定把它收掉，所以最后一层必须由这里显式 onExit()。
     */
    androidx.activity.compose.BackHandler(enabled = true) {
        com.xxxx.emby_tv.util.DiagLog.w(context, "backKey", "BackHandler 收到返回 面板=" + showPanel +
            " 二级=" + activeItem + " 更多=" + inMoreMenu)
        // 防连按;若上面的 onKeyEvent 刚处理过这次返回就跳过(避免一次退两层)
        val now = System.currentTimeMillis()
        if (now - lastBackAt < 300) return@BackHandler
        lastBackAt = now
        when {
            activeItem != null -> collapseThen { activeItem = null }
            inMoreMenu -> collapseThen { inMoreMenu = false }
            showPanel -> collapseThen { showPanel = false }
            else -> onExit()
        }
    }

    // 监听按键显示菜单
    val focusRequester = remember { FocusRequester() }
    // 进页面就把焦点放到根容器
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(120); runCatching { playerRootFocus.requestFocus() } }

    // UI 结构 - 最外层纯黑背景
    // 左上角片名 Logo 的地址：剧集/单集用所属剧集的 ClearLogo，电影用条目自己的 Logo。
    // 父亲 2026-10-02：这个 logo 要**一直显示**（原来只在按 ↓ 唤出控制面板时才画）。
    val playerLogoUrl = if (serverUrl.isEmpty()) null else {
        val sid = mediaInfo.seriesId
        val ownLogoTag = mediaInfo.imageTags?.get("Logo")
        when {
            !sid.isNullOrEmpty() -> "$serverUrl/emby/Items/$sid/Images/Logo?maxHeight=200"
            !ownLogoTag.isNullOrEmpty() ->
                "$serverUrl/emby/Items/${mediaInfo.id}/Images/Logo?maxHeight=200&tag=$ownLogoTag"
            else -> null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(enabled = true, onClick = {
                    if (isPlaying) {
                        player.pause()
                        isShowInfo = true
                    } else {
                        player.play()
                        isShowInfo = false
                    }
                })
                // 二级菜单打开时禁止左右键:Preview 阶段(父→子)先吃掉,
                // 否则横向列表/滚动容器会先消费掉,变成焦点乱跳;
                // 一级控制条内仍交给焦点系统左右移动;演职人员是横排头像卡片,保留左右
                // 根容器自身可聚焦：手柄的方向键与确认键必须有落点才能进到下面的事件处理，
                // 播放页没有可聚焦控件时按键会直接丢掉（父亲 2026-10-28 实测「手柄完全没反应」）
                .focusRequester(playerRootFocus)
                .focusable()
                /*
                 * 焦点自愈（父亲 2026-10-31：控制条自动收起后整页按键失灵，只能退出重进）。
                 * 收起时持有焦点的按钮从树里消失，Compose 不会把焦点交还父容器，
                 * 之后没有任何节点在焦点路径上 → 方向键再也到不了播放页。
                 * 这里在「整棵子树都没焦点」时补一次请求；成功即 hasFocus=true，计数归零，不会反复抢。
                 */
                .onFocusChanged {
                    com.xxxx.emby_tv.util.DiagLog.w(
                        context, "focusRoot",
                        "播放页根容器 hasFocus=" + it.hasFocus + " 面板=" + showPanel +
                            " 二级=" + activeItem + " 更多=" + inMoreMenu
                    )
                    com.xxxx.emby_tv.util.InputBridge.playerRootHasFocus = it.hasFocus
                }
                .onPreviewKeyEvent { event ->
                    /*
                     * 光点决定语义（父亲 2026-10-28）：
                     *   光点在控制条按钮上 → 摇杆左右移焦点、扣扳机触发按钮；
                     *   光点在播放画面 → 摇杆左右快退快进、扣扳机暂停/播放。
                     * 判定依据是坐标表：落点命中已登记控件（且面板开着）就算在控制条上。
                     * 菜单（二级面板）打开时，摇杆与扳机只服务菜单，不快进也不暂停。
                    */
                    val overBar = showPanel &&
                        com.xxxx.emby_tv.util.PointerTargets.cursorActive() &&
                        com.xxxx.emby_tv.util.PointerTargets.findAt(
                            com.xxxx.emby_tv.util.PointerTargets.cursorX,
                            com.xxxx.emby_tv.util.PointerTargets.cursorY
                        ) != null
                    val secondaryOpen = inMoreMenu ||
                            (activeItem != null && activeItem != PlayerMenuItem.CAST)
                    if (secondaryOpen &&
                        (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                    ) {
                        return@onPreviewKeyEvent true
                    }
                    /*
                     * 摇杆左右永远快退快进（父亲 2026-10-28）：控制条展开时也一样，
                     * 按钮之间的焦点移动只靠光点，不再用左右键。
                     * 这里提前吃掉左右键，否则横排按钮会先把它们当焦点导航用掉。
                     */
                    /*
                     * 演职人员面板开着时，左右键归那一排头像卡片滚动，不许拿去快退快进
                     * （父亲 2026-10-29 00:07：往回滚的时候偶尔把快退快进也带出来了）。
                     * 原来这个分支只排除"二级菜单"，演职人员被特意留了下来，于是抢在同一排
                     * 卡片之前吃键，谁先吃到取决于当下焦点落在哪张卡上，表现就是"有时滚、有时快进"。
                     */
                    val castRowOpen = activeItem == PlayerMenuItem.CAST
                    if (!secondaryOpen && !castRowOpen &&
                        com.xxxx.emby_tv.util.PointerTargets.cursorActive() &&
                        (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                    ) {
                        if (event.type == KeyEventType.KeyDown) {
                            seekHud = true
                            seekHudAt = System.currentTimeMillis()
                            if (event.key == Key.DirectionLeft) {
                                seekHudForward = false
                                player.seekBack()
                            } else {
                                seekHudForward = true
                                player.seekForward()
                            }
                            interactionTick++
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (event.type == KeyEventType.KeyDown && !secondaryOpen) {
                        when (event.key) {
                            // 菜单开着时扳机只给菜单，不暂停/播放
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                /*
                                 * 焦点在控制条按钮上时，确认键只交给按钮（展开菜单），不再切播放/暂停。
                                 * 原判据 only 看光标落点，遥控器没光标 → 永远判"不在这根条上"，
                                 * 于是每按一次确认都顺带暂停（父亲 2026-10-31 实测：所有按钮都有）。
                                 */
                                val focusOnBar = com.xxxx.emby_tv.util.InputBridge.playerFocusInPanel
                                if (!overBar && !focusOnBar && activeItem == null && !inMoreMenu) {
                                    if (isPlaying) player.pause() else player.play()
                                    interactionTick++
                                    return@onPreviewKeyEvent true
                                }
                            }
                            // 摇杆往下唤出控制条（父亲 2026-10-28）
                            Key.DirectionDown -> {
                                if (!showPanel && activeItem == null && !inMoreMenu) {
                                    showPanel = true
                                    interactionTick++
                                    return@onPreviewKeyEvent true
                                }
                            }
                            // 摇杆往上不做任何动作（父亲 2026-10-29：收面板改按 B/返回键）
                            Key.DirectionUp -> {
                                if (!showPanel) return@onPreviewKeyEvent true
                            }
                            else -> {}
                        }
                    }
                    false
                }
                .onKeyEvent { event ->
                    // 任何按键(含左右键)都重置自动收起的计时
                    if (event.type == KeyEventType.KeyDown) interactionTick++

                    if (event.key == Key.Back || event.key == Key.Escape) {
                        // 没有层级可收时必须放行给系统(KeyDown 与 KeyUp 都要放行):
                        // 连 KeyUp 也吃掉的话系统收不到返回,播放页就退不回上一页了
                        if (activeItem == null && !inMoreMenu && !showPanel) return@onKeyEvent false
                        // 有层级时:只在按下时处理,松开消费掉但不重复处理(否则一次按键退两层)
                        if (event.type == KeyEventType.KeyUp) return@onKeyEvent true
                        // 长按/连按返回会连发 KeyDown:300ms 内的第二下忽略
                        val now = System.currentTimeMillis()
                        if (now - lastBackAt < 300) return@onKeyEvent true
                        lastBackAt = now
                        if (activeItem != null) {
                            collapseThen { activeItem = null }
                            return@onKeyEvent true
                        }
                        if (inMoreMenu) {
                            collapseThen { inMoreMenu = false }
                            return@onKeyEvent true
                        }
                        collapseThen { showPanel = false }
                        return@onKeyEvent true
                    }
                    // 左右键:控制条未展开时快退/快进,并唤出进度条(seekHud);
                    // 一级控制条展开时交给焦点系统在条内移动焦点(二级菜单已被 Preview 拦掉)
                    if (event.key == Key.DirectionLeft) {
                        if (activeItem != null || inMoreMenu) return@onKeyEvent false
                        /*
                         * 没有光点时（电视遥控器），控制条展开着就把左右键让给焦点系统，
                         * 否则永远只能在按钮之间"快退快进"，选不中左边的按钮。
                         * 这段一直只写了注释没写判据（父亲 2026-10-29 报）。
                         */
                        if (showPanel && !com.xxxx.emby_tv.util.PointerTargets.cursorActive()) {
                            return@onKeyEvent false
                        }
                        if (event.type == KeyEventType.KeyDown) {
                            // 每一次按键(含长按产生的重复 KeyDown)都重置进度条的隐退计时:
                            // 原先只在 leftKeyDownTime==0 时重置,长按期间不重置 → 用户还在操作,
                            // 进度条却已到 5 秒自己隐退(父亲 2026-09-30 实测要求"还在操作就不要隐退")
                            seekHudForward = false
                            seekHud = true
                            seekHudAt = System.currentTimeMillis()
                            if (leftKeyDownTime == 0L) {
                                leftKeyDownTime = System.currentTimeMillis()
                            }
                        } else if (event.type == KeyEventType.KeyUp) {
                            if (leftKeyDownTime > 0) {
                                if (System.currentTimeMillis() - leftKeyDownTime < 500) {
                                    player.seekBack()
                                }
                                leftKeyDownTime = 0L
                            }
                        }
                        return@onKeyEvent true
                    }
                    if (event.key == Key.DirectionRight) {
                        if (activeItem != null || inMoreMenu) return@onKeyEvent false
                        // 同上：遥控器展开控制条时左右键归焦点系统
                        if (showPanel && !com.xxxx.emby_tv.util.PointerTargets.cursorActive()) {
                            return@onKeyEvent false
                        }
                        if (event.type == KeyEventType.KeyDown) {
                            // 同左键:每次按键都重置隐退计时(父亲 2026-09-30 要求)
                            seekHudForward = true
                            seekHud = true
                            seekHudAt = System.currentTimeMillis()
                            if (rightKeyDownTime == 0L) {
                                rightKeyDownTime = System.currentTimeMillis()
                            }
                        } else if (event.type == KeyEventType.KeyUp) {
                            if (rightKeyDownTime > 0) {
                                if (System.currentTimeMillis() - rightKeyDownTime < 500) {
                                    player.seekForward()
                                }
                                rightKeyDownTime = 0L
                            }
                        }
                        return@onKeyEvent true
                    }

                    if (event.type == KeyEventType.KeyDown) {
                        // 遥控器/蓝牙遥控上的播放暂停键:官方 TV-PP 要求必须能切换状态
                        if (event.key == Key.MediaPlayPause) {
                            seekHud = false
                            if (isPlaying) player.pause() else player.play()
                            return@onKeyEvent true
                        }
                        // Menu 键:开关面板
                        if (event.key == Key.Menu) {
                            if (activeItem != null) {
                                collapseThen { activeItem = null }
                            } else if (showPanel) {
                                collapseThen {
                                    showPanel = false
                                    inMoreMenu = false
                                }
                            } else {
                                seekHud = false
                                showPanel = true
                            }
                            return@onKeyEvent true
                        }
                        // 面板显示时,上下键全部交给焦点系统(二级菜单里才能移动焦点)
                        if (event.key == Key.DirectionDown || event.key == Key.DirectionUp) {
                            if (activeItem != null || inMoreMenu) return@onKeyEvent false
                            seekHud = false
                            if (event.key == Key.DirectionDown) {
                                showPanel = true
                                inMoreMenu = false
                                activeItem = null
                                return@onKeyEvent true
                            }
                            // 上键：不接音量、也不收面板（父亲 2026-10-29 明确摇杆往上不做动作）；
                            // 面板开着时交给焦点系统，在按钮行里往上走
                            if (showPanel) return@onKeyEvent false
                            return@onKeyEvent true
                        }
                        if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                            if (activeItem != null || inMoreMenu) return@onKeyEvent false
                            seekHud = false
                            if (isPlaying) {
                                player.pause()
                                isShowInfo = true
                            } else {
                                player.play()
                                isShowInfo = false
                            }
                            return@onKeyEvent true
                        }
                        if (showPanel) return@onKeyEvent true
                        // Show info on any key
                        isShowInfo = true
                        // Hide info after delay?
                    }
                    false
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            // 1. Video Layer
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false // Use custom overlay
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // 内置字幕视图改为由下方 overlay SubtitleView 渲染（支持时间偏移）
                        subtitleView?.visibility = View.GONE
                    }
                },
                update = { view ->
                    view.player = player
                },
//                modifier = Modifier.fillMaxSize()
            )

            // 1.5 Subtitle Layer - 接管字幕渲染，支持时间偏移
            AndroidView(
                factory = { ctx ->
                    SubtitleView(ctx).apply {
                        // 1. 允许应用字幕文件内置的样式（颜色、字体、定位等）
                        setApplyEmbeddedStyles(true)

                        // 2. 允许应用字幕文件内置的字体大小
                        setApplyEmbeddedFontSizes(true)

                        // 3. 关键：将渲染模式设为 BITMAP（位图模式）
                        // 只有在这种模式下，复杂的 ASS/SSA 特效和 PGS 图形字幕才能精准还原
                        // 默认的层次模式（VIEW_TYPE_TEXT）会丢失很多高级特效
                        // VIEW_TYPE_CANVAS = 2
                        setViewType(SubtitleView.VIEW_TYPE_CANVAS)

                        // 4. 强制设置透明背景，避免默认样式的黑色背景遮挡
                        val transparentStyle = CaptionStyleCompat(
                            android.graphics.Color.WHITE,
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_NONE,
                            android.graphics.Color.WHITE,
                            null
                        )
                        setStyle(transparentStyle)

                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        overlaySubtitleView.value = this
                    }
                },
                update = { view ->
                    view.setBottomPaddingFraction(
                        subtitleBottomPadding * (1f - SUBTITLE_TOP_RESERVED_FRACTION)
                    )
                    // 弹幕层生效时让普通字幕层让位,避免两套渲染叠在一起
                    view.visibility =
                        if (danmakuEnabled && danmakuTrack != null) View.GONE else View.VISIBLE
                },
                modifier = Modifier.fillMaxSize()
            )

            // 1.6 弹幕层 - 独立一层,逐帧按播放时间计算位置(不抖)
            AndroidView(
                factory = { ctx ->
                    DanmakuView(ctx).apply {
                        // 弹幕跟字幕偏移一起动（字幕时间轴一动，弹幕一起动），
                        // 再叠加弹幕自己的偏移：正数=提前，所以加。
                        setPositionProvider {
                            player.currentPosition - subtitleTimeOffsetMs + danmakuOffsetMs
                        }
                        danmakuViewRef.value = this
                        start()
                    }
                },
                update = { v ->
                    v.userScale = danmakuScale
                    v.setTrack(if (danmakuEnabled) danmakuTrack else null)
                },
                modifier = Modifier.fillMaxSize()
            )

            // 1.7 右上角实时网速(暂停/无流量时自动消失)
            if (showSpeed) {
                val speedText = Utils.formatBandwidth(downloadSpeed)
                if (speedText.isNotEmpty()) {
                    Text(
                        text = speedText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 18.dp, end = 22.dp)
                            .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }


            // 1.85 左上角片名 Logo：常显（不随控制面板显隐；父亲 2026-10-02）
            // 父亲 2026-10-03：只卡宽度会让「又高又窄」的 ClearLogo 竖着占半个屏。
            // 改为把 140x60dp 的框钉死，再让图按比例缩放进框内（Fit + 左对齐）。
            playerLogoUrl?.let { url ->
                coil3.compose.AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 48.dp, top = 30.dp)
                        .size(width = 140.dp, height = 60.dp)
                )
            }

            // 1.9 左右键快进快退唤出的进度条(未按 ↓ 时不展开整条控制条,不抢焦点)
            if (seekHud && !showPanel && activeItem == null && !inMoreMenu) {
                SeekHud(
                    position = position,
                    duration = duration,
                    buffered = buffered,
                    forward = seekHudForward,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                )
            }

            // 1.95 页面内提示条（替代系统 Toast，父亲 2026-10-13 口径：错误只在页面里说）
            hudText?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 96.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                        .padding(horizontal = 28.dp, vertical = 16.dp)
                ) {
                    Text(text = msg, color = Color.White, fontSize = 18.sp)
                }
            }
            LaunchedEffect(hudText) {
                if (hudText != null) {
                    delay(5000)
                    hudText = null
                }
            }

            // 2. 播放控制面板(按 ↓ 唤出):整块无底色,二级菜单浮在上方
            if (showPanel) {
                val mainMenu = PLAYER_MAIN_MENU.filter { item ->
                    when (item) {
                        PlayerMenuItem.EPISODES -> mediaInfo.seriesId != null
                        PlayerMenuItem.CAST -> !mediaInfo.people.isNullOrEmpty()
                        else -> true
                    }
                }

                // 信息/演职人员打开时整排控制收起,整屏只留内容(照官方排版)
                if (activeItem != PlayerMenuItem.INFO && activeItem != PlayerMenuItem.CAST) {
                PlayerControlPanel(
                    modifier = Modifier,
                    /* 控制条矩形由 PlayerControls 的底部横条自己上报（外层是整屏，不能当控制条） */
                    title = panelTitle(),
                    subtitle = panelSubtitle(),
                    // 官方副行没有"播放方式"这一项，传空即不显示
                    playMethodLabel = "",
                    // 「选片」回海报墙、「退出」关掉播放页（父亲 2026-10-28：与 VR 控制条同名同序）
                    onPick = onPickPoster,
                    onExitPlayer = onExit,
                    // 左上角片名 Logo 已改为常显图层（见上面的 1.85），这里传空避免重复画
                    logoUrl = null,
                    position = position,
                    duration = duration,
                    buffered = buffered,
                    onSeekTo = { target ->
                        // 立刻写 position：暂停状态下游标也要跟手（原来要等 800ms 采样）
                        position = target.coerceIn(0L, duration.coerceAtLeast(0L))
                        player.seekTo(target)
                        interactionTick++
                    },
                    isPlaying = isPlaying,
                    menuItems = mainMenu,
                    onButtonPositions = { btnPositions = it },
                    activeItem = if (inMoreMenu) PlayerMenuItem.MORE else activeItem,
                    onMenuSelect = { item ->
                        // 用 PLAYER_MAIN_MENU 的固定下标：mainMenu 是按内容过滤后的列表，
                        // 缺项时下标会偏移，而 PlayerControlPanel 里的 itemFocus 表是按 PLAYER_MAIN_MENU 建的
                        lastMenuIndex = PLAYER_MAIN_MENU.indexOf(item).coerceAtLeast(0)
                        when (item) {
                            PlayerMenuItem.MORE -> {
                                inMoreMenu = true
                                activeItem = null
                            }

                            PlayerMenuItem.BACK -> {
                                inMoreMenu = false
                                activeItem = null
                            }

                            // 「选片」不是菜单，是动作键：回打开这部片的那张海报墙
                            // （与「退出」区分：退出是直接回首页，见 onExitPlayer）
                            PlayerMenuItem.PICK -> {
                                activeItem = null
                                onPickPoster()
                            }

                            else -> activeItem = if (activeItem == item) null else item
                        }
                    },
                    onSeekBack = { player.seekBack() },
                    onSeekForward = { player.seekForward() },
                    onPlayPause = { if (isPlaying) player.pause() else player.play() },
                    initialFocusIndex = lastMenuIndex
                )
                }

                // 「更多」:从更多按钮上方长出一竖列;选中某项后本列消失,换成该项的新列
                if (inMoreMenu) {
                    SheetShell(
                        // 靠右：从右侧的「更多」按钮上方长出（父亲 2026-09-30：原先落在屏幕中间、离右边太远）
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 48.dp, bottom = 132.dp),
                        title = "",
                        contentWidth = 190.dp
                    ) {
                        PLAYER_MORE_MENU.forEachIndexed { index, item ->
                            val label = when (item) {
                                PlayerMenuItem.AUDIO ->
                                    "音频选择：" + (
                                        audioTracks.firstOrNull { it.index == selectedAudioIndex }
                                            ?.displayTitle ?: "默认"
                                        )
                                PlayerMenuItem.QUALITY -> "视频质量：" + qualityLabel(maxStreamingBitrate)
                                PlayerMenuItem.PLAY_MODE -> "播放模式：" + playModeName(playMode)
                                // 行尾写当前档，免得点进去才知道现在是什么（VR 同款）
                                PlayerMenuItem.BUFFER -> "缓冲设置：" + when (minBufferMs) {
                                    15_000 -> "标准"
                                    30_000 -> "较大"
                                    60_000 -> "最大"
                                    else -> (minBufferMs / 1000).toString() + " 秒"
                                }
                                else -> item.label
                            }
                            SheetRow(
                                label = label,
                                selected = false,
                                onClick = {
                                    inMoreMenu = false
                                    activeItem = if (item == PlayerMenuItem.BACK) null else item
                                },
                                firstFocus = if (index == 0) sheetFirstFocus else null,
                                showArrow = item != PlayerMenuItem.BACK
                            )
                        }
                    }
                }

                if (activeItem != null) {
                    val item = activeItem!!
                    if (item == PlayerMenuItem.INFO || item == PlayerMenuItem.CAST) {
                        if (item == PlayerMenuItem.INFO) {
                            // 信息:无框,海报+文字直接叠在画面左下(照官方排版)
                            InfoSheet(
                                mediaInfo = mediaInfo,
                                serverUrl = serverUrl,
                                techLine = techLineOf(
                                    media.mediaSources?.firstOrNull(),
                                    getVideoTrack(media)?.displayTitle ?: ""
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(start = 58.dp, end = 58.dp, bottom = 56.dp),
                                firstFocus = sheetFirstFocus
                            )
                        } else {
                            // 演职人员:与信息同一风格,无框,标题+头像卡片
                            CastListSheet(
                                people = mediaInfo.people ?: emptyList(),
                                serverUrl = serverUrl,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(start = 58.dp, bottom = 56.dp),
                                firstFocus = sheetFirstFocus
                            )
                        }
                    } else {
                    SheetShell(
                        // 菜单锚定在"点它的那个一级按钮"正上方（父亲 2026-09-30）：
                        // 按钮的屏幕坐标由 PlayerControlPanel 通过 onButtonPositions 上报，
                        // 菜单左边缘 = 按钮中心 − 菜单宽/2（左右都夹在安全边距内）。
                        modifier = run {
                            /*
                             * 锚点必须是真正被点的那个按钮。原先画质/播放模式/缓冲指向「更多」，
                             * 而「更多」是右组最靠边的一颗，于是画质菜单永远贴着屏幕右边出来
                             * （父亲 2026-10-31 实拍）。这三项现在都在一级控制条上，各有自己的按钮。
                             */
                            val anchor = item
                            val pos = btnPositions[anchor]
                            val menuWidthDp = if (item == PlayerMenuItem.BUFFER) 270f else 210f
                            // 用 density 系数直接换算（不用 Dp 的扩展函数，避免依赖不确定性）
                            val menuWidthPx = (density.density * menuWidthDp).toInt()
                            val minPx = (density.density * 24f).toInt()
                            val maxPx = (screenWidthPx - menuWidthPx - minPx).coerceAtLeast(minPx)
                            // 菜单左边缘 = 按钮中心 − 菜单宽/2，左右夹在安全边距内
                            val xPx = if (pos != null) {
                                (pos.first + pos.second / 2 - menuWidthPx / 2).coerceIn(minPx, maxPx)
                            } else minPx
                            Modifier
                                .align(Alignment.BottomStart)
                                .offset { IntOffset(xPx, 0) }
                                .padding(bottom = 132.dp)
                        },
                        // 三个二级菜单标题统一：字幕那颗原先没写标题，看着像半截弹窗
                        title = item.label,
                        contentWidth = when (item) {
                            PlayerMenuItem.BUFFER -> 270.dp
                            else -> 210.dp
                        }
                    ) {
                        when (item) {
                            PlayerMenuItem.EPISODES -> EpisodeListSheet(
                                seriesId = mediaInfo.seriesId,
                                currentId = mediaInfo.id,
                                repository = repository,
                                onPlay = { episode ->
                                    activeItem = null
                                    showPanel = false
                                    onNavigateToPlayer(episode)
                                },
                                firstFocus = sheetFirstFocus
                            )

                            PlayerMenuItem.SUBTITLE -> SubtitleSheet(
                                tracks = subtitleTracks,
                                selectedIndex = selectedSubtitleIndex,
                                onSelect = { index ->
                                    changeTrack(selectedAudioIndex, index)
                                    activeItem = null
                                },
                                firstFocus = sheetFirstFocus
                            )

                            PlayerMenuItem.AUDIO -> AudioSheet(
                                tracks = audioTracks,
                                selectedIndex = selectedAudioIndex,
                                onSelect = { index ->
                                    changeTrack(index, selectedSubtitleIndex)
                                    activeItem = null
                                },
                                firstFocus = sheetFirstFocus
                            )

                            PlayerMenuItem.DANMAKU -> DanmakuSheet(
                                enabled = danmakuEnabled,
                                onEnabledChange = {
                                    danmakuEnabled = it
                                    danmakuPrefs.edit().putBoolean("danmaku_enabled", it).apply()
                                },
                                scale = danmakuScale,
                                onScaleChange = {
                                    danmakuScale = it
                                    danmakuPrefs.edit().putFloat("danmaku_scale", it).apply()
                                },
                                offsetMs = danmakuOffsetMs,
                                onOffsetChange = { danmakuOffsetMs = it },
                                firstFocus = sheetFirstFocus
                            )

                            PlayerMenuItem.SPEED -> SpeedSheet(
                                current = playbackSpeed,
                                onChange = { newSpeed ->
                                    val wasNormal = preferencesManager.playbackSpeed == 1.0f
                                    val willBeNormal = newSpeed == 1.0f
                                    playbackSpeed = newSpeed
                                    preferencesManager.playbackSpeed = newSpeed
                                    if (wasNormal != willBeNormal) {
                                        // 跨过 1.0x:隧道开关必须跟着变,只能重建播放器(隧道不支持变速)
                                        rebuildPlayerKeepingPosition()
                                    } else {
                                        player.setPlaybackSpeed(newSpeed)
                                    }
                                    DiagLog.w(
                                        context, "speed",
                                        "选择 ${newSpeed}x(重建=${wasNormal != willBeNormal}) " +
                                                "→ 播放器实际 ${player.playbackParameters.speed}x"
                                    )
                                },
                                firstFocus = sheetFirstFocus
                            )

                            PlayerMenuItem.QUALITY -> QualitySheet(
                                current = maxStreamingBitrate,
                                onChange = { maxStreamingBitrate = it },
                                firstFocus = sheetFirstFocus
                            )

                            PlayerMenuItem.PLAY_MODE -> PlayModeSheet(
                                current = playMode,
                                onChange = { value ->
                                    playMode = value
                                    context.getSharedPreferences("emby_tv_prefs", Context.MODE_PRIVATE)
                                        .edit().putInt("play_mode", value).apply()
                                },
                                firstFocus = sheetFirstFocus
                            )

                            PlayerMenuItem.BUFFER -> BufferPresetSheet(
                                currentMinBufferMs = minBufferMs,
                                onApplyPreset = { minMs, maxMs ->
                                    minBufferMs = minMs
                                    maxBufferMs = maxMs
                                    preferencesManager.minBufferMs = minMs
                                    preferencesManager.maxBufferMs = maxMs
                                    rebuildPlayerKeepingPosition()
                                },
                                firstFocus = sheetFirstFocus
                            )

                            else -> {}
                        }
                    }
                    }
                }

                // 停止操作 8 秒后自动收起;二级菜单/更多列开着时不收(暂停时也不收)
                LaunchedEffect(showPanel, activeItem, inMoreMenu, isPlaying, interactionTick) {
                    if (activeItem != null || inMoreMenu) return@LaunchedEffect
                    if (isPlaying) {
                        kotlinx.coroutines.delay(8000)
                        // 收起前先把焦点交回根容器，否则控制条按钮消失时焦点一起丢
                        collapseThen { showPanel = false }
                    }
                }

            }

            // 左右键唤出的进度条:最后一次快进/快退后 5 秒收起。
            // 必须放在控制条之外 —— 原先写在 if (showPanel) 里面,控制条没展开时这段根本不在组合里,
            // 于是进度条永远不会自己消失(父亲两次实测"5 秒消失未生效")。
            LaunchedEffect(seekHudAt) {
                if (seekHudAt == 0L) return@LaunchedEffect
                kotlinx.coroutines.delay(5000)
                seekHud = false
                seekHudAt = 0L
            }

            // 3. Simple Pause/Loading Overlay
            if (!isPlaying || isBuffering) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // 起播/缓冲用 VR 那套绿色带箭头的环，不再用系统白色细弧
                            com.xxxx.emby_tv.ui.components.VrLoadingRing()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = Utils.formatBandwidth(downloadSpeed),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // Play Icon
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Paused",
                            modifier = Modifier.size(100.dp),
                            tint = Color.White
                        )
                    }
                }

                if (!isBuffering) {
                    // Menu Hint at bottom
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 48.dp)
                        ) {


                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.press_menu_down_to_show_menu),
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }


            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    } // 最外层纯黑背景 Box 闭合
}


/**
 * 这个字幕流是不是弹幕文件。
 * 判据照 VR：标题/显示名里带「弹幕」，或带 danmaku / danmu（不区分大小写）。
 */
private fun isDanmakuSubtitle(stream: com.xxxx.emby_tv.data.model.MediaStreamDto): Boolean {
    val text = (stream.displayTitle ?: "") + " " + (stream.title ?: "")
    val lower = text.lowercase()
    return text.contains("弹幕") || lower.contains("danmaku") || lower.contains("danmu")
}
