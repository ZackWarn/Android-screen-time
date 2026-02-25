# Quick Usage Tracking Test - 5 Minute Verification

## Test Setup (1 minute)

1. **Open Android Studio**
2. **Open Logcat** (View → Tool Windows → Logcat)
3. **Add filter:** `AppMonitorService|AppLimitManager`
4. **Install app on device/emulator**

---

## Test 1: Real-Time Usage Tracking (2 minutes)

### Steps:
1. Set a 10-minute limit for an app (e.g., YouTube)
2. Open that app
3. Leave it open for 2 minutes
4. Watch Logcat while it runs

### What You Should See in Logcat:
```
Every 5 seconds:
📱 [CHECK] Checking app: com.google.android.youtube
📊 [STATUS] Status: WithinLimit
Usage: XXX ms = Y min (base: 0, live: XXX)
```

### Expected Result:
- Usage should increase every 5 seconds
- Live counter goes up (1000ms per second)
- Base stays at 0 (no previous sessions)

✅ **Test Passes If:** Usage incrementally increases (0→2→4→6 minutes)

❌ **Test Fails If:** Usage stays at 0 or jumps randomly

---

## Test 2: Session Saving (2 minutes)

### Steps:
1. Keep the app open from Test 1
2. Watch Logcat for the 60-second save mark
3. Continue watching for the next save at 120 seconds

### What You Should See in Logcat:
```
At 60 seconds:
💾 [SAVE] Saving session: youtube = 1 min (time since last save: 60s)
✅ Saved 1-min session: youtube (total: 1 min)

At 120 seconds:
💾 [SAVE] Saving session: youtube = 2 min (time since last save: 60s)
✅ Saved 1-min session: youtube (total: 2 min)
```

### Expected Result:
- One save message approximately every 60 seconds
- Each save shows total accumulated minutes
- Total increases with each save (1 → 2 → 3...)

✅ **Test Passes If:** Saves appear every ~60 seconds with increasing totals

❌ **Test Fails If:** Saves don't appear or total resets between saves

---

## Test 3: Usage Persists on Close & Reopen (3 minutes)

### Steps:
1. Use the app for **exactly 1 minute**
2. Note the dashboard shows ~1 minute
3. **Press HOME button** (close the app)
4. Wait 10 seconds
5. **Open the same app again**
6. Check dashboard immediately

### What You Should See:
```
Before closing:
Dashboard: 1 minute

After closing (Logcat):
⏳ [SKIP] Not saving yet...

After reopening (Logcat):
📱 [CHECK] Checking app: youtube
📊 [STATUS] Status: WithinLimit (1 min)
Usage: 60000 ms = 1 min (base: 60000, live: 0)
                          ↑ Base is retrieved from database

Dashboard: ~1 minute (NOT 0)
```

### Expected Result:
- Dashboard shows same usage (≥1 minute)
- NOT reset to 0
- Logcat shows "base: 60000" (retrieved from saved data)

✅ **Test Passes If:** Usage persists at ≥1 minute after reopening

❌ **Test Fails If:** Usage shows 0 minutes after reopening

---

## Test 4: Overlay Shows on Limit Exceeded (2 minutes)

### Steps:
1. Set YouTube limit to **1 minute**
2. Open YouTube
3. Use it for **2 minutes**
4. Logcat should show exceeded status
5. Close YouTube
6. Reopen YouTube
7. **Overlay should appear immediately**

### What You Should See:
```
After 1 minute (Logcat):
🚨 [EXCEEDED] LIMIT EXCEEDED: 2/1 min
🚫 [OVERLAY] Showing overlay for youtube

On Dashboard:
⚠️ Shows "2/1 min"

After closing & reopening (Logcat):
📱 [CHECK] Checking app: youtube
📊 [STATUS] Status: Exceeded
Usage: 120000 ms = 2 min (base: 120000, live: 0)
🚫 [OVERLAY] Showing overlay for youtube

On Screen:
🎨 Red overlay appears saying "YouTube - 2/1 minutes"
"Return to Home" button visible
After 1.5s: Auto-closes to home
```

### Expected Result:
- Overlay appears both times (first when limit exceeded, then again on reopen)
- Shows correct "2/1 minutes" format
- Auto-closes after ~1.5 seconds

✅ **Test Passes If:** Overlay shows on both encounters

❌ **Test Fails If:** Overlay doesn't appear on second opening

---

## Quick Verification Table

| Test | Check For | Expected | Result |
|------|-----------|----------|--------|
| Test 1 | Increasing usage | 0→2→4→6 min | ✓ if increases |
| Test 2 | 💾 [SAVE] messages | One every 60s | ✓ if regular |
| Test 3 | Base usage retrieved | `base: 60000` | ✓ if shown |
| Test 3 | Dashboard after reopen | ≥1 min (not 0) | ✓ if ≥1 min |
| Test 4 | 🚫 [OVERLAY] messages | Appears twice | ✓ if appears both times |
| Test 4 | Overlay screen | Red banner | ✓ if shows |

---

## If a Test FAILS

### Test 1 Fails (Usage doesn't increase)
- Check if app is actually in foreground (logcat should show it)
- Check if system has usage data for the app (first-time use might delay)
- Wait a few minutes for system to collect data

### Test 2 Fails (No save messages)
- Check if time has actually passed (need at least 60 seconds)
- Check if usage is > 0 (must have used app to save)
- Try using app for 90 seconds then check at 60s mark

### Test 3 Fails (Usage resets to 0)
- Database issue - sessions not being saved to DB
- Check logcat for 💾 [SAVE] messages before closing
- Verify database has entries in app_usage_sessions table

### Test 4 Fails (Overlay doesn't appear on reopen)
- Check if Overlay permission is granted
- Check logcat for 🚨 [EXCEEDED] status
- Check if isBlocked field in database is true

---

## Quick Logcat Filter Tips

### Filter to just AppMonitorService:
```
tag:AppMonitorService
```

### Filter to just AppLimitManager:
```
tag:AppLimitManager
```

### Filter to see usage calculations:
```
AppLimitManager.*Usage
```

### Filter to see saves only:
```
SAVE
```

### Filter to see errors only:
```
ERROR|EXCEEDED
```

---

## Summary

Your app tracks usage in **real-time** and **persists it across closures** because:

1. ✅ System UsageStats provides base usage
2. ✅ Live session time is calculated
3. ✅ Both are combined for total
4. ✅ Sessions saved every 60 seconds
5. ✅ Overlay shows every time on reopen if exceeded

**Expected outcome:** Usage should NEVER reset to 0 when closing and reopening.

**If it does reset:** Check the Logcat logs with the emoji filters to see where it's failing.

