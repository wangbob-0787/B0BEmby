package com.xxxx.emby_tv.danmaku

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import kotlin.math.max

/**
 * 独立的弹幕绘制层。
 *
 * 设计要点(2026-09-19 父亲要求):
 *  - **单独一层**:不复用字幕 View,弹幕与普通字幕互不干扰;
 *  - **不抖**:每帧用播放器当前时间重新计算坐标(绝对时间 → 位置),
 *    不做"每帧位移累加",因此掉帧只会跳一下、不会越走越偏;
 *  - **与 vsync 对齐**:绘制完用 postInvalidateOnAnimation 请求下一帧。
 */
class DanmakuView(context: Context) : View(context) {

    private var track: DanmakuTrack? = null
    private var positionProvider: (() -> Long)? = null
    private var running = false

    /** 弹幕整体缩放倍率(用户在设置里可调,默认 1.0) */
    var userScale: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            invalidate()
        }

    /*
     * 字体与描边对齐 VR 版弹幕层（父亲 2026-10-29：TV 的字体和颜色也要像 VR）：
     * 常规字重 + 黑色阴影，取代原来的加粗黑描边 —— 描边在 1080P 电视上会把细笔画糊死。
     */
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        style = Paint.Style.FILL
        color = Color.argb(200, 0, 0, 0)
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    fun setTrack(t: DanmakuTrack?) {
        /*
         * 取证（2026-10-29）：电视上"弹幕两行仍然重叠、字还是黑的"，先确认到底哪套数据在画、
         * 首条颜色与轨道 y 是多少。日志 tag B0BDiag。
         */
        com.xxxx.emby_tv.util.DiagLog.w(
            context, "danmaku",
            "装入轨道：条数=" + (t?.items?.size ?: 0) +
                " PlayRes=" + (t?.playResX ?: 0f) + "x" + (t?.playResY ?: 0f) +
                " 首条色=0x" + Integer.toHexString(t?.items?.firstOrNull()?.style?.primaryColor ?: 0) +
                " y1=" + (t?.items?.firstOrNull()?.y1 ?: -1f) +
                " 首条移动=" + (t?.items?.firstOrNull()?.isMove ?: false))
        track = t
        invalidate()
    }

    fun setPositionProvider(provider: () -> Long) {
        positionProvider = provider
    }

    fun start() {
        if (!running) {
            running = true
            postInvalidateOnAnimation()
        }
    }

    fun stop() {
        running = false
    }

    /**
     * 滚动弹幕的实际消失时间。
     *
     * 源 ASS 的 \move 终点是固定坐标(如 -810),长弹幕会在还没完全出屏时就消失(画面里"突然不见")。
     * 这里按文字实测宽度推算"完全滚出屏幕"所需时间,取两者较大值 → 弹幕完整地从屏幕外滚入、再完整滚出。
     */
    private fun effectiveEndMs(item: DanmakuItem, scale: Float): Long {
        if (!item.isMove) return item.endMs
        val spanMs = (item.endMs - item.startMs).coerceAtLeast(1L)
        val dx = item.x1 - item.x2
        if (dx <= 0f) return item.endMs
        fillPaint.textSize = max(10f, item.style.fontSize * scale)
        val textWidthInPlayRes = fillPaint.measureText(item.text) / scale.coerceAtLeast(0.01f)
        val speed = dx / spanMs                       // PlayRes 单位 / 毫秒
        if (speed <= 0f) return item.endMs
        val fullExitMs = ((item.x1 + textWidthInPlayRes) / speed).toLong()
        return item.startMs + max(spanMs, fullExitMs)
    }

    override fun onDetachedFromWindow() {
        running = false
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = track
        if (t == null || t.items.isEmpty()) {
            if (running) postInvalidateOnAnimation()
            return
        }
        val nowMs = positionProvider?.invoke() ?: 0L
        // 弹幕画布(ASS 的 PlayRes) → 控件宽度的缩放
        val scale = if (t.playResX > 0f) (width / t.playResX) * userScale else userScale
        for (item in t.items) {
            if (item.startMs > nowMs) break          // 已按开始时间排序,后面都还没到
            if (effectiveEndMs(item, scale) < nowMs) continue   // 已完全滚出屏幕
            drawItem(canvas, item, nowMs, scale)
        }
        if (running) postInvalidateOnAnimation()
    }

    private fun drawItem(canvas: Canvas, item: DanmakuItem, nowMs: Long, scale: Float) {
        val span = (item.endMs - item.startMs).coerceAtLeast(1L)
        val progress = ((nowMs - item.startMs).toFloat() / span).coerceIn(0f, 1f)

        val x: Float
        val y: Float
        if (item.isMove) {
            x = (item.x1 + (item.x2 - item.x1) * progress) * scale
            y = (item.y1 + (item.y2 - item.y1) * progress) * scale
        } else {
            x = item.posX * scale
            y = item.posY * scale
        }
        if (x > width || x < -width) return          // 完全在画面外

        val textSize = max(10f, item.style.fontSize * scale)
        fillPaint.textSize = textSize
        outlinePaint.textSize = textSize
        /*
         * 颜色按 VR 口径统一成近白：服务端返回的彩色在电视上偏暗、看不清，
         * 深色（亮度低于 0.55）一律提白，浅色则保留原色。
         */
        val base = item.style.primaryColor
        val r = android.graphics.Color.red(base)
        val g2 = android.graphics.Color.green(base)
        val b2 = android.graphics.Color.blue(base)
        val luma = (0.299f * r + 0.587f * g2 + 0.114f * b2) / 255f
        fillPaint.color = if (luma < 0.55f) 0xFFE8F8FF.toInt() else base
        shadowPaint.textSize = textSize
        // 阴影偏移按字号缩放（VR 用的是静音 2~3px 的黑色投影，不是四周描边）
        val off = max(1.5f, textSize * 0.05f)

        // Alignment 7(左上)语义:给出的坐标是文本左上角;drawText 需要基线
        val baseline = y + textSize
        canvas.drawText(item.text, x + off, baseline + off, shadowPaint)
        canvas.drawText(item.text, x, baseline, fillPaint)
    }
}
