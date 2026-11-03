#Requires -Version 5.0
<#
Seashore Launcher 一条命令构建（Phase 1 验收复验入口）。

用法：
  .\build.ps1                      # x86_64 debug 包（AVD 主力测试机）
  .\build.ps1 -Arch arm64-v8a      # 真机包
  .\build.ps1 -JdkHome C:\path\to\jdk-17 -SdkDir D:\Android\Sdk

前置（脚本会预检并给明确报错）：
  · JDK 17（Gradle 7.3.3 上限 17，机器仅有 8/21/25 时须指 -JdkHome）
  · Android SDK（build-tools 34.0.0——30.0.3 已从 dl.google 下架）
  · NDK 27.2.12479018（r30 与 xhook 不兼容：PAGE_SIZE/PAGE_MASK 被新 Bionic 移除）
#>
param(
    [string]$Arch = "x86_64",
    [string]$NdkVer = "27.2.12479018",
    [string]$JdkHome = "",
    [string]$SdkDir = ""
)
$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $MyInvocation.MyCommand.Path

function Die([string]$msg) {
    Write-Host "[build] 失败：$msg" -ForegroundColor Red
    exit 1
}

# ---------- JDK 17 探测 ----------
function Test-Jdk17([string]$jdkPath) {
    $java = Join-Path $jdkPath "bin\java.exe"
    if (-not (Test-Path $java)) { return $false }
    # java -version 走 stderr；ErrorActionPreference=Stop 下须临时降级，否则被当终止错误
    $prev = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    $v = (& $java -version 2>&1 | Out-String)
    $ErrorActionPreference = $prev
    $v -match '"17\.'
}
if ($JdkHome) {
    if (-not (Test-Jdk17 $JdkHome)) { Die "指定的 -JdkHome $JdkHome 不是 JDK 17" }
} else {
    $cands = @()
    if ($env:JAVA_HOME) { $cands += , $env:JAVA_HOME }
    $cands += (Get-ChildItem "${env:ProgramFiles}\Eclipse Adoptium\jdk-17*", "${env:ProgramFiles}\Microsoft\jdk-17*",
               "E:\4k\tools\jdk17\jdk-17*" -Directory -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $JdkHome = $cands | Where-Object { Test-Jdk17 $_ } | Select-Object -First 1
    if (-not $JdkHome) {
        Die "未找到 JDK 17（Gradle 7.3.3 上限 17）。用 -JdkHome 指定，例：.\\build.ps1 -JdkHome C:\\tools\\jdk-17"
    }
}
$env:JAVA_HOME = $JdkHome
Write-Host "[build] JDK   = $JdkHome"

# ---------- Android SDK 探测 + local.properties 自愈 ----------
if (-not $SdkDir) {
    $lp = Join-Path $repo "local.properties"
    if (Test-Path $lp) {
        $line = Select-String -Path $lp -Pattern "^sdk\.dir=(.+)$" | Select-Object -First 1
        if ($line) { $SdkDir = $line.Matches[0].Groups[1].Value -replace '\\:', ':' -replace '\\\\', '\' }
    }
    if (-not $SdkDir -and $env:ANDROID_HOME) { $SdkDir = $env:ANDROID_HOME }
    if (-not $SdkDir -and $env:ANDROID_SDK_ROOT) { $SdkDir = $env:ANDROID_SDK_ROOT }
    if (-not $SdkDir -and (Test-Path "D:\Android\Sdk")) { $SdkDir = "D:\Android\Sdk" }
    if ($SdkDir -and -not (Test-Path (Join-Path $repo "local.properties"))) {
        # 全新 clone 没有 local.properties：写出来（该文件在 .gitignore 内，机器相关件不入库）
        "sdk.dir=$($SdkDir -replace '\\', '\\')" | Set-Content (Join-Path $repo "local.properties") -Encoding ASCII
        Write-Host "[build] 已生成 local.properties（sdk.dir=$SdkDir）"
    }
}
if (-not $SdkDir -or -not (Test-Path $SdkDir)) {
    Die "未找到 Android SDK。用 -SdkDir 指定（须含 build-tools 34.0.0），例：.\\build.ps1 -SdkDir D:\\Android\\Sdk"
}
if (-not (Get-ChildItem (Join-Path $SdkDir "build-tools") -Directory -ErrorAction SilentlyContinue |
        Where-Object Name -like "34.*")) {
    Die "SDK $SdkDir 缺 build-tools 34.x（30.0.3 已从 dl.google 下架，装 34.0.0）"
}
Write-Host "[build] SDK   = $SdkDir"

# ---------- NDK 预检 ----------
if (-not (Test-Path (Join-Path $SdkDir "ndk\$NdkVer"))) {
    $have = (Get-ChildItem (Join-Path $SdkDir "ndk") -Directory -ErrorAction SilentlyContinue |
            ForEach-Object Name) -join ", "
    Die "缺 NDK $NdkVer（本机已装：$have）。r30 与 xhook 不兼容，请装 $NdkVer 或用 -NdkVer 指定已装版本"
}
Write-Host "[build] NDK   = $NdkVer"
Write-Host "[build] arch  = $Arch"

# ---------- 构建 ----------
# PS5 陷阱：含点参数必须整体加引号（裸参 -DndkVer=27.2 会被按点切分）
& (Join-Path $repo "gradlew.bat") -p $repo :QCL:assembleDebug "-Darch=$Arch" "-DndkVer=$NdkVer"
if ($LASTEXITCODE -ne 0) { Die "gradlew 退出码 $LASTEXITCODE" }

# ---------- 产物报告 ----------
$apk = Get-ChildItem (Join-Path $repo "QCL\build*\outputs\apk") -Recurse -Filter "*.apk" -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -gt (Get-Date).AddMinutes(-90) } | Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
if (-not $apk) { Die "构建命令成功但未在 QCL\build*\outputs\apk 找到新 APK" }
$mb = [math]::Round($apk.Length / 1MB, 1)
Write-Host "[build] 完成：$($apk.FullName)（$mb MB）" -ForegroundColor Green
