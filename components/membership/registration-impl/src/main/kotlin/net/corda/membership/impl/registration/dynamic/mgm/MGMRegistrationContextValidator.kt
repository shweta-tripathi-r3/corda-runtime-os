package net.corda.membership.impl.registration.dynamic.mgm

import net.corda.configuration.read.ConfigurationGetService
import net.corda.membership.impl.registration.dynamic.verifiers.OrderVerifier
import net.corda.membership.impl.registration.dynamic.verifiers.P2pEndpointVerifier
import net.corda.membership.lib.grouppolicy.GroupPolicyConstants.PolicyValues.P2PParameters.TlsType
import net.corda.membership.lib.grouppolicy.GroupPolicyConstants.PolicyValues.P2PParameters.SessionPkiMode.NO_PKI
import net.corda.membership.lib.schema.validation.MembershipSchemaValidationException
import net.corda.membership.lib.schema.validation.MembershipSchemaValidatorFactory
import net.corda.schema.membership.MembershipSchema
import net.corda.utilities.time.Clock
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.versioning.Version
import java.security.cert.CertificateException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateFactory
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.util.Date

internal class MGMRegistrationContextValidator(
    private val membershipSchemaValidatorFactory: MembershipSchemaValidatorFactory,
    private val orderVerifier: OrderVerifier = OrderVerifier(),
    private val p2pEndpointVerifier: P2pEndpointVerifier = P2pEndpointVerifier(orderVerifier),
    private val configurationGetService: ConfigurationGetService,
    private val clock: Clock
) {

    private companion object {
        const val errorMessageTemplate = "No %s was provided."

        val errorMessageMap = errorMessageTemplate.run {
            mapOf(
                ECDH_KEY_ID to format("ECDH key"),
                REGISTRATION_PROTOCOL to format("registration protocol"),
                SYNCHRONISATION_PROTOCOL to format("synchronisation protocol"),
                P2P_MODE to format("P2P mode"),
                SESSION_KEY_POLICY to format("session key policy"),
                PKI_SESSION to format("session PKI property"),
                PKI_TLS to format("TLS PKI property"),
            )
        }
    }

    private val certificateFactory = CertificateFactory.getInstance("X.509")

    @Suppress("ThrowsCount")
    @Throws(MGMRegistrationContextValidationException::class)
    fun validate(context: Map<String, String>) {
        try {
            validateContextSchema(context)
            validateContext(context)
        } catch (ex: MembershipSchemaValidationException) {
            throw MGMRegistrationContextValidationException(
                "Onboarding MGM failed. ${ex.message}",
                ex
            )
        } catch (ex: IllegalArgumentException) {
            throw MGMRegistrationContextValidationException(
                "Onboarding MGM failed. ${ex.message ?: "Reason unknown."}",
                ex
            )
        } catch (ex: Exception) {
            throw MGMRegistrationContextValidationException(
                "Onboarding MGM failed. Unexpected error occurred during context validation. ${ex.message}",
                ex
            )
        }
    }

    @Throws(MembershipSchemaValidationException::class)
    private fun validateContextSchema(context: Map<String, String>) {
        membershipSchemaValidatorFactory
            .createValidator()
            .validateRegistrationContext(
                MembershipSchema.RegistrationContextSchema.Mgm,
                Version(1, 0),
                context
            )
    }

    @Throws(IllegalArgumentException::class)
    @Suppress("ThrowsCount")
    private fun validateContext(context: Map<String, String>) {
        for (key in errorMessageMap.keys) {
            context[key] ?: throw IllegalArgumentException(errorMessageMap[key])
        }
        p2pEndpointVerifier.verifyContext(context)
        if (context[PKI_SESSION] != NO_PKI.toString()) {
            context.keys.filter { TRUSTSTORE_SESSION.format("[0-9]+").toRegex().matches(it) }.apply {
                require(isNotEmpty()) { "No session trust store was provided." }
                require(orderVerifier.isOrdered(this, 4)) { "Provided session trust stores are incorrectly numbered." }
                this.map { key ->
                    validateTrustrootCert(context[key]!!, key)
                }
            }
        }
        context.keys.filter { TRUSTSTORE_TLS.format("[0-9]+").toRegex().matches(it) }.apply {
            require(isNotEmpty()) { "No TLS trust store was provided." }
            require(orderVerifier.isOrdered(this, 4)) { "Provided TLS trust stores are incorrectly numbered." }
            this.map { key ->
                validateTrustrootCert(context[key]!!, key)
            }
        }

        context.keys.filter { SESSION_KEY_IDS.format("[0-9]+").toRegex().matches(it) }.apply {
            require(isNotEmpty()) { "No session key was provided." }
            require(orderVerifier.isOrdered(this, 3)) { "Provided session keys are incorrectly numbered." }
        }
        val contextRegistrationTlsType = context[TLS_TYPE]?.let { tlsType ->
            TlsType.fromString(tlsType) ?: throw IllegalArgumentException("Invalid TLS type: $tlsType")
        } ?: TlsType.ONE_WAY
        val clusterTlsType = TlsType.getClusterType(configurationGetService::getSmartConfig)
        if (contextRegistrationTlsType != clusterTlsType) {
            throw IllegalArgumentException(
                "A cluster configured with TLS type of $clusterTlsType can not register " +
                "an MGM with TLS type $contextRegistrationTlsType"
            )
        }
    }

    @Suppress("ThrowsCount")
    private fun validateTrustrootCert(pemCert: String, key: String) {
        val certificate = try {
            certificateFactory.generateCertificate(pemCert.byteInputStream())
        } catch (ex: CertificateException) {
            throw IllegalArgumentException("Trust root certificate specified in registration context under key $key " +
                    "was not a valid PEM certificate.")
        }

        try {
            (certificate as X509Certificate).checkValidity(Date.from(clock.instant()))
        } catch (ex: Exception) {
            when(ex) {
                is CertificateExpiredException, is CertificateNotYetValidException -> {
                    throw IllegalArgumentException("Trust root certificate specified in registration context under key $key " +
                    "does not have a valid validity period.")
                }
                else -> throw ex
            }
        }
    }
}

internal class MGMRegistrationContextValidationException(
    val reason: String,
    e: Throwable?
) : CordaRuntimeException(reason, e)