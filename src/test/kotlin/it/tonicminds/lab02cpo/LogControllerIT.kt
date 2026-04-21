package it.tonicminds.lab02cpo

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.assertEquals

/**
 * Integration counterpart of LogControllerTest.
 * Same scenarios, no mocking — full filter chain + real LogService + H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LogControllerIT {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var logRepository: LogRepository

    @BeforeEach
    fun clearDb() {
        logRepository.deleteAll()
    }

    @Test
    fun `GET log returns 200`() {
        mockMvc.perform(get("/log"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET log with explicit headers echoes them in response`() {
        mockMvc.perform(
            get("/log")
                .header("X-Request-Id", "my-req")
                .header("X-Correlation-Id", "my-corr")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requestId").value("my-req"))
            .andExpect(jsonPath("$.correlationId").value("my-corr"))
            .andExpect(jsonPath("$.appVersion").isNotEmpty)
            .andExpect(jsonPath("$.podName").isNotEmpty)
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `GET log without headers generates non-blank IDs`() {
        mockMvc.perform(get("/log"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requestId").isNotEmpty)
            .andExpect(jsonPath("$.correlationId").isNotEmpty)
    }

    @Test
    fun `GET log persists exactly one entry to the database`() {
        mockMvc.perform(get("/log"))
            .andExpect(status().isOk)

        assertEquals(1, logRepository.count())
    }

    @Test
    fun `GET log response IDs match what was persisted`() {
        mockMvc.perform(
            get("/log")
                .header("X-Request-Id", "trace-req")
                .header("X-Correlation-Id", "trace-corr")
        )
            .andExpect(status().isOk)

        val entry = logRepository.findAll().first()
        assertEquals("trace-req", entry.requestId)
        assertEquals("trace-corr", entry.correlationId)
    }
}
