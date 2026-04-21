package it.tonicminds.lab02cpo

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration counterpart of LogServiceTest.
 * Same scenarios, no mocking — LogRepository hits the real H2 database.
 */
@SpringBootTest
class LogServiceIT {

    @Autowired private lateinit var logService: LogService
    @Autowired private lateinit var logRepository: LogRepository
    @Autowired private lateinit var appConfig: AppConfig

    @BeforeEach
    fun clearDb() {
        logRepository.deleteAll()
    }

    @Test
    fun `createSnapshot returns response with config values`() {
        val response = logService.createSnapshot("req-1", "corr-1")

        assertEquals(appConfig.version, response.appVersion)
        assertEquals(appConfig.podName, response.podName)
        assertEquals("req-1", response.requestId)
        assertEquals("corr-1", response.correlationId)
        assertNotNull(response.timestamp)
    }

    @Test
    fun `createSnapshot persists entry with correct field values`() {
        logService.createSnapshot("req-abc", "corr-xyz")

        val entries = logRepository.findAll()
        assertEquals(1, entries.size)
        with(entries.first()) {
            assertEquals(appConfig.version, appVersion)
            assertEquals(appConfig.podName, podName)
            assertEquals("req-abc", requestId)
            assertEquals("corr-xyz", correlationId)
            assertNotNull(timestamp)
        }
    }

    @Test
    fun `createSnapshot propagates requestId and correlationId unchanged`() {
        val response = logService.createSnapshot("unique-req", "shared-corr")

        assertEquals("unique-req", response.requestId)
        assertEquals("shared-corr", response.correlationId)
    }
}
