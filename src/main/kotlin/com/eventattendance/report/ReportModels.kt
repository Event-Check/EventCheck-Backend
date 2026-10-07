package com.eventattendance.report

import org.springframework.http.MediaType
import java.time.Instant

enum class AttendeeStatus(val label: String) {
    CHECKED_IN("Checked In"),
    NO_SHOW("No Show")
}

/**
 * Dashboard numbers.
 * noShows = registered - checkedIn
 * attendanceRate = checkedIn / verified * 100 (one decimal)
 */
data class ReportSummary(
    val registered: Long,
    val verified: Long,
    val checkedIn: Long,
    val noShows: Long,
    val attendanceRate: Double
)

data class AttendeeRow(
    val name: String,
    val email: String,
    val verified: Boolean,
    val status: AttendeeStatus,
    val checkedInAt: Instant?
)

data class ReportData(
    val eventName: String,
    val generatedAt: Instant,
    val summary: ReportSummary,
    /** null when the report was requested without attendees. */
    val attendees: List<AttendeeRow>?
)

enum class ReportFormat(val extension: String, val mediaType: MediaType) {
    PDF("pdf", MediaType.APPLICATION_PDF),
    EXCEL(
        "xlsx",
        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    );

    companion object {
        fun parse(value: String): ReportFormat? = when (value.trim().lowercase()) {
            "pdf" -> PDF
            "excel", "xlsx" -> EXCEL
            else -> null
        }
    }
}

class ExportedReport(
    val bytes: ByteArray,
    val fileName: String,
    val mediaType: MediaType
)
