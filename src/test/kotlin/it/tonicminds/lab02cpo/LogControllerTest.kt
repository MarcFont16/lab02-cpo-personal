package it.tonicminds.lab02cpo

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(LogController::class)
class LogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var logService: LogService

    private fun stubService(requestId: String, correlationId: String) =
        LogResponse(
            appVersion = "0.1.0",
            podName = "test-pod",
            timestamp = Instant.parse("2026-04-07T10:00:00Z"),
            requestId = requestId,
            correlationId = correlationId
        )

    @Test
    fun `GET log returns 200`() {
        whenever(logService.createSnapshot(any(), any())).thenAnswer { inv ->
            stubService(inv.arguments[0] as String, inv.arguments[1] as String)
        }

        mockMvc.perform(get("/log"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET log with explicit headers echoes them in response`() {
        whenever(logService.createSnapshot("my-req", "my-corr"))
            .thenReturn(stubService("my-req", "my-corr"))

        mockMvc.perform(
            get("/log")
                .header("X-Request-Id", "my-req")
                .header("X-Correlation-Id", "my-corr")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requestId").value("my-req"))
            .andExpect(jsonPath("$.correlationId").value("my-corr"))
            .andExpect(jsonPath("$.appVersion").value("0.1.0"))
            .andExpect(jsonPath("$.podName").value("test-pod"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `GET log without headers generates non-blank IDs`() {
        whenever(logService.createSnapshot(any(), any())).thenAnswer { inv ->
            stubService(inv.arguments[0] as String, inv.arguments[1] as String)
        }

        mockMvc.perform(get("/log"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requestId").isNotEmpty)
            .andExpect(jsonPath("$.correlationId").isNotEmpty)
    }

    @Test
    fun `GET log delegates to LogService exactly once`() {
        whenever(logService.createSnapshot(any(), any())).thenAnswer { inv ->
            stubService(inv.arguments[0] as String, inv.arguments[1] as String)
        }

        mockMvc.perform(get("/log"))

        verify(logService).createSnapshot(any(), any())
    }
}
