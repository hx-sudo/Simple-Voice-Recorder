@echo off
chcp 65001 >nul
echo ==========================================
echo   JDK 17 下载助手
echo ==========================================
echo.
echo 打开浏览器下载 JDK...
echo.

REM 打开下载页面
start https://adoptium.net/zh-CN/temurin/releases/?version=17

echo.
echo 在打开的页面中:
echo 1. 找到 "Temurin 17 (LTS)"
echo 2. 选择: Windows x64, JDK, .zip
echo 3. 点击下载
echo.
echo 下载完成后:
echo 1. 将文件重命名为: jdk17.zip
echo 2. 放到文件夹: %CD%\tools\
echo 3. 运行: build_with_jdk.bat
echo.
echo ==========================================
echo   或者使用这些直接下载链接:
echo ==========================================
echo.
echo 备选链接1 (推荐):
echo https://objects.githubusercontent.com/github-production-release-asset-2e65be/372423488/
echo.
echo 备选链接2:
echo https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip
echo.
echo 选择一个链接复制到浏览器下载
echo.
pause

REM 创建 tools 目录
if not exist "tools" mkdir tools

echo.
echo [OK] tools 文件夹已创建
echo 请将下载的 jdk17.zip 放到这里
echo.
echo 完成后运行: build_with_jdk.bat
pause

