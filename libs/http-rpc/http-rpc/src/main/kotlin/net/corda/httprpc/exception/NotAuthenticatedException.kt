package net.corda.httprpc.exception

import net.corda.httprpc.response.ResponseCode

/**
 * User authentication has failed.
 */
class NotAuthenticatedException(message: String = "User authentication failed.", details: Map<String, String> = emptyMap()) :
    HttpApiException(
        ResponseCode.NOT_AUTHENTICATED,
        message,
        details
    )