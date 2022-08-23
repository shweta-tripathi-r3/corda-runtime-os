package net.corda.configuration.rpcops.impl.exception

import net.corda.data.config.ConfigurationSchemaVersion
import net.corda.httprpc.response.ResponseCode
import net.corda.httprpc.exception.ExceptionDetails
import net.corda.httprpc.exception.HttpApiException

/**
 * Config version related exceptions.
 */
class ConfigVersionException(
    title: String,
    errorType: String,
    errorMessage: String,
    schemaVersion: ConfigurationSchemaVersion,
    config: String
) :
    HttpApiException(
        responseCode = ResponseCode.INTERNAL_SERVER_ERROR,
        title = title,
        details = mapOf(
            "schemaVersion" to "${schemaVersion.majorVersion}.${schemaVersion.minorVersion}",
            "config" to config
        ),
        exceptionDetails = ExceptionDetails(errorType, errorMessage)
    )