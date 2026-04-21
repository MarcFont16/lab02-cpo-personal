package it.tonicminds.lab02cpo

interface LogService {
    /**
     * Creates a runtime snapshot, persists it to the database, and returns it.
     * [requestId] and [correlationId] are already resolved by the caller (filter → controller).
     */
    fun createSnapshot(requestId: String, correlationId: String): LogResponse
}
