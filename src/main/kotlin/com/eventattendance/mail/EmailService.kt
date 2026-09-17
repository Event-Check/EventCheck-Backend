package com.eventattendance.mail

import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.mail.from}") private val from: String
) {
    fun sendVerificationEmail(to: String, name: String, code: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, false, "UTF-8")

        helper.setFrom(from)
        helper.setTo(to)
        helper.setSubject("Verify your event registration")

        helper.setText(
            """
            <html>
              <body>
                <h2>Verify your registration</h2>
                <p>Hello ${escape(name)},</p>
                <p>Your verification code is:</p>
                <h1 style="letter-spacing: 6px;">$code</h1>
                <p>This code expires in 10 minutes.</p>
              </body>
            </html>
            """.trimIndent(),
            true
        )

        mailSender.send(message)
    }

    fun sendQrEmail(to: String, name: String, qrPng: ByteArray) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")

        helper.setFrom(from)
        helper.setTo(to)
        helper.setSubject("Your event QR Code")

        helper.setText(
            """
            <html>
              <body>
                <h2>Your event QR Code</h2>
                <p>Hello ${escape(name)},</p>
                <p>Your email has been verified successfully.</p>
                <p>Please keep the attached QR Code and show it at the event entrance.</p>
              </body>
            </html>
            """.trimIndent(),
            true
        )

        helper.addAttachment("event-qr-code.png", ByteArrayResource(qrPng), "image/png")
        mailSender.send(message)
    }

    private fun escape(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
}
