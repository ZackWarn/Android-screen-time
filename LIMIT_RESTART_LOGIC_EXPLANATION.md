# Limit Reset Logic Explanation - When App is Reopened

## The Issue You're Seeing

**When you reopen the app, the limit counter seems to "restart" or reset**

This happens because of how the app calculates usage on reopen. Let me explain the logic:

---

## The Root Cause

### Current Implementation Problem

When you **reopen the app**, this happens:

```
1. Service restarts/initializes
2. appLimitManager.getCurrentAppUsageMillis() is called
3. It queries UsageStats for "totalTimeInForeground"
4. On service restart, this might query from a FRESH timeframe
5. RESULT: Shows recent usage only, not total accumulated usage
```

**The Bug**: If the service or app process dies and restarts, the `lastResumeTime` and `lastPauseTime` tracking variables are RESET to 0. When the next check happens, the calculation starts fresh.

---

## How Current Code Works (With The Bug)

### Session Tracking Variables
```kotlin
private var lastResumeTime = 0L      // ← RESETS to 0 on app restart
private var lastPauseTime = 0L       // ← RESETS to 0 on app restart
```

### Problem Flow on Reopen

```
BEFORE CLOSE:
├─ YouTube open for 5 minutes
├─ lastResumeTime = 1000ms (when YouTube opened)
├─ lastPauseTime = 0ms (still active)
├─ live usage = (now - 1000ms) = 5 minutes
└─ Total = base(0) + live(5) = 5 minutes

USER CLOSES APP (press home):
├─ ACTIVITY_PAUSED event triggers
├─ lastPauseTime = now
├─ live usage calculation stops

USER CLOSES SCREENTIME APP (or system kills it):
├─ All variables are RESET to 0
├─ lastResumeTime = 0L
├─ lastPauseTime = 0L
└─ (SERVICE DIES)

USER REOPENS SCREENTIME APP:
├─ Service restarts
├─ lastResumeTime = 0L (reset!)
├─ lastPauseTime = 0L (reset!)
├─ YouTube is STILL OPEN on device
└─ Problem: Can't calculate correct live session

NEXT CHECK:
├─ Queries UsageEvents from midnight to now
├─ Finds: lastResumeTime = 0L, lastPauseTime = 0L
├─ YouTube WAS resumed, but lastResumeTime wasn't captured
├─ Can't properly calculate live usage from previous session
└─ Shows incorrect/incomplete usage
```

---

## The Logic Behind This

### Why It Happens

Your app uses a **stateful tracking system** that assumes the service stays alive:

```kotlin
// In AppMonitorService.kt (NOT PERSISTED)
while (usageEvents.hasNextEvent()) {
    usageEvents.getNextEvent(event)
    if (event.packageName == packageName) {
        when (event.eventType) {
            android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                lastResumeTime = event.timeStamp  // ← Stored in RAM
            }
            android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                lastPauseTime = event.timeStamp   // ← Stored in RAM
            }
        }
    }
}
```

**Problem**: These variables are in **memory only**, not in database.

When service restarts → **Variables reset to 0**

### What Actually Happens When Service Restarts

```
Service Restart:
├─ onCreate() called
├─ All instance variables reset to 0L
├─ sessionSavedApps map cleared
├─ Database connection re-established
├─ startMonitoring() called again
└─ checkCurrentApp() starts fresh

First Check After Restart:
├─ Calls getCurrentAppUsageMillis()
├─ lastResumeTime = 0L (from fresh service)
├─ lastPauseTime = 0L (from fresh service)
├─ Since lastResumeTime (0) is NOT > lastPauseTime (0)
├─ inProgressMillis = 0L
└─ Only shows base usage, not live session!
```

---

## Code That Causes This

### AppMonitorService.kt - Variable Reset on Restart
```kotlin
class AppMonitorService : Service() {
    private var lastCheckedApp: String? = null
    private val sessionSavedApps = mutableMapOf<String, Long>()  // ← IN MEMORY
    
    override fun onCreate() {
        super.onCreate()
        // ... initialization ...
        sessionSavedApps.clear()  // ← CLEARED on restart!
        startMonitoring()
    }
}
```

### AppLimitManager.kt - Session Tracking (Not Persisted)
```kotlin
while (usageEvents.hasNextEvent()) {
    usageEvents.getNextEvent(event)
    if (event.packageName == packageName) {
        when (event.eventType) {
            ACTIVITY_RESUMED -> {
                lastResumeTime = event.timeStamp  // ← RESET on service restart
            }
            ACTIVITY_PAUSED -> {
                lastPauseTime = event.timeStamp   // ← RESET on service restart
            }
        }
    }
}

val inProgressMillis = if (lastResumeTime > lastPauseTime) {
    (now - lastResumeTime)  // ← Returns 0 if lastResumeTime = 0
} else {
    0L  // ← Returns 0 if service just restarted
}
```

**This means**: On first check after service restart, `inProgressMillis = 0`

---

## Why The Limit "Restarts"

### Scenario: You Use YouTube for 3 Minutes

```
T=0min    Database: usedTodayMinutes = 0
          Open YouTube
          lastResumeTime = timestamp

T=1min    Database: usedTodayMinutes = 1 (saved)
          Live usage: 1 minute
          Total shown: 0 (base) + 1 (live) = 1 min

T=2min    Database: usedTodayMinutes = 2 (saved)
          Live usage: 2 minutes
          Total shown: 0 (base) + 2 (live) = 2 min

T=3min    Database: usedTodayMinutes = 3 (saved)
          Live usage: 3 minutes
          Total shown: 0 (base) + 3 (live) = 3 min

SCREENTIME APP CRASHES or closes:
          Service dies
          lastResumeTime = RESET to 0

REOPEN SCREENTIME:
          Service restarts
          First check:
          ├─ Database: usedTodayMinutes = 3 (STILL THERE!)
          ├─ lastResumeTime = 0 (RESET!)
          ├─ Can't calculate live usage
          ├─ inProgressMillis = 0 (because lastResumeTime = 0)
          └─ Total shown: 3 (base) + 0 (live) = 3 min

WAIT A FEW SECONDS:
          Next check queries UsageEvents AGAIN
          Finds YouTube was resumed at time X
          Updates lastResumeTime = X
          NOW calculates: now - X = live time
          Total shown: 3 (base) + new live = correct
```

