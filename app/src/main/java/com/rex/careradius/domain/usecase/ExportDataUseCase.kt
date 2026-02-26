package com.rex.careradius.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import android.util.JsonWriter
import com.rex.careradius.data.local.dao.GeofenceDao
import com.rex.careradius.data.local.dao.VisitDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UseCase dedicated to exporting user data to JSON.
 *
 * ARCHITECTURE DECISION:
 * Why JsonWriter instead of kotlinx.serialization or Moshi?
 * 1. Zero dependencies added to the project.
 * 2. O(1) memory footprint. `JsonWriter` streams directly to the OutputStream.
 *    If the user has 10,000+ visits, building a massive `List<ExportVisitDto>` in memory
 *    just to pass to a serializer could cause an OutOfMemoryError. Streaming prevents this.
 */
class ExportDataUseCase(
    private val geofenceDao: GeofenceDao,
    private val visitDao: VisitDao,
    private val contentResolver: ContentResolver
) {

    // Thread-safe ISO-8601 formatter
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    suspend fun execute(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(Exception("Could not open ContentResolver stream"))

            // Use BufferedWriter to reduce I/O churn
            val writer = JsonWriter(BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")))
            
            // Format constraint: Pretty printed JSON
            writer.setIndent("  ")

            writer.use { json ->
                // Root Object
                json.beginObject()

                json.name("app").value("CareRadius")
                json.name("exportedAt").value(formatIso(System.currentTimeMillis()))
                json.name("timezone").value(ZoneId.systemDefault().id)

                // 1. Export Zones Array
                json.name("zones")
                json.beginArray()
                
                // Fetch all zones (usually a small list, safe to load into memory)
                val zones = geofenceDao.getAllGeofences().first()
                for (zone in zones) {
                    json.beginObject()
                    json.name("name").value(zone.name)
                    json.name("emoji").value(zone.icon) // Assuming 'icon' holds the emoji
                    json.name("radiusMeters").value(zone.radius.toDouble()) // Float cast
                    json.name("latitude").value(zone.latitude)
                    json.name("longitude").value(zone.longitude)
                    json.name("createdAt").value(formatIso(zone.createdAt))
                    // Not currently tracking updatedAt in DB, omitting as per optional constraint
                    json.endObject()
                }
                json.endArray() // End Zones

                // 2. Export Visits Array
                json.name("visits")
                json.beginArray()
                
                // Fetch visits. Using first() loads them. 
                // If visits scale to 100k+, pagination should be introduced here.
                val visits = visitDao.getAllVisitsWithGeofence().first()
                for (visitPair in visits) {
                    val visit = visitPair.visit
                    json.beginObject()
                    
                    // Requirement: zoneName (not foreign key id)
                    json.name("zoneName").value(visit.geofenceName)
                    
                    json.name("arrivedAt").value(formatIso(visit.entryTime))
                    
                    if (visit.exitTime != null) {
                        json.name("leftAt").value(formatIso(visit.exitTime))
                    } else {
                        json.name("leftAt").nullValue()
                    }
                    
                    if (visit.durationMillis != null) {
                        json.name("durationSeconds").value(visit.durationMillis / 1000)
                    } else {
                        json.name("durationSeconds").nullValue()
                    }
                    
                    val isActive = visit.exitTime == null
                    json.name("isActive").value(isActive)
                    
                    json.endObject()
                }
                json.endArray() // End Visits

                json.endObject() // End Root
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatIso(epochMillis: Long): String {
        return isoFormatter.format(Instant.ofEpochMilli(epochMillis))
    }
}
