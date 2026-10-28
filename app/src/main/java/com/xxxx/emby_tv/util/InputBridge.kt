package com.xxxx.emby_tv.util

import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 遥控器 + 手柄 + 指针（PICO 头显的光标）三套输入的统一桥。
 *
 * 目标（父亲 2026-10-28 定）：同一份 APK 装到电视上用遥控器正常，装到 PICO 上用手柄也能操作。
 *
 * 三层事实（VR 侧 2026-10-04 三层取证，见
 * Project/B0BEmby-VR/docs/plans/b0bemby-vr-plan-2026-10-03.md「手柄 ↔ 遥控器 统一语义」）：
 *  1. 电视遥控器 → 标准方向键 / OK / 返回键，Compose 焦点系统直接可用，不需要额外映射；
 *  2. 普通安卓手柄（USB / 蓝牙）→ KeyEvent(BUTTON_A/B…) 与 MotionEvent 摇杆轴，
 *     系统不会把它当方向键，必须在这里转成方向键 / OK / 返回；
 *  3. PICO 在 2D 面板模式下不发手柄按键，只把「光标 + 扳机」压成触摸事件给应用，
 *     所以头显上靠的是指针：光标指到哪儿就把焦点放到哪儿，扣扳机 = 点一下。
 *
 * 这个文件只做机械转换，不掺业务。业务侧的交互语义（播放中左右键快进、B 键退一层等）
 * 仍由各页面自己的按键处理负责。
 */
object InputBridge {

    /** 摇杆死区：PS/Xbox 手柄轻推的抖动不该当成方向键 */
    // 死区照 VR 版（0.35）：太大导致摇杆轻拨没反应，太小会漂移误触
    private const val AXIS_DEAD_ZONE = 0.35f

    /** 摇杆按住后，第一次重复之前的等待（毫秒）。短一点手感才跟手 */
    private const val AXIS_FIRST_DELAY_MS = 200L

    /** 按住期间的基础重复间隔（毫秒） */
    private const val AXIS_REPEAT_MS = 45L

    /** 按住超过这个时长后加快到 [AXIS_FAST_MS]，长列表才滚得动 */
    private const val AXIS_ACCEL_AFTER_MS = 500L
    private const val AXIS_FAST_MS = 25L

    /** 连按保险：一直推着不松最多走这么久，防止手柄丢"回中"事件导致焦点一路飞走 */
    private const val AXIS_RUNAWAY_MS = 8000L

    private var axisKey = 0
    private var axisKeyAt = 0L
    private var axisHeldFrom = 0L
    private var axisNextAt = 0L
    private val axisTimer = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * 主线程把"一次滚动格"交给 Activity（Activity 注入；为空则不派发）。
     *
     * 参数是方向与幅度：(dx, dy) 各取 -1.5..1.5，正 = 右/下。
     * Activity 侧先用滚轮派发给光点底下的容器（列表按像素滚、焦点不动），
     * 没人接才退化成方向键 —— 摇杆与光点两条路因此走同一套滚动实现。
     */
    var scrollDispatcher: ((Float, Float) -> Unit)? = null

    /** 摇杆回中时回调（Activity 用来接滚动惯性） */
    var onRelease: (() -> Unit)? = null

    private var axisX = 0f
    private var axisY = 0f

    /**
     * 手柄按键 → 遥控器语义。
     *
     * A / 扳机（R2、L2）= OK：PICO 与常见手柄都把「确认」放在这两处；
     * B = 返回；START = 菜单键（播放页开关控制条）。
     * 其它键（X/Y/摇杆按下/肩键）不映射，避免误触出奇怪行为。
     */
    /**
     * 这颗键是不是「确认」（A / 扳机 / OK）。
     *
     * 头显上有光标时，确认键不能走焦点点击，要在光标处注入一次触摸；所以先在这里认身份。
     */
    fun isConfirmKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_L2,
        KeyEvent.KEYCODE_BUTTON_1,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_DPAD_CENTER -> true

