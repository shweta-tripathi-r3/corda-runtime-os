package net.corda.httprpc.response

/**
 * Class allowing control of response codes for HTTP responses.
 */
class HttpResponse<T : Any>(
    val responseCode: ResponseCode = ResponseCode.OK,
    val responseBody: T?,
)
