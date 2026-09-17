package com.eventattendance.registration

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "registrations",
    indexes = [
        Index(name = "idx_registration_email", columnList = "email", unique = true),
        Index(name = "idx_registration_qr_token", columnList = "qr_token", unique = true)
    ]
)
class RegistrationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(nullable = false, length = 320, unique = true)
    var email: String,

    @Column(name = "verification_code", length = 6)
    var verificationCode: String? = null,

    @Column(name = "verification_expires_at")
    var verificationExpiresAt: Instant? = null,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Column(name = "qr_token", unique = true)
    var qrToken: String? = null,

    @Column(name = "checked_in", nullable = false)
    var checkedIn: Boolean = false,

    @Column(name = "checked_in_at")
    var checkedInAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
