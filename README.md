# 🎬 B0BEmby

<div align="center">

<img src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="B0BEmby" width="128px" />

**家庭自用的 Emby 电视端客户端 | Home-use Emby TV client**

[![Android](https://img.shields.io/badge/Android-6.0%2B-3DDC84?logo=android&style=flat-square)](https://www.android.com)
[![License](https://img.shields.io/badge/License-CC%20BY--NC%204.0-e85d4f?style=flat-square)](https://creativecommons.org/licenses/by-nc/4.0/)
![Repo](https://img.shields.io/badge/repo-public-brightgreen?style=flat-square)

[简体中文](#简介--introduction) · [English](#introduction)

---

</div>

## 📌 关于本仓库 / About This Repository

**本仓库是 [shareven/OpenEmby TV](https://github.com/shareven/openemby_tv) 的个人自用改造版（公开仓库，源码与 Release 资产可直接查看/下载），仅供本人与家庭内部使用，不作商业用途，也不对外提供任何形式的服务或技术支持。**

- **上游来源 / Upstream**：基于 [shareven/openemby_tv](https://github.com/shareven/openemby_tv)（快照 **v2.0.20**）改造，原作者为 **shareven**，本仓库沿用其 **CC BY-NC 4.0** 许可。
- **改动规模 / Scale**：仓库首个提交 `2e63190 import: openemby_tv v2.0.20 上游 main 快照` 是上游代码的原样导入，其后所有提交均为本仓库改动（截至本次整理：后续提交 130+ 个，涉及 54 个文件，约 +4800 / −730 行）。
- **包名 / Package**：仍沿用上游 `com.xxxx.emby_tv`（保证覆盖升级不丢数据）；应用名、客户端上报标识、图标与桌面横幅已改为 B0BEmby。
- **用途声明 / Usage**：个人与家庭自用（自建 Emby 服务器 + 投影 / 电视设备），非面向公众分发的项目。

> This repository is a **public, home-use fork** of [shareven/OpenEmby TV](https://github.com/shareven/openemby_tv) based on the **v2.0.20** snapshot. **No commercial use.** All credit for the original project goes to its author **shareven**; this fork stays under the same **CC BY-NC 4.0** license, and every change we made is listed below.

### 🔧 本仓库的改动 / Changes in This Fork

> 判定方式：仓库首个提交是上游快照，其后每个提交都是我们的改动。下表按功能域归纳，括号内为对应实现文件。

**一、品牌与构建**

| 项目 | 改动 |
|:----|:----|
| 应用标识 | 应用名改 `B0BEmby`（`res/values/strings.xml`、`res/values-zh/strings.xml`），Emby 客户端标识常量改 `B0BEmby`（`data/remote/EmbyApi.kt`），状态栏版本号文案同步 |
| 图标 / 横幅 | 换用自制图标（各密度 `mipmap-*/ic_launcher.png`）与电视桌面横幅（`res/drawable/banner.png`，替掉上游带来的官方 Emby logo 图） |
| 版本号 | `versionName` 由上游 `2.0.20` 改为 `1.0.0`；`versionCode` 由 `220` 提到 `1000`（电视端现装 220，Android 14 拒绝降级安装） |
| 签名 | 仓库内置固定 keystore（`keystore/b0bemby.p12`），云端每次编译签名一致，可覆盖升级 |
| 云端编译 | 新增 GitHub Actions 工作流（`.github/workflows/build.yml`）：推 main 或手动触发 → 编 debug + release 两个变体 → 发 Release（见「下载与安装」） |

**二、首页与导航**

| 项目 | 改动 |
|:----|:----|
| 顶部大片头 | 新增轮播大片头（`ui/components/HomeHero.kt`）：按 OK 从该条目的上次进度续播；无横版剧照时用剧集海报兜底；切换过渡 1000ms；随内容一起上滚 |
| 首屏缓存 | 新增首屏缓存（`ui/viewmodel/HomeViewModel.kt`，落盘 `filesDir/home_cache.json`）：先渲染上次数据，网络结果在后台刷新，大库首次查询不再干等 |
| 电视直播行 | 首页新增电视直播一行（`ui/components/LiveTvRow.kt`）+ 媒体库里加直播入口；频道卡改 16:9 横版、台标按 Fit 不裁切 |
| 首页行处理 | 跳过 Live TV 行（Latest 会被服务端忽略）、同名库去重、空库不占行、音乐库首页取「歌曲」而非「专辑」（专辑无封面） |
| 焦点与返回 | 退出确认面板（`ui/components/ExitConfirmDialog.kt`）；返回后焦点回原位、行内焦点记忆（`ui/FocusMemory.kt`） |
| 电视端性能 | 去掉卡片里的 SubcomposeAsyncImage 与转圈动画、海报源图降到 400px、Coil 缓存上限写死（32M 内存 / 256M 磁盘）；砍掉每帧多余的整屏绘制（卡片透明底、大片头只画一张图、渐变只铺下半屏） |

**三、详情页（上游已有页面，本仓库按官方 TV 形态重做）**

| 项目 | 改动 |
|:----|:----|
| 剧集页形态 | 去掉左侧大海报与元数据块，改为两行文字 + 简介前置 + 横向 16:9 集卡（每集用自己的静帧）+ 演职人员拆 Cast / Crew（`ui/MediaDetailScreen.kt`） |
| 电影页 | 与剧集页统一形态（去海报 / 两行元数据 / 右侧 Logo） |
| 动作按钮 | 按定稿精简为「续播 / 播放」+「全部播放」（去掉随机播放，不做预告片 / 已播放 / 删除 / 更多）；未播过显示「播放」，有续播点才显示「续播」 |
| 焦点链路 | 进页面焦点停在顶部锚点、按下先到续播按钮、季胶囊与集卡逐级串联、行内焦点记忆 |
| 音乐 | 专辑详情页列出曲目并可逐首播放（上游专辑点进去是空的） |

**四、播放页（改动最大的部分，`ui/PlayerScreen.kt` + `ui/components/PlayerControls.kt` 等）**

| 项目 | 改动 |
|:----|:----|
| 控制条 | 按 ↓ 唤出底部控制条（取代上游的全屏半透明大面板）；一级图标行 + 二级菜单（菜单锚定在对应按钮正上方）、信息页与演职人员页按官方无框排版 |
| 按键语义 | 左右键快进快退并唤出进度条、返回键分层（二级菜单 → 一级菜单 → 收起 → 放行给系统）、连按 300ms 防抖、长按重复时重置隐退计时 |
| 标题与 Logo | 标题改剧名 / 集名两行；左上角剧集或电影 Logo 常显（限宽 140dp × 限高 60dp） |
| 视频质量 | 新增「视频质量」档位（含「原画（不转码）」与「服务端转码」兜底） |
| 变速 | 非 1.0 倍速改走 PCM + Sonic 音频处理器，并强制关隧道模式、关 float 输出（否则 ExoPlayer 会把速度 mask 回 1.0x，表现为「显示已变速但音画不动」）；倍速即时下发并落日志 |
| 实时网速 | 右上角实时网速（缓冲时出现，暂停即隐藏） |
| 弹幕（见下节） | 播放菜单新增弹幕栏：显示开关 + 四档字号 + 跟随字幕提前 / 延后偏移，设置本地记忆 |
| 缓冲设置 | 改完缓冲设置会重建播放器，让配置真正生效 |
| 快进快退条 | 按官方 Emby TV 实测规格重写，去掉重复时间行 |

**五、弹幕（本仓库新增能力）**

| 项目 | 改动 |
|:----|:----|
| 自绘弹幕层 | 上游无弹幕；本仓库新增独立弹幕层（`danmaku/DanmakuView.kt` + `danmaku/AssDanmakuParser.kt`）：选中 ASS / SSA 字幕轨后单独拉取该字幕文件，自己解析并逐帧绘制——包括 `\move` 滚动定位，滚动弹幕按文字宽度推算、完全滚出屏幕才消失 |
| 弹幕来源 | 弹幕以 Emby 服务器上的外挂 ASS 字幕轨形式提供（服务端侧负责抓取入库），客户端只解析与渲染 |
| 健壮性 | 解析加异常保护与正则转义（修过正则导致的闪退）；解析失败退化为「没有弹幕」，不影响播放 |

**六、播放链路与画质**

| 项目 | 改动 |
|:----|:----|
| 禁止直连原始文件 | 播放信息请求固定 `EnableDirectPlay=false`（`data/remote/EmbyApi.kt`）：直连网盘原始文件时播放器拿不到 HDR 色彩信息、画面发灰，改由服务端**转封装**成 HLS/TS 后再播 |
| 只换封装不降画质 | 补报 HEVC 等设备能力，避免服务端把 4K HDR 降级转码成 h264 |
| 不再永久降级 | 解除「失败即永久降级」逻辑：首播失败只回退一次服务端转码（`ui/PlayerScreen.kt` 的 `hasTriedTranscodeFallback`），下次起播仍按原画质尝试 |
| 音频直链 | 音频条目自行拼静态直链（Emby 对 Audio 不返回 DirectStreamUrl，不带 `static=true` 会 500）；专辑 / 艺人条目先解析成第一首歌再取播放信息（MusicAlbum 的 PlaybackInfo 会 500） |
| 网络超时 | 放宽超时（连接 10s、写入 30s、读取 45s、整请求 60s），修首页加载失败与转圈 |

**七、诊断与工程化**

| 项目 | 改动 |
|:----|:----|
| 内置文件日志 | 投影设备不提供 logcat，播放链路关键点改写内置文件日志（`util/DiagLog.kt` → `filesDir/b0b-diag.log`），配合 debug 包用 `run-as` 读取 |
| 播放埋点 | 起播、播放信息、失败原因、音频格式、变速实测（播放位置增量 / 真实耗时）等打点 |
| 文档 | 新增 `SDR_HDR_GUIDE.md`（SDR / HDR 播放说明与排查口径） |

> 上游的核心播放流程、Emby API 集成与页面骨架保留，改的是上面列出的部分。

---

## ✨ 简介 / Introduction

**这是一个自用的 Emby 电视端客户端：** 连自己的 Emby 服务器，在电视 / 投影上用遥控器看片，重点是 **HDR / 杜比视界画质不被降级**、**遥控器按键手感**、**弹幕**和 **大库首屏速度**。

界面为深色基调（近黑背景），共 8 种配色可选，默认「暗夜」（近黑背景 + 纯绿强调色）；上游的紫色配色已移除。

**🛡️ 隐私与联网**：App 不收集任何个人信息，也不访问任何第三方接口——除了你自己填写的 Emby 服务器地址，App 不主动连接其他服务。应用内更新检查**已关闭**（`EmbyApi.getNewVersion()` 为空实现，直接返回空对象；上游会去访问 GitHub 接口，转私有后匿名接口不可用，且会拉入上游版本覆盖本机改造）；菜单里的「下载最新版本」项只在检测到新版本时出现，因此当前不会出现。

> A home-use Emby TV client: connects to your own Emby server, focuses on not-downscaled HDR / Dolby Vision playback, remote-control ergonomics, danmaku, and fast first screen on large libraries.
>
> **Privacy**: No personal data is collected, and no third-party API is contacted. In-app update checking is **disabled** in this fork (`EmbyApi.getNewVersion()` is an empty stub); the only network endpoint is the Emby server address you configure.

---

## 📥 下载与安装 / Download & Install

| 项目 | 说明 |
|:----|:----|
| 编译方式 | GitHub Actions 云端编译（`.github/workflows/build.yml`），推 `main` 或手动 `workflow_dispatch` 触发 |
| 产物 | 每次生成 `app-debug.apk` 与 `app-release.apk` 两个变体，同为仓库内固定签名，可互相覆盖安装 |
| 发布位置 | Release 标签形如 `build-<run_number>`（例：`build-136`），同时保留 14 天的 Actions artifact |
| 装机用哪个 | **一律用 `app-release.apk`**。同一份代码、同一台电视实测：debug 包每帧 650ms、卡顿率 80%；release 包每帧 32ms、卡顿率 35%（debug 变体不跑 AOT 编译，界面线程被解释执行拖死） |
| 安装命令 | `adb install -r -i com.android.packageinstaller <app-release.apk>`，装完 `adb shell am force-stop com.xxxx.emby_tv` 再启动。**必须带 `-i com.android.packageinstaller`**：裸 `adb install` 不带来源参数，系统会把应用标成「未知来源」 |
| 下载注意 | 本仓库为公开仓库，Release 资产可匿名下载；国内网络可直接走公开镜像（ghfast.top / gh-proxy.com 前缀拼 GitHub 下载地址），比带令牌走 GitHub API 快，令牌方式保留作兜底 |

| 最低 Android 版本 | 目标版本 |
|:------------------:|:--------:|
| Android 6.0+（minSdk 23） | targetSdk 36 |

---

## ⭐ 功能 / Features

### 上游原有能力（本仓库未改或仅微调）

| 功能 | 说明 |
|:----|:----|
| 扫码录入登录信息 | 应用内起一个本地 HTTP 服务 + 二维码，手机扫码填写服务器地址与账号（`QrCodeUtils.kt` / `LocalServer.kt`） |
| 多服务器 / 多账号 | 保存多个账号并可切换，搜索按「当前账号优先、其余账号一并查询」融合返回（`data/session/SessionManager.kt`、`ui/viewmodel/SearchViewModel.kt`） |
| 首页行 | 继续观看、收藏、各媒体库最新（`ui/HomeScreen.kt`、`ui/viewmodel/HomeViewModel.kt`） |
| 选集与剧集导航 | 播放页选集、剧集列表与季切换 |
| 跳过片头 | 按章节信息识别片头区间并给出跳过按钮（`util/IntroSkipHelper.kt`、`ui/components/SkipIntroButton.kt`） |
| 缓冲设置 | 可调缓冲参数（`ui/components/BufferSettings.kt`） |
| HTTP / SOCKS5 代理 | 可为 Emby 服务配置代理（`ui/ProxySettingsScreen.kt`、`data/remote/HttpClient.kt`）；海报等图片请求也走同一个客户端 |
| 本地 ffmpeg 音频解码 | 依赖 `org.jellyfin.media3:media3-ffmpeg-decoder`，扩展音频格式支持 |
| 主题色 | 多种强调色可选（`ui/theme/ThemeColor.kt`） |
| 简中 / 英文本地化 | 跟随系统语言（`res/values`、`res/values-zh`） |
| 硬解与转码展示 | 播放页展示解码器、是否硬件加速（⚡ 标识）、转码原因等 |

> 支持的音频格式：`flac`、`alac`、`pcm_mulaw`、`pcm_alaw`、`mp3`、`aac`、`ac3`、`eac3`、`dca`、`mlp`、`truehd`

### 本仓库新增或重做的能力

| 功能 | 说明 |
|:----|:----|
| 弹幕 | 自绘弹幕层（解析 ASS / SSA，支持 `\move` 滚动定位），播放菜单内开关 + 四档字号 + 偏移 |
| 首页大片头 | 轮播 + 按 OK 续播 + 海报兜底 |
| 电视直播行 | 首页直播入口 + 16:9 频道卡 |
| 详情页重做 | 官方 TV 形态（两行文字 + 横版集卡 + Cast / Crew 拆分） |
| 播放页重做 | 底部控制条 + 一级 / 二级菜单、信息页与演职人员页、视频质量档位、实时网速 |
| 倍速可用 | 非 1.0 倍速走 PCM + Sonic，真正生效 |
| 首屏缓存 | 冷启动先出旧数据，网络在后台刷新 |
| HDR / 杜比视界保真 | 禁止直连原始文件走服务端转封装、补报 HEVC 能力、播放页显示杜比视界 profile 与解码器 |
| 内置文件日志 | 无 logcat 的设备可 `run-as` 读日志排查 |
| 固定签名 + 云端编译 | 可覆盖升级，改完推送即出包 |

---

## 🖥 界面结构 / Screens

| 页面 | 实现 |
|:----|:----|
| 首页 | 大片头轮播 + 媒体库入口（含电视直播）+ 继续观看 + 收藏 + 各库最新 |
| 媒体库 | 海报网格浏览 + 排序 / 筛选切换（`ui/LibraryScreen.kt`，`LazyVerticalGrid`） |
| 搜索 | 多账号融合搜索（`ui/SearchScreen.kt`） |
| 详情页 | 剧集 / 电影统一形态：标题两行 + 动作按钮 + 简介 + 季胶囊 + 横版集卡 + 演职人员（`ui/MediaDetailScreen.kt`） |
| 播放页 | 画面 + 底部控制条 + 一级 / 二级菜单 + 信息页 + 弹幕层（`ui/PlayerScreen.kt`、`ui/components/PlayerControls.kt`、`ui/components/PlayerMenu.kt`） |
| 账号 / 登录 | 扫码或手填登录、多账号管理（`ui/LoginScreen.kt`、`ui/AccountScreen.kt`） |
| 设置入口 | 菜单弹窗：搜索 / 切换账号 / 退出登录 / 主题色（8 种）/ 代理设置（`ui/components/MenuDialog.kt`、`ui/ProxySettingsScreen.kt`）；播放菜单内：缓冲、弹幕、字幕、倍速、视频质量；另有退出确认面板（`ui/components/ExitConfirmDialog.kt`） |

> 界面截图：暂不收录（上游带来的旧界面截图已删除，`img-show/` 只保留应用 logo）。

---

## 🎯 播放链路 / Playback Flow

```
点击播放 → 取播放信息(PlaybackInfo) → 建播放器 → 起播 → 定期上报进度 → 结束上报
```

1. **取播放信息 / PlaybackInfo**：请求固定带 `EnableDirectPlay=false`，由服务端决定转封装（HLS/TS，保 HDR 色彩信息）或转码；音频条目先解析成具体歌曲、并自行拼静态直链。
2. **起播 / Start**：Media3 ExoPlayer + 系统 MediaCodec 硬解；隧道模式仅在硬件支持且倍速 = 1.0 时开启。
3. **进度上报 / Progress**：注册会话（`POST /Sessions/Playing`）、定期上报（`/Sessions/Playing/Progress`）、结束上报（`/Sessions/Playing/Stopped`）。
4. **转码回退 / Fallback**：播放失败时回退一次服务端转码（每次起播最多一次）；音频输出类错误交给音频自动恢复，不消耗这次机会。
5. **停止转码 / Stop transcode**：切轨道或重新起播时清理残留转码任务（`/Videos/ActiveEncodings/Delete`）。
6. **字幕与弹幕**：字幕走 Emby 字幕接口；选中 ASS / SSA 轨时另外拉取字幕文件，自解析后交由弹幕层绘制。
7. **失败诊断 / Diagnostics**：关键节点写入 `filesDir/b0b-diag.log`。

---

## 🌍 本地化 / Localization

界面文案中英双语，跟随系统语言自动切换；两个文件条目数保持一致（各 244 条）。

| 文件路径 / File Path | 语言 / Language |
|:--------|:----:|
| `app/src/main/res/values-zh/strings.xml` | 简体中文 / Simplified Chinese |
| `app/src/main/res/values/strings.xml` | 英文（默认） / English (default) |

> 新增文案请两个文件同时添加同名 key，避免出现未翻译的键。

---

## 🛠 构建 / Build

| 项目 | 值 |
|:----|:----|
| 工具链 | JDK 17、Android SDK platform 36、Gradle Wrapper（AGP 8.13.2 / Kotlin 2.0.21） |
| 目标 | `compileSdk 36`、`minSdk 23`、`targetSdk 36`、`applicationId com.xxxx.emby_tv` |
| 本地编译 | `./gradlew :app:assembleRelease`（产物在 `app/build/outputs/apk/release/`） |
| 签名 | 仓库内 `keystore/b0bemby.p12`（固定口令，供 debug 变体与云端 CI 使用）；本地若有 `key.properties` 则 release 变体用正式签名 |
| 云端编译 | `.github/workflows/build.yml`：push main 或手动触发，编 debug + release，发 Release `build-<run_number>` |

---

## 🔗 外部服务 / External Services

| 接口 / API | 用途 / Purpose |
|:----|:----|
| 无 / None | 应用内更新检查已关闭（`EmbyApi.getNewVersion()` 为空实现），App 不访问任何第三方接口 |
| 用户自建 Emby 服务器 | 唯一的数据来源：库列表、海报、字幕、播放流、进度上报 |

---

## ⚠️ 已知限制 / Known Limitations

- **版本号与上游脱钩**：`versionName` 已改为 `1.0.0`、`versionCode` 为 `1000`，不再跟随上游版本号。
- **应用内更新检查关闭**：当前是代码事实（`EmbyApi.getNewVersion()` 为空实现，直接返回空对象），仓库转公开后也没恢复——重新打开更新检查等于把上游版本拉进来覆盖本机改造，要恢复需另做判断。
- **装机必须用 release 包**：debug 变体在电视端性能不可接受（见「下载与安装」）。
- **界面截图暂缺**：上游带来的旧界面截图已删除，仓库内不再收录界面截图。
- **动态徽章暂未启用**：仓库已转公开，shields.io 这类动态徽章技术上可用了，但当前仍只保留静态徽章（避免徽章服务不可用时 README 出现裂图）。
- **弹幕依赖服务端**：客户端只解析与渲染，弹幕是否完整取决于服务器侧外挂 ASS 字幕轨的入库情况。

---

## 🤝 贡献与交流 / Contributing

本项目为个人自用改造版，不接收 PR，问题与想法请提给上游项目 [shareven/openemby_tv](https://github.com/shareven/openemby_tv)。
This is a personal fork; pull requests are not accepted. Please report issues and suggestions to the upstream project.

---

## 📜 许可 / License

<a rel="license" href="https://creativecommons.org/licenses/by-nc/4.0/"><img alt="CC BY-NC 4.0 License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc/4.0/88x31.png" /></a>

本项目使用 **禁止商业用途** 的许可：
This project is licensed under a **non-commercial** license:

**Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)**

| 允许 / Allowed | 禁止 / Not Allowed |
|:----|:----:|
| ✅ 复制和分发 / Copy and distribute | ❌ 商业用途 / Commercial use |
| ✅ 改编和改造 / Adapt and remix | |
| ✅ 注明作者和来源 / Attribution required | |

**简要说明**：允许复制、分发和改编，但禁止用于商业用途，使用时需注明作者并链接到许可协议。本项目为上游项目的改造版，原作者署名与许可声明一并保留。
**Summary**: You are free to copy, distribute, and adapt the work, but you cannot use it for commercial purposes. You must attribute the author and include a link to the license.

**许可证原文 / License Text:**
https://creativecommons.org/licenses/by-nc/4.0/legalcode

**SPDX-License-Identifier:** `CC-BY-NC-4.0`

---

<div align="center">

**个人自用项目，仅供学习参考，不提供技术支持**
**Personal use only — no support provided**

*Based on [shareven/openemby_tv](https://github.com/shareven/openemby_tv) · CC BY-NC 4.0*

</div>
