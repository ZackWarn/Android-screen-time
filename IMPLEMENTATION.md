# Screen Time App - Implementation Details

## Database Schema

### Tables

#### daily_progress
Stores daily screen time and points information.

| Column | Type | Constraints |
|--------|------|-------------|
| date | LocalDate | PRIMARY KEY |
| screenTimeMinutes | Int | |
| pointsEarned | Int | |
| badgesUnlockedCount | Int | |

#### earned_badges
Tracks earned badges with unlock timestamps.

| Column | Type | Constraints |
|--------|------|-------------|
| id | Int | PRIMARY KEY, AUTOINCREMENT |
| badgeType | BadgeType | Enum: FOCUSED_WEEK, ZERO_DAY, CONSISTENT_USER, IMPROVEMENT, CHAMPION |
| unlockedDate | String | ISO format date-time |
| weekNumber | Int | ISO week number |

#### weekly_stats
Aggregated statistics for completed weeks.

| Column | Type | Constraints |
|--------|------|-------------|
| weekNumber | Int | PRIMARY KEY (part) |
| year | Int | PRIMARY KEY (part) |
| totalScreenTimeMinutes | Int | |
| totalPointsEarned | Int | |
| totalBadgesEarned | Int | |
| averageDailyUsageMinutes | Float | |
| isCompleted | Boolean | For archived weeks |

#### app_settings
Application-wide configuration.

| Column | Type | Constraints |
|--------|------|-------------|
| id | Int | PRIMARY KEY (always 1) |
| dailyScreenTimeLimit | Int | Default: 240 minutes |
| lastResetTimestamp | String | ISO format |
| currentWeekNumber | Int | |
| currentYear | Int | |

## Reward Calculation Logic

### Daily Points Calculation

```kotlin
fun calculateDailyPoints(screenTimeMinutes: Int, badgesUnlockedCount: Int): Int {
    var points = 0
    
    // 1 point per hour under limit (max 4)
    if (screenTimeMinutes < 240) {
        points += (screenTimeMinutes / 60) * 1
    }
    
    // 5 point bonus for zero day
    if (screenTimeMinutes == 0) {
        points += 5
    }
    
    // 10 points per badge
    points += badgesUnlockedCount * 10
    
    return points
}
```

### Badge Unlock Conditions

#### 1. FOCUSED_WEEK
- **Condition**: 5+ days in the week with screen time < 240 minutes
- **Check**: Daily in DailyScreenTimeWorker
- **Logic**:
  ```kotlin
  val daysUnderLimit = weeklyProgress.count { it.screenTimeMinutes < 240 }
  daysUnderLimit >= 5
  ```

#### 2. ZERO_DAY
- **Condition**: Any day with screen time = 0 minutes
- **Check**: Daily in DailyScreenTimeWorker
- **Logic**: Can be unlocked multiple times per week
  ```kotlin
  progress.screenTimeMinutes == 0
  ```

#### 3. CONSISTENT_USER
- **Condition**: All 7 days of the week have activity
- **Check**: Daily check if week is complete
- **Logic**:
  ```kotlin
  weeklyProgress.size == 7
  ```

#### 4. IMPROVEMENT
- **Condition**: Current week total < (Previous week total × 0.7)
- **Check**: Daily calculation
- **Logic**:
  ```kotlin
  val improvement = ((prevWeekTotal - currentTotal) / prevWeekTotal) * 100
  improvement >= 30
  ```

#### 5. CHAMPION
- **Condition**: All 4 other badges earned in same week
- **Check**: After each badge unlock
- **Logic**:
  ```kotlin
  checkIfAllOtherBadgesUnlocked(weekNumber)
  ```

## Background Task Scheduling

### DailyScreenTimeWorker

**Type**: PeriodicWork (1 day interval)

**Execution**:
1. Query UsageStatsManager for screen time
2. Retrieve previous day's data from database
3. Calculate points for the day
4. Check badge unlock conditions
5. Save DailyProgress entry

**Retry Policy**: Automatic retry on failure (3 attempts)

### WeeklyResetWorker

**Type**: PeriodicWork (7 day interval)

**Execution**:
1. Identify previous week (week before current)
2. Aggregate daily progress into WeeklyStats
3. Count earned badges for previous week
4. Save completed WeeklyStats with isCompleted=true
5. Create new WeeklyStats entry for current week
6. Note: Individual daily entries remain for analytics

