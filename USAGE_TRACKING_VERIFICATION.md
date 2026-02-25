# Usage Tracking Verification Guide

## Summary: How Usage Tracking Works

Your app now uses Android's **UsageStatsManager API** which has two components:

### 1. Base Usage (Persistent)
- Comes from the system's `UsageStats` database
- Updated when an app goes into the background (paused)
- This is **saved to your database** through `AppUsageSession` table
- **Persists across app closures and restarts**

### 2. Live Usage (In-Progress)
- Calculated from the current time minus when the app was last resumed
- Only added if the app is currently active (lastResumeTime > lastPauseTime)
- **Not** saved to database (it's temporary)
- Clears when the app is paused

### Combined Formula
```
Total Usage = Base Usage + Live Usage
```

## What Should Happen (Expected Behavior)

### Test 1: Real-Time Usage Updates
```
1. Set YouTube limit to 10 minutes
2. Open YouTube
3. Wait 2 minutes
4. Check dashboard
   ✅ Should show ~2 minutes
   
5. Leave YouTube open for 3 more minutes  
6. Check dashboard
   ✅ Should show ~5 minutes (cumulative)
   
Emoji in logs: 📊 [STATUS] Usage for com.google.android.youtube: X ms = Y min
```

### Test 2: Usage Persists When Closed and Reopened
```
1. Use YouTube for 3 minutes (don't close app yet)
2. Dashboard shows ~3 minutes
3. Close YouTube (press home button) ← Important: this triggers ACTIVITY_PAUSED event
4. Go back to dashboard
5. Reopen YouTube
6. Check dashboard
   ✅ Should show ~3 minutes OR slightly more (if new session started)
   ❌ Should NOT show 0 minutes
   
Emoji in logs: 💾 [SAVE] Saving session: X = Y min (time since last save: Zs)
```

### Test 3: Overlay Shows When Limit Exceeded (Even After Closing)
```
1. Set YouTube limit to 1 minute
2. Use YouTube for 2 minutes
3. Close YouTube (press home)
4. Reopen YouTube
   ✅ Overlay should appear immediately showing "2/1 minutes exceeded"
   ✅ After 1.5 seconds, it should auto-close and go to home
   
Emoji in logs: 🚨 [EXCEEDED] LIMIT EXCEEDED for X: 2/1 min
                🚫 [OVERLAY] Showing overlay for X
```

### Test 4: Sessions Are Saved Every Minute
```
1. Use YouTube continuously for 3 minutes
2. Check logs - you should see:
   ⏳ [SKIP] Not saving yet: 55s until next save (at 5 seconds)
   ⏳ [SKIP] Not saving yet: 50s until next save (at 10 seconds)
   ...
   💾 [SAVE] Saving session: X = Y min (at 60 seconds)
   ⏳ [SKIP] Not saving yet: 59s until next save (at 65 seconds)
   ...
   💾 [SAVE] Saving session: X = Y min (at 120 seconds)
   ...
   💾 [SAVE] Saving session: X = Y min (at 180 seconds)
```

## How to Verify in Logcat

### Filter Setup
In Android Studio Logcat, enter this filter:
```
AppMonitorService|AppLimitManager
```

### Key Log Patterns to Look For

**Usage Check (every 5 seconds):**
```
📱 [CHECK] Checking app: com.google.android.youtube
📱 [CHECK] Current app: com.google.android.youtube, Last checked: com.google.android.youtube
📊 [STATUS] Status for com.google.android.youtube: WithinLimit
```

**Base + Live Usage Calculation:**
```
Usage for com.google.android.youtube: 3246986 ms = 54 min (base: 3139712 ms, live: 107274 ms)
```

Interpretation:
- **base: 3139712 ms** = 52 min (from previous sessions)
- **live: 107274 ms** = 1 min 47 sec (current session)
- **total: 3246986 ms** = 54 min

**Session Saving (once per minute):**
```
💾 [SAVE] Saving session: com.google.android.youtube = 54 min (time since last save: 60s)
✅ Saved 1-min session: com.google.android.youtube (total: 54 min) on 2026-02-25
```

**Limit Exceeded:**
```
🚨 [EXCEEDED] LIMIT EXCEEDED for com.google.android.youtube: 54/52 min
🚫 [OVERLAY] Showing overlay for com.google.android.youtube
```

## Potential Issues to Watch For

### ❌ Issue 1: Usage Resets to 0 When App Reopens
**Symptoms:**
- Close YouTube after using for 5 minutes
- Reopen YouTube
- Dashboard shows 0 minutes

**Cause:** Base usage not being saved properly to database

**Fix:** Check that `AppUsageSession` records are being created in the database

### ❌ Issue 2: Usage Jumps Dramatically
**Symptoms:**
- Use YouTube for 5 minutes
- Dashboard shows 5 minutes
- Close and reopen
- Now shows 10 minutes

**Cause:** Session is being saved twice or system is double-counting usage

**Fix:** Check that sessions are only saved once per 60 seconds (log shows ⏳ [SKIP] messages)

### ❌ Issue 3: Overlay Doesn't Show When App Reopened
**Symptoms:**
- Use YouTube for 2 minutes (limit: 1 minute)
- Close YouTube
- Reopen YouTube
- No overlay appears

**Cause:** isBlocked status in database not being checked on service restart

**Fix:** Service should always show overlay if `status is LimitStatus.Exceeded`

## Database Verification

To verify data is being saved:

### Option 1: Android Studio Device Explorer
1. Android Studio → View → Tool Windows → Device Explorer
2. Navigate to `/data/data/com.example.screentime/databases/screentime_db.db`
3. Right-click → Save As → Download to your computer
4. Open with SQLite viewer (e.g., DB Browser for SQLite)
5. Check these tables:
   - **app_limits** → usedTodayMinutes, isBlocked columns
   - **app_usage_sessions** → durationMinutes, packageName columns

### Option 2: ADB Shell
```bash
adb shell sqlite3 /data/data/com.example.screentime/databases/screentime_db.db
SELECT * FROM app_limits;
SELECT * FROM app_usage_sessions;
```

## Step-by-Step Testing Procedure

1. **Setup:**
   - Install the app
   - Set YouTube limit to 3 minutes
   - Open Logcat filter to show AppMonitorService logs

2. **Test Real-Time Tracking:**
   - Open YouTube
   - Wait 30 seconds, check dashboard (should show ~0.5 min)
   - Wait 60 more seconds, check dashboard (should show ~1.5 min)
   - Check logs for "📊 [STATUS]" and usage calculations

3. **Test Persistence:**
   - Use YouTube for exactly 2 minutes
   - Check dashboard (should show ~2 min)
   - Press home button (closes YouTube)
   - Check logcat for "💾 [SAVE]" message
   - Go back to dashboard
   - Reopen YouTube
   - Check dashboard (should still show ~2 min)

4. **Test Overlay:**
   - Use YouTube for 4 minutes (exceeds 3 min limit)
   - Check logs for "🚨 [EXCEEDED]" and "🚫 [OVERLAY]"
   - Overlay should show "4/3 minutes"
   - Close app automatically after 1.5 seconds
   - Reopen YouTube
   - Overlay should appear again

5. **Check Database:**
   - Use Android Studio Device Explorer
   - Download and open screentime_db.db
   - Verify app_usage_sessions table has entries for the apps you tested

## Expected Logs Timeline

For a 3-minute YouTube session:

```
00s: 📱 [CHECK] Checking app: com.google.android.youtube
00s: 📊 [STATUS] Status: WithinLimit
05s: 📱 [CHECK] Checking app: com.google.android.youtube
05s: 📊 [STATUS] Status: WithinLimit (1 min)
10s: 📱 [CHECK] Checking app: com.google.android.youtube
...
55s: ⏳ [SKIP] Not saving yet: 5s until next save
60s: 💾 [SAVE] Saving session: Y = 1 min (time since last save: 60s)
...
120s: 💾 [SAVE] Saving session: Y = 2 min
...
180s: 💾 [SAVE] Saving session: Y = 3 min
(Close app)
1 min later: 📁 [CLOSED] YouTube session ended, base usage = 3 min
(Reopen app)
01s: 📱 [CHECK] Checking app: com.google.android.youtube
01s: 📊 [STATUS] Status: WithinLimit (3 min)
```

## Summary

✅ **Your app is designed to:**
- Track real-time usage using Android's UsageStatsManager
- Save usage sessions to database every 60 seconds
- Persist usage across app closures
- Show overlay when limit is exceeded, even after closing
- Reset usage at midnight (new day)

🔍 **To verify it works:**
- Watch the logcat logs with emoji prefixes
- Check the database for session records
- Test closing and reopening apps
- Verify overlay appears on limit exceeded

If you see usage resetting, check the logcat logs to see where in the process it's failing.

