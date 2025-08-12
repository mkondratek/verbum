package com.kondratek.verbum.demo

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthCheckController {
    @GetMapping("/health")
    fun health(): Map<String, String> = mapOf("status" to "UP")
}
