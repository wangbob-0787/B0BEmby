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


    /** 光标位置是否新鲜（3 秒内动过）；新鲜才认为「用户在指东西」，否则按遥控器处理 */
    private fun cursorFresh(): Boolean =
        cursorAt > 0L && SystemClock.uptimeMillis() - cursorAt < 3_000L

    private fun recordCursor(ev: MotionEvent) {
        cursorX = ev.x
        cursorY = ev.y
        cursorAt = SystemClock.uptimeMillis()
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
                        val step = 40f
                        if (kotlin.math.abs(scrollAccumY) >= step) {
                            fireScroll(if (scrollAccumY > 0) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP)
                            scrollAccumY = 0f
                        } else if (kotlin.math.abs(scrollAccumX) >= step) {
                            fireScroll(if (scrollAccumX > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT)
                            scrollAccumX = 0f
                        }
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
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

        if (InputBridge.hasStickData(ev)) {
            InputBridge.axisToDpadKey(ev)?.let { synthetic -> super.dispatchKeyEvent(synthetic) }
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
