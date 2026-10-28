package com.xxxx.emby_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.xxxx.emby_tv.R
import com.xxxx.emby_tv.data.local.PreferencesManager
import com.xxxx.emby_tv.util.hoverToFocus


@Composable
fun TopStatusBar(
    currentVersion: String,
    newVersion: String = "",
    needUpdate: Boolean = false,
    // 更新状态（照 VR：点版本号就是更新；失败/取消的话跟在版本号后面）
    updatePhase: com.xxxx.emby_tv.ui.viewmodel.UpdatePhase =
        com.xxxx.emby_tv.ui.viewmodel.UpdatePhase.Idle,
    updateProgress: Int = 0,
    updateNote: String = "",
    onVersionClick: (() -> Unit)? = null,
    showSearchButton: Boolean = false,
    userInfo: String? = null,
    onSearchClick: (() -> Unit)? = null,
    onUserInfoClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val searchFocusRequester = remember { FocusRequester() }
    val userInfoFocusRequester = remember { FocusRequester() }
    val proxyEnabled = remember { PreferencesManager(context).proxyEnabled }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 左上角的菜单按钮已取消（父亲 2026-10-28，照 VR 首页：左组只留版本号）。
                // 菜单改由手柄 START 键 / 遥控器菜单键打开，各页面自己接 Key.Menu。

                // 版本号 = 更新入口（照 VR 2026-10-26 的口径）：
                //   有新版 → 点它开始下载；下载中 → 显示百分比，再点是取消；失败 → 显示原因，点它重试
                val updateSuffix = when (updatePhase) {
                    com.xxxx.emby_tv.ui.viewmodel.UpdatePhase.Available -> "（有新版本，点此更新）"
                    com.xxxx.emby_tv.ui.viewmodel.UpdatePhase.Downloading -> "（正在下载 " + updateProgress + "%）"
                    com.xxxx.emby_tv.ui.viewmodel.UpdatePhase.Installing -> "（正在安装…）"
                    com.xxxx.emby_tv.ui.viewmodel.UpdatePhase.Failed ->
                        if (updateNote.isNotEmpty()) "（" + updateNote + "）" else "（更新失败，点此重试）"
                    com.xxxx.emby_tv.ui.viewmodel.UpdatePhase.Idle -> ""
                }
                val versionClickable = updatePhase == com.xxxx.emby_tv.ui.viewmodel.UpdatePhase.Available ||
                    updatePhase == com.xxxx.emby_tv.ui.viewmodel.UpdatePhase.Failed ||
                    updatePhase == com.xxxx.emby_tv.ui.viewmodel.UpdatePhase.Downloading
                val versionColor = if (versionClickable) MaterialTheme.colorScheme.secondary else Color.White
                Text(
                    text = "B0BEmby " + currentVersion + updateSuffix,
                    color = versionColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = if (onVersionClick != null) {
                        Modifier.clickable(enabled = versionClickable) { onVersionClick() }
                    } else Modifier
                )

                if (proxyEnabled) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                                RoundedCornerShape(100)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.proxy_indicator),
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 搜索按钮（右侧）
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showSearchButton && onSearchClick != null) {
                    Surface(
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(48.dp)
                            .focusRequester(searchFocusRequester)
                            .hoverToFocus(searchFocusRequester),
                        shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.CircleShape),
                        // 父亲 2026-10-02：聚焦 = 绿底白图标
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White,
                            focusedContentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // 用户信息
                if (userInfo != null) {
                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        onClick = onUserInfoClick ?: {},
                        modifier = Modifier
                            .size(48.dp)
                            .focusRequester(userInfoFocusRequester)
                            .hoverToFocus(userInfoFocusRequester),
                        shape = ClickableSurfaceDefaults.shape(androidx.compose.foundation.shape.CircleShape),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White,
                            focusedContentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // 头像：用父亲 2026-10-02 给的图（圆形裁剪）
                            Image(
                                painter = painterResource(R.drawable.ic_user_avatar),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                            )
                        }
                    }

                }
            }
        }
    }
}
