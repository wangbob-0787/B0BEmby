package com.xxxx.emby_tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.xxxx.emby_tv.data.model.BaseItemDto
import kotlinx.coroutines.delay

/**
 * 首页顶部"大片头"轮播（父亲 2026-09-30 定）
 *
 * 尺寸按父亲给的参考图实测（参考图 2880×1800、比例 1.6，按占屏比例换算 1080p）：
 *   大图高度 1340/1800 = 74.4% 屏高   → 400dp
 *   左内边距 50/2880 = 1.74% 屏宽     → 与下面内容行左对齐
 *   标题字高 70/1800 = 3.89%          → 21sp
 *   元数据字高 33/1800 = 1.83%        → 10sp
 *   简介字高 28/1800 = 1.56%，共 2 行 → 9sp
 *   圆点直径 14px                     → 5dp / 4dp
 *
 * 排版（照参考图）：标题一行；元数据一行 = ★金色星标 + 评分 | 年份 | 类型 | [分级(白边框标签)]；
 *   简介最多 2 行。整块宽度收到屏幕中部（不左右顶满），左边缘与下面内容行对齐。
 *
 * 元数据取值：单集(Episode)本身没有评分/类型/分级，这些在所属剧集(Series)上，
 *   由 HomeScreen 按 ParentBackdropItemId 批量取好后通过 seriesMeta 传进来。
 */
@Composable
fun HomeHeroCarousel(
    items: List<BaseItemDto>,
    serverUrl: String,
    modifier: Modifier = Modifier,
    // 焦点进了下面内容区时由 HomeScreen 传 false → 暂停自动轮播（父亲 2026-09-30 定）
    autoAdvance: Boolean = true,
    // 在大图上按 OK：从上次播放位置续播当前这一部（父亲 2026-09-30 补充）
    onOpenItem: (BaseItemDto) -> Unit = {},
    // 上报自身焦点：HomeScreen 用它决定"焦点离开大图就暂停轮播"
    onFocusChanged: (Boolean) -> Unit = {},
    // 剧集级元数据（key = 剧集 id）：补单集缺失的评分/类型/分级/年份
    seriesMeta: Map<String, BaseItemDto> = emptyMap(),
) {
    if (items.isEmpty()) return
    val list = remember(items) { items.take(6) }
    var index by remember(list) { mutableIntStateOf(0) }
    val heroFocus = remember { FocusRequester() }
    var focusRequested by rememberSaveable { mutableStateOf(false) }

    // 首次进首页把焦点给大图：轮播才跑得起来；用户按下键即进下面内容区（轮播随即暂停）
    LaunchedEffect(Unit) {
        if (focusRequested) return@LaunchedEffect
        delay(150)
        runCatching { heroFocus.requestFocus() }
        focusRequested = true
    }

    // 自动切换：5 秒一次；焦点移到下面内容时暂停
    LaunchedEffect(index, autoAdvance, list.size) {
        if (!autoAdvance) return@LaunchedEffect
        if (list.size <= 1) return@LaunchedEffect
        delay(5000)
        index = (index + 1) % list.size
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)          // 参考图实测：大图底边 1340/1800 = 74.4% 屏高
            .onFocusChanged { onFocusChanged(it.isFocused) }
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
                    // OK / 回车：从上次播放位置续播当前显示的这一部
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onOpenItem(list[index.coerceIn(0, list.lastIndex)])
                        true
                    }
                    else -> false
                }
            }
    ) {
        // 切换过渡：淡出/淡入各 1 秒（父亲 2026-09-30 定，原先 500ms 太快）
        Crossfade(
            targetState = index,
            animationSpec = tween(durationMillis = 1000),
            label = "hero"
        ) { i ->
            val item = list[i.coerceIn(0, list.lastIndex)]
            val meta = seriesMeta[item.parentBackdropItemId ?: item.seriesId ?: ""]
            Box(modifier = Modifier.fillMaxSize()) {
                val backdrop = backdropUrlOf(item, serverUrl)
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

                // 左下信息块：宽度收到屏幕中部（父亲：不要左右顶满），左缘与下面内容行对齐
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.55f)
                        .padding(start = 32.dp)
                ) {
                    // 第一行：剧名
                    Text(
                        text = item.seriesName ?: item.name ?: "",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 第二行：★评分 | 年份 | 类型 | [分级]
                    val rating = meta?.communityRating ?: item.communityRating
                    val year = meta?.productionYear ?: item.productionYear
                    val genre = meta?.genres?.firstOrNull() ?: item.genres?.firstOrNull()
                    val cert = meta?.officialRating ?: item.officialRating
                    Row(
                        modifier = Modifier.padding(top = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var wrote = false
                        fun sep() {
                            Text(
                                text = "  |  ",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (rating != null && rating > 0) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),      // 参考图：金色星标
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "%.1f".format(rating),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            wrote = true
                        }
                        if (year != null && year > 0) {
                            if (wrote) sep()
                            Text(
                                text = year.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            wrote = true
                        }
                        if (!genre.isNullOrBlank()) {
                            if (wrote) sep()
                            Text(
                                text = genre,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            wrote = true
                        }
                        if (!cert.isNullOrBlank()) {
                            if (wrote) sep()
                            // 参考图：分级是带白边框的小标签（如 KR-15）
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = cert,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 第三行起：简介，最多 2 行
                    val overview = meta?.overview ?: item.overview
                    if (!overview.isNullOrBlank()) {
                        Text(
                            text = overview,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 9.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 7.dp)
                        )
                    }
                }
            }
        }

        // 分页圆点（放 Crossfade 外面，切换时不跟着闪）
        if (list.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                list.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (i == index) 5.dp else 4.dp)
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

/**
 * 取横版剧照 URL。回退链：
 *   自己的 Backdrop → 父级(剧集)的 Backdrop → 所属剧集的 Backdrop
 * （单集通常没有 Backdrop：实测 /Items/{epId}/Images 只有 Primary/Thumb）
 */
private fun backdropUrlOf(item: BaseItemDto, serverUrl: String): String? {
    if (serverUrl.isEmpty()) return null
    val id = item.id ?: return null
    val own = item.backdropImageTags
    val parent = item.parentBackdropImageTags
    val parentId = item.parentBackdropItemId
    return when {
        !own.isNullOrEmpty() ->
            "$serverUrl/emby/Items/$id/Images/Backdrop?maxWidth=1920&tag=${own[0]}&quality=80"
        !parent.isNullOrEmpty() && !parentId.isNullOrEmpty() ->
            "$serverUrl/emby/Items/$parentId/Images/Backdrop?maxWidth=1920&tag=${parent[0]}&quality=80"
        else ->
            "$serverUrl/emby/Items/${item.seriesId ?: id}/Images/Backdrop?maxWidth=1920&quality=80"
    }
}
