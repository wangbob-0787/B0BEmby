package com.xxxx.emby_tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.xxxx.emby_tv.util.hoverToFocus

/** 键底：与详情页季胶囊、控制条按钮同一档，聚焦才亮绿 */
private val KeyIdle = Color(0xFF3A3A3A)

/**
 * 内置屏幕键盘（照 VR 版 2026-10-10 定下的方案 B 搬过来）。
 *
 * 为什么自己画：PICO 头显里系统输入法弹不到我们这块面板上 —— 实测会话绑在 display 0，
 * 面板那层收不到输入法。自己画的键盘不依赖系统输入法，打出来的字符直接进页面自己的状态。
 *
 * 键面覆盖服务器地址与密码要用的字符：数字行、符号行、三行字母。
 * 每颗键都是可聚焦的 Surface：遥控器/手柄方向键移焦点、OK/扳机触发；头显光标悬停即拿焦点。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvKeyboard(
    onChar: (Char) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "确定",
) {
    val rows = listOf(
        "1234567890",
        "/ : . @ - _ + = ? & ",
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm",
    )
    val insetRowIndex = rows.indexOf("asdfghjkl")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (rowIndex == insetRowIndex) Spacer(modifier = Modifier.weight(0.5f))
                row.forEach { c ->
                    Key(
                        label = c.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = { onChar(c) },
                    )
                }
                if (rowIndex == insetRowIndex) Spacer(modifier = Modifier.weight(0.5f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Key("空格", Modifier.weight(2.2f), onSpace, labelSize = 15.sp)
            Key("退格", Modifier.weight(1.3f), onBackspace, labelSize = 15.sp)
            Key("清空", Modifier.weight(1.3f), onClear, labelSize = 15.sp)
            Key(confirmLabel, Modifier.weight(1.6f), onConfirm, labelSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Key(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    labelSize: TextUnit = 17.sp,
) {
    val focusRequester = remember { FocusRequester() }
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = KeyIdle,
            contentColor = Color.White,
            focusedContainerColor = MaterialTheme.colorScheme.secondary,
            focusedContentColor = Color.White,
        ),
        modifier = modifier
            .height(40.dp)
            .focusRequester(focusRequester)
            .hoverToFocus(focusRequester),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = label, fontSize = labelSize, fontWeight = FontWeight.Bold)
        }
    }
}
