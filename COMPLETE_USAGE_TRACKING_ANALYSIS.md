# Complete Usage Tracking Analysis & Verification Guide

## Your Question: "Does usage get reset when the app is closed and reopened?"

### ✅ Answer: NO - Usage should persist and NOT reset to 0

---

## System Architecture

### The Two-Layer Usage System

Your app implements a **dual-layer usage tracking system** that ensures data persistence:

```
Layer 1: Real-Time Tracking (Live Session)
├── Current time since app opened
├── Only active while app is in foreground
└── Clears when app is paused

         COMBINED

Layer 2: Persistent Storage (Database)
├── Historical sessions saved every 60 seconds
├── Survives app closures
└── Retrieved from app_usage_sessions table
```

---

## Deep Dive: How It Works

### Step 1: When User Opens an App
```
User opens YouTube
        ↓
AppMonitorService.checkCurrentApp() runs
        ↓
Gets package name: "com.google.android.youtube"
        ↓
Calls AppLimitManager.checkAppUsage(packageName)
```

### Step 2: Getting Real-Time Usage
```
getCurrentAppUsageMillis() is called
        ↓
Part A: Get Base Usage (from system UsageStats)
├── Query Android's UsageStats database
├── Find YouTube's totalTimeInForeground
├── Result: e.g., 3,139,712 ms (52 minutes)

        PLUS

Part B: Get Live Usage (current session)
├── Query UsageEvents for ACTIVITY_RESUMED
├── Query UsageEvents for ACTIVITY_PAUSED
├── If lastResumeTime > lastPauseTime (app is active)
│   └── Calculate: now - lastResumeTime
│   └── Result: e.g., 107,274 ms (1 minute 47 seconds)
├── Else (app is paused)
│   └── Return: 0 ms

        EQUALS

Total Usage = 3,139,712 + 107,274 = 3,246,986 ms = 54 minutes
```

### Step 3: Checking Against Limit
```
Current usage: 54 minutes
Limit: 52 minutes

Status = EXCEEDED (because 54 > 52)
        ↓
Save to database (if 60+ seconds since last save)
        ↓
Show overlay to user
```

### Step 4: Closing the App
```
User presses home/back
        ↓
Android sends ACTIVITY_PAUSED event
        ↓
lastPauseTime is updated
        ↓
Base usage updated in system UsageStats
        ↓
Database still has saved sessions (54 minutes recorded)
```

### Step 5: Reopening the App
```
User opens YouTube again
        ↓
Service checks app again
        ↓
Gets base usage (54 minutes - from system)
        ↓
Gets live usage (0 initially - new session just started)
        ↓
Total = 54 + 0 = 54 minutes ✓
        ↓
Dashboard shows 54 minutes (NOT reset to 0)
```

---

## The Code That Ensures Persistence

### 1. Real-Time Calculation in AppLimitManager
```kotlin
// Line 182-183
val appUsage = usageStats.find { it.packageName == packageName }
val baseUsageMillis = appUsage?.totalTimeInForeground ?: 0L
// ↑ This retrieves saved base usage from system

// Line 204-208
val inProgressMillis = if (lastResumeTime > lastPauseTime) {
    (now - lastResumeTime).coerceAtLeast(0L)
} else {
    0L
}
// ↑ This calculates current session time

// Line 210
val totalUsageMillis = baseUsageMillis + inProgressMillis
// ↑ THIS IS THE KEY: Always combines base + live
```

### 2. Database Persistence in AppLimitManager
```kotlin
// Line 273
suspend fun saveUsageSession(packageName: String, totalUsedMinutesNow: Int) {
    val session = AppUsageSession(
        packageName = packageName,
        date = today,
        startTime = now - 60000,  // ~1 minute ago
        endTime = now,
        durationMinutes = 1  // Always 1 minute increments
    )
    database.appUsageSessionDao().insertSession(session)
}
// ↑ This saves every minute to ensure data persists
```

### 3. Periodic Saving in AppMonitorService
```kotlin
// Line 139-148
if (timeSinceLastSave >= 60_000L) {
    android.util.Log.d("AppMonitorService", "💾 [SAVE] Saving session...")
    appLimitManager.saveUsageSession(currentApp, usedMinutes)
    sessionSavedApps[currentApp] = now
}
// ↑ Saves every 60 seconds to prevent duplicates
```

### 4. Overlay Shows on Reopen
```kotlin
// Line 169-182 (Updated with new code)
when (status) {
    is LimitStatus.Exceeded -> {
        // Always show overlay, no check for "already blocked"
        overlayBlockManager.showBlockingOverlay(appName, 
                                               status.usedMinutes, 
                                               status.limitMinutes)
    }
}
// ↑ Shows overlay EVERY TIME if limit exceeded, even after reopening
```

---

## What Each Log Message Means

### Checking for Usage
```
📱 [CHECK] Checking app: com.google.android.youtube
```
Service checks every 5 seconds ✓

### Status Update
```
📊 [STATUS] Status for com.google.android.youtube: WithinLimit
```
Status can be: WithinLimit | Exceeded | NoLimit

### Real Usage Details
```
Usage for com.google.android.youtube: 3246986 ms = 54 min 
(base: 3139712 ms, live: 107274 ms)
```
- **base: 3139712 ms** = Saved from previous sessions = 52 min
- **live: 107274 ms** = Current session active time = 1 min 47 sec
- **total: 3246986 ms** = Combined = 54 min

