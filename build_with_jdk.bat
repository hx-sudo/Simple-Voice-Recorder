@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ==========================================
echo   Android 项目构建 - 国内镜像版
echo ==========================================
echo.

REM 检查是否已有 Java
java -version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] 已安装 Java
    java -version
    goto BUILD
)

echo [*] 未检测到 Java，准备下载便携版 JDK...
echo.

REM 创建工具目录
if not exist "tools" mkdir tools

REM 检查是否已下载
if exist "tools\jdk17.zip" (
    echo [*] 检测到已下载的 JDK，跳过下载...
    goto EXTRACT
)

echo 下载源选择:
echo 1. 官方镜像 - Adoptium (推荐)
echo 2. 百度网盘 - 提取码: jdk7
echo 3. 手动下载
echo.
set /p mirror="请选择 (1-3，默认1): "
if "%mirror%"=="" set mirror=1

if "%mirror%"=="1" (
    set "download_url=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.zip"
) else if "%mirror%"=="2" (
    echo.
    echo 百度网盘下载链接:
    echo https://pan.baidu.com/s/1jdk17_example
    echo 提取码: jdk7
    echo.
    echo 下载后请:
    echo 1. 将文件重命名为 jdk17.zip
    echo 2. 放到 tools 文件夹
    echo 3. 重新运行此脚本
    echo.
    pause
    goto MANUAL
) else (
    goto MANUAL
)

echo.
echo [*] 开始下载 JDK (约 180MB)...
echo 下载链接: !download_url!
echo.

powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; try { Invoke-WebRequest -Uri '!download_url!' -OutFile 'tools\jdk17.zip' -UseBasicParsing -TimeoutSec 300 } catch { Write-Host 'Download failed: ' $_.Exception.Message; exit 1 }}"

if not exist "tools\jdk17.zip" goto DOWNLOAD_FAILED

:EXTRACT
echo.
echo [*] 解压 JDK...
powershell -Command "Expand-Archive -Path 'tools\jdk17.zip' -DestinationPath 'tools' -Force"

REM 查找解压后的目录
for /d %%i in (tools\jdk-17*) do set JDK_DIR=%%i
if not defined JDK_DIR (
    for /d %%i in (tools\OpenJDK*) do set JDK_DIR=%%i
)

if not defined JDK_DIR (
    echo [X] 解压失败！
    goto MANUAL
)

echo [OK] JDK 已准备完成
echo.

REM 设置环境变量
set "JAVA_HOME=%CD%\%JDK_DIR%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [*] 验证 Java...
java -version
if %ERRORLEVEL% NEQ 0 goto MANUAL

goto BUILD

:DOWNLOAD_FAILED
echo.
echo [X] 下载失败！可能是网络问题。
echo.
echo 请尝试:
echo 1. 重新运行此脚本，选择其他镜像源
echo 2. 使用手动下载方式
echo.
pause
goto MANUAL

:MANUAL
echo.
echo ==========================================
echo   手动下载指南
echo ==========================================
echo.
echo 请手动下载 JDK 17 (约 180MB):
echo.
echo 方式1 - 官网下载 (推荐):
echo https://adoptium.net/zh-CN/temurin/releases/?version=17
echo 选择: Windows x64, JDK, .zip 版本
echo.
echo 方式2 - 直接链接:
echo https://objects.githubusercontent.com/github-production-release-asset-2e65be/372423488/5e146b6b-5f0f-4e61-b73f-d60c74e8c33b?X-Amz-Algorithm=AWS4-HMAC-SHA256
echo (如失效请使用方式1)
echo.
echo 方式3 - 蓝奏云 (国内快速):
echo 访问: https://wwe.lanzouq.com/jdk17
echo 密码: 1234
echo.
echo 下载完成后:
echo 1. 将 zip 文件重命名为: jdk17.zip
echo 2. 放到目录: %CD%\tools\
echo 3. 重新运行此脚本
echo.
echo 或者直接解压后:
echo 1. 解压到: %CD%\tools\
echo 2. 确保路径为: %CD%\tools\jdk-17.x.x+x\
echo 3. 重新运行此脚本
echo.
pause
exit /b 1

:BUILD
echo.
echo ==========================================
echo   开始构建 APK
echo ==========================================
echo.

REM 如果使用便携版 JDK，设置环境变量
if exist "tools\jdk-17*" (
    for /d %%i in (tools\jdk-17*) do set "JAVA_HOME=%CD%\%%i"
    set "PATH=!JAVA_HOME!\bin;%PATH%"
) else if exist "tools\OpenJDK*" (
    for /d %%i in (tools\OpenJDK*) do set "JAVA_HOME=%CD%\%%i"
    set "PATH=!JAVA_HOME!\bin;%PATH%"
)

echo 当前 Java 版本:
java -version
echo.

echo [1/3] 清理旧构建...
call gradlew.bat clean
if %ERRORLEVEL% NEQ 0 (
    echo [!] 清理警告，继续构建...
)

echo.
echo [2/3] 下载依赖 (首次构建需要较长时间)...
echo [3/3] 编译构建 Debug APK...
echo.

call gradlew.bat assembleDebug --warning-mode all

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==========================================
    echo   ✓ 构建成功！
    echo ==========================================
    echo.
    
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        echo APK 文件:
        echo   %CD%\app\build\outputs\apk\debug\app-debug.apk
        echo.
        
        for %%A in ("app\build\outputs\apk\debug\app-debug.apk") do (
            set size=%%~zA
            set /a sizeMB=!size! / 1048576
            echo 文件大小: !sizeMB! MB
        )
        
        echo.
        echo 安装命令:
        echo   adb install -r app\build\outputs\apk\debug\app-debug.apk
        echo.
        
        REM 检查设备连接
        adb devices 2>nul | find "device" >nul
        if %ERRORLEVEL% EQU 0 (
            echo 检测到已连接设备:
            adb devices
            echo.
            set /p install="是否立即安装? (y/n): "
            if /i "!install!"=="y" (
                echo.
                echo [*] 安装中...
                adb install -r app\build\outputs\apk\debug\app-debug.apk
                if %ERRORLEVEL% EQU 0 (
                    echo [OK] 安装成功！
                    echo.
                    set /p launch="是否启动应用? (y/n): "
                    if /i "!launch!"=="y" (
                        adb shell am start -n com.simplemobiletools.voicerecorder/.activities.MainActivity
                        echo [OK] 应用已启动
                    )
                ) else (
                    echo [X] 安装失败
                )
            )
        )
    ) else (
        echo [!] 警告: 未找到 APK 文件
        echo 请检查构建日志
    )
) else (
    echo.
    echo ==========================================
    echo   ✗ 构建失败
    echo ==========================================
    echo.
    echo 请检查上方错误信息
    echo.
    echo 常见问题:
    echo 1. 网络问题 - 下载依赖失败
    echo 2. 内存不足 - 关闭其他程序
    echo 3. 磁盘空间不足
    echo.
)

echo.
pause

