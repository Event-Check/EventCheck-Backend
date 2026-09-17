package com.eventattendance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EventAttendanceApplication

fun main(args: Array<String>) {
    runApplication<EventAttendanceApplication>(*args)
}
