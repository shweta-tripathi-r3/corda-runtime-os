package net.corda.httprpc.exception

import net.corda.httprpc.response.ResponseCode

/**
 * Indicates a requested resource is unavailable.
 *
 * @param title the exception message
 */
class ServiceUnavailableException(title: String) : HttpApiException(ResponseCode.SERVICE_UNAVAILABLE, title) {
    /**
     * @param resource The resource which is unavailable.
     * @param id The ID of the resource.
     */
    constructor(resource: Any, id: String) : this("$resource '$id' is unavailable.")
}