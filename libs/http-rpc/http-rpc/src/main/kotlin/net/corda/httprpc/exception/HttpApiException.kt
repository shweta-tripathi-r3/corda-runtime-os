package net.corda.httprpc.exception

import net.corda.httprpc.response.ResponseCode
import net.corda.v5.base.exceptions.CordaRuntimeException

/**
 * Base class for HTTP exceptions.
 *
 * Inherit from this class and override the status code to create a HTTP response with a certain status code ([ResponseCode.statusCode]).
 *
 * An example HTTP response JSON from a well-constructed response error which contains :
 *
 * ```
 * {
 *   "title": "Configuration could not be updated.",
 *   "status": 500,
 *   "details": {
 *     "schemaVersion": "1.0",
 *     "config": "{\"context\": {\"description\": \"new3\"}}",
 *     "cause": "net.corda.configuration.write.WrongConfigVersionException",
 *     "reason": "New configuration represented by {\"section\": \"corda.rpc\", \"config\": \"{\\\"context\\\": {\\\"description\\\": \\\"new3\\\"}}\", \"schemaVersion\": {\"majorVersion\": 1, \"minorVersion\": 0}, \"updateActor\": \"admin\", \"version\": 3} couldn't be written to the database. Cause: net.corda.configuration.write.WrongConfigVersionException: The request specified a version of 3, but the current version in the database is 0. These versions must match to update the cluster configuration.",
 *     "code": "INTERNAL_SERVER_ERROR"
 *   }
 * }
 * ```
 *
 * @param responseCode sets the HTTP statusCode of the response. The response code name will be output into the details section.
 * @param title title of the exception response. Keep this brief by including extra information in the [details] section.
 * @param details optionally include any additional details that may be useful.
 * @param exceptionDetails optionally include details of the exception, the `cause` and `reason` will be output in details section.
 */
abstract class HttpApiException(
    val responseCode: ResponseCode,
    val title: String,
    val details: Map<String, String> = emptyMap(),
    val exceptionDetails: ExceptionDetails? = null
) : CordaRuntimeException(title)