**Idempotent**: Uses isCompleted flag to prevent duplicate resets

## View Model Implementation

### DashboardViewModel

```kotlin
class DashboardViewModel(
    private val screenTimeRepository: ScreenTimeRepository,
    private val badgeRepository: BadgeRepository,
    private val weeklyStatsRepository: WeeklyStatsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    fun loadDashboardData() {
        viewModelScope.launch {
            val weekNumber = WeekUtils.getCurrentWeekNumber()
            val weekStart = WeekUtils.getWeekStartDate(weekNumber)
            val weekEnd = WeekUtils.getWeekEndDate(weekNumber)
            
            // Fetch from repositories
            val weeklyProgress = screenTimeRepository.getWeeklyProgress(weekStart, weekEnd)
            val totalPoints = weeklyProgress.sumOf { it.pointsEarned }
            val earnedBadges = badgeRepository.getBadgesForWeek(weekNumber)
            
            // Update state
            _uiState.value = DashboardUiState(
                weekNumber = weekNumber,
                currentWeekPoints = totalPoints,
                weeklyProgress = weeklyProgress,
                earnedBadges = earnedBadges
            )
        }
    }
}
```

### AnalyticsViewModel

```kotlin
class AnalyticsViewModel(
    private val screenTimeRepository: ScreenTimeRepository,
    private val weeklyStatsRepository: WeeklyStatsRepository
) : ViewModel() {
    
    fun loadAnalyticsData() {
        viewModelScope.launch {
            // Get current week data
            val currentWeekNumber = WeekUtils.getCurrentWeekNumber()
            val currentWeekStart = WeekUtils.getWeekStartDate(currentWeekNumber)
            val currentWeekEnd = WeekUtils.getWeekEndDate(currentWeekNumber)
            
            // Get previous week data
            val prevWeekNumber = WeekUtils.getPreviousWeekNumber()
            val prevWeekYear = WeekUtils.getPreviousWeekYear()
            val prevWeekStart = WeekUtils.getWeekStartDate(prevWeekNumber, prevWeekYear)
            val prevWeekEnd = WeekUtils.getWeekEndDate(prevWeekNumber, prevWeekYear)
            
            // Calculate improvement
            val currentTotal = currentWeekProgress.sumOf { it.screenTimeMinutes }
            val prevTotal = previousWeekProgress.sumOf { it.screenTimeMinutes }
            val improvement = if (prevTotal > 0) {
                ((prevTotal - currentTotal) / prevTotal) * 100
            } else {
                0f
            }
            
            _uiState.value = AnalyticsUiState(
                improvementPercentage = improvement,
                // ... other state
            )
        }
    }
}
```

## Screen Time Capture Implementation

### ScreenTimeTrackerManager

```kotlin
fun getDailyScreenTime(dayInMillis: Long = System.currentTimeMillis()): Int {
    // Check permission first
    if (!hasUsageStatsPermission()) return 0
    
    // Get start of day
    val calendar = Calendar.getInstance().apply {
        timeInMillis = dayInMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    
    val startTime = calendar.timeInMillis
    val endTime = startTime + 24 * 60 * 60 * 1000 // 24 hours
    
    // Query UsageStatsManager
    val usageStats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )
    
    // Sum foreground time (excluding system apps)
    var totalScreenTime = 0L
    for (stats in usageStats) {
        if (!isSystemApp(stats.packageName) && 
            stats.packageName != context.packageName) {
            totalScreenTime += stats.totalTimeInForeground
        }
    }
    
    return (totalScreenTime / 1000 / 60).toInt() // Convert to minutes
}
```

**Key Points**:
- Filters out system apps (android.*, com.android.*)
- Filters out our own app to avoid circular counting
- Returns 0 if permission denied (graceful degradation)
- Converts milliseconds to minutes for storage

## Navigation Architecture

### NavigationHost Composable

