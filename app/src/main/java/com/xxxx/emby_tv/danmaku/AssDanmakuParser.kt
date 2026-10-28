package com.xxxx.emby_tv.danmaku


/** 单条弹幕的样式(取自 ASS 的 [V4+ Styles]) */
data class DanmakuStyle(
    val fontSize: Float,
    val primaryColor: Int,
    val outlineColor: Int,
    val outline: Float,
    val alignment: Int,
)

/** 单条弹幕 */
data class DanmakuItem(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val isMove: Boolean,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val posX: Float,
    val posY: Float,
    val style: DanmakuStyle,
)

data class DanmakuTrack(
    val playResX: Float,
    val playResY: Float,
    val items: List<DanmakuItem>,
)
