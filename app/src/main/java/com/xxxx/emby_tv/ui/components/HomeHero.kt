package com.xxxx.emby_tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.xxxx.emby_tv.data.model.BaseItemDto
import kotlinx.coroutines.delay

/**
 * 首页顶部"大片头"轮播（父亲 2026-09-30 定）
 *
 * 尺寸全部按父亲给的参考图实测（参考图 2880×1800，宽高比 1.6，按占屏比例换算到 1080p）：
 *   大图高度 1340/1800 = 74.4% 屏高   → 400dp
 *   左内边距 50/2880 = 1.74% 屏宽     → 17dp
 *   标题字高 70/1800 = 3.89%          → 21sp
 *   元数据字高 33/1800 = 1.83%        → 10sp
 *   简介字高 28/1800 = 1.56%，共 2 行 → 9sp
 *   圆点直径 14px                     → 5dp / 4dp
 *
 * 行为：
 *   - 每 5 秒自动切下一部，切换带淡入淡出过渡
 *   - 焦点在这一区时左右键手动切
 *   - 按 OK 从上次播放位置续播当前这一部
 *   - 焦点移到下面内容区时暂停自动切
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
        // 切换过渡：整块内容淡入淡出（父亲 2026-09-30 要求）
        Crossfade(
            targetState = index,
            animationSpec = tween(durationMillis = 500),
            label = "hero"
        ) { i ->
            val item = list[i.coerceIn(0, list.lastIndex)]
            Box(modifier = Modifier.fillMaxSize()) {
                // 背景横版剧照
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

                // 左下角信息：标题 / 元数据 / 简介
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 17.dp, end = 40.dp, bottom = 0.dp)
                ) {
                    // 标题只显示剧名（参考图上是"在溪边"这种剧名；单集的全名太长）
                    Text(
                        text = item.seriesName ?: item.name ?: "",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = buildList {
                        item.communityRating?.let { if (it > 0) add("★ %.1f".format(it)) }
                        item.productionYear?.let { add(it.toString()) }
                        // 类型（剧情/科幻…）：参考图的元数据行里有这一项
                        item.genres?.firstOrNull()?.let { add(it) }
                        item.officialRating?.let { add(it) }
                    }
                    if (meta.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = meta.joinToString("  |  "),
                            color = Color(0xFFD5D5D5),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val overview = item.overview
                    if (!overview.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = overview,
                            color = Color(0xFFE2E2E2),
                            fontSize = 9.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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
