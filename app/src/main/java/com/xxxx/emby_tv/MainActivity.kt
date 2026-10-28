package com.xxxx.emby_tv

import Emby_tvTheme
import com.xxxx.emby_tv.ui.LibraryScreen
import com.xxxx.emby_tv.ui.LoginScreen
import com.xxxx.emby_tv.ui.PlayerScreen

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xxxx.emby_tv.ui.AccountScreen
import com.xxxx.emby_tv.ui.MediaDetailScreen
import com.xxxx.emby_tv.ui.HomeScreen

import com.xxxx.emby_tv.ui.SearchScreen
import com.xxxx.emby_tv.ui.components.BuildGradientBackground
import com.xxxx.emby_tv.ui.components.ExitConfirmDialog
import com.xxxx.emby_tv.ui.components.Loading
import com.xxxx.emby_tv.ui.theme.ThemeColorManager
import com.xxxx.emby_tv.ui.viewmodel.MainViewModel
import com.xxxx.emby_tv.ui.viewmodel.HomeViewModel
import com.xxxx.emby_tv.ui.viewmodel.LoginViewModel
import com.xxxx.emby_tv.ui.viewmodel.LibraryViewModel
import com.xxxx.emby_tv.ui.viewmodel.DetailViewModel
import com.xxxx.emby_tv.ui.viewmodel.PlayerViewModel
import com.xxxx.emby_tv.ui.viewmodel.SearchViewModel
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.xxxx.emby_tv.util.DiagLog
import com.xxxx.emby_tv.util.InputBridge


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 强制设置为横屏模式
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        enableEdgeToEdge()
        setContent {
            EmbyTvApp()
        }
    }

    // ── 指针点击：把「扳机/点击」显式换算成触摸，注入 Compose 根视图 ──────────
    //
    // 事实（VR 版 2026-10-04 抓 /dev/input/event4 实测）：头显的扳机在系统层不是 KeyEvent，
    // 而是 BTN_TOUCH DOWN/UP 的一次触摸点击，坐标就是光标位置；A 键与扳机在系统层分不开。
    // TV 版是遥控器焦点模型，焦点控件不会因为「来了一个坐标」自己按下去 —— 悬停只负责移焦点，
    // 真正触发必须把点击按窗口坐标派发进 Compose（Surface(onClick)/clickable 认触摸）。
    //
    // 分工：悬停事件照旧交给 super（移焦点、亮点）；按下/抬起由这里接管，注入一次干净的点击，
    // 同一帧不会既走焦点点击又走注入，避免双击。
    private var cursorX = 0f
    private var cursorY = 0f
    private var cursorAt = 0L
    private var pressPending = false
    private var pressX = 0f
    private var pressY = 0f
    private var lastInjectAt = 0L
    private var traceAt = 0L
    private var motionPressed = false
    // 摇杆/拖动：按下后挪得超过阈值就算「拖动＝滚动」，抬手不再算点击
    private var dragMoved = false
    private var scrollAccumY = 0f
    private var scrollAccumX = 0f
    private var lastScrollAt = 0L
    // 上一次移动的时间：算光点速度用（决定滚动快慢）
    private var lastMoveAt = 0L
    private val scrollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastCursorSpeedPxPerSec = 0f
    private var lastScrollAxis = 0      // 1=上下 0=左右
    private var lastScrollSign = 0


    /** 光标位置是否新鲜（3 秒内动过）；新鲜才认为「用户在指东西」，否则按遥控器处理 */
    private fun cursorFresh(): Boolean =
        cursorAt > 0L && SystemClock.uptimeMillis() - cursorAt < 3_000L

    private fun recordCursor(ev: MotionEvent) {
        cursorX = ev.x
        cursorY = ev.y
        cursorAt = SystemClock.uptimeMillis()
        // 光标指哪儿，焦点就落到哪儿：查控件坐标表（VR 同款机制）
        com.xxxx.emby_tv.util.PointerTargets.noteCursor(cursorX, cursorY)
        com.xxxx.emby_tv.util.PointerTargets.focusAt(cursorX, cursorY)
    }

    /** 原始事件取证：DOWN/UP 必记，MOVE/HOVER 限流，避免刷屏（父亲要求先证明事件在不在） */
    private fun traceTouch(ev: MotionEvent) {
        val now = SystemClock.uptimeMillis()
        val xy = "(" + ev.x.toInt() + "," + ev.y.toInt() + ")"
        val src = "0x" + Integer.toHexString(ev.source)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                traceAt = now
                DiagLog.w(this, "input", "touch DOWN " + xy + " source=" + src)
            }
            MotionEvent.ACTION_UP ->
                DiagLog.w(this, "input", "touch UP " + xy + " source=" + src)
            MotionEvent.ACTION_CANCEL ->
                DiagLog.w(this, "input", "touch CANCEL " + xy + " source=" + src)
            else -> if (now - traceAt >= 200L) {
                traceAt = now
                DiagLog.w(
                    this, "input",
                    "touch action=" + ev.actionMasked + " " + xy + " source=" + src +
                        " btn=0x" + Integer.toHexString(ev.buttonState)
                )
            }
        }
    }

    /**
     * 在「当前焦点」上补一次确认键（DPAD_CENTER）。
     *
     * 为什么不注入鼠标点击：TV 版的控件全是 TV 库的 Surface/Button，它们的语义是
     * 「焦点 + OK」，鼠标式点击实测不触发它们（VR 版 2026-10-04 也踩过这条：
     * 电视库的 Surface 吃鼠标事件但不动作，最后靠补 OK 才成）。而光标悬停已经把焦点
     * 搬到指针所指的控件上，所以补一次 OK 就等于「在光标处点了一下」，且只发一次不会双击。
     */
    private fun fireConfirm(source: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastInjectAt < 150L) return      // 防抖：同一次扳机不重复触发
        lastInjectAt = now
        /*
         * 触发只能有一次：先查控件坐标表——命中就直接执行控件动作并返回，**不再**补发确认键；
         * 只有查不到（空白处或没登记的控件）才退回遥控器语义。两个分支同时跑就是一次扳机点两下。
         */
        if (com.xxxx.emby_tv.util.PointerTargets.activateAt(cursorX, cursorY)) {
            DiagLog.w(this, "input", "扳机命中坐标表，已直接触发（来源=" + source + "）")
            return
        }
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0)
        val up = KeyEvent(now, now + 40L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0)
        super.dispatchKeyEvent(down)
        super.dispatchKeyEvent(up)
        DiagLog.w(
            this, "input",
            "确认键已派发（来源=" + source + " 光标=(" + cursorX.toInt() + "," + cursorY.toInt() + ")）"
        )
    }

    /**
     * 把拖动转成方向键：电视列表的滚动方式就是「焦点往下走一格」，
     * 所以一步一个 DPAD_UP/DOWN/LEFT/RIGHT，列表自己会跟着滚。
     * 节流保证一次拖动不会把列表甩飞。
     */

    /**
     * 滚动惯性：摇杆松手后按残速继续发方向键，间隔逐渐拉长、约 0.8 秒停住。
     * 与「按光点速度决定步频」配套，合起来就是先加速、松手再减速的手感。
     */
    private fun startScrollInertia() {
        val speed = lastCursorSpeedPxPerSec
        if (speed < 120f) return
        var remaining = 8
        var stepMs = 45L
        val runner = object : Runnable {
            override fun run() {
                if (remaining <= 0) return
                remaining--
                if (dragMoved) return                      // 用户又拨动了：交给实时逻辑
                doScroll(
                    if (lastScrollAxis == 1) 0f else (if (lastScrollSign > 0) -1f else 1f),
                    if (lastScrollAxis == 1) (if (lastScrollSign > 0) -1f else 1f) else 0f,
                    0.16f
                )
                stepMs = (stepMs * 1.25).toLong()
                scrollHandler.postDelayed(this, stepMs)
            }
        }
        scrollHandler.postDelayed(runner, stepMs)
    }


    /**
     * 摇杆当鼠标滚轮用（照 VR 的 PanelLayer）：向落点派发一次滚轮事件，
     * 由光点下面的可滚容器自己滚 —— 焦点完全不动。
     *
     * 为什么不能发方向键：方向键由焦点系统消费，系统会一边滚一边把焦点挪走，
     * 表现就是页面一跳一跳、焦点不跟光点（父亲 2026-10-28 实机）。
     *
     * @return true = 有容器接住了滚轮，false = 没人接（调用方再退化为方向键）
     */

    /**
     * 一个滚动格：先派发滚轮给光点底下的容器（焦点不动）；没人接才退化发方向键。
     * 落点用摇杆推下去的起点，不用光点当前可能已顶到边缘的位置（照 VR）。
     */
    private fun doScroll(dx: Float, dy: Float, notches: Float) {
        val x = if (pressX > 0f) pressX else cursorX
        val y = if (pressY > 0f) pressY else cursorY
        // 没有光点（投影 + 遥控器/手柄）时落到屏幕中心：列表几乎都铺在页面中间，
        // 否则落在 (0,0) 上没人接，永远退化成焦点翻牌（父亲 2026-10-29 报的"按钮/格子感"）
        val vw = window.decorView.width
        val vh = window.decorView.height
        val px = if (x > 0f && y > 0f) x else vw / 2f
        val py = if (x > 0f && y > 0f) y else vh / 2f
        // 滚轮量竖向取负、横向取正（照 VR：向下推 = 列表往下走）
        val h = if (dx != 0f) dx * notches else 0f
        val v = if (dy != 0f) -dy * notches else 0f
        if (fireWheel(h, v, px, py)) return
        /*
         * 滚轮没人接时先自己按像素滚（2026-10-29 父亲实测：首页上下左右拨都是焦点在跳）。
         * 界面把当前页的竖向列表和当前有焦点的那一行注册在 PixelScroll 上，
         * 这里直接 scrollBy 一个像素量，焦点不动，跟 VR 的手感一致。
         * 一格滚轮按 56dp 折算，与系统滚轮一格的观感接近。
         */
        val pxPerNotch = 56f * resources.displayMetrics.density
        if (dy != 0f && com.xxxx.emby_tv.util.PixelScroll.verticalBy(dy * notches * pxPerNotch)) return
        if (dx != 0f && com.xxxx.emby_tv.util.PixelScroll.horizontalBy(dx * notches * pxPerNotch)) return
        /*
         * 到此为止，不再退化成方向键（父亲 2026-10-29 01:25 明确：焦点移动全部取消，
         * 摇杆只按像素滚）。方向键由焦点系统消费，一旦发出去就是「焦点走一格」，
         * 与像素滚动的手感互相打架，所以这一路彻底不接。
         */
        DiagLog.w(this, "input", "滚轮与像素滚动都没接住 → 本次不动（不发方向键）")
    }

    private fun fireWheel(dx: Float, dy: Float, x: Float, y: Float): Boolean {
        val now = SystemClock.uptimeMillis()
        val props = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_MOUSE
        }
        val coords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            // 滚轮量必须写在坐标对象上，否则系统读不到
            setAxisValue(MotionEvent.AXIS_VSCROLL, dy)
            setAxisValue(MotionEvent.AXIS_HSCROLL, dx)
        }
        val ev = MotionEvent.obtain(
            now, now, MotionEvent.ACTION_SCROLL, 1,
            arrayOf(props), arrayOf(coords),
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0
        )
        val handled = runCatching {
            window.decorView.dispatchGenericMotionEvent(ev)
        }.getOrDefault(false)
        ev.recycle()
        return handled
    }

    /**
     * 播放页的摇杆语义：一次一格方向键（下唤控制条 / 上音量 / 左右快退快进）。
     * 节拍 90ms，与遥控器连按的节奏一致。
     */
    private var lastStickKeyAt = 0L

    private fun stickAsKeys(dx: Float, dy: Float) {
        val now = SystemClock.uptimeMillis()
        if (now - lastStickKeyAt < 90L) return
        lastStickKeyAt = now
        /*
         * 先比大小再看方向：斜着往下拨时 dx 常有一点残余，按「先看 x」的顺序会被判成左右，
         * 表现就是往下拨唤不出控制条、反而在快进快退（父亲 2026-10-29 报的现象）。
         * 竖向占优就算竖向，横向明显过半才算横向。
         */
        val code = if (kotlin.math.abs(dy) >= kotlin.math.abs(dx)) {
            if (dy > 0f) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP
        } else {
            if (dx > 0f) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        }
        fireScroll(code)
    }

    private fun fireScroll(keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        if (now - lastScrollAt < 80L) return
        lastScrollAt = now
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up = KeyEvent(now, now + 40L, KeyEvent.ACTION_UP, keyCode, 0)
        super.dispatchKeyEvent(down)
        super.dispatchKeyEvent(up)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        traceTouch(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_ENTER -> recordCursor(event)
            MotionEvent.ACTION_DOWN -> {
                if (cursorFresh()) {
                    // 用光标的落点当按点：头显上「看着指哪」才是用户的本意
                    pressX = cursorX
                    pressY = cursorY
                    dragMoved = false
                    scrollAccumX = 0f
                    scrollAccumY = 0f
                    pressPending = true
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressPending) {
                    // 摇杆拨动在头显上就是「按下 + 移动 + 抬起」。
                    // 挪过阈值 = 用户想滚页面，不是想点：这一步开始就按滚动处理，抬手也不再补确认键。
                    val slop = ViewConfiguration.get(this).scaledTouchSlop
                    val dx = event.x - pressX
                    val dy = event.y - pressY
                    if (!dragMoved && (kotlin.math.abs(dx) > slop || kotlin.math.abs(dy) > slop)) {
                        dragMoved = true
                        DiagLog.w(this, "input", "开始拖动→按滚动处理")
                    }
                    if (dragMoved) {
                        scrollAccumY += dy
                        scrollAccumX += dx
                        pressX = event.x
                        pressY = event.y
                        /*
                         * 滚动快慢跟光点速度走（照 VR）：摇杆没有力度值，只能拿光点每秒移动多少像素换算。
                         * 每 120 像素/秒记 1 格/秒，封顶 20 格/秒 —— 轻推慢滚、猛推快滚。
                         * 攒够一格才滚一次，所以慢推时格与格之间自然拉得开。
                         */
                        val moveNow = SystemClock.uptimeMillis()
                        val moveDt = if (lastMoveAt > 0L) (moveNow - lastMoveAt).coerceIn(8L, 100L) else 16L
                        lastMoveAt = moveNow
                        val cursorSpeed = kotlin.math.max(kotlin.math.abs(dx), kotlin.math.abs(dy)) * 1000f / moveDt
                        lastCursorSpeedPxPerSec = cursorSpeed
                        val vertical = kotlin.math.abs(dy) >= kotlin.math.abs(dx)
                        lastScrollAxis = if (vertical) 1 else 0
                        lastScrollSign = if (vertical) (if (dy > 0) 1 else -1) else (if (dx > 0) 1 else -1)
                        val rate = (cursorSpeed / 120f).coerceIn(1f, 20f)   // 格/秒
                        scrollAccumY += rate * moveDt / 1000f
                        pressX = event.x
                        pressY = event.y
                        if (scrollAccumY >= 1f) {
                            // 方向口径（父亲 2026-10-28 实机）：摇杆往下 = 往下滚，往右 = 往右滚。
                            if (vertical) {
                                doScroll(0f, if (dy > 0) -1f else 1f, 0.16f)
                            } else {
                                doScroll(if (dx > 0) -1f else 1f, 0f, 0.16f)
                            }
                            scrollAccumY = 0f
                        }
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                // 松手后按最后一段的速度继续滑一小段（线性减速，约 0.8 秒），推得猛滑得远、轻推立刻停
                if (dragMoved) startScrollInertia()
                if (pressPending) {
                    pressPending = false
                    // 拖动过的手抬起来 = 滚完了，不当作点击
                    if (!dragMoved) fireConfirm("触摸抬起")
                    dragMoved = false
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> if (pressPending) {
                pressPending = false
                dragMoved = false
                return true
            }
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * 手柄按键：有光标时扳机/A 走「在光标处点一下」，没有光标时退回遥控器语义
     * （A/扳机 = OK、B = 返回、START = 菜单）。
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (cursorFresh() && InputBridge.isConfirmKey(event.keyCode)) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                pressPending = true
                pressX = cursorX
                pressY = cursorY
                return true
            }
            if (event.action == KeyEvent.ACTION_UP) {
                // 同一次动作可能已经由触摸路径处理过，pressPending 为 false 就跳过
                val pending = pressPending
                pressPending = false
                if (pending) fireConfirm("手柄确认键")
                return true
            }
        }
        val mapped = InputBridge.mapGamepadKey(event)
        return super.dispatchKeyEvent(mapped ?: event)
    }

    /**
     * 摇杆 → 方向键；同时兜住「悬停移动里夹着按键状态」这类手柄（有的设备把扳机塞在 buttonState 里）。
     *
     * 只在事件源确实是手柄/摇杆/十字键时才转方向（鼠标与头显光标的事件不能当方向键，
     * 否则光标一动焦点就乱跳）。
     */
    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_HOVER_MOVE) recordCursor(ev)

        // 按下状态变化：按下记按点，抬起注入点击（与触摸路径共用 pressPending，天然去重）
        val pressedNow = (ev.buttonState and (MotionEvent.BUTTON_PRIMARY or MotionEvent.BUTTON_SECONDARY)) != 0
        if (pressedNow != motionPressed) {
            motionPressed = pressedNow
            if (pressedNow) {
                pressPending = true
                pressX = cursorX
                pressY = cursorY
                DiagLog.w(this, "input", "buttonState 按下 -> 记按点 (" + pressX.toInt() + "," + pressY.toInt() + ")")
            } else if (pressPending) {
                pressPending = false
                fireConfirm("buttonState 抬起")
            }
        }

        /*
         * 摇杆：只报方向给 InputBridge，重复节奏由它自己的定时器发（2026-10-29 改）。
         * 原来"收到运动事件才补一个方向键"，手柄按住不动就不再发事件，于是按着不动；
         * 事件频率也因设备而异，滚动手感时快时慢。
         */
        if (InputBridge.hasStickData(ev)) {
            /*
             * 摇杆走滚动，不发方向键（父亲 2026-10-29 报"TV 滚动没 VR 丝滑"）。
             * 方向键由焦点系统消费：一步一格卡、跳完列表再把那张卡拉进视野，天生一格一格；
             * 滚轮是容器自己按像素滚、焦点不动 —— 和光点那条路同一套实现，节拍与加减速、
             * 松手滑行都由 InputBridge 的逐帧引擎给（照 VR 版物理）。
             * 播放页例外：那里的摇杆本来就是方向语义（下唤控制条、上音量、左右快退快进）。
             */
            InputBridge.scrollDispatcher = { dx, dy, notches ->
                if (InputBridge.playerScreenActive) stickAsKeys(dx, dy) else doScroll(dx, dy, notches)
            }
            InputBridge.onStick(ev)
        }
        return super.dispatchGenericMotionEvent(ev)
    }
}

