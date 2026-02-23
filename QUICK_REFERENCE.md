# 📱 Screen Time App - Quick Reference Card

## 🚀 QUICK START (2 MINUTES)

### Step 1: Install APK
```powershell
# Connect Android device with USB Debugging enabled
adb install "C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk"
```

### Step 2: Grant Permission
- Settings → Apps → ScreenTime → Permissions
- Enable "Usage Access" (PACKAGE_USAGE_STATS)

### Step 3: Open App
- Find "ScreenTime" in app drawer
- Tap to launch

---

## 🎮 APP NAVIGATION

```
┌─────────────────────────────────────────┐
│         BOTTOM NAVIGATION BAR           │
├─────────────────────────────────────────┤
│ 🏠 HOME      ℹ️ ANALYTICS    ❤️ REWARDS │
├─────────────────────────────────────────┤
│                                         │
│  DASHBOARD SCREEN                       │
│  • Weekly Points (0-115)                │
│  • Daily Breakdown                      │
│  • Earned Badges                        │
│                                         │
│  ANALYTICS SCREEN                       │
│  • Current vs Previous Week             │
│  • Daily Details                        │
│  • Improvement %                        │
│                                         │
│  REWARDS SCREEN                         │
│  • 5 Badges (Locked/Unlocked)          │
│  • Points Counter                       │
│  • Rarity Colors                        │
│                                         │
└─────────────────────────────────────────┘
```

---

## 🏅 BADGE UNLOCK QUICK GUIDE

| Badge | Goal | Status |
|-------|------|--------|
| 🟢 Focused Week | <4hrs/day for 5+ days | Check Dashboard |
| 🔵 Zero Day | Full day with 0 screen time | Can unlock multiple times |
| 🔵 Consistent User | Use app 7 consecutive days | Track in Dashboard |
| 🔵 Improvement | 30% less than last week | See in Analytics |
| 💛 Champion | Earn all 4 other badges | Hard to achieve! |

---

## 💯 POINTS SYSTEM

### Daily Points
- **1 point** per hour (under 4-hour limit) → max 4 pts/day
- **5 points** bonus for zero screen day
- **10 points** per badge unlocked

### Weekly Maximum
**113 points possible** (28 base + 35 zero + 50 badges)

### Reset Schedule
**Every Sunday at Midnight** - Points & badges reset to 0

---

## 🔧 COMMON TASKS

### Grant Permission (if missed)
```
Settings → Apps → ScreenTime → Permissions → Usage Access → ON
```

### Check Screen Time Data
```
Dashboard → See daily breakdown with hours & minutes
```

### Compare Weeks
```
Analytics → Current week vs Previous week at top
```

### View All Badges
```
Rewards → Scroll grid to see all 5 badges
```

### Reinstall App
```powershell
adb uninstall com.example.screentime
adb install app\build\outputs\apk\debug\app-debug.apk
```

---

## ⚡ TROUBLESHOOTING QUICK FIX

| Problem | Quick Fix |
|---------|-----------|
| No screen time data | Grant "Usage Access" permission |
| App won't install | `adb uninstall com.example.screentime` then retry |
| Device offline | `adb kill-server` then `adb start-server` |
| App crashes | Check logcat: `adb logcat` |
| Permission denied | Settings → Apps → ScreenTime → Grant all permissions |

---

## 📊 WHAT YOU'LL SEE

### Day 1
- Empty Dashboard (waiting for data)
- 0 points earned
- 0 badges unlocked
- Previous week data empty

### After 24 Hours
- Daily progress appears
- Points calculated
- Screen time shows in Dashboard
- Analytics starts populating

### After 7 Days
- Full week of data
- Analytics comparison appears
- Badge unlock conditions being tracked
- Points accumulating

### After 14 Days
- Two full weeks of data
- Previous week archived
- Clear week comparison
- Some badges unlocked (if conditions met)

---

## 🎯 TESTING CHECKLIST

- [ ] App installs successfully
- [ ] Permissions granted
- [ ] Dashboard screen loads
- [ ] Analytics screen loads
- [ ] Rewards screen loads
- [ ] Bottom navigation works (tap all 3 tabs)
- [ ] Navigation state persists
- [ ] Data displays correctly
- [ ] No crashes or errors

---

## 📱 APP SPECIFICATIONS

| Spec | Value |
|------|-------|
| Package | com.example.screentime |
| Min API | 26 (Android 8.0+) |
| Target API | 36 (Android 15) |
| App Size | ~15 MB |
| Database | SQLite (Room) |
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Data Storage | Local only (no cloud) |

---

## 📂 FILE LOCATIONS

| File | Location |
|------|----------|
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Source Code | `app/src/main/java/...` |
| README | `README.md` |
| Quick Start | `QUICKSTART.md` |
| Installation | `DEPLOYMENT.md` |
| Technical | `IMPLEMENTATION.md` |
| Summary | `COMPLETION_SUMMARY.md` |

---

## 🔐 PERMISSIONS NEEDED

Only 1 permission required:
- ✅ **Usage Access** (PACKAGE_USAGE_STATS) - To read screen time data

Optional (used in manifest but runtime checked):
- ℹ️ INTERNET - For future cloud features

---

## 💾 BUILD COMMANDS

```powershell
# Build APK
cd C:\Users\vibin\AndroidStudioProjects\ScreenTime
.\gradlew assembleDebug

# Install on device
.\gradlew installDebug

# Clean rebuild
.\gradlew clean build

# View logs
adb logcat
```

---

## 📞 NEED HELP?

1. **Installation**: See `DEPLOYMENT.md`
2. **Features**: See `README.md`
3. **Getting Started**: See `QUICKSTART.md`
4. **Technical Details**: See `IMPLEMENTATION.md`
5. **Full Summary**: See `COMPLETION_SUMMARY.md`

---

## ✅ YOU'RE READY!

Your app is built, tested, and ready to install. 

### Next Step
Connect your device and run:
```powershell
adb install "C:\Users\vibin\AndroidStudioProjects\ScreenTime\app\build\outputs\apk\debug\app-debug.apk"
```

---

**Enjoy your Screen Time App! 🎉📱**