```kotlin
@Composable
fun NavigationHost(context: Context) {
    val selectedTab = remember { mutableIntStateOf(0) }
    
    // Initialize dependencies
    val db = ScreenTimeDatabase.getDatabase(context)
    val dashboardViewModel = remember { DashboardViewModel(...) }
    val analyticsViewModel = remember { AnalyticsViewModel(...) }
    val rewardsViewModel = remember { RewardsViewModel(...) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                // 3 navigation items
            }
        }
    ) { innerPadding ->
        when (selectedTab.intValue) {
            0 -> DashboardScreen(viewModel = dashboardViewModel)
            1 -> AnalyticsScreen(viewModel = analyticsViewModel)
            2 -> RewardsScreen(viewModel = rewardsViewModel)
        }
    }
}
```

**Navigation Pattern**:
- Single Activity (MainActivity)
- Bottom navigation for tab switching
- ViewModels persist across tab changes
- Each screen has independent ViewModel

## Week Calculation Logic

### WeekUtils

```kotlin
fun getCurrentWeekNumber(): Int {
    return LocalDate.now().get(weekFields.weekOfWeekBasedYear())
}

fun getWeekStartDate(weekNumber: Int): LocalDate {
    return LocalDate.of(currentYear, 1, 1)
        .with(weekFields.weekOfWeekBasedYear(), weekNumber.toLong())
        .with(weekFields.dayOfWeek(), 1L) // Monday
}

fun getPreviousWeekNumber(): Int {
    val current = getCurrentWeekNumber()
    return if (current == 1) 52 else current - 1
}
```

**Key Points**:
- Uses ISO 8601 week numbering
- Week starts on Monday
- Handles year boundaries (week 1 of next year)
- Locale-aware for different regions

## Error Handling Strategy

### In Workers
- All operations wrapped in try-catch
- Logs exceptions
- Returns Result.retry() on failure
- WorkManager handles exponential backoff

### In ViewModels
- Loading state before async operations
- Error state for failed operations
- Graceful degradation (empty states)
- User-friendly error messages

### In Repository
- Suspend functions for coroutine safety
- Null checks for optional data
- Default values (empty lists, 0)
- Exception propagation to ViewModel

## Compose UI Patterns

### State Management
```kotlin
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState = viewModel.uiState.collectAsState().value
    
    // Render based on state
    if (uiState.isLoading) {
        // Loading UI
    } else if (uiState.error != null) {
        // Error UI
    } else {
        // Content UI
    }
}
```

### Lazy Layouts
```kotlin
LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    item { /* Header */ }
    items(dailyProgress) { progress ->
        DailyProgressItem(progress)
    }
    item { /* Footer */ }
}
```

### Reusable Components
```kotlin
@Composable
fun DailyProgressItem(
    dayOfWeek: String,
    screenTimeMinutes: Int,
    pointsEarned: Int,
    modifier: Modifier = Modifier
)
```

## Testing Strategy

### Manual Testing Checklist
- [ ] Dashboard loads with empty state
- [ ] Daily progress items populate after worker runs
- [ ] Points calculate correctly
- [ ] Badges unlock with correct conditions
- [ ] Analytics shows previous week data
- [ ] Week reset clears points and badges
- [ ] Improvement calculation is accurate
- [ ] Colors and rarity match badges

### Automated Testing (Future)
- Unit tests for RewardCalculationManager
- Unit tests for WeekUtils calculations
- ViewModel tests with fake repositories
- Compose UI tests for screens
- Database tests for DAOs

## Performance Considerations

1. **Database Queries**: Indexed on date, weekNumber
2. **Lazy Loading**: LazyColumn for long lists
3. **Coroutines**: All I/O on Dispatchers.IO
4. **StateFlow**: Efficient state updates
5. **Recomposition**: Only on state changes

## Security Considerations

1. **Permissions**: Runtime checks for PACKAGE_USAGE_STATS
2. **Data Privacy**: All data stored locally
3. **No Network**: No data transmission (for MVP)
4. **Database**: Encrypted with Room encryption (optional)

## Future Architecture Improvements

1. **Dependency Injection**: Add Hilt/Dagger for DI
2. **Unit Tests**: Add comprehensive test suite
3. **Data Sync**: Add Firebase for cloud backup
4. **Analytics**: Add Firebase Analytics tracking
5. **Notifications**: Add push notifications for badges
6. **Widgets**: Add home screen widgets
7. **Shortcuts**: Add app shortcuts
8. **Biometric Auth**: Add fingerprint protection

