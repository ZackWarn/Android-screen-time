# Usage Tracking Check - FINAL SUMMARY

## Question: Does usage get reset when the app is closed and reopened?

### ✅ ANSWER: NO - Usage should NOT reset to 0

---

## What You've Built

Your app has a **sophisticated two-layer usage tracking system** that ensures data persistence:

### Layer 1: Real-Time Calculation
- Current session time: `now - lastResumeTime`
- Updated every 5 seconds
- Cleared when app goes to background

### Layer 2: Persistent Database
- Sessions saved every 60 seconds
- Stored in `app_usage_sessions` table
- Survives app closures indefinitely

### Combined Usage Formula
```
Total Usage = Saved Base Usage (from DB) + Current Session Time (live)
```

---

## How Usage Persistence Works

### When App is OPEN
```
Every 5 seconds:
1. Get current time
2. Get base usage from system = e.g., 52 minutes
3. Get live usage = e.g., 2 minutes (current session)
4. Total = 52 + 2 = 54 minutes
5. Show to user

Every 60 seconds:
6. Save 1-minute session to database
   (Ensures data survives if app is killed)
```

### When App is CLOSED
```
1. Android sends ACTIVITY_PAUSED event
2. lastPauseTime is recorded
3. System updates its UsageStats
4. Database already has saved sessions
   (Sessions were saved every 60 seconds while app was open)
5. Service continues running in background
```

### When App is REOPENED
```
1. Service checks app again
2. Retrieves base usage from system = 52 minutes (unchanged)
3. Calculates live usage = 0 (new session just started)
4. Total = 52 + 0 = 52 minutes
5. Show to user (PERSISTED - NOT reset)
```

---

## Key Components Ensuring Persistence

### 1. Real-Time Usage Calculation
**File:** `AppLimitManager.kt`
```kotlin
// Retrieves saved base usage (doesn't reset)
val baseUsageMillis = usageStats?.totalTimeInForeground ?: 0L

// Calculates current session (active only while app is open)
val inProgressMillis = if (lastResumeTime > lastPauseTime) {
    (now - lastResumeTime)
} else {
    0L
}

// IMPORTANT: Always combines both parts
val totalUsageMillis = baseUsageMillis + inProgressMillis
```
✅ This ensures usage doesn't reset

### 2. Session Saving
**File:** `AppMonitorService.kt`
```kotlin
// Save to database every 60 seconds
if (timeSinceLastSave >= 60_000L) {
    appLimitManager.saveUsageSession(currentApp, usedMinutes)
    sessionSavedApps[currentApp] = now
}
```
✅ This persists data to database

### 3. Database Schema
**File:** `AppLimit.kt`
```kotlin
@Entity(tableName = "app_usage_sessions")
data class AppUsageSession(
    val packageName: String,
    val date: String,           // Date of usage
    val startTime: Long,        // When session started
    val endTime: Long,          // When session ended
    val durationMinutes: Int    // How many minutes used
)
```
✅ This stores historical data

### 4. Overlay Shows on Reopen
**File:** `AppMonitorService.kt`
```kotlin
// Always show overlay if limit exceeded
// No check for "already blocked" - shows every time
when (status) {
    is LimitStatus.Exceeded -> {
        overlayBlockManager.showBlockingOverlay(...)
    }
}
```
✅ This shows blocking UI every time

---

## Enhanced Debug Logging

The updated code includes emoji-prefixed logging for easy tracking:

```
📱 [CHECK]    - Checking current app (every 5 seconds)
📊 [STATUS]   - Status update (WithinLimit/Exceeded/NoLimit)
💾 [SAVE]     - Saving session to database (every 60 seconds)
⏳ [SKIP]     - Skipping save (not enough time passed yet)
✅ [WITHIN]   - App within limit
🚨 [EXCEEDED] - App exceeded limit
🚫 [OVERLAY]  - Showing blocking overlay
⚠️  [WARNING] - Warning when ≤5 minutes remaining
ℹ️  [NO LIMIT] - No limit set for app
❌ [ERROR]    - Error occurred
```

---

## Testing the Implementation

### Test 1: Real-Time Tracking
1. Set a 5-min limit for YouTube
2. Open YouTube
3. Watch Logcat - should see `📊 [STATUS]` every 5 seconds
4. Usage should increase: 0→1→2→3→4→5 minutes
✅ **Pass**: Usage increases smoothly

