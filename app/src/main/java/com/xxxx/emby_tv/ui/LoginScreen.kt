package com.xxxx.emby_tv.ui

import com.xxxx.emby_tv.ui.theme.FocusGreenContainer
import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.xxxx.emby_tv.LocalServer
import com.xxxx.emby_tv.QrCodeUtils
import com.xxxx.emby_tv.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.xxxx.emby_tv.ui.components.TvInputDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.xxxx.emby_tv.util.hoverToFocus
import androidx.navigation.NavController
import com.xxxx.emby_tv.ui.components.TopStatusBar
import com.xxxx.emby_tv.ui.theme.ThemeColorManager
import com.xxxx.emby_tv.ui.viewmodel.LoginViewModel
import com.xxxx.emby_tv.ui.viewmodel.MainViewModel


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel,
    mainViewModel: MainViewModel,
    navController: NavController,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val themeColor = ThemeColorManager.getThemeColorById(context, mainViewModel.currentThemeId)

    // 解析 savedServerUrl 的辅助函数
    fun parseServerUrl(url: String?): Triple<String, String, String> {
        if (url.isNullOrEmpty()) return Triple("http", "", "8096")
        return try {
            val uri = java.net.URI(url)
            Triple(
                uri.scheme ?: "http",
                uri.host ?: "",
                uri.port.takeIf { it > 0 }?.toString() ?: "8096"
            )
        } catch (e: Exception) {
            Triple("http", "", "8096")
        }
    }

    // 在初始化时就解析 savedServerUrl
    val initialParsed = remember { parseServerUrl(loginViewModel.savedServerUrl ?: "") }

    // 从 ViewModel 获取保存的值
    var protocol by remember { mutableStateOf(initialParsed.first) }
    var host by remember { mutableStateOf(initialParsed.second) }
    var port by remember { mutableStateOf(initialParsed.third) }
    var username by remember { mutableStateOf(loginViewModel.savedUsername) }
    var password by remember { mutableStateOf(loginViewModel.savedPassword) }

    // Server & QR Code State
    var qrCodeBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var localServerAddress by remember { mutableStateOf("") }
    var localServer by remember { mutableStateOf<LocalServer?>(null) }

    // Focus Requesters
    val hostFocusRequester = remember { FocusRequester() }
    val loginButtonFocusRequester = remember { FocusRequester() }

    // Initial Focus Logic - 在 host 已经被正确初始化后执行
    LaunchedEffect(Unit) {
        // 给 UI 一点时间完成组合
        kotlinx.coroutines.delay(100)
        if (host.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
            loginButtonFocusRequester.requestFocus()
        } else {
            hostFocusRequester.requestFocus()
        }
    }

    // Dialog states
    var showHostDialog by remember { mutableStateOf(false) }
    var showPortDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    fun buildServerUrl(p: String, h: String, pt: String): String {
        return if (h.isNotEmpty()) "$p://$h:$pt" else ""
    }

    // 监听 savedServerUrl 变化（用于外部改变时更新）
    LaunchedEffect(loginViewModel.savedServerUrl) {
        if (!loginViewModel.savedServerUrl.isNullOrEmpty()) {
            val (p, h, pt) = parseServerUrl(loginViewModel.savedServerUrl)
            protocol = p
            host = h
            port = pt
        }
    }

    val failText = stringResource(id = R.string.login_failed)

    // 检查更新
    LaunchedEffect(Unit) {
        mainViewModel.checkUpdate()
    }

    LaunchedEffect(themeColor) {
        withContext(Dispatchers.IO) {
            val server = LocalServer.startServer(
                themeColor.primaryDark,
                themeColor.secondaryLight
            ) { p, h, pt, user, pass ->
                protocol = p
                host = h
                port = pt
                username = user
                password = pass
                loginButtonFocusRequester.requestFocus()
            }
            localServer = server

            if (server != null) {
                val ip = QrCodeUtils.getLocalIpAddress()
                if (ip != null) {
                    val address = "http://$ip:${server.listeningPort}"
                    localServerAddress = address
                    val bitmap = QrCodeUtils.generateQrCode(address, 400)
                    qrCodeBitmap = bitmap?.asImageBitmap()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            localServer?.stop()
        }
    }

    fun onLoginClick(){
        val builtServerUrl = buildServerUrl(protocol, host, port)

        loginViewModel.login(
            serverUrl = builtServerUrl,
            username = username,
            password = password,
            onSuccess = {
                localServer?.stop()
            },
            onError = { error ->
                scope.launch {
                    android.widget.Toast.makeText(
                        context.applicationContext,
                        error,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部状态栏
        TopStatusBar(
            currentVersion = mainViewModel.currentVersion,
            needUpdate = mainViewModel.needUpdate,
            updatePhase = mainViewModel.updatePhase,
            updateProgress = mainViewModel.updateProgress,
            updateNote = mainViewModel.updateNote,
            onVersionClick = { mainViewModel.onVersionClick() }
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：QR Code 和提示
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(2f))
                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap!!,
                        contentDescription = stringResource(R.string.scan_qr_hint),
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.scan_qr_hint),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "${stringResource(R.string.local_server_url)}: $localServerAddress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.starting_server),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.weight(1.5f))

                Text(
                    text = stringResource(R.string.footer_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // 右侧：登录表单
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 48.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Server URL Input
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProtocolButton(
                        protocol = protocol,
                        onClick = { protocol = if (protocol == "http") "https" else "http" },
                        modifier = Modifier.width(80.dp)
                    )
                    TvInputButton(
                        value = host,
                        label = stringResource(R.string.host),
                        onClick = { showHostDialog = true },
                        modifier = Modifier.weight(1f),
                        focusRequester = hostFocusRequester
                    )
                    TvInputButton(
                        value = port,
                        label = stringResource(R.string.port),
                        onClick = { showPortDialog = true },
                        modifier = Modifier.width(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Username Input
                TvInputButton(
                    value = username,
                    label = stringResource(R.string.username),
                    onClick = { showUsernameDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                TvInputButton(
                    value = password,
                    label = stringResource(R.string.password),
                    visualTransformation = PasswordVisualTransformation(),
                    onClick = { showPasswordDialog = true }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onLoginClick() },
                    enabled = !loginViewModel.isLoading,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                        //兼容移动端点击  TODO：移除
                        //  .clickable(true, onClick = { onLoginClick() })
                        .focusRequester(loginButtonFocusRequester)
                        .hoverToFocus(loginButtonFocusRequester),
                    // 聚焦底与其它控件统一成绿 24%（VR 登录页同款），文字保持白色
                    colors = ButtonDefaults.colors(
                        focusedContainerColor = FocusGreenContainer,
                        focusedContentColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = ButtonDefaults.shape()
                ) {
                    if (loginViewModel.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.logging_in),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.login),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

            }
        }
    }

    // Dialogs
    if (showHostDialog) {
        TvInputDialog(
            title = stringResource(R.string.host),
            initialValue = host,
            onConfirm = {
                host = it
                showHostDialog = false
            },
            onDismiss = { showHostDialog = false }
        )
    }

    if (showPortDialog) {
        TvInputDialog(
            title = stringResource(R.string.port),
            initialValue = port,
            onConfirm = {
                port = it.filter { c -> c.isDigit() }
                showPortDialog = false
            },
            onDismiss = { showPortDialog = false },
            isNumber = true
        )
    }

    if (showUsernameDialog) {
        TvInputDialog(
            title = stringResource(R.string.username),
            initialValue = username,
            onConfirm = {
                username = it
                showUsernameDialog = false
            },
            onDismiss = { showUsernameDialog = false }
        )
    }

    if (showPasswordDialog) {
        TvInputDialog(
            title = stringResource(R.string.password),
            initialValue = password,
            onConfirm = {
                password = it
                showPasswordDialog = false
            },
            onDismiss = { showPasswordDialog = false },
            isSecret = true
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvInputButton(
    value: String,
    label: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onClick: () -> Unit,
    // 外部要指定初始焦点时传进来；不传就自己造一个，用于指针悬停拿焦点
    focusRequester: FocusRequester? = null,
) {
    val ownFocus = remember { FocusRequester() }
    val fieldFocus = focusRequester ?: ownFocus
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        // 与 VR 登录页同一套：静默时 50% 白描边，聚焦用绿 24% 底 + 白字，不再画彩色边框
        border = ClickableSurfaceDefaults.border(
            border = Border(
                BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = FocusGreenContainer,
            focusedContentColor = Color.White,
        ),
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(64.dp)
            .focusRequester(fieldFocus)
            .hoverToFocus(fieldFocus)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
            val transformedText = visualTransformation.filter(AnnotatedString(value)).text.text
            Text(
                text = if (value.isEmpty()) " " else transformedText,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProtocolButton(
    protocol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ownFocus = remember { FocusRequester() }
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            ),
            focusedBorder = Border(BorderStroke(0.dp, Color.Transparent))
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = FocusGreenContainer,
            focusedContentColor = Color.White,
        ),
        modifier = modifier
            .height(64.dp)
            .focusRequester(ownFocus)
            .hoverToFocus(ownFocus)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = protocol.uppercase(),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}