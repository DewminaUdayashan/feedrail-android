package com.feedrail.sdk.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Client-side rate limiter: 100 requests per 60-second sliding window. */
class RateLimiter(
    private val maxRequests: Int = 100,
    private val windowMs: Long = 60_000L,
) {
    private val mutex = Mutex()
    private val timestamps = ArrayDeque<Long>()

    val remaining: Int get() = maxRequests - timestamps.size

    suspend fun canProceed(): Boolean = mutex.withLock {
        val now = System.currentTimeMillis()
        while (timestamps.isNotEmpty() && now - timestamps.first() > windowMs) {
            timestamps.removeFirst()
        }
        if (timestamps.size >= maxRequests) return@withLock false
        timestamps.addLast(now)
        true
    }
}
