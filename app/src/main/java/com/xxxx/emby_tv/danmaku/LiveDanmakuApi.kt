package com.xxxx.emby_tv.danmaku

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 现拉弹幕（父亲 2026-10-28 口径，服务由乐乐在 .15 维护）。
 *
 * 客户端只做三件事：传剧名+集号 → 解析 items → 交给弹幕层。
 * 片号匹配、各平台轮询、缓存都在服务里，所以这里不传 cid/tvid/vid，也不需要读本地 ASS 文件、
 * 不往 Emby 登记字幕轨 —— Emby 那边因此看不到「弹幕」这条轨，是预期的。
 *
 * 超时按 40 秒：优酷逐段拉，第一次可能要几十秒。拉不到就当作没有弹幕，不影响出画面。
 */
object LiveDanmakuApi {

    private const val BASE = "http://192.168.150.15:8790/danmaku"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    /**
     * @param title 剧名（单集传所属剧集名，电影传片名）
     * @param ep    集号，电影传 0
     * @param year  年份，可空（给了匹配更准）
     * @param kind  tv / movie
     * @param season 季号，可空
     */
    fun fetch(
        title: String,
        ep: Int,
        year: Int?,
        kind: String,
        season: Int?,
    ): DanmakuTrack? {
        if (title.isBlank()) return null
        val q = StringBuilder(BASE)
        q.append("?title=").append(android.net.Uri.encode(title))
        q.append("&ep=").append(ep)
        if (year != null && year > 0) q.append("&year=").append(year)
        q.append("&kind=").append(kind)
        if (season != null && season > 0) q.append("&season=").append(season)

        val body = runCatching {
            client.newCall(Request.Builder().url(q.toString()).build()).execute().use { r ->
                if (!r.isSuccessful) null else r.body?.string()
            }
        }.getOrNull() ?: return null

        return runCatching { parse(body) }.getOrNull()
    }

    private fun parse(body: String): DanmakuTrack? {
        val root = JsonParser.parseString(body).asJsonObject
        if (!root.get("ok").asBoolean) return null
        val arr = root.getAsJsonArray("items") ?: return null
        val items = ArrayList<DanmakuItem>(arr.size())
        for (i in 0 until arr.size()) {
            val row = arr.get(i).asJsonArray
            if (row.size() < 2) continue
            val ms = row.get(0).asLong
            val text = row.get(1).asString
            if (text.isBlank()) continue
            val color = if (row.size() > 2) row.get(2).asInt else 0xFFFFFF
            items.add(scrollItem(ms, text, color, i))
        }
        if (items.isEmpty()) return null
        return DanmakuTrack(playResX = 1920f, playResY = 1080f, items = items)
    }

    /**
     * 服务端只给时间、文字、颜色，不给位置与模式，所以统一按「从右往左滚」生成：
     * 八条轨道轮着放，避免全挤在一行。
     */
    private fun scrollItem(ms: Long, text: String, color: Int, seq: Int): DanmakuItem {
        val lane = (seq % 8)
        return DanmakuItem(
            startMs = ms,
            endMs = ms + 9_000L,
            text = text,
            isMove = true,
            x1 = 1920f,
            y1 = 0f,
            x2 = -400f,
            y2 = 0f,
            posX = 0f,
            posY = 60f + lane * 90f,
            style = DanmakuStyle(
                fontSize = 44f,
                primaryColor = color,
                outlineColor = 0xFF000000.toInt(),
                outline = 2f,
                alignment = 7,
            ),
        )
    }
}