### Saving Session
```
💾 [SAVE] Saving session: com.google.android.youtube = 54 min 
         (time since last save: 60s)
✅ Saved 1-min session: com.google.android.youtube 
                       (total: 54 min) on 2026-02-25
```
Every 60 seconds, 1 minute of usage is saved ✓

### Warning
```
⚠️  [WARNING] Only 2 min remaining for com.google.android.youtube
```
Shown when ≤5 minutes left

### Limit Exceeded
```
🚨 [EXCEEDED] LIMIT EXCEEDED for com.google.android.youtube: 54/52 min
🚫 [OVERLAY] Showing overlay for com.google.android.youtube
```
Overlay appears every time app is opened while exceeded ✓

---

## Verification Checklist

### ✅ Does the system persist usage?
- [x] Base usage comes from Android's UsageStats (system database)
- [x] Sessions are saved every 60 seconds
- [x] Database has `app_usage_sessions` table for persistence
- [x] On reopen, combines base usage + new live usage

### ✅ Does usage show correctly?
- [x] Real-time updates every 5 seconds (via logcat)
- [x] Persists after closing app
- [x] Doesn't reset to 0 on reopening
- [x] Overlay shows every time if exceeded

### ✅ What happens each scenario?
| Scenario | Expected | Implementation |
|----------|----------|-----------------|
| Use app 3 min | Dashboard shows 3 min | Real-time calc: base + live |
| Close app | Usage saved | Session saved every 60s |
| Reopen app | Shows ≥3 min | Combines saved base + new live |
| Exceed limit | Overlay on reopen | Always show if Exceeded status |
| New day | Usage resets | Daily reset at midnight |

---

## If Usage IS Resetting (Troubleshooting)

Check these in order:

### 1. Are sessions being saved?
Look for `💾 [SAVE]` in logcat
- **Appears** → Sessions ARE being saved ✓
- **Doesn't appear** → Check timer logic (60 second delay)

### 2. Is base usage retrieving correctly?
Look for usage log with "(base: X ms"
- **Shows base > 0** → Base is being retrieved ✓
- **Shows base = 0** → System might not have usage data

### 3. Is database saving correctly?
Check the database using Android Studio Device Explorer:
1. View → Tool Windows → Device Explorer
2. Navigate to: `/data/data/com.example.screentime/databases/`
3. Download `screentime_db.db`
4. Open with SQLite viewer
5. Check `app_usage_sessions` table
   - **Has rows** → Database is working ✓
   - **Empty** → Sessions not reaching database

### 4. Is overlay showing?
Look for `🚫 [OVERLAY]` in logcat
- **Appears** → Overlay is working ✓
- **Doesn't appear** → Check if status is actually Exceeded

---

## Expected Behavior Timeline

### Scenario: Use YouTube for 3 minutes

```
00:00 (Start)
├─ 📱 [CHECK] Checking app: com.google.android.youtube
├─ 📊 [STATUS] Status: WithinLimit
├─ Usage: 0 ms = 0 min (base: 0, live: 0)
└─ ✅ [WITHIN] Within limit: 0/10 min

00:05 (After 5 seconds)
├─ 📱 [CHECK] Checking app: com.google.android.youtube
├─ 📊 [STATUS] Status: WithinLimit
├─ Usage: 5000 ms = 0 min (base: 0, live: 5000)
├─ ⏳ [SKIP] Not saving yet: 55s until save
└─ ✅ [WITHIN] Within limit: 0/10 min

01:00 (After 60 seconds)
├─ 💾 [SAVE] Saving session: youtube = 1 min
├─ ✅ Saved 1-min session: youtube (total: 1 min)
└─ ⏳ [SKIP] Not saving yet: 59s until next save

02:00 (After 120 seconds total, 60 seconds since last save)
├─ 💾 [SAVE] Saving session: youtube = 2 min
├─ ✅ Saved 1-min session: youtube (total: 2 min)
└─ ⏳ [SKIP] Not saving yet: 59s until next save

03:00 (After 180 seconds total)
├─ 💾 [SAVE] Saving session: youtube = 3 min
├─ ✅ Saved 1-min session: youtube (total: 3 min)
└─ Usage: 180000 ms = 3 min (base: 0, live: 180000)

03:05 (User presses home - CLOSES APP)
└─ [PAUSED] lastPauseTime = now

03:10 (5 seconds after closing)
└─ [Background] Service still running

03:15 (User reopens YouTube)
├─ 📱 [CHECK] Checking app: com.google.android.youtube
├─ 📊 [STATUS] Status: WithinLimit
├─ Usage: 180000 ms = 3 min (base: 180000, live: 0)
│         ↑ Base usage from saved sessions
│         ↑ Live usage from new session (just started)
└─ ✅ [WITHIN] Within limit: 3/10 min
   ✓✓✓ Usage PERSISTED - NOT reset to 0
```

---

## Summary

### How Usage Persists:
1. **Base Usage** - Saved to database every 60 seconds
2. **Live Usage** - Calculated from current session
3. **Total** = Base + Live (always combines both)
4. **On Reopen** - Retrieves base from DB + new live = same total

### Why Usage Doesn't Reset:
- Base usage is in the Android system database (UsageStats)
- We also save it to our app database (app_usage_sessions)
- On reopening, base is retrieved from system (not cleared)
- Total = saved base + current session

### How to Verify:
- Watch Logcat for `💾 [SAVE]` messages (sessions being saved)
- Check database for entries in `app_usage_sessions`
- Verify `usedTodayMinutes` in `app_limits` table
- Use app → Close → Reopen → Check if usage persists

**If usage resets to 0 on reopen, check the Logcat logs to find where the process is failing.**

