# CareRadius 📍

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
- **Adjustable Radius** - Set radius from 10m to 500m via slider
- **Edit Anytime** - Modify name, icon, radius, or location of existing geofences
- **Change Location** - Relocate a geofence by dropping a new pin or using current location

### 🔔 Smart Notifications
- **Entry Alerts** - Get notified when you enter a geofenced area
- **Exit Alerts** - Get notified when you leave a geofenced area
- **Background Monitoring** - Works even when app is closed

### ⏱️ Visit History
- **Automatic Tracking** - Entry/exit times recorded automatically
- **Duration Calculation** - See exactly how long you spent at each location
- **Active Visit Indicator** - Highlights visits still in progress
- **History Preservation** - Visits remain even if geofence is deleted
- **Clear History** - Delete all visit records with one tap
- **Individual Delete** - Remove specific visit records

### 💾 Data Management
- **Persistent Storage** - All data saved locally using Room database
- **Offline Support** - Works without internet connection
- **Database Migrations** - Smooth updates without data loss

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
| **Room** | Local SQLite database |
| **MapLibre GL** | OpenStreetMap-based maps |
| **Google Play Services** | Geofencing & Location APIs |
| **Coroutines & Flow** | Async operations & reactive data |
| **Navigation Compose** | Screen navigation |
| **Material 3** | UI components & theming |

## Architecture

The app follows **MVVM (Model-View-ViewModel)** architecture with a **Repository pattern**:

```
┌───────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                       │
│      MapScreen  │  GeofenceListScreen  │  VisitListScreen     │
└───────────────────────────────────────────────────────────────┘
                              ↓
┌───────────────────────────────────────────────────────────────┐
│                      ViewModel Layer                          │
│ MapViewModel  │  GeofenceListViewModel  │  VisitListViewModel │
└───────────────────────────────────────────────────────────────┘
                              ↓
┌───────────────────────────────────────────────────────────────┐
│                     Repository Layer                          │
│       GeofenceRepository    │    VisitRepository              │
└───────────────────────────────────────────────────────────────┘
                              ↓
┌───────────────────────────────────────────────────────────────┐
│                         Data Layer                            │
│       Room Database  │  GeofenceDao  │  VisitDao              │
└───────────────────────────────────────────────────────────────┘
                              ↓
┌───────────────────────────────────────────────────────────────┐
│                        System Layer                           │
│  GeofenceManager  │  GeofenceReceiver  │  NotificationHelper  │
└───────────────────────────────────────────────────────────────┘
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
│       └── VisitRepository.kt
├── domain/
│   └── model/                # Domain/UI models
│       ├── GeofenceModel.kt
│       └── VisitModel.kt
├── navigation/
│   ├── NavGraph.kt           # Navigation setup
│   └── Screen.kt             # Screen definitions
├── presentation/
│   ├── geofencelist/         # Geofence list & edit screen
│   │   ├── GeofenceListScreen.kt
│   │   └── GeofenceListViewModel.kt
│   ├── map/                  # Map screen
│   │   ├── MapScreen.kt
│   │   └── MapViewModel.kt
│   └── visitlist/            # Visit history screen
│       ├── VisitListScreen.kt
│       └── VisitListViewModel.kt
├── system/
│   ├── geofence/
│   │   ├── GeofenceManager.kt    # Register/unregister geofences
│   │   └── GeofenceReceiver.kt   # BroadcastReceiver for transitions
│   ├── location/
│   │   └── LocationPermissionHandler.kt
│   └── notification/
│       └── NotificationHelper.kt
├── ui/theme/                 # Material 3 theme
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
                              GeofenceReceiver triggered on ENTER/EXIT
                                                    ↓
                        Visit record created/updated → Notification sent
```

### Background Processing
The `GeofenceReceiver` is a `BroadcastReceiver` that handles transitions even when the app is closed:
- Creates visit records with geofence name on **ENTER**
- Calculates duration and updates records on **EXIT**
- Sends notifications for both events

### Data Persistence
All data is stored in a local Room database with proper migrations:

| Entity | Fields |
|--------|--------|
| `GeofenceEntity` | id, name, icon, latitude, longitude, radius, createdAt |
| `VisitEntity` | id, geofenceId (nullable), geofenceName, entryTime, exitTime, durationMillis |

**Note:** Visits preserve the geofence name even after the geofence is deleted.

## Permissions Required

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` | Precise location for geofencing |
| `ACCESS_BACKGROUND_LOCATION` | Monitor geofences when app is closed |
| `POST_NOTIFICATIONS` | Show entry/exit notifications |

## Geofencing Best Practices

For reliable geofence detection:

1. **Minimum Radius** - Using 100m+ for reliable triggers (Android limitation) (Note: the max radius for a fence to 10 - 50 Meters in this app for now )
2. **Battery Optimization** - Disable for this app (Settings → Apps → Battery → Unrestricted)
3. **Location Mode** - Use "High Accuracy" GPS mode
4. **Physical Movement** - GPS jitter doesn't trigger events; real movement required


## License

This project is open source and available under the [MIT License](LICENSE).


