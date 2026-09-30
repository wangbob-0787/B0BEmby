package com.xxxx.emby_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.xxxx.emby_tv.data.model.BaseItemDto
import kotlinx.coroutines.delay

/**
 * 首页顶部"大片头"轮播（父亲 2026-09-30 定）
 *
 * 结构：横版剧照铺满 + 底部渐变 + 左下角叠加 标题/评分·年份·类型·分级/简介 + 一排分页圆点
 * 数据：继续观看的前 6 部
 * 行为：
 *   - 每 5 秒自动切下一部
 *   - 焦点在这一区时可用左右键手动切
 *   - **焦点移到下面内容时暂停自动切**（父亲明确要求，避免上面一直闪）
 *
 * 注：这一区只做"看"，不承担播放入口（不可点播）——首页原有的「继续观看」行保留，
 *     播放仍然从那一行进（父亲 2026-09-30 明确）。
 */
@Composable
fun HomeHeroCarousel(
    items: List<BaseItemDto>,
    serverUrl: String,
    modifier: Modifier = Modifier,
    // 焦点进了下面内容区时由 HomeScreen 传 false → 暂停自动轮播（父亲 2026-09-30 定）
    autoAdvance: Boolean = true,
) {
    if (items.isEmpty()) return
    val list = remember(items) { items.take(6) }
    var index by remember(list) { mutableIntStateOf(0) }
    val heroFocus = remember { FocusRequester() }
    var focusRequested by rememberSaveable { mutableStateOf(false) }

    // 首次进首页把焦点给大图：轮播才能跑起来；用户按下键即进下面的内容区（轮播随即暂停）
    LaunchedEffect(Unit) {
        if (focusRequested) return@LaunchedEffect
        delay(150)
        runCatching { heroFocus.requestFocus() }
        focusRequested = true
    }

    // 自动切换：5 秒一次；焦点移到下面内容时暂停（否则看着下面、上面一直闪）
    LaunchedEffect(index, autoAdvance, list.size) {
        if (!autoAdvance) return@LaunchedEffect
        if (list.size <= 1) return@LaunchedEffect
        delay(5000)
        index = (index + 1) % list.size
    }

    val current = list[index.coerceIn(0, list.lastIndex)]
    val backdrop = remember(current.id, serverUrl) {
        val id = current.id
        val own = current.backdropImageTags
        val parent = current.parentBackdropImageTags
        val parentId = current.parentBackdropItemId
        when {
            id.isNullOrEmpty() || serverUrl.isEmpty() -> null
            // 自己就有横版剧照
            !own.isNullOrEmpty() ->
                "$serverUrl/emby/Items/$id/Images/Backdrop?maxWidth=1920&tag=${own[0]}&quality=80"
            // 单集一般没有 Backdrop：用父级(剧集)的
            !parent.isNullOrEmpty() && !parentId.isNullOrEmpty() ->
                "$serverUrl/emby/Items/$parentId/Images/Backdrop?maxWidth=1920&tag=${parent[0]}&quality=80"
            // 兜底：用所属剧集
            else -> "$serverUrl/emby/Items/${current.seriesId ?: id}/Images/Backdrop?maxWidth=1920&quality=80"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)          // 1080p 下约 720px ≈ 屏幕上部三分之二
            .focusRequester(heroFocus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> {
                        index = (index - 1 + list.size) % list.size
                        true
                    }
                    Key.DirectionRight -> {
                        index = (index + 1) % list.size
                        true
                    }
                    else -> false
                }
            }
    ) {
        // 背景剧照
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF101010)))
        }

        // 底部渐变：保证左下角文字在亮剧照上也读得清
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        // 左下角信息：标题 / 评分·年份·类型·分级 / 简介（最多 3 行）
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 48.dp, end = 48.dp, bottom = 36.dp)
        ) {
            Text(
                text = current.name ?: "",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = buildList {
                current.communityRating?.let { if (it > 0) add("★ %.1f".format(it)) }
                current.productionYear?.let { add(it.toString()) }
                current.genres?.firstOrNull()?.let { add(it) }
                current.officialRating?.let { add(it) }
            }
            if (meta.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = meta.joinToString("  |  "),
                    color = Color(0xFFD5D5D5),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val overview = current.overview
            if (!overview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = overview,
                    color = Color(0xFFE2E2E2),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 分页圆点
        if (list.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                list.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (i == index) 8.dp else 6.dp)
                            .background(
                                color = if (i == index) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
