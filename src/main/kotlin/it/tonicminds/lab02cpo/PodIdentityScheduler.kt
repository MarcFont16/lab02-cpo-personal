package it.tonicminds.lab02cpo

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PodIdentityScheduler(private val appConfig: AppConfig) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRateString = "\${app.pod-log-interval-ms:30000}")
    fun logPodIdentity() {
        log.info("Pod identity — name={} version={}", appConfig.podName, appConfig.version)
    }
}