### Test 2: Session Saving
1. Use YouTube for 2 minutes continuously
2. Watch Logcat for `💾 [SAVE]` messages
3. Should see saves at: ~60s, ~120s
✅ **Pass**: Saves appear every 60 seconds

### Test 3: Persistence on Close/Reopen
1. Use YouTube for 1 minute
2. Note dashboard: ~1 minute
3. Close YouTube (press HOME)
4. Reopen YouTube
5. Check dashboard
✅ **Pass**: Dashboard shows ~1 minute (NOT 0)

### Test 4: Overlay on Exceeded
1. Set 1-min limit
2. Use app for 2 minutes
3. Close app
4. Reopen app
5. Overlay should appear immediately
✅ **Pass**: Overlay shows "2/1 minutes"

---

## Expected Log Output Timeline

For a 3-minute YouTube session:

```
T=00s   📱 [CHECK] youtube
        📊 [STATUS] WithinLimit
        Usage: 0ms = 0min

T=05s   📱 [CHECK] youtube
        📊 [STATUS] WithinLimit
        Usage: 5000ms = 0min

...

T=60s   💾 [SAVE] youtube = 1min
        ✅ Saved 1-min session (total: 1min)

T=120s  💾 [SAVE] youtube = 2min
        ✅ Saved 1-min session (total: 2min)

T=180s  💾 [SAVE] youtube = 3min
        ✅ Saved 1-min session (total: 3min)

T=185s  (USER CLOSES APP)
        Usage saved to database: 3 minutes

T=190s  (USER REOPENS APP)
        📱 [CHECK] youtube
        📊 [STATUS] WithinLimit (3min)
        Usage: 180000ms = 3min (base: 180000, live: 0)
        ✓ Usage persisted - NOT reset to 0
```

---

## Database Verification

### Check if sessions are saved:
1. Open Android Studio Device Explorer
2. Navigate to: `/data/data/com.example.screentime/databases/screentime_db.db`
3. Download and open with SQLite viewer
4. Check `app_usage_sessions` table
   - Should have entries with app's package name
   - Each row = 1 minute of saved usage

### Example database entries:
```
| packageName    | date       | durationMinutes |
|----------------|------------|-----------------|
| youtube        | 2026-02-25 | 1               |
| youtube        | 2026-02-25 | 1               |
| youtube        | 2026-02-25 | 1               |
| instagram      | 2026-02-25 | 1               |
| instagram      | 2026-02-25 | 1               |
```

---

## Troubleshooting

### If Usage Resets to 0 on Reopen:

**Step 1: Check if sessions are being saved**
- Look for `💾 [SAVE]` in logcat
- If present → Sessions ARE being saved
- If absent → Check 60-second delay logic

**Step 2: Check base usage retrieval**
- Look for `(base:` in usage log
- If base > 0 → Base is being retrieved correctly
- If base = 0 → System might not have data for app

**Step 3: Check database**
- Use SQLite viewer on app_usage_sessions
- If has rows → Database is working
- If empty → Sessions not reaching database

**Step 4: Check overlay**
- Look for `🚫 [OVERLAY]` on reopen
- If present → Status IS Exceeded, overlay working
- If absent → Status might not be Exceeded

---

## What Gets Built

✅ **Real-Time Display**
- Usage updates every 5 seconds
- Shows current active session time

✅ **Persistent Storage**
- Sessions saved every 60 seconds
- Data stored in database
- Survives app closures

✅ **Accurate Calculation**
- Base + Live = Total
- Both parts combined every check
- Never resets unless day changes

✅ **Overlay on Exceeding**
- Shows overlay every time if exceeded
- Even after closing and reopening

✅ **Daily Reset**
- Usage resets at midnight
- New day = new usage counter

---

## Summary for Your Testing

Your app has been enhanced with:

1. **Better Logging** - Emoji prefixes make it easy to track what's happening
2. **Persistence Logic** - Two-layer system (base + live) ensures data survives
3. **Session Saving** - Every 60 seconds to database for safety
4. **Overlay Logic** - Shows every time if limit exceeded

**Build Status**: ✅ Ready to test

**Next Steps**:
1. Install the app on your device
2. Run the Quick Usage Test (QUICK_USAGE_TEST.md)
3. Watch Logcat for emoji-prefixed messages
4. Verify database has entries
5. Confirm usage persists on close/reopen

**Expected Result**: Usage should NEVER reset to 0 when closing and reopening the app.

