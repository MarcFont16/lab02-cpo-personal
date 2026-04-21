package it.tonicminds.lab02cpo

import org.springframework.data.jpa.repository.JpaRepository

interface LogRepository : JpaRepository<LogEntry, Long>
