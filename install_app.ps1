# Screen Time App - ADB Setup & Installation Script
# Run this script with PowerShell

Write-Host ""
Write-Host "═════════════════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "           SCREEN TIME APP - ADB SETUP & INSTALLATION SCRIPT" -ForegroundColor Cyan
Write-Host "═════════════════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Step 1: Find Android SDK
Write-Host "[1/6] Searching for Android SDK..." -ForegroundColor Yellow

$androidSdkPaths = @(
    "C:\Android\sdk",
    "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk",
    "$env:ANDROID_HOME",
    "C:\Program Files\Android\Sdk"
)

$platformToolsPath = $null

foreach ($basePath in $androidSdkPaths) {
    if ($basePath -and (Test-Path "$basePath\platform-tools\adb.exe")) {
        $platformToolsPath = "$basePath\platform-tools"
        Write-Host "✓ Found Android SDK at: $basePath" -ForegroundColor Green
        break
    }
}

if (-not $platformToolsPath) {
    Write-Host ""
    Write-Host "ERROR: Android SDK not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "SOLUTION: You need to find your Android SDK location:" -ForegroundColor Yellow
    Write-Host "  1. Open Android Studio"
    Write-Host "  2. Go to File > Settings > Android SDK"
    Write-Host "  3. Look for 'Android SDK Location'"
    Write-Host "  4. Copy that path"
    Write-Host "  5. Run this script again"
    Write-Host ""
    Write-Host "Or manually set up ADB in your PATH:"
    Write-Host "  [System Properties] > Environment Variables > Path > Add: [SDK]\platform-tools"
    Write-Host ""
    pause
    exit 1
}

# Step 2: Add to PATH if not already there
Write-Host ""
Write-Host "[2/6] Adding platform-tools to PATH..." -ForegroundColor Yellow

if ($env:Path -notlike "*platform-tools*") {
    $env:Path += ";$platformToolsPath"
    Write-Host "✓ Added to PATH: $platformToolsPath" -ForegroundColor Green
} else {
    Write-Host "✓ Already in PATH" -ForegroundColor Green
}

# Step 3: Check for connected devices
Write-Host ""
Write-Host "[3/6] Checking for connected devices..." -ForegroundColor Yellow

$adbPath = "$platformToolsPath\adb.exe"

if (-not (Test-Path $adbPath)) {
    Write-Host "ERROR: adb.exe not found at $adbPath" -ForegroundColor Red
    pause
    exit 1
}

$devices = & $adbPath devices

Write-Host $devices -ForegroundColor Gray

if ($devices -notlike "*device*" -or $devices -like "*offline*") {
    Write-Host ""
    Write-Host "⚠ No devices found or device is offline" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "  1. Plug your Android phone into your computer via USB"
    Write-Host "  2. On your phone, enable Settings > Developer Options > USB Debugging"
    Write-Host "  3. Tap 'Allow' if prompted about USB debugging"
    Write-Host "  4. Run this script again"
    Write-Host ""
    pause
    exit 1
}

Write-Host "✓ Device found!" -ForegroundColor Green

# Step 4: Uninstall old version
Write-Host ""
Write-Host "[4/6] Removing old app version..." -ForegroundColor Yellow

& $adbPath uninstall com.example.screentime | Out-Null
Write-Host "✓ Old version removed (or didn't exist)" -ForegroundColor Green

# Step 5: Install app
Write-Host ""
Write-Host "[5/6] Installing Screen Time App..." -ForegroundColor Yellow

$apkPath = "C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $apkPath)) {
    Write-Host "ERROR: APK not found at $apkPath" -ForegroundColor Red
    pause
    exit 1
}

$installOutput = & $adbPath install $apkPath

if ($installOutput -like "*Success*") {
    Write-Host "✓ App installed successfully!" -ForegroundColor Green
} else {
    Write-Host "ERROR: Installation failed" -ForegroundColor Red
    Write-Host $installOutput -ForegroundColor Red
    pause
    exit 1
}

# Step 6: Launch app
Write-Host ""
Write-Host "[6/6] Launching Screen Time App..." -ForegroundColor Yellow

& $adbPath shell am start -n com.example.screentime/.MainActivity | Out-Null
Write-Host "✓ App launched!" -ForegroundColor Green

# Success message
Write-Host ""
Write-Host "═════════════════════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "                   ✓ INSTALLATION COMPLETE!" -ForegroundColor Green
Write-Host "═════════════════════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Cyan
Write-Host "  1. On your device, grant 'Usage Access' permission:"
Write-Host "     Settings > Apps > ScreenTime > Permissions > Usage Access > ON"
Write-Host ""
Write-Host "  2. Explore the app:"
Write-Host "     • 🏠 Home (Dashboard) - Daily tracking"
Write-Host "     • 📊 Analytics - Week comparison"
Write-Host "     • ⭐ Rewards - Badges collection"
Write-Host ""
Write-Host "  3. Use your device normally for data collection"
Write-Host ""
Write-Host "  4. Check back tomorrow to see screen time data"
Write-Host ""
Write-Host "═════════════════════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host ""

pause

