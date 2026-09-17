package com.eventattendance.registration

import com.eventattendance.mail.EmailService
import com.eventattendance.qr.QrCodeService
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class RegistrationService(
    private val repository: RegistrationRepository,
    private val emailService: EmailService,
    private val qrCodeService: QrCodeService,
    @Value("\${app.verification.code-expiration-minutes:10}")
    private val verificationExpirationMinutes: Long
) {
    private val random = SecureRandom()

    @Transactional
    fun register(request: RegisterRequest): RegistrationResponse {
        val email = request.email.trim().lowercase()
        val name = request.name.trim()

        val existing = repository.findByEmailIgnoreCase(email)

        if (existing != null) {
            if (existing.emailVerified) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This email is already registered."
                )
            }

            sendNewVerification(existing)
            return RegistrationResponse(
                id = existing.id!!,
                name = existing.name,
                email = existing.email,
                emailVerified = false,
                message = "A new verification code has been sent."
            )
        }

        val entity = RegistrationEntity(
            name = name,
            email = email
        )

        sendNewVerification(entity)
        val saved = try {
            repository.saveAndFlush(entity)
        } catch (e: DataIntegrityViolationException) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "This email is already registered."
            )
        }

        return RegistrationResponse(
            id = saved.id!!,
            name = saved.name,
            email = saved.email,
            emailVerified = false,
            message = "Verification code sent to your email."
        )
    }

    @Transactional
    fun verify(request: VerifyEmailRequest): VerificationResponse {
        val email = request.email.trim().lowercase()
        val registration = repository.findByEmailIgnoreCase(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found.")

        if (registration.emailVerified && registration.qrToken != null) {
            return buildVerificationResponse(
                registration,
                "Email was already verified."
            )
        }

        val code = registration.verificationCode
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No verification code exists.")

        val expiresAt = registration.verificationExpiresAt
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code has expired.")

        if (Instant.now().isAfter(expiresAt)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code has expired.")
        }

        if (code != request.code.trim()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code.")
        }

        registration.emailVerified = true
        registration.verificationCode = null
        registration.verificationExpiresAt = null
        registration.qrToken = generateUniqueQrToken()

        val saved = repository.save(registration)
        val qrContent = saved.qrToken!!
        val qrPng = qrCodeService.generatePng(qrContent)

        emailService.sendQrEmail(
            to = saved.email,
            name = saved.name,
            qrPng = qrPng
        )

        return buildVerificationResponse(saved, "Email verified and QR Code sent.")
    }

    @Transactional
    fun resendVerification(request: ResendVerificationRequest): RegistrationResponse {
        val email = request.email.trim().lowercase()
        val registration = repository.findByEmailIgnoreCase(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found.")

        if (registration.emailVerified) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email is already verified."
            )
        }

        sendNewVerification(registration)
        repository.save(registration)

        return RegistrationResponse(
            id = registration.id!!,
            name = registration.name,
            email = registration.email,
            emailVerified = false,
            message = "A new verification code has been sent."
        )
    }

    @Transactional
    fun checkIn(qrToken: String): CheckInResponse {
        val registration = repository.findByQrToken(qrToken.trim())
            ?: return CheckInResponse(
                registrationId = null,
                name = null,
                email = null,
                checkedIn = false,
                checkedInAt = null,
                status = "INVALID_QR",
                message = "Invalid QR Code."
            )

        if (!registration.emailVerified) {
            return CheckInResponse(
                registrationId = registration.id,
                name = registration.name,
                email = registration.email,
                checkedIn = false,
                checkedInAt = null,
                status = "NOT_VERIFIED",
                message = "This registration is not verified."
            )
        }

        if (registration.checkedIn) {
            return CheckInResponse(
                registrationId = registration.id,
                name = registration.name,
                email = registration.email,
                checkedIn = true,
                checkedInAt = registration.checkedInAt,
                status = "ALREADY_CHECKED_IN",
                message = "This attendee has already checked in."
            )
        }

        registration.checkedIn = true
        registration.checkedInAt = Instant.now()
        repository.save(registration)

        return CheckInResponse(
            registrationId = registration.id,
            name = registration.name,
            email = registration.email,
            checkedIn = true,
            checkedInAt = registration.checkedInAt,
            status = "CHECKED_IN",
            message = "Check-in successful."
        )
    }

    @Transactional(readOnly = true)
    fun getStats(): StatsResponse {
        val all = repository.findAll()
        val verified = all.count { it.emailVerified }
        val checkedIn = all.count { it.checkedIn }

        return StatsResponse(
            totalRegistrations = all.size.toLong(),
            verifiedRegistrations = verified.toLong(),
            checkedIn = checkedIn.toLong(),
            notCheckedIn = (all.size - checkedIn).toLong()
        )
    }

    private fun sendNewVerification(registration: RegistrationEntity) {
        val code = generateVerificationCode()
        registration.verificationCode = code
        registration.verificationExpiresAt =
            Instant.now().plus(Duration.ofMinutes(verificationExpirationMinutes))

        emailService.sendVerificationEmail(
            to = registration.email,
            name = registration.name,
            code = code
        )
    }

    private fun generateVerificationCode(): String =
        (100000 + random.nextInt(900000)).toString()

    private fun generateUniqueQrToken(): String {
        repeat(10) {
            val token = "EVENT-" + UUID.randomUUID().toString()
                .replace("-", "")
                .uppercase()

            if (repository.findByQrToken(token) == null) {
                return token
            }
        }

        throw IllegalStateException("Could not generate a unique QR token.")
    }

    private fun buildVerificationResponse(
        registration: RegistrationEntity,
        message: String
    ) = VerificationResponse(
        id = registration.id!!,
        name = registration.name,
        email = registration.email,
        emailVerified = registration.emailVerified,
        qrToken = registration.qrToken!!,
        message = message
    )
}
