# 🚀 INSTALLATION STEPS - FOLLOW THIS GUIDE

## ✅ Your App is Ready to Install!

The Screen Time App APK has been successfully built and is ready to deploy to your Android device.

---

## 📍 APK LOCATION

```
C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk
```

---

## 🎯 INSTALLATION METHOD (RECOMMENDED - ADB)

### Prerequisites
1. **Android SDK Platform Tools** (includes ADB)
   - Usually installed with Android Studio
   - If not, download from: https://developer.android.com/tools/releases/platform-tools

2. **Physical Android Device**
   - Android 8.0 or higher
   - USB cable

3. **Developer Mode Enabled**
   - Settings → About Phone → Tap "Build Number" 7 times
   - Go back to Settings → Developer Options
   - Enable "USB Debugging"

### Installation Steps

#### Step 1: Connect Device
1. Plug your Android device into your computer via USB cable
2. A prompt may appear on your device asking to allow USB debugging
3. Tap "Allow" on your device

#### Step 2: Verify Connection
Open PowerShell and run:
```powershell
adb devices
```

**Expected output:**
```
List of attached devices
ZD222DNYXW            device
```

If you see your device with status "device" (not "offline"), you're ready!

**If offline or not showing:**
- Reconnect USB cable
- Try: `adb kill-server` then `adb start-server`
- Ensure USB Debugging is enabled

#### Step 3: Install the App
Copy and paste this command in PowerShell:

```powershell
adb install "C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk"
```

**Expected output:**
```
Success
```

If you see "Success", the app has been installed! ✅

#### Step 4: Launch the App
On your Android device:
1. Open the app drawer (swipe up or tap app icon)
2. Look for "ScreenTime" app
3. Tap to open

---

## 📱 FIRST-TIME SETUP (IMPORTANT!)

When you first open the app, you MUST grant the "Usage Access" permission:

### Grant Permission Steps

1. **When app opens**, you might see a permission request
2. **If not shown**, go to:
   - Settings → Apps → ScreenTime (or All Apps)
   - Tap ScreenTime
   - Tap "Permissions"
   - Find "Usage Access" (may be under "Special App Access")
   - Toggle it ON

3. **Close and reopen** the app after granting permission

**Without this permission:**
- App will work, but won't capture screen time data
- Dashboard will show 0 minutes
- No badges will unlock

---

## 🎮 FIRST LAUNCH WALKTHROUGH

### What You'll See

**Screen 1: Dashboard (Home Tab)**
- Mostly empty initially
- Week number displayed
- 0 / 115 points
- "No badges unlocked yet" message
- Daily breakdown section (empty)

**Screen 2: Analytics (Info Tab)**
- No data initially
- Will populate after first full week

**Screen 3: Rewards (Favorite Tab)**
- 5 badges all showing as "Locked"
- 0 points earned this week

### What to Do

1. **Explore the 3 tabs** at the bottom
2. **Use your device normally** for a few hours
3. **Come back to the app** the next day
4. **You'll see** screen time data in Dashboard

---

## ✅ VERIFICATION CHECKLIST

After installation, verify everything works:

- [ ] App installed successfully
- [ ] App icon appears in app drawer
- [ ] App opens without crashing
- [ ] "Usage Access" permission granted
- [ ] Can tap all 3 bottom tabs
- [ ] Dashboard screen displays
- [ ] Analytics screen displays
- [ ] Rewards screen displays
- [ ] All text is readable
- [ ] Bottom navigation works

---

## 🆘 IF INSTALLATION FAILS

### Error: "App not installed"
**Solution:**
1. First uninstall any previous version:
   ```powershell
   adb uninstall com.example.screentime
   ```
2. Then try install again:
   ```powershell
   adb install "C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk"
   ```

### Error: "No devices found" or "Device is OFFLINE"
**Solution:**
1. Disconnect USB cable
2. Close Android Studio (if open)
3. Open PowerShell and run:
   ```powershell
   adb kill-server
   Start-Sleep -Seconds 2
   adb start-server
   Start-Sleep -Seconds 2
   adb devices
   ```
