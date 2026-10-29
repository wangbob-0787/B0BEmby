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

    /*
     * 摇杆滚动的物理照 VR 版（panel/PanelLayer.kt 的 VR 摇杆段，父亲 2026-10-05 定稿）：
     *   过死区才滚；速度 = 3~12 格/秒，按 1.3 次方曲线过渡（轻推好控、推顶够快）；
     *   每渲染帧推进一次，位移按这一帧实际时长算；
     *   松手按残速线性减速，1 秒内停住，不急停。
     */
    private const val AXIS_DEAD_ZONE = 0.55f
    private const val RATE_MIN = 3f
    private const val RATE_MAX = 18f
    private const val RATE_CURVE = 1.3

    /** 松手后的滑行总时长与停下的阈值（格/秒） */
    private const val INERTIA_MS = 1000L
    private const val INERTIA_MIN_RATE = 0.8f

    /** 兜底：抬起事件万一丢失，推住最多滚这么久 */
    private const val HOLD_SAFETY_MS = 15_000L

    /**
     * 主线程把"这一帧该滚多少"交给 Activity（Activity 注入；为空则不派发）。
     *
     * @param dx,dy 方向，各取 -1 / 0 / 1
     * @param notches 这一帧的位移量（格），等于速度 × 帧时长；Activity 原样写进滚轮轴值
     */
    var scrollDispatcher: ((Float, Float, Float) -> Unit)? = null

    /**
     * 播放页标记（PlayerScreen 进出时切换）。
     *
     * 播放页的摇杆语义是"下=唤控制条、上交焦点系统、左右=快退快进"，不是滚列表，
     * 所以这里走方向键；其余页面才走滚轮。父亲 2026-10-29 报"往下要扫一下才出控制条"，
     * 就是播放页也被当成滚列表、键被滚轮吃掉的表现。
     */
    @Volatile
    var playerScreenActive = false

    /** 播放页：控制条是否可见、它的上边缘（像素），以及「点画面 = 播放/暂停」的回调。 */
    @Volatile var playerPanelVisible = false
    @Volatile var playerPanelTopPx = 0f

    /** 控制条与菜单的矩形（窗口坐标）。扳机落点做减法：两者之外才算「点画面」。 */
    @Volatile var playerPanelRect: android.graphics.RectF? = null
    @Volatile var playerMenuRect: android.graphics.RectF? = null
    @Volatile var playerTapToggle: (() -> Unit)? = null

    private val choreographer = android.view.Choreographer.getInstance()
    private var frameScheduled = false
    private var lastFrameAt = 0L
    private var pushRate = 0f
    private var dirX = 0f
    private var dirY = 0f
    private var holdFrom = 0L
    private var inertiaRate = 0f
    private var inertiaStartAt = 0L

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
     * 摇杆 → 滚动（照 VR 版物理，2026-10-29 重做两次的最终形态）。
     *
     * 事件只用来更新"往哪个方向推、推多深"，推进由每渲染帧的 [frameTick] 负责：
     * 推住不动也照常滚（旧写法"收到事件才补一格"在推住时等于静止），
     * 回中则进入惯性滑行。父亲的原话是"有时候滚得动有时候滚不动、按着动都不动、
     * 没有 VR 版那么丝滑"。
     */
    /** 最近是否在用摇杆轴数据（用它自己的帧引擎），光点位移那条要同时让开 */
    private var lastAxisAt = 0L
    fun axisInUse(): Boolean = SystemClock.uptimeMillis() - lastAxisAt < 200L

    fun onStick(event: MotionEvent) {
        lastAxisAt = SystemClock.uptimeMillis()
        val ax = axisOf(event, MotionEvent.AXIS_X, MotionEvent.AXIS_HAT_X)
        val ay = axisOf(event, MotionEvent.AXIS_Y, MotionEvent.AXIS_HAT_Y)
        val mag = kotlin.math.max(kotlin.math.abs(ax), kotlin.math.abs(ay))
        val now = SystemClock.uptimeMillis()
        if (mag < AXIS_DEAD_ZONE) {
            if (pushRate > 0f) {
                // 回中 = 松手：按松手瞬间的速度进入惯性，1 秒线性减速到停
                inertiaRate = pushRate
                inertiaStartAt = now
                pushRate = 0f
                scheduleFrame()
            }
            return
        }
        // 斜推只取更轴的一向，免得两轴一起乱滚
        val horizontal = kotlin.math.abs(ax) > kotlin.math.abs(ay)
        dirX = if (horizontal) (if (ax > 0f) 1f else -1f) else 0f
        dirY = if (horizontal) 0f else (if (ay > 0f) 1f else -1f)
        val t = ((mag - AXIS_DEAD_ZONE) / (1f - AXIS_DEAD_ZONE)).coerceIn(0f, 1f)
        val curved = Math.pow(t.toDouble(), RATE_CURVE.toDouble()).toFloat()
        pushRate = RATE_MIN + (RATE_MAX - RATE_MIN) * curved
        holdFrom = now
        scheduleFrame()
    }

    /** 停下并清掉惯性（退出播放页、界面切换时调用） */
    fun stopStick() {
        pushRate = 0f
        inertiaRate = 0f
        dirX = 0f
        dirY = 0f
    }

    private fun axisOf(event: MotionEvent, analog: Int, hat: Int): Float {
        val a = event.getAxisValue(analog)
        return if (kotlin.math.abs(a) > 0.01f) a else event.getAxisValue(hat)
    }

    private fun scheduleFrame() {
        if (frameScheduled) return
        frameScheduled = true
        choreographer.postFrameCallback(frameTick)
    }

    /**
     * 每帧一次：① 惯性阶段按时间线性减速；② 推住阶段按当前速度档滚。
     * 帧时长钳在 1..40ms，卡顿时不会一帧挪一大段。
     */
    private val frameTick = object : android.view.Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameScheduled = false
            val dtMs = if (lastFrameAt > 0L) {
                ((frameTimeNanos - lastFrameAt) / 1_000_000L).coerceIn(1L, 40L)
            } else {
                16L
            }
            lastFrameAt = frameTimeNanos
            val now = SystemClock.uptimeMillis()

            if (inertiaRate > 0f) {
                val elapsed = (now - inertiaStartAt).coerceAtLeast(0L)
                val rate = inertiaRate * (1f - elapsed.toFloat() / INERTIA_MS.toFloat())
                if (rate < INERTIA_MIN_RATE) {
                    inertiaRate = 0f
                    lastFrameAt = 0L
                    return
                }
                dispatch(rate * dtMs / 1000f)
                scheduleFrame()
                return
            }

            if (pushRate <= 0f) {
                lastFrameAt = 0L
                return
            }
            /*
             * 松手看门狗（父亲 2026-10-29：TV 上"按住滚动松手就停、没有惯性"）：
             * 投影遥控器的十字键在松开时不一定再发一个中位事件，只看"收到中位"就永远进不了惯性。
             * 超过 120 毫秒没有新的摇杆事件，就当作松手，按当前速度滑行出去。
             */
            if (now - lastAxisAt > 120L) {
                inertiaRate = pushRate
                inertiaStartAt = now - 120L
                pushRate = 0f
                lastFrameAt = frameTimeNanos
                scheduleFrame()
                return
            }
            if (now - holdFrom > HOLD_SAFETY_MS) {
                pushRate = 0f
                return
            }
            dispatch(pushRate * dtMs / 1000f)
            scheduleFrame()
        }
    }

    private fun dispatch(notches: Float) {
        scrollDispatcher?.invoke(dirX, dirY, notches.coerceIn(0.02f, 1.5f))
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
