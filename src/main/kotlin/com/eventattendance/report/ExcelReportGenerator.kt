package com.eventattendance.report

import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class ExcelReportGenerator(
    private val time: ReportTimeFormatter
) {
    fun generate(report: ReportData): ByteArray =
        XSSFWorkbook().use { workbook ->
            val styles = Styles(workbook)

            writeSummarySheet(workbook, styles, report)
            report.attendees?.let { writeAttendeesSheet(workbook, styles, it) }

            ByteArrayOutputStream().use { out ->
                workbook.write(out)
                out.toByteArray()
            }
        }

    private fun writeSummarySheet(workbook: Workbook, styles: Styles, report: ReportData) {
        val sheet = workbook.createSheet("Summary")
        val s = report.summary

        sheet.createRow(0).createCell(0).apply {
            setCellValue("${report.eventName} - Attendance Report")
            cellStyle = styles.title
        }
        sheet.createRow(1).createCell(0)
            .setCellValue("Generated at: ${time.format(report.generatedAt)} (${time.zoneLabel})")

        val header = sheet.createRow(3)
        header.createCell(0).apply { setCellValue("Metric"); cellStyle = styles.header }
        header.createCell(1).apply { setCellValue("Value"); cellStyle = styles.header }

        val rows = listOf(
            "Registered" to s.registered,
            "Verified" to s.verified,
            "Checked In" to s.checkedIn,
            "No Shows" to s.noShows
        )

        rows.forEachIndexed { i, (label, value) ->
            val row = sheet.createRow(4 + i)
            row.createCell(0).apply { setCellValue(label); cellStyle = styles.label }
            row.createCell(1).apply { setCellValue(value.toDouble()); cellStyle = styles.integer }
        }

        val rateRow = sheet.createRow(4 + rows.size)
        rateRow.createCell(0).apply { setCellValue("Attendance Rate"); cellStyle = styles.label }
        rateRow.createCell(1).apply {
            setCellValue(s.attendanceRate / 100.0)
            cellStyle = styles.percent
        }

        sheet.setColumnWidth(0, 28 * 256)
        sheet.setColumnWidth(1, 16 * 256)
    }

    private fun writeAttendeesSheet(workbook: Workbook, styles: Styles, attendees: List<AttendeeRow>) {
        val sheet: Sheet = workbook.createSheet("Attendees")

        val headers = listOf("Name", "Email", "Verified", "Status", "Check-in Time (${time.zoneLabel})")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, text ->
            headerRow.createCell(i).apply { setCellValue(text); cellStyle = styles.header }
        }

        attendees.forEachIndexed { i, a ->
            val row = sheet.createRow(i + 1)
            row.createCell(0).setCellValue(a.name)
            row.createCell(1).setCellValue(a.email)
            row.createCell(2).setCellValue(if (a.verified) "Yes" else "No")
            row.createCell(3).setCellValue(a.status.label)
            row.createCell(4).setCellValue(time.format(a.checkedInAt))
        }

        // Fixed widths: autoSizeColumn needs AWT fonts, which slim server images often lack.
        sheet.setColumnWidth(0, 30 * 256)
        sheet.setColumnWidth(1, 38 * 256)
        sheet.setColumnWidth(2, 11 * 256)
        sheet.setColumnWidth(3, 14 * 256)
        sheet.setColumnWidth(4, 28 * 256)

        sheet.createFreezePane(0, 1)
        sheet.setAutoFilter(CellRangeAddress(0, maxOf(attendees.size, 1), 0, headers.lastIndex))
    }

    private class Styles(workbook: Workbook) {
        val title: CellStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 16
            })
        }

        val header: CellStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            })
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
        }

        val label: CellStyle = workbook.createCellStyle().apply {
            setFont(workbook.createFont().apply { bold = true })
        }

        val integer: CellStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("0")
            alignment = HorizontalAlignment.RIGHT
        }

        val percent: CellStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("0.0%")
            alignment = HorizontalAlignment.RIGHT
        }
    }
}
