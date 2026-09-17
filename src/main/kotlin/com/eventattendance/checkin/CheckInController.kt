package com.eventattendance.checkin

import com.eventattendance.registration.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1")
class CheckInController(
    private val service: RegistrationService
) {
    @PostMapping("/check-in")
    fun checkIn(
        @Valid @RequestBody request: CheckInRequest
    ): ResponseEntity<CheckInResponse> =
        ResponseEntity.ok(service.checkIn(request.qrToken))

    @GetMapping("/admin/stats")
    fun stats(): ResponseEntity<StatsResponse> =
        ResponseEntity.ok(service.getStats())
}
