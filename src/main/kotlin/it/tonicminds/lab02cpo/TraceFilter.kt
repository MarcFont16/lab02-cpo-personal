package it.tonicminds.lab02cpo

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(1)
class TraceFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
        const val REQUEST_ID_ATTR = "requestId"
        const val CORRELATION_ID_ATTR = "correlationId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val requestId = request.getHeader(REQUEST_ID_HEADER)
            ?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val correlationId = request.getHeader(CORRELATION_ID_HEADER)
            ?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

        request.setAttribute(REQUEST_ID_ATTR, requestId)
        request.setAttribute(CORRELATION_ID_ATTR, correlationId)

        MDC.put("requestId", requestId)
        MDC.put("correlationId", correlationId)

        try {
            log.info("Incoming {} {} requestId={} correlationId={}", request.method, request.requestURI, requestId, correlationId)
            chain.doFilter(request, response)
        } finally {
            MDC.remove("requestId")
            MDC.remove("correlationId")
        }
    }
}
