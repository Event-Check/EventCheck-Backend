package com.eventattendance.registration

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 120, message = "Name must not exceed 120 characters")
    val name: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Please provide a valid email address")
    @field:Size(max = 320, message = "Email must not exceed 320 characters")
    val email: String
)

data class VerifyEmailRequest(
    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 6, max = 6)
    val code: String
)

data class ResendVerificationRequest(
    @field:NotBlank
    @field:Email
    val email: String
)

data class RegistrationResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val message: String
)

data class VerificationResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val qrToken: String,
    val message: String
)

data class CheckInRequest(
    @field:NotBlank
    val qrToken: String
)

data class CheckInResponse(
    val registrationId: UUID?,
    val name: String?,
    val email: String?,
    val checkedIn: Boolean,
    val checkedInAt: Instant?,
    val status: String,
    val message: String
)

data class StatsResponse(
    val totalRegistrations: Long,
    val verifiedRegistrations: Long,
    val checkedIn: Long,
    val notCheckedIn: Long
)

data class ApiError(
    val message: String
)
