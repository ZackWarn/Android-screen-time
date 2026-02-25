# Usage Tracking Test Plan

## Overview
This document explains how the app tracks usage in real-time and what happens when the app is closed and reopened.

## How Real-Time Usage Tracking Works

### Current Implementation
The app uses Android's `UsageStatsManager` API which provides two pieces of information:

1. **Base Usage** (`totalTimeInForeground` from UsageStats)
   - Historical usage data from the system
   - Updated when the app is paused/closed
   - This is the "committed" usage that persists in the database

2. **Live/In-Progress Session** (from UsageEvents)
   - Tracks ACTIVITY_RESUMED and ACTIVITY_PAUSED events
   - When app is currently open (lastResumeTime > lastPauseTime)
   - Calculated as: `currentTime - lastResumeTime`
   - This is the "active" usage while the app is open

### Total Usage Calculation
```
totalUsageMillis = baseUsageMillis + inProgressMillis
```

Where:
- **baseUsageMillis** = Saved usage from previous sessions
- **inProgressMillis** = Time since app was last resumed (if app is currently active)

## Test Scenarios

### Scenario 1: Real-Time Tracking While App is Open
1. Set a limit for YouTube to 5 minutes
2. Open YouTube
3. Dashboard shows usage increasing in real-time
4. Expected: Usage should increment smoothly (every 30 seconds via dashboard refresh)
5. Log Entry: `"Usage for com.google.android.youtube: X ms = Y min (base: Z ms, live: W ms)"`

### Scenario 2: Usage Persists When App is Closed and Reopened
1. Use YouTube for 3 minutes (don't close app yet)
2. Dashboard should show ~3 minutes
3. Close the app (go to home screen)
4. Wait 5 seconds
5. Reopen the app (check dashboard)
6. Expected: Usage should still show ~3 minutes OR slightly more (if new session counted)
7. NOT reset to 0 minutes

### Scenario 3: Overlay Shows When Limit is Exceeded
1. Set YouTube limit to 1 minute
2. Use YouTube for 2 minutes
3. Close YouTube (trigger pause event)
4. Reopen YouTube
5. Expected: Overlay should appear immediately showing "2/1 minutes"
6. App should send to home screen after 1.5 seconds

### Scenario 4: Session Saving (Persists to Database)
1. Use YouTube for 2 minutes continuously
2. Every minute, a session is saved to the database
3. Log Entry: `"✅ Saved 1-min session: com.google.android.youtube (total: 2 min) on 2026-02-25"`
4. Close app and let it sit for 30 seconds
5. Reopen app
6. Expected: The 2 minutes of usage is already in the database, so it persists
7. Dashboard should show ≥2 minutes (may be slightly more if new session started)

## Key Log Entries to Watch

### Real-Time Usage Check
```
AppLimitManager: Getting usage from 1771957800000 to 1772042694505
AppLimitManager: Total usage stats entries: 271
AppLimitManager: Usage for com.google.android.youtube: 3246986 ms = 54 min (base: 3139712 ms, live: 107274 ms)
```

**Interpretation:**
- base = 52 minutes (from previous sessions)
- live = 1 minute 47 seconds (current session)
- total = 54 minutes

### Session Saving (Once per minute)
```
AppMonitorService: 💾 Saving session: com.google.android.youtube = 54 min
AppLimitManager: ✅ Saved 1-min session: com.google.android.youtube (total: 54 min) on 2026-02-25
```

### Limit Exceeded
```
AppLimitManager: BLOCKED: com.google.android.youtube exceeded limit
AppMonitorService: LIMIT EXCEEDED for com.google.android.youtube: 54/52 min
AppMonitorService: 🚫 Showing overlay for com.google.android.youtube
```

## What Should NOT Happen (Bugs to Avoid)

❌ **Usage should NOT reset when app is closed** - If usage resets to 0, the base usage isn't being saved
❌ **Overlay should NOT fail to appear** - If you close YouTube at 54 min limit (52 limit) and reopen, overlay must show
❌ **Usage should NOT jump significantly** - If it jumps from 52 to 60 when reopening, sessions aren't being saved properly

## Verification Steps

1. **Check Database Persistence:**
   - Open Android Studio → Device File Explorer
   - Navigate to `/data/data/com.example.screentime/databases/screentime_db.db`
   - Export and open with SQLite viewer
   - Check `app_limits` table for `usedTodayMinutes` values
   - Check `app_usage_sessions` table for session records

2. **Check Logcat:**
   - In Android Studio, open Logcat
   - Filter by "AppLimitManager" and "AppMonitorService"
   - Watch logs as you use apps and close/reopen them

3. **Manual Testing:**
   - Set a low limit (5 minutes) for an app
   - Use the app continuously
   - Check dashboard for real-time updates
   - Close and reopen the app
   - Verify usage doesn't reset

## Expected Behavior Summary

| Action | Expected Result |
|--------|-----------------|
| Use app for 3 min | Usage shows 3 min in dashboard |
| Close app | Base usage saved (3 min committed) |
| Reopen app immediately | Usage shows ≥3 min |
| Use app for 1 more min, then close | Usage shows ~4 min |
| Wait 24 hours, then open | Usage shows 0 min (reset for new day) |
| Exceed limit while app open | Overlay shows after ~5 seconds |
| Exceed limit, close, reopen | Overlay shows immediately |

