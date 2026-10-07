package com.eventattendance.report

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class ReportTimeFormatter(
    @Value("\${app.report.timezone:UTC}") timezone: String
) {
    private val zone: ZoneId = ZoneId.of(timezone)
    private val dateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone)
    private val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(zone)

    val zoneLabel: String = "Egypt Time"

    fun format(instant: Instant?): String = instant?.let { dateTime.format(it) } ?: ""

    fun fileStamp(instant: Instant): String = stamp.format(instant)
}
