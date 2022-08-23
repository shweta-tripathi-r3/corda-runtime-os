package net.corda.httprpc.response

/**
 * Class allowing control of response codes for HTTP responses.
 */
class HttpResponse<T : Any>(
    val responseCode: ResponseCode,
    val responseBody: T?,
) {
    companion object {
        fun <T : Any> ok(responseBody: T): HttpResponse<T> {
            return HttpResponse(ResponseCode.OK, responseBody)
        }
        fun <T : Any> resourceUpdated(responseBody: T): HttpResponse<T> {
            return HttpResponse(ResponseCode.OK, responseBody)
        }
        fun <T : Any> resourceCreated(responseBody: T): HttpResponse<T> {
            return HttpResponse(ResponseCode.CREATED, responseBody)
        }
        fun <T : Any> resourceDeleted(responseBody: T): HttpResponse<T> {
            return HttpResponse(ResponseCode.OK, responseBody)
        }
        fun resourceDeleted(): HttpResponse<Void> {
            return HttpResponse(ResponseCode.OK, null)
        }
        fun <T : Any> requestAccepted(responseBody: T): HttpResponse<T> {
            return HttpResponse(ResponseCode.ACCEPTED, responseBody)
        }
        fun noContent(): HttpResponse<Void> {
            return HttpResponse(ResponseCode.NO_CONTENT, null)
        }
        fun <T : Any> seeOther(responseBody: T): HttpResponse<T> {
            return HttpResponse(ResponseCode.SEE_OTHER, responseBody)
        }
    }
}