4. Reconnect USB cable
5. On device, tap "Allow" for USB Debugging if prompted
6. Try install again

### Error: "Command not found 'adb'"
**Solution:**
1. Add ADB to PATH:
   ```powershell
   # Find Android SDK location in Android Studio Settings
   # Usually: C:\Users\YourUsername\AppData\Local\Android\Sdk\platform-tools
   
   # Add to environment:
   $env:Path += ";C:\Users\vibin\AppData\Local\Android\Sdk\platform-tools"
   ```
2. Try install again

### Error: "App crashes on launch"
**Solution:**
1. Check for errors:
   ```powershell
   adb logcat
   ```
2. Look for red error messages
3. Try uninstalling and reinstalling
4. Ensure device has Android 8.0+ (API 26+)

---

## 🔄 ALTERNATIVE INSTALLATION METHODS

### Method 2: Android Studio (If ADB fails)
1. Open project in Android Studio
2. Connect device via USB
3. Click green "Run" button (play icon)
4. Select your device from dialog
5. Click "OK"
6. Android Studio will build and install

### Method 3: Manual File Transfer
1. Copy the APK to device via cloud or USB file transfer
2. Open file manager on device
3. Navigate to the APK file
4. Tap to install
5. Allow installation from unknown sources if prompted

### Method 4: Gradle Command
If ADB is in PATH:
```powershell
cd C:\Users\vibin\AndroidStudioProjects\ScreenTime
.\gradlew installDebug
```

---

## 📊 EXPECTED BEHAVIOR TIMELINE

### Immediately After Install
- App opens
- Dashboard shows 0 points
- No screen time data (haven't collected any yet)
- All badges locked

### After 24 Hours
- Daily screen time appears in Dashboard
- Points calculated
- Daily breakdown shows usage
- Badges still locked (waiting for conditions to be met)

### After 5-7 Days
- Full week of data visible
- Analytics shows week data
- Some badges may unlock based on behavior
- Previous week data archived

### After 14+ Days
- Week-over-week comparison visible
- Multiple weeks of historical data
- Badge unlocking based on achieving goals
- Points reset every Sunday

---

## 🎯 NEXT STEPS AFTER INSTALLATION

1. ✅ **Install app** using steps above
2. ✅ **Grant "Usage Access" permission**
3. ✅ **Explore all 3 screens** (Dashboard, Analytics, Rewards)
4. ✅ **Use device normally** for a day
5. ✅ **Return to app** to see screen time data
6. ✅ **Check Analytics** for week comparison
7. ✅ **Try to unlock badges** by meeting conditions

---

## 💡 TIPS FOR BEST EXPERIENCE

1. **Keep USB Debugging Enabled** while testing
2. **Grant All Permissions** when prompted
3. **Use Device Normally** for realistic screen time data
4. **Wait 24 Hours** before checking Dashboard for data
5. **Check Permissions** if no data appears after 24 hours
6. **Restart App** after granting new permissions

---

## 📞 QUICK REFERENCE

| Task | Command |
|------|---------|
| Install | `adb install "path/to/app-debug.apk"` |
| Uninstall | `adb uninstall com.example.screentime` |
| View Logs | `adb logcat` |
| List Devices | `adb devices` |
| Restart ADB | `adb kill-server && adb start-server` |

---

## ✨ YOU'RE ALL SET!

Your Screen Time App is ready to use. Follow the installation steps above, and you'll have it running on your device in minutes!

### Summary
1. ✅ APK Built: `app\build\outputs\apk\debug\app-debug.apk`
2. ✅ Ready to Install: `adb install "path/to/app-debug.apk"`
3. ✅ Grant Permission: "Usage Access" in Settings
4. ✅ Enjoy: Dashboard, Analytics, Rewards tabs

---

**Happy Tracking! 🎉📱**

For more detailed help, see:
- `DEPLOYMENT.md` - Full installation guide
- `QUICKSTART.md` - Features overview
- `README.md` - Complete documentation

