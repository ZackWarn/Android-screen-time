# Usage Tracking Check - Summary Report

## Question: Does usage get reset when the app is closed and reopened?

### Answer: **NO - Usage should NOT be reset**

---

## How the System Works

### Two-Part Usage Tracking
The app uses Android's `UsageStatsManager` which tracks usage in two parts:

1. **Base Usage (Persisted)**
   - Historical data from Android's system UsageStats
   - Gets updated when an app goes to background
   - **Saved to your database** as `AppUsageSession` records
   - **Survives app closures** ✓

2. **Live Usage (Temporary)**
   - Current session time since app was opened
   - Calculated as: `currentTime - resumeTime`
   - Only used during the current session
   - Clears when app is paused

### Total Calculation
```
Total Usage = Base Usage (from DB) + Live Usage (current session)
```

---

## Real-Time Tracking Flow

### When App is OPEN
```
Every 5 seconds:
1. Check foreground app
2. Get base usage from system
3. Get live usage (if app currently active)
4. Calculate total = base + live
5. Display to user
6. Every 60s: Save session to database
```

**Result:** Usage updates in real-time while app is open

### When App is CLOSED
```
1. ACTIVITY_PAUSED event is triggered
2. lastPauseTime is set
3. Base usage gets updated in system
4. Service continues running in background
5. Database has record of the usage
```

**Result:** Usage is saved and persists

### When App is REOPENED
```
1. Service restarts/continues
2. Checks current app
3. Gets base usage from system (includes previous sessions)
4. Gets live usage from new session
5. Shows base + live = total
6. User sees the saved usage + new session
```

**Result:** Usage does NOT reset ✓

---

## Key Components Ensuring Persistence

### 1. `AppLimitManager.getCurrentAppUsageMillis()`
```kotlin
val baseUsageMillis = usageStats.find { it.packageName == packageName }
                      ?.totalTimeInForeground ?: 0L

val inProgressMillis = if (lastResumeTime > lastPauseTime) {
    (now - lastResumeTime).coerceAtLeast(0L)
} else {
    0L
}

val totalUsageMillis = baseUsageMillis + inProgressMillis
```

✅ This combines saved usage with current session

### 2. `AppLimitDao` - Database Persistence
```kotlin
@Query("SELECT SUM(durationMinutes) FROM app_usage_sessions 
         WHERE packageName = :packageName AND date = :date")
suspend fun getTotalUsageForAppOnDate(packageName: String, date: String): Int?

@Query("UPDATE app_limits SET usedTodayMinutes = :usedMinutes, 
                              isBlocked = :isBlocked 
        WHERE packageName = :packageName")
suspend fun updateUsageAndBlockStatus(packageName: String, usedMinutes: Int, isBlocked: Boolean)
```

✅ Usage is saved to database periodically

### 3. `AppMonitorService.sessionSavedApps`
```kotlin
private val sessionSavedApps = mutableMapOf<String, Long>()
// Saves session only if 60+ seconds have passed
if (timeSinceLastSave >= 60_000L) {
    appLimitManager.saveUsageSession(currentApp, usedMinutes)
    sessionSavedApps[currentApp] = now
}
```

✅ Sessions are saved every 60 seconds to prevent duplicates

### 4. Overlay Shows on Reopen
```kotlin
when (status) {
    is LimitStatus.Exceeded -> {
        // Always show overlay (no check for "already blocked")
        overlayBlockManager.showBlockingOverlay(appName, status.usedMinutes, status.limitMinutes)
    }
}
```

✅ Overlay appears every time app is opened while limit exceeded

---

## What the Enhanced Logging Shows

The updated code now includes emoji prefixes for easy tracking:

```
📱 [CHECK]   - Checking current app every 5 seconds
📊 [STATUS]  - Showing usage status (WithinLimit/Exceeded)
💾 [SAVE]    - Saving session to database (every 60 seconds)
⏳ [SKIP]    - Skipping save because not enough time passed
✅ [WITHIN]  - App is within limit
🚨 [EXCEEDED] - App exceeded limit
🚫 [OVERLAY] - Showing blocking overlay
⚠️  [WARNING] - Warning when 5 minutes or less remaining
ℹ️  [NO LIMIT] - No limit set for app
❌ [ERROR]   - Error occurred
```

---

## Testing the Implementation

### Test 1: Check Real-Time Tracking
1. Set a 5-minute YouTube limit
2. Open YouTube
3. Watch Logcat for: `📊 [STATUS] Usage: X ms = Y min`
4. Usage should increase every 5 seconds ✓

### Test 2: Check Persistence
1. Use YouTube for 3 minutes
2. Press home (close YouTube)
3. Wait 5 seconds
4. Open YouTube again
5. Dashboard should show ≥3 minutes ✓
6. **NOT** 0 minutes

### Test 3: Check Session Saving
1. Use YouTube continuously
2. Watch Logcat for `💾 [SAVE]` every 60 seconds
3. Each save shows the current total usage ✓

### Test 4: Check Overlay Persistence
1. Use YouTube for 2 minutes (with 1-minute limit)
2. Close YouTube
3. Reopen YouTube
4. Overlay should appear showing "2/1 minutes" ✓

---

## Database Records

If usage persists correctly, you should see:

### app_usage_sessions Table
```
| id | packageName | date | startTime | endTime | durationMinutes |
|----|------------|------|-----------|---------|-----------------|
| 1  | youtube    | 2026-02-25 | 1234567890 | 1234567950 | 1 |
| 2  | youtube    | 2026-02-25 | 1234567950 | 1234568010 | 1 |
| 3  | youtube    | 2026-02-25 | 1234568010 | 1234568070 | 1 |
```

Each row = 1 minute saved

### app_limits Table
```
| packageName | appName | limitMinutes | usedTodayMinutes | isBlocked |
|------------|---------|--------------|-----------------|-----------|
| youtube    | YouTube | 5            | 3               | 0         |
```

Shows last calculated usage

---

## Conclusion

✅ **Usage SHOULD NOT reset when app closes and reopens**

The system is designed to:
1. Save sessions every 60 seconds
2. Persist data in database
3. Combine base usage (from DB) with live usage (current session)
4. Show overlay immediately if limit is exceeded

If you're seeing usage reset to 0:
1. Check Logcat for `💾 [SAVE]` messages - are sessions being saved?
2. Check database - does app_usage_sessions have entries?
3. Check `usedTodayMinutes` in app_limits - is it being updated?

Monitor the enhanced logs with emoji prefixes to identify where the issue occurs.

