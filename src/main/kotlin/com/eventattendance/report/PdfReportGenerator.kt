package com.eventattendance.report

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.springframework.stereotype.Component
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.util.Locale

@Component
class PdfReportGenerator(
    private val time: ReportTimeFormatter
) {
    private val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
    private val sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f)
    private val normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10f)
    private val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)
    private val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color.WHITE)
    private val cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9f)

    private val headerBackground = Color(0x1F, 0x3A, 0x6E)
    private val labelBackground = Color(0xEE, 0xF1, 0xF7)

    fun generate(report: ReportData): ByteArray {
        val out = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(document, out)

        document.open()

        document.add(Paragraph("${report.eventName} - Attendance Report", titleFont))
        document.add(
            Paragraph(
                "Generated at: ${time.format(report.generatedAt)} (${time.zoneLabel})",
                normalFont
            ).apply { spacingAfter = 14f }
        )

        document.add(buildSummaryTable(report.summary))

        report.attendees?.let { attendees ->
            document.add(
                Paragraph("Attendees (${attendees.size})", sectionFont).apply {
                    spacingBefore = 20f
                    spacingAfter = 8f
                }
            )

            if (attendees.isEmpty()) {
                document.add(Paragraph("No registrations yet.", normalFont))
            } else {
                document.add(buildAttendeesTable(attendees))
            }
        }

        document.close()
        return out.toByteArray()
    }

    private fun buildSummaryTable(summary: ReportSummary): PdfPTable {
        val table = PdfPTable(2).apply {
            widthPercentage = 55f
            horizontalAlignment = Element.ALIGN_LEFT
            setWidths(floatArrayOf(3f, 2f))
        }

        val rows = listOf(
            "Registered" to summary.registered.toString(),
            "Verified" to summary.verified.toString(),
            "Checked In" to summary.checkedIn.toString(),
            "No Shows" to summary.noShows.toString(),
            "Attendance Rate" to String.format(Locale.US, "%.1f%%", summary.attendanceRate)
        )

        rows.forEach { (label, value) ->
            table.addCell(cell(label, boldFont, Element.ALIGN_LEFT, labelBackground, 7f))
            table.addCell(cell(value, normalFont, Element.ALIGN_RIGHT, null, 7f))
        }

        return table
    }

    private fun buildAttendeesTable(attendees: List<AttendeeRow>): PdfPTable {
        val table = PdfPTable(5).apply {
            widthPercentage = 100f
            setWidths(floatArrayOf(3f, 4.2f, 1.5f, 2f, 3.2f))
            headerRows = 1
        }

        listOf("Name", "Email", "Verified", "Status", "Check-in Time (${time.zoneLabel})").forEach {
            table.addCell(cell(it, headerFont, Element.ALIGN_LEFT, headerBackground, 5f))
        }

        attendees.forEach { a ->
            table.addCell(cell(a.name, cellFont, Element.ALIGN_LEFT, null, 4f))
            table.addCell(cell(a.email, cellFont, Element.ALIGN_LEFT, null, 4f))
            table.addCell(cell(if (a.verified) "Yes" else "No", cellFont, Element.ALIGN_LEFT, null, 4f))
            table.addCell(cell(a.status.label, cellFont, Element.ALIGN_LEFT, null, 4f))
            table.addCell(cell(time.format(a.checkedInAt), cellFont, Element.ALIGN_LEFT, null, 4f))
        }

        return table
    }

    private fun cell(
        text: String,
        font: com.lowagie.text.Font,
        align: Int,
        background: Color?,
        padding: Float
    ): PdfPCell = PdfPCell(Phrase(text, font)).apply {
        horizontalAlignment = align
        this.setPadding(padding)
        borderColor = Color(0xC8, 0xCD, 0xD6)
        background?.let { backgroundColor = it }
    }
}
