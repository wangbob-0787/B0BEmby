package com.xxxx.emby_tv.ui.player

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.xxxx.emby_tv.data.model.MediaStreamDto

/**
 * 播放器轨道管理器
 * 
 * 负责处理字幕和音频轨道的选择逻辑
 */
object PlayerTrackManager {
    private const val TAG = "PlayerTrackManager"

    /**
     * 选择字幕轨道
     * 
     * @param player ExoPlayer 实例
     * @param subtitleTracks Emby 返回的字幕轨道列表
     * @param selectedIndex 要选择的字幕 index
     * @param currentTracks 当前轨道信息
     */
    fun selectSubtitle(
        player: ExoPlayer,
        subtitleTracks: List<MediaStreamDto>,
        selectedIndex: Int,
        currentTracks: Tracks?
    ) {
        if (subtitleTracks.isEmpty()) return

        // 如果是 -1，关闭字幕
        if (selectedIndex == -1) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            Log.d(TAG, "Subtitle disabled")
            return
        }

        // 找到目标字幕
        val targetTrack = subtitleTracks.find { it.index == selectedIndex }
            ?: run {
                Log.w(TAG, "Target subtitle index $selectedIndex not found in metadata")
                return
            }

        val targetOrdinalIndex = subtitleTracks.indexOf(targetTrack)
        val targetLabel = targetTrack.displayTitle
        val targetIndex = targetTrack.index?.toString() ?: ""
        val isLoadedViaConfig = targetTrack.supportsExternalStream == true || targetTrack.isExternal == true

//        Log.d(TAG, "Subtitle Selection: Target [EmbyIndex:$targetIndex, Label:$targetLabel, OrdinalIndex:$targetOrdinalIndex, LoadedViaConfig:$isLoadedViaConfig]")

        // 开启文本轨道
        var parametersBuilder = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)

        val groups = currentTracks?.groups ?: player.currentTracks.groups
        val trackGroups = groups.filter { it.type == C.TRACK_TYPE_TEXT }
        var isMatched = false

        // 打印可用字幕轨道
//        Log.d(TAG, "--- Available Text Tracks ---")
        trackGroups.forEachIndexed { gi, group ->
            for (i in 0 until group.length) {
                val f = group.getTrackFormat(i)
//                Log.d(TAG, "Group[$gi] Track[$i]: ID=${f.id}, Label=${f.label}, Lang=${f.language}")
            }
        }

        /*
         * 匹配顺序（2026-10-29 重做，父亲报"字幕菜单点了没反应"）：
         *  1) 唯一 Label（"标题 [index]"）—— 外挂字幕由配置加载时最准；
         *  2) 轨道 ID 以 :index 结尾或等于 index —— 用 Emby 的流序号对 ExoPlayer 的轨道号；
         *  3) 按顺序数（内封字幕走这条）。
         * 原来 1、2 两段被整块注释掉，3 又被 isLoadedViaConfig（只认 isExternal）门住，
         * 于是**内嵌字幕一条也切不动**，只在日志里留一行 warn。
         */
        outer1@ for (group in trackGroups) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                if (format.label == "$targetLabel [$targetIndex]") {
                    parametersBuilder.setOverrideForType(
                        TrackSelectionOverride(group.mediaTrackGroup, i)
                    )
                    isMatched = true
                    break@outer1
                }
            }
        }

        if (!isMatched) {
            outer2@ for (group in trackGroups) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.id?.endsWith(":$targetIndex") == true || format.id == targetIndex) {
                        parametersBuilder.setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, i)
                        )
                        isMatched = true
                        break@outer2
                    }
                }
            }
        }

        /*
         * 语言 + 标题匹配（2026-10-29 父亲实测「选中文出来的是英文」）：
         * 前两条都靠 label/id 的写法，Emby 与外挂配置的写法对不上时就落到最后的
         * 「按顺序数」——而顺序里夹着容器内封轨，一数就偏到别的语言上。
         * 这里先按语言精确匹配，同语言多条时再用标题区分；命中就不再往下数顺序。
         */
        if (!isMatched) {
            val wantLang = targetTrack.language?.lowercase()?.takeIf { it.isNotBlank() }
            val wantTitle = (targetTrack.title ?: targetTrack.displayTitle)?.lowercase()
            val candidates = mutableListOf<Pair<TrackGroup, Int>>()
            for (group in trackGroups) {
                for (i in 0 until group.length) {
                    val f = group.getTrackFormat(i)
                    if (wantLang != null && f.language?.lowercase() != wantLang) continue
                    candidates += group.mediaTrackGroup to i
                }
            }
            if (candidates.size == 1) {
                parametersBuilder.setOverrideForType(
                    TrackSelectionOverride(candidates[0].first, candidates[0].second)
                )
                isMatched = true
            } else if (candidates.isNotEmpty() && wantTitle != null) {
                for ((group, idx) in candidates) {
                    val f = group.getFormat(idx)
                    val hay = ((f.label ?: "") + " " + (f.id ?: "")).lowercase()
                    if (hay.contains(wantTitle) || wantTitle.contains((f.label ?: "").lowercase())) {
                        parametersBuilder.setOverrideForType(TrackSelectionOverride(group, idx))
                        isMatched = true
                        break
                    }
                }
            }
        }

        if (!isMatched && targetOrdinalIndex >= 0) {
            var trackCounter = 0
            outerOrdinal@ for (group in trackGroups) {
                for (i in 0 until group.length) {
                    if (trackCounter == targetOrdinalIndex) {
                        parametersBuilder.setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, i)
                        )
                        isMatched = true
                        break@outerOrdinal
                    }
                    trackCounter++
                }
            }
        }

        if (isMatched) {
            player.trackSelectionParameters = parametersBuilder.build()
        } else {
            Log.w(TAG, "Unable to match subtitle: EmbyIndex=$targetIndex, OrdinalIndex=$targetOrdinalIndex, Label=$targetLabel")
        }
    }

    /**
     * 选择音频轨道
     * 
     * @param player ExoPlayer 实例
     * @param audioTracks Emby 返回的音频轨道列表
     * @param selectedIndex 要选择的音频 index
     * @param currentTracks 当前轨道信息
     */
    fun selectAudio(
        player: ExoPlayer,
        audioTracks: List<MediaStreamDto>,
        selectedIndex: Int,
        currentTracks: Tracks?
    ) {
        if (audioTracks.isEmpty()) return
        if (selectedIndex <= -1) return

        val targetTrack = audioTracks.find { it.index == selectedIndex }
            ?: run {
                Log.w(TAG, "Target audio index $selectedIndex not found in metadata")
                return
            }

        val targetOrdinalIndex = audioTracks.indexOf(targetTrack)
        val targetLabel = targetTrack.displayTitle
        val targetIndex = targetTrack.index?.toString() ?: ""

        Log.d(TAG, "Audio Selection: Target [EmbyIndex:$targetIndex, Label:$targetLabel, OrdinalIndex:$targetOrdinalIndex]")

        var parametersBuilder = player.trackSelectionParameters.buildUpon()

        val groups = currentTracks?.groups ?: player.currentTracks.groups
        val trackGroups = groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        var isMatched = false

        // 打印可用音频轨道
        // Log.d(TAG, "--- Available Audio Tracks ---")
        var totalTracks = 0
        trackGroups.forEachIndexed { gi, group ->
            for (i in 0 until group.length) {
                val f = group.getTrackFormat(i)
                // Log.d(TAG, "Group[$gi] Track[$i]: ID=${f.id}, Label=${f.label}, Lang=${f.language}")
                totalTracks++
            }
        }
        Log.d(TAG, "Total audio tracks: $totalTracks")

        // 使用顺序匹配：音频列表的第一个对应轨道顺序0
        if (targetOrdinalIndex >= 0) {
            var trackCounter = 0
            outerOrdinal@ for (group in trackGroups) {
                for (i in 0 until group.length) {
                    if (trackCounter == targetOrdinalIndex) {
                        parametersBuilder.setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, i)
                        )
                        isMatched = true
                        val f = group.getTrackFormat(i)
//                        Log.d(TAG, "Matched audio by Ordinal: OrdinalIndex=$targetOrdinalIndex -> Track (ID:${f.id}, Label:${f.label})")
                        break@outerOrdinal
                    }
                    trackCounter++
                }
            }
        }

        if (isMatched) {
            player.trackSelectionParameters = parametersBuilder.build()
        } else {
            Log.w(TAG, "Unable to match audio: OrdinalIndex=$targetOrdinalIndex, Label=$targetLabel")
        }
    }
}

