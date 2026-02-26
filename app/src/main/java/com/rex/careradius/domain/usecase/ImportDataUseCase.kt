package com.rex.careradius.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import android.util.JsonReader
import android.util.Log
import androidx.room.withTransaction
import com.rex.careradius.data.local.AppDatabase
import com.rex.careradius.data.local.dao.GeofenceDao
import com.rex.careradius.data.local.dao.VisitDao
import com.rex.careradius.data.local.entity.GeofenceEntity
import com.rex.careradius.data.local.entity.VisitEntity
import com.rex.careradius.system.geofence.GeofenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.time.Instant
import java.time.format.DateTimeParseException

class ImportDataUseCase(
    private val database: AppDatabase,
    private val geofenceDao: GeofenceDao,
    private val visitDao: VisitDao,
    private val geofenceManager: GeofenceManager,
    private val contentResolver: ContentResolver
) {

    suspend fun execute(uri: Uri, isReplaceMode: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Could not open file."))

            val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))

            var appName: String? = null
            val zonesToImport = mutableListOf<GeofenceEntity>()
            val visitsToImport = mutableListOf<VisitEntity>()

            var skippedZones = 0
            var skippedVisits = 0

            // 1. Parsing Phase (Memory-efficient streaming)
            reader.use { json ->
                json.beginObject()
                while (json.hasNext()) {
                    when (json.nextName()) {
                        "app" -> appName = json.nextString()
                        "exportedAt" -> json.skipValue()
                        "timezone" -> json.skipValue()
                        "zones" -> {
                            json.beginArray()
                            while (json.hasNext()) {
                                try {
                                    val zone = parseZone(json)
                                    if (zone != null) zonesToImport.add(zone) else skippedZones++
                                } catch (e: Exception) {
                                    Log.e("ImportDataUseCase", "Skipping malformed zone", e)
                                    skippedZones++
                                }
                            }
                            json.endArray()
                        }
                        "visits" -> {
                            json.beginArray()
                            while (json.hasNext()) {
                                try {
                                    val visit = parseVisit(json)
                                    if (visit != null) visitsToImport.add(visit) else skippedVisits++
                                } catch (e: Exception) {
                                    Log.e("ImportDataUseCase", "Skipping malformed visit", e)
                                    skippedVisits++
                                }
                            }
                            json.endArray()
                        }
                        else -> json.skipValue()
                    }
                }
                json.endObject()
            }

            // Validation
            if (appName != "CareRadius") {
                return@withContext Result.failure(Exception("Invalid file format. Ensure it was exported from CareRadius."))
            }

            // 2. Database Transaction Phase
            database.withTransaction {
                if (isReplaceMode) {
                    // Wipe everything first

                    visitDao.deleteAllVisits()
                    geofenceDao.deleteAllGeofences()

                    // Insert fresh
                    zonesToImport.forEach { geofenceDao.insert(it) }
                    visitsToImport.forEach { visitDao.insert(it) }
                    
                } else {
                    // Merge Strategy
                    zonesToImport.forEach { zone ->
                        val existing = geofenceDao.getGeofenceByName(zone.name)
                        if (existing == null) {
                            geofenceDao.insert(zone)
                        }
                    }

                    visitsToImport.forEach { visit ->
                        val existing = visitDao.getVisitByAttributes(
                            zoneName = visit.geofenceName,
                            entryTime = visit.entryTime,
                            exitTime = visit.exitTime
                        )
                        if (existing == null) {
                            visitDao.insert(visit)
                        }
                    }
                }
            }

            // 3. Synchronization Phase
            // DB transaction committed safely. Now sync the OS Tracking layer with our new state.
            // This safely clears old tracking IDs using PendingIntent mapping prior to reinstating the freshly created ones.
            geofenceManager.reregisterAllGeofencesSync()

            Log.d("ImportDataUseCase", "Imported ${zonesToImport.size} zones, ${visitsToImport.size} visits. Skipped: Z:$skippedZones V:$skippedVisits")
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseZone(json: JsonReader): GeofenceEntity? {
        var name = ""
        var emoji = "📍"
        var radius = 0f
        var lat = 0.0
        var lng = 0.0
        var createdAt = System.currentTimeMillis()

        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "name" -> name = json.nextString()
                "emoji" -> emoji = json.nextString()
                "radiusMeters" -> radius = json.nextDouble().toFloat()
                "latitude" -> lat = json.nextDouble()
                "longitude" -> lng = json.nextDouble()
                "createdAt" -> {
                    try {
                        createdAt = Instant.parse(json.nextString()).toEpochMilli()
                    } catch (e: DateTimeParseException) {
                        // fallback to now if invalid date
                    }
                }
                else -> json.skipValue()
            }
        }
        json.endObject()

        if (name.isBlank() || radius <= 0f || lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
            return null // Invalid zone
        }

        return GeofenceEntity(
            name = name.trim(),
            icon = emoji,
            radius = radius,
            latitude = lat,
            longitude = lng,
            createdAt = createdAt
        )
    }

    private fun parseVisit(json: JsonReader): VisitEntity? {
        var zoneName = ""
        var arrivedAt = -1L
        var leftAt: Long? = null
        var duration: Long? = null

        json.beginObject()
        while (json.hasNext()) {
            when (json.nextName()) {
                "zoneName" -> zoneName = json.nextString()
                "arrivedAt" -> {
                    try {
                        arrivedAt = Instant.parse(json.nextString()).toEpochMilli()
                    } catch (e: DateTimeParseException) {
                        // critical failure for visit
                    }
                }
                "leftAt" -> {
                    if (json.peek() == android.util.JsonToken.NULL) {
                        json.nextNull()
                    } else {
                        try {
                            leftAt = Instant.parse(json.nextString()).toEpochMilli()
                        } catch (e: DateTimeParseException) {
                           // ignoring
                        }
                    }
                }
                "durationSeconds" -> {
                    if (json.peek() == android.util.JsonToken.NULL) {
                        json.nextNull()
                    } else {
                        duration = json.nextLong() * 1000L
                    }
                }
                else -> json.skipValue()
            }
        }
        json.endObject()

        if (zoneName.isBlank() || arrivedAt == -1L) {
            return null // Invalid visit
        }

        return VisitEntity(
            geofenceId = null, // Disconnected from active tracking geofences to avoid corrupting OS state
            geofenceName = zoneName.trim(),
            entryTime = arrivedAt,
            exitTime = leftAt,
            durationMillis = duration
        )
    }
}
