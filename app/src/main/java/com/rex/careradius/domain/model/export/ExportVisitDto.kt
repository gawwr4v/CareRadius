package com.rex.careradius.domain.model.export

/**
 * DTO customized specifically for JSON exporting
 * Decoupled from core VisitEntity to handle formatting (like ISO-8601) locally
 */
data class ExportVisitDto(
    val zoneName: String,
    val arrivedAt: String, // ISO-8601 string
    val leftAt: String?,   // ISO-8601 string, null if still active
    val durationSeconds: Long?, 
    val isActive: Boolean
)