/*
 * 字幕清单与选择（照 VR 的做法：**以播放器实际报出来的轨表为准**，不再拿 Emby 的流
 * 序号去猜）。VR 侧是读内核 track-list、按 ordinal 直接切；TV 侧对应读
 * ExoPlayer 的 Tracks，按「轨道组 + 组内序号」直接 override，标签也用轨表自己的。
 * 这样菜单里出现的每一条都真实存在、勾哪条就是哪条（父亲 2026-10-29 报
 * 「选项不全、选中文出英文」就是猜序号猜歪了）。
 */
data class TextTrackItem(
    val key: String,
    val label: String,
    val sub: String?,
    val group: TrackGroup,
    val trackIndex: Int,
    val language: String?,
)

private fun langName(code: String?): String? {
    if (code.isNullOrBlank() || code == "und") return null
    return java.util.Locale.forLanguageTag(code.replace('_', '-')).displayLanguage
}

fun textTrackItems(player: ExoPlayer): List<TextTrackItem> {
    val out = mutableListOf<TextTrackItem>()
    val groups = runCatching { player.currentTracks.groups }.getOrNull() ?: return out
    groups.forEachIndexed { gi, group ->
        if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
        for (i in 0 until group.length) {
            val f = group.getTrackFormat(i)
            val lang = f.language
            val label = f.label?.takeIf { it.isNotBlank() } ?: langName(lang) ?: "字幕 ${out.size + 1}"
            val sub = langName(lang)?.takeIf { it != label }
            out += TextTrackItem("g$gi:$i", label, sub, group.mediaTrackGroup, i, lang)
        }
    }
    return out
}

/** 当前正在显示的那条（null = 关着） */
fun selectedTextTrackKey(player: ExoPlayer): String? {
    val groups = runCatching { player.currentTracks.groups }.getOrNull() ?: return null
    groups.forEachIndexed { gi, group ->
        if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
        for (i in 0 until group.length) {
            if (group.isTrackSelected(i)) return "g$gi:$i"
        }
    }
    return null
}

fun selectTextTrack(player: ExoPlayer, item: TextTrackItem?) {
    val b = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, item == null)
    if (item != null) b.setOverrideForType(TrackSelectionOverride(item.group, item.trackIndex))
    player.trackSelectionParameters = b.build()
}
