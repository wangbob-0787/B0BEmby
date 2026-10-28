package com.xxxx.emby_tv.ui.viewmodel

/**
 * 更新流程阶段（照 VR 版同一套状态机）。
 *
 * 界面只按它显示，不各自推断：Idle/Available 时版本号可以点，Downloading 显示百分比，
 * Installing 之后不再响应点击（包已交给系统安装器）。
 */
enum class UpdatePhase {
    Idle,        // 没有新版，或还没检查
    Available,   // 有新版，等用户点版本号
    Downloading, // 正在下载（再点一次取消）
    Installing,  // 已交给系统安装器
    Failed       // 失败或已取消
}
