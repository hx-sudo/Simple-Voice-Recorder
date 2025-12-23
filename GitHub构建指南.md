# 📘 GitHub Actions 在线构建指南

## 🎯 优势

- ✅ 无需下载 JDK
- ✅ 无需配置本地环境
- ✅ GitHub 服务器自动构建
- ✅ 5-10分钟获得 APK
- ✅ 完全免费

---

## 📋 详细步骤

### 步骤1：初始化 Git 仓库

打开 PowerShell，执行：

```powershell
cd C:\Desktop\recoder\Simple-Voice-Recorder

# 初始化 Git
git init

# 添加所有文件
git add .

# 提交
git commit -m "静默录音工具首次提交"
```

如果提示需要配置用户信息：
```powershell
git config --global user.email "你的邮箱@example.com"
git config --global user.name "你的名字"
```

---

### 步骤2：创建 GitHub 仓库

1. **访问 GitHub**
   - 打开：https://github.com
   - 登录账号（没有的话先注册）

2. **创建新仓库**
   - 点击右上角 "+" → "New repository"
   - 仓库名：`silent-recorder`（或任意名称）
   - **重要**：选择 **Private**（私有仓库，保护隐私）
   - 不要勾选 "Add a README file"
   - 点击 "Create repository"

3. **获取仓库地址**
   - 创建后会显示仓库 URL，格式如：
   - `https://github.com/你的用户名/silent-recorder.git`

---

### 步骤3：上传代码

在 PowerShell 中执行：

```powershell
# 添加远程仓库（替换成你的仓库地址）
git remote add origin https://github.com/你的用户名/silent-recorder.git

# 推送代码
git branch -M main
git push -u origin main
```

如果要求登录：
- 用户名：你的 GitHub 用户名
- 密码：使用 Personal Access Token（不是账号密码）

**获取 Token：**
1. GitHub → Settings → Developer settings
2. Personal access tokens → Tokens (classic)
3. Generate new token
4. 勾选 `repo` 权限
5. 生成并复制 token
6. 用这个 token 作为密码

---

### 步骤4：触发构建

代码上传后：

1. **访问仓库页面**
   - `https://github.com/你的用户名/silent-recorder`

2. **查看 Actions**
   - 点击顶部 "Actions" 标签
   - 会看到正在运行的构建任务

3. **等待构建完成**
   - 构建时间：约 5-10 分钟
   - 状态会从 🟡 变为 ✅

4. **下载 APK**
   - 点击构建任务
   - 滚动到底部 "Artifacts"
   - 下载 `app-debug`
   - 解压得到 `app-debug.apk`

---

### 步骤5：安装 APK

将下载的 APK 传到手机安装，或使用：

```powershell
adb install -r app-debug.apk
```

---

## 🔄 后续修改

如果以后修改了代码，只需：

```powershell
git add .
git commit -m "修改说明"
git push
```

GitHub 会自动重新构建！

---

## ⚠️ 隐私提醒

- ✅ 已设置为私有仓库
- ✅ 只有你能看到代码
- ✅ 构建过程在 GitHub 服务器
- ✅ 代码不会被公开

---

## ❓ 常见问题

### Q: 没有 Git？
安装：https://git-scm.com/download/win

### Q: 推送失败？
检查：
- 仓库地址是否正确
- Token 权限是否足够
- 网络连接

### Q: Actions 标签不见了？
在仓库 Settings → Actions → General
确保 "Allow all actions" 已选中

### Q: 构建失败？
查看构建日志找原因，通常是依赖下载问题
可以点击 "Re-run jobs" 重试

---

## 🎉 完成

构建成功后，你就得到了可用的 APK！

无需任何本地环境配置 🚀

