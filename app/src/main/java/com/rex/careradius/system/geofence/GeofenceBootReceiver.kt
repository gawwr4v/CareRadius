package com.rex.careradius.system.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// re registers all geofences after device reboot.
// geofences registered with Google Play Services are lost on restart,
// so this also restarts the tracking service.
class GeofenceBootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            android.util.Log.d("GeofenceBootReceiver", "Device rebooted, re-registering geofences")
            val pendingResult = goAsync()
            val geofenceManager = GeofenceManager(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    geofenceManager.reregisterAllGeofencesSync()
                    // Restart the foreground service so OEM ROMs don't kill geofence delivery
                    GeofenceTrackingService.start(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
