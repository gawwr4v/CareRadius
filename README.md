# CareRadius 📍

A geofencing Android application that tracks your visits to specific locations. Set up virtual boundaries around places you care about and automatically log when you enter and exit.

## Features

- **📌 Geofence Creation** - Long-press on the map to create geofences with custom names, icons, and radius (10-50m)
- **🔔 Entry/Exit Notifications** - Get notified when you enter or leave a geofenced area
- **⏱️ Duration Tracking** - Automatically calculates time spent at each location
- **📊 Visit History** - View all your visits with timestamps and duration
- **💾 Persistent Storage** - All data saved locally, works offline
- **🗺️ Custom Map Markers** - Use emoji icons to personalize your geofences

## Screenshots

| Map View | Geofence List | Visit History |
|----------|---------------|---------------|
| ![Map](screenshots/map.png) | ![List](screenshots/list.png) | ![Visits](screenshots/visits.png) |

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Database:** Room
- **Maps:** MapLibre GL Native (OpenStreetMap)
- **Location Services:** Google Play Services Location API
- **Architecture:** MVVM with Repository Pattern
- **Navigation:** Jetpack Navigation Compose

## Requirements

- Android 10 (API 29) or higher
- Location permissions (Fine + Background)
- Google Play Services

## Project Structure

```
app/src/main/java/com/rex/careradius/
├── data/
│   ├── local/
│   │   ├── dao/           # room DAOs
│   │   ├── entity/        # database entities
│   │   └── AppDatabase.kt
│   └── repository/        # data repositories
├── domain/
│   └── model/             # domain models
├── navigation/
│   └── NavGraph.kt        # navigation setup
├── presentation/
│   ├── geofencelist/      # geofence list screen
│   ├── map/               # map screen
│   └── visitlist/         # visit history screen
├── system/
│   ├── geofence/          # geofence manager & receiver
│   ├── location/          # permission handling
│   └── notification/      # notification helper
└── ui/theme/              # compose theme
```

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

### Geofencing
The app uses Google Play Services Geofencing API to monitor location transitions. When you create a geofence:
1. Location coordinates and radius are saved to Room database
2. A geofence is registered with the system
3. A BroadcastReceiver listens for ENTER/EXIT events
4. Events trigger notifications and database updates

### Background Processing
The `GeofenceReceiver` is a `BroadcastReceiver` that handles transitions even when the app is closed. It:
- Creates visit records on ENTER
- Calculates duration and updates records on EXIT
- Sends notifications for both events

### Data Persistence
All geofences and visits are stored in a local Room database:
- `GeofenceEntity` - location details, radius, creation time
- `VisitEntity` - entry/exit times, calculated duration

## Permissions Required

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` | Precise location for geofencing |
| `ACCESS_BACKGROUND_LOCATION` | Monitor geofences when app is closed |
| `POST_NOTIFICATIONS` | Show entry/exit notifications |
