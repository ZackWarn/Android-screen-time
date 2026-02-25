@echo off
echo ========================================
echo   Installing Screen Time App
echo ========================================
echo.

D:\sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   Installation Successful!
    echo   Launching app...
    echo ========================================
    echo.
    D:\sdk\platform-tools\adb.exe shell am start -n com.example.screentime/.MainActivity

    echo.
    echo ========================================
    echo   FEATURES ADDED:
    echo ========================================
    echo   1. Search Bar - Filter apps by name
    echo   2. Enhanced Notifications with vibration
    echo   3. "Go Home" button on notifications
    echo.
    echo   HOW TO USE:
    echo   - Set a limit for any app
    echo   - Open that app when limit exceeded
    echo   - Notification will appear
    echo   - TAP notification to exit the app
    echo ========================================
    echo.
) else (
    echo.
    echo Installation failed! Make sure emulator is running.
    echo.
)

pause

