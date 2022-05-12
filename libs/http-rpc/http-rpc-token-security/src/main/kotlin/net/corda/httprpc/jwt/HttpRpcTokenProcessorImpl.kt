package net.corda.httprpc.jwt

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import net.corda.lifecycle.Lifecycle
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.ServiceScope
import java.time.Instant
import java.util.Date

@Component(service = [HttpRpcTokenProcessor::class], scope = ServiceScope.SINGLETON)
class HttpRpcTokenProcessorImpl : HttpRpcTokenProcessor, Lifecycle {

    private var key: ECKey? = null

    override fun buildAndSignToken(subject: String): String {
        val fetchedKey = fetchKey()

        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(JOSEObjectType.JWT)
            .keyID(fetchedKey.keyID)
            .build();
        val payload = JWTClaimsSet.Builder()
            .issuer("r3")
            .subject(subject)
            .expirationTime(Date.from(Instant.now().plusSeconds(86400)))
            .issueTime(Date.from(Instant.now()))
            .notBeforeTime(Date.from(Instant.now()))
            .build()

        val signedJWT = SignedJWT(header, payload)
        signedJWT.sign(ECDSASigner(fetchedKey.toECPrivateKey()))
        return signedJWT.serialize()
    }

    override fun getSubject(jwt: String): String {
        return SignedJWT.parse(jwt).jwtClaimsSet.subject
    }

    override fun verify(jwt: String): Boolean {
        return if (key == null) {
            false
        } else {
            SignedJWT.parse(jwt).verify(ECDSAVerifier(key!!.toECPublicKey()))
        }
    }

    // add proper gen and fetch logic
    private fun fetchKey(): ECKey {

        if (key == null) {
            key = ECKeyGenerator(Curve.P_256)
                .keyID("123")
                .generate()
        }
        return key as ECKey
    }

    override val isRunning: Boolean
        get() = TODO("Not yet implemented")

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }
}