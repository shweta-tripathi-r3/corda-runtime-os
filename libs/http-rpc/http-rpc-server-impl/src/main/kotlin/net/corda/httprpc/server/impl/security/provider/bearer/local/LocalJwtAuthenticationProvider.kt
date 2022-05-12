package net.corda.httprpc.server.impl.security.provider.bearer.local

import net.corda.httprpc.jwt.HttpRpcTokenProcessor
import net.corda.httprpc.security.AuthorizingSubject
import net.corda.httprpc.security.read.RPCSecurityManager
import net.corda.httprpc.server.impl.security.provider.bearer.BearerTokenAuthenticationProvider
import net.corda.httprpc.server.impl.security.provider.credentials.tokens.BearerTokenAuthenticationCredentials
import net.corda.v5.base.util.contextLogger
import javax.security.auth.login.FailedLoginException

internal class LocalJwtAuthenticationProvider(
    private val jwtProcessor: HttpRpcTokenProcessor,
    private val rpcSecurityManager: RPCSecurityManager
) : BearerTokenAuthenticationProvider() {

    companion object {
        private val logger = contextLogger()
    }

    override fun doAuthenticate(credential: BearerTokenAuthenticationCredentials): AuthorizingSubject {

        try {
            if (jwtProcessor.verify(credential.token)) {
                return rpcSecurityManager.buildSubject(jwtProcessor.getSubject(credential.token))
            } else {
                throw FailedLoginException("Failed to parse JWT token.")
            }
        } catch (e: Exception) {
            logger.error("Unexpected exception when parsing token", e)
            // Catching Exception here to mitigate https://nvd.nist.gov/vuln/detail/CVE-2021-27568,
            // even though: `com.nimbusds.jose.util.JSONObjectUtils.parse` already has similar sort of logic,
            // but this may change in the future
            // versions.
            throw FailedLoginException("Failed to parse JWT token.")
        }
    }
}