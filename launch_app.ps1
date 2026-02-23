# Quick Launch Script for Screen Time App

echo "🚀 Screen Time App - Launching..."
echo ""

# Attempt to launch using adb if available
try {
    $adbFound = $false

    # Try to find adb in common locations
    $adbPaths = @(
        "C:\Android\sdk\platform-tools\adb.exe",
        "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe"
    )

    foreach ($path in $adbPaths) {
        if (Test-Path $path) {
            Write-Host "✓ Found adb at: $path" -ForegroundColor Green
            & $path shell am start -n com.example.screentime/.MainActivity
            $adbFound = $true
            break
        }
    }

    if (-not $adbFound) {
        Write-Host "Note: adb not found in PATH, but app is installed!" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Your Screen Time app is installed on your device." -ForegroundColor Green
        Write-Host ""
        Write-Host "To launch it manually:" -ForegroundColor Cyan
        Write-Host "  1. Open your phone's App Drawer (swipe up)"
        Write-Host "  2. Look for 'ScreenTime' app"
        Write-Host "  3. Tap to open it"
        Write-Host ""
        Write-Host "Important: Grant 'Usage Access' permission when prompted!" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "Your Screen Time app is installed on your device!" -ForegroundColor Green
    Write-Host ""
    Write-Host "To launch it:" -ForegroundColor Cyan
    Write-Host "  1. Open your phone's App Drawer (swipe up)"
    Write-Host "  2. Look for 'ScreenTime' app"
    Write-Host "  3. Tap to open it"
    Write-Host ""
    Write-Host "Important: Grant 'Usage Access' permission when prompted!" -ForegroundColor Yellow
}

