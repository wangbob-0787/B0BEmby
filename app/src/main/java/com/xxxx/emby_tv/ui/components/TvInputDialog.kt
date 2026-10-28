package com.xxxx.emby_tv.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.xxxx.emby_tv.R
import kotlinx.coroutines.delay

/**
 * 文本输入弹窗（服务器地址 / 端口 / 用户名 / 密码都用它）。
 *
 * 输入走系统输入法（父亲 2026-10-28 定：TV 版不自己画键盘）——弹窗一打开就把焦点给输入框，
 * 输入法随之弹出；遥控器/手柄方向键移焦点、OK/扳机确认，确认按钮与输入法「完成」都能提交。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvInputDialog(
    title: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isNumber: Boolean = false,
    // 密码这类内容显示成圆点（父亲 2026-10-28：弹窗里不能明文）
    isSecret: Boolean = false
) {
    var inputText by remember { mutableStateOf(initialValue) }
    val fieldFocus = remember { FocusRequester() }

    // 打开就聚焦输入框：系统输入法会跟着弹出来，用户可以直接打字
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { fieldFocus.requestFocus() }
    }

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
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = if (isNumber) it.filter { c -> c.isDigit() } else it },
                    label = { Text(stringResource(R.string.input_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    visualTransformation = if (isSecret) PasswordVisualTransformation()
                    else VisualTransformation.None,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(fieldFocus)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { commit() },
                colors = ButtonDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color(0x3D2FD57C),
                    focusedContentColor = androidx.compose.ui.graphics.Color.White,
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
                    focusedContainerColor = androidx.compose.ui.graphics.Color(0x3D2FD57C),
                    focusedContentColor = androidx.compose.ui.graphics.Color.White,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
