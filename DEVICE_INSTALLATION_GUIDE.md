# 📱 DEVICE CONNECTION & INSTALLATION GUIDE

**Status**: APK Ready ✅  
**Location**: `C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk`  
**File Size**: ~15 MB  
**Current Device Status**: Not connected (but that's OK - follow steps below)

---

## 🔌 STEP 1: CONNECT YOUR ANDROID DEVICE

### Physical Connection
1. **Plug your Android phone** into your computer using a USB cable
2. **Wait 2-3 seconds** for the connection to establish
3. **On your phone**, you may see a notification asking about "USB debugging"
4. **Tap "Allow"** if prompted

### Enable Developer Mode (if not already enabled)
1. Go to **Settings** → **About Phone**
2. Find **"Build Number"** (may be labeled differently)
3. **Tap Build Number 7 times** until you see "Developer Mode enabled"
4. Go back to **Settings** → **Developer Options**
5. Toggle **"USB Debugging"** to **ON**
6. Tap **"Allow"** when prompted about USB debugging access

---

## ✅ STEP 2: VERIFY CONNECTION

Once your device is connected, run this command to verify:

```powershell
adb devices
```

**Expected Output:**
```
List of attached devices
ZD222DNYXW            device
```

If you see your device listed as "device" (not "offline"), you're connected! ✅

### If Device Shows "offline" or Not Listed:
```powershell
# Try reconnecting:
adb kill-server
Start-Sleep -Seconds 2
adb start-server
Start-Sleep -Seconds 2
adb devices
```

---

## 🚀 STEP 3: INSTALL THE APP

Once your device is connected, run this command:

```powershell
adb install "C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk"
```

**Expected Output:**
```
Success
```

If you see "Success", the app is installed! ✅

---

## 🎮 STEP 4: LAUNCH THE APP

### Option A: Via Command Line
```powershell
adb shell am start -n com.example.screentime/.MainActivity
```

### Option B: Manual on Phone
1. Open your **App Drawer** (swipe up or tap app icon)
2. Find and tap **"ScreenTime"**
3. The app will launch

---

## 🔐 STEP 5: GRANT PERMISSION

**IMPORTANT:** The app needs "Usage Access" permission to track screen time.

### When App First Opens:
1. You may see a permission prompt
2. If not shown, manually grant it:
   - Settings → **Apps** → **ScreenTime**
   - Tap **Permissions**
   - Find **"Usage Access"** (may be under "Special App Access")
   - Toggle it **ON**

3. **Close and reopen** the app after granting permission

Without this permission, the app won't capture screen time data.

---

## ✨ STEP 6: EXPLORE THE APP

Once opened, you'll see three tabs at the bottom:

### 🏠 Dashboard (Home)
- Weekly progress bar (0-115 points)
- Daily breakdown with screen time
- Earned badges

### 📊 Analytics
- Current vs. previous week comparison
- Daily usage charts
- Improvement percentage

### ⭐ Rewards
- All 5 achievement badges
- Points counter
- Badge descriptions

---

## 🛠️ TROUBLESHOOTING

### Issue: "No connected devices"
**Solution:**
1. Disconnect USB cable
2. Wait 5 seconds
3. Reconnect USB cable
4. Check that **USB Debugging** is enabled
5. On phone, tap **Allow** if prompted
6. Try `adb devices` again

### Issue: "Device is offline"
**Solution:**
```powershell
adb kill-server
adb start-server
adb devices
```

### Issue: "App not installed"
**Solution:**
```powershell
# First uninstall any previous version
adb uninstall com.example.screentime

# Then install fresh
adb install "C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk"
```

### Issue: "Command not found 'adb'"
**Solution:** ADB is not in your system PATH
1. Open PowerShell as Administrator
2. Add ADB to PATH:
   ```powershell
   $env:Path += ";C:\Users\vibin\AppData\Local\Android\Sdk\platform-tools"
   ```
3. Try the install command again

### Issue: Permission popup doesn't appear
**Solution:** Grant manually in Settings:
1. Settings → **Apps** → **ScreenTime**
2. → **Permissions** (or **Special App Access**)
3. Find **"Usage Access"** and toggle **ON**
4. Restart the app

### Issue: App crashes on launch
**Solution:**
1. Grant "Usage Access" permission (see Step 5)
2. Check that Android 8.0+ (API 26+)
3. Try reinstalling: `adb uninstall com.example.screentime` then install again

---

## 📊 EXPECTED BEHAVIOR

### First Launch
- Empty Dashboard (no data yet)
- 0 points earned
- 0 badges unlocked
- Permission request for "Usage Access"

### After 24 Hours
- First day of screen time data appears
- Points calculated
- Dashboard shows usage

### After 7 Days
- Full week of data visible
- Analytics starts working
- Badge conditions being tracked

### After 14 Days
- Week comparison visible
- Previous week data archived
- Clear progress tracking

---

## 💡 TIPS

1. **Keep USB Debugging Enabled** while using/testing the app
2. **Grant All Permissions** when prompted
3. **Use Device Normally** - The more you use it, the more realistic the data
4. **Wait 24+ Hours** before expecting detailed analytics
5. **Check Permissions** if no data appears after a day

---

## 📱 QUICK COMMAND REFERENCE

| Action | Command |
|--------|---------|
| Check devices | `adb devices` |
| Install app | `adb install "path/to/app-debug.apk"` |
| Uninstall app | `adb uninstall com.example.screentime` |
| Launch app | `adb shell am start -n com.example.screentime/.MainActivity` |
| View logs | `adb logcat` |
| Clear logs | `adb logcat -c` |
| Restart ADB | `adb kill-server` then `adb start-server` |

---

## ✅ CHECKLIST

- [ ] USB cable connected
- [ ] USB Debugging enabled on phone
- [ ] Device shows in `adb devices`
- [ ] APK installed successfully
- [ ] "Usage Access" permission granted
- [ ] App launches without crashing
- [ ] Can navigate between 3 tabs
- [ ] App is ready to use

---

## 🎯 NEXT STEPS

1. ✅ Connect your Android device
2. ✅ Install the APK using the command above
3. ✅ Grant "Usage Access" permission
4. ✅ Open the app and explore
5. ✅ Use your device normally for data collection
6. ✅ Check back tomorrow to see screen time data

---

**Once installed, your Screen Time App is fully functional and ready to use! 🎉**

