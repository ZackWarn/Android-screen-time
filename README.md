# Screen Time App with Rewards & Analytics

A comprehensive Android application built with Jetpack Compose that tracks screen time usage, rewards users with badges and points, and provides analytics with week-over-week comparisons.

## Features

### 📊 Dashboard
- **Weekly Progress Tracking**: Visual progress bar showing current week's points accumulated
- **Daily Breakdown**: View screen time and points earned for each day of the week
- **Badge Collection**: Display earned badges this week at a glance

### 📈 Analytics
- **Week Comparison**: Side-by-side comparison of current week vs. previous week screen time
- **Daily Details**: Detailed breakdown of daily usage for both weeks
- **Improvement Metrics**: Calculate and display percentage improvement or increase vs. last week
- **Historical Data**: Access to previous weeks' statistics

### 🏆 Rewards System
- **Weekly Badges**: Earn badges by achieving milestones (reset every Sunday midnight)
- **Points Accumulation**: Earn points throughout the week based on screen time behavior
- **5 Badge Types**:
  - **Focused Week**: Keep screen time under 4 hours for 5+ days (Common)
  - **Zero Day**: Complete 24-hour period with zero screen time (Rare) 
  - **Consistent User**: Use app every day for 7 consecutive days (Rare)
  - **Improvement**: Reduce screen time by 30% vs. previous week (Rare)
  - **Champion**: Earn all other badges in a single week (Legendary)

## Architecture

### Technology Stack
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Database**: Room Database (SQLite)
- **Concurrency**: Kotlin Coroutines
- **Background Tasks**: WorkManager
- **Navigation**: Jetpack Navigation Compose
- **MVVM Architecture**: ViewModel + StateFlow

### Project Structure

