package com.eventattendance.report

import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/admin/report")
class ReportController(
    private val service: ReportService
) {
    /** Dashboard numbers as JSON (Registered / Verified / Checked In / No Shows / Attendance Rate). */
    @GetMapping("/summary")
    fun summary(): ResponseEntity<ReportSummary> =
        ResponseEntity.ok(service.getSummary())

    /** Download the report as PDF or Excel. */
    @GetMapping("/export")
    fun export(
        @RequestParam("format") format: String,
        @RequestParam("includeAttendees", defaultValue = "true") includeAttendees: Boolean
    ): ResponseEntity<ByteArray> {
        val reportFormat = ReportFormat.parse(format)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported format. Use 'pdf' or 'excel'."
            )

        val report = service.export(reportFormat, includeAttendees)

        return ResponseEntity.ok()
            .contentType(report.mediaType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(report.fileName).build().toString()
            )
            .body(report.bytes)
    }
}
