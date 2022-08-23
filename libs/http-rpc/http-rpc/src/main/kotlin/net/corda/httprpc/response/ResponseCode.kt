package net.corda.httprpc.response

/**
 * Use this enum when you want to customize the HTTP status code returned in success responses and error scenarios in HTTP APIs.
 *
 * This enum will define all HTTP status codes and their causes. They also include a reason code which will help to identify particular
 * error responses and aid in debugging and support issues.
 *
 * This code will be on response messages and forms part of the public API. Adding message with new reason codes is not considered a
 * breaking change. Changing status or reason codes after release is considered a breaking change.
 *
 * Status codes:
 * 2XX - indicate a request was successfully received, understood and accepted.
 * 3XX - indicate further action needs to be taken in order to fulfill a request.
 * 4XX - indicate a problem with the request. Requests can be re-submitted, usually with updated arguments and may succeed.
 * 5XX - indicate a problem occurred on the server side while processing the request. An application can't perform any action to correct a
 * 5xx-level error.
 *
 * @param statusCode the http status code for the http response.
 */
enum class ResponseCode constructor(val statusCode: Int) {

    /**
     * Request has succeeded.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#status.200`.
     */
    OK(200),

    /**
     * One or more resources have been successfully created.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-201-created`.
     */
    CREATED(201),

    /**
     * The request has been accepted for processing but the processing has not been completed.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-202-accepted`.
     */
    ACCEPTED(202),

    /**
     * A request has succeeded but there is no content to send to the client.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-204-no-content`.
     */
    NO_CONTENT(204),

    /**
     * The requested resource is located at another URI using the GET HTTP method. Use this for response from asynchronous APIs that return
     * a status URI.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-303-see-other`.
     */
    SEE_OTHER(303),

    /**
     * Signals the exception occurred due to invalid input data in the request or from a resource identified by the request.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-400-bad-request`.
     */
    BAD_REQUEST(400),

    /**
     * Signals the user authentication failed.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-401-unauthorized`.
     */
    NOT_AUTHENTICATED(401),

    /**
     * Signals the user is not authorized to perform an action.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-403-forbidden`.
     */
    FORBIDDEN(403),

    /**
     * Signals the requested resource was not found.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-404-not-found`.
     */
    RESOURCE_NOT_FOUND(404),

    /**
     * Signals the resource is not in the expected state.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-409-conflict`.
     */
    CONFLICT(409),

    /**
     * An unexpected condition occurred that prevented it from fulfilling the request.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-500-internal-server-error`.
     */
    INTERNAL_SERVER_ERROR(500),

    /**
     * Common causes are a server that is down for maintenance or that is overloaded.
     * This response should be used for temporary conditions and the `Retry-After` HTTP header should, if possible,
     * contain the estimated time for the recovery of the service.
     *
     * See `https://www.rfc-editor.org/rfc/rfc9110.html#name-503-service-unavailable`.
     */
    SERVICE_UNAVAILABLE(503),
    ;

    override fun toString(): String {
        return name
    }
}