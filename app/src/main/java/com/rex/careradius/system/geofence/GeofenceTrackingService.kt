package com.rex.careradius.system.geofence

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rex.careradius.MainActivity
import com.rex.careradius.R
import com.rex.careradius.data.local.AppDatabase
import com.rex.careradius.data.local.entity.VisitEntity
import com.rex.careradius.data.repository.UserPreferencesRepository
import com.rex.careradius.system.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// foreground service that polls location at a user configurable interval and checks zone distances.
// google play services geofencing is unreliable on many devices so we do our own checking.
// the GeofenceReceiver is still registered as a bonus but this is the primary detection now.
class GeofenceTrackingService : Service() {

    companion object {
        private const val TAG = "GeofenceTrackingService"
        private const val CHANNEL_ID = "tracking_channel"
        // must not collide with geofence event notification IDs which use geofenceId.toInt()
        private const val NOTIFICATION_ID = 99999

        fun start(context: Context) {
            context.startForegroundService(Intent(context, GeofenceTrackingService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GeofenceTrackingService::class.java))
        }
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    // cached so we dont recreate them on every poll or notification
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        prefsRepo = UserPreferencesRepository(this)
        notificationHelper = NotificationHelper(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startLocationPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationPolling() {
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        // read the users preferred interval from DataStore before starting location updates
        CoroutineScope(Dispatchers.IO).launch {
            val intervalMs = prefsRepo.pollingIntervalMs.first()
            Log.d(TAG, "polling interval set to ${intervalMs}ms")

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
                .setMinUpdateIntervalMillis(intervalMs / 2)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    Log.d(TAG, "location update: ${location.latitude}, ${location.longitude}")
                    CoroutineScope(Dispatchers.IO).launch {
                        checkGeofences(location)
                    }
                }
            }
            locationCallback = callback

            try {
                fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            } catch (e: SecurityException) {
                Log.e(TAG, "location permission missing, cant poll: ${e.message}")
            }
        }
    }

    // compare current location against all zones in the db.
    // if inside a zone and no open visit exists, create one.
    // if outside all zones and an open visit exists, close it.
    private suspend fun checkGeofences(currentLocation: Location) {
        val database = AppDatabase.getDatabase(this)
        val geofenceDao = database.geofenceDao()
        val visitDao = database.visitDao()

        val allGeofences = geofenceDao.getAllGeofences().first()
        val openVisits = visitDao.getOpenVisits()
        val now = System.currentTimeMillis()

        // figure out which zones we are currently inside
        val insideZoneIds = mutableSetOf<Long>()
        for (geofence in allGeofences) {
            val distance = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                geofence.latitude, geofence.longitude,
                distance
            )
            if (distance[0] <= geofence.radius) {
                insideZoneIds.add(geofence.id)
            }
        }

        // close any open visits for zones we are no longer inside
        for (visit in openVisits) {
            if (visit.geofenceId == null || visit.geofenceId !in insideZoneIds) {
                Log.d(TAG, "closing stale visit for '${visit.geofenceName}'")
                visitDao.updateVisit(visit.copy(exitTime = now, durationMillis = now - visit.entryTime))

                val geofence = visit.geofenceId?.let { geofenceDao.getGeofenceById(it) }
                sendNotification(
                    geofence?.name ?: visit.geofenceName,
                    "Exited",
                    (visit.geofenceId?.toInt() ?: 0) + 10000,
                    geofence?.exitMessage
                )
            }
        }

        // if we are inside a zone and there is no open visit for it, create one.
        // only track the first zone (single active visit rule from GeofenceReceiver)
        val openVisitGeofenceIds = openVisits.mapNotNull { it.geofenceId }.toSet()
        if (insideZoneIds.isNotEmpty()) {
            val zoneId = insideZoneIds.first()
            if (zoneId !in openVisitGeofenceIds) {
                // close any other open visits first
                visitDao.closeAllOpenVisits(now)

                val geofence = geofenceDao.getGeofenceById(zoneId) ?: return
                visitDao.insert(VisitEntity(
                    geofenceId = geofence.id,
                    geofenceName = geofence.name,
                    entryTime = now,
                    exitTime = null,
                    durationMillis = null
                ))
                Log.d(TAG, "created visit for '${geofence.name}'")
                sendNotification(geofence.name, "Entered", geofence.id.toInt(), geofence.entryMessage)
            }
        }
    }

    // checks the notification preference before sending. suspend because its called
    // from within an existing coroutine so no need for a new scope.
    private suspend fun sendNotification(zoneName: String, eventType: String, notificationId: Int, customMessage: String?) {
        val notificationsEnabled = prefsRepo.isNotificationsEnabled.first()
        if (notificationsEnabled) {
            notificationHelper.showGeofenceNotification(zoneName, eventType, notificationId, customMessage)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps CareRadius active for reliable zone tracking"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("CareRadius")
            .setContentText("Monitoring your zones")
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
