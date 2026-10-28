package com.xxxx.emby_tv.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xxxx.emby_tv.data.repository.EmbyRepository
import com.xxxx.emby_tv.data.model.BaseItemDto
import com.xxxx.emby_tv.util.ErrorHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 * 
 * 职责：
 * - 加载继续观看列表
 * - 加载媒体库最新内容
 * - 加载收藏列表
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmbyRepository.getInstance(application)

    // === 首页数据 ===
    var resumeItems by mutableStateOf<List<BaseItemDto>?>(null)
        private set
    var libraryLatestItems by mutableStateOf<List<BaseItemDto>?>(null)
        private set
    var favoriteItems by mutableStateOf<List<BaseItemDto>?>(null)
        private set
    // 直播频道（首页「电视直播」一行）
    var liveChannels by mutableStateOf<List<BaseItemDto>?>(null)
        private set
    // 服务器上的「电视直播」视图（父亲 2026-10-02：媒体库里也要有这个入口，点进去看全部频道）
    var liveTvView by mutableStateOf<BaseItemDto?>(null)
        private set

    // === 加载状态 ===
    var isLoading by mutableStateOf(false)

    /** 最近一次失败的原始原因；首页三态用它区分"还在等"与"等不到了" */
    var lastError by mutableStateOf<String?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // === 首屏缓存:上次首页数据先画出来,网络请求在后台刷新(大库首查约 6 秒,不能干等) ===
    private val cacheFile = java.io.File(application.filesDir, "home_cache.json")
    private val gson = com.google.gson.Gson()

    init {
        restoreFromCache()
        loadData()
    }

    private fun restoreFromCache() {
        try {
            if (!cacheFile.exists()) return
            val cache = gson.fromJson(cacheFile.readText(), HomeCache::class.java) ?: return
            resumeItems = cache.resume
            libraryLatestItems = cache.latest
            favoriteItems = cache.favorites
            liveChannels = cache.live ?: emptyList()
            liveTvView = cache.liveView
        } catch (e: Exception) {
            ErrorHandler.logError("HomeViewModel", "读取首页缓存失败", e)
        }
    }

    private fun saveCache() {
        try {
            if (resumeItems.isNullOrEmpty() && libraryLatestItems.isNullOrEmpty() && favoriteItems.isNullOrEmpty()) return
            cacheFile.writeText(
                gson.toJson(HomeCache(resumeItems, libraryLatestItems, favoriteItems, liveChannels, liveTvView))
            )
        } catch (e: Exception) {
            ErrorHandler.logError("HomeViewModel", "写入首页缓存失败", e)
        }
    }

    /**
     * 加载首页数据
     */
    fun loadData() {
        if (!repository.isLoggedIn) {
            resumeItems = emptyList()
            libraryLatestItems = emptyList()
            favoriteItems = emptyList()
            liveChannels = emptyList()
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val resumeDeferred = async {
                    try { repository.getResumeItems() }
                    catch (e: Exception) { errorMessage = e.message; emptyList() }
                }
                val latestDeferred = async {
                    try { repository.getLatestItems() }
                    catch (e: Exception) { errorMessage = e.message; emptyList() }
                }
                val favDeferred = async {
                    try { repository.getFavoriteItems() }
                    catch (e: Exception) { errorMessage = e.message; emptyList() }
                }
                // 直播频道：失败不影响首页其余部分（服务器没配直播源时就是空行）
                val liveDeferred = async {
                    try { repository.getLiveTvChannels() }
                    catch (e: Exception) { emptyList() }
                }
                val viewsDeferred = async {
                    try { repository.getViews() }
                    catch (e: Exception) { emptyList() }
                }

                /*
                 * 先全部 await 到局部变量，最后一次性写状态（父亲 2026-10-29 报"首页一行一行
                 * 蹦出来、位置老跳"）：每 await 完就写一次，等于五次独立重画，下面内容被顶五次。
                 */
                val resume = resumeDeferred.await()
                val latest = latestDeferred.await()
                val fav = favDeferred.await()
                val live = liveDeferred.await()
                val liveView = viewsDeferred.await()
                    .firstOrNull { it.collectionType.equals("livetv", ignoreCase = true) }

                resumeItems = resume
                libraryLatestItems = latest
                favoriteItems = fav
                liveChannels = live
                liveTvView = liveView
                lastError = null
            } catch (e: Exception) {
                /*
                 * 失败时**不要**把列表清成空表：空表会被首页当成"有数据"，一片空白还挡掉提示；
                 * 留着缓存里的内容，三态那边按 isNullOrEmpty 判，才会正确显示"请检查网络连接"。
                 */
                if (errorMessage == null) errorMessage = e.message
                lastError = e.message ?: "首页加载失败"
            } finally {
                isLoading = false
                saveCache()
            }
        }
    }

    /**
     * 刷新数据
     */
    fun refresh() = loadData()

    /**
     * 获取下一集信息
     */
    fun playNextUp(seriesId: String, onResult: (BaseItemDto) -> Unit) {
        viewModelScope.launch {
            try {
                val itemsArray = repository.getShowsNextUp(seriesId)
                val items = itemsArray.ifEmpty {
                    repository.getSeriesList(seriesId)
                }
                if (items.isNotEmpty()) {
                    onResult(items.first())
                }
            } catch (e: Exception) {
                ErrorHandler.logError("HomeViewModel", "加载数据失败", e)
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }
}

/** 首页首屏缓存,字段名即 JSON 键 */
private data class HomeCache(
    val resume: List<BaseItemDto>? = null,
    val latest: List<BaseItemDto>? = null,
    val favorites: List<BaseItemDto>? = null,
    /*
     * 直播行与「我的媒体库」里的直播图块也要进缓存（父亲 2026-10-29 报"冷启动后直播行插进来，
     * 内容再下移一格"）：它们比其余行回来得晚，缓存不带就会后插、把下面的内容顶下去。
     */
    val live: List<BaseItemDto>? = null,
    val liveView: BaseItemDto? = null
)
