package com.xxxx.emby_tv.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * TV 版专用内网更新源。
 *
 * 为什么单独一份：家里 `.15:8791` 上根目录那份 `version.json` 是 VR 版的清单（apk 是 `B0BEmbyVR-*.apk`），
 * TV 版读那份就会把 VR 包下下来 —— 两边绝不能混。所以 TV 只读 `tv/` 下自己的清单与目录：
 *   /tv/version.json      {run, apk, note, pkg}
 *   /tv/apk/<apk>         只放 TV 包（文件名以 B0BEmbyTV 开头）
 *
 * 双重校验：清单里的 pkg 必须是本应用包名，apk 文件名必须带 B0BEmbyTV；不符合就当作没有新版。
 */
object TvUpdateApi {

    const val BASE = "http://192.168.150.15:8791/tv"

    /** 本应用的包名：清单里对不上就拒绝，避免装成别的应用 */
    const val SELF_PKG = "com.xxxx.emby_tv"

    /** TV 包文件名前缀（与取包脚本 tv-fetch-apk.sh 的命名一致） */
    private const val APK_PREFIX = "B0BEmbyTV"

    data class Manifest(val run: Int, val apk: String, val note: String, val pkg: String) {
        val apkUrl: String get() = "$BASE/apk/$apk"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** 取一次清单；网络不通、字段缺失、包名或文件名对不上，一律返回 null（当作没有新版） */
    suspend fun fetchManifest(): Manifest? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$BASE/version.json").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val text = resp.body?.string() ?: return@withContext null
                val o = JSONObject(text)
                val run = o.optInt("run", 0)
                val apk = o.optString("apk", "")
                val pkg = o.optString("pkg", "")
                if (run <= 0 || apk.isEmpty()) return@withContext null
                if (pkg.isNotEmpty() && pkg != SELF_PKG) return@withContext null
                if (!apk.startsWith(APK_PREFIX)) return@withContext null
                Manifest(run, apk, o.optString("note", ""), pkg)
            }
        } catch (e: Exception) {
            null
        }
    }
}
