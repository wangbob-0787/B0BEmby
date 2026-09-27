package com.xxxx.emby_tv

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.xxxx.emby_tv.data.remote.HttpClient
import okio.Path.Companion.toOkioPath

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("App", "Uncaught exception on ${thread.name}", throwable)
            val msg = throwable.message ?: throwable.cause?.message ?: ""
            val prefs = com.xxxx.emby_tv.data.local.PreferencesManager(this)
            val proxyOn = prefs.proxyEnabled && prefs.proxyHost.isNotEmpty()
            val isProxyError = proxyOn || msg.contains("SOCKS", ignoreCase = true) ||
                    msg.contains("Proxy", ignoreCase = true) ||
                    msg.contains("proxy", ignoreCase = true) ||
                    msg.contains("Connection refused", ignoreCase = true) ||
                    msg.contains("Malformed reply", ignoreCase = true) ||
                    msg.contains("407") ||
                    msg.contains(getString(R.string.error_proxy_connection))
            if (isProxyError) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, getString(R.string.error_proxy_connection), Toast.LENGTH_LONG).show()
                }
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { HttpClient.getClient(context) }))
                }
                // 缓存上限写死：电视端内存吃紧（小米电视 3.8G RAM 常年只剩 100 多 M），
                // Coil 默认按可用堆的 25% 做内存缓存，导致 GC 反复擦洗 → 掉帧（2026-09-27）
                .memoryCache {
                    coil3.memory.MemoryCache.Builder()
                        .maxSizeBytes(32L * 1024 * 1024)
                        .build()
                }
                .diskCache {
                    coil3.disk.DiskCache.Builder()
                        // Coil 3 要 okio.Path，不是 java.io.File
                        .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                        .maxSizeBytes(256L * 1024 * 1024)
                        .build()
                }
                .build()
        }
    }
}
