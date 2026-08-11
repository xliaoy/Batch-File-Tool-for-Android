# 株馨文件批量替换工具 (Batch File Tool for Android)

Android 批量文件处理工具，支持文件内容批量替换和基于通配符的批量文件删除。

## 功能特性

- **内容批量替换** -- 递归遍历目录，支持纯文本/正则表达式替换，区分大小写，自动创建 `.bak` 备份
- **文件批量删除** -- 支持 glob 通配符匹配（`*.log`, `temp_*`, `*_bak.*`），预览确认后删除
- **Neumorphism（新拟态）UI** -- 柔和凸凹质感卡片 + Apple Terminal 风格日志终端
- **中英双语** -- 语言切换自动重启应用，所有界面/日志即时生效
- **引导式首次启动** -- 4 步向导完成权限申请、默认目录、语言选择
- **实时统计** -- 终端上方显示扫描/修改/跳过/错误计数
- **SAF 目录选择器** -- 持久化 URI 权限，支持 Android 11+ 分区存储

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 8 |
| 最低 SDK | API 24 (Android 7.0) |
| 编译 SDK | 34 |
| 构建工具 | AGP 8.2.0 / Gradle 8.5 |
| UI 框架 | AppCompat + Material Components + ViewPager2 |
| 兼容性 | 纯 Java，兼容 AIDE 开发环境 |

## 项目结构

```
com.batchfiletool
├── MainActivity.java         # 主界面：TabLayout + ViewPager2
├── ui/
│   ├── SetupActivity.java    # 首次启动引导向导（4步）
│   ├── SettingsActivity.java # 设置：默认目录、语言切换、打赏
│   ├── ReplaceFragment.java  # 内容替换功能页面
│   └── DeleteFragment.java   # 文件删除功能页面
├── worker/
│   ├── ReplaceTask.java      # AsyncTask：批量文本替换
│   ├── DeleteTask.java       # AsyncTask：批量文件删除
│   └── ScanTask.java         # AsyncTask：glob 模式扫描
└── util/
    ├── LocaleHelper.java     # 多语言管理 + 日志前缀翻译
    └── FileUtil.java         # 文件 I/O、二进制检测、glob 匹配
```

## 构建

### 标准构建（Android Studio / CLI）

```bash
cd batch-file-tool-android
./gradlew assembleRelease
```

输出 APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 架构说明

### 页面导航

```
SetupActivity (首次启动)
    └─[完成引导]─> MainActivity
                      ├── Tab 0: ReplaceFragment
                      ├── Tab 1: DeleteFragment
                      └── [设置按钮] ─> SettingsActivity
```

SetupActivity 完成后保存 `setup_done = true` 标记到 SharedPreferences，后续启动直接进入 MainActivity。

### 后台任务模式

所有文件操作通过 `AsyncTask` 在后台线程执行：

- `ReplaceTask` -- 遍历文件，读取内容，执行替换，写回。通过 `Progress` 内部类回调扫描进度和计数器。
- `DeleteTask` -- 按文件列表逐个删除。通过 `Progress` 内部类回调已删除/失败计数。
- `ScanTask` -- 递归扫描目录，匹配 glob 模式。通过 `Callback` 接口回调匹配文件。

Worker 类通过 `WeakReference<Callback>` 持有回调引用，防止内存泄漏。

### 权限处理

| Android 版本 | 权限方案 |
|-------------|---------|
| API 29 及以下 | `READ/WRITE_EXTERNAL_STORAGE` |
| API 30+ (R+) | `MANAGE_EXTERNAL_STORAGE`，跳转系统设置页申请 |

引导页第 1 步处理权限申请，`onResume()` 中检测权限状态变化。

### 多语言

`LocaleHelper.setLocale()` 包装 `Context`，通过 `createConfigurationContext()` 注入 Locale。Worker 类通过 `Locale.getDefault()` 获取语言环境（在 `setLocale` 中同步设置）。

语言资源：
- `values/strings.xml` -- 英文
- `values-zh/strings.xml` -- 简体中文

`LocaleHelper.logPrefix(key)` 返回 i18n 后的日志标签（如 "扫描中" / "Scanning"）。

### Neumorphism UI 体系

所有 UI 效果通过 XML drawable 实现，无需自定义 View：

| 资源 | 用途 |
|------|------|
| `neumorphic_bg` | 页面背景（内凹） |
| `neumorphic_card` | 卡片区域（外凸） |
| `neumorphic_input` | 输入框（内凹浅色） |
| `neumorphic_button` / `_pressed` / `_selector` | 按钮（外凸/内凹/状态切换） |
| `terminal_dot_*` | 终端红黄绿圆点 |

## 关键 SharedPreferences 键

储存在 `app_settings` 中：

| 键 | 默认值 | 说明 |
|----|--------|------|
| `setup_done` | `false` | 是否完成引导 |
| `default_dir` | `/storage/emulated/0/株馨科技/` | 默认工作目录 |
| `language` | `"auto"` | 语言偏好 (`auto`/`en`/`zh`) |

## 依赖

```groovy
dependencies {
    implementation 'androidx.appcompat:appcompat:1.5.1'
    implementation 'androidx.core:core:1.9.0'
    implementation 'androidx.activity:activity:1.6.1'
    implementation 'androidx.viewpager2:viewpager2:1.0.0'
    implementation 'com.google.android.material:material:1.9.0'
}
```

## 赞赏一下 

如果这个工具对您有帮助，欢迎赞赏支持，您的支持是我持续维护的动力！🎉

| 微信支付 | 支付宝 |
|:---:|:---:|
| ![微信打赏](wx.png) | ![支付宝打赏](zfb.png) |

---

## 成品下载

点击下方链接下载APK安装包：

| 版本 | 下载链接 | 说明 |
|:---:|:---|:---|
| v1.0 | [app-release-unsigned.apk](app/build/outputs/apk/release/app-release-unsigned.apk) | 最新稳定版 |

**安装说明：**
- Android 7.0+ 系统支持
- 首次安装需开启"允许安装未知来源应用"
- 如提示"未签名"属正常，继续安装或者自己用mt管理器签名一下即可使用