package net.corda.httprpc.exception

import net.corda.httprpc.response.ResponseCode

/**
 * The server validation of request data failed, the server could not complete the request because validation on the user's input failed.
 *
 * @param title title of the exception response. Keep this brief by including extra information in the [details] section.
 * @param details optionally include any additional details that may be useful.
 * @param exceptionDetails optionally include details of the exception, the `cause` and `reason` will be output in details section.
 */
class InvalidInputDataException(
    title: String = "Invalid input data.",
    details: Map<String, String> = emptyMap(),
    exceptionDetails: ExceptionDetails? = null
) : HttpApiException(
    ResponseCode.BAD_REQUEST,
    title,
    details,
    exceptionDetails
)