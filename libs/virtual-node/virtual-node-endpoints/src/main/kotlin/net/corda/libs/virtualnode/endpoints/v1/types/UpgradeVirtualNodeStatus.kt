package net.corda.libs.virtualnode.endpoints.v1.types

import java.time.Instant
import net.corda.httprpc.response.AsyncOperationError
import net.corda.httprpc.response.AsyncOperationStatus
import net.corda.httprpc.response.AsyncResponseStatus

class UpgradeVirtualNodeStatus(
    val virtualNodeShortHash: String,
    val cpiFileChecksum: String,
    val stage: String?,
    startTime: Instant,
    endTime: Instant?,
    status: AsyncOperationStatus,
    errors: List<AsyncOperationError>?
) : AsyncResponseStatus(startTime, endTime, status, errors)