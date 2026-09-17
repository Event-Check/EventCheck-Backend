package com.eventattendance.registration

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/registrations")
class RegistrationController(
    private val service: RegistrationService
) {
    @PostMapping
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<RegistrationResponse> =
        ResponseEntity.ok(service.register(request))

    @PostMapping("/verify")
    fun verify(
        @Valid @RequestBody request: VerifyEmailRequest
    ): ResponseEntity<VerificationResponse> =
        ResponseEntity.ok(service.verify(request))

    @PostMapping("/resend-verification")
    fun resendVerification(
        @Valid @RequestBody request: ResendVerificationRequest
    ): ResponseEntity<RegistrationResponse> =
        ResponseEntity.ok(service.resendVerification(request))
}
