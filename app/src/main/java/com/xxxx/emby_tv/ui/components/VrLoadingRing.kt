package com.xxxx.emby_tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 等待转圈：与 VR 版同一套画法（VR 的 danmaku/DanmakuSurfacePainter.drawLoadingSpinner）。
 *
 * 差异在细节：不是系统那条粗细均匀的弧，而是一圈 72 段、留 45° 缺口、越靠近缺口越亮越粗，
 * 缺口端带一个箭头，整体顺时针 149°/秒（VR 的 2.6 rad/s）。
 * 父亲 2026-10-29 要求「把带箭头的圆圈换成 VR 版那个圆圈」。
 */
@Composable
fun VrLoadingRing(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    color: Color = Color(0xFF2FD57C),
) {
    /*
     * 角度交给动画系统驱动。
     * 原来用自绘的逐帧循环 + withFrameNanos 推角度，实机上表现为"环画出来了但不动"：
     * 播放页里 Canvas 的重绘被播放器那层渲染压着，自起帧循环拿不到稳定的帧回调。
     * rememberInfiniteTransition 走 Compose 的动画时钟，动画值变化必然触发重绘。
     * 149°/秒 → 一圈约 2.42 秒。
     */
    val transition = rememberInfiniteTransition(label = "vrLoading")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2416, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f * 0.82f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val baseThick = radius * 0.12f
        val segs = 72
        val drawn = segs - 9                     // 缺口 45°
        val sweep = 360f / segs
        for (i in 0 until drawn) {
            val q = i / (drawn - 1f)
            drawArc(
                color = color.copy(alpha = 0.45f + 0.55f * q),
                startAngle = angle + i * sweep,
                sweepAngle = sweep * 1.25f,      // 段与段搭接，看起来是一条连续的线
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = baseThick * (0.45f + 0.9f * q), cap = StrokeCap.Round),
            )
        }
        // 缺口端的箭头：尖朝顺时针切向
        val headRad = Math.toRadians((angle + drawn * sweep).toDouble())
        val hx = center.x + radius * kotlin.math.cos(headRad).toFloat()
        val hy = center.y + radius * kotlin.math.sin(headRad).toFloat()
        val tx = -kotlin.math.sin(headRad).toFloat()
        val ty = kotlin.math.cos(headRad).toFloat()
        val nx = kotlin.math.cos(headRad).toFloat()
        val ny = kotlin.math.sin(headRad).toFloat()
        val tipLen = radius * 0.36f
        val backLen = radius * 0.20f
        val halfW = radius * 0.17f
        val head = Path().apply {
            moveTo(hx + tx * tipLen, hy + ty * tipLen)
            lineTo(hx - tx * backLen + nx * halfW, hy - ty * backLen + ny * halfW)
            lineTo(hx - tx * backLen - nx * halfW, hy - ty * backLen - ny * halfW)
            close()
        }
        drawPath(head, color)
    }
}
