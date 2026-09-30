package com.xxxx.emby_tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.tv.material3.*
import com.xxxx.emby_tv.data.model.BaseItemDto
import androidx.compose.ui.res.stringResource
import com.xxxx.emby_tv.R
import com.xxxx.emby_tv.data.repository.EmbyRepository
import com.xxxx.emby_tv.ui.components.BuildItem
import com.xxxx.emby_tv.ui.components.HomeHeroCarousel
import com.xxxx.emby_tv.ui.components.Loading
import com.xxxx.emby_tv.ui.components.MenuDialog
import com.xxxx.emby_tv.ui.components.NoData
import com.xxxx.emby_tv.ui.components.TopStatusBar
import com.xxxx.emby_tv.util.ErrorHandler
import com.xxxx.emby_tv.ui.viewmodel.HomeViewModel
import com.xxxx.emby_tv.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    mainViewModel: MainViewModel,
    navController: NavController,
    onSwitchAccount: () -> Unit = {},
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // 获取 serverUrl
    val repository = remember { EmbyRepository.getInstance(context) }
    val serverUrl = repository.serverUrl ?: ""

    // 从 HomeViewModel 获取数据
    val resumeItems = homeViewModel.resumeItems
    val libraryLatestItems = homeViewModel.libraryLatestItems
    val favoriteItems = homeViewModel.favoriteItems
    val isLoading = homeViewModel.isLoading
    val errorMessage = homeViewModel.errorMessage

    // 焦点是否还在顶部大片头上（离开就暂停自动轮播，父亲 2026-09-30 定）
    var heroFocused by remember { mutableStateOf(false) }

    // 剧集级元数据：单集(Episode)本身没有评分/类型/分级，这些都在所属剧集(Series)上。
    // 按 ParentBackdropItemId 批量取一次（1 个请求），供大片头显示完整元数据行。
    var seriesMeta by remember { mutableStateOf<Map<String, BaseItemDto>>(emptyMap()) }
    LaunchedEffect(resumeItems) {
        val ids = (resumeItems ?: emptyList())
            .mapNotNull { it.parentBackdropItemId ?: it.seriesId }
            .distinct()
        if (ids.isEmpty()) return@LaunchedEffect
        runCatching { repository.getItemsByIds(ids) }
            .onSuccess { list -> seriesMeta = list.associateBy { it.id ?: "" } }
            .onFailure { ErrorHandler.logError("HomeScreen", "取剧集元数据失败", it) }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
            homeViewModel.clearError()
        }
    }

    // 检查更新
    LaunchedEffect(Unit) {
        mainViewModel.checkUpdate()
    }

    // 菜单对话框
    if (showMenu) {
        MenuDialog(
            needUpdate = mainViewModel.needUpdate,
            onDismiss = { showMenu = false },
            onLogout = {
                mainViewModel.logout()
                showMenu = false
            },
            onUpdate = {
                mainViewModel.checkUpdate()
                showMenu = false
                navController.navigate("update")
            },
            onThemeChange = { themeColor ->
                mainViewModel.saveThemeId(themeColor.id)
            },
            onSwitchAccount = {
                showMenu = false
                onSwitchAccount()
            },
            onSearch = {
                showMenu = false
                navController.navigate("search")
            },
            onProxySettings = {
                showMenu = false
                navController.navigate("proxy_settings")
            }
        )
    }

    fun goPlay(item: BaseItemDto) {
        val id = item.id ?: ""
        val userData = item.userData
        val position = userData?.playbackPositionTicks ?: 0L
        navController.navigate("player/$id?position=$position")
    }

    /** 首页条目 → 进详情页(剧集进剧集详情,其余进通用详情);"继续观看"仍保留一键续播 */
    fun openDetail(item: BaseItemDto) {
        val id = item.id ?: return
        if (item.isSeries) {
            navController.navigate("series/$id")
        } else {
            navController.navigate("media/$id")
        }
    }

    // Calculate User Info
    val currentAccountId = repository.currentAccountId
    val currentAccount = repository.savedAccounts.find { it.id == currentAccountId }
    val userInfo = if (currentAccount != null) {
        val domain = try {
            val uri = java.net.URI(currentAccount.serverUrl)
            uri.host ?: currentAccount.serverUrl
        } catch (e: Exception) {
            currentAccount.serverUrl
        }
        "${currentAccount.username}@$domain"
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部状态栏
        TopStatusBar(
            currentVersion = mainViewModel.currentVersion,
            newVersion = mainViewModel.newVersion,
            needUpdate = mainViewModel.needUpdate,
            showSearchButton = true,
            userInfo = userInfo,
            onMenuClick = { showMenu = true },
            onSearchClick = {
                navController.navigate("search")
            },
            onUserInfoClick = {
                navController.navigate("account")
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 数据未加载完成时显示 Loading 组件
//        if(isLoading){
//            Loading()
//        }
        if (libraryLatestItems != null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // 顶部大片头：取「继续观看」前 6 部，5 秒自动切、左右键手动切、OK 续播
                // （父亲 2026-09-30 定：放在滚动区里，往下滚时它跟着上移，下面的内容区才够大）
                if (!resumeItems.isNullOrEmpty()) {
                    item {
                        HomeHeroCarousel(
                            items = resumeItems ?: emptyList(),
                            serverUrl = serverUrl,
                            autoAdvance = heroFocused,   // 焦点在大图上才轮播，移到下面即暂停
                            onOpenItem = { item -> goPlay(item) },
                            onFocusChanged = { heroFocused = it },
                            // 元数据用所属剧集的（单集没有评分/类型/分级）
                            seriesMeta = seriesMeta
                        )
                    }
                }

                // 我的媒体库
                item {
                    MediaSection(
                        title = stringResource(R.string.my_libraries),
                        items = libraryLatestItems,
                        isMyLibrary = true,
                        serverUrl = serverUrl,
                        onItemSelected = { item ->
                            val firstItem = item.latestItems?.firstOrNull()
                            val type = firstItem?.type ?: ""
                            val id = item.id ?: ""
                            val title = item.name ?: ""
                            navController.navigate("library/$id?libraryName=$title&type=$type")
                        },
                        onMenuPressed = { showMenu = true }
                    )
                }

                // 继续观看
                if (resumeItems != null && resumeItems.isNotEmpty()) {
                    item {
                        MediaSection(
                            title = stringResource(R.string.continue_watching),
                            items = resumeItems,
                            isShowImg17 = true,
                            isContinueWatching = true,
                            serverUrl = serverUrl,
                            onItemSelected = { item -> goPlay(item) },
                            onMenuPressed = { showMenu = true }
                        )
                    }
                }

                // 收藏
                if (favoriteItems != null && favoriteItems.isNotEmpty()) {
                    item {
                        MediaSection(
                            title = stringResource(R.string.favorite),
                            items = favoriteItems,
                            isShowImg17 = true,
                            serverUrl = serverUrl,
                            onItemSelected = { item -> openDetail(item) },
                            onMenuPressed = { showMenu = true }
                        )
                    }
                }

                // 各库最新内容（空库不占一行——合集/PikPak电影/115蓝光原盘 没有最新条目，
                // 原来会渲染一行空占位；「我的媒体库」那一排仍然保留所有库的入口）
                itemsIndexed(
                    (libraryLatestItems ?: emptyList()).filter { !it.latestItems.isNullOrEmpty() },
                    key = { _, library -> library.id ?: library.hashCode() }
                ) { _, library ->
                    MediaSection(
                        title = library.name ?: "",
                        items = library.latestItems ?: emptyList(),
                        serverUrl = serverUrl,
                        onItemSelected = { item -> openDetail(item) },
                        onMenuPressed = { showMenu = true }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaSection(
    title: String,
    items: List<BaseItemDto>,
    isMyLibrary: Boolean = false,
    isShowImg17: Boolean = false,
    isContinueWatching: Boolean = false,
    serverUrl: String,
    onItemSelected: (BaseItemDto) -> Unit,
    onMenuPressed: () -> Unit,
) {
    val maxLength = when {
        isMyLibrary -> 260.dp   // P1：库入口按官方做成宽银幕大图块
        else -> 214.dp
    }

    // P1：库入口固定 16:9（官方"我的媒体"就是宽银幕图块），内容行才按海报比例
    val maxAspectRatio = if (isMyLibrary) 1.7778f else items.mapNotNull {
        val ratio = it.primaryImageAspectRatio?.toFloat()
        if (ratio == null || ratio == 1.0f) null else ratio
    }.maxOrNull() ?: 0.666f

    val imgWidth = if (maxAspectRatio >= 1f) {
        maxLength
    } else {
        (maxLength.value * maxAspectRatio).dp
    }
    // 这里原来有一段"「继续观看」行组合时强制 requestFocus 到第一张卡"的老逻辑。
    // 首页加了顶部大片头之后，两处同时抢焦点 → 上下键焦点顺序错乱（父亲 2026-09-30 实测）。
    // 现在只由大片头在首次进入时请求一次焦点，其余交给系统的方向键导航。

    Column {
        Text(
            text = title,
            color = Color.White,
            // 与首页大片头的剧名字号保持一致（父亲 2026-09-30：我的媒体库/继续观看等标题同大片头剧集名）
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 16.dp)
        )

        if (items.isEmpty()) {
            NoData(modifier = Modifier.height(maxLength))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(
                    items,
                    key = { _, item -> item.id ?: item.hashCode() }
                ) { index, item ->

                    val modifier = Modifier



                    BuildItem(
                        modifier = modifier,
                        item = item,
                        aspectRatio = maxAspectRatio,
                        imgWidth = imgWidth,
                        isShowImg17 = isShowImg17,
                        isMyLibrary = isMyLibrary,
                        serverUrl = serverUrl,
                        onItemClick = { onItemSelected(item) },
                        onMenuClick = { onMenuPressed() },
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
}
