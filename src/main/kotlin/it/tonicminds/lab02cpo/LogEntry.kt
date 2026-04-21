package it.tonicminds.lab02cpo

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "log_entries")
class LogEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val appVersion: String,
    val podName: String,
    val timestamp: Instant,
    val requestId: String,
    val correlationId: String
)
