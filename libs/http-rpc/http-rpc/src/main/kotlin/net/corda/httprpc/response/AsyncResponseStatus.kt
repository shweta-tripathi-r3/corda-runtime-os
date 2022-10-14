package net.corda.httprpc.response

import java.time.Instant

open class AsyncResponseStatus(
    val requestTime: Instant?,
    val completionTime: Instant?,
    val status: AsyncOperationStatus,
    val errors: List<AsyncOperationError>?
)