package com.xxxx.emby_tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.xxxx.emby_tv.R

/**
 * 文本输入弹窗（服务器地址 / 端口 / 用户名 / 密码都用它）。
 *
 * 输入法这件事：PICO 头显里系统输入法弹不到我们这块面板上，所以这里不用系统输入法，
 * 直接用内置的 [TvKeyboard]。遥控器/手柄方向键移焦点、OK/扳机落字，头显光标悬停也能落字，
 * 因此「没有实体键盘」的设备也能把地址和密码打完并提交。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvInputDialog(
    title: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isNumber: Boolean = false
) {
    var inputText by remember { mutableStateOf(initialValue) }

    fun commit() {
        onConfirm(inputText)
        onDismiss()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(text = title) },
        text = {
            Column {
                // 当前输入值：始终单行，长了就跟着滚，保证看得到刚敲的字符
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (inputText.isEmpty()) stringResource(R.string.input_label) else inputText,
                        color = if (inputText.isEmpty()) Color.White.copy(alpha = 0.45f) else Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TvKeyboard(
                    onChar = { c ->
                        if (!isNumber || c.isDigit()) inputText += c
                    },
                    onSpace = { if (!isNumber) inputText += " " },
                    onBackspace = { inputText = inputText.dropLast(1) },
                    onClear = { inputText = "" },
                    onConfirm = { commit() },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { commit() },
                colors = ButtonDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.secondary,
                    focusedContentColor = MaterialTheme.colorScheme.onTertiary,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.secondary,
                    focusedContentColor = MaterialTheme.colorScheme.onTertiary,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
