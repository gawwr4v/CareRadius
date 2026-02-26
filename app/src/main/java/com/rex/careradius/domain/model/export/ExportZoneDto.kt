package com.rex.careradius.domain.model.export

/**
 * DTO customized specifically for JSON exporting
 * Deliberately isolates the export schema from internal Room Entity changes
 */
data class ExportZoneDto(
    val name: String,
    val emoji: String,
    val radiusMeters: Float,
    val latitude: Double,
    val longitude: Double,
    val createdAt: String, // ISO-8601 string
    val updatedAt: String? // ISO-8601 string (null if never updated)
)
