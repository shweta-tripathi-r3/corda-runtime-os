package net.corda.httprpc.exception

/**
 * Details of the exception that caused the request to fail.
 *
 * Included as `cause` and `reason` in the details section in the response payload.
 */
data class ExceptionDetails(val cause: String, val reason: String?)