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
 * 三条硬约束（照 VR 版的观感定的，改之前先读）：
 *
 *  1. **颜色由客户端定，不信源里的颜色值**：ASS 里的 `&H00FFFFFF&` 解析成 Int 是
 *     0x00FFFFFF，最高字节是 alpha=0，直接拿去画就是全透明，只剩黑色阴影可见 ——
 *     电视上"弹幕是黑的"就是这么来的。所以一律用不透明近白，黑色只做投影。
 *  2. **纵向位置由客户端排，不信源里的 y**：ASS 的 \move 里每条弹幕自带 y，
 *     同一部片子里大量条目共用同一个 y，照着画必然叠成一坨。这里按"轨道"重排：
 *     轨道数由控件高度决定，同一条轨道上一条完全进场后下一条才准进。
 *  3. **位置只由时间算**：每帧用播放器时间重新求坐标，不做逐帧累加，掉帧只跳一下。
 */
class DanmakuView(context: Context) : View(context) {

    private var track: DanmakuTrack? = null
    private var positionProvider: (() -> Long)? = null
    private var running = false

    /** 每条滚动弹幕落在第几条轨道（-1 = 固定弹幕，按源坐标画） */
    private var laneOf: IntArray = IntArray(0)
    /** 轨道满员判定用的"上一条屁股离开右边缘"时刻 */
    private var laneFreeAtMs: LongArray = LongArray(0)
    private var laneCount = 0

    /** 弹幕整体缩放倍率（用户在设置里可调，默认 1.0） */
    var userScale: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            rebuildLanes()
            invalidate()
        }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        style = Paint.Style.FILL
        color = Color.argb(190, 0, 0, 0)
    }

    fun setTrack(t: DanmakuTrack?) {
        track = t
        rebuildLanes()
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildLanes()
    }

    /**
     * 排轨道：按开始时间顺序扫一遍，每条挑"最早空出来"的那条。
     *
     * 一条弹幕什么时候让出轨道？它的尾巴完全进入画面之后 —— 宽度按 46 号字量，
     * 再留一段间距，否则两条首尾相接看着还是一坨。排一次缓存下来，
     * 每帧只查表，保证同一条弹幕每帧都在同一行（不能随帧重算，否则会上下跳）。
     */
    private fun rebuildLanes() {
        val t = track
        if (t == null || t.items.isEmpty()) {
            laneOf = IntArray(0)
            laneFreeAtMs = LongArray(0)
            return
        }
        val heightPx = height.toFloat()
        val sizePx = if (t.playResY > 0f) 46f * (heightPx / t.playResY) * userScale else 46f * userScale
        val stepPx = sizePx * 1.5f
        val topPx = heightPx * 0.055f
        laneCount = if (heightPx > 0f && stepPx > 1f) {
            (((heightPx * 0.58f) - topPx) / stepPx).toInt().coerceIn(1, 9)
        } else {
            2
        }

        val lanes = IntArray(t.items.size) { -1 }
        val free = LongArray(laneCount) { 0L }
        fillPaint.textSize = 46f
        val gapPx = 42f
        for (i in t.items.indices) {
            val item = t.items[i]
            if (!item.isMove) continue
            val span = (item.endMs - item.startMs).coerceAtLeast(1L)
            val dx = item.x1 - item.x2
            if (dx <= 0f) continue
            val speed = dx / span                     // PlayRes 单位 / 毫秒
            val width = fillPaint.measureText(item.text) + gapPx
            var lane = 0
            for (k in 1 until laneCount) if (free[k] < free[lane]) lane = k
            if (free[lane] <= item.startMs) {
                lanes[i] = lane
                free[lane] = item.startMs + (width / speed).toLong()
            }
            // 所有轨道都还没空出来：这条不排轨道（宁可不显示，也不叠到别人身上）
        }
        laneOf = lanes
        laneFreeAtMs = free
    }

    /** 轨道在控件像素坐标里的顶边（左上角语义） */
    private fun laneTopPx(lane: Int): Float {
        val t = track ?: return 0f
        val heightPx = height.toFloat()
        val sizePx = if (t.playResY > 0f) 46f * (heightPx / t.playResY) * userScale else 46f * userScale
        return heightPx * 0.055f + lane * sizePx * 1.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = track
        if (t == null || t.items.isEmpty()) {
            if (running) postInvalidateOnAnimation()
            return
        }
        if (laneOf.size != t.items.size) rebuildLanes()
        val nowMs = positionProvider?.invoke() ?: 0L
        val scaleX = if (t.playResX > 0f) (width / t.playResX) * userScale else userScale
        for (i in t.items.indices) {
            val item = t.items[i]
            if (item.startMs > nowMs) break
            if (effectiveEndMs(item, scaleX) < nowMs) continue
            drawItem(canvas, item, i, nowMs, scaleX)
        }
        if (running) postInvalidateOnAnimation()
    }

    /**
     * 滚动弹幕的实际消失时间。
     *
     * 源 ASS 的 \move 终点是固定坐标（如 -810），长弹幕会在还没完全出屏时就消失。
     * 这里按文字实测宽度推算"完全滚出屏幕"所需时间，取较大值。
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

    private fun drawItem(canvas: Canvas, item: DanmakuItem, index: Int, nowMs: Long, scale: Float) {
        val baseSize = max(10f, item.style.fontSize * scale)
        fillPaint.textSize = baseSize
        shadowPaint.textSize = baseSize

        val x: Float
        val y: Float
        if (item.isMove) {
            val lane = if (index < laneOf.size) laneOf[index] else -1
            if (lane < 0) return
            val span = (item.endMs - item.startMs).coerceAtLeast(1L)
            val progress = ((nowMs - item.startMs).toFloat() / span).coerceIn(0f, 1f)
            x = (item.x1 + (item.x2 - item.x1) * progress) * scale
            y = laneTopPx(lane)
        } else {
            x = item.posX * scale
            y = item.posY * scale
        }
        if (x > width || x < -width) return

        /*
         * 一律不透明近白：源里的颜色最高字节多为 0（alpha=0），照搬就是全透明，
         * 只剩黑色投影可见 —— 电视上"弹幕是黑的"的真因。
         */
        fillPaint.color = 0xFFF2FAFF.toInt()
        val off = max(1.5f, baseSize * 0.06f)
        val baseline = y + baseSize
        canvas.drawText(item.text, x + off, baseline + off, shadowPaint)
        canvas.drawText(item.text, x, baseline, fillPaint)
    }
}
