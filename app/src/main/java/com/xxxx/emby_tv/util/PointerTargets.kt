package com.xxxx.emby_tv.util

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.composed

/**
 * 控件坐标表（照 VR 的 panel/ClickTargets 做，但只保留 TV 需要的部分）。
 *
 * 为什么需要：Compose 不对外提供「给一个坐标返回哪个控件」的接口（命中测试在框架里，
 * 不外露）。而 PICO 的光标只告诉我们「指在哪」，得由我们自己把坐标换成焦点与点击，
 * 所以每个可点控件在排版时把自己的矩形登记进来，光标一动就查表。
 *
 * 三条约束（乐乐 2026-10-28 提醒，改这里之前先读）：
 * 1. 矩形在**每次布局**时刷新（onGloballyPositioned 每次排版都回调）：列表滚动、
 *    动画位移之后坐标都会变，只在首次排版记一次必然点偏。
 * 2. 查表按「面积最小者优先」：同一落点常常被外层容器与内层控件同时包含，
 *    面积小的才是用户真正指的那个。
 * 3. **触发只能有一次**：查表命中就只执行表里的动作，绝不再补发确认键；
 *    只有查不到时才退回 MainActivity 的「补发 DPAD_CENTER」。两者同时跑就是双击。
 */
object PointerTargets {

    class Target(
        val key: Any,
        val bounds: Rect,
        val focus: (() -> Unit)?,
        val activate: (() -> Unit)?,
    ) {
        val area: Float get() = bounds.width * bounds.height
        fun contains(x: Float, y: Float) = bounds.contains(androidx.compose.ui.geometry.Offset(x, y))
    }

    private val targets = LinkedHashMap<Any, Target>()

    /** 最近一次光标位置与时间：播放页据此判断「光点在控制条区还是播放区」 */
    @Volatile var cursorX = 0f
    @Volatile var cursorY = 0f
    @Volatile var cursorAt = 0L

    fun noteCursor(x: Float, y: Float) { cursorX = x; cursorY = y; cursorAt = System.currentTimeMillis() }

    /** 光标 3 秒内动过 = 用户在指东西（此时摇杆/扳机才有区域语义） */
    fun cursorActive(): Boolean =
        cursorAt > 0L && System.currentTimeMillis() - cursorAt < 3_000L

    /** 开发期开关：打开后每次查表都把命中结果写诊断日志，便于定位"点不动/点偏" */
    var debugEnabled = false

    fun put(key: Any, bounds: Rect, focus: (() -> Unit)?, activate: (() -> Unit)?) {
        synchronized(targets) { targets[key] = Target(key, bounds, focus, activate) }
    }

    fun remove(key: Any) {
        synchronized(targets) { targets.remove(key) }
    }

    /** 落点上面积最小的控件（就是用户真正指的那个） */
    fun findAt(x: Float, y: Float): Target? = synchronized(targets) {
        targets.values.filter { it.contains(x, y) }.minByOrNull { it.area }
    }

    /** 光标移到哪儿，焦点就落到那儿；返回是否找到了控件 */
    fun focusAt(x: Float, y: Float): Boolean {
        val t = findAt(x, y) ?: return false
        t.focus?.let { runCatching { it() } }
        return true
    }

    /**
     * 扣扳机时先查表：命中就执行表里的动作并返回 true（调用方据此**不再**补发确认键）。
     * 没命中返回 false，由调用方退回遥控器语义（发 DPAD_CENTER 给当前焦点）。
     */
    fun activateAt(x: Float, y: Float): Boolean {
        val t = findAt(x, y) ?: return false
        val act = t.activate ?: return false
        runCatching { act() }
        return true
    }


    /** 面板区域表：只用来判断「光点是否落在这块面板上」 */
    private val regions = LinkedHashMap<Any, androidx.compose.ui.geometry.Rect>()

    fun putRegion(key: Any, bounds: androidx.compose.ui.geometry.Rect) {
        synchronized(regions) { regions[key] = bounds }
    }

    fun removeRegion(key: Any) {
        synchronized(regions) { regions.remove(key) }
    }

    fun regionContains(key: Any, x: Float, y: Float): Boolean = synchronized(regions) {
        regions[key]?.contains(androidx.compose.ui.geometry.Offset(x, y)) ?: false
    }

    fun clear() { synchronized(targets) { targets.clear() } }
}

/**
 * 把控件登记进坐标表。焦点用外部传进来的 FocusRequester，动作就是它的 onClick。
 *
 * 用法与 hoverToFocus 相同，但多给一个 activate：光标悬停只管移焦点，
 * 扣扳机时才走 activate（保证一次扳机只触发一次）。
 */
fun Modifier.pointerTarget(
    key: Any,
    focusRequester: FocusRequester? = null,
    activate: (() -> Unit)? = null,
): Modifier = composed {
    val stableKey = remember(key) { key }
    DisposableEffect(stableKey) {
        onDispose { PointerTargets.remove(stableKey) }
    }
    this.onGloballyPositioned { coords ->
        // 每次排版都刷新：滚动/动画之后坐标就变了，只记一次会点偏
        PointerTargets.put(
            key = stableKey,
            bounds = coords.boundsInWindow(),
            focus = focusRequester?.let { fr -> { fr.requestFocus() } },
            activate = activate,
        )
    }
}

/**
 * 面板区域登记：只登记矩形，不参与焦点与点击。
 *
 * 用途是「光点指到才受控」——光点不在这个区域里时，摇杆的方向键不驱动这块面板
 *（父亲 2026-10-28 全局口径：光点指哪控哪）。
 */
fun Modifier.pointerRegion(key: Any): Modifier = composed {
    val stableKey = remember(key) { key }
    DisposableEffect(stableKey) { onDispose { PointerTargets.removeRegion(stableKey) } }
    this.onGloballyPositioned { coords -> PointerTargets.putRegion(stableKey, coords.boundsInWindow()) }
}

/** 光点此刻是否落在这个区域里；光标很久没动（遥控器场景）视为不限制 */
fun cursorInRegion(key: Any): Boolean {
    if (!PointerTargets.cursorActive()) return true
    return PointerTargets.regionContains(key, PointerTargets.cursorX, PointerTargets.cursorY)
}
