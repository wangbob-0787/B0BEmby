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

    /** 摇杆按住不放时的重复间隔（毫秒）：与遥控器连按的节奏接近，列表能滚、又不至于飞掉 */
    private const val AXIS_REPEAT_MS = 200L   // 与 VR 的 REPEAT_MS 一致

    private var lastAxisKey = 0
    private var lastAxisAt = 0L

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
     * 摇杆轴 → 方向键。
     *
     * 只在方向变化时立刻发一次，按住不放按 [AXIS_REPEAT_MS] 重复；
     * 回中时不发任何键（Compose 的焦点不会因为松开摇杆而丢）。
     * 返回 null 表示这次事件没有可用的方向。
     */
    fun axisToDpadKey(event: MotionEvent): KeyEvent? {
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
        // 斜着推时先走横向（电视焦点大多横向排布，竖排也能靠下一个事件走到）
        val key = if (h != 0) h else v
        if (key == 0) {
            lastAxisKey = 0
            return null
        }
        val now = SystemClock.uptimeMillis()
        // 方向没变且还没到重复间隔 → 这次不发
        if (key == lastAxisKey && now - lastAxisAt < AXIS_REPEAT_MS) return null
        lastAxisKey = key
        lastAxisAt = now
        return KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0)
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
    this.pointerInput(focusRequester) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Enter) {
                    runCatching { focusRequester.requestFocus() }
                }
            }
        }
    }
