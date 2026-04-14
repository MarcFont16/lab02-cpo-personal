package it.tonicminds.lab02cpo

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Log", description = "Runtime snapshot endpoint")
class LogController(private val logService: LogService) {

    @GetMapping("/log")
    @Operation(
        summary = "Create a runtime snapshot",
        description = "Persists and returns a snapshot of the current pod identity. " +
                "X-Request-Id and X-Correlation-Id are resolved by TraceFilter; generated if absent."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Snapshot created and persisted",
        content = [Content(schema = Schema(implementation = LogResponse::class))]
    )
    fun log(
        @Parameter(hidden = true) request: HttpServletRequest,
        @Parameter(description = "Unique identifier for this HTTP request. Generated if absent.")
        @RequestHeader(TraceFilter.REQUEST_ID_HEADER, required = false) requestIdHeader: String?,
        @Parameter(description = "Identifier shared across related requests. Generated if absent.")
        @RequestHeader(TraceFilter.CORRELATION_ID_HEADER, required = false) correlationIdHeader: String?
    ): LogResponse {
        val requestId = request.getAttribute(TraceFilter.REQUEST_ID_ATTR) as String
        val correlationId = request.getAttribute(TraceFilter.CORRELATION_ID_ATTR) as String
        return logService.createSnapshot(requestId, correlationId)
    }
}

data class LogResponse(
    val appVersion: String,
    val podName: String,
    val timestamp: java.time.Instant,
    val requestId: String,
    val correlationId: String
)
