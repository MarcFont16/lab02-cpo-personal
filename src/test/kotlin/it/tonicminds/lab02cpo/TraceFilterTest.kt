package it.tonicminds.lab02cpo

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TraceFilterTest {

    private val filter = TraceFilter()

    @Test
    fun `reads request ID from header when present`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-Request-Id", "header-req-id")
            addHeader("X-Correlation-Id", "header-corr-id")
        }

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals("header-req-id", request.getAttribute(TraceFilter.REQUEST_ID_ATTR))
        assertEquals("header-corr-id", request.getAttribute(TraceFilter.CORRELATION_ID_ATTR))
    }

    @Test
    fun `generates request ID when header is absent`() {
        val request = MockHttpServletRequest()

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertNotNull(request.getAttribute(TraceFilter.REQUEST_ID_ATTR))
        assertNotNull(request.getAttribute(TraceFilter.CORRELATION_ID_ATTR))
    }

    @Test
    fun `generated IDs are different across two requests`() {
        val req1 = MockHttpServletRequest()
        val req2 = MockHttpServletRequest()

        filter.doFilter(req1, MockHttpServletResponse(), MockFilterChain())
        filter.doFilter(req2, MockHttpServletResponse(), MockFilterChain())

        val id1 = req1.getAttribute(TraceFilter.REQUEST_ID_ATTR) as String
        val id2 = req2.getAttribute(TraceFilter.REQUEST_ID_ATTR) as String
        assert(id1 != id2) { "Each request should get a unique requestId" }
    }

    @Test
    fun `blank header value triggers ID generation`() {
        val request = MockHttpServletRequest().apply {
            addHeader("X-Request-Id", "   ")
        }

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        val requestId = request.getAttribute(TraceFilter.REQUEST_ID_ATTR) as String
        assert(requestId.isNotBlank())
        assert(requestId != "   ")
    }
}
