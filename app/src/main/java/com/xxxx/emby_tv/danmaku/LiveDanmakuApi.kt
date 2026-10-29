package com.xxxx.emby_tv.danmaku

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 现拉弹幕（服务由乐乐在 .15 维护，与 VR 版同一个地址、同一份数据）。
 *
 * 客户端只做三件事：传剧名+集号 → 解析 items → 排成弹幕条目。片号匹配、各平台轮询、
 * 缓存都在服务里，所以这里不传 cid/tvid/vid，也不读本地 ASS 文件、不往 Emby 登记字幕轨。
 *
 * 排布口径**照抄 VR 版**（`B0BEmby-VR/.../MainActivity.kt` 的 `buildDanmakuTrack`，
 * 父亲 2026-10-29：TV 必须和 VR 一样）。三条要点：
 *
 *  1. **只两条轨道**，行高 1.35 字，离顶 15。轨道铺满全屏不像影院。
 *  2. **先按整部片时长算容量，再均匀抽稀**：不让"前面几十条、后面全丢"，
 *     整部片从头到尾都有弹幕。
 *  3. **同轨后一条要等前一条整个身子进场**（等多久由前一条宽度决定）；排不下就丢这一条，
 *     不硬挤 —— 硬挤就是重叠。
 *
 * 颜色：源里的颜色值最高字节是 alpha，平台数据里常见 alpha=0（全透明），直接用就只剩
 * 黑色投影，看着是黑字。这里统一补成不透明。
 */
object LiveDanmakuApi {

    private const val BASE = "http://192.168.150.15:8790/danmaku"

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    private const val kFontSize = 52f
    private const val kLineHeight = kFontSize * 1.35f
    private const val kTopMargin = 15f
    private const val kSpanMs = 8000L
    private const val kStartX = 1920f
    private const val kEndX = -400f
    private const val kGapMs = 2000L
    private val kLaneY = floatArrayOf(kTopMargin, kTopMargin + kLineHeight)

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
        val raw = ArrayList<Triple<Long, String, Long>>(arr.size())
        for (i in 0 until arr.size()) {
            val row = arr.get(i).asJsonArray
            if (row.size() < 2) continue
            val ms = row.get(0).asLong
            val text = row.get(1).asString.trim()
            if (text.isEmpty()) continue
            val color = if (row.size() > 2) row.get(2).asLong else 16777215L
            raw.add(Triple(ms, text, color))
        }
        if (raw.isEmpty()) return null
        return buildTrack(raw)
    }

    private fun buildTrack(parsed: List<Triple<Long, String, Long>>): DanmakuTrack? {
        val speedPx = (kStartX - kEndX) / (kSpanMs / 1000f)      // PlayRes 单位/秒
        fun textW(t: String): Float {
            var w = 0f
            for (ch in t) w += if (ch.code < 128) kFontSize * 0.45f else kFontSize
            return w
        }
        fun needMs(t: String): Long = maxOf(kGapMs, (textW(t) / speedPx * 1000f).toLong())

        val spanAll = (parsed.maxOfOrNull { it.first } ?: 0L).coerceAtLeast(1L)
        val avgNeed = parsed.take(3000).map { needMs(it.second) }.average().toLong().coerceAtLeast(1L)
        val capacity = ((spanAll / avgNeed) * kLaneY.size).toInt().coerceAtLeast(1)
        val picked: List<Triple<Long, String, Long>> =
            if (parsed.size <= capacity) parsed
            else {
                val step = parsed.size.toDouble() / capacity
                (0 until capacity).mapNotNull { i -> parsed.getOrNull((i * step).toInt()) }
            }

        val laneFree = LongArray(kLaneY.size)
        val items = ArrayList<DanmakuItem>(picked.size)
        for (p in picked) {
            var lane = -1
            for (i in kLaneY.indices) if (laneFree[i] <= p.first) { lane = i; break }
            if (lane < 0) continue                       // 两条轨道都满：丢这一条，不硬挤
            laneFree[lane] = p.first + needMs(p.second)
            val y = kLaneY[lane]
            items.add(
                DanmakuItem(
                    startMs = p.first,
                    endMs = p.first + kSpanMs,
                    text = p.second,
                    isMove = true,
                    x1 = kStartX, y1 = y, x2 = kEndX, y2 = y,
                    posX = 960f, posY = y,
                    style = DanmakuStyle(
                        fontSize = kFontSize,
                        primaryColor = (0xFF000000L or (p.third and 0xFFFFFFL)).toInt(),
                        outlineColor = 0xFF000000.toInt(),
                        outline = 3f,
                        alignment = 2,
                    ),
                ),
            )
        }
        if (items.isEmpty()) return null
        return DanmakuTrack(playResX = 1920f, playResY = 1080f, items = items)
    }
}
