# 🎙️ 静默录音工具 (Silent Recorder)

> ⚠️ **法律声明：本应用仅供个人合法使用，禁止用于窃听或侵犯他人隐私！**

基于 [Simple Voice Recorder](https://github.com/SimpleMobileTools/Simple-Voice-Recorder) 改造的隐秘录音工具，专为个人语音备忘和合法会议记录场景设计。

---

## ✨ 主要特性

### 🔇 完全静默
- ✅ 无启动音效
- ✅ 无录音提示音
- ✅ 无停止音效
- ✅ 无振动反馈
- ✅ 无 Toast 弹窗
- ✅ 通知栏最小化

### 🌙 后台持久
- ✅ 支持息屏录音
- ✅ 支持锁屏录音
- ✅ 长时间后台运行
- ✅ 防止被系统杀后台
- ✅ WakeLock 保持运行

### 🔒 隐私保护
- ✅ 无网络权限
- ✅ 文件存储在隐藏目录
- ✅ 时间戳文件名（不易识别）
- ✅ 不添加到系统媒体库
- ✅ 仅申请必要权限

### 🎨 极简界面
- ✅ 纯黑色单页面
- ✅ 仅一个录音开关按钮
- ✅ 实时时长显示
- ✅ 录音状态指示

---

## 📸 界面预览

```
┌─────────────────────────┐
│                         │
│      00:05:32          │  ← 录音时长
│         ●              │  ← 红色指示点
│                         │
│         🎤             │  ← 录音按钮
│      (或 ⏹️)           │
│                         │
│    Ready to record     │  ← 状态文本
│    (或 Recording)       │
│                         │
└─────────────────────────┘
```

---

## 🚀 快速开始

### 1. 构建 APK

**Windows：**
```cmd
cd Simple-Voice-Recorder
build.bat
```

**Linux/Mac：**
```bash
cd Simple-Voice-Recorder
chmod +x build.sh
./build.sh
```

**手动构建：**
```bash
./gradlew clean assembleDebug
```

### 2. 安装应用

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. 配置权限

启动应用后：
1. 授予录音权限
2. 允许电池优化豁免
3. （厂商系统）进行额外后台设置

### 4. 开始使用

- 点击麦克风按钮开始录音
- 可以息屏、锁屏、后台运行
- 再次点击停止录音
- 文件自动保存

---

## 📁 文件存储位置

录音保存在：
```
/storage/emulated/0/Android/data/com.simplemobiletools.voicerecorder/files/.recordings/
```

文件命名：`时间戳.m4a`（例如：`1703345678901.m4a`）

### 如何访问文件？

**方法1：文件管理器**
1. 开启"显示隐藏文件"
2. 导航到上述路径
3. 进入 `.recordings` 文件夹

**方法2：ADB命令**
```bash
# 查看文件列表
adb shell ls -la /storage/emulated/0/Android/data/com.simplemobiletools.voicerecorder/files/.recordings/

# 拉取到电脑
adb pull /storage/emulated/0/Android/data/com.simplemobiletools.voicerecorder/files/.recordings/ ./recordings/
```

---

## 🔧 厂商系统配置

### 小米 MIUI
```
设置 → 应用设置 → 应用管理 → Voice Recorder
├── 省电策略 → 无限制
├── 自启动 → 开启
└── 后台弹出界面 → 允许
```

### 华为 EMUI / HarmonyOS
```
设置 → 应用和服务 → 应用管理 → Voice Recorder
├── 启动管理 → 手动管理（全部开启）
└── 电池 → 允许后台运行
```

### OPPO ColorOS
```
设置 → 电池 → 应用耗电管理 → Voice Recorder
└── 允许后台运行
```

### Vivo
```
设置 → 电池 → 后台高耗电 → Voice Recorder
└── 允许
```

---

## 📊 系统要求

- **Android 版本**：10 - 14 (API 29-34)
- **存储空间**：至少 100 MB
- **推荐配置**：Android 12+, 2GB+ RAM

---

## 🛠️ 技术栈

- **语言**：Kotlin
- **最小 SDK**：Android 10 (API 29)
- **目标 SDK**：Android 14 (API 34)
- **录音引擎**：MediaRecorder / LAME MP3
- **架构**：Foreground Service + EventBus

---

## 📖 文档

- [📘 使用指南](USAGE_GUIDE.md) - 详细使用说明
- [🧪 测试指南](TEST_GUIDE.md) - 完整测试流程
- [📝 改造说明](MODIFICATION_SUMMARY.md) - 技术改造细节

---

## ⚡ 核心功能实现

### 静默录音
- 移除所有音效调用
- 禁用振动反馈
- Toast 提示全部移除

### 通知最小化
```kotlin
NotificationCompat.Builder(this, channelId)
    .setSmallIcon(R.drawable.ic_empty)  // 透明图标
    .setPriority(Notification.PRIORITY_MIN)
    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
    .setSilent(true)
```

### 后台保活
```kotlin
// WakeLock
val wakeLock = powerManager.newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK,
    "SilentRecorder::RecordingWakeLock"
)
wakeLock.acquire(3 * 60 * 60 * 1000L)

// 前台服务
startForeground(NOTIFICATION_ID, notification)
```

### 隐藏存储
```kotlin
val hiddenFolder = File(getExternalFilesDir(null), ".recordings")
val fileName = "${System.currentTimeMillis()}.m4a"
```

---

## ⚠️ 免责声明

### 法律责任
1. 本应用仅供**个人合法使用**
2. 禁止用于**窃听、偷拍**等违法行为
3. 使用者需遵守**当地法律法规**
4. 任何违法使用后果**由使用者自行承担**

### 使用场景
✅ **合法场景：**
- 录制个人语音备忘
- 已获得同意的会议记录
- 个人学习录音
- 合法的工作记录

❌ **违法场景：**
- 未经同意录制他人对话
- 侵犯他人隐私
- 用于非法证据收集
- 任何违法用途

### 隐私承诺
- ✅ 无网络权限，数据不上传
- ✅ 本地存储，用户完全控制
- ✅ 无第三方 SDK
- ✅ 无广告、无追踪

---

## 🐛 已知限制

1. **通知栏图标**
   - Android 8.0+ 系统要求前台服务必须显示通知
   - 已最小化，但无法完全隐藏

2. **厂商限制**
   - 小米、华为等系统需手动配置后台运行
   - 部分机型可能仍被系统限制

3. **电池消耗**
   - 长时间录音会消耗一定电量
   - 预计：~2-3% / 小时

4. **存储限制**
   - 录音文件占用存储空间
   - 建议定期清理

---

## 🔐 安全建议

1. **定期清理**
   - 及时转移重要录音
   - 删除不需要的文件

2. **密码保护**
   - 使用系统应用锁功能
   - 设置文件夹加密

3. **合法使用**
   - 仅在允许的场景使用
   - 告知相关人员录音事实

---

## 📞 支持与反馈

### 问题报告
如遇到技术问题，请提供：
- 设备型号
- 系统版本
- 问题描述
- 复现步骤

### 功能建议
欢迎提出合理的功能建议，但以下功能**不会**实现：
- ❌ 云端上传（违背无网络原则）
- ❌ 远程控制（有安全风险）
- ❌ 完全隐藏应用（需要 Root）

---

## 📜 开源协议

基于 [Simple Voice Recorder](https://github.com/SimpleMobileTools/Simple-Voice-Recorder) 改造

原项目协议：GPL-3.0 License

---

## 🙏 致谢

- [Simple Mobile Tools](https://github.com/SimpleMobileTools) - 原版录音应用
- [EventBus](https://github.com/greenrobot/EventBus) - 事件总线
- [android-lame](https://github.com/naman14/TAndroidLame) - MP3 编码

---

## ⭐ Star History

如果这个项目对你有帮助，请给个 Star ⭐

**记住：请合法、负责任地使用本工具！**

---

**版本：** v1.0.0  
**更新日期：** 2024-12-23  
**状态：** ✅ 稳定版