---

## Where The "Restart" Happens

### The Critical Point

Look at this code in `AppLimitManager.kt`:

```kotlin
fun getCurrentAppUsageMinutes(packageName: String): Int {
    val usageMillis = getCurrentAppUsageMillis(packageName)
    return (usageMillis / 60000L).toInt()
}

private fun getCurrentAppUsageMillis(packageName: String): Long {
    // Line 182-183: Gets base usage
    val appUsage = usageStats.find { it.packageName == packageName }
    val baseUsageMillis = appUsage?.totalTimeInForeground ?: 0L
    
    // Line 186: Queries events
    val usageEvents = usageStatsManager.queryEvents(startOfDay, now)
    
    // Line 189: IN-MEMORY VARIABLES (RESET ON SERVICE RESTART)
    var lastResumeTime = 0L    // ← START AT 0
    var lastPauseTime = 0L     // ← START AT 0
    
    // Line 190-201: Updates these variables
    while (usageEvents.hasNextEvent()) {
        // ... processes events ...
    }
    
    // Line 204-208: CALCULATION HAPPENS HERE
    val inProgressMillis = if (lastResumeTime > lastPauseTime) {
        (now - lastResumeTime)  // ← Only works if lastResumeTime was updated above
    } else {
        0L  // ← Returns 0 if no resume event found
    }
    
    // Line 210: COMBINES BOTH
    val totalUsageMillis = baseUsageMillis + inProgressMillis
    // ↑ This is where the limit "restarts"
}
```

**Issue**: These `lastResumeTime` and `lastPauseTime` are LOCAL variables, not class-level. Each call to `getCurrentAppUsageMillis()` starts fresh.

---

## Why Limits "Restart" When Service Dies

### Scenario with Low Time Left

```
Set limit: 10 minutes
Current usage: 9 minutes
Remaining: 1 minute

User opens app with 30 seconds left

BEFORE SERVICE RESTART:
├─ Base: 9 minutes (from DB)
├─ Live: 30 seconds (current session)
├─ Total: 9.5 minutes
├─ Status: WITHIN LIMIT (9.5 < 10)
└─ Dashboard shows: 9.5/10 minutes

SCREENTIME APP CLOSES:
├─ Service dies
├─ Variables reset
├─ BUT database still has: usedTodayMinutes = 9

USER REOPENS SCREENTIME:
├─ Service restarts
├─ Database: usedTodayMinutes = 9 (unchanged)
├─ Live calc fails initially (can't find resume event immediately)
├─ Shows: 9/10 minutes (seems like it "restarted")
├─ Remaining shown as 1 minute again (but it's not new!)
└─ After a few seconds: Finds resume event, shows 9+ minutes again
```

---

## The Real Problem Explained

Your app uses **3 sources of truth**:

1. **Database** (`usedTodayMinutes`) - Persists ✓
2. **System UsageStats** (`totalTimeInForeground`) - Persists ✓
3. **In-Memory Variables** (`lastResumeTime`, `lastPauseTime`) - **LOST on restart** ✗

When service restarts, #3 is lost, causing calculation failures until the next UsageEvents query.

---

## How To Fix This (Solution)

### Option 1: Use Only Database & System (Recommended)

Stop relying on in-memory `lastResumeTime`. Instead:

```kotlin
private fun getCurrentAppUsageMillis(packageName: String): Long {
    // Get base usage (persists in system)
    val baseUsageMillis = usageStats?.totalTimeInForeground ?: 0L
    
    // For live session: Query latest resume time from UsageEvents
    // This is ALREADY in the queryEvents call, just process it better
    
    val usageEvents = usageStatsManager.queryEvents(startOfDay, now)
    
    var latestResumeTime = 0L
    var latestPauseTime = 0L
    
    while (usageEvents.hasNextEvent()) {
        usageEvents.getNextEvent(event)
        if (event.packageName == packageName) {
            // Always track the LATEST times, don't reset
            when (event.eventType) {
                ACTIVITY_RESUMED -> latestResumeTime = event.timeStamp
                ACTIVITY_PAUSED -> latestPauseTime = event.timeStamp
            }
        }
    }
    
    // Calculate live usage from latest events
    val inProgressMillis = if (latestResumeTime > latestPauseTime) {
        (now - latestResumeTime)
    } else {
        0L
    }
    
    return baseUsageMillis + inProgressMillis
}
```

### Option 2: Persist Resume/Pause Times to Database

Store `lastResumeTime` and `lastPauseTime` in the database so they survive service restarts.

---

## Summary: The Logic Behind The Restart

| When | What Happens | Why |
|------|-------------|-----|
| **During Use** | Calculates: base + live | Live time is current session |
| **Service Restart** | In-memory vars reset to 0 | Service init sets them to 0L |
| **First Check** | Can't find live time immediately | Variables are 0, need to re-query events |
| **Second+ Check** | Works correctly again | Events are processed, variables updated |
| **Limit "Restarts"** | Because live time = 0 briefly | It's actually just delayed calculation |

The "restart" isn't a real restart—it's a **temporary gap in calculation** caused by losing the in-memory resume/pause timestamps when the service dies.

**The database usage persists ✓, but the real-time calculation breaks temporarily ✗**

