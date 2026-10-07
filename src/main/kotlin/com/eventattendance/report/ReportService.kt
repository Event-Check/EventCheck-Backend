package com.eventattendance.report

import com.eventattendance.registration.RegistrationEntity
import com.eventattendance.registration.RegistrationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ReportService(
    private val repository: RegistrationRepository,
    private val excelGenerator: ExcelReportGenerator,
    private val pdfGenerator: PdfReportGenerator,
    private val timeFormatter: ReportTimeFormatter,
    @Value("\${app.report.event-name:Event}") private val eventName: String
) {

    @Transactional(readOnly = true)
    fun getSummary(): ReportSummary = summarize(repository.findAll())

    @Transactional(readOnly = true)
    fun buildReport(includeAttendees: Boolean): ReportData {
        // One snapshot of the data so summary numbers and the attendee list always agree.
        val all = repository.findAll()

        val attendees = if (includeAttendees) {
            all.sortedBy { it.name.lowercase() }.map { it.toRow() }
        } else {
            null
        }

        return ReportData(
            eventName = eventName,
            generatedAt = Instant.now(),
            summary = summarize(all),
            attendees = attendees
        )
    }

    fun export(format: ReportFormat, includeAttendees: Boolean): ExportedReport {
        val report = buildReport(includeAttendees)

        val bytes = when (format) {
            ReportFormat.PDF -> pdfGenerator.generate(report)
            ReportFormat.EXCEL -> excelGenerator.generate(report)
        }

        return ExportedReport(
            bytes = bytes,
            fileName = "event-attendance-report-${timeFormatter.fileStamp(report.generatedAt)}.${format.extension}",
            mediaType = format.mediaType
        )
    }

    private fun summarize(all: List<RegistrationEntity>): ReportSummary {
        val registered = all.size.toLong()
        val verified = all.count { it.emailVerified }.toLong()
        val checkedIn = all.count { it.checkedIn }.toLong()

        val rate = if (verified == 0L) 0.0 else Math.round(checkedIn * 1000.0 / verified) / 10.0

        return ReportSummary(
            registered = registered,
            verified = verified,
            checkedIn = checkedIn,
            noShows = registered - checkedIn,
            attendanceRate = rate
        )
    }

    private fun RegistrationEntity.toRow() = AttendeeRow(
        name = name,
        email = email,
        verified = emailVerified,
        status = if (checkedIn) AttendeeStatus.CHECKED_IN else AttendeeStatus.NO_SHOW,
        checkedInAt = checkedInAt
    )
}
