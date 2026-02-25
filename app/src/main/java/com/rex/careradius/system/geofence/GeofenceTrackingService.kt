package com.rex.careradius.system.geofence

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rex.careradius.MainActivity
import com.rex.careradius.R

//  Lightweight foreground service that keeps the app process alive so OEM ROMs
//  don't kill it and block geofence PendingIntent delivery.

//  This service does NO location work itself — it simply prevents process death.
//  All geofence detection is still handled by Google Play Services + GeofenceReceiver.
class GeofenceTrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "tracking_channel"
        // must not collide with geofence event notification IDs (which use geofenceId.toInt())
        private const val NOTIFICATION_ID = 99999

        fun start(context: Context) {
            val intent = Intent(context, GeofenceTrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GeofenceTrackingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If the OS kills this service, restart it automatically
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW // Low = no sound, just persistent icon
        ).apply {
            description = "Keeps CareRadius active for reliable zone tracking"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        // Tapping the notification opens the app
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
