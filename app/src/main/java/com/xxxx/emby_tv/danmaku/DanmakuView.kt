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
 * **绘制口径与 VR 版完全一致**（`B0BEmby-VR/.../danmaku/DanmakuView.kt` 的 drawItem）：
 * 位置与颜色都由条目自带 —— 轨道在 [LiveDanmakuApi] 里按 VR 的算法排好 —— 本类只按播放
 * 时间求横坐标并画字：黑色描边打底、源色填充。不在这里排轨道、也不在这里改色，
 * 两版一旦分头改造必然走偏。
 */
class DanmakuView(context: Context) : View(context) {

    private var track: DanmakuTrack? = null
    private var positionProvider: (() -> Long)? = null
    private var running = false

    /** 弹幕整体缩放倍率（用户在设置里可调，默认 1.0） */
    var userScale: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            invalidate()
        }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    fun setTrack(t: DanmakuTrack?) {
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

    override fun onDetachedFromWindow() {
        running = false
        super.onDetachedFromWindow()
    }

    /**
     * 滚动弹幕的实际消失时间：源里的终点是固定坐标，长弹幕会在没完全出屏时消失。
     * 按实测文字宽度推算"完全滚出屏幕"所需时间，取较大值。
     */
    private fun effectiveEndMs(item: DanmakuItem, scale: Float): Long {
        if (!item.isMove) return item.endMs
        val spanMs = (item.endMs - item.startMs).coerceAtLeast(1L)
        val dx = item.x1 - item.x2
        if (dx <= 0f) return item.endMs
        fillPaint.textSize = max(10f, item.style.fontSize * scale)
        val textWidthInPlayRes = fillPaint.measureText(item.text) / scale.coerceAtLeast(0.01f)
        val speed = dx / spanMs
        if (speed <= 0f) return item.endMs
        val fullExitMs = ((item.x1 + textWidthInPlayRes) / speed).toLong()
        return item.startMs + max(spanMs, fullExitMs)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = track
        if (t == null || t.items.isEmpty()) {
            if (running) postInvalidateOnAnimation()
            return
        }
        val nowMs = positionProvider?.invoke() ?: 0L
        val scale = if (t.playResX > 0f) (width / t.playResX) * userScale else userScale
        for (item in t.items) {
            if (item.startMs > nowMs) break
            if (effectiveEndMs(item, scale) < nowMs) continue
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
        if (x > width || x < -width) return

        val textSize = max(10f, item.style.fontSize * scale)
        fillPaint.textSize = textSize
        outlinePaint.textSize = textSize
        outlinePaint.strokeWidth = max(1.5f, item.style.outline * scale)
        fillPaint.color = item.style.primaryColor
        outlinePaint.color = if (item.style.outlineColor == Color.TRANSPARENT) {
            Color.argb(160, 0, 0, 0)
        } else {
            item.style.outlineColor
        }

        // Alignment 7(左上)语义：给出的是文本左上角，drawText 需要基线
        val baseline = y + textSize
        canvas.drawText(item.text, x, baseline, outlinePaint)
        canvas.drawText(item.text, x, baseline, fillPaint)
    }
}
