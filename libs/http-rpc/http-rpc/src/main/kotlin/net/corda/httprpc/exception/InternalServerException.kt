package net.corda.httprpc.exception

import net.corda.httprpc.response.ResponseCode

/**
 * The server encountered an internal error which prevented it from fulfilling the request.
 *
 * @param title title of the exception response. Keep this brief by including extra information in the [details] section.
 * @param details optionally include any additional details that may be useful.
 * @param exceptionDetails optionally include details of the exception, the `cause` and `reason` will be output in details section.
 */
class InternalServerException(
    title: String = "Internal server error.",
    details: Map<String, String> = emptyMap(),
    exceptionDetails: ExceptionDetails? = null
) : HttpApiException(
    ResponseCode.INTERNAL_SERVER_ERROR,
    title,
    details,
    exceptionDetails
)