```
com.example.screentime/
├── data/
│   ├── dao/                          # Database Access Objects
│   │   ├── DailyProgressDao.kt
│   │   ├── BadgeDao.kt
│   │   ├── WeeklyStatsDao.kt
│   │   └── AppSettingsDao.kt
│   ├── entities/                     # Database entities
│   │   ├── DailyProgress.kt
│   │   ├── Badge.kt
│   │   ├── WeeklyStats.kt
│   │   └── AppSettings.kt
│   ├── repository/                   # Data repositories
│   │   ├── ScreenTimeRepository.kt
│   │   ├── BadgeRepository.kt
│   │   └── WeeklyStatsRepository.kt
│   ├── ScreenTimeDatabase.kt         # Room database definition
│   └── Converters.kt                 # Type converters
├── domain/
│   ├── managers/                     # Business logic
│   │   ├── RewardCalculationManager.kt
│   │   └── ScreenTimeTrackerManager.kt
│   ├── workers/                      # Background workers
│   │   ├── DailyScreenTimeWorker.kt
│   │   └── WeeklyResetWorker.kt
│   └── initialization/
│       └── AppInitializer.kt
├── presentation/
│   ├── screens/                      # UI Screens
│   │   ├── DashboardScreen.kt
│   │   ├── AnalyticsScreen.kt
│   │   └── RewardsScreen.kt
│   ├── viewmodels/                   # ViewModels
│   │   ├── DashboardViewModel.kt
│   │   ├── AnalyticsViewModel.kt
│   │   └── RewardsViewModel.kt
│   ├── components/                   # Reusable Composables
│   │   ├── Cards.kt
│   │   └── Badges.kt
│   └── navigation/
│       └── NavigationHost.kt
├── utils/
│   ├── WeekUtils.kt                  # Week calculations
│   └── BadgeDefinitions.kt           # Badge configuration
└── ui/theme/                         # Theming
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## Points & Badges System

### Points Calculation (Daily)
- **1 point** per hour under 4-hour daily limit (max 4 points)
- **5 points** bonus for zero-screen day
- **10 points** per badge unlocked

**Maximum weekly points**: 115 points (7 days × 4 base + 5 zero bonuses + 5 badges × 10)

### Weekly Reset
- Badges and points reset every Sunday at midnight
- Previous week statistics are archived for analytics
- Users can track progress week-over-week

## Data Storage

### Database Schema
- **daily_progress**: Daily screen time and points earned
- **earned_badges**: Unlocked badges with timestamp
- **weekly_stats**: Aggregated statistics for completed weeks
- **app_settings**: App configuration and reset timestamps
- **badge_definitions**: Badge metadata (in-memory via BadgeDefinitions utility)

### Permissions Required
- `android.permission.PACKAGE_USAGE_STATS`: Access to screen time data (runtime checked)
- `android.permission.INTERNET`: For potential future cloud sync features

## Background Tasks

### Daily Screen Time Worker
- **Frequency**: Once per day
- **Task**: Captures daily screen usage, calculates points, checks badge unlock conditions
- **Storage**: Saves daily progress to database

### Weekly Reset Worker
- **Frequency**: Once per week (every 7 days)
- **Task**: Archives previous week stats, resets points/badges for new week
- **Data Preservation**: Maintains historical data for analytics

## Screen Time Tracking

The app uses `UsageStatsManager` to capture:
- Total foreground time per app
- System apps and internal apps are filtered out
- Converted to minutes for storage and calculation
- Returns 0 if permission is not granted (graceful degradation)

## User Flows

### First Launch
1. App initializes database
2. Background workers are scheduled
3. User views empty Dashboard
4. Receives sample data after first daily collection

### Daily Usage
1. WorkManager runs daily task at ~24-hour intervals
2. Captures screen time from UsageStatsManager
3. Calculates points and checks badge conditions
4. Displays updated stats in Dashboard
5. User can navigate to Analytics and Rewards

### Weekly Transition
1. WorkManager runs weekly reset task
2. Previous week stats are finalized and archived
3. Badges and points are reset to 0
4. New week begins with fresh statistics
5. Previous week data available in Analytics for comparison

## UI Navigation

Bottom Navigation Bar with 3 tabs:
- **Home** (Dashboard): Current week progress and daily breakdown
- **Analytics** (Info): Week comparison and detailed analytics
- **Rewards** (Favorite): Badge collection and points display

## Testing the App

### Manual Testing
1. **Dashboard**: Verify weekly progress and daily breakdown display
2. **Analytics**: Compare current vs. previous week data
3. **Rewards**: Check badge unlock conditions and point calculations
4. **Week Reset**: Change device date forward to next week to test reset

### Sample Data
The app gracefully handles lack of screen time data. For development/testing:
1. Manually insert DailyProgress records via Room database
2. Trigger workers manually using WorkManager test utilities
3. Use Android Studio's Device File Explorer to inspect database

## Configuration

### Screen Time Limit
- Default: 240 minutes (4 hours)
- Can be adjusted in `RewardCalculationManager.DAILY_SCREEN_TIME_LIMIT`

### Week Start Day
- Default: Sunday (via Locale settings)
- Uses ISO 8601 week numbering
- Configured in `WeekUtils.kt`

## Future Enhancements

- [ ] App-specific tracking (which apps contribute to screen time)
- [ ] Customizable daily limit settings
- [ ] Push notifications for milestone achievements
- [ ] Cloud sync and cross-device support
- [ ] Detailed app-level analytics
- [ ] Custom badge creation
- [ ] Leaderboard or social features
- [ ] Deep links and shortcuts

## Known Limitations

1. **Screen Time Permission**: Requires runtime permission grant on first use
2. **Historical Data**: Only starts tracking after app installation
3. **Device Time Changes**: Rapid date changes may affect calculations
4. **Low-End Devices**: Large date ranges in analytics may cause performance issues

## Build & Run

```bash
# Clean build
./gradlew clean build

# Run on device
./gradlew installDebug

# Release build
./gradlew build --variant=release
```

## Dependencies

- androidx.room:room-runtime
- androidx.work:work-runtime-ktx
- androidx.navigation:navigation-compose
- androidx.lifecycle:lifecycle-viewmodel-compose
- androidx.compose.material3:material3
- androidx.compose.ui:ui

## License

MIT License - Feel free to use and modify for your needs.

