package net.corda.httprpc.response

fun <T : Any> buildNewResourceCreatedResponse(responseBody: T) = HttpResponse(ResponseCode.CREATED, responseBody)
fun <T : Any> buildResourceUpdatedResponse(responseBody: T) = HttpResponse(ResponseCode.OK, responseBody)
fun <T : Any> buildAsynchronousOperationResponse(responseBody: T) = HttpResponse(ResponseCode.ACCEPTED, responseBody)
