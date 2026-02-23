@echo off
REM Screen Time App - Installation Helper Script
REM This script helps set up ADB and install the app

echo.
echo =============================================================================
echo         SCREEN TIME APP - DEVICE INSTALLATION HELPER
echo =============================================================================
echo.

REM Check if device is connected
echo [1/5] Checking for connected devices...
adb devices

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: ADB not found in PATH
    echo.
    echo SOLUTION: You need to add Android SDK platform-tools to your PATH
    echo.
    echo Steps:
    echo 1. Open Android Studio
    echo 2. Go to File ^> Settings ^> Android SDK
    echo 3. Note the Android SDK Location (e.g., C:\Users\username\AppData\Local\Android\Sdk)
    echo 4. Add this to your PATH: [SDK_Location]\platform-tools
    echo.
    echo Then try again.
    echo.
    pause
    exit /b 1
)

echo.
echo [2/5] Waiting for device to be ready...
adb wait-for-device

echo.
echo [3/5] Uninstalling old version (if exists)...
adb uninstall com.example.screentime

echo.
echo [4/5] Installing Screen Time App...
adb install "C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk"

if %ERRORLEVEL% EQ 0 (
    echo.
    echo [5/5] Installation successful!
    echo.
    echo =============================================================================
    echo                    ✓ APP INSTALLED SUCCESSFULLY!
    echo =============================================================================
    echo.
    echo Next steps:
    echo 1. On your device, go to Settings ^> Apps ^> ScreenTime
    echo 2. Tap Permissions and enable "Usage Access"
    echo 3. Open ScreenTime app from your app drawer
    echo 4. Explore the Dashboard, Analytics, and Rewards screens
    echo.
    pause
) else (
    echo.
    echo ERROR: Installation failed
    echo Please check the error message above
    echo.
    pause
    exit /b 1
)

