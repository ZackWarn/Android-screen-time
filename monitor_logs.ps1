# Monitor Screen Time app logs in real-time
D:\sdk\platform-tools\adb.exe logcat -c
Write-Host "Logs cleared. Monitoring Screen Time logs..." -ForegroundColor Green
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Yellow
Write-Host ""

D:\sdk\platform-tools\adb.exe logcat -s AppMonitorService:D AppLimitManager:D

