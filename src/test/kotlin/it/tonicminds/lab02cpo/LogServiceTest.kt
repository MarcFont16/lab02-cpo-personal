package it.tonicminds.lab02cpo

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class LogServiceTest {

    // Use a real AppConfig — no need to mock a plain data class
    private val appConfig = AppConfig(version = "0.1.0", podName = "test-pod")

    @Mock
    private lateinit var logRepository: LogRepository

    private lateinit var logService: LogService

    @BeforeEach
    fun setUp() {
        whenever(logRepository.save(any<LogEntry>())).thenAnswer { it.arguments[0] }
        logService = LogServiceImpl(appConfig, logRepository)
    }

    @Test
    fun `createSnapshot returns response with config values`() {
        val response = logService.createSnapshot("req-1", "corr-1")

        assertEquals("0.1.0", response.appVersion)
        assertEquals("test-pod", response.podName)
        assertEquals("req-1", response.requestId)
        assertEquals("corr-1", response.correlationId)
        assertNotNull(response.timestamp)
    }

    @Test
    fun `createSnapshot persists entry with correct field values`() {
        val captor = argumentCaptor<LogEntry>()

        logService.createSnapshot("req-abc", "corr-xyz")

        verify(logRepository).save(captor.capture())
        with(captor.firstValue) {
            assertEquals("0.1.0", appVersion)
            assertEquals("test-pod", podName)
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
