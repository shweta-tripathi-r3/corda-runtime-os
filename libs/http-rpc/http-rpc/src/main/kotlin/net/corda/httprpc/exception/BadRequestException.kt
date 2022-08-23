package net.corda.httprpc.exception

import net.corda.httprpc.response.ResponseCode

/**
 * Indicates the request was syntactically bad or contained invalid input data and the request could not be serviced.
 *
 * @param title title of the exception response. Keep this brief by including extra information in the [details] section.
 * @param details optionally include any additional details that may be useful.
 * @param exceptionDetails optionally include details of the exception, the `cause` and `reason` will be output in details section.
 */
class BadRequestException(title: String, details: Map<String, String> = emptyMap(), exceptionDetails: ExceptionDetails? = null) :
    HttpApiException(
        ResponseCode.BAD_REQUEST,
        title,
        details,
        exceptionDetails
    )