        else -> false
    }

    fun mapGamepadKey(event: KeyEvent): KeyEvent? {
        val mapped = when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_L2 -> KeyEvent.KEYCODE_DPAD_CENTER

            KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK

            KeyEvent.KEYCODE_BUTTON_START -> KeyEvent.KEYCODE_MENU

            else -> return null
        }
        return KeyEvent(
            event.downTime,
            event.eventTime,
            event.action,
            mapped,
            event.repeatCount,
            event.metaState,
            event.deviceId,
            event.scanCode,
            event.flags
        )
    }

    /**
     * 摇杆轴 → 方向键（定时驱动，2026-10-29 重做）。
     *
     * 旧写法"收到运动事件才发键"，在两个前提上都不成立：
     *  1. 手柄只会在数值变化时发事件 —— 摇杆推到某个位置停住，事件就断了，于是按住不动；
     *  2. 事件频率取决于设备 —— 有的 60Hz、有的只在变化时来一次，重复节奏完全不可控。
     * 父亲的原话是"有时候滚得动，有时候滚不动，按着动都不动"。
     * 现在改成：事件只用来确定"往哪个方向推着"，重复由主线程定时器发，推着就一直走，
     * 回中（轴值回到死区内）才停 —— 这样节奏稳定，按住也一定在动。
     */
    fun onStick(event: MotionEvent) {
        axisX = event.getAxisValue(MotionEvent.AXIS_X)
            .takeIf { kotlin.math.abs(it) > 0.01f } ?: event.getAxisValue(MotionEvent.AXIS_HAT_X)
        axisY = event.getAxisValue(MotionEvent.AXIS_Y)
            .takeIf { kotlin.math.abs(it) > 0.01f } ?: event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val key = directionOf(event)
        val now = SystemClock.uptimeMillis()
        if (key == 0) {
            val was = axisKey != 0
            stopStick()
            if (was) onRelease?.invoke()
            return
        }
        if (key != axisKey) {
            axisKey = key
            axisKeyAt = now
            axisHeldFrom = now
            emit(now)                 // 换方向：立刻走一格，手感才跟手
            schedule(AXIS_FIRST_DELAY_MS)
        } else if (axisKeyAt == 0L) {
            axisKeyAt = now
            axisHeldFrom = now
            emit(now)
            schedule(AXIS_FIRST_DELAY_MS)
        }
    }

    /** 摇杆回中：停掉重复 */
    fun stopStick() {
        axisKey = 0
        axisKeyAt = 0L
        axisHeldFrom = 0L
        axisTimer.removeCallbacksAndMessages(null)
    }

    private fun directionOf(event: MotionEvent): Int {
        val ax = event.getAxisValue(MotionEvent.AXIS_X)
        val ay = event.getAxisValue(MotionEvent.AXIS_Y)
        val hx = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hy = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val h = when {
            hx > 0.5f || ax > AXIS_DEAD_ZONE -> KeyEvent.KEYCODE_DPAD_RIGHT
            hx < -0.5f || ax < -AXIS_DEAD_ZONE -> KeyEvent.KEYCODE_DPAD_LEFT
            else -> 0
        }
        val v = when {
            hy > 0.5f || ay > AXIS_DEAD_ZONE -> KeyEvent.KEYCODE_DPAD_DOWN
            hy < -0.5f || ay < -AXIS_DEAD_ZONE -> KeyEvent.KEYCODE_DPAD_UP
            else -> 0
        }
        // 斜着推先走横向：电视界面以横向排布为主
        return if (h != 0) h else v
    }

    /**
     * 走一格滚动。幅度按推杆深浅算（0.5..1.5 倍），推得深就走得快，
     * 这样同一个重复节拍下也有快慢层次，不像开关一样只有"一格/空格"。
     */
    private fun emit(now: Long) {
        val dx: Float
        val dy: Float
        when (axisKey) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> { dx = 1f; dy = 0f }
            KeyEvent.KEYCODE_DPAD_LEFT -> { dx = -1f; dy = 0f }
            KeyEvent.KEYCODE_DPAD_DOWN -> { dx = 0f; dy = 1f }
            else -> { dx = 0f; dy = -1f }
        }
        val push = if (dx != 0f) kotlin.math.abs(axisX) else kotlin.math.abs(axisY)
        val scale = (0.5f + push.coerceIn(0f, 1f)).coerceIn(0.5f, 1.5f)
        scrollDispatcher?.invoke(dx * scale, dy * scale)
    }

    private fun schedule(delay: Long) {
        axisNextAt = SystemClock.uptimeMillis() + delay
        axisTimer.removeCallbacksAndMessages(null)
        axisTimer.postDelayed(tick, delay)
    }

    private val tick = object : Runnable {
        override fun run() {
            val key = axisKey
            if (key == 0) return
            val now = SystemClock.uptimeMillis()
            if (now - axisHeldFrom > AXIS_RUNAWAY_MS) {
                stopStick()
                return
            }
            emit(now)
            val held = now - axisHeldFrom
            schedule(if (held > AXIS_ACCEL_AFTER_MS) AXIS_FAST_MS else AXIS_REPEAT_MS)
        }
    }

    /** 这次运动事件里有没有摇杆/十字键数据（纯鼠标移动事件不必喂给方向键映射） */
    fun hasStickData(event: MotionEvent): Boolean {
        if (event.source and android.view.InputDevice.SOURCE_JOYSTICK == 0 &&
            event.source and android.view.InputDevice.SOURCE_GAMEPAD == 0 &&
            event.source and android.view.InputDevice.SOURCE_DPAD == 0
        ) return false
        return event.actionMasked == MotionEvent.ACTION_MOVE
    }
}

/**
 * 指针悬停 → 拿焦点（PICO 光标、以及在电视上插鼠标时都走这条）。
 *
 * 遥控器场景不会触发（没有指针悬停），所以对电视端行为零影响。
 */
fun Modifier.hoverToFocus(focusRequester: FocusRequester): Modifier =
    this
        // 登记进控件坐标表：指针事件未必落到控件自己的命中区（例如大片头只覆盖左半屏、
        // 浮层压在内容上），查表这条路能兜住，且随每次排版刷新坐标（列表滚动也准）。
        // activate 留空 = 扣扳机时退回"给当前焦点补一次确认键"，与控件原本的 onClick 一致。
        .pointerTarget(key = focusRequester, focusRequester = focusRequester)
        .pointerInput(focusRequester) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Enter) {
                        runCatching { focusRequester.requestFocus() }
                    }
                }
            }
        }
