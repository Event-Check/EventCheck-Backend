package com.eventattendance.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class QrCodeService {
    fun generatePng(content: String, width: Int = 700, height: Int = 700): ByteArray {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val matrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            width,
            height,
            hints
        )

        return ByteArrayOutputStream().use { output ->
            MatrixToImageWriter.writeToStream(matrix, "PNG", output)
            output.toByteArray()
        }
    }
}
