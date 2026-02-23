# 🔧 FIX ADB PATH ISSUE - Complete Guide

**Problem**: `adb: The term 'adb' is not recognized`

**Cause**: Android SDK platform-tools is not in your system PATH

**Solution**: Follow one of these methods below

---

## ⚡ METHOD 1: AUTOMATIC INSTALLATION SCRIPT (EASIEST)

I've created a PowerShell script that will automatically:
1. Find your Android SDK
2. Add it to PATH
3. Install the app
4. Launch it on your device

### Steps:
1. **Right-click** on your desktop or in File Explorer
2. **Select** "Open PowerShell window here" (or Terminal)
3. **Run this command**:
   ```powershell
   C:\Users\vibin\AndroidStudioProjects\ScreenTime\install_app.ps1
   ```

4. **Follow the on-screen prompts**

The script will handle everything automatically! ✅

---

## 🔨 METHOD 2: MANUAL ADB PATH SETUP

If the script doesn't work, manually add ADB to your PATH:

### Step 1: Find Your Android SDK Location
1. **Open Android Studio**
2. **Go to**: File → Settings (or Preferences on Mac)
3. **Find**: Android SDK
4. **Look for**: "Android SDK Location" (example: `C:\Users\username\AppData\Local\Android\Sdk`)
5. **Copy this path** - you'll need it

### Step 2: Add Platform-Tools to PATH
1. **Press**: Windows Key + X
2. **Select**: "System" or open Settings
3. **Go to**: Advanced system settings
4. **Click**: Environment Variables
5. **Under "System variables"**, find and click: "Path"
6. **Click**: Edit
7. **Click**: New
8. **Paste**: `[Your SDK Path]\platform-tools`
   - Replace `[Your SDK Path]` with your actual path from Step 1
   - Example: `C:\Users\vibin\AppData\Local\Android\Sdk\platform-tools`
9. **Click**: OK three times
10. **Close** and reopen PowerShell/Terminal

### Step 3: Verify It Works
```powershell
adb devices
```

Should show your device!

---

## 📱 METHOD 3: USE GRADLE (NO ADB NEEDED)

If ADB setup is too complex, use Gradle instead:

```powershell
cd C:\Users\vibin\AndroidStudioProjects\ScreenTime
.\gradlew installDebug
```

This builds AND installs the app automatically!

---

## ✅ AFTER SETUP

Once ADB is working or Gradle installation succeeds:

1. **On your device**, you should see a notification
2. **Open** Settings → Apps → ScreenTime
3. **Grant** "Usage Access" permission
4. **Restart** the app
5. **Explore** Dashboard, Analytics, Rewards screens

---

## 🆘 STILL NOT WORKING?

### Problem: Can't find Android SDK location
**Solution**:
1. Search Windows for "Android Studio"
2. Open it
3. Look in: File → Settings → Android SDK
4. It should show the SDK location right there

### Problem: PowerShell script won't run
**Solution**:
1. **Right-click** PowerShell
2. **Select** "Run as Administrator"
3. **Run**:
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```
4. **Type** "Y" and press Enter
5. **Then run** the install script again

### Problem: Device not showing in `adb devices`
**Solution**:
1. **Unplug** USB cable
2. **Wait** 5 seconds
3. **Plug in** again
4. **On phone**, tap "Allow" if prompted
5. **Run** `adb devices` again

---

## 📄 QUICK REFERENCE

**Location of installation files**:
```
C:\Users\vibin\AndroidStudioProjects\ScreenTime\
├─ app\build\outputs\apk\debug\app-debug.apk  (The app)
├─ install_app.ps1                             (Auto script)
└─ install_app.bat                             (Batch script)
```

**Quick commands**:
```powershell
# Check devices
adb devices

# Install app
adb install "C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk"

# Or use Gradle
.\gradlew installDebug
```

---

## 🎯 NEXT STEP

**Choose your method and follow the steps above!**

If you use the PowerShell script, it handles everything automatically. That's the easiest option! 👍

---

**Questions?** Read the troubleshooting section or consult DEVICE_INSTALLATION_GUIDE.md

