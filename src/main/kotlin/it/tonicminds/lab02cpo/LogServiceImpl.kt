package it.tonicminds.lab02cpo

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class LogServiceImpl(
    private val appConfig: AppConfig,
    private val logRepository: LogRepository
) : LogService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun createSnapshot(requestId: String, correlationId: String): LogResponse {
        val now = Instant.now()

        logRepository.save(
            LogEntry(
                appVersion = appConfig.version,
                podName = appConfig.podName,
                timestamp = now,
                requestId = requestId,
                correlationId = correlationId
            )
        )

        log.info("Snapshot created — version={} pod={} requestId={} correlationId={}",
            appConfig.version, appConfig.podName, requestId, correlationId)

        return LogResponse(
            appVersion = appConfig.version,
            podName = appConfig.podName,
            timestamp = now,
            requestId = requestId,
            correlationId = correlationId
        )
    }
}
