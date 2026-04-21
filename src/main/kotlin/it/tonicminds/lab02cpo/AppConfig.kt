package it.tonicminds.lab02cpo

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppConfig(
    val version: String = "0.0.0",
    val podName: String = "unknown",
    val podLogIntervalMs: Long = 30_000
)
