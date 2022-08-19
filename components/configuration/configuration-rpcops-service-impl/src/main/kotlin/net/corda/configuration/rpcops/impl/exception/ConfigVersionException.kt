package net.corda.configuration.rpcops.impl.exception

import net.corda.data.config.ConfigurationSchemaVersion
import net.corda.httprpc.ResponseCode
import net.corda.httprpc.exception.ExceptionDetails
import net.corda.httprpc.exception.HttpApiException

/**
 * Config version related exceptions.
 */
class ConfigVersionException(errorType: String, errorMessage: String, schemaVersion: ConfigurationSchemaVersion, config: String) :
    HttpApiException(
        responseCode = ResponseCode.INTERNAL_SERVER_ERROR,
        message = "Config version error.",
        details = mapOf(
            "schemaVersion" to "${schemaVersion.majorVersion}.${schemaVersion.minorVersion}",
            "config" to config
        ),
        exceptionDetails = ExceptionDetails(errorType, errorMessage)
    )