package com.xxxx.emby_tv.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 临时诊断日志。
 *
 * 落点选择（2026-10-31 实测定下的口径）：
 *   · 投影（Vidda）不给 logcat（`adb logcat` 返回 0 行）；
 *   · 装机一律用 release 包，而 release 不是 debuggable，`adb shell run-as` 直接被拒；
 *   · 所以日志必须写在 /sdcard/Android/media/<包名>/ 这个外部媒体目录 —— 应用自己有写权限，
 *     adb shell 不用 root 也能读，release 包同样适用。
 * 内置 filesDir 只在没有外置媒体目录时兜底（debug 包可以用 run-as 读）。
 * 排查结束后整体删除本文件与调用点。
 */
object DiagLog {
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private const val FILE_NAME = "b0b-diag.log"
    private const val MAX_BYTES = 2L * 1024 * 1024

    private fun logFile(context: Context): File {
        val ext = try {
            context.getExternalMediaDirs()?.firstOrNull()
        } catch (_: Throwable) {
            null
        }
        if (ext != null && (ext.exists() || ext.mkdirs())) return File(ext, FILE_NAME)
        return File(context.filesDir, FILE_NAME)
    }

    @Synchronized
    fun w(context: Context?, tag: String, msg: String) {
        android.util.Log.i("B0BDiag", "[$tag] $msg")
        if (context == null) return
        val line = "${fmt.format(Date())} [$tag] $msg\n"
        try {
            val f = logFile(context)
            // 长时间开着别把存储写满：超过 2MB 清空重开
            if (f.exists() && f.length() > MAX_BYTES) f.delete()
            f.appendText(line)
        } catch (_: Throwable) {
        }
    }
}
