package com.xxxx.emby_tv.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xxxx.emby_tv.data.local.PreferencesManager
import com.xxxx.emby_tv.data.remote.EmbyApi
import com.xxxx.emby_tv.data.repository.EmbyRepository
import com.xxxx.emby_tv.util.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 全局状态管理 ViewModel
 * 
 * 职责：
 * - 主题管理
 * - 全局登录状态观察
 * - 应用初始化
 * - 版本检查
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val repository = EmbyRepository.getInstance(application)

    // === 主题 ===
    var currentThemeId by mutableStateOf("dark")
        private set

    // === 登录状态（从 Repository 观察）===
    val isLoggedIn: Boolean get() = repository.isLoggedIn
    val isLoaded: Boolean get() = repository.isLoaded

    // === 版本与更新（父亲 2026-10-28：照 VR 的方式，点版本号一步更新；只读 TV 自己的更新源）===
    /** 更新流程所处阶段；界面只按它显示，不各自推断 */
    var updatePhase by mutableStateOf(UpdatePhase.Idle)
        private set

    /** 下载百分比（0~100），只在 Downloading 时有意义 */
    var updateProgress by mutableStateOf(0)
        private set

    /** 失败/取消后跟在版本号后面的那句话；空表示没有话要说 */
    var updateNote by mutableStateOf("")
        private set

    /** 有新版待处理 —— 界面据此决定版本号能不能点 */
    val needUpdate: Boolean
        get() = updatePhase != UpdatePhase.Idle

    private var manifest: com.xxxx.emby_tv.data.remote.TvUpdateApi.Manifest? = null

    /** 自身编译流水号（云端把 github.run_number 写进 resValue build_run），与清单里的 run 比大小 */
    private var selfRun: Int = 0

    // === 版本信息 ===
    var currentVersion by mutableStateOf("")
        private set
    var newVersion by mutableStateOf("")
        private set
    var downloadUrl by mutableStateOf("")
        private set
        
    var updateLog by mutableStateOf("")
        private set

    val needUpdate: Boolean
        get() {
            if (newVersion.isEmpty() || currentVersion.isEmpty()) return false
            return newVersion != currentVersion
        }

    init {
        loadThemeId()
        loadCurrentVersion()
    }

    /**
     * 初始化应用（加载凭证）
     */
    fun initialize(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.loadCredentials()
            onComplete()
        }
    }

    /**
     * 保存主题 ID
     */
    fun saveThemeId(themeId: String) {
        prefs.themeId = themeId
        currentThemeId = themeId
    }

    /**
     * 加载主题 ID
     */
    private fun loadThemeId() {
        currentThemeId = prefs.themeId
    }

    /**
     * 加载当前版本号
     */
    private fun loadCurrentVersion() {
        val context = getApplication<Application>().applicationContext
        currentVersion = try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            "v${packageInfo.versionName}"
        } catch (e: Exception) {
            ""
        }
        // 云端把 github.run_number 写进 resValue build_run；本地编译没有就按 0 处理（不提示更新）
        selfRun = try {
            context.getString(com.xxxx.emby_tv.R.string.build_run).trim().toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 检查更新
     */
    fun checkUpdate() {
        if (updatePhase != UpdatePhase.Idle) return
        viewModelScope.launch {
            val m = com.xxxx.emby_tv.data.remote.TvUpdateApi.fetchManifest() ?: return@launch
            manifest = m
            updateNote = ""
            updatePhase = if (selfRun > 0 && m.run > selfRun) UpdatePhase.Available else UpdatePhase.Idle
            if (updatePhase == UpdatePhase.Available) newVersion = "build-" + m.run
        }
    }

    /**
     * 点版本号。
     *
     * 只有「有新版本」「上次失败」才真的开始下载；下载中再点一次是取消；
     * 安装阶段不响应 —— 那时包已经交给系统安装器，进程随时会被结束。
     */
    fun onVersionClick() {
        when (updatePhase) {
            UpdatePhase.Available, UpdatePhase.Failed -> startDownload()
            UpdatePhase.Downloading -> cancelDownload()
            UpdatePhase.Idle, UpdatePhase.Installing -> Unit
        }
    }

    private fun startDownload() {
        val m = manifest ?: return
        val context = getApplication<Application>().applicationContext
        updateNote = ""
        updateProgress = 0
        updatePhase = UpdatePhase.Downloading
        com.xxxx.emby_tv.data.update.ApkUpdater.download(
            context = context,
            url = m.apkUrl,
            onProgress = { p -> viewModelScope.launch { updateProgress = p } },
            onDone = { file ->
                viewModelScope.launch {
                    updatePhase = UpdatePhase.Installing
                    val err = com.xxxx.emby_tv.data.update.ApkUpdater.install(context, file)
                    if (err != null) {
                        updatePhase = UpdatePhase.Failed
                        updateNote = "更新失败，请重试"
                        ErrorHandler.logError("MainViewModel", "调起安装器失败", Exception(err))
                    }
                }
            },
            onError = { msg ->
                viewModelScope.launch {
                    updatePhase = UpdatePhase.Failed
                    updateNote = if (msg == "已取消") "已取消更新" else "更新失败，请重试"
                }
            }
        )
    }

    private fun cancelDownload() {
        com.xxxx.emby_tv.data.update.ApkUpdater.cancel()
        updatePhase = UpdatePhase.Failed
        updateNote = "已取消更新"
    }

    /**
     * 退出登录
     */
    fun logout() {
        repository.logout()
    }
}

// 扩展函数
private fun com.google.gson.JsonObject.safeGetString(key: String): String? {
    return try {
        this[key]?.asString
    } catch (e: Exception) {
        null
    }
}
