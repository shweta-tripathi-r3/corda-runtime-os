package net.corda.httprpc.server.impl.internal

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.javalin.http.HttpResponseException
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.UnauthorizedResponse
import net.corda.httprpc.response.ResponseCode
import net.corda.httprpc.exception.HttpApiException
import net.corda.httprpc.server.impl.exception.MissingParameterException
import net.corda.v5.base.exceptions.CordaRuntimeException
import java.util.concurrent.TimeoutException
import javax.security.auth.login.FailedLoginException
import net.corda.httprpc.exception.ExceptionDetails

internal object HttpExceptionMapper {

    fun mapToResponse(e: Exception): HttpResponseException {
        return when (e) {
            // the code has already thrown the appropriate Javalin response exception.
            is HttpResponseException -> e

//            is BadRpcStartFlowRequestException -> buildBadRequestResponse("Operation failed due to bad RPC StartFlow request.", e)
            is MissingKotlinParameterException -> buildBadRequestResponse("Missing or invalid field in JSON request body.", e)
            is JsonProcessingException -> buildBadRequestResponse("Error during processing of request JSON.", e)
            is MissingParameterException -> buildBadRequestResponse("Missing parameter in request.", e)
            // TODO restore these when possible
            //  is StartFlowPermissionException -> ForbiddenResponse(loggedMessage)
            //  is FlowNotFoundException -> NotFoundResponse(loggedMessage)
            //  is InvalidMemberX500NameException -> BadRequestResponse(loggedMessage)
            //  is MemberNotFoundException -> NotFoundResponse(loggedMessage)

            // catch-all for failed login attempts
            is FailedLoginException -> UnauthorizedResponse("User authentication failed.")

            // catch-all for Timeouts
            is TimeoutException -> InternalServerErrorResponse("Timeout occurred while processing operation.")

            // catch-all for IllegalArgumentExceptions
            is IllegalArgumentException -> HttpResponseException(
                ResponseCode.INTERNAL_SERVER_ERROR.statusCode,
                "Illegal argument occurred.",
                addDetails(ResponseCode.INTERNAL_SERVER_ERROR, e)
            )

            // Http API exceptions
            is HttpApiException -> e.asHttpResponseException()

            is CordaRuntimeException -> HttpResponseException(
                ResponseCode.INTERNAL_SERVER_ERROR.statusCode,
                "Internal server error.",
                addDetails(ResponseCode.INTERNAL_SERVER_ERROR, e)
            )

            else -> HttpResponseException(
                ResponseCode.UNEXPECTED_ERROR.statusCode,
                "Unexpected error occurred.",
                addDetails(ResponseCode.UNEXPECTED_ERROR, e)
            )
        }
    }

    private fun HttpApiException.asHttpResponseException(): HttpResponseException {
        return HttpResponseException(
            responseCode.statusCode,
            message,
            details.toMutableMap().addDetails(responseCode, exceptionDetails)
        )
    }

    /**
     * Since Javalin's 'BadRequestResponse' does not allow extra details, we'll manually build the HttpResponseException with a BAD_REQUEST
     * status code, a message, and extra exception details that includes the original exception type and message to help the user fix their
     * request.
     * Unless details are already supplied by [HttpApiException].
     */
    private fun buildBadRequestResponse(message: String, e: Exception): HttpResponseException {
        return (e.cause as? HttpApiException)?.asHttpResponseException() ?: HttpResponseException(
            ResponseCode.BAD_REQUEST.statusCode,
            message,
            addDetails(ResponseCode.BAD_REQUEST, e)
        )
    }

    /**
     * We'll add the code and exception details to the response.
     */
    private fun addDetails(responseCode: ResponseCode, e: Exception?): MutableMap<String, String> {
        return addDetails(responseCode, e?.let { ExceptionDetails(e::class.java.name, e.message ?: "") })
    }

    /**
     * We'll add the code and exception details to the response.
     */
    private fun addDetails(responseCode: ResponseCode, exceptionDetails: ExceptionDetails?): MutableMap<String, String> {
        return mutableMapOf<String, String>().addDetails(responseCode, exceptionDetails)
    }

    /**
     * We'll add the code and exception details to the response.
     */
    private fun MutableMap<String, String>.addDetails(
        responseCode: ResponseCode,
        exceptionDetails: ExceptionDetails?
    ): MutableMap<String, String> {
        exceptionDetails?.let {
            this["cause"] = it.cause
            this["reason"] = it.reason
        }
        this["code"] = responseCode.name
        return this
    }
}