@Composable
fun EmbyTvApp() {
    val navController = rememberNavController()

    // 使用新的 MainViewModel
    val mainViewModel: MainViewModel = viewModel()

    // 响应式监听状态变化
    val isLoaded = mainViewModel.isLoaded
    val isLoggedIn = mainViewModel.isLoggedIn

    // 初始化加载
    LaunchedEffect(Unit) {
        mainViewModel.initialize()
    }

    // 监听状态变化并自动导航
    LaunchedEffect(isLoaded, isLoggedIn) {
        when {
            !isLoaded -> {
                if (navController.currentDestination?.route != "loading") {
                    navController.navigate("loading") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            isLoggedIn -> {
                if (navController.currentDestination?.route != "home") {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            else -> {
                if (navController.currentDestination?.route != "login") {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    // 获取当前主题色
    val context = LocalContext.current
    val currentThemeColor =
        ThemeColorManager.getThemeColorById(context, mainViewModel.currentThemeId)

    // === 退出确认（父亲 2026-10-02：连按返回键会直接把应用退掉）===
    // 只在首页拦返回键；弹层开着时返回键先关弹层（误按返回不会退出应用）。
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showExitConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = currentRoute == "home" || showExitConfirm) {
        if (showExitConfirm) showExitConfirm = false else showExitConfirm = true
    }

    Emby_tvTheme(themeColor = currentThemeColor) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BuildGradientBackground(context = context, themeColor = currentThemeColor) {
                NavHost(
                    navController = navController,
                    startDestination = "loading"
                ) {
                    // 加载页面
                    composable("loading") {
                        Loading()
                    }

                    // 登录页面
                    composable("login") {
                        val loginViewModel: LoginViewModel = viewModel()
                        LoginScreen(
                            loginViewModel = loginViewModel,
                            mainViewModel = mainViewModel,
                            navController = navController
                        )
                    }

                    // 主页面
                    composable("home") { backStackEntry ->
                        val homeViewModel: HomeViewModel = viewModel()

                        // 使用 rememberSaveable 保存是否已经初始加载过
                        var isInitialLoaded by rememberSaveable { mutableStateOf(false) }

                        // 监听路由返回，延迟刷新
                        LaunchedEffect(backStackEntry) {
                            // 如果不是首次加载（说明是从其他页面返回），则刷新数据
                            if (isInitialLoaded) {
                                homeViewModel.loadData()
                            } else {
                                // 首次加载，不刷新
                                isInitialLoaded = true
                            }
                        }

                        HomeScreen(
                            homeViewModel = homeViewModel,
                            mainViewModel = mainViewModel,
                            navController = navController,
                            onSwitchAccount = {
                                navController.navigate("account")
                            }
                        )
                    }

                    // 账号管理页面
                    composable("account") {
                        val loginViewModel: LoginViewModel = viewModel()
                        AccountScreen(
                            mainViewModel = mainViewModel,
                            savedAccounts = loginViewModel.savedAccounts,
                            currentAccountId = loginViewModel.currentAccountId,
                            onSwitchAccount = { accountId ->
                                loginViewModel.switchAccount(
                                    accountId = accountId,
                                    onSuccess = {
                                        // 切换成功后回到首页并刷新
                                        navController.navigate("home") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    onError = { /* 可选：显示错误提示 */ }
                                )
                            },
                            onDeleteAccount = { accountId ->
                                loginViewModel.removeAccount(accountId)
                            },
                            onAddAccount = {
                                // 退出当前账号，进入登录页面添加新账号
                                mainViewModel.logout()
                            }
                        )
                    }

                    // 代理设置页与更新页已取消（父亲 2026-10-28）：代理并入登录页的去处，
                    // 更新改成顶栏版本号上的一步式更新，不再单独占一页。

                    // 搜索页面
                    composable("search") {
                        val searchViewModel: SearchViewModel = viewModel()
                        val loginViewModel: LoginViewModel = viewModel()
                        SearchScreen(
                            searchViewModel = searchViewModel,
                            loginViewModel = loginViewModel,
                            mainViewModel = mainViewModel,
                            onNavigateToSeries = { seriesId ->
                                navController.navigate("series/$seriesId")
                            }
                        )
                    }

                    // 媒体库页面
                    composable(
                        "library/{libraryId}?libraryName={libraryName}&type={type}",
                        arguments = listOf(
                            navArgument("libraryId") { type = NavType.StringType },
                            navArgument("libraryName") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("type") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        val libraryId = backStackEntry.arguments?.getString("libraryId") ?: ""
                        val libraryName = backStackEntry.arguments?.getString("libraryName") ?: ""
                        val type = backStackEntry.arguments?.getString("type") ?: ""
                        val libraryViewModel: LibraryViewModel = viewModel()

                        LibraryScreen(
                            parentId = libraryId,
                            title = libraryName,
                            type = type,
                            libraryViewModel = libraryViewModel,
                            onNavigateToSeries = { seriesId ->
                                navController.navigate("series/$seriesId")
                            },
                            onNavigateToPlayer = { channelId ->
                                navController.navigate("player/$channelId?position=0")
                            }
                        )
                    }

                    // 剧集详情页面
                    composable(
                        "series/{seriesId}",
                        arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
                        val detailViewModel: DetailViewModel = viewModel()
                        MediaDetailScreen(
                            seriesId = seriesId,
                            detailViewModel = detailViewModel,
                            onNavigateToSeries = { nestedSeriesId ->
                                navController.navigate("series/$nestedSeriesId")
                            },
                            onNavigateToPlayer = { mediaItem ->
                                val id = mediaItem.id ?: ""
                                val position = mediaItem.userData?.playbackPositionTicks ?: 0L
                                navController.navigate("player/$id?position=$position")
                            }
                        )
                    }

                    // 通用媒体详情页面
                    composable(
                        "media/{mediaId}",
                        arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                        val detailViewModel: DetailViewModel = viewModel()

                        MediaDetailScreen(
                            seriesId = mediaId,
                            detailViewModel = detailViewModel,
                            onNavigateToSeries = { nestedSeriesId ->
                                navController.navigate("series/$nestedSeriesId")
                            },
                            onNavigateToPlayer = { mediaItem ->
                                val id = mediaItem.id ?: ""
                                val position = mediaItem.userData?.playbackPositionTicks ?: 0L
                                navController.navigate("player/$id?position=$position")
                            }
                        )
                    }

                    // 播放器页面
                    composable(
                        "player/{mediaId}?position={position}",
                        arguments = listOf(
                            navArgument("mediaId") { type = NavType.StringType },
                            navArgument("position") {
                                type = NavType.LongType
                                defaultValue = 0L
                            }
                        )
                    ) { backStackEntry ->
                        val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
                        val playbackPositionTicks =
                            backStackEntry.arguments?.getLong("position") ?: 0L
                        val playerViewModel: PlayerViewModel = viewModel()

                        PlayerScreen(
                            mediaId = mediaId,
                            playbackPositionTicks = playbackPositionTicks,
                            playerViewModel = playerViewModel,
                            onNavigateToPlayer = { nextItem ->
                                val nextId = nextItem.id ?: ""
                                val nextPos = nextItem.userData?.playbackPositionTicks ?: 0L
                                navController.popBackStack()
                                navController.navigate("player/$nextId?position=$nextPos")
                            },
                            onRePlayer = { nextItem, newPosition ->
                                val nextId = nextItem.id ?: ""
                                val nextPos = newPosition * 10000
                                navController.popBackStack()
                                navController.navigate("player/$nextId?position=$nextPos")
                            },
                            // 选片 = 回打开这部片的那张海报墙（退一层，通常就是库页/首页）
                            onPickPoster = { navController.popBackStack() },
                            // 退出 = 直接回首页，不是退出应用（父亲 2026-10-28 口径）
                            onExit = {
                                navController.popBackStack("home", inclusive = false)
                                Unit
                            }
                        )
                    }
                }
                if (showExitConfirm) {
                    ExitConfirmDialog(
                        onCancel = { showExitConfirm = false },
                        onConfirm = {
                            showExitConfirm = false
                            (context as? android.app.Activity)?.finish()
                        }
                    )
                }
            }
        }
    }
}
