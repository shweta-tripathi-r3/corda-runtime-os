package net.corda.httprpc.exception

import net.corda.httprpc.response.ResponseCode

/**
 * Indicates a requested resource already exists within the system
 *
 * @param title title of the exception response. Keep this brief by including extra information in the [details] section.
 */
class ResourceAlreadyExistsException(title: String) : HttpApiException(ResponseCode.CONFLICT, title) {
    /**
     * @param resource The resource which already exists
     * @param id The ID of the resource.
     */
    constructor(resource: Any, id: String) : this("$resource '$id' already exists.")
}
