package com.eventattendance.registration

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RegistrationRepository : JpaRepository<RegistrationEntity, UUID> {
    fun findByEmailIgnoreCase(email: String): RegistrationEntity?
    fun findByQrToken(qrToken: String): RegistrationEntity?
}
