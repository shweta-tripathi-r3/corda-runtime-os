package net.corda.httprpc.exception

import net.corda.httprpc.response.ResponseCode

/**
 * Indicates a requested resource does not exist.
 *
 * @param title title of the exception response. Keep this brief by including extra information in the [details] section.
 */
class ResourceNotFoundException(title: String) : HttpApiException(ResponseCode.RESOURCE_NOT_FOUND, title) {
    /**
     * @param resource The resource which could not be found.
     * @param id The ID of the resource.
     */
    constructor(resource: Any, id: String) : this("$resource '$id' not found.")
}