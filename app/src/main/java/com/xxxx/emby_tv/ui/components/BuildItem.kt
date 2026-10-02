package com.xxxx.emby_tv.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.Border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp

import com.xxxx.emby_tv.R
import com.xxxx.emby_tv.Utils
import com.xxxx.emby_tv.data.model.BaseItemDto

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BuildItem(
    item: BaseItemDto,
    imgWidth: Dp,
    aspectRatio: Float,
    modifier: Modifier,
    isMyLibrary: Boolean,
    isShowImg17: Boolean = false,
    isShowOverview: Boolean = false,
    serverUrl: String, // 直接接受 serverUrl 而不是 AppModel
    accountName: String? = null,
    isPlaying: Boolean = false,
    onItemClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
) {
    val primaryColor = MaterialTheme.colorScheme.secondary
    val isSeries = item.isSeries
    val userData = item.userData
    val isPlayed = userData?.played ?: false
    val itemId = item.id
    val imageTags = item.imageTags
    val primaryTag = imageTags?.get("Primary")

    // Construct Image URL using Utils.getImageUrl
    val imageUrl = Utils.getImageUrl(serverUrl, item, isShowImg17)

    // TV 端核心组件：Surface 自动处理焦点缩放、边框和点击
    Surface(
        onClick = onItemClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        // P1 视觉对齐（2026-09-27）：焦点态改官方 Emby 的绿色描边（原来是白色描边 + 白底反色）
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                BorderStroke(
                    3.dp,
                    Color(0xFF52B54B)
                )
            )
        ),
        scale = ClickableSurfaceDefaults
            .scale(focusedScale = 1.1f),
        colors = ClickableSurfaceDefaults.colors(
            // 卡片底色改全透明（2026-10-02 GPU 实测）：原来每张卡垫一层 20% 黑，
            // 海报完全盖住它，却要多付一次整卡面积的混合（同屏十来张 = 一屏多余混合）。
            // 焦点态保留高亮（同屏只有一张卡带焦点）。
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Black.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            pressedContentColor = MaterialTheme.colorScheme.secondary,
            focusedContentColor = MaterialTheme.colorScheme.secondary
        ),

        modifier = modifier
            .width(imgWidth)
            //兼容移动端点击  TODO：移除
            // .clickable(interactionSource = null, onClick = onItemClick)
            .wrapContentHeight()
            .onKeyEvent { keyEvent ->
                if (onMenuClick != null && keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Menu -> {
                            onMenuClick()
                            true
                        }
                        Key.Bookmark -> {
                            onMenuClick()
                            true
                        }

                        else -> false
                    }
                } else false
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. 顶部图片区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
                contentAlignment = Alignment.Center
            ) {

                // 占位只在「这一条根本没有海报」时画。
                // 2026-10-02 电视端 GPU 实测：原来每张卡都垫一层深灰圆角底 + 一个矢量图标，
                // 海报加载完又被盖住 —— 同屏十来张卡就等于多画一整屏的不透明填充。
                // 现在有海报的卡不画占位（加载中直接露底色，与官方客户端一致）。
                // （子组合 + 转圈动画已在 2026-09-27 移除：那是当时 90~150ms 帧时间的主因）
                if (imageUrl.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp))
                    )
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.35f),
                        modifier = Modifier.size(40.dp)
                    )
                }

                // 使用 Coil 加载图片（无子组合版本）
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 账号名称显示
                if (!accountName.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = accountName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(primaryColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.playing),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // 播放标记
                if (isSeries || isPlayed) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .background(primaryColor, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isPlayed) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Finished",
                                modifier = Modifier
                                    .size(13.dp)
                                    .align(Alignment.Center),
                                tint = Color.White
                            )
                        }
                        else {
                            Text(
                            text = userData?.unplayedItemCount?.toString() ?: "",
                                color = Color.White,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // 2. 播放进度条
                if (!isSeries && !isMyLibrary) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .height(3.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = item.playbackProgress)
                                .background(primaryColor)
                        )
                    }
                }
            }


            // 3. 文字内容区
            Column(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val title = if (isShowOverview) {
                    item.name
                } else {
                    item.seriesName ?: item.name
                } ?: "Unknown"

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                if (!isMyLibrary) {
                    val subTitle = if (item.parentIndexNumber != null) {
                        "S${item.parentIndexNumber}:E${item.indexNumber} ${item.name}"
                    } else if (item.type == "Actor") {
                        item.role ?: ""
                    } else {
                        // 官方卡片在标题下显示"分级 + 年份"，这里对齐
                        listOfNotNull(item.officialRating, item.productionYear?.toString())
                            .joinToString("  ").ifEmpty { "--" }
                    }
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }

                if (isShowOverview) {
                    Text(
                        text = item.overview ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
