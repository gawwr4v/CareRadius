# CareRadius 📍

![Platform: Android](https://img.shields.io/badge/Platform-Android-green)
![Min API: 29+](https://img.shields.io/badge/Min%20API-29%2B-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-blue?logo=android)
![Architecture: MVVM](https://img.shields.io/badge/Architecture-MVVM-orange)
![Room Database](https://img.shields.io/badge/Database-Room-9C27B0)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow)
![Version](https://img.shields.io/badge/Version-1.4-brightgreen)

A geofencing Android application that tracks your visits to specific locations. Set up virtual boundaries around places you care about and automatically log when you enter and exit.

## Features

### 🗺️ Interactive Map
- **Pin Drop** - Long-press anywhere on the map to create a geofence
- **Current Location** - Quick button to add geofence at your current location
- **Custom Markers** - Emoji icons displayed on the map for each geofence
- **Visual Radius** - Colored circles show geofence boundaries

### 📌 Geofence Management
- **Custom Names** - Give each location a memorable name
- **Emoji Icons** - Choose from a variety of emojis to represent locations
- **Adjustable Radius** - Set radius from 10m to 50m via slider
- **Edit Anytime** - Modify name, icon, radius, or location of existing geofences
- **Change Location** - Relocate a geofence by dropping a new pin, current location, or manual coordinates

### 🔔 Smart Notifications
- **Custom Reminders** - Set personalized arrival and exit messages per zone (e.g. "Take medicine", "Lock the door")
- **Entry Alerts** - Get notified when you enter a geofenced area
- **Exit Alerts** - Get notified when you leave a geofenced area
- **Background Monitoring** - Foreground service keeps tracking alive even when app is closed
- **Persistent Notification** - Shows "CareRadius - Monitoring your zones" while tracking is active

### ⏱️ Visit History
- **Automatic Tracking** - Entry/exit times recorded automatically
- **Duration Calculation** - See exactly how long you spent at each location
- **Active Visit Indicator** - Highlights visits still in progress
- **History Preservation** - Visits remain even if geofence is deleted
- **Clear History** - Delete all visit records with one tap
- **Individual Delete** - Remove specific visit records

### ⚙️ Settings
- **Dark / Light Theme** - Toggle between dark and light mode with preference persistence
- **Theme Follows System** - Defaults to system theme on first launch
- **Location Check Interval** - Choose how often the service checks your location (1 min, 2 min, 5 min)
- **Background Reliability** - Quick access to app battery settings for unrestricted mode
- **Notification Toggle** - Enable or disable arrival and exit alerts

### 💾 Data Management
- **JSON Export / Import** - Export all zones and visits as JSON, import with Replace or Merge strategies
- **Persistent Storage** - All data saved locally using Room database
- **Offline Support** - Works without internet connection


## Screenshots

<table>
  <tr>
    <th>Map View</th>
    <th>Geofence List</th>
    <th>Visit History</th>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/92e1c22a-61f5-4198-9c69-e2cd7f17f273" width="250"/><br/>
      <em>Adding a Location </em>
      <br/><br/>
      <img src="https://github.com/user-attachments/assets/ec13e213-d8a5-4cd2-8bea-101faa405313" width="250"/><br/>
      <em>Geofence markers</em>
    </td>
    <td align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/24cb64bf-d564-485b-8dcb-6b0540797435" width="250"/><br/>
      <em>Saved geofences with edit options</em>
    </td>
    <td align="center" valign="top">
      <img src="https://github.com/user-attachments/assets/08403c5c-1f9c-41e3-94d8-ba8cc3f32e24" width="250"/><br/>
      <em>Visit history with duration</em>
    </td>
  </tr>
</table>


## Tech Stack

| Technology | Purpose |
|------------|---------|
| **Kotlin** | Primary language |
| **Jetpack Compose** | Modern declarative UI |
| **Material 3** | UI components & theming (Nordic Utility design system) |
| **Room** | Local SQLite database |
| **DataStore** | User preferences (theme, notifications, polling interval) |
| **MapLibre GL** | OpenStreetMap-based maps |
| **Google Play Services** | FusedLocationProviderClient for location polling |
| **Foreground Service** | Keeps the app alive for reliable background tracking |
| **Coroutines & Flow** | Async operations & reactive data |
| **Navigation Compose** | Screen navigation with animated transitions |

## Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture with a **Repository pattern**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          UI Layer (Compose)                             │
│  MapScreen  │  GeofenceListScreen  │  VisitListScreen  │ SettingsScreen │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                          ViewModel Layer                                │
│ MapViewModel │ GeofenceListViewModel │ VisitListViewModel │ SettingsVM  │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                         Repository Layer                                │
│    GeofenceRepository  │  VisitRepository  │  UserPreferencesRepository │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                             Data Layer                                  │
│     Room Database  │  GeofenceDao  │  VisitDao  │  DataStore            │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                            System Layer                                 │
│  GeofenceTrackingService │ GeofenceManager │ GeofenceReceiver │ Notifs  │
└─────────────────────────────────────────────────────────────────────────┘
```

## Project Structure

```
app/src/main/java/com/rex/careradius/
├── data/
│   ├── local/
│   │   ├── dao/              # Room DAOs (GeofenceDao, VisitDao)
│   │   ├── entity/           # Database entities
│   │   │   ├── GeofenceEntity.kt
│   │   │   ├── VisitEntity.kt
│   │   │   └── VisitWithGeofence.kt
│   │   └── AppDatabase.kt    # Room database with migrations
│   └── repository/           # Data repositories
│       ├── GeofenceRepository.kt
│       ├── VisitRepository.kt
│       └── UserPreferencesRepository.kt  # DataStore for theme, notifications, polling interval
├── domain/
│   └── model/                # Domain/UI models
│       ├── GeofenceModel.kt
│       └── VisitModel.kt
├── navigation/
│   └── NavGraph.kt           # Navigation + route definitions
├── presentation/
│   ├── components/           # Shared UI components
│   │   └── PageHeader.kt
│   ├── geofencelist/         # Geofence list & edit screen
│   │   ├── GeofenceListScreen.kt
│   │   └── GeofenceListViewModel.kt
│   ├── map/                  # Map screen
│   │   ├── MapScreen.kt
│   │   └── MapViewModel.kt
│   ├── settings/             # App settings
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── visitlist/            # Visit history screen
│       ├── VisitListScreen.kt
│       └── VisitListViewModel.kt
├── system/
│   ├── geofence/
│   │   ├── GeofenceManager.kt           # Register/unregister geofences, stale visit reconciliation
│   │   ├── GeofenceReceiver.kt          # BroadcastReceiver for transitions (backup)
│   │   ├── GeofenceTrackingService.kt   # Foreground service, polls location and checks zones
│   │   └── GeofenceBootReceiver.kt      # Re-registers geofences and restarts service after reboot
│   ├── location/
│   │   └── LocationPermissionHandler.kt
│   └── notification/
│       └── NotificationHelper.kt
├── ui/theme/                 # Overall theme
│   ├── Color.kt
│   ├── Shape.kt
│   ├── Theme.kt
│   └── Type.kt
└── MainActivity.kt           # Single activity entry point
```

## Requirements

- **Android 10 (API 29)** or higher
- **Location permissions** (Fine + Background)
- **Google Play Services** installed on device
- **GPS/High Accuracy location** mode recommended

## Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/gawwr4v/CareRadius.git
   ```

2. **Open in Android Studio**
   - File → Open → Select project folder

3. **Sync Gradle**
   - Android Studio will automatically sync dependencies

4. **Run the app**
   - Connect a device or start an emulator (API 29+)
   - Click Run (▶️)

## How It Works

### Geofencing Flow
```
User creates geofence → Saved to Room DB → Registered with GeofencingClient
                                                    ↓
                  GeofenceTrackingService polls location every N seconds
                                                    ↓
              Checks distance to all zones → Creates/closes visits → Sends notifications
```

### Background Processing
The app uses a **foreground service** (`GeofenceTrackingService`) for reliable background tracking:
- Polls device location at a user-configurable interval (1 min, 2 min, or 5 min)
- Compares current position against all registered geofence radii
- Creates visit records on **ENTER**, closes them with duration on **EXIT**
- Sends notifications for both events (if enabled)
- Uses `START_STICKY` to auto-restart if the OS kills the service
- Restarts after device reboot via `GeofenceBootReceiver`

**Why not rely on Google Play Services geofencing alone?**
Google's `GeofencingClient` delivers transitions via `PendingIntent`, but on many devices these broadcasts are silently dropped when the app is in the background. The foreground service ensures the app process stays alive and handles detection directly.

### Data Persistence
All data is stored in a local Room database with proper migrations:

| Entity | Fields |
|--------|--------|
| `GeofenceEntity` | id, name, icon, latitude, longitude, radius, createdAt, entryMessage, exitMessage |
| `VisitEntity` | id, geofenceId (nullable), geofenceName, entryTime, exitTime, durationMillis |

**Note:** Visits preserve the geofence name even after the geofence is deleted.

## Permissions Required

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` | Precise location for geofencing |
| `ACCESS_BACKGROUND_LOCATION` | Monitor geofences when app is closed |
| `POST_NOTIFICATIONS` | Show entry/exit notifications |
| `FOREGROUND_SERVICE` | Run persistent tracking service |
| `FOREGROUND_SERVICE_LOCATION` | Allow location access in foreground service |
| `RECEIVE_BOOT_COMPLETED` | Re-register geofences and restart service after reboot |

## Geofencing Best Practices

For reliable geofence detection:

1. **Minimum Radius** - Android recommends 100m+ for reliable triggers. This app allows 10-50m for precise tracking, but detection may be less consistent at smaller radii
2. **Battery Optimization** - Set to Unrestricted (Settings > Apps > CareRadius > Battery > Unrestricted) for best results
3. **Location Mode** - Use "High Accuracy" GPS mode
4. **Polling Interval** - Use 1 min or 2 min for faster detection. 5 min saves battery but delays detection

## Database Migrations

The app handles database upgrades automatically:

| Version | Changes |
|---------|---------|
| 1 → 2 | Added `icon` column to geofences |
| 2 → 3 | Added `geofenceName` to visits, made `geofenceId` nullable (SET NULL on delete) |
| 3 → 4 | Added `entryMessage` and `exitMessage` columns to geofences and heavily updated the UI and overall app theme |

## License

This project is open source and available under the [MIT License](LICENSE).

---

**Built with ❤️ using Kotlin and Jetpack Compose**
