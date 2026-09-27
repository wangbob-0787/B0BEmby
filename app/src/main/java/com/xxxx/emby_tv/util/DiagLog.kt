package com.xxxx.emby_tv.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 临时诊断日志（2026-09-27 排查音乐播放不出去）：
 * 投影设备不给 logcat（`adb logcat` 返回 0 行），只能把关键信息写进外置私有目录，
 * 再用 `adb shell cat /sdcard/Android/data/com.xxxx.emby_tv/files/b0b-diag.log` 读。
 * 排查结束后可整体删除本文件与调用点。
 */
object DiagLog {
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun w(context: Context?, tag: String, msg: String) {
        android.util.Log.i("B0BDiag", "[$tag] $msg")
        if (context == null) return
        try {
            val dir = context.getExternalFilesDir(null) ?: return
            if (!dir.exists()) dir.mkdirs()
            File(dir, "b0b-diag.log").appendText("${fmt.format(Date())} [$tag] $msg\n")
        } catch (_: Throwable) {
        }
    }
}
